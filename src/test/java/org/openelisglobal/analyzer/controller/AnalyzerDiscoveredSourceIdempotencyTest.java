package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseCommittedFixtureTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Idempotency of {@code POST /rest/analyzer/discovered-sources} belongs on the
 * committed base, not the rollback base. It exercises TWO separate requests:
 * the production handler relies on a try-insert / catch-duplicate-key /
 * re-query pattern that only works because each request runs in its own
 * transaction (the first insert commits before the second runs, so the second
 * can catch the unique-constraint violation and look the stub up). Under
 * per-test rollback the two POSTs share one Hibernate session, so the second
 * insert's constraint violation poisons it and the re-query throws instead of
 * returning the existing stub. {@code NOT_SUPPORTED} gives each service call
 * its own committed transaction, matching production.
 */
public class AnalyzerDiscoveredSourceIdempotencyTest extends BaseCommittedFixtureTest {

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUpIdempotency() {
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        AnalyzerTestCleanup.clean(jdbcTemplate);
    }

    @After
    public void cleanupCommittedAnalyzers() {
        // The POSTs commit analyzer rows on this NOT_SUPPORTED base; remove the
        // TEST-/Unknown- analyzers (and resync the sequence) so they don't leak.
        AnalyzerTestCleanup.clean(jdbcTemplate);
    }

    @Test
    public void discoveredSources_duplicateSourceId_isIdempotent() throws Exception {
        String srcId = AnalyzerTestCleanup.uniqueSourceId();
        String body = "{\"sourceId\":\"" + srcId + "\",\"protocol\":\"HL7\",\"transport\":\"MLLP\"}";

        // First call — creates the stub.
        MvcResult first = mockMvc
                .perform(
                        post("/rest/analyzer/discovered-sources").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        Map<String, Object> firstResponse = objectMapper.readValue(first.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        String firstId = String.valueOf(firstResponse.get("analyzerId"));

        // Second call — duplicate sourceId returns the existing stub.
        MvcResult second = mockMvc
                .perform(
                        post("/rest/analyzer/discovered-sources").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        Map<String, Object> secondResponse = objectMapper.readValue(second.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals("Same analyzer ID on duplicate", firstId, String.valueOf(secondResponse.get("analyzerId")));
        assertEquals(true, secondResponse.get("alreadyExists"));
    }
}
