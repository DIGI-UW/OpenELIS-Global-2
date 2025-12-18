package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Traditional Medicine Testing (Product Development &
 * Testing).
 *
 * Per SRS Requirements - Product Development and Testing: - Preliminary
 * Phytochemical Screening: Test for alkaloids, flavonoids, etc. -
 * Safety/Toxicity Study: In vitro or animal models, link to external trial data
 * - Efficacy Test: Biological activity assays, document outcomes and protocols
 *
 * After testing, approved samples proceed to Formulation.
 */
public interface TraditionalMedicineTestingService {

    /**
     * Phytochemical compound types for screening.
     */
    enum PhytochemicalType {
        ALKALOIDS("ALKALOIDS", "Alkaloids"), FLAVONOIDS("FLAVONOIDS", "Flavonoids"), TANNINS("TANNINS", "Tannins"),
        SAPONINS("SAPONINS", "Saponins"), GLYCOSIDES("GLYCOSIDES", "Glycosides"),
        TERPENOIDS("TERPENOIDS", "Terpenoids"), STEROIDS("STEROIDS", "Steroids"), PHENOLS("PHENOLS", "Phenols"),
        COUMARINS("COUMARINS", "Coumarins"), ANTHRAQUINONES("ANTHRAQUINONES", "Anthraquinones"),
        CARDIAC_GLYCOSIDES("CARDIAC_GLYCOSIDES", "Cardiac Glycosides"), PHLOBATANNINS("PHLOBATANNINS", "Phlobatannins");

        private final String id;
        private final String label;

        PhytochemicalType(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static PhytochemicalType fromId(String id) {
            for (PhytochemicalType t : values()) {
                if (t.id.equals(id))
                    return t;
            }
            return null;
        }
    }

    /**
     * Safety study types.
     */
    enum SafetyStudyType {
        IN_VITRO("IN_VITRO", "In Vitro (Cell-based)"), IN_VIVO_ACUTE("IN_VIVO_ACUTE", "In Vivo - Acute Toxicity"),
        IN_VIVO_SUBACUTE("IN_VIVO_SUBACUTE", "In Vivo - Subacute Toxicity"),
        IN_VIVO_CHRONIC("IN_VIVO_CHRONIC", "In Vivo - Chronic Toxicity"),
        GENOTOXICITY("GENOTOXICITY", "Genotoxicity Study"), CYTOTOXICITY("CYTOTOXICITY", "Cytotoxicity Assay"),
        MUTAGENICITY("MUTAGENICITY", "Mutagenicity Test"),
        DERMAL_IRRITATION("DERMAL_IRRITATION", "Dermal Irritation Test"),
        OCULAR_IRRITATION("OCULAR_IRRITATION", "Ocular Irritation Test"), OTHER("OTHER", "Other");

        private final String id;
        private final String label;

        SafetyStudyType(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static SafetyStudyType fromId(String id) {
            for (SafetyStudyType t : values()) {
                if (t.id.equals(id))
                    return t;
            }
            return null;
        }
    }

    /**
     * Toxicity models/systems.
     */
    enum ToxicityModel {
        CELL_LINE("CELL_LINE", "Cell Line"), RODENT_MOUSE("RODENT_MOUSE", "Rodent (Mouse)"),
        RODENT_RAT("RODENT_RAT", "Rodent (Rat)"), ZEBRAFISH("ZEBRAFISH", "Zebrafish"),
        BRINE_SHRIMP("BRINE_SHRIMP", "Brine Shrimp"), DAPHNIA("DAPHNIA", "Daphnia"), RABBIT("RABBIT", "Rabbit"),
        OTHER("OTHER", "Other");

        private final String id;
        private final String label;

        ToxicityModel(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static ToxicityModel fromId(String id) {
            for (ToxicityModel m : values()) {
                if (m.id.equals(id))
                    return m;
            }
            return null;
        }
    }

    /**
     * Toxicity outcome categories.
     */
    enum ToxicityOutcome {
        SAFE("SAFE", "Safe - No toxicity observed"), LOW_TOXICITY("LOW_TOXICITY", "Low Toxicity"),
        MODERATE_TOXICITY("MODERATE_TOXICITY", "Moderate Toxicity"), HIGH_TOXICITY("HIGH_TOXICITY", "High Toxicity"),
        INCONCLUSIVE("INCONCLUSIVE", "Inconclusive - Further testing needed");

        private final String id;
        private final String label;

        ToxicityOutcome(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static ToxicityOutcome fromId(String id) {
            for (ToxicityOutcome o : values()) {
                if (o.id.equals(id))
                    return o;
            }
            return null;
        }
    }

    /**
     * Biological assay types for efficacy testing.
     */
    enum BiologicalAssayType {
        ANTIMICROBIAL("ANTIMICROBIAL", "Antimicrobial Activity"), ANTIOXIDANT("ANTIOXIDANT", "Antioxidant Activity"),
        ANTI_INFLAMMATORY("ANTI_INFLAMMATORY", "Anti-inflammatory Activity"),
        ANTIDIABETIC("ANTIDIABETIC", "Antidiabetic Activity"),
        ANTICANCER("ANTICANCER", "Anticancer/Cytotoxic Activity"), ANALGESIC("ANALGESIC", "Analgesic Activity"),
        HEPATOPROTECTIVE("HEPATOPROTECTIVE", "Hepatoprotective Activity"),
        IMMUNOMODULATORY("IMMUNOMODULATORY", "Immunomodulatory Activity"),
        WOUND_HEALING("WOUND_HEALING", "Wound Healing Activity"), ANTIVIRAL("ANTIVIRAL", "Antiviral Activity"),
        ANTIFUNGAL("ANTIFUNGAL", "Antifungal Activity"), ANTIPARASITIC("ANTIPARASITIC", "Antiparasitic Activity"),
        CARDIOPROTECTIVE("CARDIOPROTECTIVE", "Cardioprotective Activity"),
        NEUROPROTECTIVE("NEUROPROTECTIVE", "Neuroprotective Activity"), OTHER("OTHER", "Other");

        private final String id;
        private final String label;

        BiologicalAssayType(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static BiologicalAssayType fromId(String id) {
            for (BiologicalAssayType t : values()) {
                if (t.id.equals(id))
                    return t;
            }
            return null;
        }
    }

    /**
     * Efficacy outcome categories.
     */
    enum EfficacyOutcome {
        HIGHLY_EFFECTIVE("HIGHLY_EFFECTIVE", "Highly Effective"),
        MODERATELY_EFFECTIVE("MODERATELY_EFFECTIVE", "Moderately Effective"),
        WEAKLY_EFFECTIVE("WEAKLY_EFFECTIVE", "Weakly Effective"), NOT_EFFECTIVE("NOT_EFFECTIVE", "Not Effective"),
        INCONCLUSIVE("INCONCLUSIVE", "Inconclusive");

        private final String id;
        private final String label;

        EfficacyOutcome(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static EfficacyOutcome fromId(String id) {
            for (EfficacyOutcome o : values()) {
                if (o.id.equals(id))
                    return o;
            }
            return null;
        }
    }

    /**
     * Request object for testing operations.
     */
    record TestingRequest(List<Integer> sampleIds,
            // Phytochemical screening (list of detected compounds)
            List<String> detectedPhytochemicals, String phytochemicalScreeningNotes,
            // Safety/Toxicity
            String safetyStudyType, String toxicityModel, String toxicityOutcome, String ld50Value,
            String safetyStudyReference,
            // Efficacy
            String biologicalAssayType, String otherAssayType, String efficacyOutcome, String assayProtocol,
            String ic50Value, String efficacyStudyReference,
            // General
            String testedBy, String testDate, String notes) {
    }

    /**
     * Response object for testing operations.
     */
    record TestingResponse(boolean success, int updatedCount, int approvedCount, int rejectedCount, String message,
            String error) {

        public static TestingResponse success(int updatedCount, int approvedCount, int rejectedCount, String message) {
            return new TestingResponse(true, updatedCount, approvedCount, rejectedCount, message, null);
        }

        public static TestingResponse error(String error) {
            return new TestingResponse(false, 0, 0, 0, null, error);
        }
    }

    /**
     * Apply testing data to samples on a notebook page.
     *
     * @param pageId    the notebook page ID (Testing page)
     * @param request   the testing request with parameters
     * @param sysUserId the system user ID performing the testing
     * @return testing response with counts and status
     */
    TestingResponse applyTestingData(Integer pageId, TestingRequest request, String sysUserId);

    /**
     * Approve samples for formulation after passing safety and efficacy tests.
     * Advances samples to the next page (Formulation).
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs to approve
     * @param sysUserId the system user ID
     * @return testing response with counts
     */
    TestingResponse approveSamplesForFormulation(Integer pageId, List<Integer> sampleIds, String sysUserId);

    /**
     * Reject samples that failed safety or efficacy testing. These samples will not
     * proceed to formulation.
     *
     * @param pageId       the notebook page ID
     * @param sampleIds    list of sample IDs to reject
     * @param rejectReason the reason for rejection
     * @param sysUserId    the system user ID
     * @return testing response with counts
     */
    TestingResponse rejectSamples(Integer pageId, List<Integer> sampleIds, String rejectReason, String sysUserId);

    /**
     * Mark samples for additional testing.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs requiring more testing
     * @param sysUserId the system user ID
     * @return testing response with counts
     */
    TestingResponse markForFurtherTesting(Integer pageId, List<Integer> sampleIds, String sysUserId);

    /**
     * Get testing status for samples on a page.
     *
     * @param pageId the notebook page ID
     * @return map of sample ID to testing data
     */
    Map<Integer, Map<String, Object>> getTestingStatus(Integer pageId);

    /**
     * Validate testing request data.
     *
     * @param request the testing request
     * @return list of validation errors (empty if valid)
     */
    List<String> validateTestingRequest(TestingRequest request);

    /**
     * Get testing options for the frontend.
     *
     * @return map containing phytochemical types, safety study types, toxicity
     *         models, etc.
     */
    Map<String, Object> getTestingOptions();
}
