package org.openelisglobal.organization.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationChange;
import org.springframework.security.access.prepost.PreAuthorize;

@CrudPrivileges(write = "PRIV_ORGANIZATION_MANAGE", read = "PRIV_ORGANIZATION_VIEW")
public interface OrganizationChangeService extends BaseObjectService<OrganizationChange, Integer> {

    /** One field's change: its name, the value before and the value after. */
    record FieldChange(String field, String oldValue, String newValue) {
    }

    /**
     * Records what happened to an organization. {@code actor} names a non-user
     * origin such as an import run or the registry sync and may be null when a user
     * did it.
     */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    OrganizationChange record(Integer organizationId, String action, List<FieldChange> changes, String sysUserId,
            String actor);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<OrganizationChange> getForOrganization(Integer organizationId);

    /** The field changes stored on an entry, in order. */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<FieldChange> changesOf(OrganizationChange change);

    /**
     * Every name an organization had before its current one, keyed by organization
     * id, from the entries that renamed it.
     */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Map<Integer, List<String>> formerNames();
}
