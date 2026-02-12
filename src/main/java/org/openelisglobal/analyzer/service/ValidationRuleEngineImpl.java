package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Service;

/**
 * ValidationRuleEngine implementation - Stateless validation engine
 * (NO @Transactional)
 * 
 */
@Service
public class ValidationRuleEngineImpl implements ValidationRuleEngine {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean evaluateRule(String value, ValidationRuleConfiguration rule) {
        if (value == null || rule == null || rule.getRuleType() == null) {
            return false;
        }

        try {
            switch (rule.getRuleType()) {
            case REGEX:
                return validateRegex(value, rule.getRuleExpression());
            case RANGE:
                return validateRangeFromExpression(value, rule.getRuleExpression());
            case ENUM:
                return validateEnumFromExpression(value, rule.getRuleExpression());
            case LENGTH:
                return validateLengthFromExpression(value, rule.getRuleExpression());
            default:
                throw new IllegalArgumentException("Unknown rule type: " + rule.getRuleType());
            }
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error evaluating validation rule: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateRegex(String value, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true; // No pattern means no validation
        }
        try {
            Pattern compiledPattern = Pattern.compile(pattern);
            return compiledPattern.matcher(value).matches();
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateRange(Number value, Number min, Number max) {
        if (value == null) {
            return false;
        }
        BigDecimal numValue = new BigDecimal(value.toString());
        BigDecimal minValue = min != null ? new BigDecimal(min.toString()) : null;
        BigDecimal maxValue = max != null ? new BigDecimal(max.toString()) : null;

        if (minValue != null && numValue.compareTo(minValue) < 0) {
            return false;
        }
        if (maxValue != null && numValue.compareTo(maxValue) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean validateEnum(String value, List<String> allowedValues) {
        if (value == null || allowedValues == null || allowedValues.isEmpty()) {
            return false;
        }
        return allowedValues.contains(value);
    }

    @Override
    public boolean validateLength(String value, Integer minLength, Integer maxLength) {
        if (value == null) {
            return false;
        }
        int length = value.length();
        if (minLength != null && length < minLength) {
            return false;
        }
        if (maxLength != null && length > maxLength) {
            return false;
        }
        return true;
    }

    private boolean validateRangeFromExpression(String value, String expression) {
        try {
            Map<String, Object> rangeMap = objectMapper.readValue(expression, new TypeReference<Map<String, Object>>() {
            });
            Number min = rangeMap.get("min") != null ? new BigDecimal(rangeMap.get("min").toString()) : null;
            Number max = rangeMap.get("max") != null ? new BigDecimal(rangeMap.get("max").toString()) : null;
            Number numValue = new BigDecimal(value);
            return validateRange(numValue, min, max);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid range expression: " + e.getMessage(), e);
        }
    }

    private boolean validateEnumFromExpression(String value, String expression) {
        try {
            List<String> allowedValues = objectMapper.readValue(expression, new TypeReference<List<String>>() {
            });
            return validateEnum(value, allowedValues);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid enum expression: " + e.getMessage(), e);
        }
    }

    private boolean validateLengthFromExpression(String value, String expression) {
        try {
            Map<String, Object> lengthMap = objectMapper.readValue(expression,
                    new TypeReference<Map<String, Object>>() {
                    });
            Integer minLength = lengthMap.get("minLength") != null
                    ? Integer.parseInt(lengthMap.get("minLength").toString())
                    : null;
            Integer maxLength = lengthMap.get("maxLength") != null
                    ? Integer.parseInt(lengthMap.get("maxLength").toString())
                    : null;
            return validateLength(value, minLength, maxLength);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid length expression: " + e.getMessage(), e);
        }
    }
}
