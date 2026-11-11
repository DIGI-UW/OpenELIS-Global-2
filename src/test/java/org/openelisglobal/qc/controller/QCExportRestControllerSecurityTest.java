package org.openelisglobal.qc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qc.report.QCBenchExportService;
import org.openelisglobal.qc.service.QCChartDataService;
import org.openelisglobal.qc.service.QCChartDataService.QCExportModel;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The gate and the request validation on the OGC-706 QC export endpoints: a
 * role alone is not enough for {@code qa.view.qc}, the derived authority or the
 * GLOBAL_ADMIN fallback is, and a window or source the endpoint cannot honour
 * is refused rather than silently answered with nothing. What the export
 * renders is pinned by {@code QCExportWriterTest} against the writer itself.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class, QCExportRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class QCExportRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String CSV_URL = "/rest/qc/export/csv?instrumentId=1&startDate=2026-06-01&endDate=2026-06-30";
    private static final String PDF_URL = "/rest/qc/export/pdf?instrumentId=1&startDate=2026-06-01&endDate=2026-06-30";
    // OGC-1147: the bench register takes no instrumentId — a manual or RDT control
    // has none.
    private static final String BENCH_URL = "/rest/qc/export/bench/csv?startDate=2026-06-01&endDate=2026-06-30";

    // ==================== gate ====================

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
    public void bench_roleWithoutPermissionAuthorityReturns403() throws Exception {
        mockMvc.perform(get(BENCH_URL).with(user("tech").roles("RECEPTION"))).andExpect(status().isForbidden());
    }

    @Test
    public void bench_qaViewQcAuthorityReturns200() throws Exception {
        mockMvc.perform(get(BENCH_URL).with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isOk());
    }

    @Test
    public void pdf_roleWithoutPermissionAuthorityReturns403() throws Exception {
        mockMvc.perform(get(PDF_URL).with(user("tech").roles("RECEPTION"))).andExpect(status().isForbidden());
    }

    // ==================== validation ====================

    @Test
    public void bench_analyzerSourceReturns400() throws Exception {
        // ASTM belongs to the instrument export; silently returning nothing would be
        // worse than refusing.
        mockMvc.perform(
                get(BENCH_URL + "&source=ASTM").with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void bench_unknownSourceReturns400() throws Exception {
        mockMvc.perform(get(BENCH_URL + "&source=WORKPLAN")
                .with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void bench_reversedDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/rest/qc/export/bench/csv?startDate=2026-06-30&endDate=2026-06-01")
                .with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc"))))
                .andExpect(status().isBadRequest());
    }

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

    @Configuration
    static class TestConfig {

        @Bean
        QCChartDataService qcChartDataService() {
            QCChartDataService service = mock(QCChartDataService.class);
            when(service.getExportModel(any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(new QCExportModel("Cobas 6000", List.of(), 0, 0, false));
            return service;
        }

        /**
         * The real bench export service over stubbed lookups, rather than a mock of it:
         * Spring injects into a manually registered bean, so mocking the class would
         * need these same collaborators anyway, and the real one keeps the assembly
         * step inside what the endpoint exercises.
         */
        @Bean
        QCBenchExportService qcBenchExportService() {
            return new QCBenchExportService();
        }

        @Bean
        QCResultService qcResultService() {
            QCResultService service = mock(QCResultService.class);
            when(service.findBenchResults(any(), any(), any(), anyInt())).thenReturn(List.of());
            return service;
        }

        @Bean
        QCControlLotService qcControlLotService() {
            return mock(QCControlLotService.class);
        }

        @Bean
        TestService testService() {
            return mock(TestService.class);
        }

        @Bean
        TestSectionService testSectionService() {
            return mock(TestSectionService.class);
        }

        @Bean
        SystemUserService systemUserService() {
            return mock(SystemUserService.class);
        }

        /**
         * The controller injects the abstract type, so one mock of it is the whole
         * requirement — registering the concrete implementation instead would drag in
         * its own injected collaborators.
         */
        @Bean
        ConfigurationProperties configurationProperties() {
            return mock(ConfigurationProperties.class);
        }

        /**
         * The writers resolve their column headings through the static MessageUtil,
         * which has no fallback when unset — so the export endpoints need it even where
         * the response body is not asserted.
         */
        @Bean
        MessageSource messageSource() {
            ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
            messageSource.setBasename("classpath:languages/message");
            messageSource.setDefaultEncoding("UTF-8");
            MessageUtil.setMessageSource(messageSource);
            return messageSource;
        }

        @Bean
        QCExportRestController qcExportRestController(QCChartDataService chartDataService,
                QCBenchExportService benchExportService, ConfigurationProperties configurationProperties) {
            QCExportRestController controller = new QCExportRestController();
            ReflectionTestUtils.setField(controller, "chartDataService", chartDataService);
            ReflectionTestUtils.setField(controller, "benchExportService", benchExportService);
            ReflectionTestUtils.setField(controller, "configurationProperties", configurationProperties);
            return controller;
        }
    }
}
