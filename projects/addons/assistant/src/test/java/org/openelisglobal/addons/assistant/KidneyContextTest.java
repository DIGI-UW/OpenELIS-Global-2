package org.openelisglobal.addons.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** KDIGO facts go into the context; the Claude session is told when its previous answer was corrected. */
class KidneyContextTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode node(String json) throws Exception {
        return JSON.readTree(json);
    }

    @Test
    void factsFromResultsAndPrevious() throws Exception {
        List<JsonNode> results = List.of(
                node("{\"code\":\"EGFR\",\"name\":\"Độ lọc cầu thận eGFR\",\"value\":\"59\",\"unit\":\"mL/ph/1.73m²\"}"),
                node("{\"code\":\"ACR\",\"name\":\"Micro ALB/CRE (Ratio)\",\"value\":\"7.77\",\"unit\":\"mg/g\"}"),
                node("{\"code\":\"CREA\",\"name\":\"Creatinine\",\"value\":\"1.10\",\"unit\":\"mg/dl\"}"));
        List<JsonNode> previous = List.of(node(
                "{\"code\":\"EGFR\",\"name\":\"Độ lọc cầu thận eGFR\",\"value\":\"75\",\"unit\":\"mL/ph/1.73m²\",\"daysBefore\":92}"));
        List<Kdigo.Fact> facts = ContextText.kidneyFacts(null, results, previous, Lang.VI);
        assertEquals(3, facts.size(), facts.toString());
        assertEquals("G3a", facts.get(0).category());
        assertEquals("A1", facts.get(1).category());
        assertEquals("G2", facts.get(2).category());
        assertTrue(!facts.get(2).current());
        assertEquals("3 tháng trước", facts.get(2).when());
        assertTrue(ReferenceTables.select(List.of("ACR", "Micro ALB/CRE (Ratio)")).contains("ckd"));
    }

    @Test
    void egfrIsRecognisedByLoincWhateverItsName() throws Exception {
        List<Kdigo.Fact> facts = ContextText.kidneyFacts("Case 1",
                List.of(node("{\"code\":\"X9\",\"loinc\":\"98979-8\",\"name\":\"Filtration\",\"value\":\"44\"}")),
                List.of(), Lang.EN);
        assertEquals("G3b", facts.get(0).category());
        assertEquals("mL/min/1.73 m²", facts.get(0).unit());
    }

    @Test
    void claudeSessionIsToldAboutCorrectionAndGetsFactsEveryTurn() {
        List<Kdigo.Fact> facts = List.of(Kdigo.egfrFact(null, "59", null));
        String text = "NGỮ CẢNH\n" + Kdigo.factsBlock(facts);
        ContextText.Built ctx = new ContextText.Built(ContextText.LOOKUP, text, ContextText.sha256(text),
                List.of(59.0), List.of("EGFR"), List.of(), List.<Map<String, String>>of(), "", "patient:1", "patient",
                facts, "1");
        AiStore.Turn prev = new AiStore.Turn("Phân độ?",
                "eGFR 59 → G2.\n\n⚠️ Hệ thống đã tự sửa phân độ thận: eGFR 59 thuộc G3a (45–59 mL/ph/1,73 m², KDIGO), không phải \"G2\".\n\nGợi ý tham khảo — không thay chẩn đoán của bác sĩ.",
                "claude-runner", Instant.now(), ctx.hash());
        String msg = ClaudeRunnerProvider.message(new AiProvider.Request("r", Rules.current(Lang.VI), ctx, "Hỏi lại?",
                List.of(prev), null, "sess", 1, null));
        assertTrue(msg.contains("NGỮ CẢNH: không đổi so với lượt trước."), msg);
        assertTrue(msg.contains("câu trả lời trước của bạn đã bị hệ thống sửa"), msg);
        assertTrue(msg.contains("→ **G3a (45–59)**"), msg);
    }
}
