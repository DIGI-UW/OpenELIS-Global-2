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

    /**
     * Pools + specimen counts per site per ISO-week period (collection density).
     */
    public static class DensityAggregate {
        private String periodLabel;
        private Integer siteId;
        private String siteName;
        private long poolCount;
        private long specimenCount;

        public DensityAggregate(String periodLabel, Integer siteId, String siteName, long poolCount,
                long specimenCount) {
            this.periodLabel = periodLabel;
            this.siteId = siteId;
            this.siteName = siteName;
            this.poolCount = poolCount;
            this.specimenCount = specimenCount;
        }

        public String getPeriodLabel() {
            return periodLabel;
        }

        public Integer getSiteId() {
            return siteId;
        }

        public String getSiteName() {
            return siteName;
        }

        public long getPoolCount() {
            return poolCount;
        }

        public long getSpecimenCount() {
            return specimenCount;
        }
    }

    /** Specimen count per identified species (CONFIRMED), for distribution. */
    public static class SpeciesAggregate {
        private Integer speciesId;
        private String genus;
        private String species;
        private long specimenCount;

        public SpeciesAggregate(Integer speciesId, String genus, String species, long specimenCount) {
            this.speciesId = speciesId;
            this.genus = genus;
            this.species = species;
            this.specimenCount = specimenCount;
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

        public long getSpecimenCount() {
            return specimenCount;
        }
    }

    /** Positive vs tested pools per pathogen. */
    public static class PositivityAggregate {
        private String pathogen;
        private long poolsPositive;
        private long poolsTested;

        public PositivityAggregate(String pathogen, long poolsPositive, long poolsTested) {
            this.pathogen = pathogen;
            this.poolsPositive = poolsPositive;
            this.poolsTested = poolsTested;
        }

        public String getPathogen() {
            return pathogen;
        }

        public long getPoolsPositive() {
            return poolsPositive;
        }

        public long getPoolsTested() {
            return poolsTested;
        }
    }

    /**
     * Sporozoite-rate inputs: Anopheles pools positive for the
     * sporozoite/Plasmodium assay (CSP-ELISA) vs total Anopheles specimens tested.
     * Rate is computed MIR-style by the service.
     */
    public static class SporozoiteAggregate {
        private final long positivePools;
        private final long totalSpecimens;

        public SporozoiteAggregate(long positivePools, long totalSpecimens) {
            this.positivePools = positivePools;
            this.totalSpecimens = totalSpecimens;
        }

        public long getPositivePools() {
            return positivePools;
        }

        public long getTotalSpecimens() {
            return totalSpecimens;
        }
    }

    /**
     * QC pass counts (analyses with no failing QA event vs total surveillance
     * analyses).
     */
    public static class QcAggregate {
        private long analysesPassed;
        private long analysesTotal;

        public QcAggregate(long analysesPassed, long analysesTotal) {
            this.analysesPassed = analysesPassed;
            this.analysesTotal = analysesTotal;
        }

        public long getAnalysesPassed() {
            return analysesPassed;
        }

        public long getAnalysesTotal() {
            return analysesTotal;
        }
    }
}
