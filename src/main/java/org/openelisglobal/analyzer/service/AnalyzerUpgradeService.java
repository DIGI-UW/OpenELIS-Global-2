package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerUpgradePreparationService.ProfileSelection;
import org.openelisglobal.configuration.service.ConfigurationImportRunService;
import org.springframework.stereotype.Service;

/**
 * Resumable coordinator. Bridge connection creation runs outside the local
 * preparation transaction.
 */
@Service
public class AnalyzerUpgradeService {
    private static final String RUN_SOURCE = "ANALYZER_UPGRADE";
    private final AnalyzerService analyzers;
    private final AnalyzerUpgradePreparationService preparation;
    private final AnalyzerInstanceService instances;
    private final ConfigurationImportRunService runs;
    private final ObjectMapper json = new ObjectMapper();

    public record Outcome(String analyzerId, String name, String status, String reason) {
    }

    public AnalyzerUpgradeService(AnalyzerService analyzers, AnalyzerUpgradePreparationService preparation,
            AnalyzerInstanceService instances, ConfigurationImportRunService runs) {
        this.analyzers = analyzers;
        this.preparation = preparation;
        this.instances = instances;
        this.runs = runs;
    }

    public synchronized List<Outcome> migrate(Map<String, ProfileSelection> selections, String actor) {
        var pendingIds = preparation.pendingIds();
        var candidates = analyzers.getAllWithBindings().stream()
                .filter(analyzer -> pendingIds.contains(analyzer.getId())).toList();
        if (candidates.isEmpty())
            return List.of();
        var run = runs.start(RUN_SOURCE, actor);
        List<Outcome> outcomes = new ArrayList<>();
        try {
            for (var analyzer : candidates) {
                try {
                    var prepared = preparation.prepare(analyzer.getId(), selections.get(analyzer.getId()), actor);
                    var view = instances.ensureConnection(prepared.analyzerId(), prepared.values(), actor);
                    if (!view.connected())
                        throw new IllegalStateException(view.connectionErrorKey());
                    outcomes.add(new Outcome(analyzer.getId(), analyzer.getName(), "MIGRATED", null));
                } catch (RuntimeException exception) {
                    outcomes.add(new Outcome(analyzer.getId(), analyzer.getName(), "PENDING",
                            exception.getMessage() == null ? exception.getClass().getSimpleName()
                                    : exception.getMessage()));
                }
            }
            return List.copyOf(outcomes);
        } finally {
            try {
                runs.finish(run, json.writeValueAsString(outcomes),
                        outcomes.stream().anyMatch(row -> "PENDING".equals(row.status())));
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    public List<Outcome> pending() {
        Map<String, Outcome> last = new java.util.HashMap<>();
        var history = runs.getAllMatchingOrdered("source", RUN_SOURCE, "startedAt", true);
        if (!history.isEmpty() && history.get(0).getSummary() != null) {
            try {
                List<Outcome> outcomes = json.readValue(history.get(0).getSummary(),
                        new TypeReference<List<Outcome>>() {
                        });
                outcomes.forEach(row -> last.put(row.analyzerId(), row));
            } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
                throw new IllegalStateException("Invalid analyzer upgrade summary", exception);
            }
        }
        var pendingIds = preparation.pendingIds();
        return analyzers.getAllWithBindings().stream().filter(analyzer -> pendingIds.contains(analyzer.getId()))
                .map(analyzer -> last.getOrDefault(analyzer.getId(),
                        new Outcome(analyzer.getId(), analyzer.getName(), "PENDING", null)))
                .toList();
    }
}
