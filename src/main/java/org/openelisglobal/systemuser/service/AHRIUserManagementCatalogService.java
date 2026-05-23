package org.openelisglobal.systemuser.service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.constants.rbac.AHRIRoleCatalog;
import org.openelisglobal.common.constants.rbac.AHRITestSectionCatalog;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.role.action.bean.DisplayRole;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Filters user-management lab unit lists to the AHRI research-lab allowlist only
 * ({@link AHRITestSectionCatalog} / research-lab-linkages.csv).
 */
@Service
public class AHRIUserManagementCatalogService {

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private RoleService roleService;

    public List<DisplayRole> filterLabUnitRolesForSrs(List<DisplayRole> displayRoles) {
        return filterRolesByCatalog(displayRoles, AHRIRoleCatalog::isDepartmentRoleName,
                AHRIRoleCatalog.departmentRoleDisplayOrder());
    }

    public List<DisplayRole> filterProjectRolesForSrs(List<DisplayRole> displayRoles) {
        return filterRolesByCatalog(displayRoles, AHRIRoleCatalog::isProjectRoleName,
                AHRIRoleCatalog.projectRoleDisplayOrder());
    }

    public List<DisplayRole> filterGlobalRolesForSrs(List<DisplayRole> displayRoles) {
        return filterRolesByCatalog(displayRoles, AHRIRoleCatalog::isGlobalRoleName,
                AHRIRoleCatalog.globalRoleDisplayOrder());
    }

    private List<DisplayRole> filterRolesByCatalog(List<DisplayRole> displayRoles, Predicate<String> namePredicate,
            List<String> srsOrder) {
        if (displayRoles == null || displayRoles.isEmpty()) {
            return List.of();
        }
        List<DisplayRole> filtered = displayRoles.stream().filter(displayRole -> {
            Role role = roleService.getRoleById(displayRole.getRoleId());
            String roleName = role != null ? role.getName() : displayRole.getRoleName();
            return namePredicate.test(roleName);
        }).collect(Collectors.toList());
        sortRolesForSrsDisplay(filtered, srsOrder);
        return filtered;
    }

    public void sortRolesForSrsDisplay(List<DisplayRole> displayRoles, List<String> srsOrder) {
        if (displayRoles == null || displayRoles.isEmpty() || srsOrder == null) {
            return;
        }
        displayRoles.sort(Comparator.<DisplayRole>comparingInt(displayRole -> {
            Role role = roleService.getRoleById(displayRole.getRoleId());
            String roleName = role != null ? role.getName() : displayRole.getRoleName();
            return AHRIRoleCatalog.displayOrderIndex(srsOrder, roleName);
        }).thenComparing(displayRole -> String.valueOf(displayRole.getRoleName()), String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Lab units for {@code Lab Unit Roles} assignment: only active test sections whose
     * name is in the AHRI allowlist. Never returns the full active list.
     */
    @Transactional(readOnly = true)
    public List<IdValuePair> filterLabUnitTestSections(List<IdValuePair> activeTestSections) {
        if (activeTestSections == null || activeTestSections.isEmpty()) {
            return List.of();
        }

        List<IdValuePair> filtered = activeTestSections.stream().filter(section -> section != null)
                .filter(section -> !GenericValidator.isBlankOrNull(section.getId()))
                .filter(section -> isAllowlistedTestSection(section.getId())
                        || isAllowlistedDisplayName(section.getValue()))
                .sorted(Comparator
                        .comparing((IdValuePair section) -> String.valueOf(section.getValue()), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "filterLabUnitTestSections",
                    "No active test sections matched AHRI allowlist; returning empty list for lab unit dropdown");
        }

        return filtered;
    }

    private boolean isAllowlistedDisplayName(String displayName) {
        return AHRITestSectionCatalog.contains(displayName);
    }

    private boolean isAllowlistedTestSection(String sectionId) {
        TestSection testSection = testSectionService.get(sectionId);
        if (testSection == null) {
            return false;
        }
        if (AHRITestSectionCatalog.contains(testSection.getTestSectionName())) {
            return true;
        }
        try {
            return AHRITestSectionCatalog.contains(testSection.getLocalizedName());
        } catch (Exception ignored) {
            return false;
        }
    }
}
