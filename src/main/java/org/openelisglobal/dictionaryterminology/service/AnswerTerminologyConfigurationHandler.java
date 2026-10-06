package org.openelisglobal.dictionaryterminology.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Loads the {@code answer-terminology} catalog domain from CSV: the standard
 * codes (LOINC, SNOMED, CIEL, OCL) an answer carries.
 * <p>
 * Columns: {@code category,dictEntry,source,code,relationship,displayName}.
 * {@code category} and {@code dictEntry} name the answer as the dictionary CSV
 * does; {@code source} and {@code code} are required; {@code relationship} is
 * SAME_AS (default), BROADER_THAN or NARROWER_THAN. Rows are merged into the
 * answer's mappings by (source, code): a mapping the answer already has takes
 * the row's relationship and display name, a new one is added, the others are
 * kept. Written through
 * {@link DictionaryTerminologyMappingService#saveMappingsForDictionary}.
 */
@Component
public class AnswerTerminologyConfigurationHandler extends AbstractCatalogCsvHandler {

    private static final Set<String> SOURCES = Set.of("LOINC", "SNOMED", "CIEL", "OCL");
    private static final Set<String> RELATIONSHIPS = Set.of("SAME_AS", "BROADER_THAN", "NARROWER_THAN");

    @Autowired
    private DictionaryTerminologyMappingService mappingService;

    @Autowired
    private DictionaryService dictionaryService;

    @Override
    public String getDomainName() {
        return "answer-terminology";
    }

    /** After the dictionaries (300) that define the answers. */
    @Override
    public int getLoadOrder() {
        return 310;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "category", "dictEntry", "source", "code" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        Map<String, List<CsvRow>> byAnswer = new LinkedHashMap<>();
        for (CsvRow row : rows) {
            byAnswer.computeIfAbsent(row.get("category") + "|" + row.get("dictEntry"), k -> new ArrayList<>()).add(row);
        }
        for (List<CsvRow> group : byAnswer.values()) {
            List<LoadedRow<String>> outcomes;
            try {
                outcomes = transaction.run(() -> loadAnswer(group));
            } catch (Exception e) {
                String reason = CsvLoadSummary.reason(e);
                outcomes = new ArrayList<>();
                for (int i = 0; i < group.size(); i++) {
                    outcomes.add(LoadedRow.skipped(reason));
                }
            }
            for (int i = 0; i < group.size(); i++) {
                summary.record(outcomes.get(i), getClass().getSimpleName(), group.get(i).lineNumber());
            }
        }
    }

    private List<LoadedRow<String>> loadAnswer(List<CsvRow> group) {
        CsvRow first = group.get(0);
        List<LoadedRow<String>> outcomes = new ArrayList<>();
        Dictionary answer = dictionaryService.getDictionaryEntryByNameAndCategoryName(first.get("dictEntry"),
                first.get("category"));
        if (answer == null) {
            group.forEach(row -> outcomes.add(LoadedRow.skipped(
                    "answer '" + first.get("dictEntry") + "' not found in category '" + first.get("category") + "'")));
            return outcomes;
        }

        Map<String, DictionaryTerminologyMapping> byKey = new LinkedHashMap<>();
        for (DictionaryTerminologyMapping existing : mappingService.getActiveByDictionaryId(answer.getId())) {
            byKey.put(key(existing.getSource(), existing.getCode()), existing);
        }
        List<Boolean> createdFlags = new ArrayList<>();
        for (CsvRow row : group) {
            String source = row.get("source").toUpperCase(Locale.ROOT);
            String code = row.get("code");
            if (!SOURCES.contains(source)) {
                throw new IllegalArgumentException("source must be one of " + SOURCES);
            }
            if (code.isEmpty()) {
                throw new IllegalArgumentException("line " + row.lineNumber() + " has no code");
            }
            String relationship = row.get("relationship").toUpperCase(Locale.ROOT);
            if (relationship.isEmpty()) {
                relationship = "SAME_AS";
            }
            if (!RELATIONSHIPS.contains(relationship)) {
                throw new IllegalArgumentException("relationship must be one of " + RELATIONSHIPS);
            }
            String key = key(source, code);
            DictionaryTerminologyMapping mapping = byKey.get(key);
            boolean created = mapping == null;
            if (created) {
                mapping = new DictionaryTerminologyMapping();
                mapping.setSource(source);
                mapping.setCode(code);
                byKey.put(key, mapping);
            }
            mapping.setRelationship(relationship);
            if (!row.isBlank("displayName")) {
                mapping.setDisplayName(row.get("displayName"));
            }
            createdFlags.add(created);
        }
        mappingService.saveMappingsForDictionary(answer.getId(), new ArrayList<>(byKey.values()), SYS_USER_ID);
        for (Boolean created : createdFlags) {
            outcomes.add(created ? LoadedRow.created(answer.getId()) : LoadedRow.updated(answer.getId()));
        }
        return outcomes;
    }

    private static String key(String source, String code) {
        return source.toUpperCase(Locale.ROOT) + "|" + code.trim().toUpperCase(Locale.ROOT);
    }
}
