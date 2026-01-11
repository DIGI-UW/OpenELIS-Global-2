package org.openelisglobal.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.exception.LIMSException;

public class StringUtilTest {

    private static final String VALID_INTEGER = "123";

    private enum TestEnum {
        RED, GREEN, BLUE
    }

    @Test
    public void isNullorNill_shouldReturnTrueForNull() {
        assertTrue(StringUtil.isNullorNill(null));
    }

    @Test
    public void isNullorNill_shouldReturnTrueForEmptyString() {
        assertTrue(StringUtil.isNullorNill(""));
    }

    @Test
    public void isNullorNill_shouldReturnTrueForNullString() {
        assertTrue(StringUtil.isNullorNill("null"));
    }

    @Test
    public void isNullorNill_shouldReturnFalseForValidString() {
        assertFalse(StringUtil.isNullorNill("test"));
    }

    @Test
    public void isInteger_shouldReturnTrueForValidInteger() {
        assertTrue(StringUtil.isInteger(VALID_INTEGER));
        assertTrue(StringUtil.isInteger("-456"));
    }

    @Test
    public void isInteger_shouldReturnFalseForInvalidValue() {
        assertFalse(StringUtil.isInteger("12.34"));
        assertFalse(StringUtil.isInteger("abc"));
    }

    @Test
    public void isNumeric_shouldReturnTrueForValidNumbers() {
        assertTrue(StringUtil.isNumeric(VALID_INTEGER));
        assertTrue(StringUtil.isNumeric("3.14"));
        assertTrue(StringUtil.isNumeric("-45.67"));
    }

    @Test
    public void isNumeric_shouldReturnFalseForInvalidValue() {
        assertFalse(StringUtil.isNumeric("abc"));
        assertFalse(StringUtil.isNumeric(null));
    }

    @Test
    public void blankIfNull_shouldReturnEmptyStringForNull() {
        assertEquals("", StringUtil.blankIfNull(null));
    }

    @Test
    public void blankIfNull_shouldReturnValueForNonNull() {
        assertEquals("test", StringUtil.blankIfNull("test"));
    }

    @Test
    public void testSafeEquals_Nulls() {
        assertTrue(StringUtil.safeEquals(null, null));
        assertFalse(StringUtil.safeEquals(null, "a"));
        assertFalse(StringUtil.safeEquals("a", null));
        assertTrue(StringUtil.safeEquals(null, ""));
    }

    @Test
    public void containsOnly_shouldReturnTrueForMatchingChars() {
        assertTrue(StringUtil.containsOnly("aaaa", 'a'));
    }

    @Test
    public void containsOnly_shouldReturnFalseForMixedChars() {
        assertFalse(StringUtil.containsOnly("aaba", 'a'));
    }

    @Test
    public void containsOnly_shouldReturnFalseForNull() {
        assertFalse(StringUtil.containsOnly(null, 'a'));
    }

    @Test
    public void ellipsisString_shouldTruncateLongText() {
        assertEquals("Hello...", StringUtil.ellipsisString("Hello World", 5));
    }

    @Test
    public void ellipsisString_shouldReturnShortTextUnchanged() {
        assertEquals("Hi", StringUtil.ellipsisString("Hi", 10));
    }

    @Test
    public void capitalize_shouldCapitalizeFirstLetter() {
        assertEquals("Hello", StringUtil.capitalize("hello"));
    }

    @Test
    public void toArray_shouldSplitByComma() {
        String[] result = StringUtil.toArray("a, b, c");
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void toArray_shouldReturnEmptyArrayForNull() {
        assertArrayEquals(new String[0], StringUtil.toArray(null));
    }

    @Test
    public void replaceCharAtIndex_shouldReplaceCharacter() {
        assertEquals("hallo", StringUtil.replaceCharAtIndex("hello", 'a', 1));
    }

    @Test
    public void replaceCharAtIndex_shouldReturnUnchangedForInvalidIndex() {
        assertEquals("hello", StringUtil.replaceCharAtIndex("hello", 'a', -1));
    }

    @Test
    public void repeat_shouldRepeatString() {
        assertEquals("ababab", StringUtil.repeat("ab", 3));
    }

    @Test
    public void countInstances_shouldCountOccurrences() {
        assertEquals(3, StringUtil.countInstances("hello world", 'l'));
    }

    @Test
    public void testEscapeCSVValue_NullOrEmpty() {
        assertEquals(null, StringUtil.escapeCSVValue(null));
        assertEquals("", StringUtil.escapeCSVValue(""));
    }

    @Test
    public void testEscapeCSVValue_NoEscapingNeeded() {
        String input = "SimpleValue";
        String result = StringUtil.escapeCSVValue(input);
        assertEquals("SimpleValue", result);
    }

    @Test
    public void testEscapeCSVValue_WithComma() {
        String input = "Value,With,Comma";
        String result = StringUtil.escapeCSVValue(input);
        // Expect: "Value,With,Comma" (Wrapped in quotes)
        assertEquals("\"Value,With,Comma\"", result);
    }

    @Test
    public void testEscapeCSVValue_WithQuote() {
        String input = "Value\"WithQuote";
        String result = StringUtil.escapeCSVValue(input);
        // Expect: "Value""WithQuote" (Wrapped in quotes, internal quote doubled)
        assertEquals("\"Value\"\"WithQuote\"", result);
    }

    @Test
    public void testEscapeCSVValue_MixedCommaAndQuote() {
        String input = "Value,\"With\",Comma";
        String result = StringUtil.escapeCSVValue(input);
        // Expect: "Value,""With"",Comma"
        assertEquals("\"Value,\"\"With\"\",Comma\"", result);
    }

    @Test
    public void testSeparateCSVWithEmbededQuotes_Simple() {
        String input = "\"A\",\"B\"";
        String[] result = StringUtil.separateCSVWithEmbededQuotes(input);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void testSeparateCSVWithEmbededQuotes_WithCommaInQuotes() {
        String input = "\"Value1\",\"Value,2\"";
        String[] result = StringUtil.separateCSVWithEmbededQuotes(input);
        assertNotNull(result);
    }

    @Test
    public void testSeparateCSVWithMixedEmbededQuotes_HappyPath() {
        String input = "1,2,\"quoted, value\",4";
        String[] result = StringUtil.separateCSVWithMixedEmbededQuotes(input);

        assertEquals(4, result.length);
        assertEquals("1", result[0]);
        assertEquals("2", result[1]);
        assertEquals("quoted, value", result[2]);
        String lastElement = result[3];
        assertTrue(lastElement.startsWith("4"));
        assertTrue(lastElement.endsWith("\n") || lastElement.endsWith("\r\n"));
    }

    @Test
    public void testSeparateCSVWithMixedEmbededQuotes_Complex() {
        String input = "1,2,\"something, else, 4\",5,\"more of that thing\",8";
        String[] result = StringUtil.separateCSVWithMixedEmbededQuotes(input);

        assertEquals(6, result.length);
        assertEquals("something, else, 4", result[2]);
        assertEquals("more of that thing", result[4]);
    }

    @Test
    public void testSeparateCSVWithMixedEmbededQuotesAllRows_MultiLine() {
        String input = "Header1,Header2\nValue1,Value2";
        List<String[]> result = StringUtil.separateCSVWithMixedEmbededQuotesAllRows(input);

        assertEquals(2, result.size());
        assertArrayEquals(new String[] { "Header1", "Header2" }, result.get(0));
        assertArrayEquals(new String[] { "Value1", "Value2" }, result.get(1));
    }

    @Test
    public void testSeparateCSVWithMixedEmbededQuotesAllRows_ComplexMultiLine() {
        String input = "col1,col2,col3\n\"val,ue1\",val2,val3";
        List<String[]> result = StringUtil.separateCSVWithMixedEmbededQuotesAllRows(input);

        assertEquals(2, result.size());
        assertEquals("val,ue1", result.get(1)[0]);
    }

    @Test
    public void testCreateChunksOfText_NullOrEmpty() {
        List result = StringUtil.createChunksOfText(null, 10, false);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = StringUtil.createChunksOfText("", 10, false);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateChunksOfText_SmallText() {
        String text = "Hello";
        List result = StringUtil.createChunksOfText(text, 10, false);

        assertEquals(1, result.size());
        assertEquals("Hello", result.get(0));
    }

    @Test
    public void testCreateChunksOfText_HardSplit() {
        String text = "ABCDEFGHIJ"; // 10 chars
        List result = StringUtil.createChunksOfText(text, 3, false);

        assertEquals(4, result.size());
        assertEquals("ABC", result.get(0));
        assertEquals("DEF", result.get(1));
        assertEquals("GHI", result.get(2));
        assertEquals("J", result.get(3));
    }

    @Test
    public void testCreateChunksOfText_ObserveSpaces() {
        String text = "The quick brown fox";
        List result = StringUtil.createChunksOfText(text, 4, true);

        assertEquals(3, result.size());
        assertEquals("The quick", result.get(0));
        assertEquals(" brown", result.get(1));
        assertEquals(" fox", result.get(2));
    }

    @Test
    public void testCreateChunksOfText_ObserveSpaces_NoSpaceFound() {
        String text = "A_very_long_string_without_spaces";
        List result = StringUtil.createChunksOfText(text, 5, true);

        assertEquals(1, result.size());
        assertEquals(text, result.get(0));
    }

    @Test
    public void testReplace_HappyPath() {
        String input = "Hello World";
        String result = StringUtil.replace(input, "World", "Java");
        assertEquals("Hello Java", result);
    }

    @Test
    public void testReplace_MultipleOccurrences() {
        String input = "one, two, one, three";
        String result = StringUtil.replace(input, "one", "1");
        assertEquals("1, two, 1, three", result);
    }

    @Test
    public void testReplace_NullInput() {
        assertNull(StringUtil.replace(null, "old", "new"));
    }

    @Test
    public void testReplace_NotFound() {
        String input = "Hello World";
        String result = StringUtil.replace(input, "Universe", "Java");
        assertEquals("Hello World", result);
    }

    @Test
    public void testStrip_HappyPath() {
        String input = "AxBxC";
        String result = StringUtil.strip(input, "x");
        assertEquals("ABC", result);
    }

    @Test
    public void testStrip_NotFound() {
        String input = "ABC";
        String result = StringUtil.strip(input, "x");
        assertEquals("ABC", result);
    }

    @Test
    public void testJoin_HappyPath() {
        List<String> list = Arrays.asList("A", "B", "C");
        String result = StringUtil.join(list, ",");
        assertEquals("A,B,C", result);
    }

    @Test
    public void testJoin_Empty() {
        String result = StringUtil.join(Collections.emptyList(), ",");
        assertEquals("", result);
    }

    @Test
    public void testJoin_SingleElement() {
        List<String> list = Arrays.asList("A");
        String result = StringUtil.join(list, ",");
        assertEquals("A", result);
    }

    @Test
    public void testReplaceAllChars_HappyPath() {
        String result = StringUtil.replaceAllChars("password", '*');
        assertEquals("********", result);
    }

    @Test
    public void testReplaceAllChars_Null() {
        assertNull(StringUtil.replaceAllChars(null, '*'));
    }

    @Test
    public void testReplaceAllChars_Empty() {
        assertEquals("", StringUtil.replaceAllChars("", '*'));
    }

    @Test
    public void testReplaceNullWithEmptyString() {
        assertEquals(" ", StringUtil.replaceNullWithEmptyString(null));
        assertEquals("test", StringUtil.replaceNullWithEmptyString("test"));
    }

    @Test
    public void testFormatPhone_HappyPath() {
        String input = "(123)456-7890";
        String ext = null;
        String result = StringUtil.formatPhone(input, ext);

        assertEquals("123/456-7890", result);
    }

    @Test
    public void testFormatPhone_WithExtension() {
        String input = "(123)456-7890";
        String ext = "101";
        String result = StringUtil.formatPhone(input, ext);

        assertEquals("123/456-7890.101", result);
    }

    @Test
    public void testFormatPhone_NullInput() {
        String result = StringUtil.formatPhone(null, "123");
        assertEquals("null.123", result);
    }

    @Test
    public void testFormatPhone_InvalidFormat_TooShort() {
        String input = "123";
        String result = StringUtil.formatPhone(input, null);
        assertNull(result);
    }

    @Test
    public void testFormatPhoneForDisplay_HappyPath() {
        String input = "123/456-7890";
        String result = StringUtil.formatPhoneForDisplay(input);

        assertEquals("(123)456-7890", result);
    }

    @Test
    public void testFormatPhoneForDisplay_WithExt_Ignored() {
        String input = "123/456-7890.555";
        String result = StringUtil.formatPhoneForDisplay(input);

        assertEquals("(123)456-7890", result);
    }

    @Test
    public void testFormatPhoneForDisplay_InvalidFormat() {
        String input = "BadFormat"; // Too short
        String result = StringUtil.formatPhoneForDisplay(input);
        assertNull(result);
    }

    @Test
    public void testDoubleWithSignificantDigits_Standard() {
        double val = 123.45678;
        String result = StringUtil.doubleWithSignificantDigits(val, "2");
        assertEquals("123.46", result); // Rounded
    }

    @Test
    public void testDoubleWithSignificantDigits_NoFormatting() {
        double val = 123.45678;
        // Logic: if "-1", returns String.valueOf
        assertEquals("123.45678", StringUtil.doubleWithSignificantDigits(val, "-1"));
        assertEquals("123.45678", StringUtil.doubleWithSignificantDigits(val, (String) null));
    }

    @Test
    public void testDoubleWithSignificantDigits_IntOverload() {
        double val = 123.45678;
        String result = StringUtil.doubleWithSignificantDigits(val, 1);
        assertEquals("123.5", result);
    }

    @Test
    public void testGetActualNumericValue_Standard() {
        assertEquals("10.5", StringUtil.getActualNumericValue("10.5"));
    }

    @Test
    public void testGetActualNumericValue_WithComparators() {
        assertEquals("10.5", StringUtil.getActualNumericValue("<10.5"));
        assertEquals("20", StringUtil.getActualNumericValue(">20"));
    }

    @Test
    public void testGetActualNumericValue_NonNumeric() {
        assertEquals("NaN", StringUtil.getActualNumericValue("abc"));
        assertEquals("NaN", StringUtil.getActualNumericValue("<abc"));
    }

    @Test
    public void testCountChars() {
        String[] arr = { "a", "bb", "ccc" };
        int result = StringUtil.countChars(arr);
        assertEquals(6, result);
    }

    @Test
    public void testCountChars_Empty() {
        String[] arr = {};
        int result = StringUtil.countChars(arr);
        assertEquals(0, result);
    }

    @Test
    public void testSearchEnum_FoundExact() {
        TestEnum result = StringUtil.searchEnum(TestEnum.class, "RED");
        assertEquals(TestEnum.RED, result);
    }

    @Test
    public void testSearchEnum_FoundIgnoreCase() {
        TestEnum result = StringUtil.searchEnum(TestEnum.class, "green");
        assertEquals(TestEnum.GREEN, result);
    }

    @Test
    public void testSearchEnum_NotFound() {
        TestEnum result = StringUtil.searchEnum(TestEnum.class, "PURPLE");
        assertNull(result);
    }

    @Test
    public void testIsJavaIdentifier_Valid() {
        assertTrue(StringUtil.isJavaIdentifier("validVar"));
        assertTrue(StringUtil.isJavaIdentifier("_underScore"));
        assertTrue(StringUtil.isJavaIdentifier("$dollarSign"));
        assertTrue(StringUtil.isJavaIdentifier("var123"));
    }

    @Test
    public void testIsJavaIdentifier_InvalidStart() {
        assertFalse(StringUtil.isJavaIdentifier("1variable"));
    }

    @Test
    public void testIsJavaIdentifier_InvalidPart() {
        assertFalse(StringUtil.isJavaIdentifier("var-iable"));
        assertFalse(StringUtil.isJavaIdentifier("var iable"));
    }

    @Test
    public void testIsJavaIdentifier_Empty() {
        assertFalse(StringUtil.isJavaIdentifier(""));
    }

    @Test
    public void testTextInCommaSeperatedValues_Found() {
        String target = "B";
        String csv = "A,B,C";
        assertTrue(StringUtil.textInCommaSeperatedValues(target, csv));
    }

    @Test
    public void testTextInCommaSeperatedValues_FoundWithWhitespace() {
        String target = "B";
        String csv = "A, B ,C";
        assertTrue(StringUtil.textInCommaSeperatedValues(target, csv));
    }

    @Test
    public void testTextInCommaSeperatedValues_NotFound() {
        String target = "D";
        String csv = "A,B,C";
        assertFalse(StringUtil.textInCommaSeperatedValues(target, csv));
    }

    @Test
    public void testTextInCommaSeperatedValues_NullCSV() {
        assertFalse(StringUtil.textInCommaSeperatedValues("A", null));
    }

    @Test
    public void testEncodeForContext_HTML() {
        String input = "<script>";
        String expected = "&lt;script&gt;";

        String result = StringUtil.encodeForContext(input, StringUtil.EncodeContext.HTML);
        assertEquals(expected, result);
    }

    @Test
    public void testEncodeForContext_Javascript() {
        String input = "'";
        String result = StringUtil.encodeForContext(input, StringUtil.EncodeContext.JAVASCRIPT);

        // Assert it is not the raw quote anymore
        assertFalse(result.equals("'"));
        assertTrue(result.contains("\\") || result.contains("x27"));
    }

    @Test
    public void testNullSafeToString() {
        assertEquals("test", StringUtil.nullSafeToString("test"));
        assertEquals("", StringUtil.nullSafeToString(null));
    }

    @Test
    public void testSnipToMaxLength() {
        String input = "1234567890"; // 10 chars
        assertEquals("12345", StringUtil.snipToMaxLength(input, 5));
        assertEquals("1234567890", StringUtil.snipToMaxLength(input, 20));
    }

    @Test
    public void testSnipToMaxIdLength() {
        String longStr = "1234567890123";
        String shortStr = "123";

        assertEquals("1234567890", StringUtil.snipToMaxIdLength(longStr));
        assertEquals("123", StringUtil.snipToMaxIdLength(shortStr));
    }

    @Test
    public void testReplaceTail_HappyPath() {
        String value = "abcdefg"; // len 7
        String tail = "XYZ"; // len 3
        String result = StringUtil.replaceTail(value, tail);
        assertEquals("abcdXYZ", result);
    }

    @Test
    public void testReplaceTail_TailLongerThanValue() {
        String value = "abc";
        String tail = "longertail";
        assertThrows(IndexOutOfBoundsException.class, () -> {
            StringUtil.replaceTail(value, tail);
        });
    }

    @Test
    public void testBuildDelimitedString_DropBlanks() {
        List<String> list = Arrays.asList("A", null, "", "B");
        String result = StringUtil.buildDelimitedStringFromList(list, ",", true);
        assertEquals("A,B", result);
    }

    @Test
    public void testBuildDelimitedString_KeepBlanks() {
        List<String> list = Arrays.asList("A", null, "B");
        String result = StringUtil.buildDelimitedStringFromList(list, ",", false);
        assertEquals("A,,B", result);
    }

    @Test
    public void testBuildDelimitedString_NullOrEmptyList() {
        assertEquals("", StringUtil.buildDelimitedStringFromList(null, ",", true));
        assertEquals("", StringUtil.buildDelimitedStringFromList(new ArrayList<>(), ",", true));
    }

    @Test
    public void testDoubleWithInfinity_Constants() {
        assertEquals(Double.POSITIVE_INFINITY, StringUtil.doubleWithInfinity("Infinity"), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, StringUtil.doubleWithInfinity("-Infinity"), 0.0);
    }

    @Test
    public void testDoubleWithInfinity_ValidDouble() {
        assertEquals(123.45, StringUtil.doubleWithInfinity("123.45"), 0.0001);
    }

    @Test
    public void testDoubleWithInfinity_NullOrBlank() {
        assertNull(StringUtil.doubleWithInfinity(null));
        assertNull(StringUtil.doubleWithInfinity(""));
    }

    @Test
    public void testDoubleWithInfinity_Invalid() {
        assertNull(StringUtil.doubleWithInfinity("NotANumber"));
    }

    @Test
    public void testCompareWithNulls() {

        assertEquals(0, StringUtil.compareWithNulls(null, null)); // "" vs ""
        assertEquals(0, StringUtil.compareWithNulls(null, "")); // "" vs ""
        assertTrue(StringUtil.compareWithNulls("a", null) > 0);
        assertTrue(StringUtil.compareWithNulls(null, "a") < 0);
        assertTrue(StringUtil.compareWithNulls("a", "b") < 0);
    }

    @Test
    public void testTrim() {
        assertEquals("abc", StringUtil.trim("  abc  "));
        assertEquals("", StringUtil.trim(null));
    }

    @Test
    public void testLoadListFromString_HappyPath() throws LIMSException {
        String input = "A, B, C";
        List result = StringUtil.loadListFromStringOfElements(input, ",", false);

        assertEquals(3, result.size());
        assertEquals("A", result.get(0));
        assertEquals("B", result.get(1));
    }

    @Test
    public void testLoadListFromString_ValidateEmpty_Throws() {
        String input = "A,,B";

        assertThrows(LIMSException.class, () -> {
            StringUtil.loadListFromStringOfElements(input, ",", true); // validate=true
        });
    }

    @Test
    public void testLoadListFromString_NoValidate() throws LIMSException {
        String input = "A,,B";
        List result = StringUtil.loadListFromStringOfElements(input, ",", false);

        assertEquals(3, result.size());
        assertEquals("", result.get(1));
    }

    @Test
    public void testIsAllNumeric() {
        assertTrue(StringUtil.isAllNumeric("12345"));
        assertFalse(StringUtil.isAllNumeric("123a45"));
        assertFalse(StringUtil.isAllNumeric(""));
    }

    @Test
    public void testFormatExtensionForDisplay_HappyPath() {
        String input = "123/456-7890.EXT";
        String result = StringUtil.formatExtensionForDisplay(input);

        assertEquals("EXT", result);
    }

    @Test
    public void testFormatExtensionForDisplay_NoExtension() {
        String input = "123/456-7890";
        String result = StringUtil.formatExtensionForDisplay(input);

        assertNull(result);
    }

    @Test
    public void testFormatExtensionForDisplay_Null() {
        assertNull(StringUtil.formatExtensionForDisplay(null));
    }

    @Test
    public void testFormatExtensionForDisplay_ShortString() {
        assertNull(StringUtil.formatExtensionForDisplay("Short"));
    }

    @Test
    public void testIsRestOfStringBlank_True() {
        String input = "Hello     ";
        assertTrue(StringUtil.isRestOfStringBlank(input, 5));
    }

    @Test
    public void testIsRestOfStringBlank_False() {
        String input = "Hello World";
        assertFalse(StringUtil.isRestOfStringBlank(input, 6));
    }

    @Test
    public void testIsRestOfStringBlank_AtEnd() {
        String input = "Hello";
        assertTrue(StringUtil.isRestOfStringBlank(input, 5));
    }

    @Test
    public void testIsRestOfStringBlank_PastEnd() {
        String input = "Hello";
        assertTrue(StringUtil.isRestOfStringBlank(input, 10));
    }

    @Test
    public void testToArray_HappyPath() {
        String input = "one,two,three";
        String[] result = StringUtil.toArray(input, ",");

        assertEquals(3, result.length);
        assertArrayEquals(new String[] { "one", "two", "three" }, result);
    }

    @Test
    public void testToArray_Trimming() {
        String input = " one , two , three ";
        String[] result = StringUtil.toArray(input, ",");

        assertEquals("one", result[0]);
        assertEquals("two", result[1]);
        assertEquals("three", result[2]);
    }

    @Test
    public void testToArray_Null() {
        String[] result = StringUtil.toArray(null, ",");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testToArray_ConsecutiveDelimiters() {
        String input = "A,,B";
        String[] result = StringUtil.toArray(input, ",");

        assertEquals(2, result.length);
        assertEquals("A", result[0]);
        assertEquals("B", result[1]);
    }

    @Test
    public void testToArray_DifferentDelimiter() {
        String input = "A|B|C";
        String[] result = StringUtil.toArray(input, "|");
        assertEquals(3, result.length);
    }

    @Test
    public void testConstructor() {
        StringUtil util = new StringUtil();
        assertNotNull(util);
    }
}
