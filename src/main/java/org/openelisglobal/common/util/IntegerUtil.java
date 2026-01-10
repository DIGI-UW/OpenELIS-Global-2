package org.openelisglobal.common.util;

public class IntegerUtil {

    public static final int MIN_VALUE = -2147483648;
    private static char[] base27Characters = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'C', 'D', 'F', 'G',
            'H', 'J', 'K', 'L', 'M', 'N', 'P', 'R', 'T', 'V', 'W', 'X', 'Y' };

    // developed off of String.toString(int i, int radix)
    // developed off of String.toString(int i, int radix)
    public static String toStringBase27(int value) {
        if (value == 0) {
            return "0";
        }
        boolean negative = value < 0;
        long v = value;
        if (negative) {
            // use long to avoid overflow for Integer.MIN_VALUE
            v = -(long) value;
        }

        StringBuilder sb = new StringBuilder();
        while (v > 0) {
            int d = (int) (v % 27L);
            sb.append(base27Characters[d]);
            v /= 27L;
        }
        if (negative) {
            sb.append('-');
        }
        return sb.reverse().toString();
    }

    public static int parseIntBase27(String s) throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("null");
        }
        s = s.trim();
        if (s.length() == 0) {
            throw new NumberFormatException("For input string: \"" + s + "\" not long enough");
        }

        boolean negative = false;
        int idx = 0;
        char firstChar = s.charAt(0);
        if (firstChar == '+' || firstChar == '-') {
            negative = firstChar == '-';
            idx = 1;
            if (s.length() == 1) {
                throw new NumberFormatException(
                        "For input string: \"" + s + "\" valid first char, but expected a number to follow");
            }
        }

        long result = 0L;
        final long max = Integer.MAX_VALUE;
        final long min = (long) Integer.MIN_VALUE;

        for (; idx < s.length(); idx++) {
            char c = Character.toUpperCase(s.charAt(idx));
            int digit = searchBase27Characters(c);
            if (digit < 0) {
                throw new NumberFormatException("For input string: \"" + s + "\"");
            }

            result = result * 27L + digit;

            // check overflow for signed int, considering sign later
            long candidate = negative ? -result : result;
            if (candidate > max || candidate < min) {
                throw new NumberFormatException("For input string: \"" + s + "\"");
            }
        }

        int intResult = (int) result;
        return negative ? -intResult : intResult;
    }

    private static int searchBase27Characters(char c) {
        char up = Character.toUpperCase(c);
        for (int i = 0; i < base27Characters.length; i++) {
            if (base27Characters[i] == up) {
                return i;
            }
        }
        return -1;
    }
}
