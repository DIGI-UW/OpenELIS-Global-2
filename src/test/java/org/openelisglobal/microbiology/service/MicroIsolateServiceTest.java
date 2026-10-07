package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAmendmentDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseSpecimen;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RunWith(MockitoJUnitRunner.class)
public class MicroIsolateServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroCaseAmendmentDAO amendmentDAO;

    @Mock
    private MicroIdentificationHistoryService identificationHistoryService;

    @Mock
    private MicroCaseSpecimenDAO specimenDAO;

    @Mock
    private MicrobiologyCaseAccessService accessService;

    private MicroIsolateService service;

    @Before
    public void setUp() {
        service = new MicroIsolateServiceImpl(caseDAO, isolateDAO, activityDAO, amendmentDAO,
                identificationHistoryService, specimenDAO, accessService);
        lenient().when(caseDAO.get("case-1")).thenReturn(Optional.of(mutableCase()));
        lenient().when(caseDAO.getForUpdate("case-1")).thenAnswer(invocation -> caseDAO.get("case-1").orElseThrow());
        lenient().when(isolateDAO.getForUpdate(any(String.class)))
                .thenAnswer(invocation -> isolateDAO.get(invocation.<String>getArgument(0)).orElseThrow());
        MicroCaseSpecimen member = new MicroCaseSpecimen();
        member.setCaseId("case-1");
        member.setSampleItemId("101");
        lenient().when(specimenDAO.getByCaseAndSampleItem("case-1", "101")).thenReturn(member);
        MicroIsolateIdentificationEvent event = new MicroIsolateIdentificationEvent();
        event.setId("event-1");
        lenient().when(identificationHistoryService.recordChange(any(MicroIsolate.class), any(MicroIsolate.class),
                org.mockito.ArgumentMatchers.nullable(String.class), any(String.class))).thenReturn(event);
    }

    @Test
    public void reidentificationRequiresValidationBeforeChangingClinicalHistory() {
        MicroIsolate isolate = permissionIsolate();
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        isolate.setOrganismId("org-original");
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(accessService).requireValidation("case-1", "7");
        assertEquals(403,
                assertThrows(ResponseStatusException.class, () -> identifyAs("org-corrected")).getStatusCode().value());
        assertEquals("org-original", isolate.getOrganismId());
        verify(isolateDAO, never()).update(any());
        verify(activityDAO, never()).insert(any());
        verify(identificationHistoryService, never()).recordChange(any(), any(), any(), any());
    }

    @Test
    public void initialIdentificationUsesResultsRatherThanValidation() {
        permissionIsolate();
        identifyAs("org-first");
        verify(accessService).requireResults("case-1", "7");
        verify(accessService, never()).requireValidation(any(), any());
    }

    @Test
    public void permissionsUseTheCurrentIdentityAfterOrderCaseAndIsolateLocks() {
        permissionIsolate();
        MicroIsolate refreshed = new MicroIsolate();
        refreshed.setId("iso-1");
        refreshed.setCaseId("case-1");
        refreshed.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        refreshed.setOrganismId("org-concurrent");
        doReturn(refreshed).when(isolateDAO).getForUpdate("iso-1");
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(accessService).requireValidation("case-1", "7");
        assertEquals(403,
                assertThrows(ResponseStatusException.class, () -> identifyAs("org-corrected")).getStatusCode().value());
        var locks = inOrder(caseDAO, isolateDAO, accessService);
        locks.verify(caseDAO).lockOrder(any());
        locks.verify(caseDAO).getForUpdate("case-1");
        locks.verify(isolateDAO).getForUpdate("iso-1");
        locks.verify(accessService).requireValidation("case-1", "7");
        assertEquals("org-concurrent", refreshed.getOrganismId());
        verify(isolateDAO, never()).update(any());
    }

    private MicroIsolate permissionIsolate() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.PRELIMINARY.name());
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        return isolate;
    }

    private MicroIsolate identifyAs(String organismId) {
        return service.updateIdentification("iso-1", organismId, "Identification",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "MALDI_TOF", new BigDecimal("99.5"), "Correct identity", "7");
    }

    @Test
    public void createIsolateRecordsPreliminaryWorkupAndAdvancesCase() {
        MicroIsolate isolate = service.createIsolate("case-1", "101", "ISO-1", "Gram negative rods",
                "Lactose fermenting colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1");

        assertEquals("case-1", isolate.getCaseId());
        assertEquals("101", isolate.getSourceSampleItemId());
        assertEquals("ISO-1", isolate.getIsolateLabel());
        assertEquals("Gram negative rods", isolate.getGramStain());
        assertEquals("Lactose fermenting colonies", isolate.getColonyMorphology());
        assertNull(isolate.getOrganismId());
        assertEquals(MicroIsolateIdentificationStatus.PRELIMINARY.name(), isolate.getIdentificationStatus());
        verify(isolateDAO).insert(isolate);
        verify(activityDAO).insert(any(MicroCaseActivity.class));
        ArgumentCaptor<MicroCase> caseCaptor = ArgumentCaptor.forClass(MicroCase.class);
        verify(caseDAO).update(caseCaptor.capture());
        assertEquals(MicroCaseStage.IDENTIFICATION.name(), caseCaptor.getValue().getStage());
    }

    @Test
    public void createIsolateRequiresGramStain() {
        assertThrows(IllegalArgumentException.class, () -> service.createIsolate("case-1", "101", "ISO-1", " ",
                "colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1"));
    }

    @Test
    public void createIsolateDuringAmendmentLinksDraftToOpenAmendment() {
        MicroCase amendmentCase = mutableCase();
        amendmentCase.setStage(MicroCaseStage.AMENDED.name());
        amendmentCase.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(amendmentCase));
        MicroCaseAmendment amendment = new MicroCaseAmendment();
        amendment.setId("amendment-1");
        amendment.setCaseId("case-1");
        when(amendmentDAO.getOpenByCaseId("case-1")).thenReturn(amendment);

        MicroIsolate isolate = service.createIsolate("case-1", "101", "ISO-2", "Gram positive cocci",
                "Second colony type", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "9");

        assertEquals("amendment-1", isolate.getAmendmentId());
        assertNull(isolate.getCancelledAt());
        verify(isolateDAO).insert(isolate);
    }

    @Test
    public void updateIdentificationPreservesCaseActivityTrail() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setIsolateLabel("ISO-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        when(isolateDAO.update(isolate)).thenReturn(isolate);

        MicroIsolate updated = service.updateIdentification("iso-1", "org-1", "E. coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "MALDI_TOF", new BigDecimal("99.5"), "1");

        assertEquals("org-1", updated.getOrganismId());
        assertEquals("MALDI_TOF", updated.getIdentificationMethod());
        assertEquals(new BigDecimal("99.5"), updated.getIdentificationConfidence());
        assertEquals(MicroIsolateIdentificationStatus.CONFIRMED.name(), updated.getIdentificationStatus());
        verify(activityDAO).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void updateIdentificationRejectsBlankOrganismId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateIdentification("iso-1", "  ", "E. coli",
                        MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                        "MALDI_TOF", new BigDecimal("99.5"), "1"));
    }

    @Test
    public void updateIdentificationRejectsACompletedWorkupWithPreliminaryStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateIdentification("iso-1", "org-1", "E. coli",
                        MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.PRELIMINARY,
                        "MALDI_TOF", new BigDecimal("99.5"), "1"));
    }

    @Test
    public void updateIdentificationRejectsCancelledAmendmentIsolate() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-cancelled");
        isolate.setCaseId("case-1");
        isolate.setCancelledAt(MicroCaseServiceImpl.now());
        when(isolateDAO.get("iso-cancelled")).thenReturn(Optional.of(isolate));

        try {
            service.updateIdentification("iso-cancelled", "org-1", "E. coli",
                    MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                    "MALDI_TOF", new BigDecimal("99.5"), "1");
            fail("Expected cancelled amendment isolate to remain immutable");
        } catch (IllegalStateException expected) {
            assertEquals("ISOLATE_CANCELLED", expected.getMessage());
        }
    }

    @Test
    public void reidentificationDuringAmendmentRequiresReasonAndRecordsBeforeAfterHistory() {
        MicroCase amendmentCase = mutableCase();
        amendmentCase.setStage(MicroCaseStage.AMENDED.name());
        amendmentCase.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(amendmentCase));
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setIsolateLabel("ISO-1");
        isolate.setOrganismId("org-old");
        isolate.setPreliminaryOrganismText("Escherichia coli");
        isolate.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        when(isolateDAO.update(isolate)).thenReturn(isolate);

        MicroIsolate updated = service.updateIdentification("iso-1", "org-new", "Klebsiella pneumoniae",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED, "PCR",
                new BigDecimal("100"), "Corrected after confirmatory identification", "9");

        assertEquals("org-new", updated.getOrganismId());
        verify(identificationHistoryService).recordChange(any(MicroIsolate.class), any(MicroIsolate.class),
                org.mockito.ArgumentMatchers.eq("Corrected after confirmatory identification"),
                org.mockito.ArgumentMatchers.eq("9"));
        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(
                "Isolate ISO-1 identification changed from Escherichia coli to Klebsiella pneumoniae: Corrected after confirmatory identification",
                activity.getValue().getNote());
    }

    @Test(expected = IllegalStateException.class)
    public void createIsolateRejectsFinalReleasedCases() {
        MicroCase finalCase = mutableCase();
        finalCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        finalCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(finalCase));

        service.createIsolate("case-1", "101", "ISO-1", "Gram negative rods", "colonies",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1");
    }

    @Test
    public void createIsolateRequiresAnExplicitMemberSource() {
        assertThrows(IllegalArgumentException.class, () -> service.createIsolate("case-1", null, "ISO-1",
                "Gram negative rods", "colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1"));
        verify(isolateDAO, never()).insert(any(MicroIsolate.class));
        verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void createIsolateRejectsASpecimenOutsideTheCaseWithoutWriting() {
        assertThrows(IllegalArgumentException.class, () -> service.createIsolate("case-1", "other-specimen", "ISO-1",
                "Gram negative rods", "colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1"));
        verify(isolateDAO, never()).insert(any(MicroIsolate.class));
        verify(caseDAO, never()).update(any(MicroCase.class));
        verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void createIsolateRequiresResultsRightsInItsCaseUnit() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(accessService).requireResults("case-1", "1");
        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.createIsolate("case-1", "101", "ISO-1", "Gram negative rods", "colonies",
                        MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, "1"));
        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
        verify(isolateDAO, never()).insert(any(MicroIsolate.class));
        verify(activityDAO, never()).insert(any(MicroCaseActivity.class));
    }

    private MicroCase mutableCase() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        return microCase;
    }
}
