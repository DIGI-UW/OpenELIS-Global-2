package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerActivationRecordDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.audittrail.dao.AuditTrailService;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerActivationRecordServiceTest {

    private static final String PROFILE_ID = "fixture.synthetic-socket";
    private static final String PROFILE_FINGERPRINT = "sha256:" + "1".repeat(64);
    private static final String CONNECTION_ID = "bridge-connection-7f3c";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private AnalyzerActivationRecordDAO recordDAO;

    @Mock
    private AuditTrailService auditTrailService;

    private AnalyzerActivationRecordService service;

    @Before
    public void setUp() {
        service = new AnalyzerActivationRecordServiceImpl(recordDAO, auditTrailService);
    }

    @Test
    public void retainsOnlyOpenElisReferencesAndTheExactBridgeAcknowledgement() throws Exception {
        Fixture fixture = fixture();
        ObjectNode acknowledgement = bridgeFixture("connection-activate-ack.json");
        when(recordDAO.insert(any())).thenAnswer(invocation -> {
            AnalyzerActivationRecord record = invocation.getArgument(0);
            record.setId("81");
            return "81";
        });

        AnalyzerActivationRecord saved = service.retain(fixture.analyzer, fixture.revision, fixture.confirmation,
                acknowledgement, "ACTIVE", "17");

        ArgumentCaptor<AnalyzerActivationRecord> captured = ArgumentCaptor.forClass(AnalyzerActivationRecord.class);
        verify(recordDAO).insert(captured.capture());
        AnalyzerActivationRecord inserted = captured.getValue();
        assertEquals("81", saved.getId());
        assertSame(fixture.analyzer, inserted.getAnalyzer());
        assertSame(fixture.revision, inserted.getSiteBindingRevision());
        assertSame(fixture.confirmation, inserted.getVerificationConfirmation());
        assertEquals(CONNECTION_ID, inserted.getBridgeConnectionId());
        assertEquals("ACTIVE", inserted.getActivationIntent());
        assertEquals(acknowledgement, JSON.readTree(inserted.getRuntimeAcknowledgementJson()));
        assertEquals(acknowledgement.path("runtimeFingerprint").asText(), inserted.getRuntimeFingerprint());
        assertEquals("17", inserted.getCreatedBy());
        verify(auditTrailService).saveNewHistory(inserted, "17", "analyzer_activation_record");
    }

    @Test
    public void rejectsAcknowledgementForAnotherConnectionOrProfileRevision() throws Exception {
        Fixture fixture = fixture();
        ObjectNode wrongConnection = bridgeFixture("connection-activate-ack.json");
        wrongConnection.put("connectionId", "another-connection");

        assertThrows(IllegalArgumentException.class, () -> service.retain(fixture.analyzer, fixture.revision,
                fixture.confirmation, wrongConnection, "ACTIVE", "17"));

        ObjectNode wrongProfile = bridgeFixture("connection-activate-ack.json");
        wrongProfile.with("profileRef").put("revision", 3);
        assertThrows(IllegalArgumentException.class, () -> service.retain(fixture.analyzer, fixture.revision,
                fixture.confirmation, wrongProfile, "ACTIVE", "17"));
        verify(recordDAO, never()).insert(any());
    }

    @Test
    public void returnsEveryRecordInAuditOrder() {
        AnalyzerActivationRecord first = new AnalyzerActivationRecord();
        AnalyzerActivationRecord second = new AnalyzerActivationRecord();
        when(recordDAO.findByAnalyzerId("42")).thenReturn(List.of(first, second));

        assertEquals(List.of(first, second), service.findByAnalyzerId("42"));
    }

    private static Fixture fixture() {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setProfileId(PROFILE_ID);
        profile.setProfileRevision(2);
        profile.setProfileFingerprint(PROFILE_FINGERPRINT);
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setProfileBinding(profile);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("61");
        revision.setSiteBinding(binding);
        AnalyzerSiteBindingConfirmation confirmation = new AnalyzerSiteBindingConfirmation();
        confirmation.setId("71");
        confirmation.setSiteBindingRevision(revision);
        Analyzer analyzer = new Analyzer();
        analyzer.setId("42");
        analyzer.setBridgeConnectionId(CONNECTION_ID);
        analyzer.setSiteBindingRevision(revision);
        return new Fixture(analyzer, revision, confirmation);
    }

    private static ObjectNode bridgeFixture(String name) throws Exception {
        String json = Files.readString(
                Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1", "fixtures", name));
        return (ObjectNode) JSON.readTree(json);
    }

    private record Fixture(Analyzer analyzer, AnalyzerSiteBindingRevision revision,
            AnalyzerSiteBindingConfirmation confirmation) {
    }
}
