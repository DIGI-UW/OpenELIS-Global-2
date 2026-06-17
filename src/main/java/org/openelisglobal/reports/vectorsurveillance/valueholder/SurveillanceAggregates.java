package org.openelisglobal.reports.vectorsurveillance.valueholder;

/**
 * Raw aggregation rows returned by {@code VectorSurveillanceDAO} — the
 * un-computed counts the service turns into surveillance indices. Kept separate
 * from the response DTO so the DAO stays a pure data-retrieval layer
 * (Constitution IV) and the index math lives in the service (and is
 * unit-testable in isolation).
 */
public final class SurveillanceAggregates {

    private SurveillanceAggregates() {
    }

    /**
     * Per species × pathogen, the raw counts needed for MIR. {@code positivePools}
     * = distinct pools with >=1 CONFIRMED positive result; {@code totalSpecimens} =
     * sum of member specimen quantities across the tested pools;
     * {@code observedPositiveOrganisms} = exact descendant positive count where the
     * pool's deconvolution is COMPLETE (classical 1-per-positive-pool fallback
     * otherwise); resolution = completelyResolvedPositivePools /
     * totalPositivePools.
     */
    public static class SpeciesMirAggregate {
        private Integer speciesId;
        private String genus;
        private String species;
        private String pathogen;
        private long positivePools;
        private long totalSpecimens;
        private long observedPositiveOrganisms;
        private long totalPositivePools;
        private long completelyResolvedPositivePools;

        public SpeciesMirAggregate() {
        }

        public SpeciesMirAggregate(Integer speciesId, String genus, String species, String pathogen, long positivePools,
                long totalSpecimens, long observedPositiveOrganisms, long totalPositivePools,
                long completelyResolvedPositivePools) {
            this.speciesId = speciesId;
            this.genus = genus;
            this.species = species;
            this.pathogen = pathogen;
            this.positivePools = positivePools;
            this.totalSpecimens = totalSpecimens;
            this.observedPositiveOrganisms = observedPositiveOrganisms;
            this.totalPositivePools = totalPositivePools;
            this.completelyResolvedPositivePools = completelyResolvedPositivePools;
        }

        public Integer getSpeciesId() {
            return speciesId;
        }

        public String getGenus() {
            return genus;
        }

        public String getSpecies() {
            return species;
        }

        public String getPathogen() {
            return pathogen;
        }

        public long getPositivePools() {
            return positivePools;
        }

        public long getTotalSpecimens() {
            return totalSpecimens;
        }

        public long getObservedPositiveOrganisms() {
            return observedPositiveOrganisms;
        }

        public long getTotalPositivePools() {
            return totalPositivePools;
        }

        public long getCompletelyResolvedPositivePools() {
            return completelyResolvedPositivePools;
        }
    }
}
