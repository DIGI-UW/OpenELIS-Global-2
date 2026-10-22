package org.openelisglobal.addons.assistant;

import java.time.Duration;
import java.time.Instant;

/**
 * Which conversation a question belongs to (user, subject):
 * <ul>
 * <li>The subject comes from the SERVER side (OpenELIS's pseudonym of the
 * patient on the patient results page, the page elsewhere). A
 * {@code conversationId} from the browser is only a hint: used only for the
 * same user, the same subject, and while still active.</li>
 * <li>Active = last turn at most {@code idleMinutes} ago (default 5, the usual
 * prompt cache lifetime). Otherwise a NEW conversation; the old one, if it has
 * turns, is offered as "continue the previous conversation" (a new conversation
 * that starts from a summary, not a replay of the history).</li>
 * <li>Another patient = another subject = another conversation (never patient
 * A's context or Claude session for patient B).</li>
 * </ul>
 */
public final class Conversations {

    private Conversations() {
    }

    public static boolean active(AiStore.Conversation c, Instant now, int idleMinutes) {
        return c != null && !c.lastAt().isBefore(now.minus(Duration.ofMinutes(idleMinutes)));
    }

    /** The usable conversation for (user, subject), or null = start a new one. */
    public static AiStore.Conversation resolve(AiStore store, String userId, String subjectKey, String requestedId,
            boolean forceNew, Instant now, int idleMinutes) {
        if (forceNew) {
            return null;
        }
        if (requestedId != null && !requestedId.isBlank()) {
            AiStore.Conversation c = store.conversation(requestedId);
            if (c != null && c.userId().equals(userId) && c.subjectKey().equals(subjectKey)
                    && active(c, now, idleMinutes)) {
                return c;
            }
        }
        AiStore.Conversation latest = store.latest(userId, subjectKey);
        return active(latest, now, idleMinutes) ? latest : null;
    }

    /** An expired conversation with turns about this subject, to offer "continue". */
    public static AiStore.Conversation continuable(AiStore store, String userId, String subjectKey, Instant now,
            int idleMinutes) {
        AiStore.Conversation latest = store.latest(userId, subjectKey);
        return latest != null && latest.turns() > 0 && !active(latest, now, idleMinutes) ? latest : null;
    }
}
