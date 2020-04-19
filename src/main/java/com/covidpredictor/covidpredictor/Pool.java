package com.covidpredictor.covidpredictor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Pool{
    int size;

    List<Model> pool;

    public Pool(int size){
        if (size % 2 == 0) {
            this.size = size;
        } else {
            this.size = size + 1;
        }

        pool = new ArrayList<>() {{
            for (int i = 0; i < size; i++) {
                add(new Model());
            }
        }};
    }

    /**
     * Creates the next generation.
     *
     * Eliminates all models over 2 standard deviations below the mean
     */
    private void nextGeneration(){
        pool.sort(Comparator.comparing(Model::getScore));

        // Eliminate low outliers
        double stdev = standardDeviation();
        double mean = mean();
        double cutoff = mean - stdev * 2;

        List<Model> breedingPool = new ArrayList<>();

        for (Model model : pool) {
            if (model.getScore() > cutoff) {
                breedingPool.add(model);
            }
        }

        pool = new ArrayList<>();

        // Selects the top model, and a model in the top 10.
        // Breeds them twice, then removes both from the breeding pool
        Random random = new Random();
        while (breedingPool.size() > 1) {
            int randChoice = random.nextInt(10) + 1;

            if (randChoice > breedingPool.size() - 1) {
                randChoice = breedingPool.size() - 1;
            }

            pool.add(new Model(breedingPool.get(0), breedingPool.get(randChoice)));
            pool.add(new Model(breedingPool.get(0), breedingPool.get(randChoice)));

            breedingPool.remove(0);
            breedingPool.remove(randChoice);
        }

        // If there is a remaining model, mutates it and adds it to the pool
        if (breedingPool.size() == 1) {
            breedingPool.get(0).mutate();
            pool.add(breedingPool.get(0));
        }
    }

    private double mean() {
        double sum = 0;

        for (Model model : pool) {
            sum += model.getScore();
        }

        return sum / size;
    }

    private double standardDeviation() {
        double meanDiffs = 0;

        double mean = mean();

        for (Model model : pool) {
            meanDiffs += model.getScore() - mean;
        }

        return Math.sqrt(meanDiffs / (size - 1));
    }
}



