package org.openelisglobal.program.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;
import org.openelisglobal.program.util.DesignationScheme.PartScheme;

/**
 * Assigns the designation and the barcode a cassette or a slide carries for the
 * rest of its life, under the laboratory's own scheme (FR-9.3).
 */
public final class PathologyDesignations {

    private static final String PART_PLACEHOLDER = "{part}";
    private static final String NUMBER_PLACEHOLDER = "{n}";

    private PathologyDesignations() {
    }

    /** The designation the first part of a case carries under this scheme. */
    public static String firstPart(DesignationScheme scheme) {
        requireScheme(scheme);
        return scheme.getPartScheme() == PartScheme.NUMERIC ? "1" : "A";
    }

    /**
     * The designation for the next cassette cut from a part: the lowest number that
     * part has not already used.
     *
     * @param designationsInCase every block designation on the case, the
     *                           deactivated ones included, since their designations
     *                           are retired rather than freed
     */
    public static String nextBlockDesignation(Collection<String> designationsInCase, String part,
            DesignationScheme scheme) {
        requireScheme(scheme);
        String trimmedPart = requirePart(part);
        String format = requireNumbered(scheme.getBlockFormat(), "blockFormat");

        return firstUnused(taken(designationsInCase), number -> format.replace(PART_PLACEHOLDER, trimmedPart)
                .replace(NUMBER_PLACEHOLDER, String.valueOf(number)));
    }

    /**
     * The designation for the next slide cut from a block: the lowest number that
     * block has not already used. Slides are numbered within their own block, so
     * what the rest of the case holds does not come into it.
     *
     * @param designationsInBlock every slide designation on the block, the
     *                            deactivated ones included
     */
    public static String nextSlideDesignation(Collection<String> designationsInBlock, DesignationScheme scheme) {
        requireScheme(scheme);
        String format = requireNumbered(scheme.getSlideFormat(), "slideFormat");

        return firstUnused(taken(designationsInBlock),
                number -> format.replace(NUMBER_PLACEHOLDER, String.valueOf(number)));
    }

    /** The barcode a block's label carries. */
    public static String blockBarcode(String accessionNumber, String blockDesignation, DesignationScheme scheme) {
        requireScheme(scheme);
        return join(scheme.getSeparator(), accessionNumber, blockDesignation);
    }

    /**
     * The barcode a slide's label carries. A blank segment is left out, so a slide
     * whose block is not known still scans to the case and to itself.
     */
    public static String slideBarcode(String accessionNumber, String blockDesignation, String slideDesignation,
            DesignationScheme scheme) {
        requireScheme(scheme);
        return join(scheme.getSeparator(), accessionNumber, blockDesignation, slideDesignation);
    }

    // helpers

    /**
     * The lowest number from one upwards whose designation nobody holds. Numbering
     * starts at one because that is what the bench writes on the first cassette of
     * a part.
     */
    private static String firstUnused(Set<String> taken, IntFunction<String> designationFor) {
        int number = 1;
        while (taken.contains(designationFor.apply(number))) {
            number++;
        }
        return designationFor.apply(number);
    }

    /**
     * The designations already spoken for, trimmed. Case is significant: a scheme
     * that letters its parts distinguishes a from A.
     */
    private static Set<String> taken(Collection<String> designations) {
        Set<String> taken = new HashSet<>();
        if (designations != null) {
            for (String designation : designations) {
                if (designation != null && !designation.isBlank()) {
                    taken.add(designation.trim());
                }
            }
        }
        return taken;
    }

    private static String join(String separator, String... segments) {
        StringBuilder barcode = new StringBuilder();
        for (String segment : segments) {
            if (segment != null && !segment.isBlank()) {
                if (barcode.length() > 0) {
                    barcode.append(separator);
                }
                barcode.append(segment.trim());
            }
        }
        return barcode.toString();
    }

    private static void requireScheme(DesignationScheme scheme) {
        if (scheme == null) {
            throw new IllegalArgumentException("scheme is required to name a pathology object");
        }
    }

    private static String requirePart(String part) {
        if (part == null || part.isBlank()) {
            throw new IllegalArgumentException("part is required: a block is numbered within the part it was cut from");
        }
        return part.trim();
    }

    /**
     * A format with no number in it would name every object on the case the same,
     * so it is refused rather than allowed to collide.
     */
    private static String requireNumbered(String format, String argument) {
        if (format == null || !format.contains(NUMBER_PLACEHOLDER)) {
            throw new IllegalArgumentException(argument + " must contain " + NUMBER_PLACEHOLDER
                    + ", or every designation it produces would be the same");
        }
        return format;
    }
}
