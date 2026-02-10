package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WestgardRuleEngine {

    public static final String RULE_12S = "12s";
    public static final String RULE_13S = "13s";
    public static final String RULE_22S = "22s";
    public static final String RULE_R4S = "R4s";
    public static final String RULE_41S = "41s";
    public static final String RULE_10X = "10x";

    public static final List<String> ALL_RULE_CODES = List.of(RULE_12S, RULE_13S, RULE_22S, RULE_R4S, RULE_41S,
            RULE_10X);

    public List<String> evaluate(List<BigDecimal> values, BigDecimal mean, BigDecimal sd, List<String> enabledRules) {
        List<String> violations = new ArrayList<>();

        if (values == null || values.isEmpty() || mean == null || sd == null || sd.compareTo(BigDecimal.ZERO) == 0) {
            return violations;
        }

        List<BigDecimal> zScores = new ArrayList<>();
        for (BigDecimal value : values) {
            zScores.add(value.subtract(mean).divide(sd, 10, java.math.RoundingMode.HALF_UP));
        }

        if (enabledRules.contains(RULE_12S) && check12s(zScores)) {
            violations.add(RULE_12S);
        }
        if (enabledRules.contains(RULE_13S) && check13s(zScores)) {
            violations.add(RULE_13S);
        }
        if (enabledRules.contains(RULE_22S) && check22s(zScores)) {
            violations.add(RULE_22S);
        }
        if (enabledRules.contains(RULE_R4S) && checkR4s(zScores)) {
            violations.add(RULE_R4S);
        }
        if (enabledRules.contains(RULE_41S) && check41s(zScores)) {
            violations.add(RULE_41S);
        }
        if (enabledRules.contains(RULE_10X) && check10x(zScores)) {
            violations.add(RULE_10X);
        }

        return violations;
    }

    /**
     * 1-2s: Warning rule. Last value exceeds mean +/- 2SD.
     */
    boolean check12s(List<BigDecimal> zScores) {
        if (zScores.isEmpty())
            return false;
        BigDecimal last = zScores.get(zScores.size() - 1);
        return last.abs().compareTo(new BigDecimal("2")) > 0;
    }

    /**
     * 1-3s: Rejection rule. Last value exceeds mean +/- 3SD.
     */
    boolean check13s(List<BigDecimal> zScores) {
        if (zScores.isEmpty())
            return false;
        BigDecimal last = zScores.get(zScores.size() - 1);
        return last.abs().compareTo(new BigDecimal("3")) > 0;
    }

    /**
     * 2-2s: Rejection rule. Two consecutive values exceed the same mean +/- 2SD.
     */
    boolean check22s(List<BigDecimal> zScores) {
        if (zScores.size() < 2)
            return false;
        BigDecimal threshold = new BigDecimal("2");
        for (int i = zScores.size() - 1; i >= 1; i--) {
            BigDecimal curr = zScores.get(i);
            BigDecimal prev = zScores.get(i - 1);
            if (curr.abs().compareTo(threshold) > 0 && prev.abs().compareTo(threshold) > 0
                    && curr.signum() == prev.signum()) {
                return true;
            }
        }
        return false;
    }

    /**
     * R-4s: Rejection rule. The range between two consecutive values exceeds 4SD.
     */
    boolean checkR4s(List<BigDecimal> zScores) {
        if (zScores.size() < 2)
            return false;
        BigDecimal threshold = new BigDecimal("4");
        for (int i = zScores.size() - 1; i >= 1; i--) {
            BigDecimal range = zScores.get(i).subtract(zScores.get(i - 1)).abs();
            if (range.compareTo(threshold) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 4-1s: Rejection rule. Four consecutive values exceed the same mean +/- 1SD.
     */
    boolean check41s(List<BigDecimal> zScores) {
        if (zScores.size() < 4)
            return false;
        BigDecimal threshold = new BigDecimal("1");
        int n = zScores.size();
        for (int i = n - 4; i >= 0; i--) {
            boolean allSameSide = true;
            int sign = 0;
            for (int j = i; j < i + 4; j++) {
                if (zScores.get(j).abs().compareTo(threshold) <= 0) {
                    allSameSide = false;
                    break;
                }
                if (sign == 0) {
                    sign = zScores.get(j).signum();
                } else if (zScores.get(j).signum() != sign) {
                    allSameSide = false;
                    break;
                }
            }
            if (allSameSide && sign != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 10x: Rejection rule. Ten consecutive values fall on the same side of the
     * mean.
     */
    boolean check10x(List<BigDecimal> zScores) {
        if (zScores.size() < 10)
            return false;
        int n = zScores.size();
        for (int i = n - 10; i >= 0; i--) {
            boolean allSameSide = true;
            int sign = 0;
            for (int j = i; j < i + 10; j++) {
                int s = zScores.get(j).signum();
                if (s == 0) {
                    allSameSide = false;
                    break;
                }
                if (sign == 0) {
                    sign = s;
                } else if (s != sign) {
                    allSameSide = false;
                    break;
                }
            }
            if (allSameSide && sign != 0) {
                return true;
            }
        }
        return false;
    }

    public Map<String, String> getRuleDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put(RULE_12S, "Warning: One control exceeds mean ± 2SD");
        descriptions.put(RULE_13S, "Rejection: One control exceeds mean ± 3SD");
        descriptions.put(RULE_22S, "Rejection: Two consecutive controls exceed same mean ± 2SD");
        descriptions.put(RULE_R4S, "Rejection: Range between two consecutive controls exceeds 4SD");
        descriptions.put(RULE_41S, "Rejection: Four consecutive controls exceed same mean ± 1SD");
        descriptions.put(RULE_10X, "Rejection: Ten consecutive controls on same side of mean");
        return descriptions;
    }
}
