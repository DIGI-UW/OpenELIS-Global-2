package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
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
}
