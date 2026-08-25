package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerInstanceLocalStateServiceTest {

    private static final String FINGERPRINT = "sha256:" + "1".repeat(64);

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private AnalyzerProfileBindingService profileBindingService;

    private AnalyzerInstanceLocalStateService service;
    private AnalyzerInstanceRequest request;

    @Before
    public void setUp() {
        request = new AnalyzerInstanceRequest();
        request.setName("  Synthetic bench 1  ");
        request.setProfileId("fixture.synthetic-connection");
        request.setProfileRevision(3);
        request.setTestUnitIds(List.of("7", " 8 "));
        service = new AnalyzerInstanceLocalStateServiceImpl(analyzerService, profileBindingService);
    }

    @Test
    public void persistsOnlyOpenElisOwnedIdentityProfileBindingAndLabUnits() {
        when(analyzerService.insert(any(Analyzer.class))).thenAnswer(invocation -> {
            Analyzer analyzer = invocation.getArgument(0);
            analyzer.setId("42");
            return "42";
        });
        when(profileBindingService.assignProfile(any(Analyzer.class),
                org.mockito.ArgumentMatchers.eq("fixture.synthetic-connection"),
                org.mockito.ArgumentMatchers.eq(3), org.mockito.ArgumentMatchers.eq("17")))
                        .thenAnswer(invocation -> bind(invocation.getArgument(0)));

        AnalyzerInstanceState result = service.create(request, "17");

        assertEquals("42", result.analyzerId());
        assertEquals("Synthetic bench 1", result.name());
        assertEquals(List.of("7", "8"), result.labUnitIds());
        assertEquals("fixture.synthetic-connection", result.profileId());
        assertEquals(3, result.profileRevision());
        assertEquals(FINGERPRINT, result.profileFingerprint());
        assertNull(result.bridgeConnectionId());
        assertEquals(Analyzer.AnalyzerStatus.SETUP, result.status());

        org.mockito.ArgumentCaptor<Analyzer> inserted = org.mockito.ArgumentCaptor.forClass(Analyzer.class);
        verify(analyzerService).insert(inserted.capture());
        Analyzer analyzer = inserted.getValue();
        assertFalse(analyzer.isActive());
        assertEquals(Analyzer.AnalyzerStatus.SETUP, analyzer.getStatus());
        assertEquals("17", analyzer.getSysUserId());
        assertNull(analyzer.getBridgeConnectionId());
    }

    @Test
    public void attachesTheBridgeReferenceWithoutCopyingTheConnectionDocument() {
        Analyzer analyzer = analyzer("42");
        bind(analyzer);
        when(analyzerService.getWithType("42")).thenReturn(Optional.of(analyzer));

        AnalyzerInstanceState result = service.attachBridgeConnection("42", "bridge-connection-42", "17");

        assertEquals("bridge-connection-42", result.bridgeConnectionId());
        assertEquals("bridge-connection-42", analyzer.getBridgeConnectionId());
        verify(analyzerService).update(analyzer);
    }

    @Test
    public void rejectsReplacingAnExistingBridgeReference() {
        Analyzer analyzer = analyzer("42");
        bind(analyzer);
        analyzer.setBridgeConnectionId("bridge-connection-original");
        when(analyzerService.getWithType("42")).thenReturn(Optional.of(analyzer));

        assertThrows(IllegalStateException.class,
                () -> service.attachBridgeConnection("42", "bridge-connection-different", "17"));

        verify(analyzerService, never()).update(any(Analyzer.class));
    }

    private static AnalyzerProfileBinding bind(Analyzer analyzer) {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setProfileId("fixture.synthetic-connection");
        profile.setProfileRevision(3);
        profile.setProfileFingerprint(FINGERPRINT);
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setProfileBinding(profile);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setSiteBinding(binding);
        analyzer.setSiteBindingRevision(revision);
        return profile;
    }

    private static Analyzer analyzer(String id) {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setName("Synthetic bench 1");
        analyzer.setTestUnitIds(List.of("7", "8"));
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        analyzer.setActive(false);
        return analyzer;
    }
}
