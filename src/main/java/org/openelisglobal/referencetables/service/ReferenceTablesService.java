package org.openelisglobal.referencetables.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "BaseWebContextSensitiveTest setup, AuditTrailService (all service callers), HL7 encoding — getReferenceTableByName is public infrastructure; admin methods guarded by PRIV_SYSTEM_CONFIGURE")
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
