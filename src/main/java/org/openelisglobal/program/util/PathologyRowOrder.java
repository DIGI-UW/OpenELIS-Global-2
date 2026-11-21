package org.openelisglobal.program.util;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;

/**
 * The one order a case's blocks and slides are shown and named in. The case's
 * collections carry no order, so a read could list them differently each time,
 * and a sentence naming outstanding slides would change between two loads of
 * the same case.
 *
 * <p>
 * Blocks come in the order they were cut, by id. Slides come under their block
 * in that order, then in the order they were cut; slides with no block on
 * record come last.
 */
public final class PathologyRowOrder {

    private static final Comparator<PathologyBlock> BLOCKS = Comparator.comparing(PathologyBlock::getId,
            Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<PathologySlide> SLIDES = Comparator
            .comparing(PathologySlide::getBlockId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PathologySlide::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private PathologyRowOrder() {
    }

    /** A sorted copy; the list given is left as it is. */
    public static List<PathologyBlock> blocks(List<PathologyBlock> blocks) {
        return blocks == null ? null : blocks.stream().sorted(BLOCKS).collect(Collectors.toList());
    }

    /** A sorted copy; the list given is left as it is. */
    public static List<PathologySlide> slides(List<PathologySlide> slides) {
        return slides == null ? null : slides.stream().sorted(SLIDES).collect(Collectors.toList());
    }
}
