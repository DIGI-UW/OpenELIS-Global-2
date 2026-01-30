package org.openelisglobal.notebook.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Traditional Medicine sample preparation.
 *
 * Per SRS Requirements - Sample Preparation for Analysis: - Physical
 * Processing: Grinding, chopping, drying, or powdering - Freshly processed vs
 * samples that need drying - Yield tracking (initial weight → final weight →
 * yield percentage) - Drying-specific parameters (temperature, duration,
 * method) - Quality control checks (moisture content validation) - Derived
 * material/aliquot creation when processed material is tracked separately
 *
 * This service handles: 1. Recording processing details (type, equipment,
 * technician) 2. Tracking weight/yield throughout processing 3. Managing
 * drying-specific parameters 4. Creating derived samples/aliquots for processed
 * material 5. Quality control validation (moisture content thresholds)
 */
public interface TraditionalMedicineSamplePreparationService {

    /**
     * Processing type options per SRS.
     */
    enum ProcessingType {
        GRINDING("GRINDING", "Grinding (to powder)"),
        CHOPPING("CHOPPING", "Chopping (coarse pieces)"),
        POWDERING("POWDERING", "Powdering (fine powder for extraction)"),
        FRESHLY_PROCESSED("FRESHLY_PROCESSED", "Freshly processed samples: Used immediately");

        private final String id;
        private final String label;

        ProcessingType(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static ProcessingType fromId(String id) {
            for (ProcessingType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return null;
        }

        /**
         * Check if this processing type is a drying method.
         * Note: Drying is now handled separately via DryingMethod enum.
         */
        public boolean isDryingMethod() {
            return false; // All drying methods are now in DryingMethod enum
        }
    }

    /**
     * Material state options (fresh vs dried input material).
     */
    enum MaterialState {
        FRESH("FRESH", "Fresh material (freshly collected)"), DRIED("DRIED", "Dried material (pre-dried)"),
        PRESERVED("PRESERVED", "Preserved material (stored)"),
        PARTIALLY_DRIED("PARTIALLY_DRIED", "Partially dried (in process)");

        private final String id;
        private final String label;

        MaterialState(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static MaterialState fromId(String id) {
            for (MaterialState state : values()) {
                if (state.id.equals(id)) {
                    return state;
                }
            }
            return null;
        }
    }

    /**
     * Drying method options for sample preparation.
     */
    enum DryingMethod {
        AIR_DRYING("AIR_DRYING", "Air drying"),
        OVEN_DRYING("OVEN_DRYING", "Oven drying (controlled temperature)"),
        FREEZE_DRYING("FREEZE_DRYING", "Freeze drying (for heat-sensitive materials)");

        private final String id;
        private final String label;

        DryingMethod(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static DryingMethod fromId(String id) {
            for (DryingMethod method : values()) {
                if (method.id.equals(id)) {
                    return method;
                }
            }
            return null;
        }
    }

    /**
     * Weight unit options.
     */
    enum WeightUnit {
        GRAMS("g", "Grams"), MILLIGRAMS("mg", "Milligrams"), KILOGRAMS("kg", "Kilograms");

        private final String id;
        private final String label;

        WeightUnit(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static WeightUnit fromId(String id) {
            for (WeightUnit unit : values()) {
                if (unit.id.equals(id)) {
                    return unit;
                }
            }
            return GRAMS; // default
        }
    }

    /**
     * Request object for sample preparation operation.
     */
    record PreparationRequest(List<Integer> sampleIds, List<String> processingTypes, String materialState,
            String processingDate, String processedBy, String equipment, String particleSize,
            // Weight tracking for yield calculation
            BigDecimal initialWeight, BigDecimal finalWeight, String weightUnit,
            // Drying-specific parameters
            BigDecimal dryingTemperature, String temperatureUnit, Integer dryingDurationHours, String dryingMethod,
            // Quality control
            BigDecimal moistureContent, BigDecimal targetMoistureContent, Boolean passedQC,
            // Derived material
            String derivedMaterialId, Boolean createAliquot, String aliquotNotes,
            // General notes
            String notes) {
    }

    /**
     * Result of preparation operation.
     */
    record PreparationResponse(boolean success, int updatedCount, int completedCount, int aliquotsCreated,
            BigDecimal averageYieldPercentage, String message, String error) {

        public static PreparationResponse success(int updatedCount, int completedCount, int aliquotsCreated,
                BigDecimal averageYield, String message) {
            return new PreparationResponse(true, updatedCount, completedCount, aliquotsCreated, averageYield, message,
                    null);
        }

        public static PreparationResponse error(String error) {
            return new PreparationResponse(false, 0, 0, 0, null, null, error);
        }
    }

    /**
     * Apply preparation processing to samples on a notebook page.
     *
     * @param pageId    the notebook page ID (Page 3 - Sample Preparation)
     * @param request   the preparation request with processing details
     * @param sysUserId the system user ID performing the preparation
     * @return preparation response with counts and status
     */
    PreparationResponse processSamples(Integer pageId, PreparationRequest request, String sysUserId);

    /**
     * Mark samples as preparation complete and ready for extraction. Only allows
     * completion if all required processing data is present.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs to mark complete
     * @param sysUserId the system user ID
     * @return preparation response with counts
     */
    PreparationResponse markPreparationComplete(Integer pageId, List<Integer> sampleIds, String sysUserId);

    /**
     * Get preparation status for samples on a page.
     *
     * @param pageId the notebook page ID
     * @return map of sample ID to preparation data
     */
    Map<Integer, Map<String, Object>> getPreparationStatus(Integer pageId);

    /**
     * Calculate yield percentage from initial and final weights.
     *
     * @param initialWeight the initial weight before processing
     * @param finalWeight   the final weight after processing
     * @return yield percentage (0-100), or null if cannot calculate
     */
    BigDecimal calculateYield(BigDecimal initialWeight, BigDecimal finalWeight);

    /**
     * Validate moisture content against target threshold.
     *
     * @param moistureContent       the measured moisture content (%)
     * @param targetMoistureContent the target/maximum allowed moisture (%)
     * @return true if within acceptable range
     */
    boolean validateMoistureContent(BigDecimal moistureContent, BigDecimal targetMoistureContent);

    /**
     * Validate preparation request data.
     *
     * @param request the preparation request
     * @return list of validation errors (empty if valid)
     */
    List<String> validatePreparationRequest(PreparationRequest request);

    /**
     * Get processing options for the frontend.
     *
     * @return map containing processingTypes, materialStates, and weightUnits
     */
    Map<String, Object> getProcessingOptions();
}
