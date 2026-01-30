package org.openelisglobal.notebook.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Traditional Medicine extraction, filtration, and
 * concentration.
 *
 * Per SRS Requirements - Extraction, Filtration & Concentration: - Extraction
 * Process: Use of solvents (ethanol, methanol, water) based on target compounds
 * - Techniques: maceration, Soxhlet, ultrasonic, distillation, etc. -
 * Filtration: Remove plant debris and impurities - Concentration: Evaporation
 * or distillation to reduce volume and enrich extract
 *
 * This service handles: 1. Recording extraction parameters (solvent, technique,
 * temperature, duration) 2. Tracking solvent ratios (material weight to solvent
 * volume) 3. Managing multiple extraction cycles 4. Filtration and
 * concentration documentation 5. Yield calculation (extract weight from
 * starting material)
 */
public interface TraditionalMedicineExtractionService {

    /**
     * Solvent options for extraction.
     */
    enum Solvent {
        ETHANOL("ETHANOL", "Ethanol"), METHANOL("METHANOL", "Methanol"), WATER("WATER", "Water (Aqueous)"),
        ETHANOL_WATER("ETHANOL_WATER", "Ethanol-Water Mixture"),
        METHANOL_WATER("METHANOL_WATER", "Methanol-Water Mixture"), HEXANE("HEXANE", "Hexane"),
        CHLOROFORM("CHLOROFORM", "Chloroform"), DICHLOROMETHANE("DICHLOROMETHANE", "Dichloromethane"),
        ETHYL_ACETATE("ETHYL_ACETATE", "Ethyl Acetate"), PETROLEUM_ETHER("PETROLEUM_ETHER", "Petroleum Ether"),
        ACETONE("ACETONE", "Acetone"), OTHER("OTHER", "Other");

        private final String id;
        private final String label;

        Solvent(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static Solvent fromId(String id) {
            for (Solvent s : values()) {
                if (s.id.equals(id))
                    return s;
            }
            return null;
        }
    }

    /**
     * Extraction technique options.
     */
    enum ExtractionTechnique {
        MACERATION("MACERATION", "Maceration"), SOXHLET("SOXHLET", "Soxhlet Extraction"),
        ULTRASONIC("ULTRASONIC", "Ultrasonic-Assisted Extraction"),
        MICROWAVE("MICROWAVE", "Microwave-Assisted Extraction"), DISTILLATION("DISTILLATION", "Distillation"),
        STEAM_DISTILLATION("STEAM_DISTILLATION", "Steam Distillation"),
        HYDRODISTILLATION("HYDRODISTILLATION", "Hydrodistillation"), PERCOLATION("PERCOLATION", "Percolation"),
        REFLUX("REFLUX", "Reflux Extraction"), COLD_PRESSING("COLD_PRESSING", "Cold Pressing"),
        SUPERCRITICAL("SUPERCRITICAL", "Supercritical CO2 Extraction"), OTHER("OTHER", "Other");

        private final String id;
        private final String label;

        ExtractionTechnique(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static ExtractionTechnique fromId(String id) {
            for (ExtractionTechnique t : values()) {
                if (t.id.equals(id))
                    return t;
            }
            return null;
        }
    }

    /**
     * Filtration method options.
     */
    enum FiltrationMethod {
        VACUUM("VACUUM", "Vacuum Filtration"), GRAVITY("GRAVITY", "Gravity Filtration"),
        MEMBRANE("MEMBRANE", "Membrane Filtration"), CENTRIFUGATION("CENTRIFUGATION", "Centrifugation"),
        FILTER_PAPER("FILTER_PAPER", "Filter Paper"), BUCHNER("BUCHNER", "Buchner Funnel"),
        SINTERED_GLASS("SINTERED_GLASS", "Sintered Glass"), NONE("NONE", "None / Not Applicable");

        private final String id;
        private final String label;

        FiltrationMethod(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static FiltrationMethod fromId(String id) {
            for (FiltrationMethod m : values()) {
                if (m.id.equals(id))
                    return m;
            }
            return null;
        }
    }

    /**
     * Concentration method options.
     */
    enum ConcentrationMethod {
        ROTARY_EVAPORATOR("ROTARY_EVAPORATOR", "Rotary Evaporator"),
        VACUUM_EVAPORATOR("VACUUM_EVAPORATOR", "Vacuum Evaporator"), DISTILLATION("DISTILLATION", "Distillation"),
        FREEZE_DRYING("FREEZE_DRYING", "Freeze Drying (Lyophilization)"), SPRAY_DRYING("SPRAY_DRYING", "Spray Drying"),
        NITROGEN_EVAP("NITROGEN_EVAP", "Nitrogen Evaporation"), AIR_DRYING("AIR_DRYING", "Air Drying"),
        NONE("NONE", "None / Not Concentrated");

        private final String id;
        private final String label;

        ConcentrationMethod(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public static ConcentrationMethod fromId(String id) {
            for (ConcentrationMethod m : values()) {
                if (m.id.equals(id))
                    return m;
            }
            return null;
        }
    }

    /**
     * Request object for extraction operation.
     */
    record ExtractionRequest(List<Integer> sampleIds,
            // Extraction parameters
            String solvent, String otherSolvent, String solventConcentration, String extractionTechnique,
            String otherTechnique, String extractionDate, String operator,
            // Solvent ratio tracking
            BigDecimal materialWeight, String materialWeightUnit, BigDecimal solventVolume, String solventVolumeUnit,
            // Extraction conditions
            BigDecimal extractionTemperature, String temperatureUnit, Integer extractionDurationMinutes,
            Integer numberOfCycles,
            // Filtration
            String filtrationMethod, String filterPoreSize, Boolean debrisRemoved,
            // Concentration
            String concentrationMethod, BigDecimal concentrationTemperature, BigDecimal finalVolume,
            String finalVolumeUnit,
            // Extract output
            String extractId, BigDecimal extractWeight, String extractWeightUnit, String extractAppearance,
            String extractColor,
            // Notes
            String notes) {
    }

    /**
     * Result of extraction operation.
     */
    record ExtractionResponse(boolean success, int updatedCount, int completedCount, BigDecimal averageYieldPercentage,
            String message, String error) {

        public static ExtractionResponse success(int updatedCount, int completedCount, BigDecimal avgYield,
                String message) {
            return new ExtractionResponse(true, updatedCount, completedCount, avgYield, message, null);
        }

        public static ExtractionResponse error(String error) {
            return new ExtractionResponse(false, 0, 0, null, null, error);
        }
    }

    /**
     * Apply extraction processing to samples on a notebook page.
     *
     * @param pageId    the notebook page ID (Page 4 - Extraction)
     * @param request   the extraction request with parameters
     * @param sysUserId the system user ID performing the extraction
     * @return extraction response with counts and status
     */
    ExtractionResponse extractSamples(Integer pageId, ExtractionRequest request, String sysUserId);

    /**
     * Mark samples as extraction complete and ready for next step.
     *
     * @param pageId    the notebook page ID
     * @param sampleIds list of sample IDs to mark complete
     * @param sysUserId the system user ID
     * @return extraction response with counts
     */
    ExtractionResponse markExtractionComplete(Integer pageId, List<Integer> sampleIds, String sysUserId);

    /**
     * Get extraction status for samples on a page.
     *
     * @param pageId the notebook page ID
     * @return map of sample ID to extraction data
     */
    Map<Integer, Map<String, Object>> getExtractionStatus(Integer pageId);

    /**
     * Calculate extraction yield percentage. Yield = (extract weight / material
     * weight) × 100
     *
     * @param materialWeight the starting material weight
     * @param extractWeight  the final extract weight
     * @return yield percentage (0-100), or null if cannot calculate
     */
    BigDecimal calculateYield(BigDecimal materialWeight, BigDecimal extractWeight);

    /**
     * Calculate solvent ratio (material:solvent).
     *
     * @param materialWeight in grams
     * @param solventVolume  in mL
     * @return ratio string like "1:10" or null
     */
    String calculateSolventRatio(BigDecimal materialWeight, BigDecimal solventVolume);

    /**
     * Validate extraction request data.
     *
     * @param request the extraction request
     * @return list of validation errors (empty if valid)
     */
    List<String> validateExtractionRequest(ExtractionRequest request);

    /**
     * Get extraction options for the frontend.
     *
     * @return map containing solvents, techniques, filtration methods,
     *         concentration methods
     */
    Map<String, Object> getExtractionOptions();
}
