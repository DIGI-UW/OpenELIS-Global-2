package org.openelisglobal.common.provider.validation;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import org.junit.Test;

public class DateValidationProviderTest {

    private DateValidationProvider dateValidationProvider = new DateValidationProvider();

    @Test
    public void validateDate_shouldReturnValidForPastDateWithPastRelation() {
        // Given
        Date pastDate = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // 1 day ago

        // When
        String result = dateValidationProvider.validateDate(pastDate, "PAST");

        // Then
        assertEquals("Past date should be valid for PAST relation", "valid", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForFutureDateWithPastRelation() {
        // Given
        Date futureDate = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 1 day from now

        // When
        String result = dateValidationProvider.validateDate(futureDate, "PAST");

        // Then
        assertEquals("Future date should be invalid for PAST relation", "invalid_value_to_large", result);
    }

    @Test
    public void validateDate_shouldReturnValidForFutureDateWithFutureRelation() {
        // Given
        Date futureDate = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 1 day from now

        // When
        String result = dateValidationProvider.validateDate(futureDate, "FUTURE");

        // Then
        assertEquals("Future date should be valid for FUTURE relation", "valid", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForPastDateWithFutureRelation() {
        // Given
        Date pastDate = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // 1 day ago

        // When
        String result = dateValidationProvider.validateDate(pastDate, "FUTURE");

        // Then
        assertEquals("Past date should be invalid for FUTURE relation", "invalid_value_to_small", result);
    }

    @Test
    public void validateDate_shouldReturnValidForTodayWithTodayRelation() {
        // Given
        Date today = new Date(); // Current date

        // When
        String result = dateValidationProvider.validateDate(today, "TODAY");

        // Then
        assertEquals("Today should be valid for TODAY relation", "valid", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForPastDateWithTodayRelation() {
        // Given
        Date pastDate = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // 1 day ago

        // When
        String result = dateValidationProvider.validateDate(pastDate, "TODAY");

        // Then
        assertEquals("Past date should be invalid for TODAY relation", "invalid_value_to_small", result);
    }

    @Test
    public void validateDate_shouldReturnInvalidForFutureDateWithTodayRelation() {
        // Given
        Date futureDate = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 1 day from now

        // When
        String result = dateValidationProvider.validateDate(futureDate, "TODAY");

        // Then
        assertEquals("Future date should be invalid for TODAY relation", "invalid_value_to_small", result);
    }

    @Test
    public void validateDate_shouldReturnValidForAnyDateWithAnyRelation() {
        // Given
        Date pastDate = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // 1 day ago
        Date futureDate = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 1 day from now
        Date today = new Date(); // Current date

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
        Date pastDate = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // 1 day ago

        // When
        String result = dateValidationProvider.validateDate(pastDate, "past"); // lowercase

        // Then
        assertEquals("Should handle lowercase relation string", "valid", result);
    }
}