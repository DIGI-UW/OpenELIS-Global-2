package org.openelisglobal.orderentry.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * OGC-1424 (FR-B6a, D-216): Create on a new patient, facility or provider first
 * lists up to five existing records that look like it, against the real
 * database and its pg_trgm and fuzzystrmatch functions, and Create new anyway
 * is recorded.
 */
public class PossibleMatchRestControllerTest extends BaseWebContextSensitiveTest {

    private static final int PERSON = 987_650;
    private static final int PATIENT = 987_650;
    private static final int PROVIDER = 987_660;
    private static final int ORGANIZATION = 987_670;

    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void seed() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        cleanUp();
        person(PERSON, "Kilavuz", "Moreaqx", null);
        jdbc.update("INSERT INTO clinlims.patient (id, person_id, birth_date, national_id, lastupdated) "
                + "VALUES (?, ?, '1985-03-02', 'PNG8812345', now())", PATIENT, PERSON);
        person(PERSON + 1, "Agnesz", "Tauvoq", "Dr");
        jdbc.update("INSERT INTO clinlims.provider (id, person_id, active, lastupdated) VALUES (?, ?, true, now())",
                PROVIDER, PERSON + 1);
        jdbc.update("INSERT INTO clinlims.organization (id, name, code, is_active, lastupdated) "
                + "VALUES (?, 'Tokaravu Health Centre', 'TKV1', 'Y', now())", ORGANIZATION);
    }

    @After
    public void cleanUp() {
        JdbcTemplate template = jdbc == null ? new JdbcTemplate(dataSource) : jdbc;
        template.update("DELETE FROM clinlims.possible_match_override WHERE entered_values LIKE '%Moreaqx%'");
        template.update("DELETE FROM clinlims.patient WHERE id = ?", PATIENT);
        template.update("DELETE FROM clinlims.provider WHERE id = ?", PROVIDER);
        template.update("DELETE FROM clinlims.person WHERE id IN (?, ?)", PERSON, PERSON + 1);
        template.update("DELETE FROM clinlims.organization WHERE id = ?", ORGANIZATION);
    }

    @Test
    public void aPatientWithFirstAndLastNamesSwappedIsAPossibleMatch() throws Exception {
        List<Map<String, Object>> matches = matches(
                get("/rest/possible-matches/patient").param("firstName", "Moreaqx").param("lastName", "Kilavuz"));

        Map<String, Object> match = only(matches, String.valueOf(PATIENT));
        assertEquals(List.of("name"), match.get("matchedOn"));
        assertEquals("1985-03-02", match.get("birthDate"));
    }

    @Test
    public void aMisspelledNameWithABirthDateInTheSameYearMatchesOnBoth() throws Exception {
        List<Map<String, Object>> matches = matches(get("/rest/possible-matches/patient").param("firstName", "Kilavus")
                .param("lastName", "Moreaqz").param("birthDate", "1985-09-30"));

        assertEquals(List.of("name", "dateOfBirth"), only(matches, String.valueOf(PATIENT)).get("matchedOn"));
    }

    @Test
    public void anIdentifierOneCharacterOffMatchesUnderAnotherName() throws Exception {
        List<Map<String, Object>> matches = matches(get("/rest/possible-matches/patient").param("firstName", "Zebedee")
                .param("lastName", "Quorn").param("identifier", "PNG8812346"));

        assertEquals(List.of("identifier"), only(matches, String.valueOf(PATIENT)).get("matchedOn"));
    }

    @Test
    public void anUnrelatedPatientFindsNothing() throws Exception {
        List<Map<String, Object>> matches = matches(
                get("/rest/possible-matches/patient").param("firstName", "Xyzzyq").param("lastName", "Plughwv"));

        assertTrue(matches.stream().noneMatch(m -> String.valueOf(PATIENT).equals(m.get("id"))));
    }

    @Test
    public void aFacilityNamedTheSameApartFromCommonWordsAndSpellingIsAPossibleMatch() throws Exception {
        List<Map<String, Object>> matches = matches(
                get("/rest/possible-matches/facility").param("name", "Tokaravu health center"));

        Map<String, Object> match = only(matches, String.valueOf(ORGANIZATION));
        assertEquals(List.of("name"), match.get("matchedOn"));
        assertEquals("Tokaravu Health Centre", match.get("name"));
    }

    @Test
    public void aProviderWithASimilarNameIsAPossibleMatchWithTheirTitle() throws Exception {
        List<Map<String, Object>> matches = matches(
                get("/rest/possible-matches/provider").param("firstName", "Agnes").param("lastName", "Tauvoq"));

        assertEquals("Dr", only(matches, String.valueOf(PROVIDER)).get("title"));
    }

    @Test
    public void createNewAnywayIsRecordedWithWhatWasEnteredAndWhatWasShown() throws Exception {
        String body = json.writeValueAsString(Map.of("entered", Map.of("firstName", "Moreaqx", "lastName", "Kilavuz"),
                "matches", List.of(Map.of("id", String.valueOf(PATIENT), "matchedOn", List.of("name")))));

        org.springframework.mock.web.MockHttpServletResponse response = mockMvc.perform(
                post("/rest/possible-matches/patient/override").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse();
        assertEquals(response.getContentAsString(), 201, response.getStatus());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT record_kind, entered_values, matches_shown, last_updated FROM clinlims.possible_match_override"
                        + " WHERE entered_values LIKE '%Moreaqx%'");
        assertEquals("patient", row.get("record_kind"));
        assertTrue(String.valueOf(row.get("matches_shown")).contains(String.valueOf(PATIENT)));
        assertTrue(row.get("last_updated") != null);
    }

    @Test
    public void anUnknownRecordKindIsRefused() throws Exception {
        mockMvc.perform(post("/rest/possible-matches/vehicle/override").contentType(MediaType.APPLICATION_JSON)
                .content("{\"entered\":{\"name\":\"Moreaqx\"}}")).andExpect(status().isBadRequest());

        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.possible_match_override WHERE entered_values LIKE '%Moreaqx%'",
                        Integer.class));
    }

    private void person(int id, String firstName, String lastName, String title) {
        jdbc.update("INSERT INTO clinlims.person (id, first_name, last_name, title_code, lastupdated) "
                + "VALUES (?, ?, ?, ?, now())", id, firstName, lastName, title);
    }

    private List<Map<String, Object>> matches(MockHttpServletRequestBuilder request) throws Exception {
        String response = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        Map<String, List<Map<String, Object>>> body = json.readValue(response,
                new TypeReference<Map<String, List<Map<String, Object>>>>() {
                });
        assertTrue("never more than five possible matches", body.get("matches").size() <= 5);
        return body.get("matches");
    }

    private static Map<String, Object> only(List<Map<String, Object>> matches, String id) {
        return matches.stream().filter(m -> id.equals(m.get("id"))).findFirst()
                .orElseThrow(() -> new AssertionError("expected record " + id + " among " + matches));
    }
}
