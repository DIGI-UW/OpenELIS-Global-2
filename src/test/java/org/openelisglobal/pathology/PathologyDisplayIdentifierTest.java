package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;

/**
 * How a block or a slide names itself wherever it is printed, listed or
 * audited.
 *
 * <p>
 * Every caller used to spell the same fallback out for itself, and the audit
 * trail spelled it wrong: a block with no designation and no legacy number was
 * recorded as the literal word null, which names no object on any bench. One
 * rule on the object replaces all of them.
 */
public class PathologyDisplayIdentifierTest {

    @Test
    public void aBlockIsNamedByItsDesignation() {
        assertEquals("the identifier written on the cassette is what the bench reads", "A2",
                block(7, "A2", 11).displayIdentifier());
        assertEquals("and surrounding whitespace is not part of it", "A2", block(7, "  A2  ", 11).displayIdentifier());
    }

    @Test
    public void aBlockCutBeforeDesignationsExisted_isNamedByItsNumber() {
        assertEquals("a block that predates designations still has to be nameable", "11",
                block(7, null, 11).displayIdentifier());
        assertEquals("a blank designation is no designation", "11", block(7, "   ", 11).displayIdentifier());
    }

    @Test
    public void aBlockWithNeither_isNamedByItsRow() {
        assertEquals("the row id is the last thing that can still identify the object", "B7",
                block(7, null, null).displayIdentifier());
    }

    @Test
    public void aBlockThatIsNotYetARow_namesNothing() {
        assertEquals("a cassette that has not been stored has nothing to be named by, and an empty string is"
                + " printable where the word null is not", "", block(null, null, null).displayIdentifier());
    }

    @Test
    public void aSlideFollowsTheSameRule() {
        assertEquals("the identifier written on the slide label", "3", slide(7, "3", 9).displayIdentifier());
        assertEquals("falling back to the legacy number", "9", slide(7, null, 9).displayIdentifier());
        assertEquals("and then to the row, told apart from a block's", "S7", slide(7, null, null).displayIdentifier());
        assertEquals("", slide(null, null, null).displayIdentifier());
    }

    @Test
    public void theAuditTrailNamesTheObjectRatherThanTheWordNull() {
        PathologySample pathologySample = new PathologySample();
        pathologySample.setBlocks(Arrays.asList(block(7, null, null)));
        pathologySample.setSlides(Arrays.asList(slide(8, null, null)));

        assertTrue("a block with neither a designation nor a number is still named by its row",
                pathologySample.getBlocks_Audit().startsWith("Block: B7,"));
        assertFalse("and not by the literal word null, which names no object on any bench",
                pathologySample.getBlocks_Audit().startsWith("Block: null"));
        assertTrue("and a slide the same way", pathologySample.getSlides_Audit().startsWith("Slide: S8,"));
        assertFalse(pathologySample.getSlides_Audit().startsWith("Slide: null"));
    }

    // helpers

    private PathologyBlock block(Integer id, String designation, Integer blockNumber) {
        PathologyBlock block = new PathologyBlock();
        block.setId(id);
        block.setDesignation(designation);
        block.setBlockNumber(blockNumber);
        return block;
    }

    private PathologySlide slide(Integer id, String designation, Integer slideNumber) {
        PathologySlide slide = new PathologySlide();
        slide.setId(id);
        slide.setDesignation(designation);
        slide.setSlideNumber(slideNumber);
        return slide;
    }
}
