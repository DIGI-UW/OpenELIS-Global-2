package org.openelisglobal.common.management.controller.rest;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.SupportedLocaleService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.service.TestSectionCreateService;
import org.openelisglobal.testconfiguration.service.TestSectionTestAssignService;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { LabUnitManagementRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class LabUnitManagementRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    // Injected so the validation cases (LU-W-3) can stub the section lookup and
    // the active locales; the auth cases leave them at their default behaviour.
    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private SupportedLocaleService supportedLocaleService;

    @Autowired
    private LocalizationService localizationService;

    // Context-scoped mocks are shared across methods; reset so stubs and
    // recorded interactions never leak into the auth cases.
    @After
    public void resetMocks() {
        Mockito.reset(testSectionService, supportedLocaleService, localizationService);
    }

    @Test
    public void getLabUnits_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getLabUnits_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void getLabUnits_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    // Mutating endpoints must carry the same ADMIN gate. A non-admin PUT
    // returning 403 also pins the hasRole('ADMIN') convention.

    @Test
    public void updateLabUnit_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateLabUnit_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void updateLabUnit_AdminRole_PassesAuth() throws Exception {
        // the mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(put("/rest/lab-units-management/1").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    @Test
    public void getLabUnitById_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management/1").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void getLabUnitById_AdminRole_PassesAuth() throws Exception {
        // the mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(get("/rest/lab-units-management/1").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void updateDisplayOrder_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1/display-order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"position\":1}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void updateDisplayOrder_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1/display-order").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":1}")).andExpect(status().isForbidden());
    }

    @Test
    public void updateDisplayOrder_AdminRole_PassesAuth() throws Exception {
        // the mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(put("/rest/lab-units-management/1/display-order").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":1}")).andExpect(status().isNotFound());
    }

    @Test
    public void updateDisplayOrder_AdminRole_InvalidPosition_Returns422() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1/display-order").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":0}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void assignedTests_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management/1/tests").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void assignedTests_AdminRole_PassesAuth() throws Exception {
        // mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(get("/rest/lab-units-management/1/tests").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    // Create + bulk assignment endpoints carry the same ADMIN gate.

    @Test
    public void createLabUnit_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(post("/rest/lab-units-management").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createLabUnit_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(post("/rest/lab-units-management").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void createLabUnit_AdminRole_MissingFallbackName_Returns422() throws Exception {
        // empty body clears auth but fails validation (fallback-locale name required)
        mockMvc.perform(post("/rest/lab-units-management").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void assignableTests_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management/1/assignable-tests").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void assignableTests_AdminRole_PassesAuth() throws Exception {
        // mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(get("/rest/lab-units-management/1/assignable-tests").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void bulkAssign_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(post("/rest/lab-units-management/1/tests/assign").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/rest/lab-units-management/1/tests/reassign").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void bulkAssign_AdminRole_EmptyBody_Returns422() throws Exception {
        // empty body clears auth but fails validation (testIds required)
        mockMvc.perform(post("/rest/lab-units-management/1/tests/assign").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/rest/lab-units-management/1/tests/reassign").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void bulkAssign_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(
                post("/rest/lab-units-management/1/tests/assign").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // OGC-189 (QA LU-W-3): the 20-character name cap was enforced on create but
    // not on update — a 32-character name saved and persisted. test_section.NAME
    // is VARCHAR(20), but a rename only writes localization_value.value (text,
    // unbounded), so nothing downstream throws: the cap is a product rule the
    // update path simply skipped. These pin it on both verbs.

    /** Stubs the lookup so the request gets past the 404 to the validation. */
    private void stubExistingSection() {
        TestSection section = new TestSection();
        section.setId("1");
        section.setTestSectionName("Biochemistry");
        // An existing localization keeps the rename on the update branch;
        // without one the controller inserts a fresh Localization, which needs
        // a Spring context this slice does not have.
        Localization localization = new Localization();
        localization.setId("1");
        localization.setDescription("test unit name");
        section.setLocalization(localization);
        Mockito.when(testSectionService.getTestSectionById("1")).thenReturn(section);
        SupportedLocale english = new SupportedLocale();
        english.setLocaleCode("en");
        Mockito.when(supportedLocaleService.getAllActive()).thenReturn(Collections.singletonList(english));
        Mockito.when(supportedLocaleService.getFallbackLocaleCode()).thenReturn("en");
    }

    private static String nameOfLength(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("a");
        }
        return sb.toString();
    }

    @Test
    public void updateLabUnit_AdminRole_NameOver20Chars_Returns422() throws Exception {
        stubExistingSection();
        // 32 chars — the exact length QA saved and read back on testing.
        String body = "{\"names\":{\"en\":\"" + nameOfLength(32) + "\"}}";
        mockMvc.perform(put("/rest/lab-units-management/1").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnprocessableEntity());
        // The rename must be rejected before it reaches the store, not merely
        // reported as invalid afterwards.
        Mockito.verify(localizationService, Mockito.never()).update(Mockito.any());
    }

    @Test
    public void updateLabUnit_AdminRole_NameAtExactly20Chars_IsAccepted() throws Exception {
        stubExistingSection();
        // Boundary guard: the cap is inclusive, so 20 must NOT be rejected —
        // without this the fix could pass by rejecting every rename.
        //
        // Asserted as "not 422" rather than 200: this slice has no Spring
        // context, so getSysUserId falls through to UserContextHolder and the
        // handler 500s before the rename write, whether or not validation
        // passed. Clearing the validation gate is all this slice can observe;
        // the persisted-name behaviour is covered by QA LU-W-3 end to end.
        String body = "{\"names\":{\"en\":\"" + nameOfLength(20) + "\"}}";
        int statusCode = mockMvc.perform(put("/rest/lab-units-management/1").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
        Assert.assertNotEquals("a name of exactly 20 characters is within the cap and must not be rejected", 422,
                statusCode);
    }

    @Test
    public void updateLabUnit_AdminRole_NonFallbackLocaleNameOver20Chars_Returns422() throws Exception {
        stubExistingSection();
        SupportedLocale english = new SupportedLocale();
        english.setLocaleCode("en");
        SupportedLocale french = new SupportedLocale();
        french.setLocaleCode("fr");
        Mockito.when(supportedLocaleService.getAllActive()).thenReturn(java.util.Arrays.asList(english, french));
        // Every locale's name lands in the same 20-char product rule, not just
        // the fallback one — create only ever checked the fallback.
        String body = "{\"names\":{\"en\":\"Biochemistry\",\"fr\":\"" + nameOfLength(32) + "\"}}";
        mockMvc.perform(put("/rest/lab-units-management/1").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnprocessableEntity());
        Mockito.verify(localizationService, Mockito.never()).update(Mockito.any());
    }

    // OGC-189 (M3): the guarded deactivation flow. The ADMIN gate applies as
    // everywhere else, and the typed confirmation is validated before anything
    // is written.

    @Test
    public void deactivationImpact_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management/1/deactivation-impact").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void deactivationImpact_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management/1/deactivation-impact").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deactivate_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(post("/rest/lab-units-management/1/deactivate").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\":\"keep\",\"confirmation\":\"DEACTIVATE\"}")).andExpect(status().isForbidden());
    }

    @Test
    public void deactivate_AdminRole_WithoutTypedConfirmation_Returns422() throws Exception {
        stubExistingSection();
        // The typed confirmation is the guard against an accidental switch-off,
        // so a missing or wrong value must be refused before anything is
        // written.
        mockMvc.perform(post("/rest/lab-units-management/1/deactivate").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"option\":\"keep\"}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/rest/lab-units-management/1/deactivate").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\":\"keep\",\"confirmation\":\"deactivate\"}"))
                .andExpect(status().isUnprocessableEntity());
        Mockito.verify(testSectionService, Mockito.never()).update(Mockito.any(TestSection.class));
    }

    @Test
    public void deactivate_AdminRole_UnknownOption_Returns422() throws Exception {
        stubExistingSection();
        mockMvc.perform(post("/rest/lab-units-management/1/deactivate").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\":\"delete_everything\",\"confirmation\":\"DEACTIVATE\"}"))
                .andExpect(status().isUnprocessableEntity());
        Mockito.verify(testSectionService, Mockito.never()).update(Mockito.any(TestSection.class));
    }

    @Test
    public void deactivate_AdminRole_ReassignWithoutDestination_Returns422() throws Exception {
        stubExistingSection();
        // Reassign with nowhere to reassign to would otherwise deactivate the
        // unit and silently leave every test behind.
        mockMvc.perform(post("/rest/lab-units-management/1/deactivate").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"option\":\"reassign\",\"confirmation\":\"DEACTIVATE\"}"))
                .andExpect(status().isUnprocessableEntity());
        Mockito.verify(testSectionService, Mockito.never()).update(Mockito.any(TestSection.class));
    }

    @Configuration
    @EnableWebMvc
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        org.springframework.security.web.SecurityFilterChain securityFilterChain(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        TestSectionService testSectionService() {
            return mock(TestSectionService.class);
        }

        @Bean
        LocalizationService localizationService() {
            return mock(LocalizationService.class);
        }

        @Bean
        SupportedLocaleService supportedLocaleService() {
            return mock(SupportedLocaleService.class);
        }

        @Bean
        TestService testService() {
            return mock(TestService.class);
        }

        @Bean
        TestSectionCreateService testSectionCreateService() {
            return mock(TestSectionCreateService.class);
        }

        @Bean
        TestSectionTestAssignService testSectionTestAssignService() {
            return mock(TestSectionTestAssignService.class);
        }

        @Bean
        RoleService roleService() {
            return mock(RoleService.class);
        }

        @Bean
        AnalysisService analysisService() {
            return mock(AnalysisService.class);
        }

        @Bean
        LabUnitManagementRestController labUnitManagementRestController(TestSectionService testSectionService,
                LocalizationService localizationService, SupportedLocaleService supportedLocaleService,
                TestService testService, TestSectionCreateService testSectionCreateService,
                TestSectionTestAssignService testSectionTestAssignService, RoleService roleService,
                AnalysisService analysisService) {
            LabUnitManagementRestController controller = new LabUnitManagementRestController();
            ReflectionTestUtils.setField(controller, "testSectionService", testSectionService);
            ReflectionTestUtils.setField(controller, "localizationService", localizationService);
            ReflectionTestUtils.setField(controller, "supportedLocaleService", supportedLocaleService);
            ReflectionTestUtils.setField(controller, "testService", testService);
            ReflectionTestUtils.setField(controller, "testSectionCreateService", testSectionCreateService);
            ReflectionTestUtils.setField(controller, "testSectionTestAssignService", testSectionTestAssignService);
            ReflectionTestUtils.setField(controller, "roleService", roleService);
            ReflectionTestUtils.setField(controller, "analysisService", analysisService);
            return controller;
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
        }

        @Bean
        PageBuilderService pageBuilderService() {
            return mock(PageBuilderService.class);
        }
    }
}
