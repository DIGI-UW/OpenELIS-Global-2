package org.openelisglobal.addons.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-style chat API (OpenRouter, DeepSeek, OpenAI…). The conversation's
 * history is sent again each turn.
 */
public class OpenAiCompatibleProvider implements AiProvider {

    public static final String NAME = Settings.OPENAI;

    private final Settings settings;
    private final LlmClient client;

    public OpenAiCompatibleProvider(Settings settings) {
        this.settings = settings;
        this.client = new LlmClient(settings);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean available() {
        return settings.openAiConfigured();
    }

    /** [system, (user q, assistant a)…, user (previous summary + context + tables + question)]. */
    public static List<Map<String, String>> messages(Request req, int historyTurns) {
        List<Map<String, String>> m = new ArrayList<>();
        m.add(Map.of("role", "system", "content", req.rules().system));
        List<AiStore.Turn> h = req.history();
        for (AiStore.Turn t : h.subList(Math.max(0, h.size() - historyTurns), h.size())) {
            m.add(Map.of("role", "user", "content", t.question()));
            m.add(Map.of("role", "assistant", "content", t.answer() == null ? "" : t.answer()));
        }
        String seed = req.seedSummary() == null || req.seedSummary().isBlank() ? ""
                : (req.lang().vi() ? "TÓM TẮT HỘI THOẠI TRƯỚC (cùng chủ đề):\n"
                        : "SUMMARY OF THE PREVIOUS CONVERSATION (same subject):\n") + req.seedSummary().strip()
                        + "\n\n";
        m.add(Map.of("role", "user", "content",
                seed + PromptBuilder.userMessage(req.rules(), req.ctx(), req.question(), req.lang())));
        return m;
    }

    @Override
    public LlmClient.Reply answer(Request req) throws LlmClient.LlmException, InterruptedException {
        return client.chat(messages(req, settings.historyTurns), req.onDelta());
    }

    /** One call without streaming, with any messages (used to summarise). */
    public LlmClient.Reply raw(List<Map<String, String>> messages) throws LlmClient.LlmException, InterruptedException {
        return client.chat(messages, null);
    }
}
