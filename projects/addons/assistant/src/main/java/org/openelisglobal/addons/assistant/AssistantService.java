package org.openelisglobal.addons.assistant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * One question: daily cap → de-identified context from OpenELIS → the
 * conversation (user, subject) → providers in order → post-answer check (KDIGO
 * staging, disclaimer) → usage log and conversation. Free of any HTTP server so
 * that tests run it directly.
 */
public class AssistantService {

    /** Where events go (SSE, or collected into one JSON). */
    public interface Sink {
        void event(String name, Map<String, Object> data);
    }

    /** Where the context comes from (OpenELIS through the frame's token). */
    public interface ContextSource {
        /** @param page {@link ContextText#LOOKUP}, {@link ContextText#VALIDATION} or {@link ContextText#OTHER} */
        record Request(String token, String page, String pageName) {
        }

        ContextText.Built build(Request req, Lang lang) throws ContextException;

        /** The text without identifiers of the patients in the token's context. */
        String deidentify(Request req, String text) throws ContextException;
    }

    /** OpenELIS refused the token (code "token") or could not be reached ("openelis"). */
    public static class ContextException extends Exception {
        private static final long serialVersionUID = 1L;
        public final String code;

        public ContextException(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    public record Ask(String token, String page, String pageName, String question, String suggestionId,
            String conversationId, boolean newConversation) {
        ContextSource.Request request() {
            return new ContextSource.Request(token, page, pageName);
        }
    }

    public static final int QUESTION_MAX = 1000;
    public static final int SUMMARY_WORDS = 150;

    private final Supplier<Settings> settings;
    private final ContextSource contexts;
    private final AiStore store;
    private final Function<Settings, List<AiProvider>> providers;
    private final Clock clock;
    private volatile Instant lastPurge = Instant.EPOCH;

    public AssistantService(Supplier<Settings> settings, ContextSource contexts, AiStore store,
            Function<Settings, List<AiProvider>> providers, Clock clock) {
        this.settings = settings;
        this.contexts = contexts;
        this.store = store;
        this.providers = providers;
        this.clock = clock;
    }

    /** The real providers: the configured one, then the fallback. */
    public static List<AiProvider> configuredProviders(Settings s) {
        List<AiProvider> out = new ArrayList<>();
        for (String name : List.of(s.provider, s.fallbackProvider)) {
            AiProvider p = Settings.CLAUDE.equals(name) ? ClaudeRunnerProvider.from(s)
                    : Settings.OPENAI.equals(name) ? new OpenAiCompatibleProvider(s) : null;
            if (p != null && out.stream().noneMatch(x -> x.name().equals(p.name()))) {
                out.add(p);
            }
        }
        return out;
    }

    List<AiProvider> chain(Settings s) {
        return providers.apply(s).stream().filter(AiProvider::available).toList();
    }

    public Quota.Status quota(String userId, Settings s) {
        Instant start = Quota.dayStart(clock.instant(), s.zone);
        return Quota.status(store.usedSince(userId, start), s.dailyQuota);
    }

    static Map<String, Object> quotaMap(Quota.Status q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("used", q.used());
        m.put("limit", q.limit());
        m.put("remaining", q.remaining());
        return m;
    }

    private void purgeMaybe(Settings s) {
        Instant now = clock.instant();
        if (Duration.between(lastPurge, now).toHours() >= 1) {
            lastPurge = now;
            store.purge(now.minus(Duration.ofDays(s.retentionDays)));
        }
    }

    // ------------------------------------------------------------------ status

    /** State, suggestions and the current conversation of the page (no model call, nothing created). */
    public Map<String, Object> status(Ask ask) throws ContextException {
        Settings s = settings.get();
        ContextText.Built ctx = contexts.build(ask.request(), s.lang);
        purgeMaybe(s);
        Map<String, Object> body = new LinkedHashMap<>();
        List<AiProvider> chain = chain(s);
        body.put("enabled", !chain.isEmpty());
        if (chain.isEmpty()) {
            body.put("reason", "not-configured");
        }
        body.put("language", s.lang.code);
        body.put("quota", quotaMap(quota(ctx.userId(), s)));
        body.put("providers", chain.stream().map(AiProvider::name).toList());
        body.put("idleMinutes", s.idleMinutes);
        body.put("page", ctx.page());
        body.put("subjectKind", ctx.subjectKind());
        body.put("summary", ctx.summary());
        body.put("caseLabels", ctx.caseLabels());
        body.put("suggestions", Suggestions.forContext(ctx).stream().map(Suggestions.Suggestion::toMap).toList());
        Instant now = clock.instant();
        AiStore.Conversation c = Conversations.resolve(store, ctx.userId(), ctx.subjectKey(), ask.conversationId(),
                false, now, s.idleMinutes);
        body.put("conversation", c == null ? null : conversationMap(c, true, s));
        AiStore.Conversation prev = c == null
                ? Conversations.continuable(store, ctx.userId(), ctx.subjectKey(), now, s.idleMinutes)
                : null;
        body.put("previous", prev == null ? null : conversationMap(prev, false, s));
        return body;
    }

    Map<String, Object> conversationMap(AiStore.Conversation c, boolean withTurns, Settings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.id());
        m.put("subjectKind", c.subjectKind());
        m.put("page", c.page());
        m.put("title", c.title());
        m.put("summaryLine", c.summaryLine());
        m.put("lastAt", c.lastAt().toString());
        m.put("turnCount", c.turns());
        m.put("seeded", c.seedSummary() != null);
        m.put("active", Conversations.active(c, clock.instant(), s.idleMinutes));
        if (withTurns) {
            List<Map<String, Object>> turns = new ArrayList<>();
            for (AiStore.Turn t : store.turns(c.id())) {
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("question", t.question());
                tm.put("answer", t.answer());
                tm.put("provider", t.provider());
                tm.put("at", t.at().toString());
                turns.add(tm);
            }
            m.put("turns", turns);
        }
        return m;
    }

    /** The user's conversations, newest first (the user comes from OpenELIS, through the token). */
    public List<Map<String, Object>> history(Ask ask, int limit) throws ContextException {
        Settings s = settings.get();
        String user = contexts.build(ask.request(), s.lang).userId();
        return store.recent(user, Math.min(Math.max(limit, 1), 50)).stream().map(c -> conversationMap(c, false, s))
                .toList();
    }

    /** One conversation, for its owner only. */
    public Map<String, Object> conversation(Ask ask, String id) throws ContextException {
        Settings s = settings.get();
        String user = contexts.build(ask.request(), s.lang).userId();
        AiStore.Conversation c = store.conversation(id);
        return c == null || !c.userId().equals(user) ? null : conversationMap(c, true, s);
    }

    // ------------------------------------------------------------------ ask

    /** Asks. Everything goes through {@code sink}: meta → delta* → (reset) → done | error. */
    public void ask(Ask ask, Sink sink) throws InterruptedException {
        long t0 = System.currentTimeMillis();
        Settings s = settings.get();
        String rawQuestion = ask.question() == null ? "" : ask.question().strip();
        if (rawQuestion.isEmpty()) {
            sink.event("error", Map.of("code", "empty"));
            return;
        }
        if (rawQuestion.length() > QUESTION_MAX) {
            rawQuestion = rawQuestion.substring(0, QUESTION_MAX);
        }
        ContextText.Built ctx;
        String question;
        try {
            ctx = contexts.build(ask.request(), s.lang);
            // the user may type a name or a lab number into the question: OpenELIS removes it
            question = contexts.deidentify(ask.request(), rawQuestion);
        } catch (ContextException e) {
            sink.event("error", Map.of("code", e.code));
            return;
        }
        String user = ctx.userId();
        if (user == null || user.isBlank()) {
            sink.event("error", Map.of("code", "token"));
            return;
        }
        Quota.Status q = quota(user, s);
        if (!q.allowed()) {
            sink.event("error", Map.of("code", "quota", "quota", quotaMap(q)));
            return;
        }
        List<AiProvider> chain = chain(s);
        if (chain.isEmpty()) {
            sink.event("error", Map.of("code", "not-configured"));
            return;
        }
        Instant now = clock.instant();
        AiStore.Conversation conv = Conversations.resolve(store, user, ctx.subjectKey(), ask.conversationId(),
                ask.newConversation(), now, s.idleMinutes);
        boolean isNew = conv == null;
        if (isNew) {
            conv = new AiStore.Conversation(UUID.randomUUID().toString(), user, ctx.subjectKey(), ctx.subjectKind(),
                    ctx.page(), null, ctx.summary(), null, 0, null, null, null, now, now, 0);
        }
        List<AiStore.Turn> history = isNew ? List.of() : store.turns(conv.id());
        Rules rules = Rules.current(s.lang).filled(s.labName, s.productName);
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("requestId", requestId);
        meta.put("conversationId", conv.id());
        meta.put("newConversation", isNew);
        meta.put("summary", ctx.summary());
        meta.put("questionScrubbed", !question.equals(rawQuestion));
        sink.event("meta", meta);

        List<String> errors = new ArrayList<>();
        for (AiProvider p : chain) {
            AtomicBoolean streamed = new AtomicBoolean(false);
            try {
                LlmClient.Reply reply = p.answer(new AiProvider.Request(requestId, rules, ctx, question, history,
                        conv.seedSummary(), conv.claudeSessionId(), conv.claudeTurns(), s.lang, piece -> {
                            streamed.set(true);
                            sink.event("delta", Map.of("t", piece));
                        }));
                AnswerPostProcessor.Processed done = AnswerPostProcessor.process(reply.text(), ctx.egfrValues(),
                        ctx.kidneyFacts(), s.lang);
                long latency = System.currentTimeMillis() - t0;
                boolean claude = ClaudeRunnerProvider.NAME.equals(p.name());
                AiStore.Conversation saved = conv.touched(clock.instant(), cut(question, 120),
                        claude ? reply.sessionId() : conv.claudeSessionId(),
                        claude ? conv.turns() + 1 : conv.claudeTurns());
                store.saveConversation(saved);
                store.log(new AiStore.LogEntry(user, conv.id(), ctx.page(), ctx.hash(), question, ask.suggestionId(),
                        done.text(), p.name(), reply.model(), reply.promptTokens(), reply.completionTokens(),
                        reply.totalTokens(), latency, "ok", done.egfrCorrected(),
                        errors.isEmpty() ? null : String.join(" | ", errors)));
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("answer", done.text());
                out.put("provider", p.name());
                out.put("model", reply.model());
                out.put("egfrCorrected", done.egfrCorrected());
                out.put("latencyMs", latency);
                out.put("conversationId", conv.id());
                out.put("caseLabels", ctx.caseLabels());
                out.put("quota", quotaMap(Quota.status(q.used() + 1, q.limit())));
                sink.event("done", out);
                return;
            } catch (LlmClient.LlmException | RuntimeException e) {
                errors.add(p.name() + ": "
                        + (e instanceof LlmClient.LlmException ? e.getMessage() : e.getClass().getSimpleName()));
                if (streamed.get()) {
                    sink.event("reset", Map.of("provider", p.name()));
                }
            }
        }
        long latency = System.currentTimeMillis() - t0;
        store.log(new AiStore.LogEntry(user, isNew ? null : conv.id(), ctx.page(), ctx.hash(), question,
                ask.suggestionId(), null, null, null, 0, 0, 0, latency, "error", false, String.join(" | ", errors)));
        sink.event("error", Map.of("code", "provider"));
    }

    // ------------------------------------------------------------------ continue the previous conversation

    /**
     * "Continue the previous conversation": summarise (≤ 150 words, kept for next
     * time) the old conversation ABOUT THE SAME SUBJECT, then open a new one that
     * starts from the summary. No replay of the whole history; does not count
     * against the cap.
     */
    public Map<String, Object> continuePrevious(Ask ask, String previousId)
            throws InterruptedException, ContextException {
        Settings s = settings.get();
        ContextText.Built ctx = contexts.build(ask.request(), s.lang);
        AiStore.Conversation prev = store.conversation(previousId);
        if (prev == null || !prev.userId().equals(ctx.userId()) || !prev.subjectKey().equals(ctx.subjectKey())) {
            return null; // another user, or another subject (another patient): refused
        }
        String recap = prev.recap();
        if (recap == null || recap.isBlank()) {
            recap = summarize(store.turns(prev.id()), s);
            store.saveConversation(prev.withRecap(recap));
        }
        Instant now = clock.instant();
        AiStore.Conversation next = new AiStore.Conversation(UUID.randomUUID().toString(), ctx.userId(),
                ctx.subjectKey(), ctx.subjectKind(), ctx.page(), null, ctx.summary(), null, 0, recap, prev.id(), null,
                now, now, 0);
        store.saveConversation(next);
        Map<String, Object> m = conversationMap(next, true, s);
        m.put("seedSummary", recap);
        return m;
    }

    /** A cheap summary: the openai-compatible provider without streaming; without it, the questions. */
    String summarize(List<AiStore.Turn> turns, Settings s) throws InterruptedException {
        if (turns.isEmpty()) {
            return "";
        }
        StringBuilder transcript = new StringBuilder();
        for (AiStore.Turn t : turns) {
            transcript.append(s.lang.vi() ? "Hỏi: " : "Q: ").append(t.question())
                    .append(s.lang.vi() ? "\nĐáp: " : "\nA: ").append(t.answer()).append("\n\n");
        }
        AiProvider cheap = providers.apply(s).stream().filter(p -> OpenAiCompatibleProvider.NAME.equals(p.name()))
                .findFirst().orElse(null);
        if (cheap instanceof OpenAiCompatibleProvider o && o.available()) {
            String instruction = s.lang.vi()
                    ? "Tóm tắt hội thoại sau giữa kỹ thuật viên xét nghiệm và trợ lý, tiếng Việt, tối đa " + SUMMARY_WORDS
                            + " chữ, gạch đầu dòng: các kết quả/vấn đề đã bàn, kết luận/gợi ý chính, câu hỏi còn mở."
                            + " Không thêm thông tin mới, không có thông tin định danh, không có dòng miễn trừ."
                    : "Summarise this conversation between a laboratory technician and the assistant, in English, at"
                            + " most " + SUMMARY_WORDS + " words, as bullets: results/issues discussed, main"
                            + " conclusions/suggestions, open questions. No new information, nothing identifying, no"
                            + " disclaimer line.";
            try {
                String text = o.raw(List.of(Map.of("role", "system", "content", instruction),
                        Map.of("role", "user", "content", transcript.toString()))).text();
                return capWords(text.replace(s.lang.disclaimer, "").strip(), SUMMARY_WORDS);
            } catch (LlmClient.LlmException e) {
                // fall back to the questions
            }
        }
        StringBuilder sb = new StringBuilder(s.lang.vi() ? "Các câu đã hỏi:" : "Questions asked:");
        for (AiStore.Turn t : turns) {
            sb.append("\n- ").append(t.question());
        }
        return capWords(sb.toString(), SUMMARY_WORDS);
    }

    static String capWords(String s, int max) {
        String[] w = s.split("(?<=\\s)");
        if (w.length <= max) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(w[i]);
        }
        return sb.toString().strip() + "…";
    }

    private static String cut(String s, int n) {
        return s == null ? null : s.length() > n ? s.substring(0, n) + "…" : s;
    }
}
