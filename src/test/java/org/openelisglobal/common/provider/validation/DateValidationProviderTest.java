package org.openelisglobal.common.provider.validation;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import org.junit.Test;

public class DateValidationProviderTest {

    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L;

    private DateValidationProvider dateValidationProvider = new DateValidationProvider();

    // Fixed date offsets to avoid midnight crossing issues during test execution
    // Using offsets from a recent time to ensure dates are definitely in past/future
    private static final long BASE_TIME = System.currentTimeMillis();
    private static final Date FIXED_PAST_DATE = new Date(BASE_TIME - (30 * ONE_DAY_MILLIS)); // 30 days ago
    private static final Date FIXED_FUTURE_DATE = new Date(BASE_TIME + (30 * ONE_DAY_MILLIS)); // 30 days from now
    private static final Date FIXED_TODAY_DATE = new Date(BASE_TIME); // Current time as "today"

    @Test
    public void validateDate_shouldReturnValidForPastDateWithPastRelation() {
        // Given
        Date pastDate = FIXED_PAST_DATE; // 2 days before fixed time

        // When
        String result = dateValidationProvider.validateDate(pastDate, "PAST");

        // Then
        assertEquals("Past date should be valid for PAST relation", "valid", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForFutureDateWithPastRelation() {
        // Given
        Date futureDate = FIXED_FUTURE_DATE; // 2 days after fixed time

        // When
        String result = dateValidationProvider.validateDate(futureDate, "PAST");

        // Then
        assertEquals("Future date should be invalid for PAST relation", "invalid_value_to_large", result);
    }

    @Test
    public void validateDate_shouldReturnValidForFutureDateWithFutureRelation() {
        // Given
        Date futureDate = FIXED_FUTURE_DATE; // 2 days after fixed time

        // When
        String result = dateValidationProvider.validateDate(futureDate, "FUTURE");

        // Then
        assertEquals("Future date should be valid for FUTURE relation", "valid", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForPastDateWithFutureRelation() {
        // Given
        Date pastDate = FIXED_PAST_DATE; // 2 days before fixed time

        // When
        String result = dateValidationProvider.validateDate(pastDate, "FUTURE");

        // Then
        assertEquals("Past date should be invalid for FUTURE relation", "invalid_value_to_small", result);
    }

    @Test
    public void validateDate_shouldReturnValidForTodayWithTodayRelation() {
        // Given
        Date today = FIXED_TODAY_DATE; // Fixed "today" date

        // When
        String result = dateValidationProvider.validateDate(today, "TODAY");

        // Then
        assertEquals("Today should be valid for TODAY relation", "valid", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForPastDateWithTodayRelation() {
        // Given
        Date pastDate = FIXED_PAST_DATE; // 2 days before fixed time

        // When
        String result = dateValidationProvider.validateDate(pastDate, "TODAY");

        // Then
        assertEquals("Past date should be invalid for TODAY relation", "invalid_value_to_small", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForFutureDateWithTodayRelation() {
        // Given
        Date futureDate = FIXED_FUTURE_DATE; // 2 days after fixed time

        // When
        String result = dateValidationProvider.validateDate(futureDate, "TODAY");

        // Then
        assertEquals("Future date should be invalid for TODAY relation", "invalid_value_to_small", result);
    }

    @Test
    public void validateDate_shouldReturnValidForAnyDateWithAnyRelation() {
        // Given
        Date pastDate = FIXED_PAST_DATE; // 2 days before fixed time
        Date futureDate = FIXED_FUTURE_DATE; // 2 days after fixed time
        Date today = FIXED_TODAY_DATE; // Fixed "today" date

        // When & Then
        assertEquals("Past date should be valid for ANY relation", "valid",
                    dateValidationProvider.validateDate(pastDate, "ANY"));
        assertEquals("Future date should be valid for ANY relation", "valid",
                    dateValidationProvider.validateDate(futureDate, "ANY"));
        assertEquals("Today should be valid for ANY relation", "valid",
                    dateValidationProvider.validateDate(today, "ANY"));
    }

    @Test
    public void validateDate_shouldReturnInvalidForNullDate() {
        // Given
        Date nullDate = null;

        // When
        String result = dateValidationProvider.validateDate(nullDate, "PAST");

        // Then
        assertEquals("Null date should be invalid", "invalid", result);
    }

    @Test
    public void validateDate_shouldHandleCaseInsensitiveRelation() {
        // Given
        Date pastDate = FIXED_PAST_DATE; // 2 days before fixed time

        // When
        String result = dateValidationProvider.validateDate(pastDate, "past"); // lowercase

        // Then
        assertEquals("Should handle lowercase relation string", "valid", result);
    }
}