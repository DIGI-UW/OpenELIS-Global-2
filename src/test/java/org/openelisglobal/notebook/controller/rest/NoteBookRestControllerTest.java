package org.openelisglobal.notebook.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class NoteBookRestControllerTest extends BaseWebContextSensitiveTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private NoteBookService noteBookService;

    private UserSessionData userSessionData;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/dictionary.xml");
        executeDataSetWithStateManagement("testdata/notebook-test-data.xml");

        userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
    }

    @Test
    public void getDashboardMetrics_shouldReturnAggregatedCounts() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/notebook/dashboard/metrics").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();

        Map<?, ?> metrics = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertNotNull("Metrics response should deserialize", metrics);
        assertTrue("Metrics should include total", metrics.containsKey("total"));
        assertTrue("Metrics should include drafts", metrics.containsKey("drafts"));
        assertTrue("Metrics should include pending", metrics.containsKey("pending"));
    }

    @Test
    public void updateNoteBookStatus_shouldPersistStatusChange() throws Exception {
        NoteBook before = noteBookService.get(2);
        assertNotNull("Fixture precondition: notebook id=2 should exist", before);
        assertEquals("Fixture precondition: initial status should be DRAFT", NoteBookStatus.DRAFT, before.getStatus());

        mockMvc.perform(post("/rest/notebook/updatestatus/2").param("status", "SUBMITTED")
                .sessionAttr("userSessionData", userSessionData).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        NoteBook after = noteBookService.get(2);
        assertEquals("Status update endpoint should persist notebook status", NoteBookStatus.SUBMITTED,
                after.getStatus());
    }

    @Test
    public void getNoteBookAuditTrail_shouldReturnAuditPayloadForExistingNotebook() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/rest/notebook/auditTrail").param("notebookId", "2").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();

        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertNotNull("Audit trail response should deserialize", response);
        assertTrue("Audit trail response should be a valid payload", response.isEmpty() || response.containsKey("log"));
    }
}
