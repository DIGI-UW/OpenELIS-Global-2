package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

/**
 * An answer's codes in every system, read and saved through the catalog API.
 */
public class TestCatalogEditorAnswerTerminologyIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long ANSWER_ID = 96601L;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update("INSERT INTO clinlims.dictionary (id, dict_entry, is_active, lastupdated)"
                + " VALUES (?, 'Positive (answer API IT)', 'Y', NOW())", ANSWER_ID);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void anAnswersCodesAreReadAndSavedInEverySystem() throws Exception {
        String path = "/rest/test-catalog/answers/" + ANSWER_ID + "/terminology";
        mockMvc.perform(get(path).session(authedSession())).andExpect(status().isOk())
                .andExpect(jsonPath("$.mappings.length()").value(0));

        mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"mappings\":[{\"source\":\"LOINC\",\"code\":\"LA6576-8\",\"relationship\":\"SAME_AS\"},"
                        + "{\"source\":\"SNOMED\",\"code\":\"10828004\",\"relationship\":\"SAME_AS\","
                        + "\"displayName\":\"Positive\"}]}")
                .session(authedSession())).andExpect(status().isOk())
                .andExpect(jsonPath("$.mappings.length()").value(2));

        assertEquals("LA6576-8", jdbc.queryForObject("SELECT loinc_code FROM clinlims.dictionary WHERE id = ?",
                String.class, ANSWER_ID));
        mockMvc.perform(get(path).session(authedSession()))
                .andExpect(jsonPath("$.mappings[?(@.source == 'SNOMED')].displayName").value("Positive"));
    }

    @Test
    public void anAnswerTakesOnlyStandardSystemsAndOneOfEachCode() throws Exception {
        String path = "/rest/test-catalog/answers/" + ANSWER_ID + "/terminology";
        for (String body : new String[] { "{\"mappings\":[{\"source\":\"WHONET\",\"code\":\"POS\"}]}",
                "{\"mappings\":[{\"source\":\"LOINC\",\"code\":\"LA6576-8\"},{\"source\":\"LOINC\",\"code\":\"LA6576-8\"}]}",
                "{\"mappings\":[{\"source\":\"LOINC\",\"code\":\"LA6576-8\",\"relationship\":\"EQUIVALENT\"}]}" }) {
            mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(body).session(authedSession()))
                    .andExpect(status().isUnprocessableEntity());
        }
        mockMvc.perform(get("/rest/test-catalog/answers/99999999/terminology").session(authedSession()))
                .andExpect(status().isNotFound());
    }

    private static MockHttpSession authedSession() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return session;
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.dictionary_terminology_mapping WHERE dictionary_id = ?", ANSWER_ID);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE id = ?", ANSWER_ID);
    }
}
