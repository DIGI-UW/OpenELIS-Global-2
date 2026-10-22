package org.openelisglobal.addons.assistant;

import java.util.List;
import java.util.function.Consumer;

/**
 * A provider that answers. Two kinds:
 * <ul>
 * <li>{@code openai-compatible} — an OpenAI-style chat API (OpenRouter,
 * DeepSeek, OpenAI…); streamed; the conversation's history is sent each
 * turn.</li>
 * <li>{@code claude-runner} — {@code claude -p} without tools, one session per
 * conversation, through a runner service on the host (see README).</li>
 * </ul>
 * The configured provider is tried first, then the fallback.
 */
public interface AiProvider {

    String name();

    boolean available();

    /**
     * One question in one conversation.
     *
     * @param history         earlier turns of THIS conversation (de-identified), oldest first
     * @param seedSummary     summary of the previous conversation ("continue"), may be null
     * @param claudeSessionId the conversation's claude session (null = none yet)
     * @param claudeTurns     turns that session has seen (turns answered by another provider are retold)
     * @param onDelta         receives pieces of text when the provider streams; may be null
     */
    record Request(String requestId, Rules rules, ContextText.Built ctx, String question,
            List<AiStore.Turn> history, String seedSummary, String claudeSessionId, int claudeTurns, Lang lang,
            Consumer<String> onDelta) {

        /** Vietnamese (tests of the original behaviour). */
        public Request(String requestId, Rules rules, ContextText.Built ctx, String question,
                List<AiStore.Turn> history, String seedSummary, String claudeSessionId, int claudeTurns,
                Consumer<String> onDelta) {
            this(requestId, rules, ctx, question, history, seedSummary, claudeSessionId, claudeTurns, Lang.VI,
                    onDelta);
        }
    }

    LlmClient.Reply answer(Request req) throws LlmClient.LlmException, InterruptedException;
}
