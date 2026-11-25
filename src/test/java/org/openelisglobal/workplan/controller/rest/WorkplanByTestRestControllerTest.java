package org.openelisglobal.workplan.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import javax.sql.DataSource;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * OGC-1647: Print and the Remove boxes know a workplan row by its analysis, and
 * Print reads every page of the workplan from the session.
 */
@Transactional
public class WorkplanByTestRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private IStatusService statusService;

    private JdbcTemplate jdbc;
    private AnnotationConfigWebApplicationContext workplanContext;
    private Object statusListBefore;
    private long first;
    private long second;

    /** The test context scans no workplan controllers; this one is served here. */
    @Configuration
    @EnableWebMvc
    static class WorkplanConfig {
        @Bean
        WorkplanByTestRestController workplanByTestRestController() {
            return new WorkplanByTestRestController();
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        statusListBefore = ReflectionTestUtils.getField(WorkplanRestController.class, "statusList");
        workplanContext = new AnnotationConfigWebApplicationContext();
        workplanContext.setParent(webApplicationContext);
        workplanContext.setServletContext(webApplicationContext.getServletContext());
        workplanContext.register(WorkplanConfig.class);
        workplanContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(workplanContext).build();
        executeDataSetWithStateManagement("testdata/analyzer-results.xml");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, name, code, status_type, description, is_active,"
                + " lastupdated) VALUES (9001, 'Not Tested', 1, 'ANALYSIS', 'Not tested', 'Y', NOW())"
                + " ON CONFLICT (id) DO NOTHING");
        statusService.refreshCache();
        ReflectionTestUtils.setField(WorkplanRestController.class, "statusList",
                List.of(statusService.getStatusID(AnalysisStatus.NotStarted)));
        long sample = jdbc.queryForObject("SELECT nextval('sample_seq')", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.sample (id, accession_number, domain, entered_date, received_date,"
                        + " status_id, lastupdated) VALUES (?, 'WP1647', 'H', CURRENT_DATE, CURRENT_DATE, 7001, NOW())",
                sample);
        first = analysisOnNewTube(sample, 1);
        second = analysisOnNewTube(sample, 2);
    }

    @After
    public void restoreStatusList() {
        ReflectionTestUtils.setField(WorkplanRestController.class, "statusList", statusListBefore);
        if (workplanContext != null) {
            workplanContext.close();
        }
    }

    private long analysisOnNewTube(long sample, int sortOrder) {
        long tube = jdbc.queryForObject("SELECT nextval('sample_item_seq')", Long.class);
        jdbc.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)"
                + " VALUES (?, ?, ?, 7001, NOW())", tube, sample, sortOrder);
        long analysis = jdbc.queryForObject("SELECT nextval('analysis_seq')", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_id, status_id, analysis_type, revision,"
                        + " is_reportable, lastupdated) VALUES (?, ?, 4001, 9001, 'MANUAL', '0', 'Y', NOW())",
                analysis, tube);
        return analysis;
    }

    @Test
    public void theSameTestOnTwoSamplesOfAnOrderComesAsTwoAnalyses() throws Exception {
        mockMvc.perform(get("/rest/WorkPlanByTest").param("test_id", "4001")).andExpect(status().isOk())
                .andExpect(jsonPath("$.workplanTests.length()").value(2))
                .andExpect(jsonPath("$.workplanTests[*].accessionNumber")
                        .value(CoreMatchers.everyItem(CoreMatchers.is("WP1647"))))
                .andExpect(jsonPath("$.workplanTests[*].analysisId")
                        .value(CoreMatchers.hasItems(String.valueOf(first), String.valueOf(second))));
    }

    @Test
    public void aPageComesFromItsOwnWorkplanAfterOtherScreensLoad() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/rest/WorkPlanByTest").param("test_id", "4001").session(session))
                .andExpect(status().isOk());
        session.setAttribute(IActionConstants.RESULTS_SESSION_CACHE, List.of(List.of()));
        mockMvc.perform(get("/rest/WorkPlanByTest").param("test_id", "4002").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/rest/WorkPlanByTest").param("test_id", "4001").param("page", "1").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workplanTests.length()").value(2))
                .andExpect(jsonPath("$.workplanTests[0].accessionNumber").value("WP1647"));
    }
}
