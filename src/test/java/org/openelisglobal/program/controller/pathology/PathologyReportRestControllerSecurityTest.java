package org.openelisglobal.program.controller.pathology;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletContext;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.service.PathologyReportService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
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
 * The report requests carry no permission key of their own: an anonymous
 * request is refused before the case is looked up or any report is read or
 * written.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class,
        PathologyReportRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class PathologyReportRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String REPORTS = "/rest/pathology/caseView/7/reports";

    private PathologyReportService reportService;
    private PathologySampleService caseService;
    private ProgramSampleLabUnitScope scope;

    @Before
    public void resetStubs() {
        reportService = webApplicationContext.getBean(PathologyReportService.class);
        caseService = webApplicationContext.getBean(PathologySampleService.class);
        scope = webApplicationContext.getBean(ProgramSampleLabUnitScope.class);
        reset(reportService, caseService, scope);
    }

    @Test
    public void anAnonymousGenerate_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(post(REPORTS + "/generate").with(anonymous())).andExpect(status().isUnauthorized());

        assertNothingReached();
        verify(reportService, never()).generate(any(), any(), any());
    }

    @Test
    public void anAnonymousUpload_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(post(REPORTS).with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fileType\":\"data:application/pdf\",\"base64Image\":\"JVBERi0=\"}"))
                .andExpect(status().isUnauthorized());

        assertNothingReached();
        verify(reportService, never()).upload(any(), any(), any());
    }

    @Test
    public void anAnonymousFileRead_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(get(REPORTS + "/3/file").with(anonymous())).andExpect(status().isUnauthorized());

        assertNothingReached();
        verify(reportService, never()).fileOf(any(), any());
    }

    @Test
    public void anAnonymousWithdraw_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(post(REPORTS + "/3/withdraw").with(anonymous())).andExpect(status().isUnauthorized());

        assertNothingReached();
        verify(reportService, never()).withdraw(any(), any(), any());
    }

    /**
     * A signed-in user reaches the service at the same address, so the anonymous
     * refusals above are the filter's and not a missing endpoint.
     */
    @Test
    public void aSignedInGenerate_reachesTheServiceAsThatUser() throws Exception {
        mockMvc.perform(post(REPORTS + "/generate").with(user("tech")).sessionAttr(IActionConstants.USER_SESSION_DATA,
                sessionUser())).andExpect(status().isOk());

        verify(reportService).generate(eq(7), eq("1"), any());
    }

    // helpers

    private static UserSessionData sessionUser() {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        return userSessionData;
    }

    /** Neither the case nor the lab-unit check was reached. */
    private void assertNothingReached() {
        verify(caseService, never()).get(any());
        verify(scope, never()).requireVisible(any(), any());
    }

    @Configuration
    static class TestConfig {

        @Bean
        PathologyReportService pathologyReportService() {
            return mock(PathologyReportService.class);
        }

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
        PathologyReportRestController pathologyReportRestController(PathologyReportService pathologyReportService,
                PathologySampleService pathologySampleService, ProgramSampleLabUnitScope programSampleLabUnitScope,
                ServletContext servletContext) {
            return new PathologyReportRestController(pathologyReportService, pathologySampleService,
                    programSampleLabUnitScope, servletContext);
        }
    }
}
