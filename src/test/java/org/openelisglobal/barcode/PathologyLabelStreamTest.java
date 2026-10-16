package org.openelisglobal.barcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.labeltype.BlockLabel;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.barcode.labeltype.SlideLabel;
import org.openelisglobal.barcode.valueholder.BarcodeLabelInfo;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

/**
 * Printing every slide label for a case answered 500: the label maker reads the
 * case through a service whose transaction has closed, and the case's slides
 * are lazy, so walking them threw {@code LazyInitializationException}. Blocks
 * never failed only because they are mapped eager.
 *
 * <p>
 * The base class runs each test outside any transaction, which is how the label
 * servlet calls the maker, so these tests hit the same detached case it does.
 *
 * <p>
 * The order, block and slide labels of a case all print its lab number, and
 * their print counts were kept under that one code, so printing either the
 * block or the slide labels used up the other at the default maximum of one.
 */
public class PathologyLabelStreamTest extends BaseWebContextSensitiveTest {

    /** Case 1 in the fixture; it holds block 101 and slide 201. */
    private static final String CASE_WITH_SLIDES = "12345";

    /** Case 2 in the fixture; it holds no block and no slide. */
    private static final String CASE_WITHOUT_SLIDES = "12346";

    private String maxBlockLabels;
    private String maxSlideLabels;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        removeAddedRows();
        maxBlockLabels = ConfigurationProperties.getInstance().getPropertyValue(Property.MAX_BLOCK_LABEL_PRINTED);
        maxSlideLabels = ConfigurationProperties.getInstance().getPropertyValue(Property.MAX_SLIDE_LABEL_PRINTED);
        // the shipped maximum for both kinds
        ConfigurationProperties.getInstance().setPropertyValue(Property.MAX_BLOCK_LABEL_PRINTED, "1");
        ConfigurationProperties.getInstance().setPropertyValue(Property.MAX_SLIDE_LABEL_PRINTED, "1");
        // a second block in use, and a retired slide cut from it
        jdbcTemplate.update("INSERT INTO clinlims.pathology_block (id, pathology_sample_id, last_updated,"
                + " block_number, active) VALUES (102, 1, now(), 12, true)");
        jdbcTemplate.update("INSERT INTO clinlims.pathology_slide (id, pathology_sample_id, last_updated,"
                + " slide_number, block_id, active) VALUES (202, 1, now(), 2, 102, false)");
    }

    @After
    public void tearDown() {
        removeAddedRows();
        ConfigurationProperties.getInstance().setPropertyValue(Property.MAX_BLOCK_LABEL_PRINTED, maxBlockLabels);
        ConfigurationProperties.getInstance().setPropertyValue(Property.MAX_SLIDE_LABEL_PRINTED, maxSlideLabels);
    }

    @Test
    public void slideOrder_outsideATransaction_printsTheActiveSlidesOnly() {
        List<Label> labels = generate(CASE_WITH_SLIDES, "slideOrder");

        assertEquals("one label for the slide in use, none for the retired one", 1, labels.size());
        assertTrue(labels.get(0) instanceof SlideLabel);
    }

    @Test
    public void blockOrder_outsideATransaction_printsOneLabelPerActiveBlock() {
        List<Label> labels = generate(CASE_WITH_SLIDES, "blockOrder");

        assertEquals("one label for each of the case's two blocks in use", 2, labels.size());
        assertTrue(labels.stream().allMatch(label -> label instanceof BlockLabel));
    }

    @Test
    public void slideOrder_forACaseWithNoSlides_printsNothing() {
        List<Label> labels = generate(CASE_WITHOUT_SLIDES, "slideOrder");

        assertEquals(0, labels.size());
    }

    @Test
    public void eachLabelKind_keepsItsPrintCountUnderItsOwnKey() {
        BarcodeLabelInfo block = generate(CASE_WITH_SLIDES, "blockOrder").get(0).getLabelInfo();
        BarcodeLabelInfo slide = generate(CASE_WITH_SLIDES, "slideOrder").get(0).getLabelInfo();
        BarcodeLabelInfo order = generate(CASE_WITH_SLIDES, "order").get(0).getLabelInfo();

        assertEquals(CASE_WITH_SLIDES + "-B", block.getCode());
        assertEquals("block", block.getType());
        assertEquals(CASE_WITH_SLIDES + "-S", slide.getCode());
        assertEquals("slide", slide.getType());
        assertEquals("the order label keeps the plain lab number", CASE_WITH_SLIDES, order.getCode());
        assertEquals("order", order.getType());
    }

    @Test
    public void printingOneLabelKind_leavesTheOtherKindsPrintable() {
        assertTrue("the order label prints", print(CASE_WITH_SLIDES, "order").size() > 0);
        assertTrue("the block labels still print after the order label",
                print(CASE_WITH_SLIDES, "blockOrder").size() > 0);
        assertTrue("the slide label still prints after the blocks", print(CASE_WITH_SLIDES, "slideOrder").size() > 0);

        assertEquals("the block labels have used their own maximum of one", 0,
                generate(CASE_WITH_SLIDES, "blockOrder").size());
    }

    // helpers

    private List<Label> generate(String accessionNumber, String type) {
        return maker(accessionNumber, type).getLabels();
    }

    private ByteArrayOutputStream print(String accessionNumber, String type) {
        return maker(accessionNumber, type).createLabelsAsStreamWithMaximumPrints();
    }

    private BarcodeLabelMaker maker(String accessionNumber, String type) {
        BarcodeLabelMaker labelMaker = new BarcodeLabelMaker();
        labelMaker.setSysUserId("1");
        labelMaker.generateLabels(accessionNumber, type, "1", "false");
        return labelMaker;
    }

    private void removeAddedRows() {
        jdbcTemplate.update("DELETE FROM clinlims.barcode_label_info WHERE code LIKE ? OR code LIKE ?",
                CASE_WITH_SLIDES + "%", CASE_WITHOUT_SLIDES + "%");
        jdbcTemplate.update("DELETE FROM clinlims.pathology_slide WHERE id = 202");
        jdbcTemplate.update("DELETE FROM clinlims.pathology_block WHERE id = 102");
    }
}
