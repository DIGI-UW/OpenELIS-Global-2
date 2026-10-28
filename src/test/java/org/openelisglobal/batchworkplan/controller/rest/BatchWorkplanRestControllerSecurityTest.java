package org.openelisglobal.batchworkplan.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.service.BatchReleaseRefusedException;
import org.openelisglobal.batchworkplan.service.BatchWorkplanNotFoundException;
import org.openelisglobal.batchworkplan.service.BatchWorkplanService;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * OGC-427: the batch workplan is a Results surface, and the release passes the
 * caller's {@code qa.qc.override} authority to the service rather than gating
 * the endpoint on it, since a batch whose QC passes needs no override.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class, BatchWorkplanRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class BatchWorkplanRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String GENERATE = "/rest/batch-workplans/batches/7/generate";
    private static final String OVERRIDE = "{\"override\":true}";

    private BatchWorkplanService service;

    @Before
    public void resetStub() {
        service = webApplicationContext.getBean(BatchWorkplanService.class);
        org.mockito.Mockito.reset(service);
        when(service.generate(any(), anyBoolean(), anyBoolean(), any())).thenReturn(new BatchWorkplanResponse());
    }

    private UserSessionData sessionUser() {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        return userSessionData;
    }

    @Test
    public void receptionIsRefusedTheWholeSurface() throws Exception {
        mockMvc.perform(get("/rest/batch-workplans/batches").with(user("front").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void aResultsUserWithoutTheKeyReleasesWithoutOverrideRights() throws Exception {
        mockMvc.perform(post(GENERATE).with(user("tech").roles("RESULTS"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content(OVERRIDE)).andExpect(status().isOk());
        verify(service).generate(eq(7L), eq(true), eq(false), any());
    }

    @Test
    public void theOverrideKeyGivesOverrideRights() throws Exception {
        mockMvc.perform(post(GENERATE)
                .with(user("lead").roles("RESULTS").authorities(new SimpleGrantedAuthority("ROLE_RESULTS"),
                        new SimpleGrantedAuthority("qa.qc.override")))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content(OVERRIDE)).andExpect(status().isOk());
        verify(service).generate(eq(7L), eq(true), eq(true), any());
    }

    @Test
    public void aRefusedReleaseAnswersWithTheQcStanding() throws Exception {
        BatchQcStatusResponse qc = new BatchQcStatusResponse(BatchQcStatus.OVERDUE, false, true, false, null, null,
                List.of(), List.of());
        when(service.generate(any(), anyBoolean(), anyBoolean(), any()))
                .thenThrow(new BatchReleaseRefusedException(BatchReleaseRefusedException.Reason.OVERRIDE_REQUIRED, qc));
        mockMvc.perform(post(GENERATE).with(user("tech").roles("RESULTS"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("OVERRIDE_REQUIRED"))
                .andExpect(jsonPath("$.qc.status").value("OVERDUE"));
    }

    @Test
    public void anOverrideWithoutTheKeyIsForbidden() throws Exception {
        when(service.generate(any(), anyBoolean(), anyBoolean(), any())).thenThrow(new BatchReleaseRefusedException(
                BatchReleaseRefusedException.Reason.OVERRIDE_NOT_PERMITTED, null));
        mockMvc.perform(post(GENERATE).with(user("tech").roles("RESULTS")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content(OVERRIDE)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.reason").value("OVERRIDE_NOT_PERMITTED"));
    }

    @Test
    public void someoneElsesBatchIsNotFound() throws Exception {
        when(service.getBatch(eq(9L), any())).thenThrow(new BatchWorkplanNotFoundException(9L));
        mockMvc.perform(get("/rest/batch-workplans/batches/9").with(user("tech").roles("RESULTS")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void aRefusedEditAnswers400WithTheReason() throws Exception {
        when(service.getQcStatus(eq(8L), any())).thenThrow(new IllegalArgumentException("Select a reagent"));
        mockMvc.perform(get("/rest/batch-workplans/batches/8/qc-status").with(user("tech").roles("RESULTS")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Select a reagent"));
    }

    @Configuration
    static class TestConfig {

        @Bean
        BatchWorkplanService batchWorkplanService() {
            return mock(BatchWorkplanService.class);
        }

        @Bean
        BatchWorkplanRestController batchWorkplanRestController(BatchWorkplanService batchWorkplanService) {
            return new BatchWorkplanRestController(batchWorkplanService);
        }
    }
}
