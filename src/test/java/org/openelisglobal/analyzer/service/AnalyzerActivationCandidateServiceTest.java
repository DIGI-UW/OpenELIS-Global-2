package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.analyzer.dao.AnalyzerActivationCandidateDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationCandidate;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.audittrail.dao.AuditTrailService;

public class AnalyzerActivationCandidateServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private AnalyzerActivationCandidateDAO candidateDAO;

    @Mock
    private AuditTrailService auditTrailService;

    private AnalyzerActivationCandidateService service;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AnalyzerActivationCandidateServiceImpl(candidateDAO, auditTrailService);
    }

    @Test
    public void retainsExactDocumentsAsAnAuditedImmutableCandidate() throws Exception {
        Fixture fixture = fixture();
        when(candidateDAO.insert(any())).thenAnswer(invocation -> {
            AnalyzerActivationCandidate candidate = invocation.getArgument(0);
            candidate.setId("81");
            return "81";
        });

        AnalyzerActivationCandidate saved = service.retain(fixture.analyzer, fixture.revision, fixture.confirmation,
                fixture.documents, "17");

        ArgumentCaptor<AnalyzerActivationCandidate> captured = ArgumentCaptor
                .forClass(AnalyzerActivationCandidate.class);
        verify(candidateDAO).insert(captured.capture());
        AnalyzerActivationCandidate inserted = captured.getValue();
        assertEquals("81", saved.getId());
        assertSame(fixture.analyzer, inserted.getAnalyzer());
        assertSame(fixture.revision, inserted.getSiteBindingRevision());
        assertSame(fixture.confirmation, inserted.getVerificationConfirmation());
        assertEquals(FINGERPRINT, inserted.getDesiredStateFingerprint());
        assertEquals(fixture.documents.candidate(), JSON.readTree(inserted.getCandidateDocumentJson()));
        assertEquals(fixture.documents.registration(), JSON.readTree(inserted.getBridgeRegistrationJson()));
        assertEquals("17", inserted.getCreatedBy());
        verify(auditTrailService).saveNewHistory(inserted, "17", "analyzer_activation_candidate");
    }

    @Test
    public void returnsEveryRetainedCandidateWithoutReplacingOlderEvidence() {
        AnalyzerActivationCandidate older = new AnalyzerActivationCandidate();
        older.setId("80");
        AnalyzerActivationCandidate newer = new AnalyzerActivationCandidate();
        newer.setId("81");
        when(candidateDAO.findByAnalyzerId("42")).thenReturn(List.of(older, newer));

        assertEquals(List.of(older, newer), service.findByAnalyzerId("42"));
    }

    @Test
    public void rejectsDocumentsThatDoNotNameOneExactAnalyzerAndRegistration() throws Exception {
        Fixture fixture = fixture();
        ObjectNode wrongAnalyzer = fixture.documents.candidate();
        wrongAnalyzer.put("oeAnalyzerId", "43");

        assertThrows(IllegalArgumentException.class,
                () -> service.retain(fixture.analyzer, fixture.revision, fixture.confirmation,
                        new AnalyzerActivationDocuments(wrongAnalyzer, fixture.documents.registration()), "17"));

        ObjectNode wrongRegistration = fixture.documents.registration();
        wrongRegistration.put("desiredStateFingerprint",
                "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        assertThrows(IllegalArgumentException.class,
                () -> service.retain(fixture.analyzer, fixture.revision, fixture.confirmation,
                        new AnalyzerActivationDocuments(fixture.documents.candidate(), wrongRegistration), "17"));
        verify(candidateDAO, never()).insert(any());
        verify(auditTrailService, never()).saveNewHistory(any(), eq("17"), any());
    }

    private static Fixture fixture() {
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("61");
        AnalyzerSiteBindingConfirmation confirmation = new AnalyzerSiteBindingConfirmation();
        confirmation.setId("71");
        confirmation.setSiteBindingRevision(revision);
        confirmation.setAuditEventId("91");
        confirmation.setConfirmedAt(Timestamp.from(Instant.parse("2026-08-23T20:00:00Z")));

        Analyzer analyzer = new Analyzer();
        analyzer.setId("42");
        analyzer.setSiteBindingRevision(revision);

        ObjectNode candidate = JSON.createObjectNode();
        candidate.put("schemaVersion", "1.0");
        candidate.put("oeAnalyzerId", "42");
        candidate.put("desiredRegistrationFingerprint", FINGERPRINT);
        ObjectNode verification = candidate.putObject("verification");
        verification.put("auditEventId", "91");
        verification.put("siteBindingRevisionId", "61");

        ObjectNode registration = JSON.createObjectNode();
        registration.put("desiredStateFingerprint", FINGERPRINT);
        return new Fixture(analyzer, revision, confirmation, new AnalyzerActivationDocuments(candidate, registration));
    }

    private record Fixture(Analyzer analyzer, AnalyzerSiteBindingRevision revision,
            AnalyzerSiteBindingConfirmation confirmation, AnalyzerActivationDocuments documents) {
    }
}
