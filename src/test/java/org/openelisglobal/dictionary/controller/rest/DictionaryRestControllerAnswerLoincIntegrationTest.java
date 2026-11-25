package org.openelisglobal.dictionary.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionaryterminology.service.DictionaryTerminologyMappingService;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

/**
 * A LOINC code saved in Dictionary Management reaches the answer's terminology
 * mappings.
 */
public class DictionaryRestControllerAnswerLoincIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long CATEGORY_ID = 96701L;

    @Autowired
    private DictionaryTerminologyMappingService mappingService;

    @Autowired
    private DictionaryCategoryService categoryService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update("INSERT INTO clinlims.dictionary_category (id, name, description, local_abbrev, lastupdated)"
                + " VALUES (?, 'Answer LOINC IT', 'Answer LOINC IT', 'ALIT', NOW())", CATEGORY_ID);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void aLoincCodeSavedInDictionaryManagementReachesTheAnswersMappings() throws Exception {
        save(null, "Positive (LOINC IT)", "LA6576-8");
        String id = answerId();
        assertEquals(Map.of("LA6576-8", "SAME_AS"), loincMappings(id));

        mappingService.saveMappingsForDictionary(id,
                List.of(mapping("LA6576-8", "SAME_AS"), mapping("LA9633-4", "BROADER_THAN")), "1");

        save(id, "Positive (LOINC IT, renamed)", "LA6576-8");
        assertEquals("a save that keeps the LOINC code leaves the mappings alone",
                Map.of("LA6576-8", "SAME_AS", "LA9633-4", "BROADER_THAN"), loincMappings(id));

        save(id, "Positive (LOINC IT, renamed)", "LA6577-6");
        assertEquals(Map.of("LA6577-6", "SAME_AS"), loincMappings(id));
    }

    @Test
    public void anAnswerWhoseLoincMappingCannotBeSavedIsNotSavedEither() {
        Dictionary answer = new Dictionary();
        answer.setDictEntry("Positive (LOINC IT, refused)");
        answer.setLocalAbbreviation("POSRF");
        answer.setIsActive("Y");
        answer.setSortOrder(1);
        answer.setLoincCode("LA6576-8");
        answer.setSysUserId("1");
        answer.setDictionaryCategory(categoryService.get(String.valueOf(CATEGORY_ID)));
        // The database refuses the mapping row, so only a shared transaction keeps
        // the answer out as well.
        jdbc.execute("CREATE OR REPLACE FUNCTION clinlims.refuse_mapping_it() RETURNS trigger AS $$"
                + " BEGIN RAISE EXCEPTION 'mapping refused'; END $$ LANGUAGE plpgsql");
        jdbc.execute("CREATE TRIGGER refuse_mapping_it BEFORE INSERT ON clinlims.dictionary_terminology_mapping"
                + " FOR EACH ROW EXECUTE FUNCTION clinlims.refuse_mapping_it()");
        try {
            assertThrows(RuntimeException.class, () -> mappingService.saveAnswer(answer, null, false));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS refuse_mapping_it ON clinlims.dictionary_terminology_mapping");
            jdbc.execute("DROP FUNCTION IF EXISTS clinlims.refuse_mapping_it()");
        }

        assertEquals("the answer is rolled back with its mapping", Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.dictionary WHERE dictionary_category_id = ?",
                        Integer.class, CATEGORY_ID));
    }

    private void save(String id, String entry, String loinc) throws Exception {
        String body = "{" + (id == null ? "" : "\"id\":\"" + id + "\",") + "\"selectedDictionaryCategoryId\":\""
                + CATEGORY_ID + "\",\"isActive\":\"Y\",\"dictEntry\":\"" + entry
                + "\",\"localAbbreviation\":\"POSIT\",\"loincCode\":\"" + loinc + "\"}";
        mockMvc.perform(
                post("/rest/Dictionary").contentType(MediaType.APPLICATION_JSON).content(body).session(authedSession()))
                .andExpect(status().isOk());
    }

    private String answerId() {
        return String.valueOf(jdbc.queryForObject("SELECT id FROM clinlims.dictionary WHERE dictionary_category_id = ?",
                Long.class, CATEGORY_ID));
    }

    private Map<String, String> loincMappings(String answerId) {
        return mappingService.getActiveByDictionaryId(answerId).stream().filter(m -> "LOINC".equals(m.getSource()))
                .collect(Collectors.toMap(DictionaryTerminologyMapping::getCode,
                        DictionaryTerminologyMapping::getRelationship));
    }

    private static DictionaryTerminologyMapping mapping(String code, String relationship) {
        DictionaryTerminologyMapping mapping = new DictionaryTerminologyMapping();
        mapping.setSource("LOINC");
        mapping.setCode(code);
        mapping.setRelationship(relationship);
        return mapping;
    }

    private static MockHttpSession authedSession() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return session;
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.dictionary_terminology_mapping WHERE dictionary_id IN"
                + " (SELECT id FROM clinlims.dictionary WHERE dictionary_category_id = ?)", CATEGORY_ID);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dictionary_category_id = ?", CATEGORY_ID);
        jdbc.update("DELETE FROM clinlims.dictionary_category WHERE id = ?", CATEGORY_ID);
    }
}
