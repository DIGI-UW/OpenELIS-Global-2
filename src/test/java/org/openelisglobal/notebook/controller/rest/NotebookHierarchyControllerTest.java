package org.openelisglobal.notebook.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for Notebook Hierarchy REST endpoints.
 *
 * Note: These tests focus on endpoint availability and basic response format.
 * Full security and access control is tested through manual/E2E testing. The
 * comprehensive business logic is tested in NotebookHierarchyServiceTest.
 */
public class NotebookHierarchyControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DataSource dataSource;

    private MockMvc mockMvc;
    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/dictionary.xml");
        executeDataSetWithStateManagement("testdata/notebook-test-data.xml");

        // Reset the notebook sequence to avoid ID conflicts with test data
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT setval('clinlims.notebook_seq', 100, false)");
        }

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        session = new MockHttpSession();

        // Set up user session data for authentication
        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);
        usd.setLoginName("admin");
        session.setAttribute("userSessionData", usd);
    }

    // ========== GET /rest/notebook/hierarchy ==========

    @Test
    public void getHierarchy_authenticated_returnsHierarchy() throws Exception {
        // This endpoint returns the hierarchy of parent templates with their children
        mockMvc.perform(get("/rest/notebook/hierarchy").session(session).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    // ========== GET /rest/notebook/{notebookId}/can-accept-entries ==========

    @Test
    public void canAcceptEntries_parentTemplate_returnsFalse() throws Exception {
        // Parent templates cannot accept entries directly
        mockMvc.perform(
                get("/rest/notebook/1/can-accept-entries").session(session).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canAcceptEntries").value(false));
    }

    // ========== GET /rest/notebook/parent-templates ==========

    @Test
    public void getParentTemplates_authenticated_returnsTemplates() throws Exception {
        mockMvc.perform(get("/rest/notebook/parent-templates").session(session).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }
}
