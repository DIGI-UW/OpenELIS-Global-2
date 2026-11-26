package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.containertype.service.ContainerTypeService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patienttype.service.PatientPatientTypeService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.qachecklist.service.SampleQaChecklistService;
import org.openelisglobal.qc.dao.SampleItemQcProfileDAO;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.sample.override.service.SampleOrderOverrideService;
import org.openelisglobal.sample.service.OrderProgressService;
import org.openelisglobal.sample.service.SampleComplianceStandardService;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderProgressStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { OrderSearchRestControllerSecurityTest.TestConfig.class })
public class OrderSearchRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static MockHttpSession sessionForSysUser(int sysUserId) {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(sysUserId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return session;
    }

    // --- getDashboard (GET /rest/order/dashboard) ---

    @Test
    public void getDashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/order/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getDashboard_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/order/dashboard").with(user("res").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getDashboard_receptionRole_passesSecurity() throws Exception {
        assertPassesSecurity(
                get("/rest/order/dashboard").session(sessionForSysUser(1)).with(user("rec").roles("RECEPTION")));
    }

    @Test
    public void getDashboard_adminRole_passesSecurity() throws Exception {
        assertPassesSecurity(
                get("/rest/order/dashboard").session(sessionForSysUser(1)).with(user("adm").roles("ADMIN")));
    }

    // --- searchOrder (GET /rest/order/search) ---

    @Test
    public void searchOrder_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/order/search").param("labNumber", "ACC123")).andExpect(status().isUnauthorized());
    }

    @Test
    public void searchOrder_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/order/search").param("labNumber", "ACC123").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void searchOrder_receptionRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/order/search").param("labNumber", "ACC123").session(sessionForSysUser(1))
                .with(user("rec").roles("RECEPTION")));
    }

    @Test
    public void searchOrder_resultsRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/order/search").param("labNumber", "ACC123").session(sessionForSysUser(1))
                .with(user("res").roles("RESULTS")));
    }

    @Test
    public void searchOrder_validationRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/order/search").param("labNumber", "ACC123").session(sessionForSysUser(1))
                .with(user("val").roles("VALIDATION")));
    }

    @Test
    public void searchOrder_adminRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/order/search").param("labNumber", "ACC123").session(sessionForSysUser(1))
                .with(user("adm").roles("ADMIN")));
    }

    // --- cancelOrder (POST /rest/order/cancel) ---

    @Test
    public void cancelOrder_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/rest/order/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"labNumber\":\"ACC123\",\"reason\":\"test\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void cancelOrder_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(post("/rest/order/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"labNumber\":\"ACC123\",\"reason\":\"test\"}").with(user("res").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void cancelOrder_receptionRole_passesSecurity() throws Exception {
        assertPassesSecurity(post("/rest/order/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"labNumber\":\"ACC123\",\"reason\":\"test\"}").session(sessionForSysUser(1))
                .with(user("rec").roles("RECEPTION")));
    }

    @Test
    public void cancelOrder_adminRole_passesSecurity() throws Exception {
        assertPassesSecurity(post("/rest/order/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"labNumber\":\"ACC123\",\"reason\":\"test\"}").session(sessionForSysUser(1))
                .with(user("adm").roles("ADMIN")));
    }

    // --- updateStorageSkipped (PUT /rest/order/storage-skipped) ---

    @Test
    public void updateStorageSkipped_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/rest/order/storage-skipped").param("labNumber", "ACC123").param("storageSkipped", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateStorageSkipped_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(put("/rest/order/storage-skipped").param("labNumber", "ACC123").param("storageSkipped", "true")
                .with(user("res").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void updateStorageSkipped_receptionRole_passesSecurity() throws Exception {
        assertPassesSecurity(put("/rest/order/storage-skipped").param("labNumber", "ACC123")
                .param("storageSkipped", "true").session(sessionForSysUser(1)).with(user("rec").roles("RECEPTION")));
    }

    @Test
    public void updateStorageSkipped_adminRole_passesSecurity() throws Exception {
        assertPassesSecurity(put("/rest/order/storage-skipped").param("labNumber", "ACC123")
                .param("storageSkipped", "true").session(sessionForSysUser(1)).with(user("adm").roles("ADMIN")));
    }

    /**
     * The handler may fail on what the slice does not mock; getting that far still
     * means the request passed the role check.
     */
    private void assertPassesSecurity(MockHttpServletRequestBuilder request) throws Exception {
        try {
            int status = mockMvc.perform(request).andReturn().getResponse().getStatus();
            assertNotEquals(401, status);
            assertNotEquals(403, status);
        } catch (ServletException e) {
            assertFalse(e.getCause() instanceof AccessDeniedException);
        }
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

        @Bean
        ContainerTypeService containerTypeService() {
            return mock(ContainerTypeService.class);
        }

        @Bean
        FhirTransformService fhirTransformService() {
            return mock(FhirTransformService.class);
        }

        @Bean
        SampleService sampleService() {
            SampleService service = mock(SampleService.class);
            Sample sample = new Sample();
            sample.setId("1");
            sample.setAccessionNumber("ACC123");
            sample.setOrderProgressStatus("ENTERED");
            when(service.get(anyString())).thenReturn(sample);
            when(service.getSampleByAccessionNumber("ACC123")).thenReturn(sample);
            when(service.getSamplesNewestFirst(anyInt(), anyInt())).thenReturn(Collections.emptyList());
            return service;
        }

        @Bean
        SampleItemService sampleItemService() {
            SampleItemService service = mock(SampleItemService.class);
            when(service.getSampleItemsBySampleId(anyString())).thenReturn(Collections.emptyList());
            return service;
        }

        @Bean
        SampleHumanService sampleHumanService() {
            SampleHumanService service = mock(SampleHumanService.class);
            when(service.getPatientForSample(any())).thenReturn(null);
            when(service.getProviderForSample(any())).thenReturn(null);
            return service;
        }

        @Bean
        SystemUserService systemUserService() {
            return mock(SystemUserService.class);
        }

        @Bean
        PatientService patientService() {
            return mock(PatientService.class);
        }

        @Bean
        TypeOfSampleService typeOfSampleService() {
            return mock(TypeOfSampleService.class);
        }

        @Bean
        AddressPartService addressPartService() {
            AddressPartService service = mock(AddressPartService.class);
            when(service.getAll()).thenReturn(Collections.emptyList());
            return service;
        }

        @Bean
        PersonAddressService personAddressService() {
            return mock(PersonAddressService.class);
        }

        @Bean
        PatientPatientTypeService patientPatientTypeService() {
            return mock(PatientPatientTypeService.class);
        }

        @Bean
        PatientContactService patientContactService() {
            return mock(PatientContactService.class);
        }

        @Bean
        AnalysisService analysisService() {
            AnalysisService service = mock(AnalysisService.class);
            when(service.getAnalysesBySampleItem(any())).thenReturn(Collections.emptyList());
            return service;
        }

        @Bean
        VectorPoolService vectorPoolService() {
            VectorPoolService service = mock(VectorPoolService.class);
            when(service.getBySampleId(anyString())).thenReturn(Collections.emptyList());
            return service;
        }

        @Bean
        ObservationHistoryService observationHistoryService() {
            ObservationHistoryService service = mock(ObservationHistoryService.class);
            when(service.getObservationHistoriesByType(any())).thenReturn(Collections.emptyList());
            when(service.getRawValueForSample(any(), anyString())).thenReturn(null);
            return service;
        }

        @Bean
        ProgramSampleService programSampleService() {
            return mock(ProgramSampleService.class);
        }

        @Bean
        ProgramService programService() {
            return mock(ProgramService.class);
        }

        @Bean
        PersonService personService() {
            return mock(PersonService.class);
        }

        @Bean
        OrganizationService organizationService() {
            return mock(OrganizationService.class);
        }

        @Bean
        QuestionnaireStorageService questionnaireStorageService() {
            return mock(QuestionnaireStorageService.class);
        }

        @Bean
        SampleStorageAssignmentDAO sampleStorageAssignmentDAO() {
            return mock(SampleStorageAssignmentDAO.class);
        }

        @Bean
        SampleStorageService sampleStorageService() {
            return mock(SampleStorageService.class);
        }

        @Bean
        SampleQaChecklistService sampleQaChecklistService() {
            SampleQaChecklistService service = mock(SampleQaChecklistService.class);
            when(service.findBySampleId(anyInt())).thenReturn(null);
            return service;
        }

        @Bean
        OrderProgressService orderProgressService() {
            OrderProgressService service = mock(OrderProgressService.class);
            Sample sample = new Sample();
            sample.setId("1");
            sample.setAccessionNumber("ACC123");
            sample.setOrderProgressStatus("CANCELLED");
            when(service.statusOf(any(), anyBoolean(), anyBoolean())).thenReturn(OrderProgressStatus.ENTERED);
            when(service.isComplete(any(), any(), anyBoolean())).thenReturn(false);
            when(service.isFullyReferred(anyString())).thenReturn(false);
            when(service.sampleCheckEnabled(anyString())).thenReturn(true);
            when(service.cancel(anyString(), any(), any())).thenReturn(sample);
            return service;
        }

        @Bean
        SampleItemQcProfileDAO sampleItemQcProfileDAO() {
            SampleItemQcProfileDAO dao = mock(SampleItemQcProfileDAO.class);
            when(dao.findBySampleItemId(anyInt())).thenReturn(Optional.empty());
            return dao;
        }

        @Bean
        SampleComplianceStandardService sampleComplianceStandardService() {
            SampleComplianceStandardService service = mock(SampleComplianceStandardService.class);
            when(service.getAllForSample(anyString())).thenReturn(Collections.emptyList());
            return service;
        }

        @Bean
        ReferralService referralService() {
            return mock(ReferralService.class);
        }

        @Bean
        IStatusService statusService() {
            return mock(IStatusService.class);
        }

        @Bean
        PanelItemService panelItemService() {
            return mock(PanelItemService.class);
        }

        @Bean
        PanelService panelService() {
            return mock(PanelService.class);
        }

        @Bean
        TestSectionService testSectionService() {
            return mock(TestSectionService.class);
        }

        @Bean
        VectorSamplingSiteService vectorSamplingSiteService() {
            return mock(VectorSamplingSiteService.class);
        }

        @Bean
        SampleLabUnitScopeService sampleLabUnitScopeService() {
            SampleLabUnitScopeService service = mock(SampleLabUnitScopeService.class);
            when(service.restrictedSectionIds(any())).thenReturn(Collections.emptySet());
            when(service.isVisible(any(), any())).thenReturn(true);
            return service;
        }

        @Bean
        TestMethodService testMethodService() {
            return mock(TestMethodService.class);
        }

        @Bean
        SampleEQAService sampleEQAService() {
            SampleEQAService service = mock(SampleEQAService.class);
            when(service.findBySampleId(anyLong())).thenReturn(Optional.empty());
            return service;
        }

        @Bean
        SampleOrderOverrideService sampleOrderOverrideService() {
            SampleOrderOverrideService service = mock(SampleOrderOverrideService.class);
            when(service.findBySampleIdAndType(anyLong(), any())).thenReturn(Optional.empty());
            return service;
        }

        @Bean
        OrderSearchRestController orderSearchRestController() {
            return new OrderSearchRestController();
        }
    }
}
