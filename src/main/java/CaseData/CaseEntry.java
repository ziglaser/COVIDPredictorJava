package CaseData;

import lombok.Data;

/**
 * Stores case data for a single day
 */
@Data
public class CaseEntry {
    private final double cases;
    private final double fatalities;

    private double dailyCases;
    private double dailyFatalities;

    public CaseEntry(double cases, double fatalities) {
        this.cases = cases;
        this.fatalities = fatalities;
    }
}