package org.openelisglobal.dashboard.management.surveillance;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.Reader;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.dashboard.management.surveillance.controller.SurveillanceMappingRestController;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceImportResult;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceMappingService;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceMappingView;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class,
        SurveillanceMappingRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class SurveillanceMappingRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String URL = "/rest/dashboards/management/surveillance-mappings";

    @Test
    public void dashboardViewerCannotListOrImport() throws Exception {
        SimpleGrantedAuthority viewer = new SimpleGrantedAuthority("dashboards.view.management");
        mockMvc.perform(get(URL).with(user("viewer").authorities(viewer))).andExpect(status().isForbidden());
        mockMvc.perform(upload("m.csv").with(user("viewer").authorities(viewer))).andExpect(status().isForbidden());
    }

    @Test
    public void adminRoleWithoutTheQiGrantIsRefused() throws Exception {
        mockMvc.perform(upload("m.csv").with(user("bench").roles("ADMIN", "RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void qiManagerCanListAndImport() throws Exception {
        SimpleGrantedAuthority manager = new SimpleGrantedAuthority("qa.manage.qi");
        mockMvc.perform(get(URL).with(user("qi").authorities(manager))).andExpect(status().isOk());
        mockMvc.perform(upload("m.csv").with(user("qi").authorities(manager))).andExpect(status().isOk())
                .andExpect(jsonPath("$.rowsAdded").value(1));
    }

    @Test
    public void globalAdminGetsABadRequestForAFileTheServiceRejects() throws Exception {
        mockMvc.perform(upload("bad.csv").with(user("admin").roles("GLOBAL_ADMIN"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing columns: test"));
    }

    private static MockHttpServletRequestBuilder upload(String fileName) {
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(1);
        return multipart(URL + "/import").file(new MockMultipartFile("file", fileName, "text/csv", new byte[] { 'x' }))
                .requestAttr(IActionConstants.USER_SESSION_DATA, session);
    }

    @Configuration
    static class TestConfig {

        @Bean
        SurveillanceMappingService surveillanceMappingService() {
            return new SurveillanceMappingService() {
                @Override
                public List<SurveillanceMappingView> getAllViews() {
                    return List.of();
                }

                @Override
                public SurveillanceImportResult importCsv(Reader csv, String sourceFile, String sysUserId) {
                    if (sourceFile.equals("bad.csv")) {
                        throw new IllegalArgumentException("Missing columns: test");
                    }
                    return new SurveillanceImportResult(1, 0, List.of());
                }
            };
        }

        @Bean
        SurveillanceMappingRestController surveillanceMappingRestController() {
            return new SurveillanceMappingRestController();
        }
    }
}
