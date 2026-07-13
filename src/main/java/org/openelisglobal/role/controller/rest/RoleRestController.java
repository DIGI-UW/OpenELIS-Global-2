package org.openelisglobal.role.controller.rest;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.privilege.service.PrivilegeService;
import org.openelisglobal.privilege.valueholder.Privilege;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only role introspection for the admin UI (spec 012 T041).
 *
 * <p>
 * Authorization lives on the service interface (S011c):
 * {@code PrivilegeService#getEffectivePrivilegesForRole} is gated with
 * {@code PRIV_USER_MANAGE} — the privilege held by the user-management persona
 * this endpoint serves.
 */
@RestController
@RequestMapping("/rest/roles")
public class RoleRestController extends BaseController {

    @Autowired
    private PrivilegeService privilegeService;

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

    public static class PrivilegeView {
        private final String name;
        private final String description;
        private final String category;

        PrivilegeView(Privilege privilege) {
            this.name = privilege.getName();
            this.description = privilege.getDescription();
            this.category = privilege.getCategory();
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
