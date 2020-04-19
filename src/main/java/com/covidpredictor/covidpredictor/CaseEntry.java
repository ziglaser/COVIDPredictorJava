package com.covidpredictor.covidpredictor;

import lombok.Getter;
import lombok.Setter;

/**
 * Stores case data for a single day
 */
@Getter
class CaseEntry {
    private final double cases;
    private final double fatalities;

    @Setter
    private double dailyCases;
    @Setter
    private double dailyFatalities;

    public CaseEntry(double cases, double fatalities) {
        this.cases = cases;
        this.fatalities = fatalities;
    }
}