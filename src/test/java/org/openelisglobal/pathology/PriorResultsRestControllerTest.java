package org.openelisglobal.pathology;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.controller.PriorResultsRestController;
import org.openelisglobal.program.service.PriorResultsService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Each case view asks its own bench's address for the patient's earlier cases,
 * and gets the rows the panel prints. The test component scan leaves out the
 * program controllers, so the controller is built here over the real service
 * and scope, and a request to it cannot 404 for the wrong reason.
 */
public class PriorResultsRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PriorResultsService priorResultsService;

    @Autowired
    private ProgramSampleLabUnitScope programSampleLabUnitScope;

    private MockMvc mvc;

    private String rowsSetting;

    @Before
    public void init() throws Exception {
        PriorResultsServiceTest.removeSeededRows(jdbcTemplate);
        executeDataSetWithStateManagement(PriorResultsServiceTest.DATASET);
        PriorResultsServiceTest.seedRows(jdbcTemplate);
        rowsSetting = ConfigurationProperties.getInstance().getPropertyValue(Property.PATHOLOGY_PRIOR_RESULTS_ROWS);
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_PRIOR_RESULTS_ROWS, "5");
        mvc = MockMvcBuilders
                .standaloneSetup(new PriorResultsRestController(priorResultsService, programSampleLabUnitScope))
                .build();
    }

    @After
    public void cleanUp() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_PRIOR_RESULTS_ROWS,
                rowsSetting == null ? "5" : rowsSetting);
        PriorResultsServiceTest.removeSeededRows(jdbcTemplate);
    }

    @Test
    public void eachBenchAnswersItsOwnCasesPriorResults() throws Exception {
        mvc.perform(asViewer("/rest/pathology/caseView/15/priorResults?all=true")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12)).andExpect(jsonPath("$.rows.length()").value(12))
                .andExpect(jsonPath("$.restricted").value(false))
                .andExpect(jsonPath("$.rows[10].bench").value("PATHOLOGY"))
                .andExpect(jsonPath("$.rows[10].caseId").value(11))
                .andExpect(jsonPath("$.rows[10].labNumber").value("PR-0101"))
                .andExpect(jsonPath("$.rows[10].requestDate").value("2026-01-10"))
                .andExpect(jsonPath("$.rows[10].specimen").value("Tissue biopsy, Fluid"))
                .andExpect(jsonPath("$.rows[10].conclusionState").value("REPORTED"))
                .andExpect(jsonPath("$.rows[10].conclusion").value("Adenocarcinoma, Old term"))
                .andExpect(jsonPath("$.rows[11].bench").value("IMMUNOHISTOCHEMISTRY"))
                .andExpect(jsonPath("$.rows[11].conclusionState").value("NOT_REPORTED"))
                .andExpect(jsonPath("$.rows[11].conclusion").isEmpty());

        mvc.perform(asViewer("/rest/immunohistochemistry/caseView/21/priorResults")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.rows[0].bench").value("PATHOLOGY"))
                .andExpect(jsonPath("$.rows[0].caseId").value(11));

        mvc.perform(asViewer("/rest/cytology/caseView/32/priorResults")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7)).andExpect(jsonPath("$.rows[0].bench").value("CYTOLOGY"))
                .andExpect(jsonPath("$.rows[0].caseId").value(31)).andExpect(jsonPath("$.rows[0].specimen").value(""))
                .andExpect(jsonPath("$.rows[0].conclusion").value("Negative For Intraepithelial Lesion or Malignancy"));
    }

    @Test
    public void allTrueReturnsEveryRow_anyOtherValueIsTheCappedList() throws Exception {
        mvc.perform(asViewer("/rest/pathology/caseView/15/priorResults?all=true"))
                .andExpect(jsonPath("$.rows.length()").value(12));
        for (String capped : new String[] { "", "?all=false", "?all=TRUE", "?all=1", "?all=yes", "?all=" }) {
            mvc.perform(asViewer("/rest/pathology/caseView/15/priorResults" + capped)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.rows.length()").value(5)).andExpect(jsonPath("$.total").value(12));
        }
    }

    @Test
    public void aCaseWhoseOrderHasNoPatient_answers200WithNoRows() throws Exception {
        mvc.perform(asViewer("/rest/pathology/caseView/19/priorResults")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.rows.length()").value(0))
                .andExpect(jsonPath("$.restricted").value(false));
    }

    @Test
    public void anUnknownCaseIs404_onEachBench() throws Exception {
        mvc.perform(asViewer("/rest/pathology/caseView/999/priorResults")).andExpect(status().isNotFound());
        mvc.perform(asViewer("/rest/immunohistochemistry/caseView/999/priorResults")).andExpect(status().isNotFound());
        mvc.perform(asViewer("/rest/cytology/caseView/999/priorResults")).andExpect(status().isNotFound());
        // 11 is a pathology case: the other two benches do not know it.
        mvc.perform(asViewer("/rest/immunohistochemistry/caseView/11/priorResults")).andExpect(status().isNotFound());
        mvc.perform(asViewer("/rest/cytology/caseView/11/priorResults")).andExpect(status().isNotFound());
    }

    // helpers

    private static MockHttpServletRequestBuilder asViewer(String url) {
        UserSessionData viewer = new UserSessionData();
        viewer.setSytemUserId(Integer.parseInt(PriorResultsServiceTest.VIEWER));
        return get(url).requestAttr(IActionConstants.USER_SESSION_DATA, viewer);
    }
}
