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
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
import org.openelisglobal.program.valueholder.pathology.StageDirection;
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
 * The stage moves carry no permission key of their own: any signed-in user who
 * may read the case may move it, and nobody else reaches the service at all.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class,
        PathologyStageRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class PathologyStageRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String ADVANCE = "/rest/pathology/caseView/7/advance";
    private static final String RETURN = "/rest/pathology/caseView/7/return";

    private PathologySampleService service;

    @Before
    public void resetStubs() {
        service = webApplicationContext.getBean(PathologySampleService.class);
        reset(service, webApplicationContext.getBean(ProgramSampleLabUnitScope.class));
        when(service.advance(any(), any(), any())).thenReturn(entryOnto(PathologyStatus.PROCESSING));
        when(service.returnToPreviousStage(any(), any(), any(), any())).thenReturn(entryOnto(PathologyStatus.GROSSING));
    }

    /** An anonymous request is refused before the case is even looked up. */
    @Test
    public void anAnonymousAdvance_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(post(ADVANCE).with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"GROSSING\"}")).andExpect(status().isUnauthorized());

        verify(service, never()).get(any());
        verify(service, never()).advance(any(), any(), any());
    }

    @Test
    public void anAnonymousReturn_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(post(RETURN).with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"from\":\"PROCESSING\",\"reason\":\"re-cut\"}")).andExpect(status().isUnauthorized());

        verify(service, never()).get(any());
        verify(service, never()).returnToPreviousStage(any(), any(), any(), any());
    }

    /** A signed-in user reaches the service with the stage the page showed. */
    @Test
    public void aSignedInAdvance_reachesTheServiceAsThatUser() throws Exception {
        mockMvc.perform(post(ADVANCE).with(user("tech")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                .contentType(MediaType.APPLICATION_JSON).content("{\"from\":\"GROSSING\"}")).andExpect(status().isOk());

        verify(service).advance(eq(7), eq(PathologyStatus.GROSSING), eq("1"));
    }

    @Test
    public void aSignedInReturn_reachesTheServiceWithTheReason() throws Exception {
        mockMvc.perform(post(RETURN).with(user("tech")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                .contentType(MediaType.APPLICATION_JSON).content("{\"from\":\"PROCESSING\",\"reason\":\"re-cut\"}"))
                .andExpect(status().isOk());

        verify(service).returnToPreviousStage(eq(7), eq(PathologyStatus.PROCESSING), eq("re-cut"), eq("1"));
    }

    /**
     * Two moves of one case in the same moment race on the case's version check;
     * the one that loses is a conflict the screen can explain, not a server error.
     */
    @Test
    public void anAdvanceThatLosesTheVersionCheck_answers409() throws Exception {
        when(service.advance(any(), any(), any())).thenThrow(new OptimisticLockException());

        mockMvc.perform(post(ADVANCE).with(user("tech")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                .contentType(MediaType.APPLICATION_JSON).content("{\"from\":\"GROSSING\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.conflict").value("version"));
    }

    // helpers

    private static UserSessionData sessionUser() {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        return userSessionData;
    }

    /**
     * No times on purpose: formatting them reads site configuration this slice does
     * not load.
     */
    private static PathologyStageEvent entryOnto(PathologyStatus stage) {
        PathologyStageEvent event = new PathologyStageEvent();
        event.setId(1);
        event.setStage(stage);
        event.setDirection(stage == PathologyStatus.GROSSING ? StageDirection.RETURN : StageDirection.FORWARD);
        return event;
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
        PathologyStageRestController pathologyStageRestController(PathologySampleService pathologySampleService,
                ProgramSampleLabUnitScope programSampleLabUnitScope) {
            return new PathologyStageRestController(pathologySampleService, programSampleLabUnitScope);
        }
    }
}
