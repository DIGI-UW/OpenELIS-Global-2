package org.openelisglobal.role.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.privilege.service.PrivilegeService;
import org.openelisglobal.privilege.valueholder.Privilege;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role administration for the admin UI: introspection (spec 012 T041) plus the
 * role editor's create and privilege-assignment operations.
 *
 * <p>
 * Authorization lives on the service interfaces, never here (S011c).
 * {@code getEffectivePrivilegesForRole} is gated with {@code PRIV_USER_MANAGE}
 * (the user-management persona reads it for the privilege summary panel); the
 * catalogue read is {@code PRIV_ROLE_VIEW}; creating a role and replacing its
 * grants are {@code PRIV_ROLE_MANAGE}.
 *
 * <p>
 * <b>Deliberate scope of PRIV_ROLE_MANAGE.</b> A holder can create a role,
 * grant it any privilege in the catalogue, and — via user management — assign
 * it to themselves. The capability is therefore equivalent to full system
 * access and is intentionally not constrained to privileges the caller already
 * holds. It is seeded to User Account Administrator alongside Global
 * Administrator; treat both as trusted administrators.
 */
@RestController
@RequestMapping("/rest/roles")
public class RoleRestController extends BaseController {

    @Autowired
    private PrivilegeService privilegeService;

    @Autowired
    private RoleService roleService;

    /**
     * Full effective privilege set for a role — direct plus inherited via the
     * role's grouping-parent chain, with Global Administrator expanded to the whole
     * catalog. Powers the privilege summary panel in User Management.
     */
    @GetMapping(value = "/{roleId}/privileges", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PrivilegeView> getEffectivePrivileges(@PathVariable String roleId) {
        return privilegeService.getEffectivePrivilegesForRole(roleId).stream().map(PrivilegeView::new)
                .collect(Collectors.toList());
    }

    /**
     * The assignable roles, for the editor's picker. Containers
     * ({@code is_grouping_role}) are excluded: they are UI sections, not roles
     * anyone holds or whose privileges mean anything.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RoleView> getRoles() {
        return roleService.getAllActiveRoles().stream()
                .filter(role -> role.getGroupingRole() == null || !role.getGroupingRole()).map(RoleView::new)
                .collect(Collectors.toList());
    }

    /** The full privilege catalogue, for the role editor's checklist. */
    @GetMapping(value = "/privileges", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PrivilegeView> getPrivilegeCatalogue() {
        return privilegeService.getPrivilegeCatalogue().stream().map(PrivilegeView::new).collect(Collectors.toList());
    }

    /**
     * The privileges a role holds DIRECTLY — what the editor's checkboxes bind to.
     * Distinct from {@code /{roleId}/privileges}, which is the effective set
     * including inherited entries the editor must not present as editable.
     */
    @GetMapping(value = "/{roleId}/privileges/direct", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PrivilegeView> getDirectPrivileges(@PathVariable String roleId) {
        return privilegeService.getDirectPrivilegesForRole(roleId).stream().map(PrivilegeView::new)
                .collect(Collectors.toList());
    }

    /** Replaces a role's direct grants with exactly the ids supplied. */
    @PutMapping(value = "/{roleId}/privileges", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PrivilegeView> replaceDirectPrivileges(@PathVariable String roleId,
            @RequestBody RolePrivilegesForm form) {
        return privilegeService
                .replaceDirectPrivilegesForRole(roleId,
                        form == null || form.getPrivilegeIds() == null ? List.of() : form.getPrivilegeIds())
                .stream().map(PrivilegeView::new).collect(Collectors.toList());
    }

    /** Creates an assignable role. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RoleView createRole(@RequestBody RoleForm form, HttpServletRequest request) {
        if (form == null) {
            throw new IllegalArgumentException("A role definition is required");
        }
        Role created = roleService.createAssignableRole(form.getName(), form.getDescription(), form.getDisplayKey(),
                form.getGroupingParentName(), form.getParentRoleName(), getSysUserId(request));
        if (form.getPrivilegeIds() != null && !form.getPrivilegeIds().isEmpty()) {
            privilegeService.replaceDirectPrivilegesForRole(String.valueOf(created.getId()), form.getPrivilegeIds());
        }
        return new RoleView(created);
    }

    /** A role as the editor lists it. */
    public static class RoleView {
        private final String id;
        private final String name;
        private final String description;

        RoleView(Role role) {
            this.id = String.valueOf(role.getId());
            this.name = role.getName() == null ? null : role.getName().trim();
            this.description = role.getDescription();
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /** Request body for replacing a role's direct grants. */
    public static class RolePrivilegesForm {
        private List<Integer> privilegeIds;

        public List<Integer> getPrivilegeIds() {
            return privilegeIds;
        }

        public void setPrivilegeIds(List<Integer> privilegeIds) {
            this.privilegeIds = privilegeIds;
        }
    }

    /** Request body for creating a role. */
    public static class RoleForm {
        private String name;
        private String description;
        private String displayKey;
        private String groupingParentName;
        private String parentRoleName;
        private List<Integer> privilegeIds;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDisplayKey() {
            return displayKey;
        }

        public void setDisplayKey(String displayKey) {
            this.displayKey = displayKey;
        }

        public String getGroupingParentName() {
            return groupingParentName;
        }

        public void setGroupingParentName(String groupingParentName) {
            this.groupingParentName = groupingParentName;
        }

        public String getParentRoleName() {
            return parentRoleName;
        }

        public void setParentRoleName(String parentRoleName) {
            this.parentRoleName = parentRoleName;
        }

        public List<Integer> getPrivilegeIds() {
            return privilegeIds;
        }

        public void setPrivilegeIds(List<Integer> privilegeIds) {
            this.privilegeIds = privilegeIds;
        }
    }

    public static class PrivilegeView {
        /**
         * Needed by the role editor: the PUT that replaces a role's grants is keyed by
         * privilege id, so a catalogue without ids cannot be turned into a valid
         * request. Names are not a substitute — they are not the table's key.
         */
        private final Integer id;
        private final String name;
        private final String description;
        private final String category;

        PrivilegeView(Privilege privilege) {
            this.id = privilege.getId();
            this.name = privilege.getName();
            this.description = privilege.getDescription();
            this.category = privilege.getCategory();
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getCategory() {
            return category;
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        return null;
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
