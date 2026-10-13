package org.openelisglobal.microbiology.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class MicroBenchRequestBindingTest {
    @Test
    public void queryFieldsBindWithoutReinitializingJsonRequestBinder() throws Exception {
        var service = mock(MicroBenchService.class);
        var controller = new MicroBenchRestController(service, mock(MicroBenchCaseService.class)) {
            @Override
            protected String authenticatedUserId(jakarta.servlet.http.HttpServletRequest request) {
                return "writer";
            }
        };
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(service.search(any(), eq("writer"))).thenReturn(new MicroBenchPageForm());
        when(service.apply(any(), eq("writer"))).thenReturn(List.of());
        mvc.perform(get("/rest/microbiology/bench").param("cultureFilter", "CHECK_DUE").param("page", "2"))
                .andExpect(status().isOk());
        var query = ArgumentCaptor.forClass(MicroBenchQueryForm.class);
        verify(service).search(query.capture(), eq("writer"));
        assertEquals("CHECK_DUE", query.getValue().cultureFilter);
        assertEquals(2, query.getValue().page);
        mvc.perform(post("/rest/microbiology/bench").contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"NO_GROWTH\",\"cases\":[{\"caseId\":\"case\",\"cultureIds\":[\"plate\"]}]}"))
                .andExpect(status().isOk());
        var request = ArgumentCaptor.forClass(MicroBenchRequestForm.class);
        verify(service).apply(request.capture(), eq("writer"));
        assertEquals("NO_GROWTH", request.getValue().action);
        assertEquals(List.of("plate"), request.getValue().cases.get(0).cultureIds);
    }
}
