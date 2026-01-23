package org.openelisglobal.storage.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for BarcodeParsingService Following TDD: Write tests BEFORE
 * implementation Tests barcode parsing functionality per FR-023 through FR-027
 */
@RunWith(MockitoJUnitRunner.class)
public class BarcodeParsingServiceTest {

    private BarcodeParsingService barcodeParsingService;

    @Before
    public void setUp() {
        barcodeParsingService = new BarcodeParsingServiceImpl();
    }

    /**
     * Test parsing a 2-level barcode: ROOM-DEVICE Expected: Returns ParsedBarcode
     * with 2 components
     */
    @Test
    public void testParse2LevelBarcode() {
        // Arrange
        String barcode = "MAIN-FRZ01";

        // Act
        ParsedBarcode result = barcodeParsingService.parseBarcode(barcode);

        // Assert
        assertNotNull("ParsedBarcode should not be null", result);
        assertEquals("Should have 2 levels", 2, result.getLevelCount());
        assertEquals("Room code should be MAIN", "MAIN", result.getRoomCode());
        assertEquals("Device code should be FRZ01", "FRZ01", result.getDeviceCode());
        assertNull("Shelf code should be null for 2-level barcode", result.getShelfCode());
        assertNull("Rack code should be null for 2-level barcode", result.getRackCode());
        assertNull("Position code should be null for 2-level barcode", result.getPositionCode());
        assertTrue("Barcode should be valid", result.isValid());
    }

    /**
     * Test parsing a 3-level barcode: ROOM-DEVICE-SHELF Expected: Returns
     * ParsedBarcode with 3 components
     */
    @Test
    public void testParse3LevelBarcode() {
        // Arrange
        String barcode = "MAIN-FRZ01-SHA";

        // Act
        ParsedBarcode result = barcodeParsingService.parseBarcode(barcode);

        // Assert
        assertNotNull("ParsedBarcode should not be null", result);
        assertEquals("Should have 3 levels", 3, result.getLevelCount());
        assertEquals("Room code should be MAIN", "MAIN", result.getRoomCode());
        assertEquals("Device code should be FRZ01", "FRZ01", result.getDeviceCode());
        assertEquals("Shelf code should be SHA", "SHA", result.getShelfCode());
        assertNull("Rack code should be null for 3-level barcode", result.getRackCode());
        assertNull("Position code should be null for 3-level barcode", result.getPositionCode());
        assertTrue("Barcode should be valid", result.isValid());
    }

    /**
     * Test parsing a 4-level barcode: ROOM-DEVICE-SHELF-RACK Expected: Returns
     * ParsedBarcode with 4 components
     */
    @Test
    public void testParse4LevelBarcode() {
        // Arrange
        String barcode = "MAIN-FRZ01-SHA-RKR1";

        // Act
        ParsedBarcode result = barcodeParsingService.parseBarcode(barcode);

        // Assert
        assertNotNull("ParsedBarcode should not be null", result);
        assertEquals("Should have 4 levels", 4, result.getLevelCount());
        assertEquals("Room code should be MAIN", "MAIN", result.getRoomCode());
        assertEquals("Device code should be FRZ01", "FRZ01", result.getDeviceCode());
        assertEquals("Shelf code should be SHA", "SHA", result.getShelfCode());
        assertEquals("Rack code should be RKR1", "RKR1", result.getRackCode());
        assertNull("Position code should be null for 4-level barcode", result.getPositionCode());
        assertTrue("Barcode should be valid", result.isValid());
    }

    /**
     * Test parsing a 5-level barcode: ROOM-DEVICE-SHELF-RACK-POSITION Expected:
     * Returns ParsedBarcode with 5 components
     */
    @Test
    public void testParse5LevelBarcode() {
        // Arrange
        String barcode = "MAIN-FRZ01-SHA-RKR1-A5";

        // Act
        ParsedBarcode result = barcodeParsingService.parseBarcode(barcode);

        // Assert
        assertNotNull("ParsedBarcode should not be null", result);
        assertEquals("Should have 5 levels", 5, result.getLevelCount());
        assertEquals("Room code should be MAIN", "MAIN", result.getRoomCode());
        assertEquals("Device code should be FRZ01", "FRZ01", result.getDeviceCode());
        assertEquals("Shelf code should be SHA", "SHA", result.getShelfCode());
        assertEquals("Rack code should be RKR1", "RKR1", result.getRackCode());
        assertEquals("Position code should be A5", "A5", result.getPositionCode());
        assertTrue("Barcode should be valid", result.isValid());
    }

    /**
     * Test parsing with hyphen delimiter (standard delimiter) Expected: Accepts
     * hyphen as delimiter
     */
    @Test
    public void testParseWithHyphenDelimiter() {
        // Arrange
        String barcode = "MAIN-FRZ01-SHA";

        // Act
        ParsedBarcode result = barcodeParsingService.parseBarcode(barcode);

        // Assert
        assertNotNull("ParsedBarcode should not be null", result);
        assertTrue("Should parse barcode with hyphen delimiter", result.isValid());
        assertEquals("Should extract correct components", 3, result.getLevelCount());
    }

    /**
     * Test rejecting invalid delimiter (e.g., underscore, dot, slash) Expected:
     * Returns invalid ParsedBarcode with error message
     */
    @Test
    public void testRejectInvalidDelimiter() {
        // Arrange
        String barcodeUnderscore = "MAIN_FRZ01_SHA";
        String barcodeDot = "MAIN.FRZ01.SHA";
        String barcodeSlash = "MAIN/FRZ01/SHA";

        // Act
        ParsedBarcode resultUnderscore = barcodeParsingService.parseBarcode(barcodeUnderscore);
        ParsedBarcode resultDot = barcodeParsingService.parseBarcode(barcodeDot);
        ParsedBarcode resultSlash = barcodeParsingService.parseBarcode(barcodeSlash);

        // Assert
        assertFalse("Should reject underscore delimiter", resultUnderscore.isValid());
        assertNotNull("Should have error message for underscore", resultUnderscore.getErrorMessage());

        assertFalse("Should reject dot delimiter", resultDot.isValid());
        assertNotNull("Should have error message for dot", resultDot.getErrorMessage());

        assertFalse("Should reject slash delimiter", resultSlash.isValid());
        assertNotNull("Should have error message for slash", resultSlash.getErrorMessage());
    }

    /**
     * Test handling empty barcode string Expected: Returns invalid ParsedBarcode
     * with error message
     */
    @Test
    public void testHandleEmptyBarcode() {
        // Arrange
        String barcode = "";

        // Act
        ParsedBarcode result = barcodeParsingService.parseBarcode(barcode);

        // Assert
        assertNotNull("ParsedBarcode should not be null", result);
        assertFalse("Empty barcode should be invalid", result.isValid());
        assertNotNull("Should have error message", result.getErrorMessage());
        assertTrue("Error message should mention empty barcode",
                result.getErrorMessage().toLowerCase().contains("empty"));
    }

    /**
     * Test handling null barcode string Expected: Returns invalid ParsedBarcode
     * with error message
     */
    @Test
    public void testHandleNullBarcode() {
        // Arrange
        String barcode = null;

        // Act
        ParsedBarcode result = barcodeParsingService.parseBarcode(barcode);

        // Assert
        assertNotNull("ParsedBarcode should not be null", result);
        assertFalse("Null barcode should be invalid", result.isValid());
        assertNotNull("Should have error message", result.getErrorMessage());
        assertTrue("Error message should mention null or empty barcode",
                result.getErrorMessage().toLowerCase().contains("null")
                        || result.getErrorMessage().toLowerCase().contains("empty"));
    }

    /**
     * Test validateFormat method Expected: Returns true for valid format, false for
     * invalid
     */
    @Test
    public void testValidateFormat() {
        // Valid formats
        assertTrue("Should validate 2-level barcode", barcodeParsingService.validateFormat("MAIN-FRZ01"));
        assertTrue("Should validate 3-level barcode", barcodeParsingService.validateFormat("MAIN-FRZ01-SHA"));
        assertTrue("Should validate 4-level barcode", barcodeParsingService.validateFormat("MAIN-FRZ01-SHA-RKR1"));
        assertTrue("Should validate 5-level barcode", barcodeParsingService.validateFormat("MAIN-FRZ01-SHA-RKR1-A5"));

        // Invalid formats
        assertFalse("Should reject single component", barcodeParsingService.validateFormat("MAIN"));
        assertFalse("Should reject more than 5 levels",
                barcodeParsingService.validateFormat("MAIN-FRZ01-SHA-RKR1-A5-EXTRA"));
        assertFalse("Should reject empty string", barcodeParsingService.validateFormat(""));
        assertFalse("Should reject null", barcodeParsingService.validateFormat(null));
    }

    /**
     * Test extractComponents method Expected: Returns list of string components
     */
    @Test
    public void testExtractComponents() {
        // Arrange
        String barcode = "MAIN-FRZ01-SHA-RKR1";

        // Act
        List<String> components = barcodeParsingService.extractComponents(barcode);

        // Assert
        assertNotNull("Components list should not be null", components);
        assertEquals("Should extract 4 components", 4, components.size());
        assertEquals("First component should be MAIN", "MAIN", components.get(0));
        assertEquals("Second component should be FRZ01", "FRZ01", components.get(1));
        assertEquals("Third component should be SHA", "SHA", components.get(2));
        assertEquals("Fourth component should be RKR1", "RKR1", components.get(3));
    }

    /**
     * Test extractComponents with invalid barcode Expected: Returns empty list or
     * throws exception
     */
    @Test
    public void testExtractComponentsInvalidBarcode() {
        // Arrange
        String barcode = "INVALID_BARCODE";

        // Act
        List<String> components = barcodeParsingService.extractComponents(barcode);

        // Assert
        assertNotNull("Components list should not be null", components);
        assertTrue("Should return empty list for invalid barcode", components.isEmpty());
    }
}
