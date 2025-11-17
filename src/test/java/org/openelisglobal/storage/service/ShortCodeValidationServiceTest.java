package org.openelisglobal.storage.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;

/**
 * Unit tests for ShortCodeValidationService Following TDD: Write tests BEFORE
 * implementation Tests short code format validation, uniqueness checking, and
 * change warnings
 */
@RunWith(MockitoJUnitRunner.class)
public class ShortCodeValidationServiceTest {

    @Mock
    private StorageDeviceDAO storageDeviceDAO;

    @Mock
    private StorageShelfDAO storageShelfDAO;

    @Mock
    private StorageRackDAO storageRackDAO;

    @InjectMocks
    private ShortCodeValidationServiceImpl shortCodeValidationService;

    @Before
    public void setUp() {
        // Setup common test data
    }

    /**
     * Test short code format validation T254: Short Code Format - Max 10 chars,
     * alphanumeric, hyphen/underscore allowed
     */
    @Test
    public void testShortCodeFormat() {
        // Valid formats
        assertTrue("Valid short code: FRZ01", shortCodeValidationService.validateFormat("FRZ01").isValid());
        assertTrue("Valid short code: A1-B2", shortCodeValidationService.validateFormat("A1-B2").isValid());
        assertTrue("Valid short code: RACK_1", shortCodeValidationService.validateFormat("RACK_1").isValid());
        assertTrue("Valid short code: 1234567890 (max length)",
                shortCodeValidationService.validateFormat("1234567890").isValid());

        // Invalid formats
        assertFalse("Invalid: too long (>10 chars)",
                shortCodeValidationService.validateFormat("12345678901").isValid());
        assertFalse("Invalid: contains special chars", shortCodeValidationService.validateFormat("FRZ@01").isValid());
        assertFalse("Invalid: contains spaces", shortCodeValidationService.validateFormat("FRZ 01").isValid());
        assertFalse("Invalid: empty string", shortCodeValidationService.validateFormat("").isValid());
        assertFalse("Invalid: null", shortCodeValidationService.validateFormat(null).isValid());
    }

    /**
     * Test auto-uppercase conversion T254: Auto-Uppercase Conversion - Input
     * auto-converted to uppercase
     */
    @Test
    public void testAutoUppercaseConversion() {
        // Test that lowercase input is converted to uppercase
        String result = shortCodeValidationService.validateFormat("frz01").getNormalizedCode();
        assertEquals("Should convert to uppercase", "FRZ01", result);

        result = shortCodeValidationService.validateFormat("rack-1").getNormalizedCode();
        assertEquals("Should convert to uppercase", "RACK-1", result);

        result = shortCodeValidationService.validateFormat("a1_b2").getNormalizedCode();
        assertEquals("Should convert to uppercase", "A1_B2", result);
    }

    /**
     * Test must start with letter or number T254: Must Start With Letter Or Number
     * - Reject codes starting with hyphen/underscore
     */
    @Test
    public void testMustStartWithLetterOrNumber() {
        // Valid: starts with letter
        assertTrue("Valid: starts with letter", shortCodeValidationService.validateFormat("FRZ01").isValid());

        // Valid: starts with number
        assertTrue("Valid: starts with number", shortCodeValidationService.validateFormat("1RACK").isValid());

        // Invalid: starts with hyphen
        assertFalse("Invalid: starts with hyphen", shortCodeValidationService.validateFormat("-FRZ01").isValid());

        // Invalid: starts with underscore
        assertFalse("Invalid: starts with underscore", shortCodeValidationService.validateFormat("_RACK1").isValid());
    }

    /**
     * Test uniqueness within context
     * T254: Uniqueness Within Context - Validate uniqueness within device/shelf/rack scope
     */
    @Test
    public void testUniquenessWithinContext() {
        // Test device context
        when(storageDeviceDAO.findByShortCode("FRZ01")).thenReturn(null); // Not found = unique
        assertTrue("Short code should be unique for device", 
            shortCodeValidationService.validateUniqueness("FRZ01", "device", "1").isValid());

        // Test shelf context
        when(storageShelfDAO.findByShortCode("SHA")).thenReturn(null); // Not found = unique
        assertTrue("Short code should be unique for shelf", 
            shortCodeValidationService.validateUniqueness("SHA", "shelf", "2").isValid());

        // Test rack context
        when(storageRackDAO.findByShortCode("RKR1")).thenReturn(null); // Not found = unique
        assertTrue("Short code should be unique for rack", 
            shortCodeValidationService.validateUniqueness("RKR1", "rack", "3").isValid());

        // Test non-unique (existing device with different ID)
        StorageDevice existingDevice = new StorageDevice();
        existingDevice.setId(99);
        // Note: shortCode field doesn't exist yet, but we're testing the logic
        when(storageDeviceDAO.findByShortCode("FRZ01")).thenReturn(existingDevice);
        assertFalse("Short code should not be unique if exists for different device", 
            shortCodeValidationService.validateUniqueness("FRZ01", "device", "1").isValid());

        // Test unique when same location (updating existing)
        assertTrue("Short code should be unique when updating same location", 
            shortCodeValidationService.validateUniqueness("FRZ01", "device", "99").isValid());
    }

    /**
     * Test warning when changing short code T254: Warning When Changing Short Code
     * - Warning generated when short code changes
     */
    @Test
    public void testWarningWhenChangingShortCode() {
        // No warning when short code is null (new location)
        String warning = shortCodeValidationService.checkShortCodeChangeWarning(null, "FRZ01", "1");
        assertNull("No warning when old code is null", warning);

        // No warning when codes are the same
        warning = shortCodeValidationService.checkShortCodeChangeWarning("FRZ01", "FRZ01", "1");
        assertNull("No warning when codes are the same", warning);

        // Warning when codes are different
        warning = shortCodeValidationService.checkShortCodeChangeWarning("FRZ01", "FRZ02", "1");
        assertNotNull("Warning should be generated when short code changes", warning);
        assertTrue("Warning should mention short code change", warning.contains("FRZ01") || warning.contains("FRZ02"));
    }
}
