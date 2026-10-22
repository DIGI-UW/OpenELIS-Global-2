package org.openelisglobal.addons.assistant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The service with an in-memory store, a fake context per subject, and fake providers that record every request. */
class Fixture {

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-14T03:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }
    }

    static class FakeProvider implements AiProvider {
        final String name;
        final List<Request> requests = new ArrayList<>();
        boolean fail;
        int sessionCounter;

        FakeProvider(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public LlmClient.Reply answer(Request req) throws LlmClient.LlmException {
            requests.add(req);
            if (fail) {
                throw new LlmClient.LlmException("down", 503);
            }
            String session = req.claudeSessionId() != null ? req.claudeSessionId() : "sess-" + (++sessionCounter);
            return new LlmClient.Reply("Trả lời cho: " + req.question(), 10, 5, 15, "fake", session);
        }
    }

    /** The context source of the tests: the token is the user id, the page name is the subject. */
    static final AssistantService.ContextSource CONTEXTS = new AssistantService.ContextSource() {
        @Override
        public ContextText.Built build(Request req, Lang lang) {
            return ctx(req.pageName(), req.token());
        }

        @Override
        public String deidentify(Request req, String text) {
            return text;
        }
    };

    final MutableClock clock = new MutableClock();
    final MemoryAiStore store = new MemoryAiStore(clock);
    final FakeProvider provider = new FakeProvider(ClaudeRunnerProvider.NAME);
    final Map<String, Object> config = new LinkedHashMap<>(
            Map.of("language", "vi", "dailyQuestionLimit", 20, "timeZone", "Asia/Ho_Chi_Minh"));
    final AssistantService service;

    /** A fake context: the text names the subject, to check for leaks. */
    static ContextText.Built ctx(String subject, String user) {
        String kind = subject.startsWith("patient:") ? "patient" : "page";
        String text = "NGỮ CẢNH CỦA " + subject + " — eGFR 55.60";
        return new ContextText.Built(kind.equals("patient") ? ContextText.LOOKUP : ContextText.VALIDATION, text,
                ContextText.sha256(text), List.of(55.6), List.of("EGFR"), List.of(), List.of(), "tóm tắt " + subject,
                subject, kind, null, user);
    }

    Fixture() {
        service = new AssistantService(() -> Settings.from(config), CONTEXTS, store, s -> List.of(provider), clock);
    }

    static AssistantService.Ask askOf(int user, String subject, String question, String conversationId,
            boolean newConversation) {
        return new AssistantService.Ask(String.valueOf(user), ContextText.LOOKUP, subject, question, null,
                conversationId, newConversation);
    }

    Map<String, Object> ask(int user, String subject, String question, String conversationId, boolean newConv)
            throws InterruptedException {
        Map<String, Object> result = new LinkedHashMap<>();
        service.ask(askOf(user, subject, question, conversationId, newConv), (name, data) -> {
            if (!"delta".equals(name)) {
                if (!"meta".equals(name)) {
                    result.put("event", name);
                }
                result.putAll(data);
            }
        });
        return result;
    }
}
