package org.openelisglobal.compliance.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * OGC-1619: the Compliance Report finds an order by its lab number whatever its
 * date. The orders are those of compliance-dashboard.xml, collected in June
 * 2026, outside the default 90-day window; CD-9101 and CD-9103 are
 * environmental orders, CD-9102 is not.
 */
public class ComplianceReportLabNumberIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private SampleService sampleService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/compliance-dashboard.xml");
        jdbcTemplate.update("INSERT INTO clinlims.observation_history_type (id, type_name, description, lastupdated)"
                + " SELECT 9201, 'envWorkflowType', 'Workflow type', now() WHERE NOT EXISTS"
                + " (SELECT 1 FROM clinlims.observation_history_type WHERE type_name = 'envWorkflowType')");
        jdbcTemplate.update("DELETE FROM clinlims.observation_history WHERE id BETWEEN 9201 AND 9203");
        for (int sample : new int[] { 9101, 9103 }) {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.observation_history (id, sample_id, observation_history_type_id,"
                            + " value_type, value, lastupdated) SELECT ?, ?, id, 'L', 'environmental', now()"
                            + " FROM clinlims.observation_history_type WHERE type_name = 'envWorkflowType'",
                    sample + 100, sample);
        }
    }

    @Test
    public void aFullLabNumberFindsThatOrderWithoutDates() throws Exception {
        assertEquals(List.of("CD-9101"), labNumbersFor("CD-9101"));
    }

    @Test
    public void aPartialLabNumberMatchesTheStartAndLeavesOutOtherOrders() throws Exception {
        assertEquals(List.of("CD-9101", "CD-9103"), labNumbersFor("CD-910"));
    }

    @Test
    public void anUnknownOrNonEnvironmentalLabNumberListsNothing() throws Exception {
        mockMvc.perform(get("/rest/complianceReport").param("labNumber", "CD-9102").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders").isEmpty());
        mockMvc.perform(get("/rest/complianceReport").param("labNumber", "NO-SUCH%").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders").isEmpty());
    }

    @Test
    public void anOrderHiddenByAFilterStillCountsAsAMatch() throws Exception {
        mockMvc.perform(get("/rest/complianceReport").param("labNumber", "CD-9101")
                .param("complianceStatus", "COMPLIANT").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isEmpty()).andExpect(jsonPath("$.labNumberMatches").value(1));
        mockMvc.perform(get("/rest/complianceReport").param("labNumber", "CD-9102").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.labNumberMatches").value(0));
    }

    @Test
    public void aLabNumberTypedInLowerCaseIsFound() throws Exception {
        assertEquals(List.of("CD-9101"), labNumbersFor("cd-9101"));
    }

    @Test
    public void withoutALabNumberTheDateWindowStillApplies() throws Exception {
        mockMvc.perform(get("/rest/complianceReport").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[?(@.labNumber == 'CD-9101')]").isEmpty());
    }

    @Test
    public void clinicalOrdersSharingThePrefixDoNotCrowdOutEnvironmentalOnes() {
        for (int id = 9301; id <= 9303; id++) {
            jdbcTemplate.update("INSERT INTO clinlims.sample (id, accession_number, domain, entered_date,"
                    + " received_date, is_confirmation, lastupdated) VALUES (?, ?, 'H', now(), now(), false, now())",
                    id, "CD-90" + id);
        }

        List<String> found = sampleService.getEnvironmentalSamplesWithAccessionNumberStartingWith("CD-9", 2).stream()
                .map(Sample::getAccessionNumber).toList();

        assertEquals("the limit counts environmental orders only", List.of("CD-9101", "CD-9102"), found);
    }

    private List<String> labNumbersFor(String labNumber) throws Exception {
        String body = mockMvc.perform(get("/rest/complianceReport").param("labNumber", labNumber))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.orders[*].labNumber");
    }
}
