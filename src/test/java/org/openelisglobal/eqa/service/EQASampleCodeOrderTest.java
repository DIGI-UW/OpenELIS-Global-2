package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * Panel sample codes in the order a reader expects (OGC-1666): the numbers in a
 * code compare as numbers, so HIV-C-2 comes before HIV-C-10, and a result with
 * no sample code comes last.
 */
public class EQASampleCodeOrderTest {

    @Test
    public void numbersInACodeCompareAsNumbers() {
        assertEquals(List.of("HIV-C-2", "HIV-C-3", "HIV-C-10"), sorted("HIV-C-10", "HIV-C-2", "HIV-C-3"));
    }

    @Test
    public void aMissingCodeSortsLast() {
        assertEquals(Arrays.asList("HIV-C-2", "HIV-C-10", null, ""), sorted(null, "HIV-C-10", "", "HIV-C-2"));
    }

    @Test
    public void lettersCompareWithoutCaseAndLeadingZerosDoNotMatter() {
        assertEquals(List.of("a-1", "B-01", "b-2", "S9", "S010"), sorted("S010", "b-2", "B-01", "S9", "a-1"));
    }

    @Test
    public void aLongNumberStillComparesByValue() {
        assertEquals(List.of("LOT-99999999999999999999", "LOT-100000000000000000000"),
                sorted("LOT-100000000000000000000", "LOT-99999999999999999999"));
    }

    private static List<String> sorted(String... codes) {
        List<String> list = new ArrayList<>(Arrays.asList(codes));
        list.sort(EQASampleCodeOrder.ORDER);
        return list;
    }
}
