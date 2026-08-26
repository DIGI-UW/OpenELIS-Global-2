package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroIsolateRestController;
import org.openelisglobal.microbiology.controller.rest.MicrobiologyRestExceptionHandler;
import org.openelisglobal.microbiology.service.MicroCaseLockedException;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class MicrobiologyRestExceptionHandlerTest {

    @Test
    public void finalCaseMutationReturnsNamedConflict() {
        ResponseEntity<Map<String, Object>> response = new MicrobiologyRestExceptionHandler()
                .handleLockedCase(new MicroCaseLockedException("Final-released microbiology cases cannot be changed"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("MICROBIOLOGY_CASE_LOCKED", response.getBody().get("error"));
        assertEquals("Final-released microbiology cases cannot be changed", response.getBody().get("message"));
    }

    @Test
    public void moduleSpecificLockHandlerWinsOverGlobalRuntimeHandler() throws Exception {
        MicroIsolateService isolateService = org.mockito.Mockito.mock(MicroIsolateService.class);
        when(isolateService.createIsolate(any(), any(), any(), any(), any(), any()))
                .thenThrow(new MicroCaseLockedException("Final-released microbiology cases cannot be changed"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MicroIsolateRestController(isolateService))
                .setControllerAdvice(new MicrobiologyRestExceptionHandler(), new ControllerSetup()).build();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(42);

        mockMvc.perform(post("/rest/microbiology/isolates").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "caseId": "case-1",
                          "isolateLabel": "ISO-LOCKED",
                          "preliminaryOrganismText": "Must not persist",
                          "significance": "UNKNOWN"
                        }
                        """)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MICROBIOLOGY_CASE_LOCKED"));
    }
}
