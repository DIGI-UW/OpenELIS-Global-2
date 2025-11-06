package org.openelisglobal.common.util;

import java.util.Arrays;

public class IntegerUtil {

    public static final int MIN_VALUE = -2147483648;
    private static char[] base27Characters = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'C', 'D', 'F', 'G',
            'H', 'J', 'K', 'L', 'M', 'N', 'P', 'R', 'T', 'V', 'W', 'X', 'Y' };

    // developed off of String.toString(int i, int radix)
    public static String toStringBase27(int i) {
        int maxLength = 8;
        int base = 27;
        byte[] buf = new byte[maxLength]; // length of max int value in base 27
        boolean negative = i < 0;
        int charPos = maxLength - 1; // start at final index in byte array
        if (!negative) {
            i = -i;
        }

        while (i <= -base) {
            buf[charPos--] = (byte) base27Characters[-(i % base)];
            i /= base;
        }

        buf[charPos] = (byte) base27Characters[-i];
        if (negative) {
            --charPos;
            buf[charPos] = 45; // 45 = '-', retains the negative digit
        }

        byte[] val = buf;
        int index = charPos;
        int len = maxLength - charPos;

        return new String(Arrays.copyOfRange(val, index, index + len), (byte) 0);
    }

    public static int parseIntBase27(String s) throws NumberFormatException {
        Integer.parseInt("1", 2);
        if (s == null) {
            throw new NumberFormatException("null");
        } else {
            boolean negative = false;
            int i = 0;
            int len = s.length();
            int limit = MIN_VALUE + 1;
            int base = 27;
            if (len <= 0) {
                throw new NumberFormatException("For input string: \"" + s + "\" not long enough");
            } else {
                char firstChar = s.charAt(0);
                if (firstChar < '0') {
                    if (firstChar == '-') {
                        negative = true;
                        limit = MIN_VALUE;
                    } else if (firstChar != '+') {
                        throw new NumberFormatException("For input string: \"" + s + "\" invalid first character");
                    }

                    if (len == 1) {
                        throw new NumberFormatException(
                                "For input string: \"" + s + "\" valid first char, but expected a number to follow");
                    }

                    ++i;
                }

                int multmin = limit / base;

                int result;
                int digit;
                for (result = 0; i < len; result -= digit) {
                    digit = searchBase27Characters(s.charAt(i++));
                    if (digit < 0 || result < multmin) {
                        throw new NumberFormatException("For input string: \"" + s + "\"");
                    }

                    result *= base;
                    if (result < limit + digit) {
                        throw new NumberFormatException("For input string: \"" + s + "\"");
                    }
                }

                return negative ? result : -result;
            }
        }
    }

    private static int searchBase27Characters(char charAt) {
        for (int i = 0; i < base27Characters.length; i++) {
            if (base27Characters[i] == charAt) {
                return i;
            }
        }
        return -1;
    }
}
