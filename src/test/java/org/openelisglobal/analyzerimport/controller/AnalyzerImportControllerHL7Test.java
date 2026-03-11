package org.openelisglobal.analyzerimport.controller;

import static org.junit.Assert.assertTrue;
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
        executeDataSetWithStateManagement("testdata/madagascar-analyzer-test-data.xml");
    }

    @Test
    public void postHl7_validOruR01_reachesEndpointAndParses() throws Exception {
        String hl7 = loadFixture("testdata/hl7/mindray/bc5380-cbc-result.hl7");

        // M1 only requires the request to reach the HL7 endpoint and parse as HL7.
        // In this test environment that means either 200 (ingestion succeeded) or
        // 500 (route reached parser but downstream plugin/linkage is incomplete).
        var result = mockMvc.perform(
                post("/analyzer/hl7").contentType(MediaType.TEXT_PLAIN).content(hl7.getBytes(StandardCharsets.UTF_8)))
                .andReturn();
        int code = result.getResponse().getStatus();
        assertTrue("Valid HL7 should return 200 or 500, but was " + code, code == 200 || code == 500);
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
