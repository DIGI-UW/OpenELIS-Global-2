package org.openelisglobal.orderentry.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.orderentry.dao.OrderTestTestedElsewhereDAO;
import org.openelisglobal.orderentry.valueholder.OrderTestTestedElsewhere;
import org.springframework.beans.factory.annotation.Autowired;

public class TestedElsewhereServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String LAB_NUMBER = "TEW0000000001";
    private static final String EMPTY_ORDER_LAB_NUMBER = "TEW0000000002";
    private static final Integer SAMPLE_ID = 987700;
    private static final String DIRECT_TEST_ID = "987700";
    private static final String PANEL_TEST_ID = "987701";
    private static final String ANALYSIS_TEST_ID = "987702";
    private static final String UNRELATED_TEST_ID = "987703";
    private static final String LAB_ID = "987700";
    private static final String SECOND_LAB_ID = "987701";

    @Autowired
    private TestedElsewhereService testedElsewhereService;

    @Autowired
    private OrderTestTestedElsewhereDAO testedElsewhereDAO;

    @Before
    public void setUpFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/tested-elsewhere-service.xml");
    }

    @Test
    public void forOrder_withMarkedTests_shouldReturnFormattedMarks() {
        List<Map<String, Object>> marks = testedElsewhereService.forOrder(LAB_NUMBER);

        assertEquals(1, marks.size());
        Map<String, Object> mark = marks.get(0);
        assertEquals(DIRECT_TEST_ID, mark.get("testId"));
        assertEquals(LAB_ID, mark.get("performingLabId"));
        assertEquals("TEW Reference Laboratory", mark.get("performingLabName"));
        assertEquals("Positive", mark.get("reportedValue"));
    }

    @Test
    public void forOrder_withNoMarks_shouldReturnEmptyList() {
        List<Map<String, Object>> marks = testedElsewhereService.forOrder(EMPTY_ORDER_LAB_NUMBER);

        assertEquals(0, marks.size());
    }

    @Test
    public void forOrder_withUnknownLabNumber_shouldThrowException() {
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> testedElsewhereService.forOrder("NONEXISTENT"));
        assertEquals("no order has lab number NONEXISTENT", thrown.getMessage());
    }

    @Test
    public void forOrder_withBlankLabNumber_shouldThrowException() {
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> testedElsewhereService.forOrder("   "));
        assertEquals("no order has lab number    ", thrown.getMessage());
    }

    @Test
    public void mark_updateExistingDirectTest_shouldUpdateAndPersist() {
        Map<String, Object> result = testedElsewhereService.mark(LAB_NUMBER, DIRECT_TEST_ID, SECOND_LAB_ID, "Negative",
                "2");

        assertEquals(DIRECT_TEST_ID, result.get("testId"));
        assertEquals(SECOND_LAB_ID, result.get("performingLabId"));
        assertEquals("Second Reference Lab", result.get("performingLabName"));
        assertEquals("Negative", result.get("reportedValue"));

        OrderTestTestedElsewhere persisted = testedElsewhereDAO
                .forSampleAndTest(SAMPLE_ID, Integer.valueOf(DIRECT_TEST_ID)).orElseThrow();
        assertEquals(Integer.valueOf(SECOND_LAB_ID), persisted.getPerformingLabId());
        assertEquals("Negative", persisted.getReportedValue());
        assertEquals(Integer.valueOf(2), persisted.getRecordedById());
    }

    @Test
    public void mark_withPanelMemberTest_shouldCreateNewMark() {
        Map<String, Object> result = testedElsewhereService.mark(LAB_NUMBER, PANEL_TEST_ID, LAB_ID, "Reactive", "1");

        assertEquals(PANEL_TEST_ID, result.get("testId"));
        assertEquals(LAB_ID, result.get("performingLabId"));
        assertEquals("TEW Reference Laboratory", result.get("performingLabName"));
        assertEquals("Reactive", result.get("reportedValue"));

        OrderTestTestedElsewhere persisted = testedElsewhereDAO
                .forSampleAndTest(SAMPLE_ID, Integer.valueOf(PANEL_TEST_ID)).orElseThrow();
        assertEquals(SAMPLE_ID, persisted.getSampleId());
        assertEquals(Integer.valueOf(PANEL_TEST_ID), persisted.getTestId());
        assertEquals(Integer.valueOf(LAB_ID), persisted.getPerformingLabId());
        assertEquals("Reactive", persisted.getReportedValue());
        assertEquals(Integer.valueOf(1), persisted.getRecordedById());
    }

    @Test
    public void mark_withAnalysisTest_shouldCreateNewMark() {
        Map<String, Object> result = testedElsewhereService.mark(LAB_NUMBER, ANALYSIS_TEST_ID, LAB_ID, "120 mg/dL",
                "1");

        assertEquals(ANALYSIS_TEST_ID, result.get("testId"));
        assertEquals(LAB_ID, result.get("performingLabId"));
        assertEquals("TEW Reference Laboratory", result.get("performingLabName"));
        assertEquals("120 mg/dL", result.get("reportedValue"));

        OrderTestTestedElsewhere persisted = testedElsewhereDAO
                .forSampleAndTest(SAMPLE_ID, Integer.valueOf(ANALYSIS_TEST_ID)).orElseThrow();
        assertEquals(SAMPLE_ID, persisted.getSampleId());
        assertEquals(Integer.valueOf(ANALYSIS_TEST_ID), persisted.getTestId());
    }

    @Test
    public void mark_withBlankLabAndNullReportedValue_shouldAllowEmptyValues() {
        Map<String, Object> result = testedElsewhereService.mark(LAB_NUMBER, PANEL_TEST_ID, "", null, null);

        assertEquals(PANEL_TEST_ID, result.get("testId"));
        assertEquals("", result.get("performingLabId"));
        assertEquals("", result.get("performingLabName"));
        assertEquals("", result.get("reportedValue"));

        OrderTestTestedElsewhere persisted = testedElsewhereDAO
                .forSampleAndTest(SAMPLE_ID, Integer.valueOf(PANEL_TEST_ID)).orElseThrow();
        assertEquals(Optional.empty(), Optional.ofNullable(persisted.getPerformingLabId()));
        assertEquals(Optional.empty(), Optional.ofNullable(persisted.getReportedValue()));
    }

    @Test
    public void mark_withUnknownOrder_shouldThrowException() {
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> testedElsewhereService.mark("UNKNOWN_ORDER", DIRECT_TEST_ID, LAB_ID, "Value", "1"));
        assertEquals("no order has lab number UNKNOWN_ORDER", thrown.getMessage());
    }

    @Test
    public void mark_withNonNumericTestId_shouldThrowException() {
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> testedElsewhereService.mark(LAB_NUMBER, "not-a-number", LAB_ID, "Value", "1"));
        assertEquals("the test is not on this order", thrown.getMessage());
    }

    @Test
    public void mark_withTestNotOnOrder_shouldThrowException() {
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> testedElsewhereService.mark(LAB_NUMBER, UNRELATED_TEST_ID, LAB_ID, "Value", "1"));
        assertEquals("the test is not on this order", thrown.getMessage());
    }

    @Test
    public void mark_withNonNumericLabId_shouldThrowException() {
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> testedElsewhereService.mark(LAB_NUMBER, DIRECT_TEST_ID, "invalid-lab", "Value", "1"));
        assertEquals("unknown performing laboratory", thrown.getMessage());
    }

    @Test
    public void mark_withNonExistentLabId_shouldThrowException() {
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> testedElsewhereService.mark(LAB_NUMBER, DIRECT_TEST_ID, "999999", "Value", "1"));
        assertEquals("unknown performing laboratory", thrown.getMessage());
    }

    @Test
    public void mark_withReportedValueExceedingMaxLength_shouldThrowException() {
        String longValue = "A".repeat(256);
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> testedElsewhereService.mark(LAB_NUMBER, DIRECT_TEST_ID, LAB_ID, longValue, "1"));
        assertEquals("the reported value is longer than 255", thrown.getMessage());
    }

    @Test
    public void unmark_withExistingMark_shouldDeleteAndReturnTrue() {
        boolean removed = testedElsewhereService.unmark(LAB_NUMBER, DIRECT_TEST_ID);

        assertTrue(removed);
        assertEquals(Optional.empty(), testedElsewhereDAO.forSampleAndTest(SAMPLE_ID, Integer.valueOf(DIRECT_TEST_ID)));
    }

    @Test
    public void unmark_withUnmarkedOrderedTest_shouldReturnFalse() {
        boolean removed = testedElsewhereService.unmark(LAB_NUMBER, PANEL_TEST_ID);

        assertFalse(removed);
    }

    @Test
    public void unmark_withNonNumericTestId_shouldReturnFalse() {
        boolean removed = testedElsewhereService.unmark(LAB_NUMBER, "invalid-test");

        assertFalse(removed);
    }

    @Test
    public void unmark_withUnknownOrder_shouldThrowException() {
        OrderEntryRequestRefusedException thrown = assertThrows(OrderEntryRequestRefusedException.class,
                () -> testedElsewhereService.unmark("UNKNOWN_ORDER", DIRECT_TEST_ID));
        assertEquals("no order has lab number UNKNOWN_ORDER", thrown.getMessage());
    }
}
