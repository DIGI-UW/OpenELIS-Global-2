package org.openelisglobal.addons.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link MemoryAiStore} whose usage log is also appended to
 * {@code <dir>/usage.jsonl} (one JSON object per question: who, when, page,
 * de-identified question and answer, provider, tokens, status) and read back at
 * start, so the daily cap and the audit trail survive a restart. Conversations
 * themselves stay in memory. Entries older than the retention are dropped by
 * {@link #purge}.
 */
public class FileAiStore extends MemoryAiStore {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path file;

    public FileAiStore(Clock clock, Path dir) throws IOException {
        super(clock);
        Files.createDirectories(dir);
        this.file = dir.resolve("usage.jsonl");
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                try {
                    ObjectNode o = (ObjectNode) JSON.readTree(line);
                    restore(fromJson(o), Instant.parse(o.path("at").asText()));
                } catch (Exception e) {
                    // a torn last line after a crash: skip it
                }
            }
        }
    }

    @Override
    public synchronized void log(LogEntry e) {
        super.log(e);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            w.write(toJson(e, logTimes.get(logTimes.size() - 1)).toString());
            w.write('\n');
        } catch (IOException ex) {
            // the in-memory log still counts the cap until the next restart
        }
    }

    @Override
    public synchronized int purge(Instant before) {
        int n = super.purge(before);
        if (n > 0) {
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < logs.size(); i++) {
                lines.add(toJson(logs.get(i), logTimes.get(i)).toString());
            }
            try {
                Path tmp = file.resolveSibling("usage.jsonl.tmp");
                Files.write(tmp, lines, StandardCharsets.UTF_8);
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ex) {
                // keep the old file; the next purge tries again
            }
        }
        return n;
    }

    static ObjectNode toJson(LogEntry e, Instant at) {
        ObjectNode o = JSON.valueToTree(e);
        o.put("at", at.toString());
        return o;
    }

    static LogEntry fromJson(ObjectNode o) {
        return new LogEntry(o.path("userId").asText(), o.path("conversationId").asText(null),
                o.path("page").asText(null), o.path("contextHash").asText(null), o.path("question").asText(null),
                o.path("suggestionId").asText(null), o.path("answer").asText(null), o.path("provider").asText(null),
                o.path("model").asText(null), o.path("promptTokens").asInt(), o.path("completionTokens").asInt(),
                o.path("totalTokens").asInt(), o.path("latencyMs").asLong(), o.path("status").asText(),
                o.path("egfrCorrected").asBoolean(), o.path("error").asText(null));
    }
}
