package org.openelisglobal.analysis.valueholder;

import java.sql.Timestamp;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisAnchor;
import org.openelisglobal.analysis.service.AnalysisAnchorService;
import org.openelisglobal.analysis.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Covers the three states of the {@code ck_analysis_pool_or_item} XOR
 * constraint as resolved through {@link AnalysisAnchorService#resolveAnchor}:
 *
 * <ol>
 * <li>Sample-item-anchored ({@code sampitem_id} set): the anchor wraps the
 * direct sample_item.</li>
 * <li>Pool-anchored ({@code vector_pool_id} set, {@code sampitem_id} null): the
 * anchor resolves the pool's parent Sample via {@code vector_pool.sample_id}
 * and a representative organism via {@code vector_pool_member}.</li>
 * <li>Both set: the CHECK constraint rejects the row at INSERT time, so the
 * resolver never has to disambiguate which anchor wins.</li>
 * </ol>
 *
 * <p>
 * Analyses are inserted via {@code jdbcTemplate} rather than the dbunit
 * fixture: FlatXmlDataSet in default mode infers each table's column list from
 * the first row, which makes it impossible to seed one analysis row with
 * {@code sampitem_id} set and another with only {@code vector_pool_id}.
 */
public class AnalysisVectorPoolResolutionTest extends BaseWebContextSensitiveTest {

    private static final String ITEM_ANCHORED_ANALYSIS_ID = "600";
    private static final String ITEM_ANCHORED_SAMPLE_ITEM_ID = "600";
    private static final String POOL_ANCHORED_ANALYSIS_ID = "601";
    private static final String POOL_FIRST_MEMBER_SAMPLE_ITEM_ID = "601";
    private static final String POOL_PARENT_SAMPLE_ACCESSION = "VCT-RESOLVE-B";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private AnalysisAnchorService analysisAnchorService;

    @Before
    public void loadFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis-vector-pool-resolution.xml");

        Timestamp now = new Timestamp(System.currentTimeMillis());
        // Sample-item-anchored analysis: sampitem_id=600, vector_pool_id=null.
        jdbcTemplate.update(insertSql(), 600, 600, null, 600, 600, 1, 600, "1", now, now, "MANUAL", false, false, false,
                false, "Flea", "00000000-0000-4000-8000-000000000600", now);
        // Pool-anchored analysis: sampitem_id=null, vector_pool_id=601.
        jdbcTemplate.update(insertSql(), 601, null, 601, 600, 600, 1, 600, "1", now, now, "MANUAL", false, false, false,
                false, "Flea", "00000000-0000-4000-8000-000000000601", now);
    }

    private static String insertSql() {
        return "INSERT INTO clinlims.analysis" + " (id, sampitem_id, vector_pool_id, test_id, test_sect_id, revision,"
                + "  status_id, status, started_date, entry_date, analysis_type,"
                + "  reflex_trigger, referred_out, corrected, result_calculated,"
                + "  type_of_sample_name, fhir_uuid, lastupdated)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::uuid, ?)";
    }

    @Test
    public void resolveAnchor_returnsDirectMember_whenSampleItemAnchored() {
        Analysis analysis = analysisService.get(ITEM_ANCHORED_ANALYSIS_ID);
        Assert.assertNotNull("fixture analysis must load", analysis);

        AnalysisAnchor anchor = analysisAnchorService.resolveAnchor(analysis);

        Assert.assertNotNull("sample-item-anchored analysis must resolve an anchor", anchor);
        Assert.assertNotNull("anchor must carry the direct sample_item", anchor.getSampleItem());
        Assert.assertEquals("resolver must return the configured sampitem_id without touching vector_pool",
                ITEM_ANCHORED_SAMPLE_ITEM_ID, anchor.getSampleItem().getId());
        Assert.assertNotNull("Sample must come from the sample_item's own parent", anchor.getSample());
    }

    @Test
    public void resolveAnchor_returnsFirstPoolMember_whenPoolAnchored() {
        Analysis analysis = analysisService.get(POOL_ANCHORED_ANALYSIS_ID);
        Assert.assertNotNull("fixture analysis must load", analysis);
        Assert.assertNotNull("pool-anchored analysis must carry vectorPoolId", analysis.getVectorPoolId());
        Assert.assertNull("pool-anchored analysis must have no direct sample_item", analysis.getSampleItem());

        AnalysisAnchor anchor = analysisAnchorService.resolveAnchor(analysis);

        Assert.assertNotNull("pool-anchored analysis must resolve a non-null anchor", anchor);
        Assert.assertNotNull("anchor must include a representative pool member", anchor.getSampleItem());
        Assert.assertEquals("representative must be the first non-voided pool member by sort_order",
                POOL_FIRST_MEMBER_SAMPLE_ITEM_ID, anchor.getSampleItem().getId());
        Assert.assertNotNull("anchor must include the pool's parent Sample", anchor.getSample());
        Assert.assertEquals("anchor's Sample must be the vector_pool.sample_id row, not an organism's parent",
                POOL_PARENT_SAMPLE_ACCESSION, anchor.getSample().getAccessionNumber());
    }

    @Test
    public void resolveSample_shortCircuits_forSampleItemAnchored() {
        Analysis analysis = analysisService.get(ITEM_ANCHORED_ANALYSIS_ID);

        Assert.assertEquals("Sample must be reached through the direct sample_item, not via vector_pool",
                analysis.getSampleItem().getSample().getId(), analysisAnchorService.resolveSample(analysis).getId());
    }

    @Test
    public void insert_failsWhenBothSampleItemAndPoolIdSet() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try {
            jdbcTemplate.update(insertSql(), 699, 600, 601, 600, 600, 1, 600, "1", now, now, "MANUAL", false, false,
                    false, false, "Flea", "00000000-0000-4000-8000-000000000699", now);
            Assert.fail("inserting an analysis with both sampitem_id and vector_pool_id must violate"
                    + " ck_analysis_pool_or_item");
        } catch (DataIntegrityViolationException expected) {
            String message = expected.getMessage() == null ? "" : expected.getMessage().toLowerCase();
            Assert.assertTrue("constraint violation must reference ck_analysis_pool_or_item, was: " + message,
                    message.contains("ck_analysis_pool_or_item") || message.contains("check constraint"));
        }
    }
}
