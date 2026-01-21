package org.openelisglobal.biorepository;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.PackagingCondition;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for BioSampleService.
 *
 * BioSample is now an extension record for SampleItem, containing only
 * biorepository-specific metadata. Core sample data (barcode, type, quantity,
 * dates) is stored in Sample and SampleItem entities.
 *
 * Tests verify: - Creating BioSample extension for existing SampleItem -
 * Querying by SampleItem, Shipment, BiosafetyLevel - Ethics/MTA/PI search
 * capabilities
 */
public class BioSampleServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SystemUserService systemUserService;

    private SystemUser testUser;
    private TypeOfSample testSampleType;
    private Shipment testShipment;
    private Sample testSample;

    @Before
    public void setUp() {
        // Setup test user
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_biosample_user");
            testUser.setFirstName("Test");
            testUser.setLastName("BioSample User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }

        // Setup test sample type (using existing type or create new)
        var sampleTypes = typeOfSampleService.getAll();
        if (!sampleTypes.isEmpty()) {
            testSampleType = sampleTypes.get(0);
        } else {
            testSampleType = new TypeOfSample();
            testSampleType.setDescription("Blood");
            testSampleType.setDomain("H");
            typeOfSampleService.save(testSampleType);
        }

        // Setup test shipment
        testShipment = createTestShipment("BS-SHIP-" + (System.currentTimeMillis() % 1000000));

        // Setup test sample (accession level) - keep under 20 chars
        testSample = createTestSample("BS" + (System.currentTimeMillis() % 100000000));
    }

    // ========== CREATE FOR SAMPLE ITEM TESTS ==========

    @Test
    public void testCreateForSampleItem_Success() {
        // Arrange - Create SampleItem first
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-ITEM-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setEthicsApprovalRef("ETH-2026-001");
        bioSample.setRequiredTempMin(new BigDecimal("-80.0"));
        bioSample.setRequiredTempMax(new BigDecimal("-70.0"));
        bioSample.setSysUserId(testUser.getId().toString());

        // Act
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Assert
        assertNotNull("BioSample ID should be generated", created.getId());
        assertNotNull("SampleItem link should be set", created.getSampleItem());
        assertEquals("SampleItem ID should match", sampleItem.getId(), created.getSampleItem().getId());
        assertEquals("Biosafety level should be BSL_2", BiosafetyLevel.BSL_2, created.getBiosafetyLevel());
        assertEquals("Ethics approval should match", "ETH-2026-001", created.getEthicsApprovalRef());
        assertEquals("Min temp should match", new BigDecimal("-80.0"), created.getRequiredTempMin());
        assertEquals("Max temp should match", new BigDecimal("-70.0"), created.getRequiredTempMax());
    }

    @Test
    public void testCreateForSampleItem_WithAllFields() {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-FULL-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_3);
        bioSample.setEthicsApprovalRef("ETH-2026-002");
        bioSample.setMtaReference("MTA-2026-XYZ");
        bioSample.setConsentId("CONSENT-12345");
        bioSample.setPrincipalInvestigator("Dr. Jane Smith");
        bioSample.setRequiredTempMin(new BigDecimal("-196.0"));
        bioSample.setRequiredTempMax(new BigDecimal("-150.0"));
        bioSample.setPreservationMedium("Cryoprotectant");
        bioSample.setSpecialHandling("Handle with extreme care - biohazard");
        bioSample.setArrivalCondition("Frozen");
        bioSample.setArrivalConditionNotes("Arrived in good condition");
        bioSample.setShipment(testShipment);
        bioSample.setSysUserId(testUser.getId().toString());

        // Act
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Assert
        assertNotNull("BioSample ID should be generated", created.getId());
        assertEquals("Biosafety level should be BSL_3", BiosafetyLevel.BSL_3, created.getBiosafetyLevel());
        assertEquals("Ethics approval should match", "ETH-2026-002", created.getEthicsApprovalRef());
        assertEquals("MTA reference should match", "MTA-2026-XYZ", created.getMtaReference());
        assertEquals("Consent ID should match", "CONSENT-12345", created.getConsentId());
        assertEquals("PI should match", "Dr. Jane Smith", created.getPrincipalInvestigator());
        assertEquals("Min temp should match", new BigDecimal("-196.0"), created.getRequiredTempMin());
        assertEquals("Max temp should match", new BigDecimal("-150.0"), created.getRequiredTempMax());
        assertEquals("Preservation medium should match", "Cryoprotectant", created.getPreservationMedium());
        assertEquals("Special handling should match", "Handle with extreme care - biohazard",
                created.getSpecialHandling());
        assertEquals("Arrival condition should match", "Frozen", created.getArrivalCondition());
        assertNotNull("Shipment should be set", created.getShipment());
        assertEquals("Shipment ID should match", testShipment.getId(), created.getShipment().getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateForSampleItem_NullSampleItem_ThrowsException() {
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setSysUserId(testUser.getId().toString());

        // Act - should throw
        bioSampleService.createForSampleItem(null, bioSample);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateForSampleItem_DuplicateExtension_ThrowsException() {
        // Arrange - Create SampleItem and first BioSample extension
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-DUP-" + System.currentTimeMillis());

        BioSample bioSample1 = new BioSample();
        bioSample1.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample1.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample1);

        // Act - Try to create second extension for same SampleItem (should throw)
        BioSample bioSample2 = new BioSample();
        bioSample2.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample2.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample2);
    }

    // ========== QUERY BY SAMPLE ITEM TESTS ==========

    @Test
    public void testGetBySampleItemId_Found() {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-FIND-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        BioSample found = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem.getId()));

        // Assert
        assertNotNull("BioSample should be found", found);
        assertEquals("ID should match", created.getId(), found.getId());
        assertEquals("Biosafety level should match", BiosafetyLevel.BSL_2, found.getBiosafetyLevel());
    }

    @Test
    public void testGetBySampleItemId_NotFound() {
        // Act
        BioSample found = bioSampleService.getBySampleItemId(999999);

        // Assert
        assertNull("Should return null for non-existent SampleItem", found);
    }

    @Test
    public void testExistsBySampleItemId() {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-EXISTS-" + System.currentTimeMillis());

        // Assert - before creating extension
        assertFalse("Should not exist before creation",
                bioSampleService.existsBySampleItemId(Integer.valueOf(sampleItem.getId())));

        // Create extension
        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Assert - after creating extension
        assertTrue("Should exist after creation",
                bioSampleService.existsBySampleItemId(Integer.valueOf(sampleItem.getId())));
    }

    // ========== BIOSAFETY LEVEL TESTS ==========

    @Test
    public void testAllBiosafetyLevels_StoredCorrectly() {
        // Test all biosafety levels can be stored and retrieved
        for (BiosafetyLevel level : BiosafetyLevel.values()) {
            SampleItem sampleItem = createTestSampleItem(testSample,
                    "BIO-BSL-" + level.name() + "-" + System.currentTimeMillis());

            BioSample bioSample = new BioSample();
            bioSample.setBiosafetyLevel(level);
            bioSample.setSysUserId(testUser.getId().toString());

            BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);
            BioSample retrieved = bioSampleService.get(created.getId());

            assertEquals("Biosafety level " + level + " should persist correctly", level,
                    retrieved.getBiosafetyLevel());
        }
    }

    @Test
    public void testGetByBiosafetyLevel() {
        // Arrange - Create samples with specific biosafety level
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-BSL-QUERY-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_3);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> bsl3Samples = bioSampleService.getByBiosafetyLevel(BiosafetyLevel.BSL_3);

        // Assert
        assertFalse("Should find at least one BSL-3 sample", bsl3Samples.isEmpty());
        assertTrue("All results should be BSL-3",
                bsl3Samples.stream().allMatch(s -> BiosafetyLevel.BSL_3.equals(s.getBiosafetyLevel())));
    }

    @Test
    public void testCountByBiosafetyLevel() {
        // Arrange
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-COUNT-BSL-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_4);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        long count = bioSampleService.countByBiosafetyLevel(BiosafetyLevel.BSL_4);

        // Assert
        assertTrue("Should have at least 1 BSL-4 sample", count >= 1);
    }

    // ========== SHIPMENT QUERY TESTS ==========

    @Test
    public void testGetByShipmentId() {
        // Arrange
        SampleItem sampleItem1 = createTestSampleItem(testSample, "BIO-SHIP1-" + System.currentTimeMillis());
        SampleItem sampleItem2 = createTestSampleItem(testSample, "BIO-SHIP2-" + System.currentTimeMillis());

        BioSample bioSample1 = new BioSample();
        bioSample1.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample1.setShipment(testShipment);
        bioSample1.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem1, bioSample1);

        BioSample bioSample2 = new BioSample();
        bioSample2.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample2.setShipment(testShipment);
        bioSample2.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem2, bioSample2);

        // Act
        List<BioSample> shipmentSamples = bioSampleService.getByShipmentId(testShipment.getId());

        // Assert
        assertTrue("Should find at least 2 samples in shipment", shipmentSamples.size() >= 2);
    }

    @Test
    public void testCountByShipmentId() {
        // Arrange
        Shipment newShipment = createTestShipment("COUNT-SHIP-" + System.currentTimeMillis());

        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-SHIP-COUNT-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setShipment(newShipment);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        long count = bioSampleService.countByShipmentId(newShipment.getId());

        // Assert
        assertEquals("Should count 1 sample in new shipment", 1L, count);
    }

    // ========== ETHICS/MTA/PI SEARCH TESTS ==========

    @Test
    public void testGetByEthicsApprovalRef() {
        // Arrange
        String uniqueEthicsRef = "ETH-UNIQUE-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-ETH-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setEthicsApprovalRef(uniqueEthicsRef);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> found = bioSampleService.getByEthicsApprovalRef(uniqueEthicsRef);

        // Assert
        assertEquals("Should find exactly 1 sample with this ethics ref", 1, found.size());
        assertEquals("Ethics ref should match", uniqueEthicsRef, found.get(0).getEthicsApprovalRef());
    }

    @Test
    public void testGetByMtaReference() {
        // Arrange
        String uniqueMtaRef = "MTA-UNIQUE-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-MTA-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setMtaReference(uniqueMtaRef);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> found = bioSampleService.getByMtaReference(uniqueMtaRef);

        // Assert
        assertEquals("Should find exactly 1 sample with this MTA ref", 1, found.size());
        assertEquals("MTA ref should match", uniqueMtaRef, found.get(0).getMtaReference());
    }

    @Test
    public void testGetByPrincipalInvestigator() {
        // Arrange
        String uniquePI = "Dr. Unique PI " + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-PI-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setPrincipalInvestigator(uniquePI);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> found = bioSampleService.getByPrincipalInvestigator(uniquePI);

        // Assert
        assertEquals("Should find exactly 1 sample with this PI", 1, found.size());
        assertEquals("PI should match", uniquePI, found.get(0).getPrincipalInvestigator());
    }

    // ========== SAMPLE ITEM RELATIONSHIP TESTS ==========

    @Test
    public void testSampleItemRelationship_Persisted() {
        // Arrange
        String externalId = "EXT-ID-" + System.currentTimeMillis();
        SampleItem sampleItem = createTestSampleItem(testSample, externalId);

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act - Fetch and verify SampleItem relationship
        BioSample retrieved = bioSampleService.get(created.getId());

        // Assert - SampleItem relationship is persisted
        assertNotNull("SampleItem relationship should be persisted", retrieved.getSampleItem());
        assertEquals("SampleItem ID should match", sampleItem.getId(), retrieved.getSampleItem().getId());

        // Verify we can fetch by SampleItem ID
        BioSample foundBySampleItem = bioSampleService.getBySampleItemId(Integer.valueOf(sampleItem.getId()));
        assertNotNull("Should find BioSample by SampleItem ID", foundBySampleItem);
        assertEquals("Found BioSample should match", created.getId(), foundBySampleItem.getId());
    }

    // ========== GET BY SAMPLE ITEM IDS TESTS ==========

    @Test
    public void testGetBySampleItemIds_MultipleSamples_ReturnsAll() {
        // Arrange - Create 3 sample items with BioSample extensions
        SampleItem sampleItem1 = createTestSampleItem(testSample, "BIO-MULTI-1-" + System.currentTimeMillis());
        SampleItem sampleItem2 = createTestSampleItem(testSample, "BIO-MULTI-2-" + System.currentTimeMillis());
        SampleItem sampleItem3 = createTestSampleItem(testSample, "BIO-MULTI-3-" + System.currentTimeMillis());

        BioSample bioSample1 = new BioSample();
        bioSample1.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample1.setRequiredTempMin(new BigDecimal("-20.0"));
        bioSample1.setRequiredTempMax(new BigDecimal("-15.0"));
        bioSample1.setSysUserId(testUser.getId().toString());
        BioSample created1 = bioSampleService.createForSampleItem(sampleItem1, bioSample1);

        BioSample bioSample2 = new BioSample();
        bioSample2.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample2.setRequiredTempMin(new BigDecimal("-80.0"));
        bioSample2.setRequiredTempMax(new BigDecimal("-70.0"));
        bioSample2.setSysUserId(testUser.getId().toString());
        BioSample created2 = bioSampleService.createForSampleItem(sampleItem2, bioSample2);

        BioSample bioSample3 = new BioSample();
        bioSample3.setBiosafetyLevel(BiosafetyLevel.BSL_3);
        bioSample3.setRequiredTempMin(new BigDecimal("-196.0"));
        bioSample3.setRequiredTempMax(new BigDecimal("-150.0"));
        bioSample3.setSysUserId(testUser.getId().toString());
        BioSample created3 = bioSampleService.createForSampleItem(sampleItem3, bioSample3);

        // Act
        List<Integer> sampleItemIds = List.of(Integer.valueOf(sampleItem1.getId()),
                Integer.valueOf(sampleItem2.getId()), Integer.valueOf(sampleItem3.getId()));
        List<BioSample> found = bioSampleService.getBySampleItemIds(sampleItemIds);

        // Assert
        assertEquals("Should find all 3 samples", 3, found.size());

        // Verify temperature requirements are loaded (ensures relationships are
        // fetched)
        // Use compareTo for BigDecimal as assertEquals uses equals() which checks scale
        BioSample foundSample1 = found.stream().filter(s -> s.getId().equals(created1.getId())).findFirst()
                .orElse(null);
        assertNotNull("Should find sample 1", foundSample1);
        assertTrue("Sample 1 temp min should match",
                new BigDecimal("-20.0").compareTo(foundSample1.getRequiredTempMin()) == 0);
        assertTrue("Sample 1 temp max should match",
                new BigDecimal("-15.0").compareTo(foundSample1.getRequiredTempMax()) == 0);

        BioSample foundSample2 = found.stream().filter(s -> s.getId().equals(created2.getId())).findFirst()
                .orElse(null);
        assertNotNull("Should find sample 2", foundSample2);
        assertTrue("Sample 2 temp min should match",
                new BigDecimal("-80.0").compareTo(foundSample2.getRequiredTempMin()) == 0);

        BioSample foundSample3 = found.stream().filter(s -> s.getId().equals(created3.getId())).findFirst()
                .orElse(null);
        assertNotNull("Should find sample 3", foundSample3);
        assertTrue("Sample 3 temp min should match",
                new BigDecimal("-196.0").compareTo(foundSample3.getRequiredTempMin()) == 0);
    }

    @Test
    public void testGetBySampleItemIds_EmptyList_ReturnsEmpty() {
        // Act
        List<BioSample> found = bioSampleService.getBySampleItemIds(List.of());

        // Assert
        assertNotNull("Should return empty list, not null", found);
        assertTrue("Should return empty list for empty input", found.isEmpty());
    }

    @Test
    public void testGetBySampleItemIds_NullList_ReturnsEmpty() {
        // Act
        List<BioSample> found = bioSampleService.getBySampleItemIds(null);

        // Assert
        assertNotNull("Should return empty list, not null", found);
        assertTrue("Should return empty list for null input", found.isEmpty());
    }

    @Test
    public void testGetBySampleItemIds_NonExistentIds_ReturnsEmpty() {
        // Act - Use IDs that don't exist
        List<BioSample> found = bioSampleService.getBySampleItemIds(List.of(999998, 999999));

        // Assert
        assertNotNull("Should return empty list, not null", found);
        assertTrue("Should return empty list for non-existent IDs", found.isEmpty());
    }

    @Test
    public void testGetBySampleItemIds_MixedValidInvalid_ReturnsOnlyValid() {
        // Arrange - Create one sample item with BioSample extension
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-MIXED-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setRequiredTempMin(new BigDecimal("-80.0"));
        bioSample.setRequiredTempMax(new BigDecimal("-75.0"));
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act - Include valid ID and invalid IDs
        List<Integer> sampleItemIds = List.of(Integer.valueOf(sampleItem.getId()), 999998, 999999);
        List<BioSample> found = bioSampleService.getBySampleItemIds(sampleItemIds);

        // Assert
        assertEquals("Should find only 1 valid sample", 1, found.size());
        assertEquals("Should be the correct sample", created.getId(), found.get(0).getId());
        assertTrue("Temperature requirements should be loaded",
                new BigDecimal("-80.0").compareTo(found.get(0).getRequiredTempMin()) == 0);
    }

    @Test
    public void testGetBySampleItemIds_RelationshipsEagerlyLoaded() {
        // Arrange - Create sample with all relationships
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-EAGER-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setShipment(testShipment);
        bioSample.setRequiredTempMin(new BigDecimal("-80.0"));
        bioSample.setRequiredTempMax(new BigDecimal("-75.0"));
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> found = bioSampleService.getBySampleItemIds(List.of(Integer.valueOf(sampleItem.getId())));

        // Assert - Verify all relationships are loaded (no LazyInitializationException)
        assertEquals("Should find 1 sample", 1, found.size());
        BioSample result = found.get(0);

        // Accessing relationships should not throw LazyInitializationException
        assertNotNull("SampleItem should be loaded", result.getSampleItem());
        assertNotNull("SampleItem externalId should be accessible", result.getSampleItem().getExternalId());
        assertNotNull("Sample should be loaded via SampleItem", result.getSampleItem().getSample());
        assertNotNull("Sample accession number should be accessible",
                result.getSampleItem().getSample().getAccessionNumber());
        assertNotNull("TypeOfSample should be loaded", result.getSampleItem().getTypeOfSample());
        assertNotNull("Shipment should be loaded", result.getShipment());
        assertEquals("Shipment ID should match", testShipment.getId(), result.getShipment().getId());
    }

    // ========== RETENTION EXPIRY / DISPOSAL DASHBOARD TESTS ==========

    @Test
    public void testGetExpiredSamples_ReturnsSamplesWithPastExpiryDate() {
        // Arrange - Create sample with expiry date in the past
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-EXPIRED-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        // Set expiry date 30 days in the past
        java.time.LocalDate pastDate = java.time.LocalDate.now().minusDays(30);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(pastDate));
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> expired = bioSampleService.getExpiredSamples();

        // Assert
        assertNotNull("Should return a list", expired);
        assertTrue("Should find at least 1 expired sample", expired.size() >= 1);
        assertTrue("Should contain the created expired sample",
                expired.stream().anyMatch(s -> s.getId().equals(created.getId())));

        // Verify the sample is actually expired
        BioSample foundExpired = expired.stream().filter(s -> s.getId().equals(created.getId())).findFirst()
                .orElse(null);
        assertNotNull("Should find the expired sample", foundExpired);
        assertTrue("Expiry date should be in the past",
                foundExpired.getRetentionExpiryDate().toLocalDate().isBefore(java.time.LocalDate.now()));
    }

    @Test
    public void testGetExpiredSamples_ExcludesDisposedSamples() {
        // Arrange - Create sample with past expiry date but marked as disposed
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-DISPOSED-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        java.time.LocalDate pastDate = java.time.LocalDate.now().minusDays(30);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(pastDate));
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.DISPOSED);
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> expired = bioSampleService.getExpiredSamples();

        // Assert - Disposed sample should NOT be in the list
        assertFalse("Should not contain disposed sample",
                expired.stream().anyMatch(s -> s.getId().equals(created.getId())));
    }

    @Test
    public void testGetExpiringSamples_ReturnsSamplesExpiringWithinWindow() {
        // Arrange - Create sample expiring in 15 days
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-EXPIRING-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        // Set expiry date 15 days in the future
        java.time.LocalDate futureDate = java.time.LocalDate.now().plusDays(15);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(futureDate));
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act - Get samples expiring within 30 days
        List<BioSample> expiring30 = bioSampleService.getExpiringSamples(30);

        // Assert
        assertNotNull("Should return a list", expiring30);
        assertTrue("Should find at least 1 expiring sample within 30 days", expiring30.size() >= 1);
        assertTrue("Should contain the created expiring sample",
                expiring30.stream().anyMatch(s -> s.getId().equals(created.getId())));

        // Verify the sample has correct expiry date
        BioSample foundExpiring = expiring30.stream().filter(s -> s.getId().equals(created.getId())).findFirst()
                .orElse(null);
        assertNotNull("Should find the expiring sample", foundExpiring);
        assertEquals("Expiry date should match", futureDate, foundExpiring.getRetentionExpiryDate().toLocalDate());
    }

    @Test
    public void testGetExpiringSamples_ExcludesSamplesOutsideWindow() {
        // Arrange - Create sample expiring in 90 days (outside 30-day window)
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-FAR-EXP-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        // Set expiry date 90 days in the future
        java.time.LocalDate futureDate = java.time.LocalDate.now().plusDays(90);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(futureDate));
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act - Get samples expiring within 30 days
        List<BioSample> expiring30 = bioSampleService.getExpiringSamples(30);

        // Assert - Sample expiring in 90 days should NOT be in the 30-day list
        assertFalse("Should not contain sample expiring in 90 days",
                expiring30.stream().anyMatch(s -> s.getId().equals(created.getId())));

        // But should be in 100-day window
        List<BioSample> expiring100 = bioSampleService.getExpiringSamples(100);
        assertTrue("Should contain sample in 100-day window",
                expiring100.stream().anyMatch(s -> s.getId().equals(created.getId())));
    }

    @Test
    public void testGetExpiringSamples_ExcludesAlreadyExpiredSamples() {
        // Arrange - Create sample with past expiry date
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-PAST-EXP-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        // Set expiry date 10 days in the past
        java.time.LocalDate pastDate = java.time.LocalDate.now().minusDays(10);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(pastDate));
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act - Get samples expiring within 30 days
        List<BioSample> expiring30 = bioSampleService.getExpiringSamples(30);

        // Assert - Already expired sample should NOT be in expiring list
        assertFalse("Should not contain already expired sample",
                expiring30.stream().anyMatch(s -> s.getId().equals(created.getId())));
    }

    @Test
    public void testGetSamplesForDisposalDashboard_ReturnsAllSamplesWithRetentionData() {
        // Arrange - Create samples with different retention dates
        SampleItem sampleItem1 = createTestSampleItem(testSample, "BIO-DASH-1-" + System.currentTimeMillis());
        SampleItem sampleItem2 = createTestSampleItem(testSample, "BIO-DASH-2-" + System.currentTimeMillis());

        BioSample bioSample1 = new BioSample();
        bioSample1.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample1.setRetentionExpiryDate(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(30))); // Expired
        bioSample1.setSysUserId(testUser.getId().toString());
        BioSample created1 = bioSampleService.createForSampleItem(sampleItem1, bioSample1);

        BioSample bioSample2 = new BioSample();
        bioSample2.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample2.setRetentionExpiryDate(java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(30))); // Expiring
        bioSample2.setSysUserId(testUser.getId().toString());
        BioSample created2 = bioSampleService.createForSampleItem(sampleItem2, bioSample2);

        // Act
        List<BioSample> dashboard = bioSampleService.getSamplesForDisposalDashboard();

        // Assert
        assertNotNull("Should return a list", dashboard);
        assertTrue("Should find at least 2 samples", dashboard.size() >= 2);
        assertTrue("Should contain expired sample",
                dashboard.stream().anyMatch(s -> s.getId().equals(created1.getId())));
        assertTrue("Should contain expiring sample",
                dashboard.stream().anyMatch(s -> s.getId().equals(created2.getId())));
    }

    @Test
    public void testGetSamplesForDisposalDashboard_ExcludesDisposedSamples() {
        // Arrange - Create disposed sample
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-DASH-DISP-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(30)));
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.DISPOSED);
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> dashboard = bioSampleService.getSamplesForDisposalDashboard();

        // Assert - Disposed sample should NOT be in dashboard
        assertFalse("Should not contain disposed sample",
                dashboard.stream().anyMatch(s -> s.getId().equals(created.getId())));
    }

    @Test
    public void testGetSamplesForDisposalDashboard_ExcludesSamplesWithoutRetentionDate() {
        // Arrange - Create sample without retention expiry date
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-NO-RET-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        // Don't set retentionExpiryDate
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> dashboard = bioSampleService.getSamplesForDisposalDashboard();

        // Assert - Sample without retention date should NOT be in dashboard
        assertFalse("Should not contain sample without retention date",
                dashboard.stream().anyMatch(s -> s.getId().equals(created.getId())));
    }

    @Test
    public void testGetExpiringSamples_RelationshipsEagerlyLoaded() {
        // Arrange - Create sample with relationships
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-EXP-REL-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(15)));
        bioSample.setShipment(testShipment);
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act
        List<BioSample> expiring = bioSampleService.getExpiringSamples(30);

        // Assert - Verify relationships are loaded (no LazyInitializationException)
        BioSample found = expiring.stream().filter(s -> s.getId().equals(created.getId())).findFirst().orElse(null);
        assertNotNull("Should find the sample", found);
        assertNotNull("SampleItem should be loaded", found.getSampleItem());
        assertNotNull("SampleItem externalId should be accessible", found.getSampleItem().getExternalId());
        assertNotNull("Sample should be loaded", found.getSampleItem().getSample());
        assertNotNull("TypeOfSample should be loaded", found.getSampleItem().getTypeOfSample());
        assertNotNull("Shipment should be loaded", found.getShipment());
    }

    // ========== DISPOSAL SERVICE TESTS ==========

    @Test
    public void testDisposeBioSample_UpdatesWorkflowStatusToDisposed() {
        // Arrange - Create sample with BioSample extension
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-DISP-1-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_2);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(30)));
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Verify initial state
        assertEquals("Initial status should be STORED", BioSample.WorkflowStatus.STORED, created.getWorkflowStatus());

        // Act - Dispose the sample
        java.util.Map<String, Object> result = bioSampleService.disposeBioSample(sampleItem.getId(),
                "RETENTION_EXPIRED", "AUTOCLAVE", "Test disposal notes");

        // Assert - Verify the result
        assertNotNull("Should return a result", result);
        assertEquals("Should indicate disposed status", "disposed", result.get("status"));

        // Verify BioSample workflowStatus is updated
        BioSample updated = bioSampleService.get(created.getId());
        assertEquals("BioSample workflowStatus should be DISPOSED", BioSample.WorkflowStatus.DISPOSED,
                updated.getWorkflowStatus());
    }

    @Test
    public void testDisposeBioSample_UpdatesSampleItemStatusToDisposed() {
        // Arrange - Create sample with BioSample extension
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-DISP-2-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Verify initial SampleItem status is not "24" (disposed)
        assertNotEquals("Initial SampleItem status should not be 24", "24", sampleItem.getStatusId());

        // Act - Dispose the sample
        bioSampleService.disposeBioSample(sampleItem.getId(), "QUALITY_FAILED", "INCINERATION", null);

        // Assert - Verify SampleItem status is updated to "24" (disposed)
        SampleItem updatedSampleItem = sampleItemService.get(sampleItem.getId());
        assertEquals("SampleItem statusId should be 24 (disposed)", "24", updatedSampleItem.getStatusId());
    }

    @Test
    public void testDisposeBioSample_ReturnsDisposalResultWithRequiredFields() {
        // Arrange - Create sample with BioSample extension
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-DISP-3-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setSysUserId(testUser.getId().toString());
        bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Act - Dispose the sample
        java.util.Map<String, Object> result = bioSampleService.disposeBioSample(sampleItem.getId(), "DAMAGED",
                "CHEMICAL_NEUTRALIZATION", "Sample was compromised");

        // Assert - Verify the result contains expected fields
        assertNotNull("Should return a result", result);
        assertNotNull("Should have sampleItemId", result.get("sampleItemId"));
        assertEquals("Should have status as disposed", "disposed", result.get("status"));
        assertNotNull("Should have disposedDate", result.get("disposedDate"));
        assertEquals("Should have correct reason", "DAMAGED", result.get("reason"));
        assertEquals("Should have correct method", "CHEMICAL_NEUTRALIZATION", result.get("method"));
        assertEquals("Should have correct notes", "Sample was compromised", result.get("notes"));
    }

    @Test
    public void testDisposeBioSample_DisposedSampleExcludedFromExpiringSamplesQuery() {
        // Arrange - Create sample expiring within 30 days
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-DISP-4-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(15)));
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Verify sample appears in expiring list before disposal
        List<BioSample> expiringBefore = bioSampleService.getExpiringSamples(30);
        assertTrue("Sample should be in expiring list before disposal",
                expiringBefore.stream().anyMatch(s -> s.getId().equals(created.getId())));

        // Act - Dispose the sample
        bioSampleService.disposeBioSample(sampleItem.getId(), "CONSENT_WITHDRAWN", "AUTOCLAVE", null);

        // Assert - Disposed sample should no longer appear in expiring list
        List<BioSample> expiringAfter = bioSampleService.getExpiringSamples(30);
        assertFalse("Disposed sample should NOT be in expiring list",
                expiringAfter.stream().anyMatch(s -> s.getId().equals(created.getId())));
    }

    @Test
    public void testDisposeBioSample_DisposedSampleExcludedFromDashboard() {
        // Arrange - Create sample with retention expiry date
        SampleItem sampleItem = createTestSampleItem(testSample, "BIO-DISP-5-" + System.currentTimeMillis());

        BioSample bioSample = new BioSample();
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setRetentionExpiryDate(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(10)));
        bioSample.setSysUserId(testUser.getId().toString());
        BioSample created = bioSampleService.createForSampleItem(sampleItem, bioSample);

        // Verify sample appears in dashboard before disposal
        List<BioSample> dashboardBefore = bioSampleService.getSamplesForDisposalDashboard();
        assertTrue("Sample should be in dashboard before disposal",
                dashboardBefore.stream().anyMatch(s -> s.getId().equals(created.getId())));

        // Act - Dispose the sample
        bioSampleService.disposeBioSample(sampleItem.getId(), "RETENTION_EXPIRED", "AUTOCLAVE", null);

        // Assert - Disposed sample should no longer appear in dashboard
        List<BioSample> dashboardAfter = bioSampleService.getSamplesForDisposalDashboard();
        assertFalse("Disposed sample should NOT be in dashboard",
                dashboardAfter.stream().anyMatch(s -> s.getId().equals(created.getId())));
    }

    // ========== HELPER METHODS ==========

    private Sample createTestSample(String accessionNumber) {
        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(testUser.getId().toString());
        return sampleService.save(sample);
    }

    private SampleItem createTestSampleItem(Sample sample, String externalId) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setExternalId(externalId);
        sampleItem.setTypeOfSample(testSampleType);
        sampleItem.setSortOrder("1");
        sampleItem.setQuantity(10.0);
        sampleItem.setCollectionDate(new Timestamp(System.currentTimeMillis()));
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(testUser.getId().toString());
        return sampleItemService.save(sampleItem);
    }

    private Shipment createTestShipment(String deliveryReference) {
        Shipment shipment = new Shipment();
        shipment.setDeliveryReference(deliveryReference);
        shipment.setSenderName("Test Sender");
        shipment.setSenderOrganization("Test Organization");
        shipment.setReceiver(testUser);
        shipment.setPackagingCondition(PackagingCondition.INTACT);
        shipment.setReceptionTimestamp(new Timestamp(System.currentTimeMillis()));
        shipment.setSysUserId(testUser.getId().toString());
        return shipmentService.receiveShipment(shipment);
    }
}
