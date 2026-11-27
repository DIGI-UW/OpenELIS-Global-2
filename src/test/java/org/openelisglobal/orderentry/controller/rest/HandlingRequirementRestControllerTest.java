package org.openelisglobal.orderentry.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1424 (FR-C9a): the Required line of a sample's Handling group reads the
 * test catalog's storage condition and holding time; unknown or malformed test
 * ids are skipped rather than failing the sample.
 */
public class HandlingRequirementRestControllerTest extends BaseWebContextSensitiveTest {

    private static final int REFRIGERATED_TEST = 987_710;
    private static final int UNCONFIGURED_TEST = 987_711;

    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void seed() {
        jdbc = new JdbcTemplate(dataSource);
        cleanUp();
        jdbc.update("INSERT INTO clinlims.test (id, name, description, guid, is_active, time_holding, lastupdated) "
                + "VALUES (?, 'HRQ cold', 'HRQ cold', 'hrq-cold', 'Y', '240', now())", REFRIGERATED_TEST);
        jdbc.update("INSERT INTO clinlims.test (id, name, description, guid, is_active, lastupdated) "
                + "VALUES (?, 'HRQ plain', 'HRQ plain', 'hrq-plain', 'Y', now())", UNCONFIGURED_TEST);
        jdbc.update(
                "INSERT INTO clinlims.test_sample_handling (id, test_id, storage_condition, version, is_active, "
                        + "lastupdated, last_updated) VALUES ('hrq-handling', ?, 'REFRIGERATED', 0, 'Y', now(), now())",
                REFRIGERATED_TEST);
    }

    @After
    public void cleanUp() {
        JdbcTemplate template = jdbc == null ? new JdbcTemplate(dataSource) : jdbc;
        template.update("DELETE FROM clinlims.test_sample_handling WHERE id = 'hrq-handling'");
        template.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", REFRIGERATED_TEST, UNCONFIGURED_TEST);
    }

    @Test
    public void theCatalogConditionAndHoldingTimeAreTheRequirement() throws Exception {
        List<Map<String, Object>> requirements = requirements(REFRIGERATED_TEST + "," + UNCONFIGURED_TEST);

        assertEquals(2, requirements.size());
        assertEquals("REFRIGERATED", requirements.get(0).get("storageCondition"));
        assertEquals(240, requirements.get(0).get("holdingMinutes"));
        assertNull("a test the catalog sets nothing for has no requirement",
                requirements.get(1).get("storageCondition"));
        assertNull(requirements.get(1).get("holdingMinutes"));
    }

    @Test
    public void unknownAndMalformedIdsAreSkipped() throws Exception {
        List<Map<String, Object>> requirements = requirements("abc,,999999999," + REFRIGERATED_TEST);

        assertEquals(1, requirements.size());
        assertEquals(String.valueOf(REFRIGERATED_TEST), requirements.get(0).get("testId"));
    }

    private List<Map<String, Object>> requirements(String testIds) throws Exception {
        String response = mockMvc.perform(get("/rest/sample-handling/requirements").param("testIds", testIds))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readValue(response, new TypeReference<List<Map<String, Object>>>() {
        });
    }
}
