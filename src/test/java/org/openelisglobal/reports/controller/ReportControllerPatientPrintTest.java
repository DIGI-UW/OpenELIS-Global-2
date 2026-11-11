package org.openelisglobal.reports.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.reportqueue.service.ReportQueueServiceImpl;
import org.openelisglobal.reports.action.implementation.PatientCILNSPClinical_vreduit;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The Patient Status Report form prints through the Report Print Queue when the
 * order has a released result, and still prints the preliminary report the way
 * it always did when nothing is released yet.
 */
public class ReportControllerPatientPrintTest extends BaseWebContextSensitiveTest {

    private static final long SAMPLE = 9301L;
    private static final String LAB_NO = "V1RPT0000009301";
    private static final int ADMIN = 9301;

    @Autowired
    private IStatusService statusService;

    private UserSessionData admin;
    private String originalBillingLabel;

    @Before
    public void loadOrder() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-report-v1.xml");
        originalBillingLabel = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL);
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "9309");
        removePrintHistory();
        ensureSystemRole(Constants.ROLE_GLOBAL_ADMIN);
        jdbcTemplate.update("INSERT INTO clinlims.system_user_role (system_user_id, role_id)"
                + " SELECT ?, id FROM clinlims.system_role WHERE name = ?", ADMIN, Constants.ROLE_GLOBAL_ADMIN);
        ensureSiteInformation(ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO, "2026-01-01 00:00:00");
        jdbcTemplate.update("UPDATE clinlims.site_information SET value = '2026-01-01 00:00:00' WHERE name = ?",
                ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO);
        admin = new UserSessionData();
        admin.setSytemUserId(ADMIN);
        acceptEveryAccessionNumber();
    }

    /**
     * The report form checks the lab number's format through the validator factory,
     * which AppTestConfig registers as a Mockito mock.
     */
    private static void acceptEveryAccessionNumber() throws Exception {
        IAccessionNumberValidator validator = Mockito.mock(IAccessionNumberValidator.class);
        Mockito.when(validator.validFormat(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(ValidationResults.SUCCESS);
        Mockito.when(validator.getMaxAccessionLength()).thenReturn(20);
        Mockito.when(validator.getMinAccessionLength()).thenReturn(5);
        AccessionNumberValidatorFactory factory = SpringContext.getBean(AccessionNumberValidatorFactory.class);
        Mockito.when(factory.getValidator(Mockito.any())).thenReturn(validator);
    }

    @After
    public void restore() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL,
                originalBillingLabel);
        jdbcTemplate.update("DELETE FROM clinlims.system_user_role WHERE system_user_id = ?", ADMIN);
        removePrintHistory();
    }

    private void removePrintHistory() {
        jdbcTemplate.update("DELETE FROM clinlims.document_track WHERE row_id = ?"
                + " AND table_id = (SELECT id FROM clinlims.reference_tables WHERE name = 'SAMPLE')", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_print_event WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = ?)", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_version_item WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = ?)", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_version WHERE sample_id = ?", SAMPLE);
    }

    private MockHttpServletResponse printPatientReport() throws Exception {
        return mockMvc
                .perform(get("/ReportPrint").param("type", "patient")
                        .param("report", PatientCILNSPClinical_vreduit.REPORT_NAME).param("accessionDirect", LAB_NO)
                        .sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk()).andReturn().getResponse();
    }

    private static void assertPdf(MockHttpServletResponse response) {
        assertTrue("content type " + response.getContentType(),
                String.valueOf(response.getContentType()).startsWith("application/pdf"));
        assertEquals("%PDF-", new String(response.getContentAsByteArray(), 0, 5));
    }

    private int versions() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.report_version WHERE sample_id = ?",
                Integer.class, SAMPLE);
    }

    @Test
    public void aReleasedOrderPrintsThroughTheQueueAndIsArchived() throws Exception {
        assertPdf(printPatientReport());

        assertEquals(1, versions());
    }

    @Test
    public void anOrderWithNothingReleasedPrintsThePreliminaryReportWithoutArchivingIt() throws Exception {
        String technicalAcceptance = statusService.getStatusID(AnalysisStatus.TechnicalAcceptance);
        jdbcTemplate.update("UPDATE clinlims.analysis SET status_id = CAST(? AS numeric) WHERE id IN (9301, 9305)",
                technicalAcceptance);

        assertPdf(printPatientReport());

        assertEquals(0, versions());
    }
}
