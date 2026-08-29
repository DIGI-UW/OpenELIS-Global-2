package org.openelisglobal.compliance.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for ComplianceReportRestController date-handling.
 *
 * The report endpoint converts the ISO yyyy-MM-dd dates sent by the React
 * frontend into whatever format getSamplesReceivedInDateRange() expects via
 * DateUtil.getDateFormat(). These tests pin DEFAULT_DATE_LOCALE to fr-FR
 * (dd/MM/yyyy) so they are deterministic regardless of server configuration.
 *
 * Bug: formatToSystemDate() was hardcoded to "MM/dd/yyyy", producing
 * "05/31/2026" for 2026-05-31. Under dd/MM/yyyy, month 31 is invalid →
 * LIMSRuntimeException → 500. Fix: use DateUtil.getDateFormat() at runtime.
 */
public class ComplianceReportRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DefaultConfigurationProperties configurationProperties;

    private String originalDateLocale;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        originalDateLocale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE);
        // Pin to fr-FR (dd/MM/yyyy) so tests are deterministic regardless of the
        // server locale configured in SystemConfiguration.properties.
        configurationProperties.setPropertyValue(Property.DEFAULT_DATE_LOCALE, "fr-FR");
    }

    @After
    public void restoreDateLocale() {
        if (originalDateLocale != null) {
            configurationProperties.setPropertyValue(Property.DEFAULT_DATE_LOCALE, originalDateLocale);
        }
    }

    /**
     * Regression test for the Laporan Hasil "Unparseable date" bug.
     *
     * With the locale pinned to fr-FR (dd/MM/yyyy), the broken hardcoded
     * "MM/dd/yyyy" conversion produces "05/31/2026" which DateUtil refuses to parse
     * (month 31 does not exist) → 500. The fix uses DateUtil.getDateFormat() which
     * produces "31/05/2026" → 200.
     *
     * The date 2026-05-31 is chosen because its month (5) and day (31) cannot be
     * swapped without creating an impossible month value, making the locale
     * mismatch unambiguous.
     */
    @Test
    public void getReport_ddMMyyyyLocale_dayGreaterThan12_mustReturn200NotDateParseError() throws Exception {
        mockMvc.perform(get("/rest/complianceReport").param("dateFrom", "2026-05-31").param("dateTo", "2026-05-31")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    /**
     * Full-month range through the same conversion path — sanity baseline.
     */
    @Test
    public void getReport_ddMMyyyyLocale_isoDateRange_shouldReturn200() throws Exception {
        mockMvc.perform(get("/rest/complianceReport").param("dateFrom", "2026-01-01").param("dateTo", "2026-12-31")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    /**
     * No date params — the controller computes today.minusDays(90) and today via
     * LocalDate and calls formatToSystemDate() on those too. Verifies the default
     * window path does not blow up.
     */
    @Test
    public void getReport_ddMMyyyyLocale_noDateParams_shouldReturn200UsingDefaultWindow() throws Exception {
        mockMvc.perform(get("/rest/complianceReport").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }
}
