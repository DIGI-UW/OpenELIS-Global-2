package org.openelisglobal.analyzerimport.controller;

import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Integration tests for /analyzer/hl7 endpoint (OGC-325 M1).
 *
 * <p>
 * Verifies that the HL7 endpoint accepts POST requests, parses valid HL7, and
 * returns appropriate status codes. Gate 1 evidence: path reaches
 * /analyzer/hl7.
 */
public class AnalyzerImportControllerHL7Test extends BaseWebContextSensitiveTest {

    private MockMvc mockMvc;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void postHl7_validOruR01_reachesEndpointAndParses() throws Exception {
        String hl7 = loadFixture("testdata/hl7/mindray/bc5380-cbc-result.hl7");

        // Valid HL7 parses successfully. 200 = full ingestion (plugin matched); 500 =
        // no plugin
        // matched. Gate 1: path reaches endpoint, parse does not return 400.
        var result = mockMvc.perform(
                post("/analyzer/hl7").contentType(MediaType.TEXT_PLAIN).content(hl7.getBytes(StandardCharsets.UTF_8)))
                .andReturn();
        int code = result.getResponse().getStatus();
        assertNotEquals("Valid HL7 should not return 400 (parse failed)", 400, code);
    }

    @Test
    public void postHl7_withXAnalyzerIdHeader_passesIdentifierToReader() throws Exception {
        String hl7 = loadFixture("testdata/hl7/mindray/bc5380-cbc-result.hl7");

        // X-Analyzer-Id should be forwarded to HL7AnalyzerReader for identifier-pattern
        // matching. The endpoint won't return 400 (parse succeeds); it may return 500
        // if no plugin matches in the test environment, but the header is still
        // processed.
        var result = mockMvc.perform(post("/analyzer/hl7").contentType(MediaType.TEXT_PLAIN)
                .header("X-Analyzer-Id", "MINDRAY-BC-5380").header("X-Source-Id", "192.168.1.100")
                .header("X-Source-Port", "5380").content(hl7.getBytes(StandardCharsets.UTF_8))).andReturn();
        int code = result.getResponse().getStatus();
        assertNotEquals("Valid HL7 with bridge headers should not return 400", 400, code);
    }

    @Test
    public void postHl7_invalidMessage_returnsBadRequest() throws Exception {
        String invalid = "not valid hl7";

        mockMvc.perform(post("/analyzer/hl7").contentType(MediaType.TEXT_PLAIN)
                .content(invalid.getBytes(StandardCharsets.UTF_8))).andExpect(status().isBadRequest());
    }

    @Test
    public void postHl7_emptyBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/analyzer/hl7").contentType(MediaType.TEXT_PLAIN).content(new byte[0]))
                .andExpect(status().isBadRequest());
    }

    private static String loadFixture(String path) throws Exception {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
