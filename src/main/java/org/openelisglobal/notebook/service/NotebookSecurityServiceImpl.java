package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of NotebookSecurityService for location-based access control.
 *
 * Access rules: - Admins can do everything - Users see templates assigned to
 * their organization (via loginLabUnit) - Entry creation requires allowed role
 * for user's lab unit - Entries inherit accessible organizations from template
 */
@Service
public class NotebookSecurityServiceImpl implements NotebookSecurityService {

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private RoleService roleService;

    // ========== TEMPLATE ACCESS (Admin Only for Edit) ==========

    @Override
    public boolean canEditTemplate(String sysUserId) {
        // Allow Global Administrator or Notebook Administrator to manage templates
        return hasGlobalAdminRole(sysUserId) || userRoleService.userInRole(sysUserId, Constants.ROLE_NOTEBOOK_ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canViewTemplate(NoteBook template, String sysUserId, String loginLabUnit) {
        // Admin sees all
        if (hasGlobalAdminRole(sysUserId)) {
            return true;
        }

        // Check departments (test sections) first - this is the primary access control
        Set<TestSection> templateDepts = template.getDepartments();
        if (templateDepts != null && !templateDepts.isEmpty()) {
            // If user has no specific lab unit, check AllLabUnits access
            if (loginLabUnit == null || loginLabUnit.isEmpty()) {
                return hasAllLabUnitsAccess(sysUserId);
            }
            // User's loginLabUnit must match one of the template's departments
            return templateDepts.stream().anyMatch(dept -> matchesLoginLabUnitToDepartment(dept, loginLabUnit));
        }

        // Fall back to organizations for backward compatibility
        Set<Organization> templateOrgs = template.getOrganizations();
        if (templateOrgs != null && !templateOrgs.isEmpty()) {
            if (loginLabUnit == null || loginLabUnit.isEmpty()) {
                return hasAllLabUnitsAccess(sysUserId);
            }
            return templateOrgs.stream().anyMatch(org -> matchesLoginLabUnit(org, loginLabUnit));
        }

        // If no departments or organizations are assigned, template is NOT visible by
        // default
        // Strict access control: only show if explicitly allowed
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canViewTemplate(Integer notebookId, String sysUserId, String loginLabUnit) {
        // Admin sees all
        if (hasGlobalAdminRole(sysUserId)) {
            return true;
        }

        try {
            // First check if this is an entry (not a template) - if so, use parent template
            NoteBook notebook = noteBookService.get(notebookId);
            if (notebook == null) {
                return false;
            }

            Integer effectiveNotebookId = notebookId;
            if (Boolean.FALSE.equals(notebook.getIsTemplate())) {
                // This is either a child instance or an entry (not a template)
                // First check if it's a child instance (has parentNotebook)
                NoteBook parentNotebook = notebook.getParentNotebook();
                if (parentNotebook != null) {
                    // This is a child instance - check access via the parent template
                    effectiveNotebookId = parentNotebook.getId();
                    LogEvent.logInfo(this.getClass().getSimpleName(), "canViewTemplate", "NotebookId=" + notebookId
                            + " is a child instance, using parent template id=" + effectiveNotebookId);
                } else {
                    // Not a child instance - try finding parent via entries collection (legacy)
                    NoteBook parent = noteBookService.getParentTemplate(notebookId);
                    if (parent != null) {
                        effectiveNotebookId = parent.getId();
                        LogEvent.logInfo(this.getClass().getSimpleName(), "canViewTemplate", "NotebookId=" + notebookId
                                + " is an entry, using parent template id=" + effectiveNotebookId);
                    } else {
                        // Orphaned entry - allow if user is creator or technician
                        if (notebook.getTechnician() != null
                                && String.valueOf(notebook.getTechnician().getId()).equals(sysUserId)) {
                            return true;
                        }
                        if (notebook.getCreator() != null
                                && String.valueOf(notebook.getCreator().getId()).equals(sysUserId)) {
                            return true;
                        }
                        // No parent and not creator/technician - deny
                        LogEvent.logInfo(this.getClass().getSimpleName(), "canViewTemplate",
                                "NotebookId=" + notebookId + " is an orphaned entry with no parent, access denied");
                        return false;
                    }
                }
            }

            // Check departments (test sections)
            // Using safe fetch method that handles initializing lazy collection
            Set<TestSection> templateDepts = noteBookService.getNoteBookDepartments(effectiveNotebookId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "canViewTemplate",
                    "Checking access for notebookId=" + effectiveNotebookId + ", loginLabUnit=" + loginLabUnit
                            + ", templateDepts count=" + (templateDepts != null ? templateDepts.size() : "null"));

            if (templateDepts != null && !templateDepts.isEmpty()) {
                // Log department names for debugging
                for (TestSection dept : templateDepts) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "canViewTemplate",
                            "Template dept: id=" + dept.getId() + ", name=" + dept.getTestSectionName()
                                    + ", localizedName=" + dept.getLocalizedName());
                }

                // If user has no specific lab unit (loginLabUnit is null/empty),
                // check if they have AllLabUnits access
                if (loginLabUnit == null || loginLabUnit.isEmpty()) {
                    return hasAllLabUnitsAccess(sysUserId);
                }
                // User's loginLabUnit must match one of the template's departments
                boolean matches = templateDepts.stream()
                        .anyMatch(dept -> matchesLoginLabUnitToDepartment(dept, loginLabUnit));
                LogEvent.logInfo(this.getClass().getSimpleName(), "canViewTemplate",
                        "Department match result for loginLabUnit=" + loginLabUnit + ": " + matches);
                return matches;
            }

            // Fall back to organizations
            // Using safe fetch method
            Set<Organization> templateOrgs = noteBookService.getNoteBookOrganizations(effectiveNotebookId);
            if (templateOrgs != null && !templateOrgs.isEmpty()) {
                if (loginLabUnit == null || loginLabUnit.isEmpty()) {
                    return hasAllLabUnitsAccess(sysUserId);
                }
                return templateOrgs.stream().anyMatch(org -> matchesLoginLabUnit(org, loginLabUnit));
            }

            // If no departments or organizations are assigned, template is NOT visible by
            // default
            return false;
        } catch (Exception e) {
            // Log error or handle gracefully if notebook doesn't exist
            return false;
        }
    }

    /**
     * Check if user has access to all lab units (typically supervisors or
     * managers).
     *
     * @param sysUserId the system user ID
     * @return true if user has AllLabUnits access
     */
    private boolean hasAllLabUnitsAccess(String sysUserId) {
        if (sysUserId == null) {
            return false;
        }
        UserLabUnitRoles userLabRoles = userRoleService.getUserLabUnitRoles(sysUserId);
        if (userLabRoles == null || userLabRoles.getLabUnitRoleMap() == null) {
            return false;
        }
        // Check if user has any role assigned to "AllLabUnits"
        return userLabRoles.getLabUnitRoleMap().stream()
                .anyMatch(roleMap -> "AllLabUnits".equalsIgnoreCase(roleMap.getLabUnit()));
    }

    // ========== ENTRY ACCESS (Role + Location Based) ==========

    @Override
    @Transactional(readOnly = true)
    public boolean canCreateEntry(NoteBook template, String sysUserId, String loginLabUnit) {
        // Admin can create entries on any template
        if (hasGlobalAdminRole(sysUserId)) {
            return true;
        }

        // Must have access to template's organization
        if (!canViewTemplate(template, sysUserId, loginLabUnit)) {
            return false;
        }

        // Must have one of the allowed roles for their lab unit
        Set<String> allowedRoles = template.getAllowedRoles();

        // If no roles configured, allow any authenticated user with org access
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return true;
        }

        return hasRequiredRoleForLabUnit(sysUserId, loginLabUnit, allowedRoles);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCreateEntry(Integer notebookId, String sysUserId, String loginLabUnit) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "canCreateEntry", "Checking canCreateEntry for notebookId="
                + notebookId + ", sysUserId=" + sysUserId + ", loginLabUnit=" + loginLabUnit);

        // Admin can create entries on any template
        if (hasGlobalAdminRole(sysUserId)) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "canCreateEntry",
                    "User is admin, allowing entry creation");
            return true;
        }

        // Must have access to template (uses safe Integer-based method)
        boolean canView = canViewTemplate(notebookId, sysUserId, loginLabUnit);
        LogEvent.logInfo(this.getClass().getSimpleName(), "canCreateEntry",
                "canViewTemplate result=" + canView + " for notebookId=" + notebookId);
        if (!canView) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "canCreateEntry",
                    "User cannot view template, denying entry creation");
            return false;
        }

        // Must have one of the allowed roles for their lab unit
        // Use safe method that initializes lazy collection within transaction
        Set<String> allowedRoles = noteBookService.getNoteBookAllowedRoles(notebookId);
        LogEvent.logInfo(this.getClass().getSimpleName(), "canCreateEntry",
                "Template allowedRoles=" + (allowedRoles != null ? allowedRoles : "null"));

        // If no roles configured, allow any authenticated user with org access
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "canCreateEntry",
                    "No roles configured for template, allowing entry creation");
            return true;
        }

        boolean hasRole = hasRequiredRoleForLabUnit(sysUserId, loginLabUnit, allowedRoles);
        LogEvent.logInfo(this.getClass().getSimpleName(), "canCreateEntry",
                "hasRequiredRoleForLabUnit result=" + hasRole);
        return hasRole;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canViewEntry(NotebookEntry entry, String sysUserId, String loginLabUnit) {
        // Admin sees all
        if (hasGlobalAdminRole(sysUserId)) {
            return true;
        }

        // Check if user's loginLabUnit matches entry's accessible organizations
        Set<Organization> accessibleOrgs = entry.getAccessibleOrganizations();

        // If no accessible orgs defined, check primary organization
        if (accessibleOrgs == null || accessibleOrgs.isEmpty()) {
            Organization primaryOrg = entry.getOrganization();
            if (primaryOrg != null) {
                return matchesLoginLabUnit(primaryOrg, loginLabUnit);
            }
            // No org restrictions - allow access (backward compatibility)
            return true;
        }

        return accessibleOrgs.stream().anyMatch(org -> matchesLoginLabUnit(org, loginLabUnit));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canViewEntry(Integer entryId, String sysUserId, String loginLabUnit) {
        NotebookEntry entry = notebookEntryService.get(entryId);
        if (entry == null) {
            return false;
        }
        return canViewEntry(entry, sysUserId, loginLabUnit);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditEntry(NotebookEntry entry, String sysUserId, String loginLabUnit) {
        // Must be able to view first
        if (!canViewEntry(entry, sysUserId, loginLabUnit)) {
            return false;
        }

        // Admin can always edit
        if (hasGlobalAdminRole(sysUserId)) {
            return true;
        }

        // Check if user has allowed role for their lab unit
        NoteBook template = entry.getNotebook();
        if (template == null) {
            return false;
        }

        Set<String> allowedRoles = template.getAllowedRoles();
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return true;
        }

        return hasRequiredRoleForLabUnit(sysUserId, loginLabUnit, allowedRoles);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditEntry(Integer entryId, String sysUserId, String loginLabUnit) {
        NotebookEntry entry = notebookEntryService.get(entryId);
        if (entry == null) {
            return false;
        }
        return canEditEntry(entry, sysUserId, loginLabUnit);
    }

    // ========== PAGE ACCESS (Role Based) ==========

    @Override
    @Transactional(readOnly = true)
    public boolean canViewPage(NoteBookPage page, String sysUserId, String loginLabUnit) {
        // Admin can view any page
        if (hasGlobalAdminRole(sysUserId)) {
            return true;
        }

        // Notebook Admin can view any page (for template management)
        if (userRoleService.userInRole(sysUserId, Constants.ROLE_NOTEBOOK_ADMIN)) {
            return true;
        }

        // If page has no role restrictions, anyone can view it
        Set<String> allowedRoles = page.getAllowedRoles();
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return true;
        }

        // Check if user has one of the allowed roles for their lab unit
        return hasRequiredRoleForLabUnit(sysUserId, loginLabUnit, allowedRoles);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canViewPage(Integer pageId, String sysUserId, String loginLabUnit) {
        NoteBookPage page = noteBookPageService.get(pageId);
        if (page == null) {
            return false;
        }
        return canViewPage(page, sysUserId, loginLabUnit);
    }

    // ========== HELPER METHODS ==========

    @Override
    public boolean hasGlobalAdminRole(String sysUserId) {
        if (sysUserId == null) {
            return false;
        }
        return userRoleService.userInRole(sysUserId, Constants.ROLE_GLOBAL_ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationForLoginLabUnit(String loginLabUnit) {
        if (loginLabUnit == null || loginLabUnit.isEmpty()) {
            return null;
        }

        // Try to find by short name first
        Organization org = organizationService.getOrganizationByShortName(loginLabUnit, true);
        if (org != null) {
            return org;
        }

        // Try by local abbreviation
        Organization searchOrg = new Organization();
        searchOrg.setOrganizationLocalAbbreviation(loginLabUnit);
        org = organizationService.getOrganizationByLocalAbbreviation(searchOrg, true);
        if (org != null) {
            return org;
        }

        // Try by name
        searchOrg = new Organization();
        searchOrg.setOrganizationName(loginLabUnit);
        org = organizationService.getOrganizationByName(searchOrg, true);

        return org;
    }

    @Override
    public boolean matchesLoginLabUnit(Organization organization, String loginLabUnit) {
        if (organization == null || loginLabUnit == null || loginLabUnit.isEmpty()) {
            return false;
        }

        // Match against shortName
        if (loginLabUnit.equalsIgnoreCase(organization.getShortName())) {
            return true;
        }

        // Match against organizationLocalAbbreviation
        if (loginLabUnit.equalsIgnoreCase(organization.getOrganizationLocalAbbreviation())) {
            return true;
        }

        // Match against organizationName
        if (loginLabUnit.equalsIgnoreCase(organization.getOrganizationName())) {
            return true;
        }

        // Match against code
        if (loginLabUnit.equalsIgnoreCase(organization.getCode())) {
            return true;
        }

        return false;
    }

    /**
     * Check if user has any of the required roles for the given lab unit.
     *
     * @param sysUserId     the system user ID
     * @param loginLabUnit  the user's login lab unit (localized name like
     *                      "Cytology")
     * @param requiredRoles the set of roles that grant access (role names like
     *                      "Technician")
     * @return true if user has at least one of the required roles
     */
    private boolean hasRequiredRoleForLabUnit(String sysUserId, String loginLabUnit, Set<String> requiredRoles) {
        if (sysUserId == null || requiredRoles == null || requiredRoles.isEmpty()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "hasRequiredRoleForLabUnit",
                    "Early return: sysUserId=" + sysUserId + ", requiredRoles=" + requiredRoles);
            return false;
        }

        // First check global roles (not lab-unit specific)
        if (userRoleService.userInRole(sysUserId, requiredRoles)) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "hasRequiredRoleForLabUnit",
                    "User has global role matching requiredRoles=" + requiredRoles);
            return true;
        }

        // Then check lab unit specific roles
        UserLabUnitRoles userLabRoles = userRoleService.getUserLabUnitRoles(sysUserId);
        if (userLabRoles == null || userLabRoles.getLabUnitRoleMap() == null) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "hasRequiredRoleForLabUnit",
                    "No lab unit roles found for user " + sysUserId);
            return false;
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "hasRequiredRoleForLabUnit",
                "User " + sysUserId + " has " + userLabRoles.getLabUnitRoleMap().size() + " lab unit role mappings");

        for (LabUnitRoleMap roleMap : userLabRoles.getLabUnitRoleMap()) {
            String labUnit = roleMap.getLabUnit(); // This is the test_section ID (e.g., "165")
            Set<String> userRoleIds = roleMap.getRoles(); // These are role IDs (e.g., "4", "5", "7", "10")

            LogEvent.logInfo(this.getClass().getSimpleName(), "hasRequiredRoleForLabUnit",
                    "Checking labUnit=" + labUnit + ", userRoleIds=" + userRoleIds + ", loginLabUnit=" + loginLabUnit);

            // Check if this lab unit mapping applies to the user's current login lab unit
            // labUnit can be "AllLabUnits" (special value) or a test section ID
            boolean labUnitMatches = false;
            if ("AllLabUnits".equalsIgnoreCase(labUnit)) {
                labUnitMatches = true;
            } else {
                // labUnit is a test section ID, we need to compare with loginLabUnit (which is
                // the localized name)
                TestSection ts = testSectionService.get(labUnit);
                if (ts != null) {
                    String tsLocalizedName = ts.getLocalizedName();
                    String tsName = ts.getTestSectionName();
                    labUnitMatches = loginLabUnit.equalsIgnoreCase(tsLocalizedName)
                            || loginLabUnit.equalsIgnoreCase(tsName) || loginLabUnit.equals(labUnit); // Also match by
                                                                                                      // ID
                    LogEvent.logInfo(this.getClass().getSimpleName(), "hasRequiredRoleForLabUnit",
                            "TestSection lookup: id=" + labUnit + ", localizedName=" + tsLocalizedName + ", name="
                                    + tsName + ", matches=" + labUnitMatches);
                }
            }

            if (labUnitMatches && userRoleIds != null) {
                // Convert user role IDs to role names and check against required roles
                for (String userRoleId : userRoleIds) {
                    Role role = roleService.getRoleById(userRoleId);
                    if (role != null) {
                        String userRoleName = role.getName();
                        // Trim whitespace - some role names have trailing spaces in the database
                        String trimmedUserRoleName = userRoleName != null ? userRoleName.trim() : null;
                        LogEvent.logInfo(this.getClass().getSimpleName(), "hasRequiredRoleForLabUnit",
                                "User has role: id=" + userRoleId + ", name='" + trimmedUserRoleName + "'");
                        for (String requiredRole : requiredRoles) {
                            String trimmedRequiredRole = requiredRole != null ? requiredRole.trim() : null;
                            if (trimmedUserRoleName != null
                                    && trimmedUserRoleName.equalsIgnoreCase(trimmedRequiredRole)) {
                                LogEvent.logInfo(this.getClass().getSimpleName(), "hasRequiredRoleForLabUnit",
                                        "Match found: userRole='" + trimmedUserRoleName + "' matches requiredRole='"
                                                + trimmedRequiredRole + "'");
                                return true;
                            }
                        }
                    }
                }
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "hasRequiredRoleForLabUnit",
                "No matching role found for requiredRoles=" + requiredRoles);
        return false;
    }

    /**
     * Check if a TestSection (department) matches the user's loginLabUnit. The
     * loginLabUnit is the localized name of the TestSection the user logged in
     * with.
     *
     * @param department   the TestSection to check
     * @param loginLabUnit the user's login lab unit (TestSection localized name)
     * @return true if the department matches the user's loginLabUnit
     */
    private boolean matchesLoginLabUnitToDepartment(TestSection department, String loginLabUnit) {
        if (department == null || loginLabUnit == null || loginLabUnit.isEmpty()) {
            return false;
        }

        String normalizedLoginLabUnit = loginLabUnit.trim();

        // Match against localized name (this is what getLoginLabUnit returns)
        String localizedName = department.getLocalizedName();
        if (localizedName != null && normalizedLoginLabUnit.equalsIgnoreCase(localizedName.trim())) {
            return true;
        }

        // Match against test section name
        String testSectionName = department.getTestSectionName();
        if (testSectionName != null && normalizedLoginLabUnit.equalsIgnoreCase(testSectionName.trim())) {
            return true;
        }

        // Match against ID (in case loginLabUnit is passed as ID)
        if (normalizedLoginLabUnit.equals(department.getId())) {
            return true;
        }

        // Additional matching: check if loginLabUnit contains the department name or
        // vice versa
        // This handles cases where names might have slight variations
        if (localizedName != null) {
            String normalizedDeptName = localizedName.trim().toLowerCase();
            String normalizedLabUnit = normalizedLoginLabUnit.toLowerCase();
            if (normalizedDeptName.contains(normalizedLabUnit) || normalizedLabUnit.contains(normalizedDeptName)) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "matchesLoginLabUnitToDepartment",
                        "Fuzzy match found: loginLabUnit=" + loginLabUnit + " matched department=" + localizedName);
                return true;
            }
        }

        return false;
    }

    /**
     * Find the TestSection (department) that matches the user's loginLabUnit. This
     * method can be used to get the exact department for the user.
     *
     * @param loginLabUnit the user's login lab unit
     * @return the matching TestSection, or null if not found
     */
    @Override
    @Transactional(readOnly = true)
    public TestSection getDepartmentForLoginLabUnit(String loginLabUnit) {
        if (loginLabUnit == null || loginLabUnit.isEmpty()) {
            return null;
        }

        List<TestSection> allDepts = testSectionService.getAllActiveTestSections();
        for (TestSection dept : allDepts) {
            if (matchesLoginLabUnitToDepartment(dept, loginLabUnit)) {
                return dept;
            }
        }
        return null;
    }
}
