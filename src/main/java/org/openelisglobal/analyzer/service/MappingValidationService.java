package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;

/**
 * Service interface for mapping validation operations
 * 
 * 
 * Provides validation metrics and analysis for analyzer field mappings
 */
public interface MappingValidationService {

    /**
     * Calculate mapping accuracy based on historical test executions
     * 
     * @param analyzerId The analyzer ID
     * @return Percentage of successfully mapped test messages (0.0-1.0)
     */
    double calculateMappingAccuracy(String analyzerId);

    /**
     * Identify analyzer fields without active mappings
     * 
     * @param analyzerId The analyzer ID
     * @return List of field names that are not mapped
     */
    List<String> identifyUnmappedFields(String analyzerId);

    /**
     * Validate type compatibility for all mappings
     * 
     * @param mappings The mappings to validate
     * @return List of warning messages for incompatible types
     */
    List<String> validateTypeCompatibility(List<AnalyzerFieldMapping> mappings);

    /**
     * Generate coverage report by test unit
     * 
     * @param analyzerId The analyzer ID
     * @return Map of test unit names to coverage percentage (0.0-1.0)
     */
    Map<String, Double> generateCoverageReport(String analyzerId);

    /**
     * Get comprehensive validation metrics for an analyzer
     * 
     * @param analyzerId The analyzer ID
     * @return ValidationMetrics containing all validation data
     */
    ValidationMetrics getValidationMetrics(String analyzerId);

    /**
     * Validation metrics data structure
     */
    class ValidationMetrics {
        private double accuracy;
        private int unmappedCount;
        private List<String> unmappedFields;
        private List<String> warnings;
        private Map<String, Double> coverageByTestUnit;

        public ValidationMetrics() {
            this.accuracy = 0.0;
            this.unmappedCount = 0;
            this.unmappedFields = new java.util.ArrayList<>();
            this.warnings = new java.util.ArrayList<>();
            this.coverageByTestUnit = new java.util.HashMap<>();
        }

        public double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(double accuracy) {
            this.accuracy = accuracy;
        }

        public int getUnmappedCount() {
            return unmappedCount;
        }

        public void setUnmappedCount(int unmappedCount) {
            this.unmappedCount = unmappedCount;
        }

        public List<String> getUnmappedFields() {
            return unmappedFields;
        }

        public void setUnmappedFields(List<String> unmappedFields) {
            this.unmappedFields = unmappedFields;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }

        public Map<String, Double> getCoverageByTestUnit() {
            return coverageByTestUnit;
        }

        public void setCoverageByTestUnit(Map<String, Double> coverageByTestUnit) {
            this.coverageByTestUnit = coverageByTestUnit;
        }
    }
}
