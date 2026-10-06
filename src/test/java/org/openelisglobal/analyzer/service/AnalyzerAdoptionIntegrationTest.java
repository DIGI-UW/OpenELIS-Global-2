package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.AnalyzerTestProfileCatalog;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Rule 7 through the real services: an analyzer on revision 1 adopts revision
 * 2, the operator confirms and applies it, and the analyzer keeps its identity
 * while OE2 and the Bridge both move to revision 2.
 */
public class AnalyzerAdoptionIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String CONNECTION_ID = "bridge-connection-adoption";
    private static final String CONFIG_FINGERPRINT = "sha256:" + "c".repeat(64);
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
    private DataSource dataSource;

    @Autowired
    private AnalyzerResultsService results;
    @Autowired
    private AnalyzerMappingCatalogService catalog;
    @Autowired
    private AnalyzerInstanceService instances;
    @Autowired
    private AnalyzerActivationService activations;

    private String analyzerId;
    private String deactivatedTestId;
    private BridgeAnalyzerConnectionClient realBridge;

    @After
    public void deleteAnalyzer() {
        if (realBridge != null) {
            useBridge(realBridge);
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        if (deactivatedTestId != null) {
            jdbc.update("UPDATE test SET is_active = 'Y' WHERE id = ?", Long.valueOf(deactivatedTestId));
        }
        if (analyzerId == null) {
            return;
        }
        Long id = Long.valueOf(analyzerId);
        jdbc.update("DELETE FROM analyzer_results WHERE analyzer_id = ?", id);
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
        apply(adopted);

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

    @Test
    public void aRemovedCodeWithHeldResultsBlocksAdoptionAndTheRefusalNamesIt() {
        analyzerOnRevisionOne();
        hold("ADOPT-C", 1);

        AnalyzerAdoptionService.AdoptionPlan plan = adoptionService.prepareAdoption(analyzerId, 2);
        AnalyzerMappingAdoption.Row blocked = row(plan, "ADOPT-C");
        assertEquals(AnalyzerMappingAdoption.Bucket.BLOCKED, blocked.bucket());
        assertEquals(AnalyzerMappingAdoption.BlockReason.HELD_RESULTS, blocked.blockReason());

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> adoptionService.adopt(analyzerId, 2, proposals(plan), "1"));
        assertTrue(refusal.getMessage(), refusal.getMessage().contains("ADOPT-C still has held results"));
        assertEquals("nothing was saved on revision 2", 1,
                mappingService.findLatestByAnalyzerId(analyzerId).orElseThrow().mapping().getProfileRevision());
    }

    @Test
    public void anOverrideOnATestNoLongerActiveBlocksUntilTheOperatorChangesIt() {
        Analyzer analyzer = analyzerOnRevisionOne();
        String retiredTest = overrideAdoptAOnAnotherTest(analyzer);
        deactivate(retiredTest);

        AnalyzerAdoptionService.AdoptionPlan plan = adoptionService.prepareAdoption(analyzerId, 2);
        AnalyzerMappingAdoption.Row blocked = row(plan, "ADOPT-A");
        assertEquals(AnalyzerMappingAdoption.Bucket.BLOCKED, blocked.bucket());
        assertEquals(AnalyzerMappingAdoption.BlockReason.INACTIVE_TEST, blocked.blockReason());

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> adoptionService.adopt(analyzerId, 2, proposals(plan), "1"));
        assertTrue(refusal.getMessage(),
                refusal.getMessage().contains("ADOPT-A is mapped to a test that is no longer active"));
        assertEquals(1, mappingService.findLatestByAnalyzerId(analyzerId).orElseThrow().mapping().getProfileRevision());
    }

    @Test
    public void anActiveAnalyzerReceivesOnItsRevisionUntilApplyWhichMovesAndReactivatesTheConnection() {
        analyzerOnRevisionOne();
        String revisionOne = inForceMappingId();
        markActive();
        BridgeAnalyzerConnectionClient bridge = bridgeOn(1);

        AnalyzerMappingSnapshot adopted = adoptionService.adopt(analyzerId, 2,
                proposals(adoptionService.prepareAdoption(analyzerId, 2)), "1");
        confirm(adopted);
        verifyZeroInteractions(bridge);
        assertEquals("adopting and confirming leave revision 1 in force", revisionOne, inForceMappingId());

        apply(adopted);

        assertEquals(adopted.mapping().getId(), inForceMappingId());
        InOrder bridgeOrder = inOrder(bridge);
        bridgeOrder.verify(bridge).updateConnection(eq(CONNECTION_ID), any(ObjectNode.class));
        bridgeOrder.verify(bridge).applyRuntimeCommand(eq(CONNECTION_ID), anyInt(), eq("ACTIVATE"), anyString());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertEquals("the connection is active on the adopted revision", adopted.mapping().getId(),
                jdbc.queryForObject(
                        "SELECT r.mapping_id FROM analyzer a JOIN analyzer_activation_record r"
                                + " ON a.latest_activation_record_id = r.id WHERE a.id = ?",
                        String.class, Long.valueOf(analyzerId)));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM analyzer WHERE id = ?", String.class,
                Long.valueOf(analyzerId)));
    }

    @Test
    public void aBridgeThatCannotMoveTheConnectionLeavesTheAnalyzerOnItsRevision() {
        analyzerOnRevisionOne();
        String revisionOne = inForceMappingId();
        markActive();
        BridgeAnalyzerConnectionClient bridge = bridgeOn(1);
        when(bridge.updateConnection(eq(CONNECTION_ID), any(ObjectNode.class)))
                .thenThrow(new BridgeAnalyzerConnectionException("analyzer.bridge.connection.unreachable"));
        AnalyzerMappingSnapshot adopted = adoptionService.adopt(analyzerId, 2,
                proposals(adoptionService.prepareAdoption(analyzerId, 2)), "1");
        confirm(adopted);

        assertThrows(BridgeAnalyzerConnectionException.class, () -> apply(adopted));

        assertEquals(revisionOne, inForceMappingId());
        verify(bridge, never()).applyRuntimeCommand(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    public void aReactivationTheBridgeRefusesPutsThePinBackAndLeavesTheAnalyzerOnItsRevision() {
        analyzerOnRevisionOne();
        String revisionOne = inForceMappingId();
        markActive();
        BridgeAnalyzerConnectionClient bridge = bridgeOn(1);
        when(bridge.applyRuntimeCommand(eq(CONNECTION_ID), anyInt(), eq("ACTIVATE"), anyString()))
                .thenThrow(new BridgeAnalyzerConnectionException("analyzer.bridge.connection.unreachable"));
        AnalyzerMappingSnapshot adopted = adoptionService.adopt(analyzerId, 2,
                proposals(adoptionService.prepareAdoption(analyzerId, 2)), "1");
        confirm(adopted);

        assertThrows(BridgeAnalyzerConnectionException.class, () -> apply(adopted));

        assertEquals(revisionOne, inForceMappingId());
        ArgumentCaptor<ObjectNode> pins = ArgumentCaptor.forClass(ObjectNode.class);
        verify(bridge, times(2)).updateConnection(eq(CONNECTION_ID), pins.capture());
        assertEquals("the connection is pinned back to revision 1", 1,
                pins.getAllValues().get(1).path("profileRef").path("revision").asInt());
    }

    private String overrideAdoptAOnAnotherTest(Analyzer analyzer) {
        AnalyzerMappingSnapshot first = mappingService.findLatestByAnalyzerId(analyzerId).orElseThrow();
        AnalyzerMappingDraft draft = AnalyzerMappingDraft.of(first);
        String inForce = draft.tests().stream().filter(test -> test.sourceRowKey().equals("ADOPT-A"))
                .map(AnalyzerMappingTestDraft::testId).filter(Objects::nonNull).findFirst().orElse(null);
        String other = catalog.searchActiveTests(null).stream().map(AnalyzerMappingCatalogService.TestOption::id)
                .filter(id -> !id.equals(inForce)).findFirst().orElseThrow();
        List<AnalyzerMappingTestDraft> tests = draft.tests().stream()
                .map(test -> test.sourceRowKey().equals("ADOPT-A")
                        ? new AnalyzerMappingTestDraft(test.sourceRowKey(), AnalyzerMappingState.BOUND, other, null,
                                null, AnalyzerMappingOrigin.OVERRIDE, test.subIdentity(), null)
                        : test)
                .toList();
        mappingService.appendRevision(analyzer,
                new AnalyzerMappingDraft(tests,
                        draft.results().stream().filter(result -> !result.sourceRowKey().equals("ADOPT-A")).toList()),
                "1");
        return other;
    }

    private void deactivate(String testId) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("UPDATE test SET is_active = 'N' WHERE id = ?", Long.valueOf(testId));
        deactivatedTestId = testId;
    }

    private void hold(String code, int revision) {
        AnalyzerResults held = new AnalyzerResults();
        held.setAnalyzerId(analyzerId);
        held.setAccessionNumber("ADOPT-HELD-1");
        held.setTestName(code);
        held.setResult("7.1");
        held.setIsControl(false);
        held.setRawTestCode(code);
        held.setSourceProfileId(AnalyzerTestProfileCatalog.ADOPTABLE_PROFILE_ID);
        held.setSourceProfileRevision(revision);
        held.setSourceConnectionId(CONNECTION_ID);
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY);
        held.setReadOnly(true);
        results.insertAnalyzerResults(List.of(held), "1");
    }

    private static AnalyzerMappingAdoption.Row row(AnalyzerAdoptionService.AdoptionPlan plan, String code) {
        return plan.rows().stream().filter(row -> row.key().equals(AnalyzerMappingRowKey.main(code))).findFirst()
                .orElseThrow(() -> new AssertionError("no adoption row for " + code));
    }

    private Analyzer analyzerOnRevisionOne() {
        Analyzer analyzer = new Analyzer();
        analyzer.ensureFhirUuid();
        analyzer.setName("Adoption bench");
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        analyzer.setActive(false);
        analyzer.setTestUnitIds(List.of(new JdbcTemplate(dataSource)
                .queryForObject("SELECT min(id)::text FROM test_section WHERE is_active = 'Y'", String.class)));
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

    private void apply(AnalyzerMappingSnapshot adopted) {
        instances.applyMapping(analyzerId, adopted.mapping().getId(), adopted.mapping().getRevisionNumber(),
                adopted.mapping().getMappingFingerprint(), "1");
    }

    private String inForceMappingId() {
        return new JdbcTemplate(dataSource).queryForObject("SELECT mapping_id FROM analyzer WHERE id = ?", String.class,
                Long.valueOf(analyzerId));
    }

    private void markActive() {
        new JdbcTemplate(dataSource).update("UPDATE analyzer SET status = 'ACTIVE', is_active = true WHERE id = ?",
                Long.valueOf(analyzerId));
    }

    /**
     * Swaps the Bridge client inside the Spring beans so Apply runs through their
     * real transaction; the original is put back after each test.
     */
    private BridgeAnalyzerConnectionClient bridgeOn(int revision) {
        BridgeAnalyzerConnectionClient bridge = mock(BridgeAnalyzerConnectionClient.class);
        when(bridge.getConnection(CONNECTION_ID)).thenReturn(connection(revision), connection(2));
        when(bridge.updateConnection(eq(CONNECTION_ID), any(ObjectNode.class))).thenReturn(connection(2));
        when(bridge.applyRuntimeCommand(eq(CONNECTION_ID), anyInt(), eq("ACTIVATE"), anyString()))
                .thenAnswer(call -> acknowledgement(call.getArgument(3)));
        if (realBridge == null) {
            realBridge = (BridgeAnalyzerConnectionClient) ReflectionTestUtils
                    .getField(AopTestUtils.<Object>getUltimateTargetObject(instances), "bridgeClient");
        }
        useBridge(bridge);
        return bridge;
    }

    private void useBridge(BridgeAnalyzerConnectionClient bridge) {
        ReflectionTestUtils.setField(AopTestUtils.<Object>getUltimateTargetObject(instances), "bridgeClient", bridge);
        ReflectionTestUtils.setField(AopTestUtils.<Object>getUltimateTargetObject(activations), "bridgeClient", bridge);
    }

    private ObjectNode acknowledgement(String commandId) {
        ObjectNode acknowledgement = JSON.createObjectNode();
        acknowledgement.put("schemaVersion", "1.0");
        acknowledgement.put("commandId", commandId);
        acknowledgement.put("action", "ACTIVATE");
        acknowledgement.put("outcome", "APPLIED");
        acknowledgement.put("connectionId", CONNECTION_ID);
        acknowledgement.set("profileRef", connection(2).path("profileRef"));
        acknowledgement.put("configRevision", 1);
        acknowledgement.put("configFingerprint", CONFIG_FINGERPRINT);
        acknowledgement.put("runtimeRevision", 2);
        acknowledgement.put("runtimeFingerprint", "sha256:" + "d".repeat(64));
        acknowledgement.put("desiredRuntimeState", "ACTIVE");
        acknowledgement.put("actualRuntimeState", "ACTIVE");
        acknowledgement.putArray("blockers");
        acknowledgement.put("acknowledgedAt", "2026-10-06T12:00:00Z");
        return acknowledgement;
    }

    private ObjectNode connection(int revision) {
        ObjectNode connection = JSON.createObjectNode();
        connection.put("connectionId", CONNECTION_ID);
        connection.put("clientAnalyzerId", analyzerId);
        connection.putObject("profileRef").put("profileId", AnalyzerTestProfileCatalog.ADOPTABLE_PROFILE_ID)
                .put("revision", revision)
                .put("fingerprint", AnalyzerTestProfileCatalog.adoptableFingerprint(revision));
        connection.put("configRevision", 1);
        connection.put("configFingerprint", CONFIG_FINGERPRINT);
        connection.putObject("readiness").put("ready", true).putArray("blockers");
        return connection;
    }
}
