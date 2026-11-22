package org.openelisglobal.qaevent.service;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.internationalization.UiMessagesWithoutOverrides;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.worker.NonConformingEventWorker;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public class NceReportServiceTest {

    @BeforeClass
    public static void catalogueWithoutOverrides() throws Exception {
        UiMessagesWithoutOverrides.install();
    }

    @AfterClass
    public static void restoreSpringContext() throws Exception {
        UiMessagesWithoutOverrides.restore();
    }

    private final SampleService sampleService = mock(SampleService.class);
    private final SampleItemService sampleItemService = mock(SampleItemService.class);

    private NceReportService service(NonConformingEventWorker worker, NceNumberGeneratorService numberService,
            NCEventService eventService, SystemUserService userService) {
        return new NceReportServiceImpl(worker, numberService, eventService, userService, sampleService,
                sampleItemService);
    }

    @Test
    public void createsAndCompletesOneNceWithAuthenticatedActor() {
        NonConformingEventWorker worker = mock(NonConformingEventWorker.class);
        NceNumberGeneratorService numberService = mock(NceNumberGeneratorService.class);
        NCEventService eventService = mock(NCEventService.class);
        SystemUserService userService = mock(SystemUserService.class);
        NceReportService service = service(worker, numberService, eventService, userService);
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

    /**
     * The order steps report an NCE against the order they show without naming a
     * specimen; it is linked to the order's specimens (OGC-1507 walk).
     */
    @Test
    public void anOrderWithoutAChosenSpecimenLinksTheOrdersSpecimens() {
        NonConformingEventWorker worker = mock(NonConformingEventWorker.class);
        NceNumberGeneratorService numberService = mock(NceNumberGeneratorService.class);
        NCEventService eventService = mock(NCEventService.class);
        NceReportService service = service(worker, numberService, eventService, mock(SystemUserService.class));
        NonConformingEventForm form = validForm();
        form.setSpecimenId("");
        Sample order = new Sample();
        order.setId("500");
        when(sampleService.getSampleByAccessionNumber("ACC-1")).thenReturn(order);
        SampleItem rejected = item("1003");
        rejected.setRejected(true);
        when(sampleItemService.getSampleItemsBySampleId("500"))
                .thenReturn(List.of(item("1001"), item("1002"), rejected));
        NcEvent created = new NcEvent();
        created.setId(44);
        when(numberService.generateNceNumber()).thenReturn("NCE-2026-00044");
        when(worker.create(eq("ACC-1"), eq(List.of("1001", "1002")), eq("17"), eq("NCE-2026-00044"), eq(null)))
                .thenReturn(created);
        when(worker.update(form)).thenReturn(true);
        when(eventService.get(44)).thenReturn(created);

        assertSame(created, service.report(form, "17"));
    }

    @Test
    public void anUnknownOrderIsRefusedBeforePersistence() {
        NonConformingEventWorker worker = mock(NonConformingEventWorker.class);
        NceNumberGeneratorService numberService = mock(NceNumberGeneratorService.class);
        NceReportService service = service(worker, numberService, mock(NCEventService.class),
                mock(SystemUserService.class));
        NonConformingEventForm form = validForm();
        form.setSpecimenId("");

        try {
            service.report(form, "17");
            org.junit.Assert.fail("an unknown lab number was accepted");
        } catch (IllegalArgumentException expected) {
            org.junit.Assert.assertEquals("No order has the lab number ACC-1.", expected.getMessage());
        }
        verify(numberService, never()).generateNceNumber();
    }

    /**
     * An event that concerns no sample (a training gap, a policy violation) is
     * reported without an order, as the report form offers (OGC-1507 walk).
     */
    @Test
    public void reportsAnEventLinkedToNoOrder() {
        NonConformingEventWorker worker = mock(NonConformingEventWorker.class);
        NceNumberGeneratorService numberService = mock(NceNumberGeneratorService.class);
        NCEventService eventService = mock(NCEventService.class);
        NceReportService service = service(worker, numberService, eventService, mock(SystemUserService.class));
        NonConformingEventForm form = validForm();
        form.setLabOrderNumber("");
        form.setSpecimenId("");
        NcEvent created = new NcEvent();
        created.setId(43);
        when(numberService.generateNceNumber()).thenReturn("NCE-2026-00043");
        when(worker.create(eq(""), eq(java.util.List.of()), eq("17"), eq("NCE-2026-00043"), eq(null)))
                .thenReturn(created);
        when(worker.update(form)).thenReturn(true);
        when(eventService.get(43)).thenReturn(created);

        assertSame(created, service.report(form, "17"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSpecimensWithoutTheirOrder() {
        NceReportService service = service(mock(NonConformingEventWorker.class), mock(NceNumberGeneratorService.class),
                mock(NCEventService.class), mock(SystemUserService.class));
        NonConformingEventForm form = validForm();
        form.setLabOrderNumber(" ");

        service.report(form, "17");
    }

    private static SampleItem item(String id) {
        SampleItem item = new SampleItem();
        item.setId(id);
        return item;
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
