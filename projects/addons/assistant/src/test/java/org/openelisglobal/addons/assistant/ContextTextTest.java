package org.openelisglobal.addons.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** The context text from OpenELIS's de-identified JSON: same wording as the first (in-core) assistant. */
class ContextTextTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** What OpenELIS returns for one patient (shape of AiContextServiceImpl). */
    static final String PATIENT = """
            {"user":"7","subject":"p_x1","view":"patient","cases":[{"label":1,
             "patient":{"sex":"F","ageYears":69},"receivedDaysAgo":91,
             "results":[
              {"section":"Biochemistry","code":"AST","name":"AST (SGOT)","value":"40.61","unit":"u/L","low":10.0,"high":40.0,"status":"Finalized"},
              {"section":"Biochemistry","code":"EGFR","loinc":"98979-8","name":"Độ lọc cầu thận eGFR","value":"55.60","unit":"mL/ph/1.73m²","low":60.0,"high":120.0,"status":"Finalized"},
              {"section":"Biochemistry","code":"GLUC","name":"Glucose","value":"6.38","unit":"mmol/L","low":3.9,"high":5.9,"status":"Finalized"},
              {"section":"Biochemistry","code":"TG","name":"Triglycerides","value":"1.96","unit":"mmol/L","low":0.46,"high":1.88,"status":"Finalized"},
              {"section":"Biochemistry","code":"URIC","name":"Acid Uric","value":"176.36","unit":"µmol/L","low":154.7,"high":357.0,"status":"Finalized"},
              {"section":"Serology","code":"NOTE","name":"Ghi chú","value":"BN [removed] gọi [removed] ngày [date]","unit":null,"low":null,"high":null,"status":"Finalized"}],
             "previous":[{"code":"EGFR","loinc":"98979-8","name":"Độ lọc cầu thận eGFR","value":"61.2","unit":"mL/ph/1.73m²","low":60.0,"high":120.0,"daysBefore":92}]}]}
            """;

    @Test
    void vietnameseLookupTextMatchesTheOriginalWording() throws Exception {
        JsonNode ctx = JSON.readTree(PATIENT);
        ContextText.Built b = ContextText.build(ctx, ContextText.LOOKUP, null, Lang.VI);
        String text = b.text();
        assertTrue(text.startsWith("MÀN HÌNH: Tra cứu kết quả bệnh nhân\nBỆNH NHÂN: Nữ, 69 tuổi.\n"), text);
        assertTrue(text.contains("PHIẾU ĐANG XEM: nhận mẫu 3 tháng trước, 6 kết quả.\nKẾT QUẢ:\n[Sinh hoá]\n"), text);
        assertTrue(text.contains("- AST (SGOT): 40.61 u/L (tham chiếu 10–40) → CAO"), text);
        assertTrue(text.contains("- Độ lọc cầu thận eGFR: 55.60 mL/ph/1.73m² (tham chiếu 60–120) → THẤP"), text);
        assertTrue(text.contains("- Glucose: 6.38 mmol/L (tham chiếu 3.9–5.9) → CAO"), text);
        assertTrue(text.contains("- Triglycerides: 1.96 mmol/L (tham chiếu 0.46–1.88) → CAO"), text);
        assertTrue(text.contains("- Acid Uric: 176.36 µmol/L (tham chiếu 154.7–357)\n"), text);
        assertTrue(text.contains("[Miễn dịch – huyết thanh]\n- Ghi chú: BN [đã ẩn] gọi [đã ẩn] ngày [ngày]"), text);
        assertTrue(text.contains("KẾT QUẢ CÁC LẦN TRƯỚC (cùng xét nghiệm, tính từ phiếu đang xem):\n"
                + "- Độ lọc cầu thận eGFR: 61.2 mL/ph/1.73m² (tham chiếu 60–120) — 3 tháng trước"), text);
        assertTrue(text.contains("eGFR phiếu đang xem 55,60 mL/ph/1,73 m² → **G3a (45–59)**"), text);
        assertTrue(text.contains("eGFR lần trước (3 tháng trước) 61,2 mL/ph/1,73 m² → **G2 (60–89)**"), text);
        assertEquals("patient:p_x1", b.subjectKey());
        assertEquals("7", b.userId());
        assertEquals("Nữ, 69 tuổi · 6 kết quả · 4 bất thường", b.summary());
        assertEquals(java.util.List.of(55.6, 61.2), b.egfrValues());
        assertTrue(ReferenceTables.select(b.testKeys()).containsAll(java.util.List.of("ckd", "glucose", "lipid", "liver")));
    }

    @Test
    void englishLookupText() throws Exception {
        ContextText.Built b = ContextText.build(JSON.readTree(PATIENT), ContextText.LOOKUP, null, Lang.EN);
        String text = b.text();
        assertTrue(text.startsWith("SCREEN: Patient results\nPATIENT: Female, 69 years.\n"), text);
        assertTrue(text.contains("CURRENT ORDER: received 3 months ago, 6 results."), text);
        assertTrue(text.contains("- AST (SGOT): 40.61 u/L (reference 10–40) → HIGH"), text);
        assertTrue(text.contains("- Ghi chú: BN [removed] gọi [removed] ngày [date]"), text);
        assertTrue(text.contains("eGFR current order 55.60 mL/min/1.73 m² → **G3a (45–59)**"), text);
        assertTrue(text.contains("eGFR earlier (3 months ago) 61.2 mL/min/1.73 m² → **G2 (60–89)**"), text);
    }

    @Test
    void validationCasesListOnlyAbnormalAndLargeChanges() throws Exception {
        String json = """
                {"user":"7","subject":"mixed","view":"cases","cases":[
                 {"label":1,"patient":{"sex":"M","ageYears":90,"ageCapped":true},"receivedDaysAgo":0,
                  "results":[{"code":"EGFR","name":"eGFR","value":"59","unit":"mL/ph/1.73m²","low":60.0,"high":null,"status":"TechnicalAcceptance"},
                             {"code":"GLU","name":"Glucose","value":"5.0","unit":"mmol/L","low":3.9,"high":6.1,"status":"TechnicalAcceptance"}],
                  "previous":[{"code":"EGFR","name":"eGFR","value":"75","unit":"mL/ph/1.73m²","low":60.0,"daysBefore":30}]},
                 {"label":2,"patient":{"sex":"F","ageMonths":5},"receivedDaysAgo":1,
                  "results":[{"code":"GLU","name":"Glucose","value":"5.0","unit":"mmol/L","low":3.9,"high":6.1,"status":"Finalized"}],
                  "previous":[]}]}
                """;
        ContextText.Built b = ContextText.build(JSON.readTree(json), ContextText.VALIDATION, null, Lang.VI);
        String text = b.text();
        assertTrue(text.contains("DANH SÁCH CHỜ DUYỆT: 2 ca."), text);
        assertTrue(text.contains("Ca 1: Nam, ≥ 90 tuổi; nhận mẫu hôm nay; 2 kết quả (2 chờ duyệt).\n"
                + "  - eGFR: 59 mL/ph/1.73m² (tham chiếu > 60) → THẤP | lần trước 75 (4 tuần trước), thay đổi −21%\n"), text);
        assertFalse(text.contains("- Glucose: 5.0"), text);
        assertTrue(text.contains("Ca 2: Nữ, 5 tháng tuổi; nhận mẫu 1 ngày trước; 1 kết quả.\n"
                + "  Tất cả trong khoảng tham chiếu, không lệch nhiều so với lần trước.\n"), text);
        assertTrue(text.contains("- Ca 1: eGFR phiếu đang xem 59 mL/ph/1,73 m² → **G3a (45–59)**"), text);
        assertEquals("page:validation", b.subjectKey());
        assertEquals("2 ca · 1 kết quả bất thường", b.summary());
    }

    @Test
    void aPageWithoutOrdersHasOnlyTheScreen() {
        ContextText.Built b = ContextText.build(null, ContextText.OTHER, "result-validation", Lang.EN);
        assertEquals("SCREEN: result-validation\n", b.text());
        assertEquals("route:result-validation", b.subjectKey());
    }

    @Test
    void relativeDays() {
        assertEquals("cùng ngày", ContextText.relative(0L, Lang.VI));
        assertEquals("5 ngày trước", ContextText.relative(5L, Lang.VI));
        assertEquals("3 tuần trước", ContextText.relative(21L, Lang.VI));
        assertEquals("3 tháng trước", ContextText.relative(92L, Lang.VI));
        assertEquals("2 năm trước", ContextText.relative(730L, Lang.VI));
        assertEquals("1 day ago", ContextText.relative(1L, Lang.EN));
        assertEquals("3 months ago", ContextText.relative(92L, Lang.EN));
    }

    @Test
    void longTextResultsAreCapped() throws Exception {
        JsonNode r = JSON.readTree("{\"name\":\"Note\",\"value\":\"" + "x".repeat(100) + "\"}");
        assertTrue(ContextText.resultLine(r, Lang.EN).endsWith("…"));
    }
}
