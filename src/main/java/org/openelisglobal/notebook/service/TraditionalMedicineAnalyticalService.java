package org.openelisglobal.notebook.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Traditional Medicine Analytical Pathways (Page 6).
 *
 * Per SRS Requirements - STAGE 6: Analytical Pathways
 *
 * Branching Workflow:
 * PATH A: Advanced Analysis (Mandatory all 4 steps)
 *   - Fractionation: Separate extract using chromatography
 *   - Identification/Isolation: Detect active constituents
 *   - Purification: Remove impurities, assess purity
 *   - Characterization: Determine structure (NMR, MS, IR)
 * PATH B: Direct to Production (skips this page)
 *
 * Pathway is selected at end of Page 5 and LOCKED (cannot be changed).
 */
public interface TraditionalMedicineAnalyticalService {

    /**
     * Analytical pathway options - selected at end of Page 5.
     */
    enum AnalyticalPathway {
        PATH_A("path_a", "Advanced Analysis (Fractionation, Identification, Purification, Characterization)"),
        PATH_B("path_b", "Direct to Production (Skip Advanced Analysis)");

        private final String id;
        private final String label;

        AnalyticalPathway(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static AnalyticalPathway fromId(String id) {
            for (AnalyticalPathway pathway : values()) {
                if (pathway.id.equals(id)) {
                    return pathway;
                }
            }
            return null;
        }
    }

    /**
     * Fractionation - Chromatography methods.
     */
    enum ChromatographyMethod {
        COLUMN_CHROMATOGRAPHY("column_chromatography", "Column Chromatography"),
        HPLC_PREP("hplc_prep", "HPLC Prep"),
        OTHER("other", "Other");

        private final String id;
        private final String label;

        ChromatographyMethod(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static ChromatographyMethod fromId(String id) {
            for (ChromatographyMethod method : values()) {
                if (method.id.equals(id)) {
                    return method;
                }
            }
            return null;
        }
    }

    /**
     * Identification/Isolation - Detection methods.
     */
    enum DetectionMethod {
        TLC("tlc", "TLC (Thin Layer Chromatography)"),
        HPLC("hplc", "HPLC (High Performance Liquid Chromatography)"),
        GC_MS("gc_ms", "GC-MS (Gas Chromatography-Mass Spectrometry)"),
        LC_MS("lc_ms", "LC-MS (Liquid Chromatography-Mass Spectrometry)"),
        OTHER("other", "Other");

        private final String id;
        private final String label;

        DetectionMethod(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static DetectionMethod fromId(String id) {
            for (DetectionMethod method : values()) {
                if (method.id.equals(id)) {
                    return method;
                }
            }
            return null;
        }
    }

    /**
     * Purification - Methods to remove impurities.
     */
    enum PurificationMethod {
        RECRYSTALLIZATION("recrystallization", "Recrystallization"),
        PREP_HPLC("prep_hplc", "Prep-HPLC"),
        COLUMN_CHROMATOGRAPHY("column_chromatography", "Column Chromatography"),
        OTHER("other", "Other");

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
            for (PurificationMethod method : values()) {
                if (method.id.equals(id)) {
                    return method;
                }
            }
            return null;
        }
    }

    /**
     * Purity assessment method.
     */
    enum PurityAssessmentMethod {
        HPLC("hplc", "HPLC"),
        NMR("nmr", "NMR"),
        OTHER("other", "Other");

        private final String id;
        private final String label;

        PurityAssessmentMethod(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static PurityAssessmentMethod fromId(String id) {
            for (PurityAssessmentMethod method : values()) {
                if (method.id.equals(id)) {
                    return method;
                }
            }
            return null;
        }
    }

    /**
     * Characterization - Spectroscopy techniques.
     */
    enum SpectroscopyTechnique {
        NMR("nmr", "NMR (Nuclear Magnetic Resonance)"),
        MASS_SPECTROMETRY("mass_spectrometry", "Mass Spectrometry (MS)"),
        IR_FTIR("ir_ftir", "IR/FTIR (Infrared Spectroscopy)"),
        OTHER("other", "Other");

        private final String id;
        private final String label;

        SpectroscopyTechnique(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static SpectroscopyTechnique fromId(String id) {
            for (SpectroscopyTechnique technique : values()) {
                if (technique.id.equals(id)) {
                    return technique;
                }
            }
            return null;
        }
    }

    /**
     * Pathway selection request - performed at end of Page 5.
     */
    record PathwaySelectionRequest(List<Integer> sampleIds, String analyticalPathway) {
    }

    /**
     * Fractionation request for PAGE 6.
     */
    record FractionationRequest(List<Integer> sampleIds, String chromatographyMethod,
            Integer numberOfFractions, String fractionLabels, String fractionDescription,
            String fractionationNotes) {
    }

    /**
     * Identification request for PAGE 6.
     */
    record IdentificationRequest(List<Integer> sampleIds, String detectionMethod,
            String activeConstituentsFound, String knownCompoundsIdentified,
            String identificationNotes) {
    }

    /**
     * Purification request for PAGE 6.
     */
    record PurificationRequest(List<Integer> sampleIds, String purificationMethod,
            BigDecimal purityLevel, String purityAssessmentMethod, String purificationNotes) {
    }

    /**
     * Characterization request for PAGE 6.
     */
    record CharacterizationRequest(List<Integer> sampleIds, List<String> spectroscopyTechniques,
            String structureDetermination, String propertiesIdentified,
            String characterizationNotes) {
    }

    /**
     * Response for analytical operations.
     */
    record AnalyticalResponse(boolean success, int updatedCount, String message, String error) {

        public static AnalyticalResponse success(int updatedCount, String message) {
            return new AnalyticalResponse(true, updatedCount, message, null);
        }

        public static AnalyticalResponse error(String error) {
            return new AnalyticalResponse(false, 0, null, error);
        }
    }

    /**
     * Select analysis pathway (PATH A or PATH B) at end of Page 5.
     * Once selected, pathway is LOCKED and cannot be changed.
     *
     * @param pageId    the notebook page ID (Page 5 - Extraction)
     * @param request   the pathway selection request
     * @param sysUserId the system user ID
     * @return analytical response with status
     */
    AnalyticalResponse selectPathway(Integer pageId, PathwaySelectionRequest request, String sysUserId);

    /**
     * Record fractionation - MANDATORY STEP 1 for PATH A on PAGE 6.
     *
     * @param pageId    the notebook page ID (Page 6 - Analytical Pathways)
     * @param request   the fractionation request
     * @param sysUserId the system user ID
     * @return analytical response with status
     */
    AnalyticalResponse recordFractionation(Integer pageId, FractionationRequest request, String sysUserId);

    /**
     * Record identification/isolation - MANDATORY STEP 2 for PATH A on PAGE 6.
     *
     * @param pageId    the notebook page ID (Page 6 - Analytical Pathways)
     * @param request   the identification request
     * @param sysUserId the system user ID
     * @return analytical response with status
     */
    AnalyticalResponse recordIdentification(Integer pageId, IdentificationRequest request, String sysUserId);

    /**
     * Record purification - MANDATORY STEP 3 for PATH A on PAGE 6.
     *
     * @param pageId    the notebook page ID (Page 6 - Analytical Pathways)
     * @param request   the purification request
     * @param sysUserId the system user ID
     * @return analytical response with status
     */
    AnalyticalResponse recordPurification(Integer pageId, PurificationRequest request, String sysUserId);

    /**
     * Record characterization - MANDATORY STEP 4 for PATH A on PAGE 6.
     *
     * @param pageId    the notebook page ID (Page 6 - Analytical Pathways)
     * @param request   the characterization request
     * @param sysUserId the system user ID
     * @return analytical response with status
     */
    AnalyticalResponse recordCharacterization(Integer pageId, CharacterizationRequest request, String sysUserId);

    /**
     * Get analytical status for samples on a page.
     *
     * @param pageId the notebook page ID
     * @return map of sample ID to analytical data
     */
    Map<Integer, Map<String, Object>> getAnalyticalStatus(Integer pageId);

    /**
     * Validate that all 4 mandatory steps are complete for PATH A samples.
     * Used before marking samples as complete and advancing to next page.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs to check
     * @return list of validation errors (empty if all steps complete)
     */
    List<String> validateAllStepsComplete(Integer pageId, List<Integer> sampleIds);

    /**
     * Mark samples as analysis complete and ready for next stage (Page 7).
     * For PATH A: All 4 steps must be complete before marking.
     * For PATH B: Samples skip this page entirely.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs to mark complete
     * @param sysUserId the system user ID
     * @return analytical response with status
     */
    AnalyticalResponse markAnalysisComplete(Integer pageId, List<Integer> sampleIds, String sysUserId);
}
