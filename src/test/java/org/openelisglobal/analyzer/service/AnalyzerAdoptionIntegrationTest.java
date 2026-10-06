package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.AnalyzerTestProfileCatalog;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Rule 7 through the real services: an analyzer on revision 1 adopts revision
 * 2, the operator confirms and applies it, and the analyzer keeps its identity
 * while OE2 and the Bridge both move to revision 2.
 */
public class AnalyzerAdoptionIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String CONNECTION_ID = "bridge-connection-adoption";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private AnalyzerDAO analyzerDAO;
    @Autowired
    private AnalyzerMappingService mappingService;
    @Autowired
    private AnalyzerAdoptionService adoptionService;
    @Autowired
    private AnalyzerMappingConfirmationService confirmations;
    @Autowired
    private AnalyzerInstanceLocalStateService localState;
    @Autowired
    private DataSource dataSource;

    private String analyzerId;

    @After
    public void deleteAnalyzer() {
        if (analyzerId == null) {
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long id = Long.valueOf(analyzerId);
        jdbc.update("UPDATE analyzer SET latest_activation_record_id = NULL, mapping_id = NULL WHERE id = ?", id);
        jdbc.update("DELETE FROM analyzer_activation_record WHERE analyzer_id = ?", id);
        String mappingIds = "(SELECT id FROM analyzer_mapping WHERE analyzer_id = ?)";
        jdbc.update("DELETE FROM analyzer_mapping_confirmation WHERE mapping_id IN " + mappingIds, id);
        jdbc.update("DELETE FROM analyzer_mapping_result WHERE mapping_id IN " + mappingIds, id);
        jdbc.update("DELETE FROM analyzer_mapping_test WHERE mapping_id IN " + mappingIds, id);
        jdbc.update("UPDATE analyzer_mapping SET supersedes_mapping_id = NULL WHERE analyzer_id = ?", id);
        jdbc.update("DELETE FROM analyzer_mapping WHERE analyzer_id = ?", id);
        jdbc.update("DELETE FROM analyzer WHERE id = ?", id);
    }

    @Test
    public void anAnalyzerAdoptsANewerRevisionKeepingItsIdentityAndMovesTheBridgeWithIt() {
        Analyzer analyzer = analyzerOnRevisionOne();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        int activationRecords = jdbc.queryForObject(
                "SELECT count(*) FROM analyzer_activation_record WHERE analyzer_id = ?", Integer.class,
                Long.valueOf(analyzerId));

        AnalyzerAdoptionService.AdoptionPlan plan = adoptionService.prepareAdoption(analyzerId, 2);
        AnalyzerMappingSnapshot adopted = adoptionService.adopt(analyzerId, 2, proposals(plan), "1");
        confirm(adopted);
        BridgeAnalyzerConnectionClient bridge = bridgeOn(1);
        new AnalyzerInstanceServiceImpl(localState, bridge, () -> "adoption-request").applyMapping(analyzerId,
                adopted.mapping().getId(), adopted.mapping().getRevisionNumber(),
                adopted.mapping().getMappingFingerprint(), "1");

        Analyzer after = analyzerDAO.get(analyzerId).orElseThrow();
        assertEquals(analyzer.getName(), after.getName());
        assertEquals(analyzer.getTestUnitIds(), after.getTestUnitIds());
        assertEquals(CONNECTION_ID, after.getBridgeConnectionId());
        assertEquals(Integer.valueOf(activationRecords),
                jdbc.queryForObject("SELECT count(*) FROM analyzer_activation_record WHERE analyzer_id = ?",
                        Integer.class, Long.valueOf(analyzerId)));
        assertEquals("the adopted revision is in force", adopted.mapping().getId(), jdbc.queryForObject(
                "SELECT mapping_id FROM analyzer WHERE id = ?", String.class, Long.valueOf(analyzerId)));
        assertEquals(Integer.valueOf(2),
                jdbc.queryForObject(
                        "SELECT m.profile_revision FROM analyzer a JOIN analyzer_mapping m ON a.mapping_id = m.id"
                                + " WHERE a.id = ?",
                        Integer.class, Long.valueOf(analyzerId)));
        ArgumentCaptor<ObjectNode> repin = ArgumentCaptor.forClass(ObjectNode.class);
        verify(bridge).updateConnection(eq(CONNECTION_ID), repin.capture());
        assertEquals(2, repin.getValue().path("profileRef").path("revision").asInt());
    }

    private Analyzer analyzerOnRevisionOne() {
        Analyzer analyzer = new Analyzer();
        analyzer.ensureFhirUuid();
        analyzer.setName("Adoption bench");
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        analyzer.setActive(false);
        analyzer.setTestUnitIds(List.of("1"));
        analyzer.setSysUserId("1");
        analyzerDAO.insert(analyzer);
        analyzerId = analyzer.getId();
        AnalyzerMappingSnapshot first = mappingService.assignProfile(analyzer,
                AnalyzerTestProfileCatalog.ADOPTABLE_PROFILE_ID, 1, "1");
        new JdbcTemplate(dataSource).update("UPDATE analyzer SET mapping_id = ?, bridge_connection_id = ? WHERE id = ?",
                Long.valueOf(first.mapping().getId()), CONNECTION_ID, Long.valueOf(analyzerId));
        return analyzer;
    }

    private static AnalyzerMappingDraft proposals(AnalyzerAdoptionService.AdoptionPlan plan) {
        List<AnalyzerMappingTestDraft> tests = new ArrayList<>();
        List<AnalyzerMappingResultDraft> results = new ArrayList<>();
        plan.rows().stream().filter(row -> row.proposed() != null).forEach(row -> {
            tests.add(row.proposed().test());
            results.addAll(row.proposed().results());
        });
        return new AnalyzerMappingDraft(tests, results);
    }

    private void confirm(AnalyzerMappingSnapshot adopted) {
        AnalyzerMappingSnapshot candidate = mappingService.findById(adopted.mapping().getId()).orElseThrow();
        List<AnalyzerMappingSourceRow> confirmed = candidate.tests().stream()
                .filter(row -> row.getMappingState() == AnalyzerMappingState.BOUND)
                .map(row -> new AnalyzerMappingSourceRow(row.getId().getSourceRowKey(), null,
                        row.getId().getSubIdentity()))
                .toList();
        confirmations
                .confirm(candidate, AnalyzerTestProfileCatalog.ADOPTABLE_RECOGNITION_FINGERPRINT,
                        new AnalyzerMappingConfirmationRequest(candidate.mapping().getMappingFingerprint(),
                                AnalyzerTestProfileCatalog.ADOPTABLE_RECOGNITION_FINGERPRINT, confirmed, List.of()),
                        "1");
    }

    private BridgeAnalyzerConnectionClient bridgeOn(int revision) {
        BridgeAnalyzerConnectionClient bridge = mock(BridgeAnalyzerConnectionClient.class);
        when(bridge.getConnection(CONNECTION_ID)).thenReturn(connection(revision));
        when(bridge.updateConnection(eq(CONNECTION_ID), any(ObjectNode.class))).thenReturn(connection(2));
        return bridge;
    }

    private ObjectNode connection(int revision) {
        ObjectNode connection = JSON.createObjectNode();
        connection.put("connectionId", CONNECTION_ID);
        connection.put("clientAnalyzerId", analyzerId);
        connection.putObject("profileRef").put("profileId", AnalyzerTestProfileCatalog.ADOPTABLE_PROFILE_ID)
                .put("revision", revision)
                .put("fingerprint", AnalyzerTestProfileCatalog.adoptableFingerprint(revision));
        connection.put("configRevision", 1);
        return connection;
    }
}
