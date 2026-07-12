package org.openelisglobal.referencetables.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "AuditTrailService resolves the reference table for EVERY audited insert/update by"
        + " EVERY user, and domain services resolve their table id in @PostConstruct — reference-table reads are"
        + " audit infrastructure, not a user-facing surface. Enumeration and paging remain gated with"
        + " PRIV_SYSTEM_CONFIGURE")
public interface ReferenceTablesService extends BaseObjectService<ReferenceTables, String> {
    void getData(ReferenceTables referenceTables);

    List<ReferenceTables> getAllReferenceTablesForHl7Encoding();

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<ReferenceTables> getAllReferenceTables();

    ReferenceTables getReferenceTableByName(String tableName);

    ReferenceTables getReferenceTableByName(ReferenceTables referenceTables);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    Integer getTotalReferenceTableCount();

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<ReferenceTables> getPageOfReferenceTables(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    Integer getTotalReferenceTablesCount();
}
