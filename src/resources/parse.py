import numpy as np
import pandas as pd
from collections import defaultdict
import glob

import fitz

date = "2020-04-11"


def parse_stream(stream):
    data_raw = []
    data_transformed = []
    rotparams = None
    npatches = 0
    for line in stream.splitlines():
        if line.endswith(" cm"):
            rotparams = list(map(float, line.split()[:-1]))
        elif line.endswith(" l"):
            x, y = list(map(float, line.split()[:2]))
            a, b, c, d, e, f = rotparams
            xp = a * x + c * y + e
            yp = b * x + d * y + f
            data_transformed.append([xp, yp])
            data_raw.append([x, y])
        elif line.endswith(" m"):
            npatches += 1
        else:
            pass
    data_raw = np.array(data_raw)
    if len(data_raw) < 1:
        return dict(data=np.array(data_raw), npatches=npatches, good=False)
    base_x, base_y = data_raw[-1]
    good = False
    if base_x == 0.:
        data_raw[:, 1] = base_y - data_raw[:, 1]
        data_raw[:, 1] *= 100 / 60.
        data_raw = data_raw[data_raw[:, 1] != 0.]
        if npatches == 1: good = True
    return dict(data=np.array(data_raw), npatches=npatches, good=good)


def parse_page(doc, page_index, verbose=False):
    categories = [
        "Retail & recreation",
        "Grocery & pharmacy",
        "Parks",
        "Transit stations",
        "Workplace",
        "Residential",
    ]

    regions = []
    curr_county = None
    curr_category = None
    data = defaultdict(lambda: defaultdict(list))
    pagetext = doc.getPageText(page_index)
    lines = pagetext.splitlines()
    tickdates = list(filter(lambda x: len(x.split()) == 3, set(lines[-10:])))

    in_name = True
    name = ""
    for line in range(len(lines)):
        # if we encountered a category, add to dict, otherwise
        # push all seen lines into the existing dict entry
        if any(lines[line].startswith(c) for c in categories):
            if in_name:
                in_name = False
                regions.append(name)
                curr_county = name
                name = ""
            curr_category = lines[line]
        elif curr_category:
            data[curr_county][curr_category].append(lines[line])

        # If it doesn't match anything, then it's a county name
        if (all(c not in lines[line] for c in categories)
                and ("compared to baseline" not in lines[line])
                and ("Not enough data" not in lines[line])
        ):
            in_name = True
            # saw both counties already
            if len(data.keys()) == 2: break
            if name == "":
                name = lines[line]
            else:
                name = " ".join([name, lines[line]])


    newdata = {}
    for county in data:
        newdata[county] = {}
        for category in data[county]:
            # if the category text ends with a space, then there was a star/asterisk there
            # indicating lack of data. we skip these.
            if category.endswith(" "): continue
            temp = [x for x in data[county][category] if "compared to baseline" in x]
            if not temp: continue
            percent = int(temp[0].split()[0].replace("%", ""))
            newdata[county][category.strip()] = percent
    data = newdata

    tomatch = []
    for county in regions:
        for category in categories:
            try:
                if category in data[county]:
                    tomatch.append([county, category, data[county][category]])
            except KeyError:
                print(data)

    if verbose:
        print(len(tomatch))
        print(data)

    goodplots = []
    xrefs = sorted(doc.getPageXObjectList(page_index), key=lambda x: int(x[1].replace("X", "")))
    for i, xref in enumerate(xrefs):
        stream = doc.xrefStream(xref[0]).decode()
        info = parse_stream(stream)
        if not info["good"]: continue
        goodplots.append(info)
    if verbose:
        print(len(goodplots))

    ret = []

    if len(tomatch) != len(goodplots):
        return ret

    for m, g in zip(tomatch, goodplots):
        xs = g["data"][:, 0]
        ys = g["data"][:, 1]
        maxys = ys[np.where(xs == xs.max())[0]]
        maxy = maxys[np.argmax(np.abs(maxys))]

        # parsed the tick date labels as text. find the min/max (first/last)
        # and make evenly spaced dates, one per day, to assign to x values between
        # 0 and 200 (the width of the plots).
        ts = list(map(lambda x: pd.Timestamp(x.split(None, 1)[-1] + ", 2020"), tickdates))
        low, high = min(ts), max(ts)
        dr = list(map(lambda x: str(x).split()[0], pd.date_range(low, high, freq="D")))
        lutpairs = list(zip(np.linspace(0, 200, len(dr)), dr))

        dates = []
        values = []
        asort = xs.argsort()
        xs = xs[asort]
        ys = ys[asort]
        for x, y in zip(xs, ys):
            date = min(lutpairs, key=lambda v: abs(v[0] - x))[1]
            dates.append(date)
            values.append(round(y, 3))

        ret.append(dict(
            county=m[0], category=m[1], change=m[2],
            values=values,
            dates=dates,
            changecalc=maxy,
        ))
    return ret


def parse_page_country(country, doc, page_index, verbose=False):
    categories = [
        "Retail & recreation",
        "Grocery & pharmacy",
        "Parks",
        "Transit stations",
        "Workplace",
        "Residential",
        "Workplaces"
    ]

    current_category = None
    data = defaultdict(lambda: defaultdict(list))

    # goes through all the lines in the page
    # adds them to the list of the last category identified
    pagetext = doc.getPageText(page_index)
    lines = pagetext.splitlines()

    lines_to_search = []
    for line in lines:
        if any(day in line for day in ["Sun", "Sat"]):
            lines_to_search.append(line)

    tick_dates = list(filter(lambda x: len(x.split()) == 3, set(lines_to_search[-3:])))

    for line in range(len(lines)):
        # if we encountered a category, add to dict, otherwise
        # push all seen lines into the existing dict entry
        for c in categories:
            if lines[line].startswith(c):
                current_category = lines[line]
        if current_category:
            if current_category in data:
                data[current_category].append(lines[line - 1] + " " + lines[line])
            else:
                data[current_category] = [lines[line - 1] + " " + lines[line]]

    newdata = {}

    # Eliminates the categories with too little data, in theory
    # Doesn't seem to work
    for category in data:
        # if the category text ends with a space, then there was a star/asterisk there
        # indicating lack of data. we skip these.
        if not (category.endswith(" ") or category.endswith("*")):
            temp = [x for x in data[category] if "compared to baseline" in x and "Not enough data" not in x]
            if not temp: continue
            try:
                percent = int(temp[0].split()[0].replace("%", ""))

                newdata[category.strip()] = percent
            except:
                continue

    data = newdata
    # print(data)

    tomatch = []

    # In the national data, category "Workplace" is entered "Workplaces"
    for category in categories:
        if category in data:
            if category == "Workplaces":
                tomatch.append([country, "Workplace", data[category]])
            else:
                tomatch.append([country, category, data[category]])

    # Some countries have graphs with gaps without an astrix
    errors = {
        "LI": ['Transit stations'],
        "RE": ['Workplace'],
        "LU": ['Residential']
    }

    for c, e in errors.items():
        if country == c:
            print(tomatch)
            for item in tomatch:
                if any(a in item for a in e):
                    tomatch.remove(item)

    if verbose:
        print(len(tomatch))
        print(data)

    # Gathers all the data which fitz deems good
    goodplots = []
    xrefs = sorted(doc.getPageXObjectList(page_index), key=lambda x: int(x[1].replace("X", "")))
    for i, xref in enumerate(xrefs):
        stream = doc.xrefStream(xref[0]).decode()
        info = parse_stream(stream)
        #print(len(info['data']))
        if not info["good"] and not len(info['data'][0]) < 43: continue
        goodplots.append(info)
    if verbose:
        print(len(goodplots))

    result = []

    # Fails if the the number of good plots doesn't match the number of expected plots
    if len(tomatch) != len(goodplots):
        print(country)
        print("data :", data)
        print("tomatch :", tomatch)
        print("goodplots :", goodplots)
        print(len(tomatch), len(goodplots))
        print("failed")
        print("\n\n\n")
        return result

    for m, g in zip(tomatch, goodplots):
        xs = g["data"][:, 0]
        ys = g["data"][:, 1]
        maxys = ys[np.where(xs == xs.max())[0]]
        maxy = maxys[np.argmax(np.abs(maxys))]

        # parsed the tick date labels as text. find the min/max (first/last)
        # and make evenly spaced dates, one per day, to assign to x values between
        # 0 and 200 (the width of the plots).
        ts = list(map(lambda x: pd.Timestamp(x.split(None, 1)[-1] + ", 2020"), tick_dates))
        low, high = min(ts), max(ts)
        dr = list(map(lambda x: str(x).split()[0], pd.date_range(low, high, freq="D")))
        lutpairs = list(zip(np.linspace(0, 200, len(dr)), dr))

        dates = []
        values = []
        asort = xs.argsort()
        xs = xs[asort]
        ys = ys[asort]
        for x, y in zip(xs, ys):
            date = min(lutpairs, key=lambda v: abs(v[0] - x))[1]
            dates.append(date)
            values.append(round(y, 3))

        result.append(dict(
            country=m[0], category=m[1], change=m[2],
            values=values,
            dates=dates,
            changecalc=maxy,
        ))
    return result


def parse_state(state):
    data = []
    if state in [x.split("_US_", 1)[1].split("_Mobility", 1)[0] for x in glob.glob("mobilityData/US/*.pdf")]:
        document = fitz.Document(f"mobilityDataPDF/US/{date}_US_{state}_Mobility_Report_en.pdf")
        for i in range(2, document.pageCount - 1):
            for entry in parse_page(document, i):
                entry["state"] = state
                entry["page"] = i
                data.append(entry)
        df = pd.DataFrame(data)
        return df[["state", "county", "category", "change", "changecalc", "dates", "values", "page"]]

    else:
        document = fitz.Document(f"mobilityDataPDF/2020-04-11_{state}_Mobility_Report_en.pdf")
        if document.pageCount < 4:
            return pd.DataFrame(data)
        for i in range(2, document.pageCount - 1):
            for entry in parse_page(document, i):
                entry["country"] = state
                entry["page"] = i
                data.append(entry)
        df = pd.DataFrame(data)
        return df[["country", "county", "category", "change", "changecalc", "dates", "values", "page"]]


def parse_name(document):
    lines = document.getPageText(0).splitlines()
    return " ".join(lines[1].split()[:-3])


def parse_country(country):
    document = fitz.Document(f"mobilityDataPDF/2020-04-11_{country}_Mobility_Report_en.pdf")

    country_name = parse_name(document)

    data = []
    for i in range(0, 2):
        for entry in parse_page_country(country, document, i):
            entry["country"] = country_name
            entry["page"] = i
            data.append(entry)

    if not data:
        return None
    df = pd.DataFrame(data)
    return df[["country", "category", "change", "changecalc", "dates", "values", "page"]]


if __name__ == '__main__':
    states = [x.split("_US_", 1)[1].split("_Mobility", 1)[0] for x in glob.glob("mobilityData/US/*.pdf")]
    countries = [x[27:29] for x in glob.glob("mobilityDataPDF/*.pdf")]

    # for state in tqdm(states):
    #
    #     dataFrame = parse_state(state)
    #     data = []
    #
    #     for i,row in dataFrame.iterrows():
    #         # do a little clean up and unstack the dates/values as separate rows
    #         dorig = dict()
    #         dorig["state"] = row["state"].replace("_"," ")
    #         dorig["county"] = row["county"]
    #         dorig["category"] = row["category"].replace(" & ","/").replace(" ","").lower()
    #         dorig["page"] = row["page"]
    #         dorig["change"] = row["change"]
    #         dorig["changecalc"] = row["changecalc"]
    #         for x,y in zip(row["dates"],row["values"]):
    #             d = dorig.copy()
    #             d["date"] = x
    #             d["value"] = y
    #             data.append(d)
    #     dataFrame = pd.DataFrame(data)
    #     dataFrame.to_json("data/States/" + state + ".json", orient="records", indent=2)

    for country in countries:
        dataFrame = parse_country(country)
        data = []

        if dataFrame is not None:
            for i, row in dataFrame.iterrows():
                dorig = dict()
                dorig["country"] = row["country"].replace("_", " ")
                dorig["category"] = row["category"].replace(" & ", "/").replace(" ", "").lower()
                dorig["page"] = row["page"]
                dorig["change"] = row["change"]
                dorig["changecalc"] = row["changecalc"]
                for date, value in zip(row["dates"], row["values"]):
                    d = dorig.copy()
                    d["date"] = date
                    d["value"] = value
                    data.append(d)

        dataFrame = parse_state(country)
        for i, row in dataFrame.iterrows():
            # do a little clean up and unstack the dates/values as separate rows
            dorig = dict()
            dorig["country"] = row["country"].replace("_", " ")
            dorig["region"] = row["county"]
            dorig["category"] = row["category"].replace(" & ", "/").replace(" ", "").lower()
            dorig["page"] = row["page"]
            dorig["change"] = row["change"]
            dorig["changecalc"] = row["changecalc"]
            for x, y in zip(row["dates"], row["values"]):
                d = dorig.copy()
                d["date"] = x
                d["value"] = y
                data.append(d)

        dataFrame = pd.DataFrame(data)
        dataFrame.to_json("mobilityData/" + country + ".json", orient="records", indent=2)
