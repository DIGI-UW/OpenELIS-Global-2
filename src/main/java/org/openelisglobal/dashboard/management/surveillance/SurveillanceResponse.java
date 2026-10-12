package org.openelisglobal.dashboard.management.surveillance;

import java.util.List;

public record SurveillanceResponse(List<Program> programs) {

    // name is a message key for seeded programs, else plain text. missingCodes are
    // WHONET codes or organism groups that match no active organism or antibiotic.
    public record Program(String code, String name, int testCount, List<Metric> metrics, List<String> missingCodes) {
    }

    // value and delta are percentages, null when the metric has too few results.
    public record Metric(String key, String label, Double value, int numerator, int denominator, Double delta) {
    }
}
