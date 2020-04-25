package com.covidpredictor.covidpredictor;

import CaseData.Country;
import CaseData.DataGenerator;
import CaseData.Region;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Trainer {
    private Pool pool;

    private static LocalDate initialDate;

    private DataGenerator dataGenerator;

    private Country country;
    private Region region;

    private int poolSize;

    private String statistic;

    private LocalDate startTraining;

    private double[] testCase;


    public Trainer(DataGenerator dataGenerator, Country country, int poolSize, String statistic, LocalDate startTraining) {
        this.dataGenerator = dataGenerator;
        this.country = country;
        this.poolSize = poolSize;
        this.statistic = statistic;
        this.startTraining = startTraining;

        generateTestCase();
    }

    private void generateTestCase() {
        testCase = new double[7];

        LocalDate date = startTraining;

        if (region == null) {
            for (int i = 0; i < 7; i++) {
                testCase[i] = country.getCaseData().get(date.plusDays(i)).getCases();
            }
        } else {
            for (int i = 0; i < 7; i++) {
                testCase[i] = region.getCaseData().get(date.plusDays(i)).getCases();
            }
        }
    }

    public void train() {

    }

    private double rsmle(double[] predicted, double baseline) {
        double total = 0;

        double predictedCumulative = baseline;
        double actualCumulative = baseline;

        for (int i = 0; i < 7; i++) {
            predictedCumulative += predicted[i] < 0 ? 0 : predicted[i];
            actualCumulative += testCase[i] < 0 ? 0 : testCase[i];

            total += Math.pow(Math.log(predictedCumulative + 1) - Math.log(actualCumulative + 1), 2);
        }

        return Math.sqrt(total / 7);
    }

//    private void predict(Model model, LocalDate startDate) {
//        Region zone;
//
//        if (region == null) {
//            zone = region;
//        } else {
//            zone = country;
//        }
//
//        double previousCases = zone.getCaseData().get(startDate).getCases();
//
//        int lag = model.getLag();
//
//        double[] weekPrediction = new double[7];
//
//        for (int i = 0; i < 7; i++) {
//            Map<String, Map<LocalDate, Double>> mobilityData = new HashMap<>();
//            for (Map.Entry<String, Map<LocalDate, Double>> entry : zone.getCategories().entrySet()) {
//                Map<LocalDate, Double> data = new HashMap<>();
//                for (int j = i - lag - 4; j < i + 1; j++) {
//                    data.put(
//                            startDate.plusDays(j),
//                            entry.getValue().get(startDate.plusDays(j))
//                    );
//                }
//                mobilityData.put(entry.getKey(), data);
//            }
//            prediction = model.predict(previousCases, mobilityData, zone.getInfectionRate(), zone.getPopulation());
//        }
//    }

    public static void main(String[] args) {
        new Trainer(new DataGenerator(), null, 10, "", LocalDate.now());
    }
}
