package org.openelisglobal.common.util;

public class MathUtil {

    public static boolean isEqual(Double first, Double second) {
        if (first != null) {
            return first.equals(second);
        } else {
            return first == null && second == null;
        }
    }
}
