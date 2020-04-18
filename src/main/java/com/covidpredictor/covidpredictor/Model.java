package com.covidpredictor.covidpredictor;

import java.util.Random;

public class Model {
    static final double immunityVariability = 0.05;
    static final int mobilityLagVariability = 1;
    static final double mobilityConstantVariability = 0.1;
    static final double fatalityVariability = 0.01;

    double[] mobilityConstants;
    double immunityConstant;
    double fatalityRatio;
    double score = 100.0;

    int lag;


    /**
     * @param mobilityConstants initial mobility constants
     * @param immunityConstant initial immunity constant
     * @param fatalityRatio initial fatality ratio
     * @param lag initial lag
     */
    public Model(double[] mobilityConstants, double immunityConstant, double fatalityRatio, int lag) {
        this.mobilityConstants = mobilityConstants;
        this.immunityConstant = immunityConstant;
        this.fatalityRatio = fatalityRatio;
        this.lag = lag;
    }

    /**
     * Generates a random model, used to seed a pool
     */
    public Model(){
        Random r = new Random();

        mobilityConstants = new double[5];
        for (int i = 0; i < 5; i ++) {
            mobilityConstants[i] = 2 * (r.nextDouble() - 0.5);
        }
        immunityConstant = r.nextDouble();
        lag = 16;
        fatalityRatio = r.nextDouble() / 2;
    }

    /**
     * Varies the values of the model based on the variability constants
     */
    public void mutate(){
        Random random = new Random();
        immunityConstant *= random.nextGaussian() * immunityVariability + 1;
        fatalityRatio *= random.nextGaussian() * fatalityVariability + 1;
        lag += random.nextInt(2) - 1;
        if (lag > 7) {
            lag = 7;
        } else if (lag < 7) {
            lag = 0;
        }
    }

    /**
     * Generates a prediction based on the current situation of a region
     *
     * @param current current case count
     * @param mobility double array of mobility data based on the model's lag
     * @param infectionRate the rate of case growth
     * @param population the population of the region
     * @return a double of the predicted number of cases for the next day
     */
    public double predict(float current, double[] mobility, double infectionRate, float population){
        double mobilityFactor = 0;

        for (int i = 0; i < mobilityConstants.length; i++) {
            mobilityFactor += mobilityConstants[i] * mobility[i];
        }

        double immunityFactor = 1 - immunityConstant * (current / population);

        return current * infectionRate * mobilityFactor * immunityFactor;
    }
}