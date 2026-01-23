package org.openelisglobal.notebook.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Basic implementation of Westgard Rules Engine for immediate lab use.
 *
 * This is a simplified but functional implementation that covers essential
 * Westgard rules for QC validation in bioanalytical testing.
 */
@Service
public class WestgardRulesServiceImpl implements WestgardRulesService {

    // Default enabled Westgard rules for this lab
    private static final Map<String, Boolean> DEFAULT_ENABLED_RULES = Map.of("1:2s", true, // Warning rule
            "1:3s", true, // Rejection rule
            "2:2s", true, // Rejection rule
            "R:4s", true, // Rejection rule
            "4:1s", true, // Rejection rule
            "10:x", true // Rejection rule
    );

    @Override
    public WestgardEvaluation evaluateWestgardRules(List<QCControl> controls) {
        if (controls == null || controls.isEmpty()) {
            return new WestgardEvaluation(List.of(), "INVALID", "No QC controls to evaluate", 0, 0, 0);
        }

        List<WestgardRuleResult> ruleResults = new ArrayList<>();
        Map<String, Boolean> enabledRules = getEnabledWestgardRules();

        // Evaluate each Westgard rule
        if (enabledRules.getOrDefault("1:2s", false)) {
            ruleResults.add(evaluate1_2s(controls));
        }
        if (enabledRules.getOrDefault("1:3s", false)) {
            ruleResults.add(evaluate1_3s(controls));
        }
        if (enabledRules.getOrDefault("2:2s", false)) {
            ruleResults.add(evaluate2_2s(controls));
        }
        if (enabledRules.getOrDefault("R:4s", false)) {
            ruleResults.add(evaluateR_4s(controls));
        }
        if (enabledRules.getOrDefault("4:1s", false)) {
            ruleResults.add(evaluate4_1s(controls));
        }
        if (enabledRules.getOrDefault("10:x", false)) {
            ruleResults.add(evaluate10_x(controls));
        }

        // Calculate overall status
        long failedRules = ruleResults.stream().filter(result -> "FAIL".equals(result.status())).count();

        long warningRules = ruleResults.stream().filter(result -> "WARNING".equals(result.status())).count();

        String overallStatus;
        String recommendation;

        if (failedRules > 0) {
            overallStatus = "FAIL";
            recommendation = "QC run failed Westgard rules. Review and repeat analysis.";
        } else if (warningRules > 0) {
            overallStatus = "WARNING";
            recommendation = "QC run has warnings. Review control values and consider repeating.";
        } else {
            overallStatus = "PASS";
            recommendation = "QC run passed all Westgard rules. Analysis can proceed.";
        }

        return new WestgardEvaluation(ruleResults, overallStatus, recommendation, controls.size(),
                (int) ruleResults.stream().filter(r -> "PASS".equals(r.status())).count(), (int) failedRules);
    }

    @Override
    public List<QCControl> extractQCControlsFromResults(List<Map<String, Object>> qcResults) {
        if (qcResults == null || qcResults.isEmpty()) {
            return List.of();
        }

        List<QCControl> controls = new ArrayList<>();

        // Calculate basic statistics from the QC results
        List<Double> values = qcResults.stream().map(qc -> {
            Object accuracy = qc.get("accuracy");
            if (accuracy instanceof Number) {
                return ((Number) accuracy).doubleValue();
            }
            return null;
        }).filter(val -> val != null && val > 0).toList();

        if (values.isEmpty()) {
            return List.of();
        }

        // Simple statistics calculation (in production, use established control limits)
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(100.0);
        double variance = values.stream().mapToDouble(val -> Math.pow(val - mean, 2)).average().orElse(4.0); // Default
                                                                                                             // variance
        double standardDeviation = Math.sqrt(variance);

        // Create QC controls from results
        for (int i = 0; i < values.size(); i++) {
            controls.add(new QCControl(values.get(i), mean, standardDeviation, "Level" + (i % 3 + 1) // Simple level
                                                                                                     // assignment
            ));
        }

        return controls;
    }

    @Override
    public Map<String, Double> calculateControlStatistics(List<Double> controlValues) {
        if (controlValues == null || controlValues.isEmpty()) {
            return Map.of("mean", 100.0, "standardDeviation", 2.0);
        }

        double mean = controlValues.stream().mapToDouble(Double::doubleValue).average().orElse(100.0);
        double variance = controlValues.stream().mapToDouble(val -> Math.pow(val - mean, 2)).average().orElse(4.0);
        double standardDeviation = Math.sqrt(variance);

        return Map.of("mean", mean, "standardDeviation", standardDeviation);
    }

    @Override
    public Map<String, Boolean> getEnabledWestgardRules() {
        return new HashMap<>(DEFAULT_ENABLED_RULES);
    }

    @Override
    public boolean isQCRunValid(List<Map<String, Object>> qcResults) {
        List<QCControl> controls = extractQCControlsFromResults(qcResults);
        if (controls.isEmpty()) {
            return false;
        }

        WestgardEvaluation evaluation = evaluateWestgardRules(controls);
        return "PASS".equals(evaluation.overallStatus());
    }

    // Individual Westgard rule implementations

    /**
     * 1:2s rule - Warning when 1 control exceeds ±2SD
     */
    private WestgardRuleResult evaluate1_2s(List<QCControl> controls) {
        List<Integer> violations = new ArrayList<>();

        for (int i = 0; i < controls.size(); i++) {
            QCControl control = controls.get(i);
            double deviation = Math.abs(control.value() - control.mean()) / control.standardDeviation();

            if (deviation > 2.0) {
                violations.add(i);
            }
        }

        String status = violations.isEmpty() ? "PASS" : "WARNING";
        String message = violations.isEmpty() ? "All controls within ±2SD"
                : String.format("%d control(s) exceeded ±2SD", violations.size());

        return new WestgardRuleResult("1:2s", "Single control outside ±2SD", status, message, violations);
    }

    /**
     * 1:3s rule - Rejection when 1 control exceeds ±3SD
     */
    private WestgardRuleResult evaluate1_3s(List<QCControl> controls) {
        List<Integer> violations = new ArrayList<>();

        for (int i = 0; i < controls.size(); i++) {
            QCControl control = controls.get(i);
            double deviation = Math.abs(control.value() - control.mean()) / control.standardDeviation();

            if (deviation > 3.0) {
                violations.add(i);
            }
        }

        String status = violations.isEmpty() ? "PASS" : "FAIL";
        String message = violations.isEmpty() ? "All controls within ±3SD"
                : String.format("%d control(s) exceeded ±3SD", violations.size());

        return new WestgardRuleResult("1:3s", "Single control outside ±3SD", status, message, violations);
    }

    /**
     * 2:2s rule - Rejection when 2 consecutive controls exceed same ±2SD
     */
    private WestgardRuleResult evaluate2_2s(List<QCControl> controls) {
        List<Integer> violations = new ArrayList<>();

        for (int i = 0; i < controls.size() - 1; i++) {
            QCControl control1 = controls.get(i);
            QCControl control2 = controls.get(i + 1);

            double dev1 = (control1.value() - control1.mean()) / control1.standardDeviation();
            double dev2 = (control2.value() - control2.mean()) / control2.standardDeviation();

            // Check if both exceed 2SD on the same side
            if ((dev1 > 2.0 && dev2 > 2.0) || (dev1 < -2.0 && dev2 < -2.0)) {
                violations.add(i);
                violations.add(i + 1);
            }
        }

        String status = violations.isEmpty() ? "PASS" : "FAIL";
        String message = violations.isEmpty() ? "No consecutive controls exceeded ±2SD"
                : "Consecutive controls exceeded ±2SD on same side";

        return new WestgardRuleResult("2:2s", "Two consecutive controls outside same ±2SD", status, message,
                violations);
    }

    /**
     * R:4s rule - Rejection when range of controls > 4SD
     */
    private WestgardRuleResult evaluateR_4s(List<QCControl> controls) {
        if (controls.size() < 2) {
            return new WestgardRuleResult("R:4s", "Range of controls > 4SD", "PASS",
                    "Insufficient controls for range check", List.of());
        }

        List<Integer> violations = new ArrayList<>();

        for (int i = 0; i < controls.size() - 1; i++) {
            QCControl control1 = controls.get(i);
            QCControl control2 = controls.get(i + 1);

            double range = Math.abs(control1.value() - control2.value());
            double maxSD = Math.max(control1.standardDeviation(), control2.standardDeviation());

            if (range > 4.0 * maxSD) {
                violations.add(i);
                violations.add(i + 1);
            }
        }

        String status = violations.isEmpty() ? "PASS" : "FAIL";
        String message = violations.isEmpty() ? "Control range within 4SD" : "Control range exceeded 4SD";

        return new WestgardRuleResult("R:4s", "Range of controls > 4SD", status, message, violations);
    }

    /**
     * 4:1s rule - Rejection when 4 consecutive controls exceed same ±1SD
     */
    private WestgardRuleResult evaluate4_1s(List<QCControl> controls) {
        if (controls.size() < 4) {
            return new WestgardRuleResult("4:1s", "Four consecutive controls outside same ±1SD", "PASS",
                    "Insufficient controls", List.of());
        }

        List<Integer> violations = new ArrayList<>();

        for (int i = 0; i <= controls.size() - 4; i++) {
            boolean allPositive = true;
            boolean allNegative = true;

            for (int j = i; j < i + 4; j++) {
                QCControl control = controls.get(j);
                double deviation = (control.value() - control.mean()) / control.standardDeviation();

                if (deviation <= 1.0) {
                    allPositive = false;
                }
                if (deviation >= -1.0) {
                    allNegative = false;
                }
            }

            if (allPositive || allNegative) {
                for (int j = i; j < i + 4; j++) {
                    violations.add(j);
                }
            }
        }

        String status = violations.isEmpty() ? "PASS" : "FAIL";
        String message = violations.isEmpty() ? "No 4 consecutive controls exceeded ±1SD"
                : "Four consecutive controls exceeded ±1SD on same side";

        return new WestgardRuleResult("4:1s", "Four consecutive controls outside same ±1SD", status, message,
                violations);
    }

    /**
     * 10:x rule - Rejection when 10 consecutive controls on same side of mean
     */
    private WestgardRuleResult evaluate10_x(List<QCControl> controls) {
        if (controls.size() < 10) {
            return new WestgardRuleResult("10:x", "Ten consecutive controls on same side", "PASS",
                    "Insufficient controls", List.of());
        }

        List<Integer> violations = new ArrayList<>();

        for (int i = 0; i <= controls.size() - 10; i++) {
            boolean allAbove = true;
            boolean allBelow = true;

            for (int j = i; j < i + 10; j++) {
                QCControl control = controls.get(j);

                if (control.value() <= control.mean()) {
                    allAbove = false;
                }
                if (control.value() >= control.mean()) {
                    allBelow = false;
                }
            }

            if (allAbove || allBelow) {
                for (int j = i; j < i + 10; j++) {
                    violations.add(j);
                }
            }
        }

        String status = violations.isEmpty() ? "PASS" : "FAIL";
        String message = violations.isEmpty() ? "No 10 consecutive controls on same side"
                : "Ten consecutive controls on same side of mean";

        return new WestgardRuleResult("10:x", "Ten consecutive controls on same side", status, message, violations);
    }
}