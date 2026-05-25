package org.openelisglobal.notebook.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookStageAction;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin, restricted department, no active department, and SRS persona scenarios.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotebookStageAccessRbacMatrixTest {

    @Mock
    private WorkflowRegistryService workflowRegistryService;

    @Mock
    private NoteBookPageService noteBookPageService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private DepartmentIsolationService departmentIsolationService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private NotebookStageAccessService service;

    private NoteBook notebook;
    private NoteBookPage page;

    @Before
    public void setUp() {
        notebook = new NoteBook();
        notebook.setWorkflowType("biorepository");
        page = new NoteBookPage();
        page.setId(10);
        page.setPageId("intake");
        page.setOrder(1);
        page.setNotebook(notebook);
    }

    @Test
    public void admin_unrestricted_skipsPersonaCheck() {
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(request)).thenReturn(true);
        doNothing().when(departmentIsolationService).assertNotebookDepartmentAccess(any(), any());

        service.assertStageAccess(request, notebook, page, NotebookStageAction.EDIT);
    }

    @Test
    public void noActiveDepartment_denied() {
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(request)).thenReturn(false);
        when(departmentIsolationService.getRestrictedUserTestSectionIds(request)).thenReturn(Set.of());

        assertThrows(ResponseStatusException.class,
                () -> service.assertActiveDepartment(request));
    }

    @Test
    public void restrictedUser_wrongPersona_denied() {
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(request)).thenReturn(false);
        when(departmentIsolationService.getRestrictedUserTestSectionIds(request)).thenReturn(Set.of(5));
        doNothing().when(departmentIsolationService).assertNotebookDepartmentAccess(any(), any());
        when(departmentIsolationService.getSysUserId(request)).thenReturn("42");
        when(workflowRegistryService.isActionPermitted("biorepository", "intake", 1, NotebookStageAction.EDIT))
                .thenReturn(true);
        when(workflowRegistryService.resolveAllowedPersonasForAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of("Lab Manager"));
        when(userRoleService.getRoleIdsForUser("42")).thenReturn(List.of("r1"));
        Role tech = new Role();
        tech.setName("Laboratory Technician");
        when(roleService.getRoleById("r1")).thenReturn(tech);
        when(userRoleService.getUserLabUnitRoles("42")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> service.assertStageAccess(request, notebook, page, NotebookStageAction.EDIT));
    }

    @Test
    public void sampleCollector_allowedOnIntake() {
        when(departmentIsolationService.hasUnrestrictedDepartmentAccess(request)).thenReturn(false);
        when(departmentIsolationService.getRestrictedUserTestSectionIds(request)).thenReturn(Set.of(5));
        doNothing().when(departmentIsolationService).assertNotebookDepartmentAccess(any(), any());
        when(departmentIsolationService.getSysUserId(request)).thenReturn("42");
        when(workflowRegistryService.isActionPermitted("biorepository", "intake", 1, NotebookStageAction.EDIT))
                .thenReturn(true);
        when(workflowRegistryService.resolveAllowedPersonasForAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of("Sample Collector", "Laboratory Technician"));
        when(userRoleService.getRoleIdsForUser("42")).thenReturn(List.of("r1"));
        Role collector = new Role();
        collector.setName("Sample Collector");
        when(roleService.getRoleById("r1")).thenReturn(collector);
        when(userRoleService.getUserLabUnitRoles("42")).thenReturn(null);

        service.assertStageAccess(request, notebook, page, NotebookStageAction.EDIT);
    }
}
