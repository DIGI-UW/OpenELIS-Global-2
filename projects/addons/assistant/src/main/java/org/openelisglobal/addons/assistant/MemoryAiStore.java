package org.openelisglobal.addons.assistant;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** {@link AiStore} in memory. */
public class MemoryAiStore implements AiStore {

    public final List<LogEntry> logs = new ArrayList<>();
    public final List<Instant> logTimes = new ArrayList<>();
    public final Map<String, Conversation> conversations = new LinkedHashMap<>();
    protected final Clock clock;

    public MemoryAiStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized int usedSince(String userId, Instant since) {
        int n = 0;
        for (int i = 0; i < logs.size(); i++) {
            LogEntry e = logs.get(i);
            if (userId.equals(e.userId()) && "ok".equals(e.status()) && !logTimes.get(i).isBefore(since)) {
                n++;
            }
        }
        return n;
    }

    @Override
    public synchronized void log(LogEntry e) {
        logs.add(e);
        logTimes.add(clock.instant());
    }

    /** Loading a saved log: the entry with its original time. */
    protected synchronized void restore(LogEntry e, Instant at) {
        logs.add(e);
        logTimes.add(at);
    }

    @Override
    public synchronized Conversation conversation(String id) {
        return id == null ? null : conversations.get(id);
    }

    @Override
    public synchronized Conversation latest(String userId, String subjectKey) {
        return conversations.values().stream()
                .filter(c -> c.userId().equals(userId) && c.subjectKey().equals(subjectKey))
                .max(Comparator.comparing(Conversation::lastAt)).orElse(null);
    }

    @Override
    public synchronized void saveConversation(Conversation c) {
        conversations.put(c.id(), c);
    }

    @Override
    public synchronized List<Turn> turns(String conversationId) {
        List<Turn> out = new ArrayList<>();
        for (int i = 0; i < logs.size(); i++) {
            LogEntry e = logs.get(i);
            if (conversationId.equals(e.conversationId()) && "ok".equals(e.status())) {
                out.add(new Turn(e.question(), e.answer(), e.provider(), logTimes.get(i), e.contextHash()));
            }
        }
        return out;
    }

    @Override
    public synchronized List<Conversation> recent(String userId, int limit) {
        return conversations.values().stream().filter(c -> c.userId().equals(userId) && c.turns() > 0)
                .sorted(Comparator.comparing(Conversation::lastAt).reversed()).limit(limit).toList();
    }

    @Override
    public synchronized int purge(Instant before) {
        int n = 0;
        for (int i = logs.size() - 1; i >= 0; i--) {
            if (logTimes.get(i).isBefore(before)) {
                logs.remove(i);
                logTimes.remove(i);
                n++;
            }
        }
        n += conversations.values().removeIf(c -> c.lastAt().isBefore(before)) ? 1 : 0;
        return n;
    }
}
