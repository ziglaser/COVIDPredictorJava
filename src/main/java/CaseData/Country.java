package CaseData;

import com.covidpredictor.covidpredictor.DataHeaders;
import org.apache.commons.csv.CSVRecord;
import org.json.simple.JSONObject;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;



public class Country extends Region {
    Map<String, Region> regions;

    public Country(String name) {
        super(name);

        regions = new HashMap<>();
    }

    public void addCases(CSVRecord record) {
        String region = record.get(DataHeaders.Province_State);
        LocalDate date = LocalDate.parse(record.get(DataHeaders.Date));

        if (!region.equals("")) {
            if (!regions.containsKey(region)) {
                regions.put(region, new Region(region));
            }
            regions.get(region).addCases(date, record);
        } else {
            caseData.put(
                    date,
                    new CaseEntry(
                            Double.parseDouble(record.get(DataHeaders.ConfirmedCases)),
                            Double.parseDouble(record.get(DataHeaders.Fatalities))
                    )
            );
        }
    }

    public void addMovement(JSONObject entry) {
        LocalDate date = LocalDate.parse((String) entry.get("date"));

        if (entry.containsKey("region") && entry.get("region") != null) {
            if (entry.get("region") != null) {
                String region = (String) entry.get("region");
                if (!regions.containsKey(region)) {
                    regions.put(
                            region,
                            new Region(region)
                    );
                }
                regions.get(region).addMovement(entry);
            }
        } else {
            if (!categories.containsKey((String) entry.get("category"))) {
                categories.put(
                        (String) entry.get("category"),
                        new HashMap<>()
                );
            }
            categories.get((String) entry.get("category")).put(
                    date, sigmoid((Double) entry.get("value"))
            );
        }
    }
}
