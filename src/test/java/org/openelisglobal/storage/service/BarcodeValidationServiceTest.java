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
import org.openelisglobal.storage.dao.StoragePositionDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.*;

/**
 * Unit tests for BarcodeValidationService Following TDD: Write tests BEFORE
 * implementation Tests 5-step barcode validation process per FR-024 through
 * FR-027
 */
@RunWith(MockitoJUnitRunner.class)
public class BarcodeValidationServiceTest {

    @Mock
    private BarcodeParsingService barcodeParsingService;

    @Mock
    private StorageRoomDAO storageRoomDAO;

    @Mock
    private StorageDeviceDAO storageDeviceDAO;

    @Mock
    private StorageShelfDAO storageShelfDAO;

    @Mock
    private StorageRackDAO storageRackDAO;

    @Mock
    private StoragePositionDAO storagePositionDAO;

    @InjectMocks
    private BarcodeValidationServiceImpl barcodeValidationService;

    private ParsedBarcode validParsedBarcode;
    private StorageRoom testRoom;
    private StorageDevice testDevice;
    private StorageShelf testShelf;
    private StorageRack testRack;
    private StoragePosition testPosition;

    @Before
    public void setUp() {
        // Create valid parsed barcode
        validParsedBarcode = new ParsedBarcode();
        validParsedBarcode.setValid(true);
        validParsedBarcode.setRoomCode("MAIN");
        validParsedBarcode.setDeviceCode("FRZ01");
        validParsedBarcode.setShelfCode("SHA");
        validParsedBarcode.setRackCode("RKR1");
        validParsedBarcode.setPositionCode("A5");

        // Create test entities
        testRoom = new StorageRoom();
        testRoom.setId(1);
        testRoom.setCode("MAIN");
        testRoom.setActive(true);

        testDevice = new StorageDevice();
        testDevice.setId(2);
        testDevice.setCode("FRZ01");
        testDevice.setParentRoom(testRoom);
        testDevice.setActive(true);

        testShelf = new StorageShelf();
        testShelf.setId(3);
        testShelf.setLabel("SHA");
        testShelf.setParentDevice(testDevice);
        testShelf.setActive(true);

        testRack = new StorageRack();
        testRack.setId(4);
        testRack.setLabel("RKR1");
        testRack.setParentShelf(testShelf);
        testRack.setActive(true);

        testPosition = new StoragePosition();
        testPosition.setId(5);
        testPosition.setCoordinate("A5");
        testPosition.setParentRack(testRack);
        // Note: StoragePosition doesn't have an active field
    }

    /**
     * Test Step 1: Format Validation Expected: Rejects invalid barcode format
     */
    @Test
    public void testStep1FormatValidation() {
        // Arrange
        String invalidBarcode = "INVALID_FORMAT";
        ParsedBarcode invalidParsed = new ParsedBarcode();
        invalidParsed.setValid(false);
        invalidParsed.setErrorMessage("Invalid format");

        when(barcodeParsingService.parseBarcode(invalidBarcode)).thenReturn(invalidParsed);

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(invalidBarcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertFalse("Validation should fail for invalid format", response.isValid());
        assertEquals("Step should be FORMAT_VALIDATION", "FORMAT_VALIDATION", response.getFailedStep());
        assertNotNull("Error message should be present", response.getErrorMessage());
        assertTrue("Error message should mention format", response.getErrorMessage().toLowerCase().contains("format"));
    }

    /**
     * Test Step 2: Location Existence Check Expected: Rejects barcode when location
     * does not exist in database
     */
    @Test
    public void testStep2LocationExistenceCheck() {
        // Arrange
        String barcode = "MAIN-FRZ01-SHA-RKR1-A5";
        when(barcodeParsingService.parseBarcode(barcode)).thenReturn(validParsedBarcode);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(null); // Room doesn't exist

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(barcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertFalse("Validation should fail when room doesn't exist", response.isValid());
        assertEquals("Step should be LOCATION_EXISTENCE", "LOCATION_EXISTENCE", response.getFailedStep());
        assertNotNull("Error message should be present", response.getErrorMessage());
        assertTrue("Error message should mention 'not found'",
                response.getErrorMessage().toLowerCase().contains("not found"));
    }

    /**
     * Test Step 3: Hierarchy Validation Expected: Rejects barcode when hierarchy is
     * broken (parent-child mismatch)
     */
    @Test
    public void testStep3HierarchyValidation() {
        // Arrange
        String barcode = "MAIN-FRZ01-SHA-RKR1-A5";
        StorageRoom wrongRoom = new StorageRoom();
        wrongRoom.setId(99);
        wrongRoom.setCode("WRONG_ROOM");

        StorageDevice deviceWithWrongParent = new StorageDevice();
        deviceWithWrongParent.setId(2);
        deviceWithWrongParent.setCode("FRZ01");
        deviceWithWrongParent.setParentRoom(wrongRoom); // Wrong parent!

        when(barcodeParsingService.parseBarcode(barcode)).thenReturn(validParsedBarcode);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(testRoom);
        when(storageDeviceDAO.findByCode("FRZ01")).thenReturn(deviceWithWrongParent); // Device exists elsewhere
        when(storageDeviceDAO.findByCodeAndParentRoom("FRZ01", testRoom)).thenReturn(null); // Hierarchy broken

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(barcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertFalse("Validation should fail when hierarchy is broken", response.isValid());
        assertEquals("Step should be HIERARCHY_VALIDATION", "HIERARCHY_VALIDATION", response.getFailedStep());
        assertNotNull("Error message should be present", response.getErrorMessage());
        assertTrue("Error message should mention hierarchy",
                response.getErrorMessage().toLowerCase().contains("hierarchy")
                        || response.getErrorMessage().toLowerCase().contains("parent"));
    }

    /**
     * Test Step 4: Activity Check Expected: Rejects barcode when any location in
     * hierarchy is inactive
     */
    @Test
    public void testStep4ActivityCheck() {
        // Arrange
        String barcode = "MAIN-FRZ01-SHA-RKR1-A5";
        testDevice.setActive(false); // Device is inactive

        when(barcodeParsingService.parseBarcode(barcode)).thenReturn(validParsedBarcode);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(testRoom);
        when(storageDeviceDAO.findByCode("FRZ01")).thenReturn(testDevice); // Device exists
        when(storageDeviceDAO.findByCodeAndParentRoom("FRZ01", testRoom)).thenReturn(testDevice);

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(barcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertFalse("Validation should fail when device is inactive", response.isValid());
        assertEquals("Step should be ACTIVITY_CHECK", "ACTIVITY_CHECK", response.getFailedStep());
        assertNotNull("Error message should be present", response.getErrorMessage());
        assertTrue("Error message should mention 'inactive'",
                response.getErrorMessage().toLowerCase().contains("inactive"));
    }

    /**
     * Test Step 5: Conflict Check (Position Occupied) Note: With Phase 4
     * polymorphic location model, occupancy checking happens during assignment, not
     * during barcode validation. Barcode validation focuses on format, existence,
     * hierarchy, and activity. Skipping this test as it's no longer applicable with
     * the new data model.
     */
    @Test
    public void testStep5ConflictCheck_NotApplicableWithPolymorphicModel() {
        // This test is deprecated - conflict checking happens during assignment, not
        // validation
        // Barcode validation validates that the barcode is well-formed and points to
        // valid locations
        assertTrue("Test skipped - conflict check moved to assignment phase", true);
    }

    /**
     * Test error messages for each failure type Expected: Each failure type has
     * specific, user-friendly error message
     */
    @Test
    public void testErrorMessagesForEachFailureType() {
        // Step 1: Format error
        ParsedBarcode invalidParsed = new ParsedBarcode();
        invalidParsed.setValid(false);
        invalidParsed.setErrorMessage("Invalid format");
        when(barcodeParsingService.parseBarcode("INVALID")).thenReturn(invalidParsed);
        BarcodeValidationResponse formatError = barcodeValidationService.validateBarcode("INVALID");
        assertTrue("Format error message should be descriptive", formatError.getErrorMessage().length() > 10);

        // Step 2: Not found error
        when(barcodeParsingService.parseBarcode("MAIN-FRZ01")).thenReturn(validParsedBarcode);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(null);
        BarcodeValidationResponse notFoundError = barcodeValidationService.validateBarcode("MAIN-FRZ01");
        assertTrue("Not found error message should be descriptive", notFoundError.getErrorMessage().length() > 10);

        // Step 4: Inactive error
        testRoom.setActive(false);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(testRoom);
        BarcodeValidationResponse inactiveError = barcodeValidationService.validateBarcode("MAIN-FRZ01");
        assertTrue("Inactive error message should be descriptive", inactiveError.getErrorMessage().length() > 10);
    }

    /**
     * Test partial validation with valid components Expected: Returns valid
     * components even if full validation fails
     */
    @Test
    public void testPartialValidationWithValidComponents() {
        // Arrange - Barcode with only 2 valid levels (room and device exist, but shelf
        // doesn't)
        String barcode = "MAIN-FRZ01-NONEXISTENT";
        ParsedBarcode parsedBarcode = new ParsedBarcode();
        parsedBarcode.setValid(true);
        parsedBarcode.setRoomCode("MAIN");
        parsedBarcode.setDeviceCode("FRZ01");
        parsedBarcode.setShelfCode("NONEXISTENT");

        when(barcodeParsingService.parseBarcode(barcode)).thenReturn(parsedBarcode);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(testRoom);
        when(storageDeviceDAO.findByCode("FRZ01")).thenReturn(testDevice); // Device exists
        when(storageDeviceDAO.findByCodeAndParentRoom("FRZ01", testRoom)).thenReturn(testDevice);
        when(storageShelfDAO.findByLabel("NONEXISTENT")).thenReturn(null); // Shelf doesn't exist

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(barcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertFalse("Overall validation should fail", response.isValid());
        assertNotNull("Valid components should be populated", response.getValidComponents());
        assertEquals("Should have 2 valid components", 2, response.getValidComponents().size());
        assertTrue("Should include valid room", response.getValidComponents().containsKey("room"));
        assertTrue("Should include valid device", response.getValidComponents().containsKey("device"));
        assertFalse("Should not include invalid shelf", response.getValidComponents().containsKey("shelf"));
    }

    /**
     * Test pre-fill valid components in response Expected: Response includes IDs
     * and names of valid components for form pre-filling
     */
    @Test
    public void testPreFillValidComponentsInResponse() {
        // Arrange - Valid 2-level barcode
        String barcode = "MAIN-FRZ01";
        ParsedBarcode parsedBarcode = new ParsedBarcode();
        parsedBarcode.setValid(true);
        parsedBarcode.setRoomCode("MAIN");
        parsedBarcode.setDeviceCode("FRZ01");

        when(barcodeParsingService.parseBarcode(barcode)).thenReturn(parsedBarcode);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(testRoom);
        when(storageDeviceDAO.findByCode("FRZ01")).thenReturn(testDevice); // Device exists
        when(storageDeviceDAO.findByCodeAndParentRoom("FRZ01", testRoom)).thenReturn(testDevice);

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(barcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertTrue("Validation should succeed for valid 2-level barcode", response.isValid());
        assertNotNull("Valid components should be populated", response.getValidComponents());

        // Verify room component
        assertTrue("Should have room component", response.getValidComponents().containsKey("room"));
        Object roomComponent = response.getValidComponents().get("room");
        assertNotNull("Room component should not be null", roomComponent);

        // Verify device component
        assertTrue("Should have device component", response.getValidComponents().containsKey("device"));
        Object deviceComponent = response.getValidComponents().get("device");
        assertNotNull("Device component should not be null", deviceComponent);
    }

    /**
     * Test successful validation of complete 5-level barcode Expected: All 5 steps
     * pass, response is valid
     */
    @Test
    public void testSuccessfulValidationComplete5LevelBarcode() {
        // Arrange
        String barcode = "MAIN-FRZ01-SHA-RKR1-A5";
        when(barcodeParsingService.parseBarcode(barcode)).thenReturn(validParsedBarcode);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(testRoom);
        when(storageDeviceDAO.findByCode("FRZ01")).thenReturn(testDevice);
        when(storageDeviceDAO.findByCodeAndParentRoom("FRZ01", testRoom)).thenReturn(testDevice);
        when(storageShelfDAO.findByLabel("SHA")).thenReturn(testShelf);
        when(storageShelfDAO.findByLabelAndParentDevice("SHA", testDevice)).thenReturn(testShelf);
        when(storageRackDAO.findByLabel("RKR1")).thenReturn(testRack);
        when(storageRackDAO.findByLabelAndParentShelf("RKR1", testShelf)).thenReturn(testRack);
        when(storagePositionDAO.findByCoordinates("A5")).thenReturn(testPosition);
        when(storagePositionDAO.findByCoordinatesAndParentRack("A5", testRack)).thenReturn(testPosition);
        // Note: Occupancy checking removed - done during assignment, not validation

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(barcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertTrue("Validation should succeed for valid barcode", response.isValid());
        assertNull("Failed step should be null on success", response.getFailedStep());
        assertNull("Error message should be null on success", response.getErrorMessage());
        assertNotNull("Valid components should be populated", response.getValidComponents());
        assertEquals("Should have all 5 components", 5, response.getValidComponents().size());
    }

    /**
     * Test successful validation of 2-level barcode (minimum requirement) Expected:
     * Validation passes with minimum 2 levels
     */
    @Test
    public void testSuccessfulValidation2LevelBarcode() {
        // Arrange
        String barcode = "MAIN-FRZ01";
        ParsedBarcode parsedBarcode = new ParsedBarcode();
        parsedBarcode.setValid(true);
        parsedBarcode.setRoomCode("MAIN");
        parsedBarcode.setDeviceCode("FRZ01");

        when(barcodeParsingService.parseBarcode(barcode)).thenReturn(parsedBarcode);
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(testRoom);
        when(storageDeviceDAO.findByCode("FRZ01")).thenReturn(testDevice);
        when(storageDeviceDAO.findByCodeAndParentRoom("FRZ01", testRoom)).thenReturn(testDevice);

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(barcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertTrue("Validation should succeed for valid 2-level barcode", response.isValid());
        assertNotNull("Valid components should be populated", response.getValidComponents());
        assertEquals("Should have 2 components", 2, response.getValidComponents().size());
    }

    /**
     * Test barcode type detection: Location barcode (hierarchical format) T250:
     * Dual Barcode Auto-Detection - Location format
     */
    @Test
    public void testDetectLocationBarcode() {
        // Arrange
        String locationBarcode = "MAIN-FRZ01-SHA-RKR1";

        // Mock parsing service to return valid parsed barcode
        ParsedBarcode parsed = new ParsedBarcode();
        parsed.setValid(true);
        parsed.setRoomCode("MAIN");
        parsed.setDeviceCode("FRZ01");
        parsed.setShelfCode("SHA");
        parsed.setRackCode("RKR1");
        when(barcodeParsingService.parseBarcode(locationBarcode)).thenReturn(parsed);

        // Mock DAOs to return valid entities
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(testRoom);
        when(storageDeviceDAO.findByCode("FRZ01")).thenReturn(testDevice);
        when(storageDeviceDAO.findByCodeAndParentRoom("FRZ01", testRoom)).thenReturn(testDevice);
        when(storageShelfDAO.findByLabel("SHA")).thenReturn(testShelf);
        when(storageShelfDAO.findByLabelAndParentDevice("SHA", testDevice)).thenReturn(testShelf);
        when(storageRackDAO.findByLabel("RKR1")).thenReturn(testRack);
        when(storageRackDAO.findByLabelAndParentShelf("RKR1", testShelf)).thenReturn(testRack);

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(locationBarcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Barcode type should be 'location'", "location", response.getBarcodeType());
    }

    /**
     * Test barcode type detection: Sample barcode (accession number format) T250:
     * Dual Barcode Auto-Detection - Sample format
     */
    @Test
    public void testDetectSampleBarcode() {
        // Arrange - Sample barcode formats: YY-XXXXX, S-YYYY-NNNNN, etc.
        String sampleBarcode = "25-00001"; // Year-based format

        // Mock parsing service to return invalid parse (not hierarchical format)
        ParsedBarcode parsed = new ParsedBarcode();
        parsed.setValid(false);
        parsed.setErrorMessage("Invalid barcode format");
        when(barcodeParsingService.parseBarcode(sampleBarcode)).thenReturn(parsed);

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(sampleBarcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Barcode type should be 'sample'", "sample", response.getBarcodeType());
    }

    /**
     * Test barcode type detection: Unknown format T250: Dual Barcode Auto-Detection
     * - Unknown format
     */
    @Test
    public void testUnknownBarcodeType() {
        // Arrange - Invalid format that doesn't match location or sample patterns
        String unknownBarcode = "INVALID-CODE-123";

        // Mock parsing service to return invalid parse
        ParsedBarcode parsed = new ParsedBarcode();
        parsed.setValid(false);
        parsed.setErrorMessage("Invalid barcode format");
        when(barcodeParsingService.parseBarcode(unknownBarcode)).thenReturn(parsed);

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(unknownBarcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Barcode type should be 'unknown'", "unknown", response.getBarcodeType());
    }

    /**
     * Test error message format per FR-024g specification T250: Error Message
     * Format - Includes raw barcode and parsed components
     */
    @Test
    public void testErrorMessageFormatIncludesRawAndParsed() {
        // Arrange - Barcode with valid format but invalid hierarchy (rack not found)
        String barcode = "MAIN-FRZ01-SHA-RKR1";

        // Mock parsing service to return valid parsed barcode
        ParsedBarcode parsed = new ParsedBarcode();
        parsed.setValid(true);
        parsed.setRoomCode("MAIN");
        parsed.setDeviceCode("FRZ01");
        parsed.setShelfCode("SHA");
        parsed.setRackCode("RKR1");
        when(barcodeParsingService.parseBarcode(barcode)).thenReturn(parsed);

        // Mock DAOs - room, device, shelf exist, but rack doesn't
        when(storageRoomDAO.findByCode("MAIN")).thenReturn(testRoom);
        when(storageDeviceDAO.findByCode("FRZ01")).thenReturn(testDevice);
        when(storageDeviceDAO.findByCodeAndParentRoom("FRZ01", testRoom)).thenReturn(testDevice);
        when(storageShelfDAO.findByLabel("SHA")).thenReturn(testShelf);
        when(storageShelfDAO.findByLabelAndParentDevice("SHA", testDevice)).thenReturn(testShelf);
        when(storageRackDAO.findByLabel("RKR1")).thenReturn(null); // Rack not found

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(barcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertFalse("Validation should fail", response.isValid());
        assertNotNull("Error message should not be null", response.getErrorMessage());

        // Verify error message format: "Scanned code: {barcode} ({parsed components}).
        // {specific error}"
        String errorMessage = response.getErrorMessage();
        assertTrue("Error message should start with 'Scanned code: '",
                errorMessage.startsWith("Scanned code: " + barcode));
        assertTrue("Error message should include parsed components", errorMessage.contains("Room: MAIN"));
        assertTrue("Error message should include parsed components", errorMessage.contains("Device: FRZ01"));
        assertTrue("Error message should include parsed components", errorMessage.contains("Shelf: SHA"));
        assertTrue("Error message should include parsed components", errorMessage.contains("Rack: RKR1"));
        assertTrue("Error message should include specific error about rack not found",
                errorMessage.contains("Rack not found") || errorMessage.contains("RKR1"));
    }

    /**
     * Test error message format when parsing fails T250: Error Message Format -
     * Parsing failure shows only raw barcode
     */
    @Test
    public void testErrorMessageFormatWhenParsingFails() {
        // Arrange - Invalid barcode format
        String invalidBarcode = "INVALID-CODE-123";

        // Mock parsing service to return invalid parse
        ParsedBarcode parsed = new ParsedBarcode();
        parsed.setValid(false);
        parsed.setErrorMessage("Invalid barcode format");
        when(barcodeParsingService.parseBarcode(invalidBarcode)).thenReturn(parsed);

        // Act
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(invalidBarcode);

        // Assert
        assertNotNull("Response should not be null", response);
        assertFalse("Validation should fail", response.isValid());
        assertNotNull("Error message should not be null", response.getErrorMessage());

        // Verify error message format: "Scanned code: {barcode}. {error}"
        String errorMessage = response.getErrorMessage();
        assertTrue("Error message should start with 'Scanned code: '",
                errorMessage.startsWith("Scanned code: " + invalidBarcode));
        assertTrue("Error message should not include parsed components when parsing fails",
                !errorMessage.contains("Room:") && !errorMessage.contains("Device:"));
        assertTrue("Error message should include format error",
                errorMessage.contains("Invalid barcode format") || errorMessage.contains("Invalid barcode format."));
    }
}
