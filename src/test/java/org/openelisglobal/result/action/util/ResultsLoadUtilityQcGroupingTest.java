package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for {@link ResultsLoadUtility#groupQcRowsUnderParent(List)}.
 *
 * <p>
 * The sort is the load-bearing piece of the QC-under-parent rendering:
 * DUPLICATE rows must land immediately after their parent client row, while
 * BLANK / CONTROL rows belong at the end of their accession group. End-to-end
 * DAO-level filtering is covered separately by {@code AnalysisExcludingQcTest}.
 *
 * <p>
 * Extends BaseWebContextSensitiveTest because ResultsLoadUtility's static
 * initializers call FormFields.getInstance(), which requires the Spring context
 * — but the grouping logic itself is in-memory and doesn't touch the DB.
 */
public class ResultsLoadUtilityQcGroupingTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultsLoadUtility utility;

    @Test
    public void duplicateRowMovesImmediatelyAfterParent() {
        TestResultItem client = clientRow("1", "ACC-1");
        TestResultItem duplicate = qcRow("3", "ACC-1", "DUPLICATE", "1");

        // Seed the duplicate BEFORE the client so a no-op implementation can't pass.
        List<TestResultItem> rows = new ArrayList<>(Arrays.asList(duplicate, client));
        utility.groupQcRowsUnderParent(rows);

        assertEquals(2, rows.size());
        assertEquals("Client row must end first", "1", rows.get(0).getSampleItemId());
        assertEquals("Duplicate must move to slot directly after parent", "3", rows.get(1).getSampleItemId());
        assertEquals("DUPLICATE", rows.get(1).getQcType());
    }

    @Test
    public void blankAndControlAppendToEndOfAccessionGroup() {
        TestResultItem client = clientRow("1", "ACC-1");
        TestResultItem blank = qcRow("2", "ACC-1", "BLANK", null);
        TestResultItem control = qcRow("4", "ACC-1", "CONTROL", null);

        // Insert QC rows ahead of the client to prove the sort actively reorders.
        List<TestResultItem> rows = new ArrayList<>(Arrays.asList(blank, client, control));
        utility.groupQcRowsUnderParent(rows);

        assertEquals(3, rows.size());
        assertEquals("Client row must be first", "1", rows.get(0).getSampleItemId());
        assertEquals("BLANK keeps its slot-1 position (relative to other orphan QC)", "2",
                rows.get(1).getSampleItemId());
        assertEquals("BLANK", rows.get(1).getQcType());
        assertEquals("CONTROL goes last", "4", rows.get(2).getSampleItemId());
        assertEquals("CONTROL", rows.get(2).getQcType());
    }

    @Test
    public void fullFixtureOrdering_clientThenDuplicateThenBlankThenControl() {
        TestResultItem client = clientRow("1", "QC-TEST-001");
        TestResultItem blank = qcRow("2", "QC-TEST-001", "BLANK", null);
        TestResultItem duplicate = qcRow("3", "QC-TEST-001", "DUPLICATE", "1");
        TestResultItem control = qcRow("4", "QC-TEST-001", "CONTROL", null);

        // Original SQL order: by sample_item insert order.
        List<TestResultItem> rows = new ArrayList<>(Arrays.asList(client, blank, duplicate, control));
        utility.groupQcRowsUnderParent(rows);

        assertEquals(4, rows.size());
        assertEquals("Client row stays first", "1", rows.get(0).getSampleItemId());
        assertEquals("DUPLICATE must slot directly under its parent", "3", rows.get(1).getSampleItemId());
        assertEquals("BLANK pushed below DUPLICATE", "2", rows.get(2).getSampleItemId());
        assertEquals("BLANK", rows.get(2).getQcType());
        assertEquals("CONTROL last in accession group", "4", rows.get(3).getSampleItemId());
        assertEquals("CONTROL", rows.get(3).getQcType());
    }

    @Test
    public void duplicatesFromDifferentAccessionsLandUnderTheirOwnParent() {
        TestResultItem clientA = clientRow("10", "ACC-A");
        TestResultItem clientB = clientRow("20", "ACC-B");
        TestResultItem dupA = qcRow("11", "ACC-A", "DUPLICATE", "10");
        TestResultItem dupB = qcRow("21", "ACC-B", "DUPLICATE", "20");

        // Seed both duplicates far from their parents (and crossed) so neither lands
        // correctly without an active reorder.
        List<TestResultItem> rows = new ArrayList<>(Arrays.asList(dupB, dupA, clientA, clientB));
        utility.groupQcRowsUnderParent(rows);

        assertEquals(4, rows.size());
        assertEquals("clientA first", "10", rows.get(0).getSampleItemId());
        assertEquals("DUPLICATE for ACC-A lands directly after its parent", "11", rows.get(1).getSampleItemId());
        assertEquals("clientB second client", "20", rows.get(2).getSampleItemId());
        assertEquals("DUPLICATE for ACC-B lands directly after its parent", "21", rows.get(3).getSampleItemId());
    }

    @Test
    public void allClientRowsPassThroughUntouched() {
        TestResultItem a = clientRow("1", "ACC-1");
        TestResultItem b = clientRow("2", "ACC-2");

        List<TestResultItem> rows = new ArrayList<>(Arrays.asList(a, b));
        utility.groupQcRowsUnderParent(rows);

        assertEquals(2, rows.size());
        assertEquals("1", rows.get(0).getSampleItemId());
        assertEquals("2", rows.get(1).getSampleItemId());
    }

    private TestResultItem clientRow(String sampleItemId, String accession) {
        TestResultItem item = new TestResultItem();
        item.setSampleItemId(sampleItemId);
        item.setAccessionNumber(accession);
        return item;
    }

    private TestResultItem qcRow(String sampleItemId, String accession, String qcType, String parentSampleItemId) {
        TestResultItem item = clientRow(sampleItemId, accession);
        item.setQcType(qcType);
        item.setParentSampleItemId(parentSampleItemId);
        return item;
    }
}
