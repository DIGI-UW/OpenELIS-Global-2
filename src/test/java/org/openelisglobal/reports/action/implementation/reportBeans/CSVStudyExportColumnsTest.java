package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opencsv.CSVReader;
import jakarta.annotation.PreDestroy;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.AppTestConfig;
import org.openelisglobal.BaseTestConfig;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.controller.ReportController;
import org.openelisglobal.reports.controller.rest.ReportRestController;
import org.openelisglobal.reports.form.ReportForm.DateType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Export readers open their own connections, so this suite owns a dedicated
 * database for committed DBUnit fixtures and per-test result mutations.
 */
@ContextConfiguration(inheritLocations = false, classes = { AppTestConfig.class,
        CSVStudyExportColumnsTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CSVStudyExportColumnsTest extends BaseWebContextSensitiveTest {
    @Configuration
    public static class TestConfig extends BaseTestConfig {
        private final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:14.4");

        @Override
        protected PostgreSQLContainer<?> databaseContainer() {
            return database;
        }

        @PreDestroy
        public void stopDatabase() {
            database.stop();
        }
    }

    private static final int SAMPLE = 992200;
    private static final Date DAY = Date.valueOf("2002-03-04");
    private Field dictionaryInstance;
    private Object previousDictionary;

    @Before
    public void createFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/csv-study-export.xml");
        SpringContext.getBean(IStatusService.class).refreshCache();
        dictionaryInstance = ResourceTranslator.DictionaryTranslator.class.getDeclaredField("instance");
        dictionaryInstance.setAccessible(true);
        previousDictionary = dictionaryInstance.get(null);
        dictionaryInstance.set(null, null);

    }

    @After
    public void cleanFixture() throws Exception {
        if (dictionaryInstance != null) {
            dictionaryInstance.set(null, previousDictionary);
        }
        SpringContext.getBean(IStatusService.class).refreshCache();
    }

    @Test
    public void quotedLabelRemainsAHeaderAndCannotChangeTheQuery() throws Exception {
        String label = "quoted\"; SELECT 987654321; --";
        insertResult(992201, label, "N", "7.25");
        Exported row = exportResults(992201);
        assertEquals(List.of(label), row.headers());
        assertEquals(List.of("7.25"), row.values());
        assertEquals(Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT count(*) FROM sample WHERE id = ?", Integer.class, SAMPLE));
    }

    @Test
    public void duplicateAndLongUnicodeLabelsKeepTheirOwnValuesInRequestedOrder() throws Exception {
        String label = "Échantillon 日本語 ".repeat(7);
        insertResult(992202, label, "N", "13.5");
        insertResult(992203, label, "N", "42");
        Exported row = exportResults(992203, 992202);
        assertEquals(List.of(label, label), row.headers());
        assertEquals(List.of("42", "13.5"), row.values());
    }

    @Test
    public void sharedDisplayNameDoesNotMixNumericAndDictionaryResults() throws Exception {
        jdbcTemplate
                .update("INSERT INTO dictionary (id, dict_entry, is_active) VALUES (992290, 'Study positive', 'Y')");
        insertResult(992204, "Same label", "N", "992290");
        insertResult(992205, "Same label", "D", "992290");
        assertEquals(List.of("992290", "Study positive"), exportResults(992204, 992205).values());
    }

    @Test
    public void missingResultsStayBlankAndTextIsNotReinterpreted() throws Exception {
        insertResult(992206, "Text", "A", "<5, pending");
        insertResult(992207, "Missing", "N", null);
        assertEquals(List.of("<5, pending", ""), exportResults(992206, 992207).values());
    }

    @Test
    public void noTestsStillExportsTheSampleColumn() throws Exception {
        CSVColumnBuilder builder = resultBuilder();
        builder.add("sample_id", "Sample", CSVColumnBuilder.Strategy.NONE);
        assertEquals(new Exported(List.of("Sample"), List.of(String.valueOf(SAMPLE))), read(builder));
    }

    @Test
    public void observationIdentifierIsQuotedAndRetainsItsValue() throws Exception {
        String name = "Name\"; SELECT 42; --";
        jdbcTemplate.update("INSERT INTO person (id, first_name, last_name) VALUES (?, 'Fixture', 'Patient')", SAMPLE);
        jdbcTemplate.update("INSERT INTO patient (id, person_id) VALUES (?, ?)", SAMPLE, SAMPLE);
        jdbcTemplate.update("INSERT INTO observation_history_type (id, type_name) VALUES (?, ?)", SAMPLE, name);
        jdbcTemplate.update(
                "INSERT INTO observation_history (id, patient_id, sample_id, observation_history_type_id, value_type, value)"
                        + " VALUES (?, ?, ?, ?, 'A', 'unchanged')",
                SAMPLE, SAMPLE, SAMPLE, SAMPLE);
        CSVColumnBuilder builder = new CSVColumnBuilder(null) {
            {
                allObHistoryTypes = SpringContext.getBean(ObservationHistoryTypeService.class).getAllOrdered("typeName",
                        false);
                add(name, "Observation", Strategy.NONE);
            }

            @Override
            public void makeSQL() {
                query = new StringBuilder("SELECT demo.* FROM sample s");
                appendObservationHistoryCrosstab(DAY, DAY, "s.entered_date");
                query.append(" WHERE s.id = ").append(SAMPLE).append(" AND demo.samp_id = s.id");
            }
        };
        assertEquals(List.of("unchanged"), read(builder).values());
        CSVRoutineColumnBuilder routine = new CSVRoutineColumnBuilder(null) {
            {
                allObHistoryTypes = SpringContext.getBean(ObservationHistoryTypeService.class).getAllOrdered("typeName",
                        false);
                add(name, "Observation", Strategy.NONE);
            }

            @Override
            public void makeSQL() {
                query = new StringBuilder("SELECT demo.* FROM sample s");
                appendObservationHistoryCrosstab(DAY, DAY);
                query.append(" WHERE s.id = ").append(SAMPLE).append(" AND demo.samp_id = s.id");
            }
        };
        try {
            routine.buildDataSource();
            assertTrue(routine.next());
            assertEquals(List.of("Observation"), parse(routine.getColumnNamesLine()));
            assertEquals(List.of("unchanged"), parse(routine.nextLine()));
            assertFalse(routine.next());
        } finally {
            routine.closeResultSet();
        }
    }

    @Test
    public void haitiExportKeepsDemographicsAndLocalizedResultHeadings() throws Exception {
        linkPatientAndOrganization();
        String label = "Résultat \"quoted\"";
        insertResult(992208, label, "N", "15.75");
        Exported row = read(new HaitiColumnBuilder(dateRange()));
        assertEquals("STUDY-PRIVATE-FIXTURE", row.value("LABNO"));
        assertEquals("Fixture", row.value("PRENOM"));
        assertEquals("Export clinic", row.value("SERVICE"));
        assertEquals("15.75", row.value(label));
    }

    @Test
    public void hpvExportUsesItsPanelTestIdentityAndDictionaryValue() throws Exception {
        linkPatientAndOrganization();
        jdbcTemplate.update("INSERT INTO dictionary (id, dict_entry, is_active) VALUES (992290, 'Positive', 'Y')");
        // HPV installations configure this observation alongside the panel.
        jdbcTemplate
                .update("INSERT INTO observation_history_type (id, type_name) VALUES (992291, 'hpvsamplingmethod')");
        insertResult(992209, "HPV \"quoted\"", "D", "992290");
        jdbcTemplate.update(
                "INSERT INTO panel (id, name, description, name_localization_id) VALUES (?, 'HPV HR', 'Fixture panel', ?)",
                SAMPLE, SAMPLE);
        jdbcTemplate.update("INSERT INTO panel_item (id, panel_id, test_id) VALUES (?, ?, 992209)", SAMPLE, SAMPLE);
        Exported row = read(new HPVColumnBuilder(dateRange(), "", DateType.ORDER_DATE));
        assertEquals("STUDY-PRIVATE-FIXTURE", row.value("Lab No"));
        assertEquals("Positive", row.value("HPV \"quoted\""));
    }

    @Test
    public void studyEidExportRetainsDictionaryText() throws Exception {
        prepareEidFixture();
        Exported study = read(new StudyEIDColumnBuilder(dateRange(), "", DateType.ORDER_DATE));
        assertEquals("1=Positive", study.value("DNA PCR"));
        Exported eid = read(new EIDColumnBuilder(dateRange(), ""));
        assertEquals("1=Positive", eid.value("DNA PCR"));
    }

    @Test
    public void studyLogColumnUsesTheSameResultAsItsNumericColumn() throws Exception {
        insertResult(992211, "Viral Load", "N", "1000");
        CSVColumnBuilder builder = resultBuilder(992211);
        builder.addViralLoadLogColumn();
        assertEquals(List.of("1000", "3.0"), read(builder).values());
    }

    private static final List<String> CSV_REPORTS = List.of("CISampleExport", "HaitiExportReport",
            "HaitiLNSPExportReport", "CIStudyExport", "CISampleRoutineExport", "TBOrderExport", "Trends",
            "ExportWHONETReportByDate", "ForCIDashboard", "indicatorHaitiLNSPSiteTestCount", "sampleRejectionReport");

    @Test
    public void csvExportsRejectAnActiveUserWithoutReportsBeforeReadingClinicalData() throws Exception {
        for (String report : CSV_REPORTS) {
            csvExport(report, 992221, "999999", 403, false);
            csvExport(report, 992221, "999999", 403, true);
        }
    }

    @Test
    public void csvExportsRejectAnInactiveReportUser() throws Exception {
        for (String report : CSV_REPORTS) {
            csvExport(report, 992224, "992220", 403, false);
            csvExport(report, 992224, "992220", 403, true);
        }
    }

    @Test
    public void restoredExportsStillReturnCsvForReportUsersAndAdministrators() throws Exception {
        prepareEidFixture();
        jdbcTemplate.update("UPDATE sample_projects SET proj_id = 992220 WHERE samp_id = ?", SAMPLE);
        for (String report : List.of("CISampleExport", "HaitiExportReport", "CIStudyExport")) {
            for (int actor : List.of(992222, 992223)) {
                for (boolean legacy : List.of(false, true)) {
                    String csv = csvExport(report, actor, "992220", 200, legacy);
                    assertTrue(report + " includes its fixture sample", csv.contains("STUDY-PRIVATE-FIXTURE"));
                }
            }
        }
    }

    private String csvExport(String report, int actor, String project, int expectedStatus, boolean legacy)
            throws Exception {
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(actor);
        String day = DateUtil.convertSqlDateToStringDate(DAY);
        var request = legacy
                ? get("/ReportPrint").param("type", "patient").param("report", report).param("projectCode", project)
                        .param("lowerDateRange", day).param("upperDateRange", day).param("dateType", "ORDER_DATE")
                : post("/rest/ReportPrint").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"report\":\"" + report + "\",\"projectCode\":\"" + project
                                + "\",\"dateType\":\"ORDER_DATE\",\"lowerDateRange\":\"" + day
                                + "\",\"upperDateRange\":\"" + day + "\"}");
        return MockMvcBuilders
                .standaloneSetup(webApplicationContext.getBean(ReportRestController.class),
                        webApplicationContext.getBean(ReportController.class))
                .setControllerAdvice(new ControllerSetup()).build()
                .perform(request.sessionAttr(IActionConstants.USER_SESSION_DATA, user))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }

    private void prepareEidFixture() {
        jdbcTemplate.update("INSERT INTO dictionary_category (id, name, description)"
                + " SELECT 992292, 'EID Type of Clinic', 'EID Type of Clinic' WHERE NOT EXISTS"
                + " (SELECT 1 FROM dictionary_category WHERE description = 'EID Type of Clinic')");
        jdbcTemplate.update("INSERT INTO dictionary (id, dictionary_category_id, dict_entry, is_active)"
                + " SELECT 992293, id, '4 = Other(Specify)', 'Y' FROM dictionary_category c"
                + " WHERE c.description = 'EID Type of Clinic' AND NOT EXISTS"
                + " (SELECT 1 FROM dictionary d WHERE d.dictionary_category_id = c.id"
                + " AND d.dict_entry = '4 = Other(Specify)')");
        linkPatientAndOrganization();
        jdbcTemplate.update("INSERT INTO dictionary (id, dict_entry, is_active) VALUES (992290, '1=Positive', 'Y')");
        insertResult(992210, "DNA PCR", "D", "992290");
        jdbcTemplate.update("UPDATE test SET name = 'DNA PCR' WHERE id = 992210");
        String dnaTest = SpringContext.getBean(TestService.class).getActiveTestByName("DNA PCR").get(0).getId();
        jdbcTemplate.update("UPDATE analysis SET test_id = ? WHERE id = 992210", Long.parseLong(dnaTest));
        jdbcTemplate.update("UPDATE test_result SET test_id = ? WHERE id = 992210", Long.parseLong(dnaTest));
    }

    private DateRange dateRange() {
        DateRange range = mock(DateRange.class);
        when(range.getLowDate()).thenReturn(DAY);
        when(range.getHighDate()).thenReturn(DAY);
        return range;
    }

    private void linkPatientAndOrganization() {
        jdbcTemplate.update("INSERT INTO person (id, first_name, last_name) VALUES (?, 'Fixture', 'Patient')", SAMPLE);
        jdbcTemplate.update("INSERT INTO patient (id, person_id, external_id) VALUES (?, ?, 'STUDY-PATIENT')", SAMPLE,
                SAMPLE);
        jdbcTemplate.update("INSERT INTO sample_human (id, samp_id, patient_id) VALUES (?, ?, ?)", SAMPLE, SAMPLE,
                SAMPLE);
        jdbcTemplate.update("INSERT INTO organization (id, name) VALUES (?, 'Export clinic')", SAMPLE);
        jdbcTemplate.update("INSERT INTO sample_organization (id, samp_id, org_id) VALUES (?, ?, ?)", SAMPLE, SAMPLE,
                SAMPLE);
        jdbcTemplate.update("INSERT INTO sample_projects (id, samp_id, proj_id) SELECT ?, ?, min(id) FROM project",
                SAMPLE, SAMPLE);
        jdbcTemplate.update(
                "INSERT INTO observation_history (id, patient_id, sample_id, observation_history_type_id, value_type, value)"
                        + " SELECT ?, ?, ?, min(id), 'A', 'fixture' FROM observation_history_type",
                SAMPLE, SAMPLE, SAMPLE);
    }

    private Exported exportResults(int... ids) throws Exception {
        return read(resultBuilder(ids));
    }

    private CSVColumnBuilder resultBuilder(int... ids) {
        return new CSVColumnBuilder(null) {
            {
                allTests = Arrays.stream(ids)
                        .mapToObj(id -> SpringContext.getBean(TestService.class).getTestById(String.valueOf(id)))
                        .collect(Collectors.toList());
                defineAllTestsAndResults();
                addAllResultsColumns();
            }

            @Override
            public void makeSQL() {
                query = new StringBuilder("SELECT s.id AS sample_id, result.* FROM sample s");
                appendResultCrosstab(DAY, DAY, "s.entered_date");
                query.append(" WHERE s.id = ").append(SAMPLE).append(" AND result.samp_id = s.id");
            }
        };
    }

    private Exported read(CSVColumnBuilder builder) throws Exception {
        try {
            builder.buildDataSource();
            assertTrue("fixture sample must be exported", builder.next());
            Exported row = new Exported(parse(builder.getColumnNamesLine()), parse(builder.nextLine()));
            assertFalse("no unrelated samples", builder.next());
            return row;
        } finally {
            builder.closeResultSet();
        }
    }

    private List<String> parse(String line) throws Exception {
        try (CSVReader reader = new CSVReader(new StringReader(line))) {
            return Arrays.asList(reader.readNext());
        }
    }

    private void insertResult(int id, String label, String type, String value) {
        jdbcTemplate.update("INSERT INTO localization (id, description) VALUES (?, 'Study fixture label')", id);
        jdbcTemplate.update(
                "INSERT INTO localization_value (id, localization_id, locale, value) VALUES (?, ?, 'en', ?)", id, id,
                label);
        jdbcTemplate.update(
                "INSERT INTO test (id, name, description, guid, test_section_id, is_active, name_localization_id)"
                        + " VALUES (?, ?, ?, ?, ?, 'Y', ?)",
                id, "Study " + id, "Study " + id, "study-" + id, SAMPLE, id);
        jdbcTemplate.update("INSERT INTO analysis (id, sampitem_id, test_id, analysis_type) VALUES (?, ?, ?, 'MANUAL')",
                id, SAMPLE, id);
        jdbcTemplate.update("UPDATE analysis SET status_id = ? WHERE id = ?", Long.parseLong(
                SpringContext.getBean(IStatusService.class).getStatusID(StatusService.AnalysisStatus.Finalized)), id);
        jdbcTemplate.update("INSERT INTO test_result (id, test_id, tst_rslt_type) VALUES (?, ?, ?)", id, id, type);
        if (value != null) {
            jdbcTemplate.update("INSERT INTO result (id, analysis_id, test_result_id, value) VALUES (?, ?, ?, ?)", id,
                    id, id, value);
        }
    }

    private record Exported(List<String> headers, List<String> values) {
        String value(String heading) {
            assertTrue("missing CSV header: " + heading, headers.contains(heading));
            return values.get(headers.indexOf(heading));
        }
    }
}
