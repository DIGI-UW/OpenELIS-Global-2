package org.openelisglobal.program.controller.pathology;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.OptimisticLockException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.valueholder.pathology.CoverslipQc;
import org.openelisglobal.program.valueholder.pathology.StainStatus;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Staining and the coverslip check carry no permission key of their own: any
 * signed-in user who may read the case may record them, and nobody else reaches
 * the service at all.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class,
        PathologySlideWorkRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class PathologySlideWorkRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String STAIN_STATUS = "/rest/pathology/caseView/7/slides/stain-status";
    private static final String COVERSLIP_QC = "/rest/pathology/caseView/7/slides/coverslip-qc";

    private PathologySampleService service;

    @Before
    public void resetStubs() {
        service = webApplicationContext.getBean(PathologySampleService.class);
        reset(service, webApplicationContext.getBean(ProgramSampleLabUnitScope.class));
        when(service.recordStainStatus(any(), any(), any(), any())).thenReturn(List.of());
        when(service.recordCoverslipQc(any(), any(), any(), any())).thenReturn(List.of());
    }

    /** An anonymous request is refused before the case is even looked up. */
    @Test
    public void anAnonymousStainStatus_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(post(STAIN_STATUS).with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"slideIds\":[91],\"status\":\"STAINED\"}")).andExpect(status().isUnauthorized());

        verify(service, never()).get(any());
        verify(service, never()).recordStainStatus(any(), any(), any(), any());
    }

    @Test
    public void anAnonymousCoverslipCheck_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(post(COVERSLIP_QC).with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"slideIds\":[91],\"result\":\"PASS\"}")).andExpect(status().isUnauthorized());

        verify(service, never()).get(any());
        verify(service, never()).recordCoverslipQc(any(), any(), any(), any());
    }

    /** A signed-in user reaches the service with the slides and value named. */
    @Test
    public void aSignedInStainStatus_reachesTheServiceAsThatUser() throws Exception {
        mockMvc.perform(post(STAIN_STATUS).with(user("tech"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"slideIds\":[91,92],\"status\":\"STAINED\"}")).andExpect(status().isOk());

        verify(service).recordStainStatus(eq(7), eq(List.of(91, 92)), eq(StainStatus.STAINED), eq("1"));
    }

    @Test
    public void aSignedInCoverslipCheck_reachesTheServiceAsThatUser() throws Exception {
        mockMvc.perform(
                post(COVERSLIP_QC).with(user("tech")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slideIds\":[91],\"result\":\"FAIL\"}"))
                .andExpect(status().isOk());

        verify(service).recordCoverslipQc(eq(7), eq(List.of(91)), eq(CoverslipQc.FAIL), eq("1"));
    }

    /** A request that loses to another change on the case is a conflict, not an error. */
    @Test
    public void aCheckThatLosesTheVersionCheck_answers409() throws Exception {
        when(service.recordCoverslipQc(any(), any(), any(), any())).thenThrow(new OptimisticLockException());

        mockMvc.perform(post(COVERSLIP_QC).with(user("tech"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"slideIds\":[91],\"result\":\"PASS\"}")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflict").value("version"));
    }

    // helpers

    private static UserSessionData sessionUser() {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        return userSessionData;
    }

    @Configuration
    static class TestConfig {

        @Bean
        PathologySampleService pathologySampleService() {
            return mock(PathologySampleService.class);
        }

        @Bean
        ProgramSampleLabUnitScope programSampleLabUnitScope() {
            return mock(ProgramSampleLabUnitScope.class);
        }

        /**
         * Spring still injects the scope's own field on the mock it is handed, so that
         * field needs a candidate even though no call reaches it.
         */
        @Bean
        SampleLabUnitScopeService sampleLabUnitScopeService() {
            return mock(SampleLabUnitScopeService.class);
        }

        @Bean
        PathologySlideWorkRestController pathologySlideWorkRestController(PathologySampleService pathologySampleService,
                ProgramSampleLabUnitScope programSampleLabUnitScope) {
            return new PathologySlideWorkRestController(pathologySampleService, programSampleLabUnitScope);
        }
    }
}
