package org.openelisglobal.program.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;

/**
 * A case's blocks and slides come back from the database in no fixed order, so
 * the case view, its sentences naming slides and the answers of the slide
 * endpoints all go through one order. Plain JUnit: the rule is pinned without a
 * database that might happen to return rows in it anyway.
 */
public class PathologyRowOrderTest {

    @Test
    public void blocks_comeInTheOrderTheyWereCut() {
        List<PathologyBlock> blocks = List.of(block(12), block(3), block(7));

        assertEquals("by id, the order the bench cut them", List.of(3, 7, 12),
                blockIds(PathologyRowOrder.blocks(blocks)));
    }

    @Test
    public void slides_comeUnderTheirBlockInBlockOrder_thenInTheOrderTheyWereCut() {
        List<PathologySlide> slides = List.of(slide(40, 7), slide(41, 3), slide(42, 7), slide(39, 3));

        assertEquals("block 3's slides first, each block's slides by id", List.of(39, 41, 40, 42),
                slideIds(PathologyRowOrder.slides(slides)));
    }

    @Test
    public void slides_withNoBlockOnRecord_comeLast() {
        List<PathologySlide> slides = List.of(slide(5, null), slide(41, 3), slide(2, null));

        assertEquals("a legacy slide names no block, so it follows every slide that does", List.of(41, 2, 5),
                slideIds(PathologyRowOrder.slides(slides)));
    }

    @Test
    public void rowsNotYetSaved_comeLastAndDoNotFail() {
        List<PathologySlide> slides = List.of(slide(null, 3), slide(41, 3));
        List<PathologyBlock> blocks = List.of(block(null), block(3));

        assertEquals("a slide with no id yet follows the saved ones under its block", Arrays.asList(41, null),
                slideIds(PathologyRowOrder.slides(slides)));
        assertEquals("a block with no id yet follows the saved ones", Arrays.asList(3, null),
                blockIds(PathologyRowOrder.blocks(blocks)));
    }

    @Test
    public void noList_staysNoList() {
        assertNull("a case whose collections were never read has none", PathologyRowOrder.slides(null));
        assertNull(PathologyRowOrder.blocks(null));
    }

    @Test
    public void theListGiven_isLeftAsItWas() {
        List<PathologySlide> slides = new ArrayList<>(List.of(slide(42, 7), slide(41, 3)));
        List<PathologyBlock> blocks = new ArrayList<>(List.of(block(7), block(3)));

        PathologyRowOrder.slides(slides);
        PathologyRowOrder.blocks(blocks);

        assertEquals("a case's own collection is never reordered in place, which would mark it changed",
                List.of(42, 41), slideIds(slides));
        assertEquals(List.of(7, 3), blockIds(blocks));
    }

    // helpers

    private static PathologyBlock block(Integer id) {
        PathologyBlock block = new PathologyBlock();
        block.setId(id);
        return block;
    }

    private static PathologySlide slide(Integer id, Integer blockId) {
        PathologySlide slide = new PathologySlide();
        slide.setId(id);
        slide.setBlockId(blockId);
        return slide;
    }

    private static List<Integer> blockIds(List<PathologyBlock> blocks) {
        return blocks.stream().map(PathologyBlock::getId).collect(Collectors.toList());
    }

    private static List<Integer> slideIds(List<PathologySlide> slides) {
        return slides.stream().map(PathologySlide::getId).collect(Collectors.toList());
    }
}
