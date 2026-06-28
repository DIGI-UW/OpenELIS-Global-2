package org.openelisglobal.compliance.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.compliance.service.ComplianceEvaluationService;
import org.openelisglobal.compliance.service.ComplianceReportGenerationService;
import org.openelisglobal.compliance.service.LhuAmendmentService;
import org.openelisglobal.esig.service.ElectronicSignatureService;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sample.dao.SampleComplianceStandardDAO;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Auth-ordering tests for POST /rest/complianceReport/reissue (OGC-776 T-301).
 *
 * Constitution V.6 invariant: 403 must fire BEFORE body validation (400). A
 * caller without ROLE_RESULTS / ROLE_SUPERVISOR / ADMIN must be rejected at the
 * security layer, regardless of whether the request body is valid.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { ComplianceReportReissueSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ComplianceReportReissueSecurityTest extends SecuritySliceMockMvcTest {

    private static final String VALID_BODY = "{\"sampleId\":1,\"reason\":\"Incorrect result\"}";
    private static final String BLANK_REASON_BODY = "{\"sampleId\":1,\"reason\":\"\"}";

    /**
     * Unauthenticated caller → 401 (Spring Security blocks before the controller).
     */
    @Test
    public void reissue_unauthenticated_returns401() throws Exception {
        mockMvc.perform(
                post("/rest/complianceReport/reissue").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Authenticated but wrong role → 403. Body is valid, so if auth fires AFTER
     * body validation this would return 400 instead — catching ordering violations.
     */
    @Test
    public void reissue_wrongRole_returns403BeforeBodyValidation() throws Exception {
        mockMvc.perform(post("/rest/complianceReport/reissue").with(user("readonly").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)).andExpect(status().isForbidden());
    }

    /**
     * Blank reason with wrong role → must still get 403, not 400. Proves auth
     * ordering is enforced even when body would fail validation.
     */
    @Test
    public void reissue_wrongRoleBlankReason_returns403NotBadRequest() throws Exception {
        mockMvc.perform(post("/rest/complianceReport/reissue").with(user("readonly").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content(BLANK_REASON_BODY)).andExpect(status().isForbidden());
    }

    /**
     * ROLE_RESULTS holder passes auth (gets past the 403 gate). The request body is
     * valid but sampleId=1 has no released analysis in this slice context, so we
     * expect 404 — confirming auth did NOT block.
     */
    @Test
    public void reissue_roleResults_passesSecurity() throws Exception {
        mockMvc.perform(post("/rest/complianceReport/reissue").with(user("analyst").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
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
        ComplianceReportRestController complianceReportRestController() {
            ComplianceReportRestController controller = new ComplianceReportRestController();
            ReflectionTestUtils.setField(controller, "sampleService", mock(SampleService.class));
            ReflectionTestUtils.setField(controller, "sampleItemService", mock(SampleItemService.class));
            ReflectionTestUtils.setField(controller, "sampleComplianceStandardDAO",
                    mock(SampleComplianceStandardDAO.class));
            ReflectionTestUtils.setField(controller, "complianceEvaluationService",
                    mock(ComplianceEvaluationService.class));
            ReflectionTestUtils.setField(controller, "reportGenerationService",
                    mock(ComplianceReportGenerationService.class));
            ReflectionTestUtils.setField(controller, "electronicSignatureService",
                    mock(ElectronicSignatureService.class));
            ReflectionTestUtils.setField(controller, "observationHistoryService",
                    mock(ObservationHistoryService.class));
            ReflectionTestUtils.setField(controller, "vectorSamplingSiteService",
                    mock(VectorSamplingSiteService.class));
            ReflectionTestUtils.setField(controller, "resultService", mock(ResultService.class));
            ReflectionTestUtils.setField(controller, "analysisService", mock(AnalysisService.class));

            LhuAmendmentService lhuAmendmentService = mock(LhuAmendmentService.class);
            when(lhuAmendmentService.hasBeenReleased(anyLong())).thenReturn(false);
            when(lhuAmendmentService.certificateNumberWithAmendmentSuffix(any(), any())).thenReturn("24-00001");
            ReflectionTestUtils.setField(controller, "lhuAmendmentService", lhuAmendmentService);
            return controller;
        }
    }
}
