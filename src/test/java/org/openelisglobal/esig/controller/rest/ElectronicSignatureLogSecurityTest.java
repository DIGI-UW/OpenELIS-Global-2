package org.openelisglobal.esig.controller.rest;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.esig.service.ElectronicSignatureService;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.SignatureMeaning;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Verifies the /rest/esig/log gate is the qa.view.qms permission authority (QA
 * permission model): a role name alone does not grant access, the derived
 * authority does, and the GLOBAL_ADMIN fallback still works. Also covers the
 * endpoint's parameter validation once past the gate.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { ElectronicSignatureLogSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ElectronicSignatureLogSecurityTest extends SecuritySliceMockMvcTest {

    private static final String LOG_URL = "/rest/esig/log?fromDate=2026-06-01&toDate=2026-07-01";
    private static final String CSV_URL = "/rest/esig/log/export?fromDate=2026-06-01&toDate=2026-07-01";
    private static final String PDF_URL = "/rest/esig/log/exportPdf?fromDate=2026-06-01&toDate=2026-07-01";

    /**
     * Content tests re-stub the shared mock; restore the empty defaults before
     * every test so assertions stay order-independent.
     */
    @Before
    public void resetServiceStub() {
        ElectronicSignatureService service = webApplicationContext.getBean(ElectronicSignatureService.class);
        reset(service);
        when(service.searchSignatures(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        when(service.countSearchSignatures(any(), any(), any(), any(), any())).thenReturn(0L);
    }

    @Test
    public void log_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(LOG_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void log_roleWithoutPermissionAuthorityReturns403() throws Exception {
        mockMvc.perform(get(LOG_URL).with(user("validator").roles("VALIDATION"))).andExpect(status().isForbidden());
    }

    @Test
    public void log_qaViewQmsAuthorityReturns200() throws Exception {
        mockMvc.perform(get(LOG_URL).with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    public void log_globalAdminRoleFallbackReturns200() throws Exception {
        mockMvc.perform(get(LOG_URL).with(user("admin").roles("GLOBAL_ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void log_invalidMeaningReturns400() throws Exception {
        mockMvc.perform(get(LOG_URL + "&meaning=BOGUS")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void log_reversedDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/rest/esig/log?fromDate=2026-07-01&toDate=2026-06-01")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void log_rangeOverOneYearReturns400() throws Exception {
        mockMvc.perform(get("/rest/esig/log?fromDate=2024-01-01&toDate=2026-07-01")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isBadRequest());
    }

    // ========================
    // Export endpoints (OGC-703)
    // ========================

    @Test
    public void exportCsv_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(CSV_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void exportCsv_roleWithoutPermissionAuthorityReturns403() throws Exception {
        mockMvc.perform(get(CSV_URL).with(user("validator").roles("VALIDATION"))).andExpect(status().isForbidden());
    }

    @Test
    public void exportCsv_reversedDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/rest/esig/log/export?fromDate=2026-07-01&toDate=2026-06-01")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void exportCsv_containsHeaderRowAndEscapedData() throws Exception {
        stubSignatures();

        String csv = mockMvc
                .perform(get(CSV_URL).with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"e-signature-log.csv\""))
                .andReturn().getResponse().getContentAsString();

        String[] lines = csv.strip().split("\\R");
        assertTrue("Header row should lead the file", lines[0].equals("Signed At,Signer,Action,Subject,Reason"));
        assertTrue("Should have exactly header + 2 data rows, got " + lines.length, lines.length == 3);
        assertTrue("First row should carry the rejection with quoted comma reason",
                lines[1].contains("Jane Supervisor") && lines[1].contains("Rejected")
                        && lines[1].contains("QC_RESULT #42") && lines[1].contains("\"Hemolyzed, recollect\""));
        assertTrue("Second row should guard the formula-injection reason", lines[2].contains("John Tech")
                && lines[2].contains("Validated & Released") && lines[2].contains("'=cmd"));
    }

    @Test
    public void exportPdf_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(PDF_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void exportPdf_roleWithoutPermissionAuthorityReturns403() throws Exception {
        mockMvc.perform(get(PDF_URL).with(user("validator").roles("VALIDATION"))).andExpect(status().isForbidden());
    }

    @Test
    public void exportPdf_returnsPdfDocument() throws Exception {
        stubSignatures();

        byte[] pdf = mockMvc
                .perform(get(PDF_URL).with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.qms"))))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"e-signature-log.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue("PDF body should not be empty", pdf.length > 0);
        assertTrue("Body should start with the PDF magic bytes", new String(pdf, 0, 5).equals("%PDF-"));
    }

    private void stubSignatures() {
        ElectronicSignature rejected = new ElectronicSignature();
        rejected.setSignedAt(Timestamp.valueOf("2026-06-15 10:30:00"));
        rejected.setSignerNamePrinted("Jane Supervisor");
        rejected.setSignatureMeaning(SignatureMeaning.REJECTED);
        rejected.setRecordType("QC_RESULT");
        rejected.setRecordId(Long.valueOf(42L));
        rejected.setRejectionReason("Hemolyzed, recollect");

        ElectronicSignature validated = new ElectronicSignature();
        validated.setSignedAt(Timestamp.valueOf("2026-06-14 09:00:00"));
        validated.setSignerNamePrinted("John Tech");
        validated.setSignatureMeaning(SignatureMeaning.VALIDATED_AND_RELEASED);
        validated.setRecordType("VALIDATION_BATCH");
        validated.setRecordId(Long.valueOf(7L));
        validated.setRejectionReason("=cmd");

        ElectronicSignatureService service = webApplicationContext.getBean(ElectronicSignatureService.class);
        when(service.searchSignatures(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(rejected, validated));
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
        ElectronicSignatureService electronicSignatureService() {
            // The slice exercises the @PreAuthorize gate and request
            // validation, not the query.
            ElectronicSignatureService service = mock(ElectronicSignatureService.class);
            when(service.searchSignatures(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of());
            when(service.countSearchSignatures(any(), any(), any(), any(), any())).thenReturn(0L);
            return service;
        }

        @Bean
        ElectronicSignatureRestController electronicSignatureRestController(ElectronicSignatureService service) {
            ElectronicSignatureRestController controller = new ElectronicSignatureRestController();
            ReflectionTestUtils.setField(controller, "electronicSignatureService", service);
            return controller;
        }

        @Bean
        ControllerSetup controllerSetup() {
            // Real @ControllerAdvice in the slice so @PreAuthorize denials
            // surface as 403s, not 500s (guards the advice ordering).
            return new ControllerSetup();
        }

        @Bean
        MessageSource messageSource() {
            // The export endpoints resolve headers/meanings through the static
            // MessageUtil; back it with the real bundle so content assertions
            // check actual strings.
            ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
            messageSource.setBasename("classpath:languages/message");
            messageSource.setDefaultEncoding("UTF-8");
            MessageUtil.setMessageSource(messageSource);
            return messageSource;
        }

        @Bean
        DefaultConfigurationProperties defaultConfigurationProperties() {
            // Satisfies the controller's injected ConfigurationProperties for
            // the PDF lab-name lookup. Deliberately NOT registering a
            // SpringContext bean here: its ApplicationContextAware callback
            // overwrites the static holder for every later test in the JVM.
            return mock(DefaultConfigurationProperties.class);
        }

        // The DefaultConfigurationProperties mock still receives superclass
        // @Autowired field injection — satisfy it with inert mocks.
        @Bean
        org.openelisglobal.siteinformation.service.SiteInformationService siteInformationService() {
            return mock(org.openelisglobal.siteinformation.service.SiteInformationService.class);
        }

        @Bean
        org.openelisglobal.externalconnections.service.BasicAuthenticationDataService basicAuthenticationDataService() {
            return mock(org.openelisglobal.externalconnections.service.BasicAuthenticationDataService.class);
        }

        @Bean
        org.openelisglobal.externalconnections.service.ExternalConnectionService externalConnectionService() {
            return mock(org.openelisglobal.externalconnections.service.ExternalConnectionService.class);
        }
    }
}
