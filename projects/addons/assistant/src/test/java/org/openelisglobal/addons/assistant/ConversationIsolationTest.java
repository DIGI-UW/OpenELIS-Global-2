package org.openelisglobal.addons.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConversationIsolationTest {

    @Test
    void followUpOnSamePatientCarriesHistoryAndSession() throws Exception {
        Fixture f = new Fixture();
        Map<String, Object> a1 = f.ask(1, "patient:A", "câu 1 về A", null, false);
        String convA = (String) a1.get("conversationId");
        f.clock.advance(Duration.ofMinutes(2));
        Map<String, Object> a2 = f.ask(1, "patient:A", "câu 2 về A", convA, false);
        assertEquals(convA, a2.get("conversationId"));
        AiProvider.Request second = f.provider.requests.get(1);
        assertEquals("sess-1", second.claudeSessionId()); // --resume đúng phiên của A
        assertEquals(1, second.history().size());
        assertEquals("câu 1 về A", second.history().get(0).question());
        // cùng ngữ cảnh + phiên đã thấy → không gửi lại khối ngữ cảnh
        String msg = ClaudeRunnerProvider.message(second);
        assertTrue(msg.contains("NGỮ CẢNH: không đổi") && !msg.contains("NGỮ CẢNH CỦA"), msg);
        // tầng openai-compatible (không giữ phiên) luôn gửi ngữ cảnh
        assertTrue(OpenAiCompatibleProvider.messages(second, 6).toString().contains("NGỮ CẢNH CỦA patient:A"));
    }

    /** Lượt hỏi tiếp trên bệnh nhân B KHÔNG được mang ngữ cảnh, lịch sử hay phiên claude của A. */
    @Test
    void switchingPatientNeverCarriesPatientAContext() throws Exception {
        Fixture f = new Fixture();
        Map<String, Object> a1 = f.ask(1, "patient:A", "câu về A", null, false);
        String convA = (String) a1.get("conversationId");
        f.clock.advance(Duration.ofMinutes(1));
        // trình duyệt (cố ý hoặc lỗi) gửi lại conversationId của A khi đang xem B
        Map<String, Object> b1 = f.ask(1, "patient:B", "câu về B", convA, false);
        assertNotEquals(convA, b1.get("conversationId"));
        AiProvider.Request rb = f.provider.requests.get(1);
        assertNull(rb.claudeSessionId(), "phiên claude của A bị dùng cho B");
        assertTrue(rb.history().isEmpty(), "lịch sử của A lọt sang B");
        assertFalse(rb.ctx().text().contains("patient:A"));
        String sentToClaude = ClaudeRunnerProvider.message(rb);
        assertFalse(sentToClaude.contains("câu về A"), sentToClaude);
        assertFalse(sentToClaude.contains("patient:A"), sentToClaude);
        String sentToOpenAi = OpenAiCompatibleProvider.messages(rb, 6).toString();
        assertFalse(sentToOpenAi.contains("câu về A"), sentToOpenAi);
        // follow-up trên B: phiên của B, không phải của A
        Map<String, Object> b2 = f.ask(1, "patient:B", "câu 2 về B", (String) b1.get("conversationId"), false);
        AiProvider.Request rb2 = f.provider.requests.get(2);
        assertEquals("sess-2", rb2.claudeSessionId());
        assertEquals(List.of("câu về B"), rb2.history().stream().map(AiStore.Turn::question).toList());
        // quay lại A trong hạn: đúng hội thoại của A
        Map<String, Object> a2 = f.ask(1, "patient:A", "câu 2 về A", (String) b2.get("conversationId"), false);
        assertEquals(convA, a2.get("conversationId"));
        assertEquals("sess-1", f.provider.requests.get(3).claudeSessionId());
    }

    @Test
    void otherUserCannotAttachToConversation() throws Exception {
        Fixture f = new Fixture();
        String conv = (String) f.ask(1, "patient:A", "của user 1", null, false).get("conversationId");
        Map<String, Object> other = f.ask(2, "patient:A", "user 2", conv, false);
        assertNotEquals(conv, other.get("conversationId"));
        assertTrue(f.provider.requests.get(1).history().isEmpty());
        assertNull(f.service.conversation(Fixture.askOf(2, "patient:A", null, null, false), conv));
        assertNotNull(f.service.conversation(Fixture.askOf(1, "patient:A", null, null, false), conv));
    }

    @Test
    void idleBeyondCutoffStartsNewConversationAndOffersContinue() throws Exception {
        Fixture f = new Fixture();
        String conv = (String) f.ask(1, "patient:A", "câu 1", null, false).get("conversationId");
        f.clock.advance(Duration.ofMinutes(6)); // > 5 phút (TTL cache chuẩn)
        Map<String, Object> st = f.service.status(Fixture.askOf(1, "patient:A", null, conv, false));
        assertNull(st.get("conversation"));
        assertEquals(conv, ((Map<?, ?>) st.get("previous")).get("id"));
        Map<String, Object> next = f.ask(1, "patient:A", "câu 2", conv, false);
        assertNotEquals(conv, next.get("conversationId"));
        assertNull(f.provider.requests.get(1).claudeSessionId());
        // "Tiếp tục hội thoại trước": hội thoại mới mang tóm tắt, không phát lại lịch sử
        f.clock.advance(Duration.ofMinutes(10));
        Map<String, Object> cont = f.service.continuePrevious(Fixture.askOf(1, "patient:A", null, null, true),
                (String) next.get("conversationId"));
        assertNotNull(cont);
        assertTrue(((String) cont.get("seedSummary")).contains("câu 2"));
        Map<String, Object> after = f.ask(1, "patient:A", "câu 3", (String) cont.get("id"), false);
        assertEquals(cont.get("id"), after.get("conversationId"));
        AiProvider.Request r = f.provider.requests.get(2);
        assertTrue(r.history().isEmpty());
        assertTrue(ClaudeRunnerProvider.message(r).contains("TÓM TẮT HỘI THOẠI TRƯỚC"));
        // tiếp tục hội thoại của bệnh nhân KHÁC → từ chối
        assertNull(f.service.continuePrevious(Fixture.askOf(1, "patient:B", null, null, true), conv));
    }

    @Test
    void newConversationButtonForcesFreshSession() throws Exception {
        Fixture f = new Fixture();
        String conv = (String) f.ask(1, "page:validation", "q1", null, false).get("conversationId");
        Map<String, Object> fresh = f.ask(1, "page:validation", "q2", conv, true);
        assertNotEquals(conv, fresh.get("conversationId"));
        assertNull(f.provider.requests.get(1).claudeSessionId());
    }

    @Test
    void fallbackTurnIsReplayedToClaudeLater() throws Exception {
        Fixture f = new Fixture();
        Fixture.FakeProvider cheap = new Fixture.FakeProvider(OpenAiCompatibleProvider.NAME);
        AssistantService svc = new AssistantService(() -> Settings.from(f.config), Fixture.CONTEXTS, f.store,
                s -> List.of(f.provider, cheap), f.clock);
        java.util.function.BiFunction<String, String, String> ask = (q, c) -> {
            Map<String, Object> m = new java.util.HashMap<>();
            try {
                svc.ask(Fixture.askOf(1, "patient:A", q, c, false), (n, d) -> m.putAll(d));
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return (String) m.get("conversationId");
        };
        String conv = ask.apply("q1", null); // claude
        f.provider.fail = true;
        ask.apply("q2", conv); // claude lỗi → openai-compatible trả lời
        f.provider.fail = false;
        ask.apply("q3", conv); // về claude: phải được kể lại lượt q2
        AiProvider.Request last = f.provider.requests.get(f.provider.requests.size() - 1);
        assertEquals("sess-1", last.claudeSessionId());
        String msg = ClaudeRunnerProvider.message(last);
        assertTrue(msg.contains("HỆ THỐNG DỰ PHÒNG") && msg.contains("q2") && !msg.contains("Hỏi: q1"), msg);
    }
}
