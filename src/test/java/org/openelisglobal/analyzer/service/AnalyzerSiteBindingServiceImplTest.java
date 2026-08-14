package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingTestDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.audittrail.dao.AuditTrailService;

public class AnalyzerSiteBindingServiceImplTest {

    private AnalyzerSiteBindingDAO bindingDAO;
    private AnalyzerSiteBindingRevisionDAO revisionDAO;
    private AnalyzerSiteBindingTestDAO testDAO;
    private AuditTrailService auditTrailService;
    private AnalyzerSiteBindingService service;

    @Before
    public void setUp() {
        bindingDAO = mock(AnalyzerSiteBindingDAO.class);
        revisionDAO = mock(AnalyzerSiteBindingRevisionDAO.class);
        testDAO = mock(AnalyzerSiteBindingTestDAO.class);
        auditTrailService = mock(AuditTrailService.class);
        service = new AnalyzerSiteBindingServiceImpl(bindingDAO, revisionDAO, testDAO, auditTrailService);
    }

    @Test
    public void createsAuditedInitialRevisionWithIndependentSortedSourceRows() {
        AnalyzerSiteBindingDraft draft = draft(List.of(
                row("row-2", "WBC_ALIAS", List.of("WHITE_COUNT"), AnalyzerSiteBindingTest.MappingState.UNRESOLVED, null,
                        null),
                row("row-1", "WBC", List.of("WHITE_COUNT", "WBC#"), AnalyzerSiteBindingTest.MappingState.BOUND, "9701",
                        "component-1")));

        AnalyzerSiteBindingSnapshot created = service.create(draft, "42");

        assertEquals(1, created.revision().getRevisionNumber().intValue());
        assertEquals("site.mock-hematology", created.revision().getBridgeProfileId());
        assertEquals(3, created.revision().getBridgeProfileRevision().intValue());
        assertEquals("42", created.revision().getCreatedBy());
        assertEquals("sha256:836016e624b68ae797172f7c86e49ba83149070333917853528f486ecfb2d1c6",
                created.revision().getFingerprint());
        assertEquals(List.of("row-1", "row-2"),
                created.tests().stream().map(test -> test.getId().getSourceRowKey()).toList());
        assertSame(created.revision(), created.tests().get(0).getSiteBindingRevision());

        verify(bindingDAO).insert(created.binding());
        verify(revisionDAO).insert(created.revision());
        verify(testDAO).insert(created.tests().get(0));
        verify(testDAO).insert(created.tests().get(1));
        verify(auditTrailService).saveNewHistory(created.revision(), "42", "analyzer_site_binding_revision");
    }

    @Test
    public void canonicalFingerprintIgnoresInputAndAliasOrderingButTracksTargetChanges() {
        AnalyzerSiteBindingDraft first = draft(List.of(
                row("row-2", "WBC_ALIAS", List.of("WHITE_COUNT"), AnalyzerSiteBindingTest.MappingState.UNRESOLVED, null,
                        null),
                row("row-1", "WBC", List.of("WBC#", "WHITE_COUNT"), AnalyzerSiteBindingTest.MappingState.BOUND, "9701",
                        "component-1")));
        AnalyzerSiteBindingDraft reordered = draft(List.of(
                row("row-1", "WBC", List.of("WHITE_COUNT", "WBC#"), AnalyzerSiteBindingTest.MappingState.BOUND, "9701",
                        "component-1"),
                row("row-2", "WBC_ALIAS", List.of("WHITE_COUNT"), AnalyzerSiteBindingTest.MappingState.UNRESOLVED, null,
                        null)));
        AnalyzerSiteBindingDraft changedTarget = draft(List.of(
                row("row-1", "WBC", List.of("WHITE_COUNT", "WBC#"), AnalyzerSiteBindingTest.MappingState.BOUND, "9702",
                        "component-1"),
                row("row-2", "WBC_ALIAS", List.of("WHITE_COUNT"), AnalyzerSiteBindingTest.MappingState.UNRESOLVED, null,
                        null)));

        assertEquals(AnalyzerSiteBindingFingerprint.calculate(first),
                AnalyzerSiteBindingFingerprint.calculate(reordered));
        assertNotEquals(AnalyzerSiteBindingFingerprint.calculate(first),
                AnalyzerSiteBindingFingerprint.calculate(changedTarget));
    }

    @Test
    public void revisingLocksAggregateAndAppendsWithoutMutatingPriorRevision() {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("binding-1");
        AnalyzerSiteBindingRevision current = new AnalyzerSiteBindingRevision();
        current.setId("revision-4");
        current.setSiteBinding(binding);
        current.setRevisionNumber(4);
        when(bindingDAO.findByIdForUpdate("binding-1")).thenReturn(Optional.of(binding));
        when(revisionDAO.findLatestByBindingId("binding-1")).thenReturn(Optional.of(current));

        AnalyzerSiteBindingSnapshot revised = service.revise("binding-1",
                draft(List
                        .of(row("row-1", "WBC", List.of(), AnalyzerSiteBindingTest.MappingState.BOUND, "9701", null))),
                "42");

        assertSame(binding, revised.binding());
        assertEquals(5, revised.revision().getRevisionNumber().intValue());
        assertSame(current, revised.revision().getSupersedesRevision());
        assertEquals("revision-4", current.getId());
        verify(bindingDAO, never()).insert(any(AnalyzerSiteBinding.class));
        verify(revisionDAO).insert(revised.revision());
        verify(auditTrailService).saveNewHistory(revised.revision(), "42", "analyzer_site_binding_revision");
    }

    @Test
    public void rejectsDuplicateSourceRowsAndInvalidTargetsBeforeWriting() {
        AnalyzerSiteBindingDraft duplicate = draft(List.of(
                row("row-1", "WBC", List.of(), AnalyzerSiteBindingTest.MappingState.BOUND, "9701", null),
                row("row-1", "WBC_ALIAS", List.of(), AnalyzerSiteBindingTest.MappingState.UNRESOLVED, null, null)));
        AnalyzerSiteBindingDraft invalidTarget = draft(
                List.of(row("row-2", "WBC", List.of(), AnalyzerSiteBindingTest.MappingState.UNRESOLVED, "9701", null)));

        assertEquals("Duplicate sourceRowKey: row-1",
                assertThrows(IllegalArgumentException.class, () -> service.create(duplicate, "42")).getMessage());
        assertEquals("UNRESOLVED row row-2 cannot have a local target",
                assertThrows(IllegalArgumentException.class, () -> service.create(invalidTarget, "42")).getMessage());

        verifyZeroInteractions(bindingDAO, revisionDAO, testDAO, auditTrailService);
    }

    @Test
    public void rejectsMissingActorAndMissingAggregateBeforeWritingRevision() {
        AnalyzerSiteBindingDraft draft = draft(
                List.of(row("row-1", "WBC", List.of(), AnalyzerSiteBindingTest.MappingState.BOUND, "9701", null)));
        when(bindingDAO.findByIdForUpdate(eq("missing"))).thenReturn(Optional.empty());

        assertEquals("actor is required",
                assertThrows(IllegalArgumentException.class, () -> service.create(draft, " ")).getMessage());
        assertEquals("Analyzer site binding not found: missing",
                assertThrows(IllegalArgumentException.class, () -> service.revise("missing", draft, "42"))
                        .getMessage());
        verify(revisionDAO, never()).findLatestByBindingId(any());
    }

    private static AnalyzerSiteBindingDraft draft(List<AnalyzerSiteBindingTestDraft> rows) {
        return new AnalyzerSiteBindingDraft("site.mock-hematology", 3, rows);
    }

    private static AnalyzerSiteBindingTestDraft row(String sourceRowKey, String rawCode, List<String> aliases,
            AnalyzerSiteBindingTest.MappingState state, String testId, String componentId) {
        return new AnalyzerSiteBindingTestDraft(sourceRowKey, rawCode, aliases, "White blood cells", "NUMERIC",
                "http://loinc.org", "6690-2", state, testId, componentId);
    }
}
