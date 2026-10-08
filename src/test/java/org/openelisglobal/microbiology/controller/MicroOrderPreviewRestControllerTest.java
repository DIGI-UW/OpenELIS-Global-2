package org.openelisglobal.microbiology.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroOrderPreviewRestController;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyRestExceptionHandler;
import org.openelisglobal.microbiology.form.MicroOrderPreviewForm;
import org.openelisglobal.microbiology.form.MicroOrderPreviewRequestForm;
import org.openelisglobal.microbiology.service.MicroOrderPreviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

public class MicroOrderPreviewRestControllerTest {
    private MockMvc mvc;
    private MicroOrderPreviewService service;
    private MockHttpSession session;

    @Before
    public void setUp() {
        service = mock(MicroOrderPreviewService.class);
        mvc = MockMvcBuilders.standaloneSetup(new MicroOrderPreviewRestController(service))
                .setControllerAdvice(new MicrobiologyRestExceptionHandler()).build();
        session = new MockHttpSession();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(42);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, user);
    }

    @Test
    public void serializesThePreviewAndUsesOnlyTheSessionActor() throws Exception {
        when(service.preview(any(), eq("42"))).thenReturn(new MicroOrderPreviewForm(
                List.of(new MicroOrderPreviewForm.CaseLine("1", "Microbiology",
                        List.of(new MicroOrderPreviewForm.SpecimenLine(0, "Blood")), List.of("Culture"), true, List.of(), List.of())),
                List.of(), List.of(), List.of(), List.of()));
        mvc.perform(post("/rest/microbiology/order-preview").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"specimens\":[{\"sampleTypeId\":\"5\",\"testIds\":[\"culture\"]}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cases[0].labUnitName").value("Microbiology"))
                .andExpect(jsonPath("$.cases[0].specimens[0].index").value(0))
                .andExpect(jsonPath("$.ordinaryTests").isArray());
        verify(service).preview(argThat(request -> request.specimens.get(0).testIds.equals(List.of("culture"))),
                eq("42"));
    }

    @Test
    public void deniedUnitAndInvalidSelectionAreNotSuccessfulPredictions() throws Exception {
        when(service.preview(any(MicroOrderPreviewRequestForm.class), eq("42")))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .thenThrow(new IllegalArgumentException("Unknown preview test"));
        mvc.perform(post("/rest/microbiology/order-preview").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"specimens\":[]}")).andExpect(status().isForbidden());
        mvc.perform(post("/rest/microbiology/order-preview").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"specimens\":[]}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MICROBIOLOGY_VALIDATION_ERROR"));
    }
}
