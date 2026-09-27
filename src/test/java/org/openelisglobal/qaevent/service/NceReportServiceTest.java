package org.openelisglobal.qaevent.service;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.worker.NonConformingEventWorker;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public class NceReportServiceTest {

    @Test
    public void createsAndCompletesOneNceWithAuthenticatedActor() {
        NonConformingEventWorker worker = mock(NonConformingEventWorker.class);
        NceNumberGeneratorService numberService = mock(NceNumberGeneratorService.class);
        NCEventService eventService = mock(NCEventService.class);
        SystemUserService userService = mock(SystemUserService.class);
        NceReportService service = new NceReportServiceImpl(worker, numberService, eventService, userService);
        NonConformingEventForm form = validForm();
        form.setCurrentUserId("spoofed-user");
        NcEvent created = new NcEvent();
        created.setId(42);
        when(numberService.generateNceNumber()).thenReturn("NCE-2026-00042");
        SystemUser authenticatedUser = new SystemUser();
        authenticatedUser.setFirstName("Authenticated");
        authenticatedUser.setLastName("Reporter");
        when(userService.getUserById("17")).thenReturn(authenticatedUser);
        when(worker.create(eq("ACC-1"), anyList(), eq("17"), eq("NCE-2026-00042"), eq(null))).thenReturn(created);
        when(worker.update(form)).thenReturn(true);
        when(eventService.get(42)).thenReturn(created);

        NcEvent result = service.report(form, "17");

        assertSame(created, result);
        verify(worker).create(eq("ACC-1"), eq(java.util.List.of("1001")), eq("17"), eq("NCE-2026-00042"), eq(null));
        verify(worker).update(form);
        org.junit.Assert.assertEquals("17", form.getCurrentUserId());
        org.junit.Assert.assertEquals("42", form.getId());
        org.junit.Assert.assertEquals("Authenticated Reporter", form.getReporterName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingLinkedSpecimenBeforePersistence() {
        NceReportService service = new NceReportServiceImpl(mock(NonConformingEventWorker.class),
                mock(NceNumberGeneratorService.class), mock(NCEventService.class), mock(SystemUserService.class));
        NonConformingEventForm form = validForm();
        form.setSpecimenId("");

        service.report(form, "17");
    }

    private NonConformingEventForm validForm() {
        NonConformingEventForm form = new NonConformingEventForm();
        form.setLabOrderNumber("ACC-1");
        form.setSpecimenId("1001");
        form.setDateOfEvent("08/05/2026");
        form.setReportingUnit(7);
        form.setDescription("Specimen handling issue");
        form.setSeverity("MAJOR");
        form.setNceCategoryId("3");
        form.setNceTypeId("19");
        return form;
    }
}
