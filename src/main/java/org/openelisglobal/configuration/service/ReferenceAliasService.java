package org.openelisglobal.configuration.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.configuration.valueholder.ReferenceAlias;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReferenceAliasService extends BaseObjectService<ReferenceAlias, String> {

    /**
     * The record id remembered for this spelling of this kind of reference, or
     * null.
     */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    String resolve(String referenceType, String name);

    /**
     * Remembers a spelling for a record, replacing an earlier alias of the same
     * spelling.
     */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    ReferenceAlias remember(String referenceType, String name, String targetId, String sysUserId);
}
