package org.openelisglobal.addon.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class DeidentifierTest {

    private static final List<String> IDS = List.of("Nguyễn Thị Lan", "Nguyễn", "Lan", "BN000123",
            "DEV01260000000000063", "0939123456", "123 Trần Hưng Đạo, Phường 5, Cao Lãnh", "lan.nguyen@gmail.com",
            "001157012345");

    @Test
    public void knownIdentifiersAreRemovedIgnoringCaseAndAccents() {
        Deidentifier d = new Deidentifier(IDS);
        String q = "bệnh nhân NGUYEN THI LAN (mã bn000123, phiếu DEV01260000000000063, sđt 0939.123.456) "
                + "sinh 01/01/1957, email lan.nguyen@gmail.com có eGFR 55.6 không?";
        String s = d.scrub(q);
        assertFalse(s, s.toLowerCase().contains("nguyen"));
        assertFalse(s, s.toLowerCase().contains("bn000123"));
        assertFalse(s, s.contains("DEV0126"));
        assertFalse(s, s.contains("0939"));
        assertFalse(s, s.contains("1957"));
        assertFalse(s, s.contains("gmail"));
        assertTrue("clinical numbers stay: " + s, s.contains("eGFR 55.6"));
        assertFalse(s, d.leaks(s));
    }

    @Test
    public void patternsAreRemovedWithoutAKnownList() {
        Deidentifier d = new Deidentifier(List.of());
        String s = d.scrub("Order ABC1234567, call 0912345678 or +84 912 345 678 or +1 415 555 0100,"
                + " id 079123456789, on 5/6/2026, 2026-06-05, 5 June 2026 and March 3, 2024");
        for (String bad : List.of("ABC1234567", "0912345678", "912 345 678", "415 555", "079123456789", "5/6/2026",
                "2026-06-05", "June 2026", "March 3")) {
            assertFalse(bad + " in " + s, s.contains(bad));
        }
    }

    @Test
    public void clinicalNumbersAndCommonWordsAreKept() {
        Deidentifier d = new Deidentifier(List.of("Trần Văn Nam"));
        String text = "Glucose 6.38 mmol/L, HbA1c 6.5%, PLT 150 G/L, tiểu cầu 0.45, HCT 0.0001234, Việt Nam";
        assertEquals(text, d.scrub(text));
    }

    /**
     * A one-word name part keeps its accents: "Lan" must not remove "lần", "Hà" not
     * "ha".
     */
    @Test
    public void aSingleNamePartDoesNotRemoveOrdinaryWords() {
        Deidentifier d = new Deidentifier(List.of("Nguyễn Thị Lan", "Lan", "Hà"));
        assertEquals("so với lần trước, ha ha", d.scrub("so với lần trước, ha ha"));
        assertEquals("cô [removed] hỏi", d.scrub("cô LAN hỏi"));
        assertEquals("bn [removed] ổn", d.scrub("bn nguyen thi lan ổn"));
    }

    @Test
    public void englishNamesInAnyOrderAreRemoved() {
        Deidentifier d = new Deidentifier(List.of("Jane Mary Doe", "Doe Jane", "Jane Doe"));
        assertEquals("is [removed] diabetic?", d.scrub("is jane doe diabetic?"));
        assertEquals("[removed], 59", d.scrub("DOE JANE, 59"));
    }
}
