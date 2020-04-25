package CaseData;

import com.covidpredictor.covidpredictor.DataHeaders;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class DataGenerator {
    Map<String, Country> countries;

    public DataGenerator() {
        countries = new TreeMap<>();

        caseData();
        movementData();
        populationData();
    }

    private void caseData() {
        Iterable<CSVRecord> records = null;
        try {
            Reader reader = new FileReader("src/main/resources/data/train.csv");
            records = CSVFormat.DEFAULT.withHeader(DataHeaders.class).parse(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (records == null) {
            System.out.println("No case data found");
            return;
        }

         boolean header = true;

        for (CSVRecord record : records) {
            if (!header) {
                if (!countries.containsKey(record.get(DataHeaders.Country_Region))) {
                    countries.put(
                            record.get(DataHeaders.Country_Region),
                            new Country(record.get(DataHeaders.Country_Region))
                    );
                }
                countries.get(record.get(DataHeaders.Country_Region)).addCases(record);
            }
            header = false;
        }
    }

    private void movementData() {
        List<String> files = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(Paths.get("src/main/resources/data/mobilityData"), 1)) {

            files = walk.map(Path::toString)
                    .filter(f -> f.endsWith(".json")).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (files.isEmpty()) {
            System.out.println("No movement data found");
        }

        JSONParser jsonParser = new JSONParser();

        for (String file : files) {
            try (Reader reader = new FileReader(file)) {
                Object object = jsonParser.parse(reader);

                JSONArray entries = (JSONArray) object;

                if (!entries.isEmpty()) {
                    String country = (String) ((JSONObject) entries.get(0)).get("country");

                    switch (country) {
                        case "Cape Verde":
                            country = "Cabo Verde";
                            break;
                        case "Côte d'Ivoire":
                            country = "Cote d'Ivoire";
                            break;
                        case "Myanmar (Burma)":
                            country = "Burma";
                            break;
                        case "South Korea":
                            country = "Korea, South";
                            break;
                        case "Taiwan":
                            country = "Taiwan*";
                            break;
                        case "The Bahamas":
                            country = "Bahamas";
                            break;
                        case "United States":
                            country = "US";
                            break;
                        case "Dominican Republic":
                            country = "Dominica";
                            break;
                    }

                    if (countries.containsKey(country)) {
                        for (Object entry : entries) {
                            JSONObject jsonObject = (JSONObject) entry;
                            countries.get(country).addMovement(jsonObject);
                        }
                    } else if (country.equals("Aruba")) {
                        for (Object entry : entries) {
                            JSONObject jsonObject = (JSONObject) entry;
                            countries.get("Netherlands").regions.get("Aruba").addMovement(jsonObject);
                        }
                    } else if (country.equals("Hong Kong")) {
                        for (Object entry : entries) {
                            JSONObject jsonObject = (JSONObject) entry;
                            countries.get("China").regions.get("Hong Kong").addMovement(jsonObject);
                        }
                    } else if (country.equals("Puerto Rico")) {
                        for (Object entry : entries) {
                            JSONObject jsonObject = (JSONObject) entry;
                            countries.get("US").regions.get("Puerto Rico").addMovement(jsonObject);
                        }
                    }
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void populationData() {
        List<String> files = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(Paths.get("src/main/resources/data/population"))) {

            files = walk.map(Path::toString)
                    .filter(f -> f.endsWith(".csv")).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String file : files) {
            System.out.println(file);
            Iterable<CSVRecord> records = null;

            try {
                Reader reader = new FileReader(file);
                records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (CSVRecord record : records) {
                System.out.println(record.get("Region") + " " + record.get("Population"));
            }
        }
    }

    public static void main(String[] args) {
        new DataGenerator();
    }
}
