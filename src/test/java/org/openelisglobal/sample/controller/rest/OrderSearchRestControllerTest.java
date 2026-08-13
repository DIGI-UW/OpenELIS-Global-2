package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.test.util.ReflectionTestUtils;

public class OrderSearchRestControllerTest {

    @Test
    public void addsPersistedMicrobiologyDraftToOrderSearchResponse() {
        OrderSearchRestController controller = new OrderSearchRestController();
        MicroCaseOrderDetailService service = mock(MicroCaseOrderDetailService.class);
        MicroCaseOrderDetailRequestForm draft = new MicroCaseOrderDetailRequestForm();
        Sample sample = new Sample();
        sample.setId("42");
        when(service.getOrderDraft("42")).thenReturn(draft);
        ReflectionTestUtils.setField(controller, "microCaseOrderDetailService", service);
        Map<String, Object> response = new HashMap<>();

        controller.addMicrobiologyOrderDetail(response, sample);

        assertSame(draft, response.get("microbiologyOrderDetail"));
    }

    @Test
    public void leavesResponseUnchangedWhenMicrobiologyIsUnavailable() {
        OrderSearchRestController controller = new OrderSearchRestController();
        Sample sample = new Sample();
        sample.setId("42");
        Map<String, Object> response = new HashMap<>();

        controller.addMicrobiologyOrderDetail(response, sample);

        assertTrue(response.isEmpty());
    }

    @Test
    public void mapsCanonicalProgramIdentityForReloadedOrders() {
        OrderSearchRestController controller = new OrderSearchRestController();
        Program program = new Program();
        program.setId("8");
        program.setProgramName("Microbiology");
        program.setCode("MICROBIOLOGY");
        Map<String, Object> sampleOrderItems = new HashMap<>();

        ReflectionTestUtils.invokeMethod(controller, "addProgramSelection", sampleOrderItems, program);

        assertEquals("8", sampleOrderItems.get("programId"));
        assertEquals("Microbiology", sampleOrderItems.get("program"));
        assertEquals("MICROBIOLOGY", sampleOrderItems.get("programCode"));
    }
}
