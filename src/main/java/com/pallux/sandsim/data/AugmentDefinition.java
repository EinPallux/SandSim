package com.pallux.sandsim.data;

/**
 * Immutable definition of a single Augment tier.
 * All values are loaded from augments.yml via AugmentManager.
 * Multipliers are percentage values (e.g. 2.0 = +2%).
 */
public class AugmentDefinition {

    private final int    tier;
    private final double sandPercent;
    private final double gemsPercent;
    private final double sandbucksPercent;
    private final long   researchTimeSeconds;
    private final long   gemCost;

    public AugmentDefinition(int tier,
                             double sandPercent,
                             double gemsPercent,
                             double sandbucksPercent,
                             long researchTimeSeconds,
                             long gemCost) {
        this.tier                = tier;
        this.sandPercent         = sandPercent;
        this.gemsPercent         = gemsPercent;
        this.sandbucksPercent    = sandbucksPercent;
        this.researchTimeSeconds = researchTimeSeconds;
        this.gemCost             = gemCost;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int    getTier()                { return tier; }
    public double getSandPercent()         { return sandPercent; }
    public double getGemsPercent()         { return gemsPercent; }
    public double getSandbucksPercent()    { return sandbucksPercent; }
    public long   getResearchTimeSeconds() { return researchTimeSeconds; }
    public long   getGemCost()            { return gemCost; }

    /** Display name, e.g. "Augment III" */
    public String getDisplayName() {
        return "Augment " + toRoman(tier);
    }

    /** Converts an integer to Roman numerals. Works for any positive integer. */
    public static String toRoman(int n) {
        if (n <= 0) return String.valueOf(n);
        int[]    vals = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
        String[] syms = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            while (n >= vals[i]) { sb.append(syms[i]); n -= vals[i]; }
        }
        return sb.toString();
    }
}