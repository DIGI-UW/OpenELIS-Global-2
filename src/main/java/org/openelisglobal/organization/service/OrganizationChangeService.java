package org.openelisglobal.organization.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationChange;

public interface OrganizationChangeService extends BaseObjectService<OrganizationChange, Integer> {

    /** One field's change: its name, the value before and the value after. */
    record FieldChange(String field, String oldValue, String newValue) {
    }

    /**
     * Records what happened to an organization. {@code actor} names a non-user
     * origin such as an import run or the registry sync and may be null when a user
     * did it.
     */
    OrganizationChange record(Integer organizationId, String action, List<FieldChange> changes, String sysUserId,
            String actor);

    List<OrganizationChange> getForOrganization(Integer organizationId);

    /** The field changes stored on an entry, in order. */
    List<FieldChange> changesOf(OrganizationChange change);

    /**
     * Every name an organization had before its current one, keyed by organization
     * id, from the entries that renamed it.
     */
    Map<Integer, List<String>> formerNames();
}
