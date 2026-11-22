package org.openelisglobal.orderentry.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * OGC-1424 (FR-B20): a test on an order can be marked tested elsewhere with the
 * performing laboratory and the reported value, read back with the laboratory's
 * name, and unmarked; a test that is not on the order, an unknown laboratory or
 * an over-long value is refused with a reason.
 */
public class TestedElsewhereRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String URL = "/rest/order-tests/tested-elsewhere";
    private static final String LAB_NUMBER = "TEW0000000001";
    private static final int SAMPLE = 987_700;
    private static final int REQUEST = 987_700;
    private static final int ORDERED_TEST = 987_700;
    private static final int OTHER_TEST = 987_701;
    private static final int LAB = 987_700;

    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void seed() {
        jdbc = new JdbcTemplate(dataSource);
        cleanUp();
        for (int testId : new int[] { ORDERED_TEST, OTHER_TEST }) {
            jdbc.update(
                    "INSERT INTO clinlims.test (id, name, description, guid, is_active, lastupdated) "
                            + "VALUES (?, ?, ?, ?, 'Y', now())",
                    testId, "TEW test " + testId, "TEW test " + testId, "tew-guid-" + testId);
        }
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, lastupdated) "
                + "VALUES (?, ?, now(), now(), now())", SAMPLE, LAB_NUMBER);
        Integer typeOfSample = jdbc.queryForObject("SELECT min(id) FROM clinlims.type_of_sample", Integer.class);
        jdbc.update("INSERT INTO clinlims.sample_type_request (id, sample_id, type_of_sample_id, requested_tests) "
                + "VALUES (?, ?, ?, ?)", REQUEST, SAMPLE, typeOfSample, String.valueOf(ORDERED_TEST));
        jdbc.update("INSERT INTO clinlims.organization (id, name, is_active, lastupdated) "
                + "VALUES (?, 'TEW Reference Laboratory', 'Y', now())", LAB);
    }

    @After
    public void cleanUp() {
        JdbcTemplate template = jdbc == null ? new JdbcTemplate(dataSource) : jdbc;
        template.update("DELETE FROM clinlims.order_test_tested_elsewhere WHERE sample_id = ?", SAMPLE);
        template.update("DELETE FROM clinlims.sample_type_request WHERE id = ?", REQUEST);
        template.update("DELETE FROM clinlims.sample WHERE id = ?", SAMPLE);
        template.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", ORDERED_TEST, OTHER_TEST);
        template.update("DELETE FROM clinlims.organization WHERE id = ?", LAB);
    }

    @Test
    public void aTestMarkedTestedElsewhereReadsBackWithTheLaboratoryAndValue() throws Exception {
        expect(put(URL).contentType(MediaType.APPLICATION_JSON)
                .content(body(String.valueOf(ORDERED_TEST), String.valueOf(LAB), " 13.2 g/dL ")), 200);

        List<Map<String, Object>> marks = marks();
        assertEquals(1, marks.size());
        assertEquals(String.valueOf(ORDERED_TEST), marks.get(0).get("testId"));
        assertEquals("TEW Reference Laboratory", marks.get(0).get("performingLabName"));
        assertEquals("13.2 g/dL", marks.get(0).get("reportedValue"));
    }

    @Test
    public void markingAgainUpdatesTheSameRow() throws Exception {
        expect(put(URL).contentType(MediaType.APPLICATION_JSON).content(body(String.valueOf(ORDERED_TEST), "", "")),
                200);
        expect(put(URL).contentType(MediaType.APPLICATION_JSON)
                .content(body(String.valueOf(ORDERED_TEST), String.valueOf(LAB), "Positive")), 200);

        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.order_test_tested_elsewhere WHERE sample_id = ?",
                        Integer.class, SAMPLE));
        assertEquals("Positive", marks().get(0).get("reportedValue"));
    }

    @Test
    public void unmarkingRemovesTheMark() throws Exception {
        expect(put(URL).contentType(MediaType.APPLICATION_JSON)
                .content(body(String.valueOf(ORDERED_TEST), String.valueOf(LAB), "Positive")), 200);

        expect(delete(URL).param("labNumber", LAB_NUMBER).param("testId", String.valueOf(ORDERED_TEST)), 200);

        assertTrue(marks().isEmpty());
    }

    @Test
    public void aTestThatIsNotOnTheOrderIsRefused() throws Exception {
        expect(put(URL).contentType(MediaType.APPLICATION_JSON)
                .content(body(String.valueOf(OTHER_TEST), String.valueOf(LAB), "Positive")), 400);

        assertTrue(marks().isEmpty());
    }

    @Test
    public void anUnknownLaboratoryOrAnOverLongValueIsRefused() throws Exception {
        expect(put(URL).contentType(MediaType.APPLICATION_JSON)
                .content(body(String.valueOf(ORDERED_TEST), "999999999", "Positive")), 400);
        expect(put(URL).contentType(MediaType.APPLICATION_JSON)
                .content(body(String.valueOf(ORDERED_TEST), String.valueOf(LAB), "x".repeat(256))), 400);

        assertTrue(marks().isEmpty());
    }

    @Test
    public void anUnknownOrderIsRefused() throws Exception {
        expect(get(URL).param("labNumber", "NO-SUCH-ORDER"), 400);
    }

    private String expect(MockHttpServletRequestBuilder request, int status) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
        assertEquals(response.getContentAsString(), status, response.getStatus());
        return response.getContentAsString();
    }

    private String body(String testId, String labId, String value) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("labNumber", LAB_NUMBER);
        body.put("testId", testId);
        body.put("performingLabId", labId);
        body.put("reportedValue", value);
        return json.writeValueAsString(body);
    }

    private List<Map<String, Object>> marks() throws Exception {
        String response = expect(get(URL).param("labNumber", LAB_NUMBER), 200);
        return json.readValue(response, new TypeReference<List<Map<String, Object>>>() {
        });
    }
}
