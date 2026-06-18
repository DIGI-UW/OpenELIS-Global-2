package org.openelisglobal.reports.vectorsurveillance.valueholder;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Computed surveillance indices for a scope (date range + optional site). Fully
 * compiled in the service transaction (Constitution IV) and returned as JSON.
 * All figures derive from OpenELIS's own recorded data — no external system
 * (FR-011).
 */
public class SurveillanceIndicesDTO {

    private Timestamp freshness;
    private List<DensityRow> collectionDensity = new ArrayList<>();
    private List<SpeciesRow> speciesDistribution = new ArrayList<>();
    private List<MirRow> mirBySpecies = new ArrayList<>();
    private List<PositivityRow> pathogenPositivity = new ArrayList<>();
    private QcPassRate qcPassRate = new QcPassRate();
    /** Anopheles sporozoite rate (MIR-style %), null when not computable. */
    private Double sporozoiteRatePct;
    /**
     * False when vector results exist but none carry a catalog significance
     * classification — the frontend then shows "positivity not configured" on the
     * positivity-dependent panels rather than misleading zeros.
     */
    private boolean positivityConfigured = true;
    /** Distinct sampling sites with at least one positive pool in scope. */
    private long sitesWithPositives;

    public Timestamp getFreshness() {
        return freshness;
    }

    public void setFreshness(Timestamp freshness) {
        this.freshness = freshness;
    }

    public List<DensityRow> getCollectionDensity() {
        return collectionDensity;
    }

    public void setCollectionDensity(List<DensityRow> collectionDensity) {
        this.collectionDensity = collectionDensity;
    }

    public List<SpeciesRow> getSpeciesDistribution() {
        return speciesDistribution;
    }

    public void setSpeciesDistribution(List<SpeciesRow> speciesDistribution) {
        this.speciesDistribution = speciesDistribution;
    }

    public List<MirRow> getMirBySpecies() {
        return mirBySpecies;
    }

    public void setMirBySpecies(List<MirRow> mirBySpecies) {
        this.mirBySpecies = mirBySpecies;
    }

    public List<PositivityRow> getPathogenPositivity() {
        return pathogenPositivity;
    }

    public void setPathogenPositivity(List<PositivityRow> pathogenPositivity) {
        this.pathogenPositivity = pathogenPositivity;
    }

    public QcPassRate getQcPassRate() {
        return qcPassRate;
    }

    public void setQcPassRate(QcPassRate qcPassRate) {
        this.qcPassRate = qcPassRate;
    }

    public Double getSporozoiteRatePct() {
        return sporozoiteRatePct;
    }

    public void setSporozoiteRatePct(Double sporozoiteRatePct) {
        this.sporozoiteRatePct = sporozoiteRatePct;
    }

    public boolean isPositivityConfigured() {
        return positivityConfigured;
    }

    public void setPositivityConfigured(boolean positivityConfigured) {
        this.positivityConfigured = positivityConfigured;
    }

    public long getSitesWithPositives() {
        return sitesWithPositives;
    }

    public void setSitesWithPositives(long sitesWithPositives) {
        this.sitesWithPositives = sitesWithPositives;
    }

    public static class DensityRow {
        private String periodLabel;
        private Integer siteId;
        private String siteName;
        private long poolCount;
        private long specimenCount;

        public DensityRow() {
        }

        public DensityRow(String periodLabel, Integer siteId, String siteName, long poolCount, long specimenCount) {
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

    public static class SpeciesRow {
        private Integer speciesId;
        private String genus;
        private String species;
        private long specimenCount;
        private double pct;

        public SpeciesRow() {
        }

        public SpeciesRow(Integer speciesId, String genus, String species, long specimenCount, double pct) {
            this.speciesId = speciesId;
            this.genus = genus;
            this.species = species;
            this.specimenCount = specimenCount;
            this.pct = pct;
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

        public double getPct() {
            return pct;
        }
    }

    /**
     * MIR per species × pathogen. The sporozoite rate is a top-level figure
     * (Anopheles only), not a per-row column; {@code positiveResolutionPct} drives
     * its gating downstream.
     */
    public static class MirRow {
        private Integer speciesId;
        private String speciesLabel;
        private String pathogen;
        private double mirClassic;
        private double infectionRateObserved;
        private double positiveResolutionPct;
        private long positivePools;
        private long totalSpecimens;

        public MirRow() {
        }

        public Integer getSpeciesId() {
            return speciesId;
        }

        public void setSpeciesId(Integer speciesId) {
            this.speciesId = speciesId;
        }

        public String getSpeciesLabel() {
            return speciesLabel;
        }

        public void setSpeciesLabel(String speciesLabel) {
            this.speciesLabel = speciesLabel;
        }

        public String getPathogen() {
            return pathogen;
        }

        public void setPathogen(String pathogen) {
            this.pathogen = pathogen;
        }

        public double getMirClassic() {
            return mirClassic;
        }

        public void setMirClassic(double mirClassic) {
            this.mirClassic = mirClassic;
        }

        public double getInfectionRateObserved() {
            return infectionRateObserved;
        }

        public void setInfectionRateObserved(double infectionRateObserved) {
            this.infectionRateObserved = infectionRateObserved;
        }

        public double getPositiveResolutionPct() {
            return positiveResolutionPct;
        }

        public void setPositiveResolutionPct(double positiveResolutionPct) {
            this.positiveResolutionPct = positiveResolutionPct;
        }

        public long getPositivePools() {
            return positivePools;
        }

        public void setPositivePools(long positivePools) {
            this.positivePools = positivePools;
        }

        public long getTotalSpecimens() {
            return totalSpecimens;
        }

        public void setTotalSpecimens(long totalSpecimens) {
            this.totalSpecimens = totalSpecimens;
        }
    }

    public static class PositivityRow {
        private String pathogen;
        private long poolsPositive;
        private long poolsTested;
        private double positivityPct;

        public PositivityRow() {
        }

        public PositivityRow(String pathogen, long poolsPositive, long poolsTested, double positivityPct) {
            this.pathogen = pathogen;
            this.poolsPositive = poolsPositive;
            this.poolsTested = poolsTested;
            this.positivityPct = positivityPct;
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

        public double getPositivityPct() {
            return positivityPct;
        }
    }

    public static class QcPassRate {
        private long analysesPassed;
        private long analysesTotal;
        private double passRatePct;

        public QcPassRate() {
        }

        public QcPassRate(long analysesPassed, long analysesTotal, double passRatePct) {
            this.analysesPassed = analysesPassed;
            this.analysesTotal = analysesTotal;
            this.passRatePct = passRatePct;
        }

        public long getAnalysesPassed() {
            return analysesPassed;
        }

        public long getAnalysesTotal() {
            return analysesTotal;
        }

        public double getPassRatePct() {
            return passRatePct;
        }
    }
}
