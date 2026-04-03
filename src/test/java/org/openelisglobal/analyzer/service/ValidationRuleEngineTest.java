package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;

/**
 * Unit tests for ValidationRuleEngine implementation
 * 
 * References: - Testing Roadmap: .specify/guides/testing-roadmap.md - Template:
 * JUnit 4 Service Test
 * 
 * TDD Workflow (MANDATORY for complex logic): - RED: Write failing test first
 * (defines expected behavior) - GREEN: Write minimal code to make test pass -
 * REFACTOR: Improve code quality while keeping tests green
 * 
 * 
 * Test Naming: test{MethodName}_{Scenario}_{ExpectedResult}
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidationRuleEngineTest {

    private ValidationRuleEngine validationRuleEngine;

    private CustomFieldType testCustomFieldType;
    private ValidationRuleConfiguration regexRule;
    private ValidationRuleConfiguration rangeRule;
    private ValidationRuleConfiguration enumRule;
    private ValidationRuleConfiguration lengthRule;

    @Before
    public void setUp() {
        validationRuleEngine = new ValidationRuleEngineImpl();

        // Setup test custom field type
        testCustomFieldType = new CustomFieldType();
        testCustomFieldType.setId("CFT-001");
        testCustomFieldType.setTypeName("TestType");
        testCustomFieldType.setDisplayName("Test Type");

        // Setup regex rule
        regexRule = new ValidationRuleConfiguration();
        regexRule.setId("RULE-001");
        regexRule.setCustomFieldType(testCustomFieldType);
        regexRule.setRuleName("Email Pattern");
        regexRule.setRuleType(ValidationRuleConfiguration.RuleType.REGEX);
        regexRule.setRuleExpression("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        regexRule.setErrorMessage("Invalid email format");
        regexRule.setIsActive(true);

        // Setup range rule
        rangeRule = new ValidationRuleConfiguration();
        rangeRule.setId("RULE-002");
        rangeRule.setCustomFieldType(testCustomFieldType);
        rangeRule.setRuleName("Age Range");
        rangeRule.setRuleType(ValidationRuleConfiguration.RuleType.RANGE);
        rangeRule.setRuleExpression("{\"min\": 0, \"max\": 120}");
        rangeRule.setErrorMessage("Age must be between 0 and 120");
        rangeRule.setIsActive(true);

        // Setup enum rule
        enumRule = new ValidationRuleConfiguration();
        enumRule.setId("RULE-003");
        enumRule.setCustomFieldType(testCustomFieldType);
        enumRule.setRuleName("Status Values");
        enumRule.setRuleType(ValidationRuleConfiguration.RuleType.ENUM);
        enumRule.setRuleExpression("[\"ACTIVE\", \"INACTIVE\", \"PENDING\"]");
        enumRule.setErrorMessage("Status must be ACTIVE, INACTIVE, or PENDING");
        enumRule.setIsActive(true);

        // Setup length rule
        lengthRule = new ValidationRuleConfiguration();
        lengthRule.setId("RULE-004");
        lengthRule.setCustomFieldType(testCustomFieldType);
        lengthRule.setRuleName("Name Length");
        lengthRule.setRuleType(ValidationRuleConfiguration.RuleType.LENGTH);
        lengthRule.setRuleExpression("{\"minLength\": 2, \"maxLength\": 50}");
        lengthRule.setErrorMessage("Name must be between 2 and 50 characters");
        lengthRule.setIsActive(true);
    }

    /**
     * Test: Validate regex with matching value returns true
     */
    @Test
    public void testValidateRegex_WithMatchingValue_ReturnsTrue() {
        // Arrange
        String value = "test@example.com";
        String pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        // Act
        boolean result = validationRuleEngine.validateRegex(value, pattern);

        // Assert
        assertTrue("Valid email should match regex pattern", result);
    }

    /**
     * Test: Validate regex with non-matching value returns false
     */
    @Test
    public void testValidateRegex_WithNonMatchingValue_ReturnsFalse() {
        // Arrange
        String value = "invalid-email";
        String pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        // Act
        boolean result = validationRuleEngine.validateRegex(value, pattern);

        // Assert
        assertFalse("Invalid email should not match regex pattern", result);
    }

    /**
     * Test: Validate range with value in range returns true
     */
    @Test
    public void testValidateRange_WithValueInRange_ReturnsTrue() {
        // Arrange
        Number value = 25;
        Number min = 0;
        Number max = 120;

        // Act
        boolean result = validationRuleEngine.validateRange(value, min, max);

        // Assert
        assertTrue("Value within range should return true", result);
    }

    /**
     * Test: Validate range with value out of range returns false
     */
    @Test
    public void testValidateRange_WithValueOutOfRange_ReturnsFalse() {
        // Arrange
        Number value = 150;
        Number min = 0;
        Number max = 120;

        // Act
        boolean result = validationRuleEngine.validateRange(value, min, max);

        // Assert
        assertFalse("Value outside range should return false", result);
    }

    /**
     * Test: Validate range with value at boundaries returns true
     */
    @Test
    public void testValidateRange_WithValueAtBoundaries_ReturnsTrue() {
        // Arrange
        Number minValue = 0;
        Number maxValue = 120;
        Number min = 0;
        Number max = 120;

        // Act
        boolean minResult = validationRuleEngine.validateRange(minValue, min, max);
        boolean maxResult = validationRuleEngine.validateRange(maxValue, min, max);

        // Assert
        assertTrue("Value at minimum boundary should return true", minResult);
        assertTrue("Value at maximum boundary should return true", maxResult);
    }

    /**
     * Test: Validate enum with allowed value returns true
     */
    @Test
    public void testValidateEnum_WithAllowedValue_ReturnsTrue() {
        // Arrange
        String value = "ACTIVE";
        java.util.List<String> allowedValues = java.util.Arrays.asList("ACTIVE", "INACTIVE", "PENDING");

        // Act
        boolean result = validationRuleEngine.validateEnum(value, allowedValues);

        // Assert
        assertTrue("Allowed value should return true", result);
    }

    /**
     * Test: Validate enum with disallowed value returns false
     */
    @Test
    public void testValidateEnum_WithDisallowedValue_ReturnsFalse() {
        // Arrange
        String value = "UNKNOWN";
        java.util.List<String> allowedValues = java.util.Arrays.asList("ACTIVE", "INACTIVE", "PENDING");

        // Act
        boolean result = validationRuleEngine.validateEnum(value, allowedValues);

        // Assert
        assertFalse("Disallowed value should return false", result);
    }

    /**
     * Test: Validate length with valid length returns true
     */
    @Test
    public void testValidateLength_WithValidLength_ReturnsTrue() {
        // Arrange
        String value = "John Doe";
        Integer minLength = 2;
        Integer maxLength = 50;

        // Act
        boolean result = validationRuleEngine.validateLength(value, minLength, maxLength);

        // Assert
        assertTrue("Value with valid length should return true", result);
    }

    /**
     * Test: Validate length with invalid length returns false
     */
    @Test
    public void testValidateLength_WithInvalidLength_ReturnsFalse() {
        // Arrange
        String tooShort = "A";
        String tooLong = "A".repeat(51);
        Integer minLength = 2;
        Integer maxLength = 50;

        // Act
        boolean shortResult = validationRuleEngine.validateLength(tooShort, minLength, maxLength);
        boolean longResult = validationRuleEngine.validateLength(tooLong, minLength, maxLength);

        // Assert
        assertFalse("Value too short should return false", shortResult);
        assertFalse("Value too long should return false", longResult);
    }

    /**
     * Test: Validate length with value at boundaries returns true
     */
    @Test
    public void testValidateLength_WithValueAtBoundaries_ReturnsTrue() {
        // Arrange
        String minLengthValue = "AB";
        String maxLengthValue = "A".repeat(50);
        Integer minLength = 2;
        Integer maxLength = 50;

        // Act
        boolean minResult = validationRuleEngine.validateLength(minLengthValue, minLength, maxLength);
        boolean maxResult = validationRuleEngine.validateLength(maxLengthValue, minLength, maxLength);

        // Assert
        assertTrue("Value at minimum length should return true", minResult);
        assertTrue("Value at maximum length should return true", maxResult);
    }

    /**
     * Test: Evaluate rule with regex type returns correct result
     */
    @Test
    public void testEvaluateRule_WithRegexType_ReturnsCorrectResult() {
        // Arrange
        String validValue = "test@example.com";
        String invalidValue = "invalid-email";

        // Act
        boolean validResult = validationRuleEngine.evaluateRule(validValue, regexRule);
        boolean invalidResult = validationRuleEngine.evaluateRule(invalidValue, regexRule);

        // Assert
        assertTrue("Valid email should pass regex validation", validResult);
        assertFalse("Invalid email should fail regex validation", invalidResult);
    }

    /**
     * Test: Evaluate rule with range type returns correct result
     */
    @Test
    public void testEvaluateRule_WithRangeType_ReturnsCorrectResult() {
        // Arrange
        String validValue = "25";
        String invalidValue = "150";

        // Act
        boolean validResult = validationRuleEngine.evaluateRule(validValue, rangeRule);
        boolean invalidResult = validationRuleEngine.evaluateRule(invalidValue, rangeRule);

        // Assert
        assertTrue("Value in range should pass validation", validResult);
        assertFalse("Value out of range should fail validation", invalidResult);
    }

    /**
     * Test: Evaluate rule with enum type returns correct result
     */
    @Test
    public void testEvaluateRule_WithEnumType_ReturnsCorrectResult() {
        // Arrange
        String validValue = "ACTIVE";
        String invalidValue = "UNKNOWN";

        // Act
        boolean validResult = validationRuleEngine.evaluateRule(validValue, enumRule);
        boolean invalidResult = validationRuleEngine.evaluateRule(invalidValue, enumRule);

        // Assert
        assertTrue("Allowed value should pass enum validation", validResult);
        assertFalse("Disallowed value should fail enum validation", invalidResult);
    }

    /**
     * Test: Evaluate rule with length type returns correct result
     */
    @Test
    public void testEvaluateRule_WithLengthType_ReturnsCorrectResult() {
        // Arrange
        String validValue = "John Doe";
        String invalidValue = "A";

        // Act
        boolean validResult = validationRuleEngine.evaluateRule(validValue, lengthRule);
        boolean invalidResult = validationRuleEngine.evaluateRule(invalidValue, lengthRule);

        // Assert
        assertTrue("Value with valid length should pass validation", validResult);
        assertFalse("Value with invalid length should fail validation", invalidResult);
    }

    /**
     * Test: Evaluate rule with invalid rule expression throws exception
     */
    @Test(expected = org.openelisglobal.common.exception.LIMSRuntimeException.class)
    public void testEvaluateRule_WithInvalidRuleExpression_ThrowsException() {
        // Arrange
        ValidationRuleConfiguration invalidRule = new ValidationRuleConfiguration();
        invalidRule.setId("RULE-INVALID");
        invalidRule.setCustomFieldType(testCustomFieldType);
        invalidRule.setRuleName("Invalid Rule");
        invalidRule.setRuleType(ValidationRuleConfiguration.RuleType.RANGE);
        invalidRule.setRuleExpression("invalid json");
        invalidRule.setIsActive(true);

        String value = "25";

        // Act
        validationRuleEngine.evaluateRule(value, invalidRule);

        // Assert: Exception expected (handled by @Test(expected))
    }

    /**
     * Test: Evaluate rule with null value returns false
     */
    @Test
    public void testEvaluateRule_WithNullValue_ReturnsFalse() {
        // Act
        boolean result = validationRuleEngine.evaluateRule(null, regexRule);

        // Assert
        assertFalse("Null value should return false", result);
    }

    /**
     * Test: Evaluate rule with null rule returns false
     */
    @Test
    public void testEvaluateRule_WithNullRule_ReturnsFalse() {
        // Arrange
        String value = "test@example.com";

        // Act
        boolean result = validationRuleEngine.evaluateRule(value, null);

        // Assert
        assertFalse("Null rule should return false", result);
    }

    /**
     * Test: Validate regex with null pattern returns true (no validation)
     */
    @Test
    public void testValidateRegex_WithNullPattern_ReturnsTrue() {
        // Arrange
        String value = "any value";

        // Act
        boolean result = validationRuleEngine.validateRegex(value, null);

        // Assert
        assertTrue("Null pattern should return true (no validation)", result);
    }

    /**
     * Test: Validate regex with empty pattern returns true (no validation)
     */
    @Test
    public void testValidateRegex_WithEmptyPattern_ReturnsTrue() {
        // Arrange
        String value = "any value";

        // Act
        boolean result = validationRuleEngine.validateRegex(value, "");

        // Assert
        assertTrue("Empty pattern should return true (no validation)", result);
    }

    /**
     * Test: Validate range with null min/max handles correctly
     */
    @Test
    public void testValidateRange_WithNullMinMax_HandlesCorrectly() {
        // Arrange
        Number value = 25;

        // Act
        boolean resultWithNullMin = validationRuleEngine.validateRange(value, null, 100);
        boolean resultWithNullMax = validationRuleEngine.validateRange(value, 0, null);
        boolean resultWithBothNull = validationRuleEngine.validateRange(value, null, null);

        // Assert
        assertTrue("Value should pass when min is null", resultWithNullMin);
        assertTrue("Value should pass when max is null", resultWithNullMax);
        assertTrue("Value should pass when both min and max are null", resultWithBothNull);
    }

    /**
     * Test: Validate enum with null value returns false
     */
    @Test
    public void testValidateEnum_WithNullValue_ReturnsFalse() {
        // Arrange
        java.util.List<String> allowedValues = java.util.Arrays.asList("ACTIVE", "INACTIVE");

        // Act
        boolean result = validationRuleEngine.validateEnum(null, allowedValues);

        // Assert
        assertFalse("Null value should return false", result);
    }

    /**
     * Test: Validate length with null value returns false
     */
    @Test
    public void testValidateLength_WithNullValue_ReturnsFalse() {
        // Arrange
        Integer minLength = 2;
        Integer maxLength = 50;

        // Act
        boolean result = validationRuleEngine.validateLength(null, minLength, maxLength);

        // Assert
        assertFalse("Null value should return false", result);
    }
}
