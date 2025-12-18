package org.openelisglobal.notebook.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Traditional Medicine Analytical Pathway.
 *
 * Per SRS Requirements - Advanced Analysis (Optional): - Fractionation:
 * Separate compounds, record method and fractions - Identification/Isolation:
 * Detect active constituents, link to compound database - Purification: Remove
 * impurities, log purity level - Characterization: Determine structure and
 * properties (NMR, MS, IR spectral data)
 *
 * This pathway is OPTIONAL - samples can skip directly to Testing/Formulation
 * if the extract is ready post-concentration.
 */
public interface TraditionalMedicineAnalyticalService {

    /**
     * Fractionation methods for compound separation.
     */
    enum FractionationMethod {
        COLUMN_CHROMATOGRAPHY("COLUMN_CHROMATOGRAPHY", "Column Chromatography"),
        TLC("TLC", "Thin Layer Chromatography (TLC)"), HPLC("HPLC", "High Performance Liquid Chromatography (HPLC)"),
        FLASH_CHROMATOGRAPHY("FLASH_CHROMATOGRAPHY", "Flash Chromatography"),
        LIQUID_LIQUID_EXTRACTION("LIQUID_LIQUID_EXTRACTION", "Liquid-Liquid Extraction"),
        SOLID_PHASE_EXTRACTION("SOLID_PHASE_EXTRACTION", "Solid Phase Extraction (SPE)"),
        GEL_FILTRATION("GEL_FILTRATION", "Gel Filtration"), ION_EXCHANGE("ION_EXCHANGE", "Ion Exchange Chromatography"),
        SIZE_EXCLUSION("SIZE_EXCLUSION", "Size Exclusion Chromatography"),
        PREPARATIVE_TLC("PREPARATIVE_TLC", "Preparative TLC"), OTHER("OTHER", "Other");

        private final String id;
        private final String label;

        FractionationMethod(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static FractionationMethod fromId(String id) {
            for (FractionationMethod m : values()) {
                if (m.id.equals(id))
                    return m;
            }
            return null;
        }
    }

    /**
     * Purification methods.
     */
    enum PurificationMethod {
        RECRYSTALLIZATION("RECRYSTALLIZATION", "Recrystallization"),
        PREPARATIVE_HPLC("PREPARATIVE_HPLC", "Preparative HPLC"), PREPARATIVE_TLC("PREPARATIVE_TLC", "Preparative TLC"),
        COLUMN_CHROMATOGRAPHY("COLUMN_CHROMATOGRAPHY", "Column Chromatography"),
        SUBLIMATION("SUBLIMATION", "Sublimation"), DISTILLATION("DISTILLATION", "Distillation"),
        PRECIPITATION("PRECIPITATION", "Precipitation"), DIALYSIS("DIALYSIS", "Dialysis"),
        ULTRAFILTRATION("ULTRAFILTRATION", "Ultrafiltration"), OTHER("OTHER", "Other");

        private final String id;
        private final String label;

        PurificationMethod(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static PurificationMethod fromId(String id) {
            for (PurificationMethod m : values()) {
                if (m.id.equals(id))
                    return m;
            }
            return null;
        }
    }

    /**
     * Structural characterization techniques.
     */
    enum CharacterizationTechnique {
        NMR("NMR", "Nuclear Magnetic Resonance (NMR)"), MS("MS", "Mass Spectrometry (MS)"),
        IR("IR", "Infrared Spectroscopy (IR)"), UV_VIS("UV_VIS", "UV-Visible Spectroscopy"),
        XRAY("XRAY", "X-Ray Crystallography"), ELEMENTAL_ANALYSIS("ELEMENTAL_ANALYSIS", "Elemental Analysis"),
        MELTING_POINT("MELTING_POINT", "Melting Point Determination"),
        OPTICAL_ROTATION("OPTICAL_ROTATION", "Optical Rotation");

        private final String id;
        private final String label;

        CharacterizationTechnique(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static CharacterizationTechnique fromId(String id) {
            for (CharacterizationTechnique t : values()) {
                if (t.id.equals(id))
                    return t;
            }
            return null;
        }
    }

    /**
     * Request object for analytical pathway operations.
     */
    record AnalyticalRequest(List<Integer> sampleIds,
            // Fractionation
            String fractionationMethod, String otherFractionationMethod, Integer numberOfFractions, String fractionIds,
            String mobilePhaseSolvent, String stationaryPhase,
            // Identification/Isolation
            String activeConstituentsIdentified, String compoundDatabaseLinks, String isolatedCompoundId,
            BigDecimal isolatedCompoundWeight, String isolatedCompoundWeightUnit,
            // Purification
            String purificationMethod, String otherPurificationMethod, BigDecimal purityLevel,
            String purityAssessmentMethod,
            // Characterization
            List<String> characterizationTechniques, String spectralFileReference, String molecularFormula,
            BigDecimal molecularWeight, String structureDescription,
            // General
            String analyst, String analysisDate, String notes) {
    }

    /**
     * Response object for analytical operations.
     */
    record AnalyticalResponse(boolean success, int updatedCount, int completedCount, int skippedCount, String message,
            String error) {

        public static AnalyticalResponse success(int updatedCount, int completedCount, int skippedCount,
                String message) {
            return new AnalyticalResponse(true, updatedCount, completedCount, skippedCount, message, null);
        }

        public static AnalyticalResponse error(String error) {
            return new AnalyticalResponse(false, 0, 0, 0, null, error);
        }
    }

    /**
     * Apply analytical data to samples on a notebook page.
     *
     * @param pageId    the notebook page ID (Analytical page)
     * @param request   the analytical request with parameters
     * @param sysUserId the system user ID performing the analysis
     * @return analytical response with counts and status
     */
    AnalyticalResponse applyAnalyticalData(Integer pageId, AnalyticalRequest request, String sysUserId);

    /**
     * Mark samples as characterized and complete on this page. Advances samples to
     * the next page (Testing).
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs to mark complete
     * @param sysUserId the system user ID
     * @return analytical response with counts
     */
    AnalyticalResponse markAnalyticalComplete(Integer pageId, List<Integer> sampleIds, String sysUserId);

    /**
     * Skip the analytical pathway for samples. Advances samples directly to Testing
     * without analytical processing.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs to skip
     * @param sysUserId the system user ID
     * @return analytical response with counts
     */
    AnalyticalResponse skipAnalyticalPathway(Integer pageId, List<Integer> sampleIds, String sysUserId);

    /**
     * Get analytical status for samples on a page.
     *
     * @param pageId the notebook page ID
     * @return map of sample ID to analytical data
     */
    Map<Integer, Map<String, Object>> getAnalyticalStatus(Integer pageId);

    /**
     * Validate analytical request data.
     *
     * @param request the analytical request
     * @return list of validation errors (empty if valid)
     */
    List<String> validateAnalyticalRequest(AnalyticalRequest request);

    /**
     * Get analytical options for the frontend.
     *
     * @return map containing fractionation methods, purification methods,
     *         characterization techniques
     */
    Map<String, Object> getAnalyticalOptions();
}
