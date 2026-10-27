package org.openelisglobal.addons.assistant;

import java.time.Instant;
import java.util.List;

/**
 * Conversations and the usage log (which also counts the daily cap).
 * {@link MemoryAiStore} keeps everything in memory; {@link FileAiStore} also
 * appends the log to the add-on's data volume so the cap and the audit trail
 * survive a restart.
 */
public interface AiStore {

    /** Successful answers of the user since {@code since}. */
    int usedSince(String userId, Instant since);

    void log(LogEntry e);

    Conversation conversation(String id);

    /** The user's latest conversation (largest lastAt) about the subject. */
    Conversation latest(String userId, String subjectKey);

    void saveConversation(Conversation c);

    /** The turns (question + successful answer) of a conversation, oldest first. */
    List<Turn> turns(String conversationId);

    /** The user's recent conversations, newest first. */
    List<Conversation> recent(String userId, int limit);

    /** Deletes conversations and log entries older than {@code before}; returns the count removed. */
    int purge(Instant before);

    record LogEntry(String userId, String conversationId, String page, String contextHash, String question,
            String suggestionId, String answer, String provider, String model, int promptTokens, int completionTokens,
            int totalTokens, long latencyMs, String status, boolean egfrCorrected, String error) {
    }

    /**
     * A conversation, keyed by (user, subject). {@code subjectKey}: "patient:&lt;pseudonym&gt;"
     * on the patient results page, "page:…"/"route:…" elsewhere — NEVER sent to the
     * model. {@code claudeSessionId}: the {@code claude -p} session of this
     * conversation; {@code claudeTurns}: turns that session has seen (turns answered
     * by another provider are retold when Claude takes over again).
     */
    record Conversation(String id, String userId, String subjectKey, String subjectKind, String page, String title,
            String summaryLine, String claudeSessionId, int claudeTurns, String seedSummary, String previousId,
            String recap, Instant createdAt, Instant lastAt, int turns) {

        public Conversation touched(Instant at, String newTitle, String claudeSession, int newClaudeTurns) {
            return new Conversation(id, userId, subjectKey, subjectKind, page, title == null ? newTitle : title,
                    summaryLine, claudeSession, newClaudeTurns, seedSummary, previousId, recap, createdAt, at,
                    turns + 1);
        }

        public Conversation withRecap(String r) {
            return new Conversation(id, userId, subjectKey, subjectKind, page, title, summaryLine, claudeSessionId,
                    claudeTurns, seedSummary, previousId, r, createdAt, lastAt, turns);
        }
    }

    record Turn(String question, String answer, String provider, Instant at, String contextHash) {
    }
}
