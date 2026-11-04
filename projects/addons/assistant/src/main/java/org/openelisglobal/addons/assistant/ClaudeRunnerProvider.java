package org.openelisglobal.addons.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Claude provider: POST to a runner service on the host (see the
 * {@code k37-ai} runner in the README) that runs {@code claude -p} WITHOUT
 * tools, one session per conversation: {@code --session-id <uuid>} on the first
 * turn, {@code --resume <uuid>} after. The add-on container cannot run claude
 * itself. An error or a 503 (busy, not logged in, not running) makes the chain
 * try the fallback provider.
 *
 * <p>
 * Configuration: {@code runnerUrl} (e.g. http://host.containers.internal:4190,
 * its host must be in the manifest's {@code network.egress}) and the secret
 * {@code runnerToken} (header X-K37-Token, shared with the runner).
 */
public class ClaudeRunnerProvider implements AiProvider {

    public static final String NAME = Settings.CLAUDE;

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Start of the warning line {@link EgfrCheck} adds to a corrected answer. */
    static String correctionMark(Lang lang) {
        return EgfrCheck.Words.of(lang).correctionMark();
    }

    private static final HttpClient HTTP = Egress.client(5);

    private final String url;
    private final String token;
    private final int timeoutSeconds;
    private final HttpClient http;

    public ClaudeRunnerProvider(String url, String token, int timeoutSeconds) {
        this.url = url == null || url.isBlank() ? "" : url.replaceAll("/+$", "");
        this.token = token == null ? "" : token.trim();
        this.timeoutSeconds = timeoutSeconds <= 0 ? 120 : timeoutSeconds;
        this.http = HTTP;
    }

    public static ClaudeRunnerProvider from(Settings s) {
        return new ClaudeRunnerProvider(s.runnerUrl, s.runnerToken, Math.max(s.timeoutSeconds, 120));
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean available() {
        return !token.isEmpty() && !url.isEmpty();
    }

    /**
     * The message for the claude session. First turn (no session yet): retell the
     * summary and the earlier turns of THIS conversation (e.g. turns the fallback
     * answered). Later turns: only the turns the session has not seen.
     */
    public static String message(Request req) {
        StringBuilder sb = new StringBuilder();
        boolean vi = req.lang().vi();
        boolean resume = req.claudeSessionId() != null;
        if (!resume && req.seedSummary() != null && !req.seedSummary().isBlank()) {
            sb.append(vi ? "TÓM TẮT HỘI THOẠI TRƯỚC (cùng chủ đề):\n"
                    : "SUMMARY OF THE PREVIOUS CONVERSATION (same subject):\n").append(req.seedSummary().strip())
                    .append("\n\n");
        }
        List<AiStore.Turn> h = req.history();
        int from = resume ? Math.min(Math.max(0, req.claudeTurns()), h.size()) : 0;
        if (from < h.size()) {
            sb.append(resume
                    ? (vi ? "CÁC LƯỢT TRƯỚC DO HỆ THỐNG DỰ PHÒNG TRẢ LỜI:\n" : "EARLIER TURNS ANSWERED BY THE FALLBACK:\n")
                    : (vi ? "CÁC LƯỢT TRƯỚC TRONG HỘI THOẠI NÀY:\n" : "EARLIER TURNS IN THIS CONVERSATION:\n"));
            for (AiStore.Turn t : h.subList(from, h.size())) {
                sb.append(vi ? "- Hỏi: " : "- Q: ").append(t.question().strip()).append(vi ? "\n  Đáp: " : "\n  A: ")
                        .append(t.answer() == null ? "" : t.answer().strip()).append('\n');
            }
            sb.append('\n');
        }
        // the session only remembers its RAW answer; if the check corrected it, tell the session
        AiStore.Turn last = h.isEmpty() ? null : h.get(h.size() - 1);
        if (resume && last != null && last.answer() != null) {
            int i = last.answer().indexOf(correctionMark(req.lang()));
            if (i >= 0) {
                String line = last.answer().substring(i).split("\n", 2)[0].strip();
                sb.append(vi ? "LƯU Ý: câu trả lời trước của bạn đã bị hệ thống sửa phân độ: "
                        : "NOTE: the system corrected the staging in your previous answer: ").append(line)
                        .append(vi ? " Dùng đúng PHÂN ĐỘ KDIGO DO MÁY CHỦ TÍNH SẴN.\n\n"
                                : " Use the KDIGO STAGES COMPUTED BY THE SERVER.\n\n");
            }
        }
        // the session saw the same context last turn (same hash): do not send it again (it remembers),
        // except the KDIGO facts (short), repeated every turn
        boolean sameContext = resume && from >= h.size() && last != null && req.ctx().hash().equals(last.contextHash());
        if (sameContext) {
            sb.append(vi ? "NGỮ CẢNH: không đổi so với lượt trước.\n\n" : "CONTEXT: unchanged since the last turn.\n\n");
            String facts = Kdigo.factsBlock(req.ctx().kidneyFacts(), req.lang());
            if (!facts.isEmpty()) {
                sb.append(facts).append('\n');
            }
            sb.append(vi ? "CÂU HỎI: " : "QUESTION: ").append(req.question().strip());
        } else {
            sb.append(PromptBuilder.userMessage(req.rules(), req.ctx(), req.question(), req.lang()));
        }
        return sb.toString();
    }

    @Override
    public LlmClient.Reply answer(Request req) throws LlmClient.LlmException, InterruptedException {
        if (!available()) {
            throw new LlmClient.LlmException("claude runner not configured (runnerUrl, runnerToken)", 503);
        }
        if (!Egress.permits(url + "/ask")) {
            throw new LlmClient.LlmException("runner host is not in the add-on's network.egress", 503);
        }
        boolean resume = req.claudeSessionId() != null;
        String sessionId = resume ? req.claudeSessionId() : UUID.randomUUID().toString();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", req.requestId());
        body.put("sessionId", sessionId);
        body.put("resume", resume);
        body.put("system", req.rules().system);
        body.put("message", message(req));
        HttpResponse<String> res;
        try {
            res = http.send(HttpRequest.newBuilder(URI.create(url + "/ask"))
                    .timeout(Duration.ofSeconds(timeoutSeconds + 10)).header("Content-Type", "application/json")
                    .header("X-K37-Token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new LlmClient.LlmException("claude runner unreachable: " + e.getClass().getSimpleName(), 503);
        }
        if (res.statusCode() != 200) {
            throw new LlmClient.LlmException("claude runner HTTP " + res.statusCode(), res.statusCode());
        }
        try {
            JsonNode j = JSON.readTree(res.body());
            String answer = j.path("answer").asText("");
            if (answer.isBlank()) {
                throw new LlmClient.LlmException("claude runner empty answer", 502);
            }
            JsonNode u = j.path("usage");
            int in = u.path("input_tokens").asInt(0) + u.path("cache_read_input_tokens").asInt(0)
                    + u.path("cache_creation_input_tokens").asInt(0);
            int out = u.path("output_tokens").asInt(0);
            return new LlmClient.Reply(answer, in, out, in + out, j.path("model").asText("claude"),
                    j.path("sessionId").asText(sessionId));
        } catch (IOException e) {
            throw new LlmClient.LlmException("claude runner bad JSON", 502);
        }
    }
}
