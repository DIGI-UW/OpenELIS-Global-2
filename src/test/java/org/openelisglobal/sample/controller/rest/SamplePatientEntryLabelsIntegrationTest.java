package org.openelisglobal.sample.controller.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-285 M5b SAFETY guard for the order-save label-persistence hook. The
 * legacy {@code populateWorkflowPrintModels}/{@code extractLabelQuantities}
 * BarcodeWorkflowPrintService path that this controller used to run on every
 * save was deleted in the OGC-285 flow migration (the post-save print dialog
 * now reads the persisted JSONB snapshots from {@code GET
 * /api/orders/by-accession/{labNo}/labels}). These tests pin the remaining
 * contract: {@code maybePersistLabelRequests} fires the snapshot persistence
 * ONLY when the save body carried a {@code labelPersistRequest}, so every
 * legacy / decoupled / batch save (which leaves the field null) is completely
 * untouched.
 */
@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryLabelsIntegrationTest {

    @Mock
    private SamplePatientEntryService samplePatientService;

    private SamplePatientEntryRestController controller;

    @Before
    public void setUp() {
        controller = new SamplePatientEntryRestController();
        ReflectionTestUtils.setField(controller, "samplePatientService", samplePatientService);
    }

    @Test
    public void maybePersistLabelRequests_firesPersistence_whenPayloadPresent() {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        OrderLabelPersistRequest payload = new OrderLabelPersistRequest();
        form.setLabelPersistRequest(payload);
        // mock (not new) — SamplePatientUpdateData's constructor reaches into
        // SpringContext, and the helper only passes the value through to the
        // (mocked) service, never dereferencing it.
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);

        controller.maybePersistLabelRequests(form, updateData, "1");

        verify(samplePatientService).persistLabelRequests(updateData, payload, "1");
    }

    @Test
    public void maybePersistLabelRequests_isNoOp_whenPayloadNull() {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        // no labelPersistRequest set — the legacy/decoupled save path
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);

        controller.maybePersistLabelRequests(form, updateData, "1");

        verify(samplePatientService, never()).persistLabelRequests(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
