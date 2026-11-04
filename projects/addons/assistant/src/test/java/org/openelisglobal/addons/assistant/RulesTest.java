package org.openelisglobal.addons.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RulesTest {

    @Test
    void vietnameseRulesHaveTheSystemPromptAndTables() throws Exception {
        Rules r = Rules.fromFile(Path.of("src/main/resources/rules/vi.md"), Lang.VI);
        assertTrue(r.system.contains(AnswerPostProcessor.DISCLAIMER), r.system);
        assertTrue(r.system.startsWith("Bạn là \"Phụ tá AI\""), r.system);
        for (String k : List.of("ckd", "glucose", "lipid", "liver", "cbc", "uric", "urine", "hepatitis", "qc")) {
            assertTrue(r.tables.containsKey(k), k);
        }
        assertTrue(r.tables.get("ckd").contains("G3a: 45–59"));
        assertTrue(r.tables.get("ckd").contains("G3b: 30–44"));
    }

    @Test
    void englishRulesMirrorTheVietnameseOnes() throws Exception {
        Rules en = Rules.fromFile(Path.of("src/main/resources/rules/en.md"), Lang.EN);
        Rules vi = Rules.fromFile(Path.of("src/main/resources/rules/vi.md"), Lang.VI);
        assertEquals(vi.tables.keySet(), en.tables.keySet());
        assertTrue(en.system.contains(Lang.EN.disclaimer), en.system);
        assertTrue(en.system.contains("KDIGO STAGES COMPUTED BY THE SERVER"), en.system);
        assertTrue(en.tables.get("ckd").contains("G3a: 45–59"));
    }

    @Test
    void labAndProductNamesAreFilledIn() {
        Rules r = Rules.current(Lang.VI).filled("Medilab", "OpenELIS Vietnam");
        assertTrue(r.system.contains("OpenELIS Vietnam của phòng xét nghiệm Medilab"), r.system);
        assertFalse(r.system.contains("{labName}"));
        assertTrue(Rules.current(Lang.EN).filled("", "").system.contains("the OpenELIS laboratory information system"));
    }

    @Test
    void tablesSelectedByTestsInContext() {
        List<String> keys = ReferenceTables.select(List.of("AST", "AST (SGOT)", "EGFR", "Độ lọc cầu thận eGFR",
                "GLUC", "Glucose", "TG", "Triglycerides", "U-LEU", "HBSAG-N", "HBsAg (định tính)"));
        assertTrue(keys.containsAll(List.of("liver", "ckd", "glucose", "lipid", "urine", "hepatitis")), keys.toString());
        assertTrue(!keys.contains("cbc"), keys.toString());
        assertTrue(ReferenceTables.select(List.of("WBC", "HGB", "PLT")).contains("cbc"));
        assertTrue(ReferenceTables.select(List.of("X1", "98979-8")).contains("ckd"), "eGFR by LOINC");
    }
}
