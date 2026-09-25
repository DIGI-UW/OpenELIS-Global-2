package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.StringType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportService;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.configuration.service.ConfigurationImportRunService;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Populated upgrade using real persistence/services, with only the remote
 * Bridge boundary replaced.
 */
public class AnalyzerUpgradeIntegrationTest extends BaseWebContextSensitiveTest {
    private static final String FINGERPRINT = "sha256:" + "a".repeat(64);
    private static final String RECOGNITION = "sha256:" + "2".repeat(64);
    private static final long TEST_ID = 98605;
    private static final String RAW = "VENDOR-NEW-42";
    @Autowired
    private DataSource dataSource;
    @Autowired
    private AnalyzerService analyzers;
    @Autowired
    private AnalyzerUpgradePreparationService preparation;
    @Autowired
    private AnalyzerProfileBindingService profiles;
    @Autowired
    private AnalyzerSiteBindingService bindings;
    @Autowired
    private AnalyzerSiteBindingConfirmationService confirmations;
    @Autowired
    private AnalyzerInstanceLocalStateService localState;
    @Autowired
    private AnalyzerNormalizedResultImportService imports;
    @Autowired
    private AnalyzerResultsService results;
    @Autowired
    private ConfigurationImportRunService runs;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private HistoryService history;
    @Autowired
    private ReferenceTablesService referenceTables;
    @Autowired
    private PlatformTransactionManager transactionManager;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, ObjectNode> connections = new LinkedHashMap<>();
    private final List<ObjectNode> requests = new ArrayList<>();
    private final List<String> ids = new ArrayList<>();
    private final List<Runnable> restore = new ArrayList<>();
    private JdbcTemplate jdbc;
    private BridgeProfileCatalogService catalog;
    private BridgeAnalyzerConnectionClient bridge;

    @Before
    public void arrangeUpgrade() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        // The populated fixture and prior tests use explicit IDs; keep generated keys
        // ahead of them.
        for (String table : List.of("analyzer_profile_binding", "analyzer_site_binding",
                "analyzer_site_binding_revision")) {
            jdbc.execute("SELECT setval('" + table + "_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM " + table
                    + "), 1)::bigint)");
        }
        jdbc.update(
                "INSERT INTO test (id, guid, name, description, is_active, is_reportable, orderable, lastupdated)"
                        + " VALUES (?, ?, 'Upgrade saved test', 'Upgrade saved test', 'Y', 'Y', true, NOW())",
                TEST_ID, UUID.randomUUID().toString());
        catalog = mock(BridgeProfileCatalogService.class);
        replace(preparation, "catalog", catalog);
        replace(profiles, "catalogService", catalog);
        var audit = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(audit, "historyService", history);
        ReflectionTestUtils.setField(audit, "referenceTablesService", referenceTables);
        replace(confirmations, "auditTrailService", audit);
        when(fhirContext.newJsonParser()).thenAnswer(call -> FhirContext.forR4Cached().newJsonParser());
        bridge = mock(BridgeAnalyzerConnectionClient.class);
        when(bridge.createConnection(any())).thenAnswer(call -> {
            ObjectNode request = call.getArgument(0);
            requests.add(request.deepCopy());
            String analyzerId = request.path("clientAnalyzerId").asText();
            // Bridge's existing API reconciles equivalent creation by clientAnalyzerId.
            return connections.computeIfAbsent(analyzerId,
                    key -> request.deepCopy().put("connectionId", "upgrade-" + key));
        });
    }

    @After
    public void restoreServices() {
        restore.forEach(Runnable::run);
        cleanup();
    }

    @Test
    public void populatedUpgradePreservesIdsSettingsAndMappingsAndRepeatDoesNothing() throws Exception {
        List<BridgeProfileCatalog.ProfileRevision> revisions = new ArrayList<>();
        for (String protocol : List.of("ASTM", "HL7", "FILE")) {
            String id = addAnalyzer(protocol);
            ObjectNode profile = profile(protocol);
            var revision = new BridgeProfileCatalog.ProfileRevision(profile, json.createObjectNode());
            revisions.add(revision);
            when(catalog.getProfile("upgrade-" + protocol, 1)).thenReturn(revision);
        }
        when(catalog.getCatalog()).thenReturn(new BridgeProfileCatalog("1.0", FINGERPRINT, revisions));
        jdbc.update("INSERT INTO analyzer (id, name, is_active, status, last_updated)"
                + " VALUES (98613, 'New setup draft', false, 'SETUP', NOW())");
        assertFalse(preparation.pendingIds().contains("98613"));
        AnalyzerUpgradeService migration = migration(localState);

        var outcomes = migration.migrate(Map.of(), "1");
        assertEquals(3, outcomes.size());
        for (var outcome : outcomes)
            assertEquals(outcome.reason(), "MIGRATED", outcome.status());
        assertEquals(3, connections.size());
        assertTrue(migration.pending().isEmpty());
        List<String> bindingIds = new ArrayList<>();
        for (String id : ids) {
            Analyzer analyzer = analyzers.getWithBinding(id).orElseThrow();
            assertEquals("upgrade-" + id, analyzer.getBridgeConnectionId());
            assertEquals(Analyzer.AnalyzerStatus.SETUP, analyzer.getStatus());
            assertFalse(analyzer.isActive());
            assertEquals(List.of("1"), analyzer.getTestUnitIds());
            assertNotNull(analyzer.getFhirUuid());
            var binding = bindings.findByRevisionId(analyzer.getSiteBindingRevision().getId()).orElseThrow();
            bindingIds.add(binding.revision().getId());
            assertEquals(String.valueOf(TEST_ID), binding.tests().stream()
                    .filter(row -> RAW.equals(row.getId().getSourceRowKey())).findFirst().orElseThrow().getTestId());
            assertNull(jdbc.queryForObject(
                    "SELECT max(id) FROM analyzer_site_binding_confirmation WHERE site_binding_revision_id = ?",
                    Long.class, Long.valueOf(binding.revision().getId())));
            assertEquals(Long.valueOf(TEST_ID), jdbc.queryForObject(
                    "SELECT test_id FROM analyzer_test_map WHERE analyzer_id = ?", Long.class, Long.valueOf(id)));
            assertEquals("10.20.30.40", jdbc.queryForObject("SELECT ip_address FROM analyzer WHERE id = ?",
                    String.class, Long.valueOf(id)));
            ObjectNode values = (ObjectNode) connections.get(id).path("values");
            assertEquals(12000, values.path("timeout").asInt());
            if ("FILE".equals(analyzer.getName())) {
                assertEquals("/data/upgrade-files", values.path("directory").asText());
                assertEquals("*.csv", values.path("filePattern").asText());
            } else {
                assertEquals("10.20.30.40", values.path("host").asText());
                assertFalse(values.has("port"));
            }
        }
        assertTrue(migration.migrate(Map.of(), "1").isEmpty());
        assertEquals(3, requests.size());
        assertEquals(bindingIds, ids.stream()
                .map(id -> analyzers.getWithBinding(id).orElseThrow().getSiteBindingRevision().getId()).toList());
        deliverToOriginalAnalyzer(ids.get(1));
    }

    @Test
    public void interruptedPointerSaveResumesUsingTheOriginalAnalyzerAndConnection() throws Exception {
        String id = addAnalyzer("ASTM");
        var revision = new BridgeProfileCatalog.ProfileRevision(profile("ASTM"), json.createObjectNode());
        when(catalog.getCatalog()).thenReturn(new BridgeProfileCatalog("1.0", FINGERPRINT, List.of(revision)));
        when(catalog.getProfile("upgrade-ASTM", 1)).thenReturn(revision);
        AnalyzerInstanceLocalStateService interrupted = mock(AnalyzerInstanceLocalStateService.class);
        when(interrupted.get(id)).thenAnswer(call -> localState.get(id));
        AtomicBoolean failFirstSave = new AtomicBoolean(true);
        when(interrupted.attachBridgeConnection(any(), any(), any())).thenAnswer(call -> {
            if (failFirstSave.getAndSet(false))
                throw new IllegalStateException("Interrupted before saving reference");
            return localState.attachBridgeConnection(call.getArgument(0), call.getArgument(1), call.getArgument(2));
        });
        AnalyzerUpgradeService migration = migration(interrupted);
        var first = migration.migrate(Map.of(), "1").get(0);
        assertEquals("PENDING", first.status());
        assertEquals("Interrupted before saving reference", migration.pending().get(0).reason());
        assertNull(analyzers.getWithBinding(id).orElseThrow().getBridgeConnectionId());
        assertEquals(1, connections.size());
        var second = migration.migrate(Map.of(), "1").get(0);
        assertEquals(second.reason(), "MIGRATED", second.status());
        assertEquals(1, connections.size());
        assertEquals("upgrade-" + id, analyzers.getWithBinding(id).orElseThrow().getBridgeConnectionId());
        assertEquals(requests.get(0).path("profileRef"), requests.get(1).path("profileRef"));
        assertEquals(requests.get(0).path("values"), requests.get(1).path("values"));
        assertTrue(migration.migrate(Map.of(), "1").isEmpty());
    }

    private AnalyzerUpgradeService migration(AnalyzerInstanceLocalStateService state) {
        AnalyzerService selected = mock(AnalyzerService.class);
        when(selected.getAllWithBindings())
                .thenAnswer(call -> ids.stream().map(id -> analyzers.getWithBinding(id).orElseThrow()).toList());
        return new AnalyzerUpgradeService(selected, preparation, new AnalyzerInstanceServiceImpl(state, bridge), runs);
    }

    private String addAnalyzer(String protocol) {
        long id = 98610 + ids.size();
        long typeId = id + 10;
        jdbc.update(
                "INSERT INTO analyzer_type (id, name, protocol, is_active, last_updated) VALUES (?, ?, ?, true, NOW())",
                typeId, protocol, protocol);
        jdbc.update(
                "INSERT INTO analyzer (id, name, is_active, status, fhir_uuid, analyzer_type_id, ip_address, port, communication_mode, import_directory, file_pattern, file_format, last_updated)"
                        + " VALUES (?, ?, true, 'ACTIVE', ?, ?, '10.20.30.40', 5000, 'ANALYZER_INITIATED', ?, ?, ?, NOW())",
                id, protocol, UUID.randomUUID(), typeId, "FILE".equals(protocol) ? "/data/upgrade-files" : null,
                "FILE".equals(protocol) ? "*.csv" : null, "FILE".equals(protocol) ? "CSV" : null);
        jdbc.update("UPDATE analyzer SET test_unit_ids = '1' WHERE id = ?", id);
        jdbc.update(
                "INSERT INTO analyzer_test_map (analyzer_id, analyzer_test_name, test_id, last_updated) VALUES (?, ?, ?, NOW())",
                id, RAW, TEST_ID);
        jdbc.update(
                "INSERT INTO analyzer_plugin_config (analyzer_id, config, last_updated) VALUES (?, '{\"timeout\":12000}'::jsonb, NOW())",
                id);
        ids.add(String.valueOf(id));
        return String.valueOf(id);
    }

    private ObjectNode profile(String protocol) throws Exception {
        ObjectNode profile = (ObjectNode) json.readTree(
                """
                        {"profileMeta":{"id":"placeholder","displayName":"placeholder"},
                         "protocol":{"name":"ASTM"}, "communication":{"mode":"ANALYZER_INITIATED"},
                         "catalog":{"revision":1,"source":"SITE","status":"ACTIVE"},
                         "configDefaults":{"connectionRole":"SERVER","fileFormat":"CSV"},
                         "connectionFields":[{"key":"host"},{"key":"port"},{"key":"directory"},{"key":"filePattern"},{"key":"timeout"}],
                         "default_test_mappings":[{"test_code":"VENDOR-NEW-42","loinc":"upgrade-unmatched","result_type":"quantitative"}]}
                        """);
        ((ObjectNode) profile.path("profileMeta")).put("id", "upgrade-" + protocol).put("displayName", protocol);
        ((ObjectNode) profile.path("protocol")).put("name", protocol);
        ((ObjectNode) profile.path("catalog")).put("revisionFingerprint", FINGERPRINT);
        return profile;
    }

    private void deliverToOriginalAnalyzer(String id) throws Exception {
        String payload = Files.readString(Path
                .of("tools/openelis-analyzer-bridge/contracts/analyzer/v1/fixtures/normalized-unknown-test.fhir.json"));
        Bundle bundle = FhirContext.forR4Cached().newJsonParser().parseResource(Bundle.class, payload);
        Device device = (Device) bundle.getEntryFirstRep().getResource();
        device.getIdentifier().forEach(identifier -> identifier
                .setValue(identifier.getSystem().endsWith("analyzer-connection-id") ? "upgrade-" + id : id));
        device.getExtensionByUrl("https://openelis-global.org/fhir/StructureDefinition/analyzer-profile-id")
                .setValue(new StringType("upgrade-HL7"));
        device.getExtensionByUrl("https://openelis-global.org/fhir/StructureDefinition/analyzer-profile-revision")
                .setValue(new org.hl7.fhir.r4.model.IntegerType(1));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var analyzer = analyzers.getWithBinding(id).orElseThrow();
            var binding = bindings.findByRevisionId(analyzer.getSiteBindingRevision().getId()).orElseThrow();
            confirmations.confirm(binding, RECOGNITION,
                    new AnalyzerSiteBindingConfirmationRequest(binding.revision().getBindingFingerprint(), RECOGNITION,
                            List.of(new AnalyzerSiteBindingSourceRow(RAW, null)), List.of()),
                    "1");
        });
        var receipt = imports.importBundle(bundle, "1");
        assertEquals(id, receipt.analyzerId());
        assertEquals(1, receipt.resultsStaged());
        assertEquals(0, receipt.resultsHeld());
        assertEquals(String.valueOf(TEST_ID), results.getResultsbyAnalyzer(id).get(0).getTestId());
    }

    private void replace(Object bean, String field, Object value) {
        Object target = AopTestUtils.getUltimateTargetObject(bean);
        Object previous = ReflectionTestUtils.getField(target, field);
        restore.add(() -> ReflectionTestUtils.setField(target, field, previous));
        ReflectionTestUtils.setField(target, field, value);
    }

    private void cleanup() {
        if (jdbc == null)
            return;
        jdbc.update("DELETE FROM analyzer_results WHERE analyzer_id BETWEEN 98610 AND 98612");
        jdbc.update("DELETE FROM analyzer_delivery_receipt WHERE analyzer_id BETWEEN 98610 AND 98612");
        jdbc.update("DELETE FROM analyzer_plugin_config WHERE analyzer_id BETWEEN 98610 AND 98612");
        jdbc.update("DELETE FROM analyzer_test_map WHERE analyzer_id BETWEEN 98610 AND 98612");
        jdbc.update("DELETE FROM analyzer WHERE id BETWEEN 98610 AND 98613");
        String revisions = "SELECT r.id FROM analyzer_site_binding_revision r JOIN analyzer_site_binding b ON b.id=r.site_binding_id JOIN analyzer_profile_binding p ON p.id=b.profile_binding_id WHERE p.profile_id LIKE 'upgrade-%'";
        for (String table : List.of("analyzer_site_binding_confirmation", "analyzer_site_binding_result",
                "analyzer_site_binding_test")) {
            jdbc.update("DELETE FROM " + table + " WHERE site_binding_revision_id IN (" + revisions + ")");
        }
        jdbc.update("DELETE FROM analyzer_site_binding_revision WHERE id IN (" + revisions + ")");
        jdbc.update(
                "DELETE FROM analyzer_site_binding WHERE profile_binding_id IN (SELECT id FROM analyzer_profile_binding WHERE profile_id LIKE 'upgrade-%')");
        jdbc.update("DELETE FROM analyzer_profile_binding WHERE profile_id LIKE 'upgrade-%'");
        jdbc.update("DELETE FROM analyzer_type WHERE id BETWEEN 98620 AND 98622");
        jdbc.update("DELETE FROM test WHERE id = ?", TEST_ID);
    }
}
