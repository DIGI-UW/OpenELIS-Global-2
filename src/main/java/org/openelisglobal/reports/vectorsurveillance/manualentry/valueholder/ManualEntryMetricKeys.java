package org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder;

/**
 * Stable metric keys recognised by the Manual Entry Helper field map. These are
 * the eight default seed metrics (spec Assumptions / data-model A1). The field
 * map (FR-009) lets deployments reorder / hide / relabel them, but the key set
 * is the contract the view service derives values for.
 */
public final class ManualEntryMetricKeys {

    private ManualEntryMetricKeys() {
    }

    public static final String POOLS_TESTED = "POOLS_TESTED";
    public static final String POOLS_POSITIVE = "POOLS_POSITIVE";
    public static final String CONFIRMED_POSITIVE_ORGANISMS = "CONFIRMED_POSITIVE_ORGANISMS";
    public static final String TOP_SPECIES = "TOP_SPECIES";
    public static final String MIR_CLASSIC = "MIR_CLASSIC";
    public static final String MIR_OBSERVED = "MIR_OBSERVED";
    public static final String SPOROZOITE_RATE = "SPOROZOITE_RATE";
    public static final String SITES_WITH_POSITIVES = "SITES_WITH_POSITIVES";
}
