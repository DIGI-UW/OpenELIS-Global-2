package org.openelisglobal.qc.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qc.service.QCChartDataService;
import org.openelisglobal.qc.service.QCChartDataService.LotSection;
import org.openelisglobal.qc.service.QCChartDataService.QCExportModel;
import org.openelisglobal.qc.service.SigmaMetrics;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.security.DaemonContextExecutor;
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
 * Verifies the OGC-706 QC export endpoints: the {@code qa.view.qc} gate (a role
 * alone is not enough; the derived authority or the GLOBAL_ADMIN fallback is),
 * request validation, and the rendered CSV/PDF content — the Unicode rule
 * codes, formula-injection guard, and the honest truncation notice.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { QCExportRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class QCExportRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String CSV_URL = "/rest/qc/export/csv?instrumentId=1&startDate=2026-06-01&endDate=2026-06-30";
    private static final String PDF_URL = "/rest/qc/export/pdf?instrumentId=1&startDate=2026-06-01&endDate=2026-06-30";
    // U+2083 U+209B = the "1₃ₛ" Westgard rule code (subscript 3, subscript s)
    private static final String RULE_1_3S = "1₃ₛ";

    @Before
    public void resetStub() {
        QCChartDataService service = webApplicationContext.getBean(QCChartDataService.class);
        org.mockito.Mockito.reset(service);
        when(service.getExportModel(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(new QCExportModel("Cobas 6000", List.of(), 0, 0, false));
    }

    // ==================== gate ====================

    @Test
    public void csv_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(CSV_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void csv_roleWithoutPermissionAuthorityReturns403() throws Exception {
        mockMvc.perform(get(CSV_URL).with(user("tech").roles("RECEPTION"))).andExpect(status().isForbidden());
    }

    @Test
    public void csv_qaViewQcAuthorityReturns200() throws Exception {
        mockMvc.perform(get(CSV_URL).with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isOk());
    }

    @Test
    public void csv_globalAdminFallbackReturns200() throws Exception {
        mockMvc.perform(get(CSV_URL).with(user("admin").roles("GLOBAL_ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void pdf_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get(PDF_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    public void pdf_roleWithoutPermissionAuthorityReturns403() throws Exception {
        mockMvc.perform(get(PDF_URL).with(user("tech").roles("RECEPTION"))).andExpect(status().isForbidden());
    }

    // ==================== validation ====================

    @Test
    public void csv_reversedDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/rest/qc/export/csv?instrumentId=1&startDate=2026-06-30&endDate=2026-06-01")
                .with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void csv_rangeOverOneYearReturns400() throws Exception {
        mockMvc.perform(get("/rest/qc/export/csv?instrumentId=1&startDate=2024-01-01&endDate=2026-06-01")
                .with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isBadRequest());
    }

    // ==================== CSV content ====================

    @Test
    public void csv_hasBomHeaderRowAndEscapedUnicodeData() throws Exception {
        stubModel(false);

        String csv = mockMvc
                .perform(get(CSV_URL).with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"qc-Cobas-6000-2026-06-01_2026-06-30.csv\""))
                .andReturn().getResponse().getContentAsString();

        assertTrue("UTF-8 BOM must lead the file so Excel renders subscript rule codes", csv.startsWith("﻿"));
        String[] lines = csv.split("\\R");
        assertTrue("header carries the OGC-706 columns", lines[0].contains("Instrument")
                && lines[0].contains("Violated Rules") && lines[0].contains("Severity"));
        // 2 data rows for the 2 results (BOM+header line is lines[0])
        assertTrue("comma-bearing test name is quoted", csv.contains("\"Glucose, serum\""));
        assertTrue("Cobas 6000 appears on the data rows", csv.contains("Cobas 6000"));
        assertTrue("the rejection row carries the Unicode rule code and REJECTION severity",
                csv.contains(RULE_1_3S) && csv.contains("REJECTION"));
        assertTrue("formula-injection unit is neutralised with a leading apostrophe", csv.contains("'=danger"));
        assertTrue("negative z-score stays a raw number (no formula-guard apostrophe) so Excel types it numerically",
                csv.contains("-2.5") && !csv.contains("'-2.5"));
    }

    @Test
    public void csv_appendsTruncationNoticeWhenCapped() throws Exception {
        stubModel(true);

        String csv = mockMvc
                .perform(get(CSV_URL).with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertTrue("a capped export must surface a truncation notice, never drop rows silently",
                csv.contains("Export truncated at the maximum row limit"));
    }

    // ==================== PDF content ====================

    @Test
    public void pdf_rendersReportWithInstrumentAndSigma() throws Exception {
        stubModel(false);

        byte[] pdf = mockMvc
                .perform(get(PDF_URL).with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"qc-Cobas-6000-2026-06-01_2026-06-30.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue("PDF body should not be empty", pdf.length > 0);
        assertEquals("Body should start with the PDF magic bytes", "%PDF-", new String(pdf, 0, 5));

        PdfReader reader = new PdfReader(pdf);
        String text = PdfTextExtractor.getTextFromPage(reader, 1);
        reader.close();
        assertTrue("report title present", text.contains("Quality Control Inspector Report"));
        assertTrue("instrument name present", text.contains("Cobas 6000"));
        assertTrue("sigma interpretation present", text.contains("ACCEPTABLE"));
        // CV is computed from mean/SD (8/200*100 = 4.00), independent of the sigma
        // result's own cv — proves it's no longer blanked when TEa/sigma is missing.
        assertTrue("CV computed from mean/SD (4.00) shown in the report", text.contains("4.00"));
        // The Unicode rule code 1₃ₛ is NFKD-folded to ASCII so iText (no subscript
        // glyphs) renders it legibly rather than stripping it to "1".
        assertTrue("rule code rendered as folded ASCII (13s)", text.contains("13s"));
    }

    private void stubModel(boolean truncated) {
        QCControlLot lot = new QCControlLot();
        lot.setControlLevel("NORMAL");
        lot.setLotNumber("LOT-A");
        lot.setTestId("412");

        QCResult r1 = new QCResult();
        r1.setId("r1");
        r1.setResultValue(new BigDecimal("100.0"));
        r1.setZScore(new BigDecimal("-2.5"));
        r1.setUnitOfMeasure("mg/dL");
        r1.setResultStatus("ACCEPTED");
        r1.setNonConformityFlag(Boolean.FALSE);
        r1.setRunDateTime(Timestamp.valueOf("2026-06-15 09:00:00"));

        QCResult r2 = new QCResult();
        r2.setId("r2");
        r2.setResultValue(new BigDecimal("108.0"));
        r2.setZScore(new BigDecimal("3.6"));
        r2.setUnitOfMeasure("=danger"); // formula-injection probe
        r2.setResultStatus("ACCEPTED");
        r2.setNonConformityFlag(Boolean.TRUE);
        r2.setRunDateTime(Timestamp.valueOf("2026-06-16 09:00:00"));

        QCRuleViolation violation = new QCRuleViolation();
        violation.setTriggeringResultId("r2");
        violation.setRuleCode(RULE_1_3S);
        violation.setSeverity("REJECTION");
        violation.setResolutionStatus("UNRESOLVED");
        violation.setViolationDateTime(Timestamp.valueOf("2026-06-16 09:00:00"));

        QCStatistics stats = new QCStatistics();
        stats.setMean(new BigDecimal("200.0"));
        stats.setStandardDeviation(new BigDecimal("8.0"));
        stats.setNumValues(30);
        stats.setCalculationMethod("ROLLING");
        stats.setCalculationDate(Timestamp.valueOf("2026-06-30 12:00:00"));

        LotSection section = new LotSection(lot, "Glucose, serum", List.of(r1, r2), List.of(violation), stats,
                new SigmaMetrics.SigmaResult(2.0, 5.0, SigmaMetrics.ACCEPTABLE));
        QCExportModel model = new QCExportModel("Cobas 6000", List.of(section), 2, 1, truncated);

        QCChartDataService service = webApplicationContext.getBean(QCChartDataService.class);
        when(service.getExportModel(any(), any(), any(), any(), any(), anyInt())).thenReturn(model);
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

        // The controller autowires ConfigurationProperties, whose
        // DefaultConfigurationProperties gained an @Autowired DaemonContextExecutor
        // in #3356 — so the slice must supply it (and its @Qualifier
        // "daemonSysUserId" dependency), as
        // ConfigurationReloadRestControllerSecurityTest does.
        @Bean("daemonSysUserId")
        String daemonSysUserId() {
            return "1";
        }

        @Bean
        DaemonContextExecutor daemonContextExecutor() {
            return new DaemonContextExecutor();
        }

        @Bean
        QCChartDataService qcChartDataService() {
            QCChartDataService service = mock(QCChartDataService.class);
            when(service.getExportModel(any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(new QCExportModel("Cobas 6000", List.of(), 0, 0, false));
            return service;
        }

        @Bean
        QCExportRestController qcExportRestController(QCChartDataService service) {
            QCExportRestController controller = new QCExportRestController();
            ReflectionTestUtils.setField(controller, "chartDataService", service);
            return controller;
        }

        @Bean
        ControllerSetup controllerSetup() {
            // Real @ControllerAdvice so @PreAuthorize denials surface as 403s, not 500s.
            return new ControllerSetup();
        }

        @Bean
        MessageSource messageSource() {
            ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
            messageSource.setBasename("classpath:languages/message");
            messageSource.setDefaultEncoding("UTF-8");
            MessageUtil.setMessageSource(messageSource);
            return messageSource;
        }

        @Bean
        DefaultConfigurationProperties defaultConfigurationProperties() {
            // Satisfies the controller's injected ConfigurationProperties for the PDF
            // lab-name lookup. Not registering a SpringContext bean here (its
            // ApplicationContextAware callback overwrites the static holder JVM-wide).
            return mock(DefaultConfigurationProperties.class);
        }

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
