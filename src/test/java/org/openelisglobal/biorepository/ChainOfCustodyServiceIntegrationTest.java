package org.openelisglobal.biorepository;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.ChainOfCustodyService;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
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
 * Integration tests for ChainOfCustodyService. Tests the immutable audit log
 * functionality for sample custody tracking.
 */
public class ChainOfCustodyServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ChainOfCustodyService custodyService;

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
    private Sample testSample;

    @Before
    public void setUp() {
        // Create test user (login_name max 20 chars, unique names)
        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
        testUser = new SystemUser();
        testUser.setLoginName("coc_" + suffix);
        testUser.setFirstName("Coc" + suffix);
        testUser.setLastName("Tst" + suffix);
        testUser.setIsActive("Y");
        testUser.setIsEmployee("Y");
        testUser.setSysUserId("1");
        testUser = systemUserService.save(testUser);

        var sampleTypes = typeOfSampleService.getAll();
        testSampleType = sampleTypes.isEmpty() ? createSampleType() : sampleTypes.get(0);

        testSample = createSample("COC" + (System.currentTimeMillis() % 100000000));
    }

    @Test
    public void testLogCustodyAction_CreatesImmutableEntry() {
        SampleItem sampleItem = createSampleItem("COC-LOG-" + System.currentTimeMillis());

        ChainOfCustodyLog log = custodyService.logCustodyAction(sampleItem, CustodyAction.CHECKOUT_REQUESTED, null,
                null, "Freezer-A/Shelf-1/Box-3/A1", testUser, "Storage", "Lab", new BigDecimal("-80.0"),
                "Request submitted", testUser.getId().toString());

        assertNotNull("Log entry should have ID", log.getId());
        assertEquals(CustodyAction.CHECKOUT_REQUESTED, log.getCustodyAction());
        assertEquals("Freezer-A/Shelf-1/Box-3/A1", log.getStorageCoordinates());
        assertEquals("Storage", log.getFromLocation());
        assertEquals("Lab", log.getToLocation());
        assertEquals(new BigDecimal("-80.0"), log.getTemperature());
        assertNotNull("Timestamp should be set", log.getActionTimestamp());
    }

    @Test
    public void testGetBySampleItemId_ReturnsChronologicalOrder() {
        SampleItem sampleItem = createSampleItem("COC-HIST-" + System.currentTimeMillis());
        Integer sampleItemId = Integer.valueOf(sampleItem.getId());

        // Create multiple custody entries
        custodyService.logCustodyAction(sampleItem, CustodyAction.CHECKOUT_REQUESTED, null, null, null, testUser, null,
                null, null, "First", testUser.getId().toString());
        custodyService.logCustodyAction(sampleItem, CustodyAction.CHECKOUT_APPROVED, null, null, null, testUser, null,
                null, null, "Second", testUser.getId().toString());
        custodyService.logCustodyAction(sampleItem, CustodyAction.CHECKOUT_RETRIEVED, null, null, "Location-A",
                testUser, "Storage", null, null, "Third", testUser.getId().toString());

        List<ChainOfCustodyLog> history = custodyService.getBySampleItemId(sampleItemId);

        assertEquals("Should have 3 log entries", 3, history.size());
        assertEquals(CustodyAction.CHECKOUT_REQUESTED, history.get(0).getCustodyAction());
        assertEquals(CustodyAction.CHECKOUT_APPROVED, history.get(1).getCustodyAction());
        assertEquals(CustodyAction.CHECKOUT_RETRIEVED, history.get(2).getCustodyAction());
    }

    @Test
    public void testMultipleLogs_DifferentActions() {
        SampleItem si1 = createSampleItem("COC-ACT1-" + System.currentTimeMillis());

        // Create multiple logs with different actions
        ChainOfCustodyLog log1 = custodyService.logCustodyAction(si1, CustodyAction.RETURN_STORED, null, null, null,
                testUser, null, null, null, "Return 1", testUser.getId().toString());
        ChainOfCustodyLog log2 = custodyService.logCustodyAction(si1, CustodyAction.CHECKOUT_REQUESTED, null, null,
                null, testUser, null, null, null, "Request", testUser.getId().toString());

        assertNotNull(log1.getId());
        assertNotNull(log2.getId());
        assertEquals(CustodyAction.RETURN_STORED, log1.getCustodyAction());
        assertEquals(CustodyAction.CHECKOUT_REQUESTED, log2.getCustodyAction());
    }

    @Test
    public void testGetRecentActions_RespectsLimit() {
        SampleItem sampleItem = createSampleItem("COC-REC-" + System.currentTimeMillis());

        for (int i = 0; i < 5; i++) {
            custodyService.logCustodyAction(sampleItem, CustodyAction.CHECKOUT_REQUESTED, null, null, null, testUser,
                    null, null, null, "Entry " + i, testUser.getId().toString());
        }

        List<ChainOfCustodyLog> recent = custodyService.getRecentActions(3);

        assertTrue("Should return at most 3 entries", recent.size() <= 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLogCustodyAction_NullSampleItem_Fails() {
        custodyService.logCustodyAction(null, CustodyAction.CHECKOUT_REQUESTED, null, null, null, testUser, null, null,
                null, "Should fail", testUser.getId().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLogCustodyAction_NullAction_Fails() {
        SampleItem sampleItem = createSampleItem("COC-NULL-" + System.currentTimeMillis());
        custodyService.logCustodyAction(sampleItem, null, null, null, null, testUser, null, null, null, "Should fail",
                testUser.getId().toString());
    }

    // ========== HELPER METHODS ==========

    private TypeOfSample createSampleType() {
        TypeOfSample type = new TypeOfSample();
        type.setDescription("Serum");
        type.setDomain("H");
        return typeOfSampleService.save(type);
    }

    private Sample createSample(String accession) {
        Sample sample = new Sample();
        sample.setAccessionNumber(accession);
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(testUser.getId().toString());
        return sampleService.save(sample);
    }

    private SampleItem createSampleItem(String externalId) {
        SampleItem item = new SampleItem();
        item.setSample(testSample);
        item.setExternalId(externalId);
        item.setTypeOfSample(testSampleType);
        item.setSortOrder("1");
        item.setQuantity(10.0);
        item.setCollectionDate(new Timestamp(System.currentTimeMillis()));
        item.setStatusId("1");
        item.setSysUserId(testUser.getId().toString());
        return sampleItemService.save(item);
    }
}
