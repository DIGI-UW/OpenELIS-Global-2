package org.openelisglobal.addons.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EgfrCheckTest {

    @Test
    void stages() {
        assertEquals("G1", EgfrCheck.stage(95));
        assertEquals("G2", EgfrCheck.stage(60));
        assertEquals("G3a", EgfrCheck.stage(59.9));
        assertEquals("G3a", EgfrCheck.stage(55.6));
        assertEquals("G3a", EgfrCheck.stage(45));
        assertEquals("G3b", EgfrCheck.stage(44.9));
        assertEquals("G3b", EgfrCheck.stage(30));
        assertEquals("G4", EgfrCheck.stage(15));
        assertEquals("G5", EgfrCheck.stage(14.9));
    }

    /** Hồi quy: thử thật, model gọi eGFR 55,6 là "độ 2". */
    @Test
    void regressionEgfr556CalledDo2() {
        String answer = "- **eGFR 55.6** giảm, tương ứng suy thận độ 2 (nhẹ).\n- Glucose 6,38 tăng nhẹ.";
        EgfrCheck.Outcome o = EgfrCheck.check(answer, List.of(55.6));
        assertTrue(o.corrected());
        String body = o.text().substring(0, o.text().indexOf("⚠️"));
        assertFalse(body.contains("độ 2"), o.text());
        assertTrue(o.text().contains("G3a"), o.text());
        assertTrue(o.text().contains("⚠️ Hệ thống đã tự sửa phân độ thận"), o.text());
        assertTrue(o.text().contains("Glucose 6,38 tăng nhẹ."), o.text()); // câu khác giữ nguyên
    }

    @Test
    void variantsAreCaught() {
        for (String wrong : List.of("eGFR 55,6 mL/ph: CKD giai đoạn 2.", "Mức lọc cầu thận 55.6 thuộc G2.",
                "Chức năng thận giảm, stage 2.", "eGFR 55.6 → bệnh thận mạn giai đoạn II.",
                "eGFR 55,6 tương ứng G3b.")) {
            EgfrCheck.Outcome o = EgfrCheck.check(wrong, List.of(55.6));
            assertTrue(o.corrected(), wrong + " → " + o.text());
            assertTrue(o.text().contains("G3a"), o.text());
        }
    }

    @Test
    void correctOrNeutralStatementsUntouched() {
        for (String ok : List.of("eGFR 55,6 → G3a (45–59) theo KDIGO.", "eGFR 55.6 thuộc giai đoạn 3a.",
                "eGFR 55,6: bệnh thận mạn giai đoạn 3 nếu kéo dài > 3 tháng.",
                "Theo KDIGO, eGFR chia G1–G5; 55,6 nằm ở G3a.", "eGFR 55,6 không phải độ 2 mà là G3a.",
                "Tăng huyết áp độ 2 cần đo lại.", "Glucose 6,38 mmol/L, cần làm HbA1c.")) {
            EgfrCheck.Outcome o = EgfrCheck.check(ok, List.of(55.6));
            assertFalse(o.corrected(), ok + " → " + o.text());
            assertEquals(ok, o.text());
        }
    }

    @Test
    void usesTheValueTheSentenceTalksAbout() {
        // lần trước 61,2 = G2 (đúng) — không được sửa thành G3a
        String a = "Lần trước eGFR 61,2 (G2), nay 55,6 (G3a).";
        EgfrCheck.Outcome o = EgfrCheck.check(a, List.of(55.6, 61.2));
        assertFalse(o.corrected(), o.text());
    }

    @Test
    void postProcessorAppendsDisclaimerExactlyOnce() {
        AnswerPostProcessor.Processed p = AnswerPostProcessor.process(
                "eGFR 55.6 là độ 2.\n\n*Gợi ý tham khảo - không thay chẩn đoán của bác sĩ*", List.of(55.6));
        assertTrue(p.egfrCorrected());
        assertTrue(p.text().endsWith(AnswerPostProcessor.DISCLAIMER), p.text());
        assertEquals(p.text().indexOf("Gợi ý tham khảo"), p.text().lastIndexOf("Gợi ý tham khảo"), p.text());
        AnswerPostProcessor.Processed q = AnswerPostProcessor.process("Không có gì.", List.of());
        assertEquals("Không có gì.\n\n" + AnswerPostProcessor.DISCLAIMER, q.text());
    }

    // ------------------------------------------------------------------ hồi quy 2026-10-19 (phòng thử)

    /** Nguyên văn câu THÔ model trả (đúng) ở lượt 2–3; bản cũ đổi "G2 (60–89)" thành "G3a (60–89)". */
    static final String RAW_2026_10_19 = "**Phân độ (tra bảng KDIGO)**\n"
            + "- **eGFR 59 → G3a (45–59)**, giảm nhẹ đến trung bình, nằm **sát ranh giới** G2 (60–89).\n"
            + "- Creatinine 1,10 mg/dl chỉ cao hơn ngưỡng trên (1,09) **0,01**, tức lệch rất nhẹ.\n"
            + "- **Chưa đủ kết luận bệnh thận mạn**: mới có một lần đo, không có kết quả trước.";

    @Test
    void regression20261019NeighbourStageIsNotRewritten() {
        EgfrCheck.Outcome o = EgfrCheck.check(RAW_2026_10_19, List.of(59.0));
        assertFalse(o.corrected(), o.text());
        assertEquals(RAW_2026_10_19, o.text());
        assertFalse(o.text().contains("G3a (60–89)"));
        // cùng câu khi tách dòng (lượt 1, lượt 4)
        String split = "- **eGFR 59 → G3a (45–59)**, giảm nhẹ đến trung bình. Giá trị nằm **sát ranh giới** G2 (60–89).";
        assertEquals(split, EgfrCheck.check(split, List.of(59.0)).text());
    }

    /** Câu sai đúng như người dùng thấy: "G3a (60–89)" cho eGFR 59 → phải sửa + cảnh báo. */
    @Test
    void exactFailureG3a6089IsCorrected() {
        for (String bad : List.of("eGFR 59 → G3a (60–89).", "eGFR 59, nằm sát ranh giới G3a (60–89).",
                "eGFR 59 thuộc G3a: 60–89.")) {
            EgfrCheck.Outcome o = EgfrCheck.check(bad, List.of(59.0));
            assertTrue(o.corrected(), bad);
            String body = o.text().substring(0, o.text().indexOf("⚠️"));
            assertFalse(body.contains("60–89"), o.text());
            assertTrue(body.contains("G3a (45–59)"), o.text());
            assertTrue(o.text().contains("⚠️ Hệ thống đã tự sửa phân độ thận"), o.text());
        }
    }

    @Test
    void wrongStageWithRangeIsReplacedAsAUnit() {
        EgfrCheck.Outcome o = EgfrCheck.check("eGFR 59 → G2 (60–89), giảm nhẹ.", List.of(59.0));
        assertTrue(o.corrected());
        assertTrue(o.text().startsWith("eGFR 59 → G3a (45–59), giảm nhẹ."), o.text());
        EgfrCheck.Outcome p = EgfrCheck.check("eGFR 59 thuộc G2 (60–89 mL/ph/1,73 m²).", List.of(59.0));
        assertTrue(p.text().startsWith("eGFR 59 thuộc G3a (45–59)."), p.text());
    }

    @Test
    void wrongRangeOnAReferencedStageIsFixed() {
        EgfrCheck.Outcome o = EgfrCheck.check("eGFR 59 → G3a (45–59), sát ranh giới G2 (45–59).", List.of(59.0));
        assertTrue(o.corrected());
        assertTrue(o.text().startsWith("eGFR 59 → G3a (45–59), sát ranh giới G2 (60–89)."), o.text());
    }

    @Test
    void boundaryValues() {
        assertFalse(EgfrCheck.check("eGFR 59,5 → G3a (45–59).", List.of(59.5)).corrected());
        assertTrue(EgfrCheck.check("eGFR 59,5 → G2 (60–89).", List.of(59.5)).corrected());
        assertFalse(EgfrCheck.check("eGFR 60 → G2 (60–89).", List.of(60.0)).corrected());
        assertTrue(EgfrCheck.check("eGFR 60 → G3a (45–59).", List.of(60.0)).corrected());
        assertFalse(EgfrCheck.check("eGFR 44,9 → G3b (30–44).", List.of(44.9)).corrected());
        EgfrCheck.Outcome o = EgfrCheck.check("eGFR 44,9 → G3a (45–59).", List.of(44.9));
        assertTrue(o.text().startsWith("eGFR 44,9 → G3b (30–44)."), o.text());
        assertFalse(EgfrCheck.check("eGFR 44 → G3b (30–44), gần G3a (45–59).", List.of(44.0)).corrected());
        assertFalse(EgfrCheck.check("eGFR 89 → G2 (60–89), sát G1 (≥ 90).", List.of(89.0)).corrected());
        assertTrue(EgfrCheck.check("eGFR 89 → G1 (≥ 90).", List.of(89.0)).corrected());
        assertFalse(EgfrCheck.check("eGFR 12 → G5 (< 15).", List.of(12.0)).corrected());
    }

    @Test
    void ambiguousSentenceIsFlaggedNotRewritten() {
        String a = "eGFR 59, gần ngưỡng, là G2.";
        EgfrCheck.Outcome o = EgfrCheck.check(a, List.of(59.0));
        assertTrue(o.corrected(), o.text());
        assertTrue(o.text().startsWith(a), o.text());
        assertTrue(o.text().contains("cần đối chiếu"), o.text());
    }

    @Test
    void validationCasesUseTheirOwnValues() {
        List<Kdigo.Fact> facts = List.of(Kdigo.egfrFact("Ca 1", "59", null), Kdigo.egfrFact("Ca 2", "75", null));
        String ok = "**Ca 1**: eGFR 59 → G3a (45–59).\n**Ca 2**: eGFR 75 → G2 (60–89).";
        assertFalse(EgfrCheck.checkFacts(ok, facts).corrected());
        EgfrCheck.Outcome o = EgfrCheck.checkFacts("**Ca 2**: eGFR 75 → G3a (45–59).", facts);
        assertTrue(o.text().startsWith("**Ca 2**: eGFR 75 → G2 (60–89)."), o.text());
        // không có số: dùng nhãn ca phía trên
        EgfrCheck.Outcome p = EgfrCheck.checkFacts("**Ca 2**\n- Chức năng thận: G3a.", facts);
        assertTrue(p.text().contains("Chức năng thận: G2."), p.text());
        // không số, không nhãn, độ không khớp ca nào → gắn cờ, không sửa
        EgfrCheck.Outcome q = EgfrCheck.checkFacts("Chức năng thận giảm, G4.", facts);
        assertTrue(q.corrected());
        assertTrue(q.text().startsWith("Chức năng thận giảm, G4."), q.text());
        assertTrue(q.text().contains("cần đối chiếu"), q.text());
    }

    @Test
    void albuminuriaIsCheckedAgainstAcr() {
        List<Kdigo.Fact> facts = List.of(Kdigo.egfrFact(null, "59", null), Kdigo.acrFact(null, "45", "mg/g", null));
        assertFalse(EgfrCheck.checkFacts("ACR 45 mg/g → A2 (30–300 mg/g).", facts).corrected());
        EgfrCheck.Outcome o = EgfrCheck.checkFacts("ACR 45 mg/g → A1 (< 30 mg/g).", facts);
        assertTrue(o.text().startsWith("ACR 45 mg/g → A2 (30–300 mg/g)."), o.text());
        EgfrCheck.Outcome r = EgfrCheck.checkFacts("Albumin niệu sát A3 (> 30 mg/g).", facts);
        assertTrue(r.corrected(), r.text());
        // không có ACR: nêu độ A là không có căn cứ → gắn cờ; liệt kê A1–A3 thì không
        List<Kdigo.Fact> noAcr = List.of(Kdigo.egfrFact(null, "59", null));
        assertTrue(EgfrCheck.checkFacts("Albumin niệu A1.", noAcr).corrected());
        assertFalse(EgfrCheck.checkFacts("Cần làm ACR (albumin niệu) để phân độ A1–A3.", noAcr).corrected());
        assertFalse(EgfrCheck.checkFacts("HbA1c 6,1 %; eGFR 59 → G3a (45–59).", noAcr).corrected());
    }

    @Test
    void stageRangeWithoutPatientValuesIsStillChecked() {
        assertTrue(EgfrCheck.checkFacts("Theo KDIGO, G3a (60–89).", List.of()).corrected());
        assertFalse(EgfrCheck.checkFacts("Theo KDIGO, G3a (45–59).", List.of()).corrected());
    }

    /** Phòng thử 19/10 sau bản sửa đầu: eGFR 89, model liệt kê độ lân cận đúng nhưng bị đổi "G3a (45–59)" → "G2". */
    @Test
    void neighbourListIsNotRewritten() {
        String raw = "**Phân độ KDIGO (theo máy chủ)**\n- eGFR 89 mL/ph/1,73 m² → **G2 (60–89)**, giảm nhẹ.\n\n"
                + "**Các độ lân cận**\n- Độ trên: **G1 (≥ 90)**. eGFR 89 chỉ thấp hơn ngưỡng này 1 đơn vị.\n"
                + "- Độ dưới: **G3a (45–59)**, tức eGFR còn cách ngưỡng 60 khá xa so với kết quả hiện tại.\n"
                + "- Vì vậy độ G2 này nên xem là **tạm thời** cho đến khi kiểm tra lại.";
        EgfrCheck.Outcome o = EgfrCheck.check(raw, List.of(89.0));
        assertFalse(o.corrected(), o.text());
        assertEquals(raw, o.text());
        String one = "eGFR 89 → G2 (60–89), độ dưới là G3a (45–59).";
        assertEquals(one, EgfrCheck.check(one, List.of(89.0)).text());
        // nhưng gán cho bệnh nhân thì vẫn sửa
        assertTrue(EgfrCheck.check("Bệnh nhân ở G3a (45–59).", List.of(89.0)).corrected());
        assertTrue(EgfrCheck.check("- Suy thận độ 3.", List.of(89.0)).corrected());
    }

    /** Câu thô thật từ phòng thử (phát lại 73 câu trả lời cũ): đúng cả, không được sửa. */
    @Test
    void realAnswersReplayedStayUntouched() {
        for (Object[] c : new Object[][] {
                { 59.0, "- Thấp hơn: **G3b (30–44)**, giảm trung bình đến nặng. Giá trị này còn cách xa G3b." },
                { 44.0, "- Độ nhẹ hơn: **G3a (45–59)**, giảm nhẹ đến trung bình. Kết quả 44 chỉ thấp hơn ngưỡng 45"
                        + " một đơn vị, nên **sát ranh giới G3a**." },
                { 60.0, "- eGFR 60 mL/ph/1,73 m² → **G2 (60–89)**, giảm nhẹ. Đây là đúng ngưỡng dưới của G2"
                        + " (≥ 60 là G2, không làm tròn)." } }) {
            EgfrCheck.Outcome o = EgfrCheck.check((String) c[1], List.of((Double) c[0]));
            assertFalse(o.corrected(), o.text());
        }
        // ngưỡng một phía SAI vẫn bị bắt
        assertTrue(EgfrCheck.check("G2 (≥ 45).", List.of()).corrected());
    }
}
