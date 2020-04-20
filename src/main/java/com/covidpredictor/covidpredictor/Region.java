package com.covidpredictor.covidpredictor;

import lombok.Getter;
import org.apache.commons.csv.CSVRecord;
import org.json.simple.JSONObject;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Getter
public class Region {
    private final String name;

    private Map<LocalDate, CaseEntry> caseData;

    private Map<String, Map<LocalDate, Double>> categories;

    public Region(String name) {
        this.name = name;

        caseData = new TreeMap<>();

        categories = new HashMap<>();
    }

    public void addCases(LocalDate date, CSVRecord record) {
        caseData.put(
                date,
                new CaseEntry(
                        Double.parseDouble(record.get(DataHeaders.ConfirmedCases)),
                        Double.parseDouble(record.get(DataHeaders.Fatalities))
                )
        );
    }

    public void addMovement(JSONObject entry) {
        if (!categories.containsKey((String) entry.get("category"))) {
            categories.put((String) entry.get("category"), new HashMap<>());
        }

        LocalDate date = LocalDate.parse((String) entry.get("date"));

        Map<LocalDate, Double> category = categories.get((String) entry.get("category"));

        category.put(date, sigmoid((Double) entry.get("value")));
    }

    static double sigmoid(double x) {
        return .4 * (1 + Math.exp(-.33 * x)) + .8;
    }

    public void calculateDaily() {
        for (LocalDate date : caseData.keySet()) {
            LocalDate previousDay = date.minusDays(1);
            // If there is data for the previous date, set the daily
            // values to current date - previous date.
            // Otherwise, daily value is equal to cumulative value
            if (caseData.containsKey(previousDay)) {
                caseData.get(date).setDailyCases(
                        caseData.get(date).getCases() - caseData.get(previousDay).getCases()
                );
                caseData.get(date).setDailyFatalities(
                        caseData.get(date).getFatalities() - caseData.get(previousDay).getFatalities()
                );
            } else {
                caseData.get(date).setDailyCases(
                        caseData.get(date).getCases()
                );
                caseData.get(date).setDailyFatalities(
                        caseData.get(date).getFatalities()
                );
            }
        }
    }

    public double getInfectionRate() {
        LocalDate startDate = null;

        for (LocalDate date : caseData.keySet()) {
            if (caseData.get(date).getCases() > 0) {
                startDate = date;
            }
        }

        if (startDate == null) {
            return 0;
        }

        double total = 0;

        for (int i = 10; i < 20; i++) {
            LocalDate date = startDate.plusDays(i);
            if (caseData.containsKey(date)) {
                total += caseData.get(date).getCases() / caseData.get(date.minusDays(1)).getCases();
            }
        }

        return total / 10;
    }
}

