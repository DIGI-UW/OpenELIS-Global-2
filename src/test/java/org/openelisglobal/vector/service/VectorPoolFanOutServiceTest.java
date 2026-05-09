package org.openelisglobal.vector.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;

public class VectorPoolFanOutServiceTest extends BaseWebContextSensitiveTest {

    private static final String PARENT_SAMPLE_ITEM_ID = "500";
    private static final String SAMPLE_ID = "500";
    private static final String ANALYSIS_ID = "500";
    private static final String SYS_USER_ID = "1";

    @Autowired
    private VectorPoolFanOutService vectorPoolFanOutService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Before
    public void loadFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/vector-pool-fanout.xml");
    }

    @Test
    public void fanOut_shouldCreateNSiblingsWhenPoolCountGreaterThanOne() {
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);
        Analysis analysis = analysisService.get(ANALYSIS_ID);

        List<SampleItem> siblings = vectorPoolFanOutService.fanOut(parent, List.of(analysis), 5, SYS_USER_ID);

        Assert.assertEquals("fanOut should create one sibling per pool count", 5, siblings.size());
        for (SampleItem sibling : siblings) {
            Assert.assertNotNull("each sibling must have a generated id", sibling.getId());
            Assert.assertNotEquals("sibling id must differ from the parent's", PARENT_SAMPLE_ITEM_ID, sibling.getId());
            Assert.assertEquals("sibling quantity must be 1 (one organism each)", Double.valueOf(1.0),
                    sibling.getQuantity());
            Assert.assertNull("siblings must not point at the parent (Architecture D rule)",
                    sibling.getParentSampleItem());
        }
    }

    @Test
    public void fanOut_shouldGiveSiblingsDistinctSortOrdersGreaterThanParent() {
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);
        int parentSortOrder = Integer.parseInt(parent.getSortOrder());

        List<SampleItem> siblings = vectorPoolFanOutService.fanOut(parent, List.of(), 4, SYS_USER_ID);

        Set<String> sortOrders = new HashSet<>();
        for (SampleItem sibling : siblings) {
            int order = Integer.parseInt(sibling.getSortOrder());
            Assert.assertTrue("sibling sort_order must exceed the parent's so the unique uk holds",
                    order > parentSortOrder);
            sortOrders.add(sibling.getSortOrder());
        }
        Assert.assertEquals("sibling sort_orders must be distinct", siblings.size(), sortOrders.size());
    }

    @Test
    public void fanOut_shouldDeleteOriginalSampleItem() {
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);
        Assert.assertFalse("precondition: parent starts un-voided", parent.isVoided());

        vectorPoolFanOutService.fanOut(parent, List.of(), 3, SYS_USER_ID);

        // Per the supervisor's rule, sample_item should only contain
        // individual organisms — the pool placeholder must be hard-deleted,
        // not soft-voided. The row should be gone after fan-out.
        Integer rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.sample_item WHERE id = ?",
                Integer.class, Integer.valueOf(PARENT_SAMPLE_ITEM_ID));
        Assert.assertEquals("parent placeholder must be deleted, not soft-voided", Integer.valueOf(0), rowCount);
    }

    @Test
    public void fanOut_shouldCreateVectorPoolForSampleAndLinkAllSiblings() {
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);

        vectorPoolFanOutService.fanOut(parent, List.of(), 7, SYS_USER_ID);

        List<VectorPool> pools = vectorPoolService.getBySampleId(SAMPLE_ID);
        Assert.assertEquals("exactly one pool per fanOut call", 1, pools.size());
        VectorPool pool = pools.get(0);
        Assert.assertEquals("pool sample_id must point at the source order", SAMPLE_ID, pool.getSampleId());
        // Pool size is no longer cached on vector_pool — derive it from the
        // vector_pool_member join, the authoritative source.
        Integer memberRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinlims.vector_pool_member WHERE vector_pool_id = ?", Integer.class,
                pool.getId());
        Assert.assertEquals("every sibling must be a vector_pool_member of the new pool", Integer.valueOf(7),
                memberRows);
    }

    @Test
    public void fanOut_shouldReFkAnalysesFromSampleItemToVectorPool() {
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);
        Analysis analysis = analysisService.get(ANALYSIS_ID);

        vectorPoolFanOutService.fanOut(parent, List.of(analysis), 3, SYS_USER_ID);
        List<VectorPool> pools = vectorPoolService.getBySampleId(SAMPLE_ID);
        Assert.assertEquals(1, pools.size());
        String poolIdAsString = String.valueOf(pools.get(0).getId());

        Analysis reloaded = analysisService.get(ANALYSIS_ID);
        Assert.assertNull("ck_analysis_pool_or_item: sampitem_id must be null once vector_pool_id is set",
                reloaded.getSampleItem());
        Assert.assertEquals("analysis must now reference the new pool", poolIdAsString, reloaded.getVectorPoolId());
    }

    @Test
    public void fanOut_shouldReturnEmptyWhenPoolCountIsOne() {
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);

        List<SampleItem> siblings = vectorPoolFanOutService.fanOut(parent, List.of(), 1, SYS_USER_ID);

        Assert.assertTrue("a pool of one is a single specimen — no fan-out", siblings.isEmpty());
        Assert.assertTrue("no pool row should be created on a no-op",
                vectorPoolService.getBySampleId(SAMPLE_ID).isEmpty());

        SampleItem reloaded = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);
        Assert.assertFalse("parent must remain un-voided on a no-op", reloaded.isVoided());
    }

    @Test
    public void fanOut_shouldReturnEmptyWhenPoolCountIsZero() {
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);

        List<SampleItem> siblings = vectorPoolFanOutService.fanOut(parent, List.of(), 0, SYS_USER_ID);

        Assert.assertTrue(siblings.isEmpty());
        Assert.assertTrue(vectorPoolService.getBySampleId(SAMPLE_ID).isEmpty());
    }

    @Test
    public void fanOut_shouldReturnEmptyWhenOriginalIsNull() {
        List<SampleItem> siblings = vectorPoolFanOutService.fanOut(null, List.of(), 3, SYS_USER_ID);

        Assert.assertTrue(siblings.isEmpty());
        Assert.assertTrue(vectorPoolService.getBySampleId(SAMPLE_ID).isEmpty());
    }

    @Test
    public void fanOut_shouldReturnEmptyWhenOriginalHasNoId() {
        SampleItem transientItem = new SampleItem();

        List<SampleItem> siblings = vectorPoolFanOutService.fanOut(transientItem, List.of(), 3, SYS_USER_ID);

        Assert.assertTrue("a transient SampleItem can't anchor a pool — skip", siblings.isEmpty());
    }

    @Test
    public void fanOut_shouldHandleNullAnalysesList() {
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);

        List<SampleItem> siblings = vectorPoolFanOutService.fanOut(parent, null, 2, SYS_USER_ID);

        Assert.assertEquals("siblings still get created when no analyses need re-FK'ing", 2, siblings.size());
        Assert.assertEquals(1, vectorPoolService.getBySampleId(SAMPLE_ID).size());
    }

    @Test
    public void fanOut_shouldReFkStragglerAnalysesEvenWhenCallerPassesEmptyList() {
        // Pre-condition: fixture's analysis 500 references parent 500.
        // Caller passes an empty analyses list, but fan-out must still re-FK
        // any analyses that are pointing at the parent — otherwise the
        // hard-delete of the parent would trip analysis_sampitem_fk.
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);
        vectorPoolFanOutService.fanOut(parent, new ArrayList<>(), 2, SYS_USER_ID);

        List<VectorPool> pools = vectorPoolService.getBySampleId(SAMPLE_ID);
        Assert.assertEquals(1, pools.size());
        String poolIdAsString = String.valueOf(pools.get(0).getId());

        Analysis reloaded = analysisService.get(ANALYSIS_ID);
        Assert.assertNull("straggler analyses must be re-FK'd off the parent before delete", reloaded.getSampleItem());
        Assert.assertEquals("straggler analyses must point at the new pool", poolIdAsString,
                reloaded.getVectorPoolId());
    }

    @Test
    public void fanOut_shouldCreateVectorPoolMemberForEverySibling() {
        SampleItem parent = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);

        List<SampleItem> siblings = vectorPoolFanOutService.fanOut(parent, Collections.emptyList(), 4, SYS_USER_ID);

        VectorPool pool = vectorPoolService.getBySampleId(SAMPLE_ID).get(0);
        // Membership lives in the vector_pool_member join (pure M:N) — every
        // sibling must have exactly one row tying it to the new pool.
        Set<String> memberSampleItemIds = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT sample_item_id::text FROM clinlims.vector_pool_member WHERE vector_pool_id = ?", String.class,
                pool.getId()));
        Set<String> siblingIds = new HashSet<>();
        siblings.forEach(s -> siblingIds.add(s.getId()));
        Assert.assertEquals("every sibling must be a vector_pool_member of the new pool", siblingIds,
                memberSampleItemIds);
    }

    @Test
    public void fanOut_shouldNotCollideSortOrdersAcrossMultipleParents() {
        // Drop in a second parent sample_item alongside the fixture's id=500.
        // Both have small sort_orders; without a global max-aware lookup the
        // two fanOut windows would overlap (parent A: sort_order 2..N+1,
        // parent B: sort_order 3..N+2), producing the collision pattern seen
        // in production.
        // The columns are NUMERIC(10,0) / NUMERIC, so bind as Integer/Double
        // — the JDBC driver doesn't bridge String → NUMERIC implicitly here.
        jdbcTemplate.update(
                "INSERT INTO clinlims.sample_item "
                        + "(id, samp_id, sort_order, status_id, typeosamp_id, quantity, voided, "
                        + " collection_date, lastupdated) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                501, Integer.valueOf(SAMPLE_ID), 2, 500, 500, 4.0, false,
                java.sql.Timestamp.valueOf("2026-05-09 00:00:00"), java.sql.Timestamp.valueOf("2026-05-09 00:00:00"));

        SampleItem parentA = sampleItemService.get(PARENT_SAMPLE_ITEM_ID);
        SampleItem parentB = sampleItemService.get("501");

        List<SampleItem> siblingsA = vectorPoolFanOutService.fanOut(parentA, List.of(), 5, SYS_USER_ID);
        List<SampleItem> siblingsB = vectorPoolFanOutService.fanOut(parentB, List.of(), 4, SYS_USER_ID);

        Assert.assertEquals(5, siblingsA.size());
        Assert.assertEquals(4, siblingsB.size());

        // Every unvoided sibling across both fan-outs must have a distinct
        // sort_order so accession.sortOrder barcodes round-trip cleanly.
        List<Integer> unvoidedSortOrders = jdbcTemplate.queryForList(
                "SELECT sort_order FROM clinlims.sample_item WHERE samp_id = ? AND voided = false ORDER BY id",
                Integer.class, Integer.valueOf(SAMPLE_ID));
        Set<Integer> distinct = new HashSet<>(unvoidedSortOrders);
        Assert.assertEquals("sort_order must be unique across all unvoided sibling sample_items in the order; got "
                + unvoidedSortOrders, unvoidedSortOrders.size(), distinct.size());
    }
}
