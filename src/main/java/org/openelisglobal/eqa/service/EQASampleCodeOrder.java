package org.openelisglobal.eqa.service;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Locale;

/**
 * The order panel sample codes are listed in wherever scores are shown
 * (OGC-1666). The digits in a code compare as a number, so HIV-C-2 comes before
 * HIV-C-10; the letters compare without case; a result that names no sample,
 * blank or null, comes last.
 */
public final class EQASampleCodeOrder {

    public static final Comparator<String> ORDER = EQASampleCodeOrder::compare;

    private EQASampleCodeOrder() {
    }

    private static int compare(String left, String right) {
        boolean leftBlank = left == null || left.isBlank();
        boolean rightBlank = right == null || right.isBlank();
        if (leftBlank || rightBlank) {
            return Boolean.compare(leftBlank, rightBlank);
        }
        String a = left.trim().toLowerCase(Locale.ROOT);
        String b = right.trim().toLowerCase(Locale.ROOT);
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            int iEnd = chunkEnd(a, i);
            int jEnd = chunkEnd(b, j);
            String aChunk = a.substring(i, iEnd);
            String bChunk = b.substring(j, jEnd);
            int order = Character.isDigit(a.charAt(i)) && Character.isDigit(b.charAt(j))
                    ? new BigInteger(aChunk).compareTo(new BigInteger(bChunk))
                    : aChunk.compareTo(bChunk);
            if (order != 0) {
                return order;
            }
            i = iEnd;
            j = jEnd;
        }
        return Integer.compare(a.length() - i, b.length() - j);
    }

    private static int chunkEnd(String value, int start) {
        boolean digits = Character.isDigit(value.charAt(start));
        int end = start + 1;
        while (end < value.length() && Character.isDigit(value.charAt(end)) == digits) {
            end++;
        }
        return end;
    }
}
