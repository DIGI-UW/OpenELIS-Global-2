package org.openelisglobal.configuration.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.configuration.valueholder.ReferenceAlias;

public interface ReferenceAliasService extends BaseObjectService<ReferenceAlias, String> {

    /**
     * The record id remembered for this spelling of this kind of reference, or
     * null.
     */
    String resolve(String referenceType, String name);

    /**
     * Remembers a spelling for a record, replacing an earlier alias of the same
     * spelling.
     */
    ReferenceAlias remember(String referenceType, String name, String targetId, String sysUserId);
}
