package org.openelisglobal.referencetables.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReferenceTablesService extends BaseObjectService<ReferenceTables, String> {
    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    void getData(ReferenceTables referenceTables);

    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    List<ReferenceTables> getAllReferenceTablesForHl7Encoding();

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<ReferenceTables> getAllReferenceTables();

    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    ReferenceTables getReferenceTableByName(String tableName);

    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    ReferenceTables getReferenceTableByName(ReferenceTables referenceTables);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    Integer getTotalReferenceTableCount();

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<ReferenceTables> getPageOfReferenceTables(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    Integer getTotalReferenceTablesCount();
}
