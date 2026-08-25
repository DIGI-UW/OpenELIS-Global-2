package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.analyzer.form.AnalyzerMigrationReferenceRequest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;

public class AnalyzerMigrationReferenceServiceTest {

    private static final String FINGERPRINT = "sha256:" + "a".repeat(64);

    @Test
    public void attachesTheExplicitProfileAndBridgeReferenceToTheExistingAnalyzer() {
        AnalyzerService analyzers = mock(AnalyzerService.class);
        AnalyzerProfileBindingService profiles = mock(AnalyzerProfileBindingService.class);
        Analyzer analyzer = analyzer("42");
        AnalyzerProfileBinding profile = profile(FINGERPRINT);
        when(analyzers.getWithType("42")).thenReturn(java.util.Optional.of(analyzer));
        when(profiles.assignProfile(analyzer, "fluorocycler-xt", 1, "17")).thenReturn(profile);
        AnalyzerMigrationReferenceService service = new AnalyzerMigrationReferenceService(analyzers, profiles);

        AnalyzerMigrationReferenceView reference = service.attach("42", request(FINGERPRINT), "17");

        assertEquals("42", reference.sourceAnalyzerId());
        assertEquals("bridge-42", reference.bridgeConnectionId());
        assertEquals("fluorocycler-xt", reference.profileRef().profileId());
        assertEquals(1, reference.profileRef().revision());
        assertEquals(FINGERPRINT, reference.profileRef().fingerprint());
        assertEquals("bridge-42", analyzer.getBridgeConnectionId());
        assertEquals("17", analyzer.getSysUserId());
        verify(analyzers).update(analyzer);
    }

    @Test
    public void rejectsAProfileFingerprintThatDoesNotMatchBridgeCatalogEvidence() {
        AnalyzerService analyzers = mock(AnalyzerService.class);
        AnalyzerProfileBindingService profiles = mock(AnalyzerProfileBindingService.class);
        Analyzer analyzer = analyzer("42");
        when(analyzers.getWithType("42")).thenReturn(java.util.Optional.of(analyzer));
        when(profiles.assignProfile(analyzer, "fluorocycler-xt", 1, "17"))
                .thenReturn(profile("sha256:" + "b".repeat(64)));
        AnalyzerMigrationReferenceService service = new AnalyzerMigrationReferenceService(analyzers, profiles);

        assertThrows(IllegalStateException.class, () -> service.attach("42", request(FINGERPRINT), "17"));

        verify(analyzers, never()).update(analyzer);
    }

    @Test
    public void rejectsReplacingAnExistingBridgeReference() {
        AnalyzerService analyzers = mock(AnalyzerService.class);
        AnalyzerProfileBindingService profiles = mock(AnalyzerProfileBindingService.class);
        Analyzer analyzer = analyzer("42");
        analyzer.setBridgeConnectionId("bridge-existing");
        when(analyzers.getWithType("42")).thenReturn(java.util.Optional.of(analyzer));
        AnalyzerMigrationReferenceService service = new AnalyzerMigrationReferenceService(analyzers, profiles);

        assertThrows(IllegalStateException.class, () -> service.attach("42", request(FINGERPRINT), "17"));

        verify(profiles, never()).assignProfile(analyzer, "fluorocycler-xt", 1, "17");
        verify(analyzers, never()).update(analyzer);
    }

    private static Analyzer analyzer(String id) {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setName("Released analyzer " + id);
        return analyzer;
    }

    private static AnalyzerProfileBinding profile(String fingerprint) {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setProfileId("fluorocycler-xt");
        profile.setProfileRevision(1);
        profile.setProfileFingerprint(fingerprint);
        return profile;
    }

    private static AnalyzerMigrationReferenceRequest request(String fingerprint) {
        AnalyzerMigrationReferenceRequest request = new AnalyzerMigrationReferenceRequest();
        request.setProfileId("fluorocycler-xt");
        request.setProfileRevision(1);
        request.setProfileFingerprint(fingerprint);
        request.setBridgeConnectionId("bridge-42");
        return request;
    }
}
