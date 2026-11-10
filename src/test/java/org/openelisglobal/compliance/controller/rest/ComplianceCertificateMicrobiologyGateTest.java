package org.openelisglobal.compliance.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.compliance.service.ComplianceEvaluationService;
import org.openelisglobal.compliance.service.ComplianceReportArchiveService;
import org.openelisglobal.compliance.service.ComplianceReportGenerationService;
import org.openelisglobal.compliance.service.LhuAmendmentService;
import org.openelisglobal.esig.service.ElectronicSignatureService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sample.dao.SampleComplianceStandardDAO;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * OGC-1620: an environmental microbiology case is certified once its final
 * report is released. Until then the certificate is refused with 409 and a
 * reason, not a server error.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { ComplianceCertificateMicrobiologyGateTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ComplianceCertificateMicrobiologyGateTest extends SecuritySliceMockMvcTest {

    @Test
    public void aCertificateWaitsForTheMicrobiologyReportRelease() throws Exception {
        MvcResult refused = mockMvc.perform(post("/rest/complianceReport/exportPdf")
                .with(user("analyst").roles("RESULTS")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                .contentType(MediaType.APPLICATION_JSON).content("{\"sampleId\":1}")).andReturn();

        assertEquals(409, refused.getResponse().getStatus());
        assertEquals("The microbiology report must be released before the report is generated",
                refused.getResponse().getErrorMessage());
    }

    @Test
    public void aReissueWaitsForTheMicrobiologyReportRelease() throws Exception {
        MvcResult refused = mockMvc
                .perform(post("/rest/complianceReport/reissue").with(user("analyst").roles("RESULTS"))
                        .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sampleId\":1,\"reason\":\"Typo\"}"))
                .andReturn();

        assertEquals(409, refused.getResponse().getStatus());
        assertEquals("The microbiology report must be released before the report is reissued",
                refused.getResponse().getContentAsString());
    }

    private UserSessionData sessionUser() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(7);
        return usd;
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
            return new ComplianceReportRestController();
        }

        @Bean
        org.openelisglobal.microbiology.service.MicroEnvironmentalReportService microbiologyReports() {
            var reports = mock(org.openelisglobal.microbiology.service.MicroEnvironmentalReportService.class);
            when(reports.released(any(), any()))
                    .thenThrow(new IllegalStateException("MICROBIOLOGY_CERTIFICATE_RELEASE_REQUIRED"));
            return reports;
        }

        @Bean
        SampleService sampleService() {
            SampleService samples = mock(SampleService.class);
            Sample sample = new Sample();
            sample.setId("1");
            sample.setAccessionNumber("ENV-1");
            when(samples.get("1")).thenReturn(sample);
            return samples;
        }

        @Bean
        SampleItemService sampleItemService() {
            return mock(SampleItemService.class);
        }

        @Bean
        SampleComplianceStandardDAO sampleComplianceStandardDAO() {
            return mock(SampleComplianceStandardDAO.class);
        }

        @Bean
        ComplianceEvaluationService complianceEvaluationService() {
            ComplianceEvaluationService evaluation = mock(ComplianceEvaluationService.class);
            when(evaluation.orderedTestCount(any())).thenReturn(1);
            return evaluation;
        }

        @Bean
        ComplianceReportGenerationService reportGenerationService() {
            return mock(ComplianceReportGenerationService.class);
        }

        @Bean
        ElectronicSignatureService electronicSignatureService() {
            return mock(ElectronicSignatureService.class);
        }

        @Bean
        ObservationHistoryService observationHistoryService() {
            return mock(ObservationHistoryService.class);
        }

        @Bean
        VectorSamplingSiteService vectorSamplingSiteService() {
            return mock(VectorSamplingSiteService.class);
        }

        @Bean
        ResultService resultService() {
            return mock(ResultService.class);
        }

        @Bean
        AnalysisService analysisService() {
            return mock(AnalysisService.class);
        }

        @Bean
        ComplianceReportArchiveService archiveService() {
            return mock(ComplianceReportArchiveService.class);
        }

        @Bean
        LhuAmendmentService lhuAmendmentService() {
            LhuAmendmentService lhuAmendmentService = mock(LhuAmendmentService.class);
            when(lhuAmendmentService.hasBeenReleased(anyLong())).thenReturn(true);
            when(lhuAmendmentService.certificateNumberWithAmendmentSuffix(any(), any())).thenReturn("24-00001");
            return lhuAmendmentService;
        }
    }
}
