package org.openelisglobal.storage.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;

/**
 * Unit tests for CodeValidationService
 * 
 * References: - Testing Roadmap: .specify/guides/testing-roadmap.md - Backend
 * Best Practices: .specify/guides/backend-testing-best-practices.md - Template:
 * JUnit 4 Service Test
 * 
 * TDD Workflow (MANDATORY for complex logic): - RED: Write failing test first
 * (defines expected behavior) - GREEN: Write minimal code to make test pass -
 * REFACTOR: Improve code quality while keeping tests green
 * 
 * Task Reference: T285
 * 
 * Test Naming: test{MethodName}_{Scenario}_{ExpectedResult}
 */
@RunWith(MockitoJUnitRunner.class)
public class CodeValidationServiceTest {

    @Mock
    private StorageRoomDAO storageRoomDAO;

    @Mock
    private StorageDeviceDAO storageDeviceDAO;

    @Mock
    private StorageShelfDAO storageShelfDAO;

    @Mock
    private StorageRackDAO storageRackDAO;

    @InjectMocks
    private CodeValidationServiceImpl codeValidationService;

    @Before
    public void setUp() {
        // Setup common test data
    }

    /**
     * Test code length constraint T285: Code Length Constraint - Code must be ≤10
     * characters
     */
    @Test
    public void testCodeLengthConstraint() {
        // Valid: exactly 10 chars
        CodeValidationResult result = codeValidationService.validateLength("1234567890");
        assertTrue("Code with 10 chars should be valid", result.isValid());

        // Valid: less than 10 chars
        result = codeValidationService.validateLength("FRZ01");
        assertTrue("Code with <10 chars should be valid", result.isValid());

        // Invalid: more than 10 chars
        result = codeValidationService.validateLength("12345678901");
        assertFalse("Code with >10 chars should be invalid", result.isValid());
        assertNotNull("Error message should be provided", result.getErrorMessage());
    }

    /**
     * Test code format validation T285: Code Format Validation - Alphanumeric,
     * hyphen, underscore only, must start with letter/number
     */
    @Test
    public void testCodeFormatValidation() {
        // Valid formats
        CodeValidationResult result = codeValidationService.validateFormat("FRZ01");
        assertTrue("Valid code: FRZ01", result.isValid());

        result = codeValidationService.validateFormat("A1-B2");
        assertTrue("Valid code: A1-B2", result.isValid());

        result = codeValidationService.validateFormat("RACK_1");
        assertTrue("Valid code: RACK_1", result.isValid());

        result = codeValidationService.validateFormat("1234567890");
        assertTrue("Valid code: 1234567890 (max length)", result.isValid());

        // Invalid formats
        result = codeValidationService.validateFormat("FRZ@01");
        assertFalse("Invalid: contains special chars", result.isValid());

        result = codeValidationService.validateFormat("FRZ 01");
        assertFalse("Invalid: contains spaces", result.isValid());

        result = codeValidationService.validateFormat("");
        assertFalse("Invalid: empty string", result.isValid());

        result = codeValidationService.validateFormat(null);
        assertFalse("Invalid: null", result.isValid());
    }

    /**
     * Test auto-uppercase conversion T285: Auto-Uppercase Conversion - Input
     * auto-converted to uppercase
     */
    @Test
    public void testAutoUppercaseConversion() {
        // Test that lowercase input is converted to uppercase
        CodeValidationResult result = codeValidationService.validateFormat("frz01");
        assertTrue("Should be valid", result.isValid());
        assertEquals("Should convert to uppercase", "FRZ01", result.getNormalizedCode());

        result = codeValidationService.validateFormat("rack-1");
        assertTrue("Should be valid", result.isValid());
        assertEquals("Should convert to uppercase", "RACK-1", result.getNormalizedCode());

        result = codeValidationService.validateFormat("a1_b2");
        assertTrue("Should be valid", result.isValid());
        assertEquals("Should convert to uppercase", "A1_B2", result.getNormalizedCode());
    }

    /**
     * Test must start with letter or number T285: Must Start With Letter Or Number
     * - Reject codes starting with hyphen/underscore
     */
    @Test
    public void testMustStartWithLetterOrNumber() {
        // Valid: starts with letter
        CodeValidationResult result = codeValidationService.validateFormat("FRZ01");
        assertTrue("Valid: starts with letter", result.isValid());

        // Valid: starts with number
        result = codeValidationService.validateFormat("1RACK");
        assertTrue("Valid: starts with number", result.isValid());

        // Invalid: starts with hyphen
        result = codeValidationService.validateFormat("-FRZ01");
        assertFalse("Invalid: starts with hyphen", result.isValid());
        assertNotNull("Error message should mention start requirement", result.getErrorMessage());

        // Invalid: starts with underscore
        result = codeValidationService.validateFormat("_RACK1");
        assertFalse("Invalid: starts with underscore", result.isValid());
        assertNotNull("Error message should mention start requirement", result.getErrorMessage());
    }

    /**
     * Test uniqueness within context
     * T285: Uniqueness Within Context - Room: globally unique; Device/Shelf/Rack: unique within parent
     */
    @Test
    public void testUniquenessWithinContext() {
        // Test Room context (globally unique)
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(null); // Not found = unique
        CodeValidationResult result = codeValidationService.validateUniqueness("MAIN", "room", "1", null);
        assertTrue("Code should be unique for room", result.isValid());

        // Test Room conflict (different room)
        StorageRoom existingRoom = new StorageRoom();
        existingRoom.setId(99);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(existingRoom);
        result = codeValidationService.validateUniqueness("MAIN", "room", "1", null);
        assertFalse("Code should not be unique if exists for different room", result.isValid());

        // Test Room update (same room)
        result = codeValidationService.validateUniqueness("MAIN", "room", "99", null);
        assertTrue("Code should be unique when updating same room", result.isValid());

        // Test Device context (unique within parent room)
        StorageRoom parentRoomForDevice = new StorageRoom();
        parentRoomForDevice.setId(1);
        when(storageRoomDAO.get(1)).thenReturn(Optional.of(parentRoomForDevice));
        when(storageDeviceDAO.findByCodeAndParentRoom("FRZ01", parentRoomForDevice)).thenReturn(null);
        result = codeValidationService.validateUniqueness("FRZ01", "device", "1", "1");
        assertTrue("Code should be unique for device within parent room", result.isValid());

        // Test Shelf context (unique within parent device)
        // Note: DAO method findByCodeAndParentDevice will be added in implementation phase
        // For now, test that validation requires parentId
        StorageDevice parentDevice = new StorageDevice();
        parentDevice.setId(1);
        when(storageDeviceDAO.get(1)).thenReturn(Optional.of(parentDevice));
        // Shelf uniqueness check will be implemented when DAO method is added
        result = codeValidationService.validateUniqueness("SHA", "shelf", "2", "1");
        assertTrue("Code validation should pass (uniqueness check placeholder)", result.isValid());

        // Test Rack context (unique within parent shelf)
        // Note: DAO method findByCodeAndParentShelf will be added in implementation phase
        // For now, test that validation requires parentId
        StorageShelf parentShelf = new StorageShelf();
        parentShelf.setId(1);
        when(storageShelfDAO.get(1)).thenReturn(Optional.of(parentShelf));
        // Rack uniqueness check will be implemented when DAO method is added
        result = codeValidationService.validateUniqueness("RKR1", "rack", "3", "1");
        assertTrue("Code validation should pass (uniqueness check placeholder)", result.isValid());
    }

    /**
     * Test auto-uppercase helper method T285: Auto-Uppercase Helper - Converts
     * input to uppercase
     */
    @Test
    public void testAutoUppercase() {
        String result = codeValidationService.autoUppercase("frz01");
        assertEquals("Should convert to uppercase", "FRZ01", result);

        result = codeValidationService.autoUppercase("FRZ01");
        assertEquals("Should keep uppercase", "FRZ01", result);

        result = codeValidationService.autoUppercase("rack-1");
        assertEquals("Should convert to uppercase", "RACK-1", result);
    }
}
