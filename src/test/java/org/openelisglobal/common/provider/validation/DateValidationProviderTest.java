package org.openelisglobal.common.provider.validation;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import org.junit.Test;

public class DateValidationProviderTest {

    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L;

    // Use current time as base and create offsets to ensure dates are definitely in
    // past/future
    // relative to when the CustomDateValidator runs (which uses current system
    // time)
    private static final long CURRENT_TIME = System.currentTimeMillis();
    private static final Date PAST_DATE = new Date(CURRENT_TIME - (30 * ONE_DAY_MILLIS)); // 30 days ago
    private static final Date FUTURE_DATE = new Date(CURRENT_TIME + (30 * ONE_DAY_MILLIS)); // 30 days from now
    private static final Date TODAY_DATE = new Date(CURRENT_TIME); // Current time as "today"

    @Test
    public void validateDate_shouldReturnValidForPastDateWithPastRelation() throws Exception {
        // Given - using date that represents a past date
        Date pastDate = PAST_DATE; // 30 days ago from current time

        // When - create provider directly and test validation
        DateValidationProvider provider = new DateValidationProvider();
        String result = provider.validateDate(pastDate, "PAST");

        // Then
        assertEquals("Past date should be valid for PAST relation", "valid", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForFutureDateWithPastRelation() throws Exception {
        // Given - using date that represents a future date
        Date futureDate = FUTURE_DATE; // 30 days from current time

        // When - create provider directly and test validation
        DateValidationProvider provider = new DateValidationProvider();
        String result = provider.validateDate(futureDate, "PAST");

        // Then
        assertEquals("Future date should be invalid for PAST relation", "invalid_value_to_large", result);
    }

    @Test
    public void validateDate_shouldReturnValidForFutureDateWithFutureRelation() throws Exception {
        // Given - using date that represents a future date
        Date futureDate = FUTURE_DATE; // 30 days from current time

        // When - create provider directly and test validation
        DateValidationProvider provider = new DateValidationProvider();
        String result = provider.validateDate(futureDate, "FUTURE");

        // Then
        assertEquals("Future date should be valid for FUTURE relation", "valid", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForPastDateWithFutureRelation() throws Exception {
        // Given - using date that represents a past date
        Date pastDate = PAST_DATE; // 30 days ago from current time

        // When - create provider directly and test validation
        DateValidationProvider provider = new DateValidationProvider();
        String result = provider.validateDate(pastDate, "FUTURE");

        // Then
        assertEquals("Past date should be invalid for FUTURE relation", "invalid_value_to_small", result);
    }

    @Test
    public void validateDate_shouldReturnValidForTodayWithTodayRelation() throws Exception {
        // Given - using date that represents today
        Date todayDate = TODAY_DATE; // Current time (today)

        // When - create provider directly and test validation
        DateValidationProvider provider = new DateValidationProvider();
        String result = provider.validateDate(todayDate, "TODAY");

        // Then
        assertEquals("Today should be valid for TODAY relation", "valid", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForPastDateWithTodayRelation() throws Exception {
        // Given - using date that represents a past date
        Date pastDate = PAST_DATE; // 30 days ago from current time

        // When - create provider directly and test validation
        DateValidationProvider provider = new DateValidationProvider();
        String result = provider.validateDate(pastDate, "TODAY");

        // Then
        assertEquals("Past date should be invalid for TODAY relation", "invalid_value_to_small", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForFutureDateWithTodayRelation() throws Exception {
        // Given - using date that represents a future date
        Date futureDate = FUTURE_DATE; // 30 days from current time

        // When - create provider directly and test validation
        DateValidationProvider provider = new DateValidationProvider();
        String result = provider.validateDate(futureDate, "TODAY");

        // Then
        assertEquals("Future date should be invalid for TODAY relation", "invalid_value_to_small", result);
    }

    @Test
    public void validateDate_shouldReturnValidForAnyDateWithAnyRelation() throws Exception {
        // Test that ANY relation accepts past, present, and future dates
        DateValidationProvider provider = new DateValidationProvider();

        // Past date
        assertEquals("Past date should be valid for ANY relation", "valid", provider.validateDate(PAST_DATE, "ANY"));

        // Today
        assertEquals("Today should be valid for ANY relation", "valid", provider.validateDate(TODAY_DATE, "ANY"));

        // Future date
        assertEquals("Future date should be valid for ANY relation", "valid",
                provider.validateDate(FUTURE_DATE, "ANY"));
    }

    @Test
    public void validateDate_shouldReturnInvalidForNullDate() throws Exception {
        // Given - null date
        Date nullDate = null;

        // When - create provider directly and test validation
        DateValidationProvider provider = new DateValidationProvider();
        String result = provider.validateDate(nullDate, "PAST");

        // Then
        assertEquals("Null date should be invalid", "invalid", result);
    }

    @Test
    public void validateDate_shouldHandleCaseInsensitiveRelation() throws Exception {
        // Given - using lowercase relation
        Date pastDate = PAST_DATE; // Past date

        // When - create provider directly and test validation with lowercase relation
        DateValidationProvider provider = new DateValidationProvider();
        String result = provider.validateDate(pastDate, "past"); // lowercase

        // Then
        assertEquals("Should handle lowercase relation string", "valid", result);
    }
}