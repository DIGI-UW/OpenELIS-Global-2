package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerMigrationSourceSnapshotServiceImpl implements AnalyzerMigrationSourceSnapshotService {

    private final AnalyzerService analyzerService;
    private final AnalyzerPluginConfigService pluginConfigService;
    private final AnalyzerTestMappingService testMappingService;
    private final AnalyzerFieldService fieldService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AnalyzerMigrationSourceSnapshotServiceImpl(AnalyzerService analyzerService,
            AnalyzerPluginConfigService pluginConfigService, AnalyzerTestMappingService testMappingService,
            AnalyzerFieldService fieldService) {
        this.analyzerService = analyzerService;
        this.pluginConfigService = pluginConfigService;
        this.testMappingService = testMappingService;
        this.fieldService = fieldService;
        this.objectMapper = new ObjectMapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerMigrationSourceSnapshot snapshot() {
        List<AnalyzerMigrationSourceSnapshot.AnalyzerSource> sources = analyzerService.getAllWithTypes().stream()
                .sorted(Comparator.comparing(Analyzer::getId)).map(this::source).toList();
        return new AnalyzerMigrationSourceSnapshot(fingerprint(sources), sources);
    }

    private AnalyzerMigrationSourceSnapshot.AnalyzerSource source(Analyzer analyzer) {
        Map<String, Object> state = new TreeMap<>();
        state.put("id", analyzer.getId());
        state.put("name", analyzer.getName());
        state.put("scriptId", analyzer.getScript_id());
        state.put("machineId", analyzer.getMachineId());
        state.put("type", analyzer.getType());
        state.put("description", analyzer.getDescription());
        state.put("location", analyzer.getLocation());
        state.put("active", analyzer.isActive());
        state.put("hasSetupPage", analyzer.getHasSetupPage());
        state.put("identifierPattern", analyzer.getIdentifierPattern());
        state.put("status", analyzer.getStatus());
        state.put("lastActivatedDate", analyzer.getLastActivatedDate());
        state.put("labUnitIds",
                analyzer.getTestUnitIds() == null ? List.of() : analyzer.getTestUnitIds().stream().sorted().toList());
        state.put("analyzerType", analyzerType(analyzer.getAnalyzerType()));
        state.put("pluginConfig", new TreeMap<>(pluginConfigService.getConfigAsMap(analyzer.getId())));
        state.put("testMappings", testMappings(analyzer.getId()));
        state.put("fields", analyzerFields(analyzer.getId()));
        return new AnalyzerMigrationSourceSnapshot.AnalyzerSource(analyzer.getId(), fingerprint(state));
    }

    private Map<String, Object> analyzerType(AnalyzerType type) {
        if (type == null) {
            return Map.of();
        }
        Map<String, Object> state = new TreeMap<>();
        state.put("id", type.getId());
        state.put("name", type.getName());
        state.put("description", type.getDescription());
        state.put("protocol", type.getProtocol());
        state.put("pluginClassName", type.getPluginClassName());
        state.put("identifierPattern", type.getIdentifierPattern());
        state.put("genericPlugin", type.isGenericPlugin());
        state.put("active", type.isActive());
        return state;
    }

    private List<Map<String, Object>> testMappings(String analyzerId) {
        List<AnalyzerTestMapping> mappings = new ArrayList<>(testMappingService.getAllForAnalyzer(analyzerId));
        mappings.sort(Comparator.comparing(AnalyzerTestMapping::getAnalyzerTestName,
                Comparator.nullsFirst(String::compareTo)));
        return mappings.stream().map(mapping -> {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("analyzerTestName", mapping.getAnalyzerTestName());
            state.put("testId", mapping.getTestId());
            state.put("componentId", mapping.getComponentId());
            return state;
        }).toList();
    }

    private List<Map<String, Object>> analyzerFields(String analyzerId) {
        List<AnalyzerField> fields = new ArrayList<>(fieldService.getFieldsByAnalyzerId(analyzerId));
        fields.sort(Comparator.comparing(AnalyzerField::getId, Comparator.nullsFirst(String::compareTo)));
        return fields.stream().map(field -> {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("id", field.getId());
            state.put("fieldName", field.getFieldName());
            state.put("astmRef", field.getAstmRef());
            state.put("fieldType", field.getFieldType());
            state.put("unit", field.getUnit());
            state.put("customFieldTypeId", field.getCustomFieldTypeId());
            state.put("active", field.getIsActive());
            return state;
        }).toList();
    }

    private String fingerprint(Object value) {
        try {
            byte[] canonical = objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to fingerprint analyzer migration source data", exception);
        }
    }
}
