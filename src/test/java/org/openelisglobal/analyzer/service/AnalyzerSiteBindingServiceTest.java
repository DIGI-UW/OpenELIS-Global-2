package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingResultDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingTestDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.audittrail.dao.AuditTrailService;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerSiteBindingServiceTest {

    private static final String PROFILE_ID = "site.mock-hematology";
    private static final int PROFILE_REVISION = 3;
    private static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private AnalyzerSiteBindingDAO bindingDAO;
    @Mock
    private AnalyzerSiteBindingRevisionDAO revisionDAO;
    @Mock
    private AnalyzerSiteBindingTestDAO testDAO;
    @Mock
    private AnalyzerSiteBindingResultDAO resultDAO;
    @Mock
    private AuditTrailService auditTrailService;

    private AnalyzerSiteBindingService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        service = new AnalyzerSiteBindingServiceImpl(bindingDAO, revisionDAO, testDAO, resultDAO, auditTrailService);
        when(bindingDAO.insert(any(AnalyzerSiteBinding.class))).thenAnswer(invocation -> {
            AnalyzerSiteBinding binding = invocation.getArgument(0);
            binding.setId("51");
            return "51";
        });
        when(revisionDAO.insert(any(AnalyzerSiteBindingRevision.class))).thenAnswer(invocation -> {
            AnalyzerSiteBindingRevision revision = invocation.getArgument(0);
            revision.setId("61");
            return "61";
        });
    }

    @Test
    public void resolveInitialRevisionCreatesEveryProfileMappingUnresolvedAndAudits() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        when(bindingDAO.findByProfileBindingId("41")).thenReturn(Optional.empty());

        AnalyzerSiteBindingSnapshot created = service.resolveInitialRevision(profileBinding, portableProfile(), "17");

        assertEquals("51", created.binding().getId());
        assertSame(profileBinding, created.binding().getProfileBinding());
        assertEquals(1, created.revision().getRevisionNumber());
        assertEquals(2, created.tests().size());
        assertEquals(List.of("HIV", "WBC"),
                created.tests().stream().map(row -> row.getId().getSourceRowKey()).toList());
        assertEquals(2, created.results().size());
        assertEquals(List.of("NEG", "POS"), created.results().stream().map(row -> row.getId().getRawValue()).toList());
        created.tests().forEach(row -> assertEquals(AnalyzerSiteBindingMappingState.UNRESOLVED, row.getMappingState()));
        created.results()
                .forEach(row -> assertEquals(AnalyzerSiteBindingMappingState.UNRESOLVED, row.getMappingState()));

        verify(bindingDAO).insert(created.binding());
        verify(revisionDAO).insert(created.revision());
        verify(testDAO).insert(created.tests().get(0));
        verify(testDAO).insert(created.tests().get(1));
        verify(resultDAO).insert(created.results().get(0));
        verify(resultDAO).insert(created.results().get(1));
        verify(auditTrailService).saveNewHistory(created.revision(), "17", "analyzer_site_binding_revision");
    }

    @Test
    public void resolveInitialRevisionReusesExistingBindingWithoutNewWrites() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerSiteBinding binding = binding(profileBinding);
        AnalyzerSiteBindingRevision revision = revision(binding);
        AnalyzerSiteBindingTest test = new AnalyzerSiteBindingTest();
        AnalyzerSiteBindingResult result = new AnalyzerSiteBindingResult();
        when(bindingDAO.findByProfileBindingId("41")).thenReturn(Optional.of(binding));
        when(revisionDAO.findLatestByBindingId("51")).thenReturn(Optional.of(revision));
        when(testDAO.findByRevisionId("61")).thenReturn(List.of(test));
        when(resultDAO.findByRevisionId("61")).thenReturn(List.of(result));

        AnalyzerSiteBindingSnapshot existing = service.resolveInitialRevision(profileBinding, portableProfile(), "17");

        assertSame(binding, existing.binding());
        assertSame(revision, existing.revision());
        assertEquals(List.of(test), existing.tests());
        assertEquals(List.of(result), existing.results());
        verify(bindingDAO, never()).insert(any());
        verify(revisionDAO, never()).insert(any());
        verifyZeroInteractions(auditTrailService);
    }

    @Test
    public void resolveInitialRevisionRejectsMismatchedPortableIdentityBeforeWriting() throws Exception {
        JsonNode wrongRevision = portableProfile().deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) wrongRevision.path("catalog")).put("revision", 4);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.resolveInitialRevision(profileBinding(), wrongRevision, "17"));

        assertEquals("Portable profile does not match the selected profile reference", error.getMessage());
        verifyZeroInteractions(bindingDAO, revisionDAO, testDAO, resultDAO, auditTrailService);
    }

    @Test
    public void appendRevisionRejectsDuplicateRowsAndInvalidTargetsBeforeWriting() {
        AnalyzerSiteBinding binding = binding(profileBinding());
        AnalyzerSiteBindingDraft duplicate = new AnalyzerSiteBindingDraft(
                List.of(test("wbc", AnalyzerSiteBindingMappingState.BOUND, "9701"),
                        test("wbc", AnalyzerSiteBindingMappingState.UNRESOLVED, null)),
                List.of());
        AnalyzerSiteBindingDraft invalidTarget = new AnalyzerSiteBindingDraft(
                List.of(test("wbc", AnalyzerSiteBindingMappingState.UNRESOLVED, "9701")), List.of());

        assertEquals("Duplicate test source row: wbc",
                assertThrows(IllegalArgumentException.class, () -> service.appendRevision(binding, duplicate, "17"))
                        .getMessage());
        assertEquals("UNRESOLVED test row wbc cannot have a local target",
                assertThrows(IllegalArgumentException.class, () -> service.appendRevision(binding, invalidTarget, "17"))
                        .getMessage());
        verifyZeroInteractions(revisionDAO, testDAO, resultDAO, auditTrailService);
    }

    @Test
    public void appendRevisionPreservesIndependentRowsAndSupersedesWithoutMutation() {
        AnalyzerSiteBinding binding = binding(profileBinding());
        AnalyzerSiteBindingRevision current = revision(binding);
        when(revisionDAO.findLatestByBindingId("51")).thenReturn(Optional.of(current));
        AnalyzerSiteBindingDraft draft = new AnalyzerSiteBindingDraft(
                List.of(test("wbc-primary", AnalyzerSiteBindingMappingState.BOUND, "9701"),
                        test("wbc-alias", AnalyzerSiteBindingMappingState.BOUND, "9701")),
                List.of());

        AnalyzerSiteBindingSnapshot appended = service.appendRevision(binding, draft, "17");

        assertEquals(2, appended.revision().getRevisionNumber());
        assertSame(current, appended.revision().getSupersedesRevision());
        assertEquals(List.of("wbc-alias", "wbc-primary"),
                appended.tests().stream().map(row -> row.getId().getSourceRowKey()).toList());
        assertEquals(1, current.getRevisionNumber());
        verify(auditTrailService).saveNewHistory(appended.revision(), "17", "analyzer_site_binding_revision");
    }

    private AnalyzerProfileBinding profileBinding() {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("41");
        binding.setProfileId(PROFILE_ID);
        binding.setProfileRevision(PROFILE_REVISION);
        binding.setProfileFingerprint(PROFILE_FINGERPRINT);
        return binding;
    }

    private static AnalyzerSiteBinding binding(AnalyzerProfileBinding profileBinding) {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("51");
        binding.setProfileBinding(profileBinding);
        return binding;
    }

    private static AnalyzerSiteBindingRevision revision(AnalyzerSiteBinding binding) {
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("61");
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(1);
        revision.setBindingFingerprint("sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        return revision;
    }

    private JsonNode portableProfile() throws Exception {
        return objectMapper.readTree("""
                {
                  "profileMeta":{"id":"site.mock-hematology"},
                  "catalog":{
                    "revision":3,
                    "revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                  },
                  "default_test_mappings":[
                    {
                      "test_code":"WBC",
                      "loinc":"6690-2",
                      "result_type":"quantitative"
                    },
                    {
                      "test_code":"HIV",
                      "loinc":"20447-9",
                      "result_type":"qualitative",
                      "values":["POS","NEG"]
                    }
                  ]
                }
                """);
    }

    private static AnalyzerSiteBindingTestDraft test(String sourceRowKey, AnalyzerSiteBindingMappingState state,
            String testId) {
        return new AnalyzerSiteBindingTestDraft(sourceRowKey, state, testId);
    }
}
