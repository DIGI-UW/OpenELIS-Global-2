package org.openelisglobal.addons.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class KdigoTest {

    @Test
    void gfrCategoriesIncludingBoundaries() {
        assertEquals("G1", Kdigo.gfrCategory(150));
        assertEquals("G1", Kdigo.gfrCategory(90));
        assertEquals("G2", Kdigo.gfrCategory(89.9));
        assertEquals("G2", Kdigo.gfrCategory(89));
        assertEquals("G2", Kdigo.gfrCategory(60.0));
        assertEquals("G3a", Kdigo.gfrCategory(59.5)); // không làm tròn lên 60
        assertEquals("G3a", Kdigo.gfrCategory(59.99));
        assertEquals("G3a", Kdigo.gfrCategory(59));
        assertEquals("G3a", Kdigo.gfrCategory(45));
        assertEquals("G3b", Kdigo.gfrCategory(44.9));
        assertEquals("G3b", Kdigo.gfrCategory(44));
        assertEquals("G3b", Kdigo.gfrCategory(30));
        assertEquals("G4", Kdigo.gfrCategory(29.9));
        assertEquals("G4", Kdigo.gfrCategory(15));
        assertEquals("G5", Kdigo.gfrCategory(14.9));
        assertEquals("G5", Kdigo.gfrCategory(0));
    }

    @Test
    void displayedStringsAreStagedAsShown() {
        assertEquals("G3a", Kdigo.egfrFact(null, "59", null).category());
        assertEquals("G3a", Kdigo.egfrFact(null, "59.5", null).category());
        assertEquals("G3a", Kdigo.egfrFact(null, "59,50", null).category());
        assertEquals("G2", Kdigo.egfrFact(null, "60.0", null).category());
        assertEquals("G3b", Kdigo.egfrFact(null, "44.9", null).category());
        assertEquals("G2", Kdigo.egfrFact(null, "89", null).category());
        assertEquals("G5", Kdigo.egfrFact(null, "<5", null).category());
        assertEquals("G1", Kdigo.egfrFact(null, ">150", null).category());
        assertNull(Kdigo.egfrFact(null, "Không tính được", null));
    }

    @Test
    void albuminuriaCategories() {
        assertEquals("A1", Kdigo.albuminuriaCategory(29.9, "mg/g"));
        assertEquals("A2", Kdigo.albuminuriaCategory(30, "mg/g"));
        assertEquals("A2", Kdigo.albuminuriaCategory(300, "mg/g"));
        assertEquals("A3", Kdigo.albuminuriaCategory(300.1, "mg/g"));
        assertEquals("A1", Kdigo.albuminuriaCategory(2.9, "mg/mmol"));
        assertEquals("A2", Kdigo.albuminuriaCategory(3, "mg/mmol"));
        assertEquals("A2", Kdigo.albuminuriaCategory(30, "mg/mmol"));
        assertEquals("A3", Kdigo.albuminuriaCategory(30.5, "mg/mmol"));
        assertNull(Kdigo.albuminuriaCategory(50, "mg/L")); // không phải tỉ số
        assertNull(Kdigo.acrFact(null, "50", null, null));
        assertEquals("A2", Kdigo.acrFact(null, "45", "mg/g", null).category());
    }

    @Test
    void factsBlockStatesStageAndRange() {
        String block = Kdigo.factsBlock(List.of(Kdigo.egfrFact(null, "59", null),
                Kdigo.egfrFact(null, "75", "3 tháng trước"), Kdigo.acrFact(null, "12.5", "mg/g", null)));
        assertTrue(block.contains("eGFR phiếu đang xem 59 mL/ph/1,73 m² → **G3a (45–59)**"), block);
        assertTrue(block.contains("eGFR lần trước (3 tháng trước) 75 mL/ph/1,73 m² → **G2 (60–89)**"), block);
        assertTrue(block.contains("ACR phiếu đang xem 12,5 mg/g → **A1 (< 30 mg/g)**"), block);
        assertTrue(block.contains("G1 ≥ 90 · G2 60–89 · G3a 45–59 · G3b 30–44 · G4 15–29 · G5 < 15"), block);
        assertEquals("", Kdigo.factsBlock(List.of()));
    }

    /** Bảng trong tro-ly.md (model đọc) phải khớp đúng bảng chương trình dùng để tính. */
    @Test
    void rulesTableMatchesCode() throws Exception {
        String md = Files.readString(Path.of("src/main/resources/rules/vi.md"));
        String ckd = Rules.parse(md).tables.get("ckd");
        for (Kdigo.Band b : Kdigo.GFR) {
            Matcher m = Pattern.compile("- " + b.code() + ": (.+?) — ").matcher(ckd);
            assertTrue(m.find(), b.code());
            assertEquals(b.range(), m.group(1).trim(), b.code());
        }
        assertTrue(ckd.contains("A1 < 30, A2 30–300, A3 > 300"), ckd);
    }
}
