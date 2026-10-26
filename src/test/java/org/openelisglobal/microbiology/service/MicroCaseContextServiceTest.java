package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;

public class MicroCaseContextServiceTest {
    private final MicroCaseDAO cases = mock(MicroCaseDAO.class);
    private final MicroCaseAuthorizationService authorization = mock(MicroCaseAuthorizationService.class);
    private final MicroCaseActivityDAO activities = mock(MicroCaseActivityDAO.class);
    private final SampleService samples = mock(SampleService.class);
    private final VectorSamplingSiteService sites = mock(VectorSamplingSiteService.class);
    private final TestSectionService units = mock(TestSectionService.class);
    private final MicroCaseContextService service = new MicroCaseContextService(cases, authorization, activities,
            samples, sites, mock(ObservationHistoryService.class), units,
            mock(org.openelisglobal.program.service.ProgramService.class));
    private MicroCase first;
    private MicroCase second;
    private Sample order;

    @Before
    public void setup() {
        first = aCase("a");
        second = aCase("b");
        order = new Sample();
        order.setId("1");
        order.setDomain("environmental");
        when(samples.get("1")).thenReturn(order);
        when(cases.getByOrder("1")).thenReturn(List.of(first, second));
        var unit = new TestSection();
        unit.setDomain("environmental");
        when(units.get("2")).thenReturn(unit);
        var site = new VectorSamplingSite();
        site.setName("Ward surface");
        site.setCode("WARD");
        when(sites.get(3)).thenReturn(site);
    }

    private MicroCase aCase(String id) {
        var c = new MicroCase();
        c.setId(id);
        c.setSampleId("1");
        c.setSiteId("3");
        c.setLabUnitId("2");
        when(cases.get(id)).thenReturn(Optional.of(c));
        when(authorization.requireWrite(eq(id), eq("actor"), anyString())).thenReturn(c);
        return c;
    }

    @Test
    public void purposeIsPerCaseAndReplicatesAreSharedWithTwoTimelineEntries() {
        var request = new MicroCaseContextRequestForm();
        request.culturePurpose = "OUTBREAK_INVESTIGATION";
        request.replicateCount = 3;
        service.update("a", request, "actor");
        assertEquals("OUTBREAK_INVESTIGATION", first.getCulturePurpose());
        assertEquals("ROUTINE_MONITORING", second.getCulturePurpose());
        assertEquals(Integer.valueOf(3), order.getMicrobiologyReplicateCount());
        var events = org.mockito.ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activities, times(3)).insert(events.capture());
        assertEquals(2,
                events.getAllValues().stream().filter(e -> "REPLICATES_CHANGED".equals(e.getActivityType())).count());
        assertEquals(List.of("a", "b"),
                events.getAllValues().stream().filter(e -> "REPLICATES_CHANGED".equals(e.getActivityType()))
                        .map(MicroCaseActivity::getCaseId).toList());
    }

    @Test
    public void lockedSiblingPreventsAllChanges() {
        when(authorization.requireWrite(eq("b"), eq("actor"), anyString()))
                .thenThrow(new IllegalStateException("locked"));
        var request = new MicroCaseContextRequestForm();
        request.culturePurpose = "COMPLAINT";
        request.replicateCount = 2;
        assertThrows(IllegalStateException.class, () -> service.update("a", request, "actor"));
        assertEquals("ROUTINE_MONITORING", first.getCulturePurpose());
        assertNull(order.getMicrobiologyReplicateCount());
        verify(activities, never()).insert(any());
        verify(samples, never()).update(any());
    }

    @Test
    public void siteSubjectClearsAccidentalPatientDataAndRejectsClinicalPurpose() {
        var form = new MicroCaseShellForm();
        form.patientId = "patient";
        form.patientName = "should not leak";
        form.gender = "F";
        service.compile(first, form);
        assertNull(form.patientId);
        assertNull(form.patientName);
        assertNull(form.gender);
        assertEquals("Ward surface", form.siteName);
        assertEquals("WARD", form.siteCode);
        assertFalse(form.purposeOptions.contains("DIAGNOSTIC"));
        var request = new MicroCaseContextRequestForm();
        request.culturePurpose = "DIAGNOSTIC";
        assertThrows(IllegalArgumentException.class, () -> service.update("a", request, "actor"));
        verify(cases, never()).update(any());
    }
}
