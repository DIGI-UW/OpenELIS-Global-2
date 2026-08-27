package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.form.MicroCaseNonconformanceRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.service.NceReportService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.sample.service.SampleItemRejectionService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public class MicroCaseNonconformanceServiceTest {

    @Test
    public void reportsFlagOnlyNceAgainstExistingSampleWithoutRejectingWork() {
        Fixture fixture = fixture(MicroCaseStage.INCUBATING);
        MicroCaseNonconformanceRequestForm request = request("FLAG_ONLY", "NONCONFORMANCE");

        MicroCaseNonconformanceResult result = fixture.service.report("case-1", request, "17");

        assertEquals("NCE-2026-00001", result.nceNumber());
        assertEquals(List.of("case-1"), result.affectedCaseIds());
        verify(fixture.rejectionService, never()).reject(any(), any(), any());
        verify(fixture.caseDAO, never()).update(any());
        ArgumentCaptor<NonConformingEventForm> nceForm = ArgumentCaptor.forClass(NonConformingEventForm.class);
        verify(fixture.nceReportService).report(nceForm.capture(), org.mockito.ArgumentMatchers.eq("17"));
        assertEquals("ACC-1", nceForm.getValue().getLabOrderNumber());
        assertEquals("1001", nceForm.getValue().getSpecimenId());
    }

    @Test
    public void lostPhysicalSpecimenRejectsWorkAndTransitionsAllOpenSiblingCases() {
        Fixture fixture = fixture(MicroCaseStage.INCUBATING);
        MicroCase sibling = microCase("case-2", MicroCaseStage.IDENTIFICATION);
        when(fixture.caseDAO.getBySampleItem("1001")).thenReturn(List.of(fixture.microCase, sibling));
        MicroCaseNonconformanceRequestForm request = request("REJECT_TEST", "SPECIMEN_LOST");

        MicroCaseNonconformanceResult result = fixture.service.report("case-1", request, "17");

        verify(fixture.rejectionService).reject("1001", "Specimen lost", "17");
        assertEquals(MicroCaseStage.LOST_SPECIMEN.name(), fixture.microCase.getStage());
        assertEquals(MicroCaseStage.LOST_SPECIMEN_POSITIVE.name(), sibling.getStage());
        verify(fixture.caseDAO).update(fixture.microCase);
        verify(fixture.caseDAO).update(sibling);
        assertEquals(List.of("case-1", "case-2"), result.affectedCaseIds());
        verify(fixture.activityDAO, org.mockito.Mockito.times(2)).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void retestDispositionIsDeferredToTheRepeatAstLayer() {
        Fixture fixture = fixture(MicroCaseStage.REVIEW_READY);
        MicroCaseNonconformanceRequestForm request = request("RETEST", "NONCONFORMANCE");
        try {
            fixture.service.report("case-1", request, "17");
            org.junit.Assert.fail("Expected repeat AST to remain unavailable in the workbench layer");
        } catch (MicroCaseWorkflowConflictException exception) {
            assertEquals("MICROBIOLOGY_AST_RETEST_NOT_AVAILABLE", exception.getMessage());
        }
    }

    private Fixture fixture(MicroCaseStage stage) {
        MicroCaseDAO caseDAO = mock(MicroCaseDAO.class);
        MicroCaseActivityDAO activityDAO = mock(MicroCaseActivityDAO.class);
        SampleItemService sampleItemService = mock(SampleItemService.class);
        NceReportService nceReportService = mock(NceReportService.class);
        SampleItemRejectionService rejectionService = mock(SampleItemRejectionService.class);
        MicroCase microCase = microCase("case-1", stage);
        Sample sample = new Sample();
        sample.setAccessionNumber("ACC-1");
        SampleItem item = new SampleItem();
        item.setId("1001");
        item.setSample(sample);
        NcEvent nce = new NcEvent();
        nce.setId(1);
        nce.setNceNumber("NCE-2026-00001");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseDAO.getBySampleItem("1001")).thenReturn(List.of(microCase));
        when(sampleItemService.get("1001")).thenReturn(item);
        when(nceReportService.report(any(NonConformingEventForm.class), org.mockito.ArgumentMatchers.eq("17")))
                .thenReturn(nce);
        MicroCaseNonconformanceService service = new MicroCaseNonconformanceServiceImpl(caseDAO, activityDAO,
                sampleItemService, nceReportService, rejectionService);
        return new Fixture(service, caseDAO, activityDAO, nceReportService, rejectionService, microCase);
    }

    private MicroCase microCase(String id, MicroCaseStage stage) {
        MicroCase microCase = new MicroCase();
        microCase.setId(id);
        microCase.setSampleItemId("1001");
        microCase.setWorkflowType("BACTERIOLOGY");
        microCase.setStage(stage.name());
        return microCase;
    }

    private MicroCaseNonconformanceRequestForm request(String disposition, String eventType) {
        MicroCaseNonconformanceRequestForm request = new MicroCaseNonconformanceRequestForm();
        request.categoryId = "3";
        request.typeId = "19";
        request.reportingUnitId = 7;
        request.severity = "MAJOR";
        request.title = "Specimen event";
        request.description = "Specimen handling issue";
        request.disposition = disposition;
        request.eventType = eventType;
        return request;
    }

    private record Fixture(MicroCaseNonconformanceService service, MicroCaseDAO caseDAO,
            MicroCaseActivityDAO activityDAO, NceReportService nceReportService,
            SampleItemRejectionService rejectionService, MicroCase microCase) {
    }
}
