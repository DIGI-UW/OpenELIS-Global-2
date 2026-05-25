package org.openelisglobal.notebook.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookSecurityService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class NoteBookRestControllerDepartmentsTest {

    @InjectMocks
    private NoteBookRestController controller;

    @Mock
    private DepartmentIsolationService departmentIsolationService;

    @Mock
    private NoteBookService noteBookService;

    @Mock
    private NotebookSecurityService notebookSecurityService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void departmentsEndpointReturnsAssignableWorkflowDepartments() throws Exception {
        when(departmentIsolationService.getAssignableWorkflowDepartments(any()))
                .thenReturn(List.of(Map.of("id", "7", "name", "Pathology", "shortName", "Pathology")));

        mockMvc.perform(get("/rest/notebook/departments").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("7"))
                .andExpect(jsonPath("$[0].name").value("Pathology"));
    }
}
