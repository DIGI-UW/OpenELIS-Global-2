package org.openelisglobal.notebookControllerTest;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NoteBookRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WebApplicationContext wac;

    // spring search for session factory
    @Autowired(required = false)
    private SessionFactory sessionFactory;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void testGetDashboard_ShouldReturnMetrics() throws Exception {
        executeDataSetWithStateManagement("NoteBookRestControllerTest-data.xml");

        mockMvc.perform(get("/rest/notebook/dashboard/metrics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdateStatus_ShouldPersistInDb() throws Exception {
        executeDataSetWithStateManagement("NoteBookRestControllerTest-data.xml");

        mockMvc.perform(post("/rest/notebook/updatestatus/1")
                        .param("status", "SUBMITTED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // check database
        // If the sessionFactory is still null, we will use the Bean from the Context in a generic way
        SessionFactory sf = (sessionFactory != null) ? sessionFactory : (SessionFactory) wac.getBean(SessionFactory.class);

        Session session = sf.openSession();
        try {
            NoteBook updatedNoteBook = (NoteBook) session.get(NoteBook.class, 1);
            assertEquals(NoteBookStatus.SUBMITTED, updatedNoteBook.getStatus());
        } finally {
            session.close();
        }
    }

    @Test
    public void testGetAuditTrail_ShouldReturnData() throws Exception {
        executeDataSetWithStateManagement("NoteBookRestControllerTest-data.xml");

        mockMvc.perform(get("/rest/notebook/auditTrail")
                        .param("notebookId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}