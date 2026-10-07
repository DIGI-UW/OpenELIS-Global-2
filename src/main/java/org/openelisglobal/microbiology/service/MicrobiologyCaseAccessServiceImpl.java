package org.openelisglobal.microbiology.service;

import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MicrobiologyCaseAccessServiceImpl implements MicrobiologyCaseAccessService {
    private final MicroCaseDAO caseDAO;
    private final UserRoleService userRoleService;
    private final RoleService roleService;

    public MicrobiologyCaseAccessServiceImpl(MicroCaseDAO caseDAO, UserRoleService userRoleService,
            RoleService roleService) {
        this.caseDAO = caseDAO;
        this.userRoleService = userRoleService;
        this.roleService = roleService;
    }

    @Override
    public MicrobiologyWorklistAccess getWorklistAccess(String systemUserId) {
        if (!hasText(systemUserId)) {
            return new MicrobiologyWorklistAccess(false, Set.of());
        }
        if (userRoleService.userInRole(systemUserId, Constants.ROLE_GLOBAL_ADMIN)) {
            return new MicrobiologyWorklistAccess(true, Set.of());
        }
        Set<String> roleIds = new HashSet<>();
        for (String name : Set.of(Constants.ROLE_RESULTS, Constants.ROLE_VALIDATION)) {
            Role role = roleService.getRoleByName(name);
            if (role != null && hasText(role.getId())) {
                roleIds.add(role.getId());
            }
        }
        UserLabUnitRoles labRoles = userRoleService.getUserLabUnitRoles(systemUserId);
        Set<String> units = new HashSet<>();
        if (labRoles != null && labRoles.getLabUnitRoleMap() != null) {
            for (var mapping : labRoles.getLabUnitRoleMap()) {
                if (mapping == null || mapping.getRoles() == null || !hasText(mapping.getLabUnit())
                        || mapping.getRoles().stream().noneMatch(roleIds::contains)) {
                    continue;
                }
                if (UnifiedSystemUserController.ALL_LAB_UNITS.equals(mapping.getLabUnit())) {
                    return new MicrobiologyWorklistAccess(true, Set.of());
                }
                units.add(mapping.getLabUnit());
            }
        }
        return new MicrobiologyWorklistAccess(false, units);
    }

    @Override
    public boolean canReadCase(String caseId, String systemUserId) {
        return hasText(systemUserId) && hasText(caseId) && caseDAO.get(caseId).isPresent();
    }

    @Override
    public boolean canViewOnWorklist(String caseId, String systemUserId) {
        return canEnterResults(caseId, systemUserId) || canValidateResults(caseId, systemUserId);
    }

    @Override
    public boolean canEnterResults(String caseId, String systemUserId) {
        return hasCaseRole(caseId, systemUserId, Constants.ROLE_RESULTS);
    }

    @Override
    public boolean canValidateResults(String caseId, String systemUserId) {
        return hasCaseRole(caseId, systemUserId, Constants.ROLE_VALIDATION);
    }

    @Override
    public boolean canEnterResultsInUnit(String testSectionId, String systemUserId) {
        return hasUnitRole(testSectionId, systemUserId, Constants.ROLE_RESULTS);
    }

    @Override
    @Transactional
    public void requireResults(String caseId, String systemUserId) {
        requireCaseRole(caseId, systemUserId, Constants.ROLE_RESULTS);
    }

    @Override
    @Transactional
    public void requireValidation(String caseId, String systemUserId) {
        requireCaseRole(caseId, systemUserId, Constants.ROLE_VALIDATION);
    }

    private void requireCaseRole(String caseId, String systemUserId, String roleName) {
        String message = roleName + " access in the case lab unit is required";
        if (!hasText(caseId) || !hasText(systemUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
        MicroCase observed = caseDAO.get(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, message));
        // Use the routing/transfer lock order and refresh cached ownership before
        // authorizing. The surrounding mutation retains these locks until commit.
        caseDAO.lockOrder(observed.getSampleId());
        MicroCase current = caseDAO.getForUpdate(caseId);
        if (!hasUnitRole(current.getTestSectionId(), systemUserId, roleName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private boolean hasCaseRole(String caseId, String systemUserId, String roleName) {
        if (!hasText(caseId) || !hasText(systemUserId)) {
            return false;
        }
        MicroCase microCase = caseDAO.get(caseId).orElse(null);
        return microCase != null && hasUnitRole(microCase.getTestSectionId(), systemUserId, roleName);
    }

    private boolean hasUnitRole(String testSectionId, String systemUserId, String roleName) {
        if (!hasText(testSectionId) || !hasText(systemUserId)) {
            return false;
        }
        if (userRoleService.userInRole(systemUserId, Constants.ROLE_GLOBAL_ADMIN)) {
            return true;
        }
        Role role = roleService.getRoleByName(roleName);
        UserLabUnitRoles labRoles = userRoleService.getUserLabUnitRoles(systemUserId);
        if (role == null || role.getId() == null || labRoles == null || labRoles.getLabUnitRoleMap() == null) {
            return false;
        }
        return labRoles.getLabUnitRoleMap().stream()
                .anyMatch(mapping -> mapping != null
                        && (testSectionId.equals(mapping.getLabUnit())
                                || UnifiedSystemUserController.ALL_LAB_UNITS.equals(mapping.getLabUnit()))
                        && mapping.getRoles() != null && mapping.getRoles().contains(role.getId()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
