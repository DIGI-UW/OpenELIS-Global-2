package org.openelisglobal.department.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookSecurityService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentIsolationService {

    @Autowired
    private NotebookSecurityService notebookSecurityService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private UserRoleService userRoleService;

    public String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = getUserSessionData(request);
        return usd != null ? String.valueOf(usd.getSystemUserId()) : null;
    }

    public String getLoginLabUnit(HttpServletRequest request) {
        UserSessionData usd = getUserSessionData(request);
        if (usd == null || usd.getLoginLabUnit() == 0) {
            return null;
        }
        TestSection testSection = testSectionService.getTestSectionById(String.valueOf(usd.getLoginLabUnit()));
        if (testSection == null) {
            return String.valueOf(usd.getLoginLabUnit());
        }
        if (testSection.getTestSectionName() != null && !testSection.getTestSectionName().isBlank()) {
            return testSection.getTestSectionName();
        }
        String localizedName = testSection.getLocalizedName();
        return localizedName != null && !localizedName.isBlank() ? localizedName : String.valueOf(usd.getLoginLabUnit());
    }

    public boolean hasUnrestrictedDepartmentAccess(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return false;
        }
        return notebookSecurityService.hasGlobalAdminRole(sysUserId) || hasAllLabUnitsAccess(sysUserId);
    }

    @Transactional(readOnly = true)
    public boolean canAccessInventoryItem(InventoryItem item, HttpServletRequest request) {
        if (item == null) {
            return false;
        }
        if (hasUnrestrictedDepartmentAccess(request)) {
            return true;
        }
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);
        String projectName = item.getProjectName();
        if (sysUserId == null || loginLabUnit == null || projectName == null || projectName.isBlank()) {
            return false;
        }
        NoteBook notebook = findNotebookForProject(projectName.trim());
        return notebook != null && notebookSecurityService.canViewTemplate(notebook.getId(), sysUserId, loginLabUnit);
    }

    @Transactional(readOnly = true)
    public boolean canAccessSampleItem(SampleItem sampleItem, HttpServletRequest request) {
        if (sampleItem == null) {
            return false;
        }
        if (hasUnrestrictedDepartmentAccess(request)) {
            return true;
        }
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);
        if (sysUserId == null || loginLabUnit == null) {
            return false;
        }
        List<NotebookEntry> entries = notebookEntryService.findBySampleItemId(Integer.valueOf(sampleItem.getId()));
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        return entries.stream().anyMatch(entry -> notebookSecurityService.canViewEntry(entry, sysUserId, loginLabUnit));
    }

    private UserSessionData getUserSessionData(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            usd = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
        }
        return usd;
    }

    private boolean hasAllLabUnitsAccess(String sysUserId) {
        UserLabUnitRoles userLabRoles = userRoleService.getUserLabUnitRoles(sysUserId);
        return userLabRoles != null && userLabRoles.getLabUnitRoleMap() != null
                && userLabRoles.getLabUnitRoleMap().stream()
                        .anyMatch(roleMap -> "AllLabUnits".equalsIgnoreCase(roleMap.getLabUnit()));
    }

    private NoteBook findNotebookForProject(String projectName) {
        if (projectName.matches("\\d+")) {
            NoteBook byId = noteBookService.get(Integer.valueOf(projectName));
            if (byId != null) {
                return byId;
            }
        }
        List<NoteBook> matches = noteBookService.getAllMatching("title", projectName);
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }
}
