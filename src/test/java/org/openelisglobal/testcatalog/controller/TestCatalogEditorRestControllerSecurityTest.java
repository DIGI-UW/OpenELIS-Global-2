package org.openelisglobal.testcatalog.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogNumericIdGuard;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingService;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * FR-004: the unified Test Catalog editor surface is privilege-gated at the
 * service layer (S011c) — the API returns 401 for the unauthenticated and 403
 * for authenticated users lacking the catalog privileges (PRIV_RESULT_VIEW /
 * PRIV_TEST_CONFIGURE / PRIV_SAMPLE_TYPE_VIEW / PRIV_PANEL_*). Service
 * collaborators are JDK-Proxy stubs (nullStub) not Mockito mocks, so
 * withoutAnnotations() so the interface's @PreAuthorize is the single
 * annotation source Spring Security evaluates.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { TestCatalogEditorRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class TestCatalogEditorRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getEnvelope_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getEnvelope_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/1").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getEnvelope_adminUnknownTestReturns404() throws Exception {
        // Admin passes the gate; the (mocked) service returns null → 404, proving
        // the request reached the controller rather than being blocked by auth.
        mockMvc.perform(get("/rest/test-catalog/tests/999999")
                .with(user("admin").authorities(AuthorityUtils.createAuthorityList("PRIV_RESULT_VIEW",
                        "PRIV_TEST_CONFIGURE", "PRIV_SAMPLE_TYPE_VIEW", "PRIV_PANEL_VIEW", "PRIV_PANEL_MANAGE"))))
                .andExpect(status().isNotFound());
    }

    /**
     * OGC-1153 — the numeric-id guard is a {@code WebMvcConfigurer} that registers
     * itself, so simply having the bean in an {@code @EnableWebMvc} context must be
     * enough to make a malformed id 404. This is the wiring proof: no test code
     * registers the interceptor, only the bean exists.
     */
    @Test
    public void getEnvelope_adminNonNumericTestIdReturns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/notanumber").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    /**
     * The guard is an MVC interceptor, so it runs before {@code @PreAuthorize} — a
     * non-admin asking for a malformed id sees the same 404 an admin sees rather
     * than 403. That leaks nothing, and role enforcement is untouched for
     * well-formed ids (the 403 cases either side of this).
     */
    @Test
    public void getEnvelope_nonAdminNonNumericTestIdReturns404() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/notanumber").with(user("results").roles("RESULTS")))
                .andExpect(status().isNotFound());
    }

    /**
     * The guard must never shadow authentication: the security filter chain runs
     * ahead of the DispatcherServlet, so an anonymous caller still gets 401 for a
     * malformed id rather than a 404 that would confirm the URL exists.
     */
    @Test
    public void getEnvelope_withoutAuthenticationNonNumericTestIdStillReturns401() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/notanumber")).andExpect(status().isUnauthorized());
    }

    @Test
    public void saveBasicInfo_nonAdminReturns403() throws Exception {
        mockMvc.perform(put("/rest/test-catalog/tests/1/basic-info").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void saveBasicInfo_adminUnknownTestReturns404() throws Exception {
        // Admin passes the gate; the mocked service returns null for an unknown test
        // → 404, proving the write-path reached the controller past auth.
        mockMvc.perform(put("/rest/test-catalog/tests/999999/basic-info")
                .with(user("admin").authorities(AuthorityUtils.createAuthorityList("PRIV_RESULT_VIEW",
                        "PRIV_TEST_CONFIGURE", "PRIV_SAMPLE_TYPE_VIEW", "PRIV_PANEL_VIEW", "PRIV_PANEL_MANAGE")))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    @Test
    public void getAnalyzers_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/1/analyzers").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getAnalyzers_adminUnknownTestReturns404() throws Exception {
        // Admin passes the gate; the mocked TestService returns null → 404, proving
        // the read-path reached the controller past auth.
        mockMvc.perform(get("/rest/test-catalog/tests/999999/analyzers")
                .with(user("admin").authorities(AuthorityUtils.createAuthorityList("PRIV_RESULT_VIEW",
                        "PRIV_TEST_CONFIGURE", "PRIV_SAMPLE_TYPE_VIEW", "PRIV_PANEL_VIEW", "PRIV_PANEL_MANAGE"))))
                .andExpect(status().isNotFound());
    }

    @Test
    public void listSampleTypes_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/sample-types").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getTestOrder_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/sample-types/1/test-order").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void saveTestOrder_nonAdminReturns403() throws Exception {
        mockMvc.perform(put("/rest/test-catalog/sample-types/1/test-order").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void getTerminology_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/1/terminology").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void saveTerminology_nonAdminReturns403() throws Exception {
        mockMvc.perform(put("/rest/test-catalog/tests/1/terminology").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void listPanels_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/panels").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getTestPanels_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/test-catalog/tests/1/panels").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void saveTestPanels_nonAdminReturns403() throws Exception {
        mockMvc.perform(put("/rest/test-catalog/tests/1/panels").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        /**
         * Unknown ids resolve to null as before, but a non-numeric one runs the same
         * {@code Integer.parseInt} {@code LIMSStringNumberUserType} does when it binds
         * the id — so if the guard bean below ever stops self-registering, the
         * malformed-id cases blow up here instead of quietly passing on a null.
         */
        @Bean
        TestService testService() {
            // JDK-Proxy stub, not a Mockito mock: a Mockito mock subclass re-declares
            // the interface's @PreAuthorize, and Spring Security's unique-annotation
            // scan then finds it twice and fails. getTestById mirrors the
            // LIMSStringNumberUserType id binding — non-numeric ids blow up here — while
            // every other method falls back to nullStub's null/default.
            TestService delegate = nullStub(TestService.class);
            return (TestService) java.lang.reflect.Proxy.newProxyInstance(TestService.class.getClassLoader(),
                    new Class<?>[] { TestService.class }, (proxy, method, args) -> {
                        if (method.getName().equals("getTestById") && args != null && args.length == 1
                                && args[0] instanceof String) {
                            Integer.parseInt((String) args[0]);
                            return null;
                        }
                        return method.invoke(delegate, args);
                    });
        }

        /**
         * Declared as a plain bean on purpose: it is a {@code WebMvcConfigurer} and
         * must wire its own interceptor into the {@code @EnableWebMvc} context.
         */
        @Bean
        TestCatalogNumericIdGuard testCatalogNumericIdGuard() {
            return new TestCatalogNumericIdGuard();
        }

        // TypeOfSampleService and PanelService are gated by a CLASS-level
        // @PreAuthorize on the interface. Gates only bind to Spring BEANS — the
        // method-security advisor never sees stubs created inline in a factory
        // method — so these two collaborators are registered as beans.
        @Bean
        TypeOfSampleService typeOfSampleService() {
            return nullStub(TypeOfSampleService.class);
        }

        @Bean
        PanelService panelService() {
            return nullStub(PanelService.class);
        }

        @Bean
        TestCatalogEditorRestController testCatalogEditorRestController(TestService testService,
                TypeOfSampleService typeOfSampleService, PanelService panelService) {
            // Only the auth ordering is under test; the section services are unused here.
            return new TestCatalogEditorRestController(testService, nullStub(TestResultComponentService.class),
                    nullStub(TestResultInterpretationService.class), nullStub(TestResultService.class),
                    nullStub(ResultLimitService.class), mock(RangeCoverageValidationService.class),
                    nullStub(TestSampleHandlingService.class), nullStub(AnalyzerService.class),
                    nullStub(AnalyzerTestMappingService.class), typeOfSampleService,
                    nullStub(TypeOfSampleTestService.class), nullStub(TestTerminologyMappingService.class),
                    panelService, nullStub(PanelItemService.class));
        }
    }
}
