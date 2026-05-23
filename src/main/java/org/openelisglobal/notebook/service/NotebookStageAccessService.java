package org.openelisglobal.notebook.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.constants.rbac.AHRIRoleCatalog;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookStageAction;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Department ownership first, SRS persona + stage action second.
 * Project/notebook metadata does not grant department ownership.
 */
@Service
public class NotebookStageAccessService {

    @Autowired
    private WorkflowRegistryService workflowRegistryService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private DepartmentIsolationService departmentIsolationService;

    @Transactional(readOnly = true)
    public void assertActiveDepartment(HttpServletRequest request) {
        if (isUnrestricted(request)) {
            return;
        }
        if (departmentIsolationService.getRestrictedUserTestSectionIds(request).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Select an active department first");
        }
    }

    @Transactional(readOnly = true)
    public void assertNotebookWorkflowAccess(HttpServletRequest request, NoteBook notebook) {
        assertActiveDepartment(request);
        if (notebook == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notebook is required");
        }
        departmentIsolationService.assertNotebookDepartmentAccess(request, notebook);
    }

    @Transactional(readOnly = true)
    public void assertStageAccessForPageId(HttpServletRequest request, Integer pageId, NotebookStageAction action) {
        NoteBookPage page = requirePage(pageId);
        NoteBook notebook = requireNotebook(page);
        assertStageAccess(request, notebook, page, action);
    }

    @Transactional(readOnly = true)
    public void assertPersonaForPage(HttpServletRequest request, NoteBook notebook, Integer pageId) {
        NoteBookPage page = requirePage(pageId);
        assertStageAccess(request, notebook, page, NotebookStageAction.COMPLETE);
    }

    @Transactional(readOnly = true)
    public void assertStageAccess(HttpServletRequest request, NoteBook notebook, NoteBookPage page,
            NotebookStageAction action) {
        assertNotebookWorkflowAccess(request, notebook);
        if (isUnrestricted(request)) {
            return;
        }

        String pageKey = NotebookPageKeyResolver.resolvePageKey(page);
        int order = page.getOrder() != null ? page.getOrder() : 0;
        String workflowType = notebook.getWorkflowType();

        if (action != null && !workflowRegistryService.isActionPermitted(workflowType, pageKey, order, action)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Action " + action + " is not permitted for this workflow stage");
        }

        List<String> allowedPersonas = resolveAllowedPersonas(notebook, page, action);
        if (allowedPersonas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No SRS personas configured for this workflow stage");
        }

        Set<String> userPersonas = getUserDepartmentPersonaNames(request,
                departmentIsolationService.getSysUserId(request));
        boolean allowed = allowedPersonas.stream().anyMatch(
                persona -> userPersonas.contains(AHRIRoleCatalog.normalizeRoleName(persona)));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Insufficient lab role for this workflow stage");
        }
    }

    private List<String> resolveAllowedPersonas(NoteBook notebook, NoteBookPage page, NotebookStageAction action) {
        Set<String> pageRoles = page.getAllowedRoles();
        List<String> explicit = pageRoles == null ? List.of()
                : pageRoles.stream().filter(AHRIRoleCatalog::isDepartmentRoleName).collect(Collectors.toList());

        String workflowType = notebook.getWorkflowType();
        String pageKey = NotebookPageKeyResolver.resolvePageKey(page);
        int order = page.getOrder() != null ? page.getOrder() : 0;

        return workflowRegistryService.resolveAllowedPersonasForAction(workflowType, pageKey, order, order, action,
                explicit);
    }

    private NoteBookPage requirePage(Integer pageId) {
        NoteBookPage page = noteBookPageService.get(pageId);
        if (page == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook page not found");
        }
        return page;
    }

    private NoteBook requireNotebook(NoteBookPage page) {
        NoteBook notebook = page.getNotebook();
        if (notebook == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notebook page has no parent notebook");
        }
        return notebook;
    }

    private Set<String> getUserDepartmentPersonaNames(HttpServletRequest request, String sysUserId) {
        Set<String> names = new HashSet<>();
        if (sysUserId == null) {
            return names;
        }

        addDepartmentPersonaNames(names, userRoleService.getRoleIdsForUser(sysUserId));

        UserLabUnitRoles labRoles = userRoleService.getUserLabUnitRoles(sysUserId);
        if (labRoles == null || labRoles.getLabUnitRoleMap() == null) {
            return names;
        }

        String activeLabUnit = departmentIsolationService.getLoginLabUnit(request);
        for (LabUnitRoleMap map : labRoles.getLabUnitRoleMap()) {
            if (map == null || map.getRoles() == null || map.getLabUnit() == null) {
                continue;
            }
            String mapped = map.getLabUnit().trim();
            // AllLabUnits is for admin role assignment only — not an owning department scope.
            if ("AllLabUnits".equalsIgnoreCase(mapped)) {
                continue;
            }
            if (activeLabUnit != null && activeLabUnit.equalsIgnoreCase(mapped)) {
                addDepartmentPersonaNames(names, map.getRoles());
            }
        }
        return names;
    }

    private void addDepartmentPersonaNames(Set<String> names, Collection<String> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (String roleId : roleIds) {
            if (roleId == null || roleId.isBlank()) {
                continue;
            }
            Role role = roleService.getRoleById(roleId);
            if (role != null && AHRIRoleCatalog.isDepartmentRoleName(role.getName())) {
                names.add(AHRIRoleCatalog.normalizeRoleName(role.getName()));
            }
        }
    }

    private boolean isUnrestricted(HttpServletRequest request) {
        return departmentIsolationService.hasUnrestrictedDepartmentAccess(request);
    }
}
