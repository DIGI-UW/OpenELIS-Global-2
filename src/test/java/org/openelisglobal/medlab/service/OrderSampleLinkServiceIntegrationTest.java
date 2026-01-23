/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.medlab.valueholder.OrderSampleLink;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

/**
 * Integration tests for OrderSampleLinkService. Tests the complete workflow of
 * linking samples to orders and verifying Analysis creation.
 */
public class OrderSampleLinkServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private OrderSampleLinkService orderSampleLinkService;

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private MedLabPatientOrderService medLabPatientOrderService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    // Test data IDs from medlab-patient-order-test-data.xml
    private static final String TEST_PATIENT_ID = "8001";
    private static final String TEST_TEST_ID_1 = "8001";
    private static final String TEST_TEST_ID_2 = "8002";
    private static final Integer TEST_NOTEBOOK_ENTRY_ID = 8001;
    private static final String TEST_USER_ID = "1";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/medlab-patient-order-test-data.xml");
    }

    /**
     * Tests that linkSampleToOrder creates Analysis records. This is the CRITICAL
     * missing functionality that causes samples to show "0 tests".
     *
     * Workflow: 1. Create sample via generic notebook workflow (no order yet) 2.
     * Create order separately 3. Link sample to order via linkSampleToOrder 4.
     * Verify Analysis records are created
     */
    @Test
    @Rollback
    public void testLinkSampleToOrder_CreatesAnalysisRecords() {
        // Given - Create order with 2 tests
        String labNo = "TEST-LINK-001";
        List<String> testIds = List.of(TEST_TEST_ID_1, TEST_TEST_ID_2);

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        // Get the created order
        ElectronicOrder order = electronicOrderService.getElectronicOrdersByExternalId(labNo).get(0);
        assertNotNull("Order should be created", order);

        // Given - Create sample via generic workflow (simulating generic notebook
        // collection)
        // This creates a sample WITHOUT an order (like VALID-003/004 were created)
        Sample sample = new Sample();
        sample.setAccessionNumber("SAMPLE-FOR-LINK-001");
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setStatusId("1"); // Entered status
        sample.setSysUserId(TEST_USER_ID);
        String sampleId = sampleService.insert(sample);
        assertNotNull("Sample should be created", sampleId);

        // Create SampleItem for the sample
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sampleService.get(sampleId));
        sampleItem.setTypeOfSample(typeOfSampleService.get("8001")); // Blood
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(TEST_USER_ID);
        String sampleItemId = sampleItemService.insert(sampleItem);
        assertNotNull("SampleItem should be created", sampleItemId);

        // Verify no Analysis records exist yet
        List<Analysis> analysesBeforeLink = analysisService.getAnalysesBySampleId(sampleId);
        assertTrue("Should have NO Analysis records before linking",
                analysesBeforeLink == null || analysesBeforeLink.isEmpty());

        // When - Link sample to order with tests (simulating "Link Sample to Order"
        // modal)
        Integer orderId = Integer.parseInt(order.getId());
        Integer sampleIdInt = Integer.parseInt(sampleId);
        Integer sampleItemIdInt = Integer.parseInt(sampleItemId);

        // Link for test 1
        OrderSampleLink link1 = orderSampleLinkService.linkSampleToOrder(orderId, sampleIdInt, sampleItemIdInt,
                Integer.parseInt(TEST_TEST_ID_1), Integer.parseInt(TEST_USER_ID));
        assertNotNull("Link 1 should be created", link1);

        // Link for test 2
        OrderSampleLink link2 = orderSampleLinkService.linkSampleToOrder(orderId, sampleIdInt, sampleItemIdInt,
                Integer.parseInt(TEST_TEST_ID_2), Integer.parseInt(TEST_USER_ID));
        assertNotNull("Link 2 should be created", link2);

        // Then - Verify Analysis records were created
        List<Analysis> analysesAfterLink = analysisService.getAnalysesBySampleId(sampleId);
        assertNotNull("Analysis list should not be null", analysesAfterLink);
        assertEquals("Should have 2 Analysis records (one per test)", 2, analysesAfterLink.size());

        // Verify each analysis has correct test assignment
        List<String> analysisTestIds = analysesAfterLink.stream()
                .map(a -> a.getTest() != null ? a.getTest().getId() : null).filter(id -> id != null).toList();

        assertEquals("Should have 2 test IDs from analyses", 2, analysisTestIds.size());
        assertTrue("Should contain test 1", analysisTestIds.contains(TEST_TEST_ID_1));
        assertTrue("Should contain test 2", analysisTestIds.contains(TEST_TEST_ID_2));

        // Verify analysis status and type
        for (Analysis analysis : analysesAfterLink) {
            assertNotNull("Analysis should have a test", analysis.getTest());
            assertNotNull("Analysis should have a sample item", analysis.getSampleItem());
            assertEquals("Analysis should link to correct sample item", sampleItemId, analysis.getSampleItem().getId());
            assertNotNull("Analysis should have a status", analysis.getStatusId());
            assertEquals("Analysis type should be MANUAL", "MANUAL", analysis.getAnalysisType());
        }
    }

    /**
     * Tests that hasOrderForSample returns true after linking.
     */
    @Test
    @Rollback
    public void testHasOrderForSample_AfterLinking() {
        // Given - Create order and sample (same as above test)
        String labNo = "TEST-HAS-ORDER-001";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        ElectronicOrder order = electronicOrderService.getElectronicOrdersByExternalId(labNo).get(0);

        Sample sample = new Sample();
        sample.setAccessionNumber("SAMPLE-HAS-ORDER-001");
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setStatusId("1");
        sample.setSysUserId(TEST_USER_ID);
        String sampleId = sampleService.insert(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sampleService.get(sampleId));
        sampleItem.setTypeOfSample(typeOfSampleService.get("8001"));
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(TEST_USER_ID);
        String sampleItemId = sampleItemService.insert(sampleItem);

        // Verify no order link before linking
        assertFalse("Should have NO order before linking",
                orderSampleLinkService.hasOrderForSample(Integer.parseInt(sampleId)));

        // When - Link sample to order
        orderSampleLinkService.linkSampleToOrder(Integer.parseInt(order.getId()), Integer.parseInt(sampleId),
                Integer.parseInt(sampleItemId), Integer.parseInt(TEST_TEST_ID_1), Integer.parseInt(TEST_USER_ID));

        // Then - Verify order link exists
        assertTrue("Should have order after linking",
                orderSampleLinkService.hasOrderForSample(Integer.parseInt(sampleId)));
    }

    /**
     * Tests that duplicate links are not created.
     */
    @Test
    @Rollback
    public void testLinkSampleToOrder_DuplicateLink_ReturnsSameLink() {
        // Given - Create order and sample
        String labNo = "TEST-DUPLICATE-LINK";
        List<String> testIds = List.of(TEST_TEST_ID_1);

        medLabPatientOrderService.createPatientOrder(TEST_PATIENT_ID, labNo, "2026-01-09", "2026-01-09", "ROUTINE",
                testIds, TEST_NOTEBOOK_ENTRY_ID, 8002, TEST_USER_ID);

        ElectronicOrder order = electronicOrderService.getElectronicOrdersByExternalId(labNo).get(0);

        Sample sample = new Sample();
        sample.setAccessionNumber("SAMPLE-DUP-LINK");
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setStatusId("1");
        sample.setSysUserId(TEST_USER_ID);
        String sampleId = sampleService.insert(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sampleService.get(sampleId));
        sampleItem.setTypeOfSample(typeOfSampleService.get("8001"));
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId("1");
        sampleItem.setSysUserId(TEST_USER_ID);
        String sampleItemId = sampleItemService.insert(sampleItem);

        Integer orderId = Integer.parseInt(order.getId());
        Integer sampleIdInt = Integer.parseInt(sampleId);
        Integer sampleItemIdInt = Integer.parseInt(sampleItemId);
        Integer testIdInt = Integer.parseInt(TEST_TEST_ID_1);

        // When - Link sample to order twice
        OrderSampleLink firstLink = orderSampleLinkService.linkSampleToOrder(orderId, sampleIdInt, sampleItemIdInt,
                testIdInt, Integer.parseInt(TEST_USER_ID));

        OrderSampleLink secondLink = orderSampleLinkService.linkSampleToOrder(orderId, sampleIdInt, sampleItemIdInt,
                testIdInt, Integer.parseInt(TEST_USER_ID));

        // Then - Should return same link, not create duplicate
        assertEquals("Should return same link ID", firstLink.getId(), secondLink.getId());

        // Verify only ONE Analysis record was created
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sampleId);
        assertEquals("Should have exactly 1 Analysis record", 1, analyses.size());
    }
}
