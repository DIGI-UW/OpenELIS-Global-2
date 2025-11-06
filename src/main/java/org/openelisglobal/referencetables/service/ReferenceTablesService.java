package org.openelisglobal.referencetables.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;

public interface ReferenceTablesService extends BaseObjectService<ReferenceTables, String> {
    void getData(ReferenceTables referenceTables);

    List<ReferenceTables> getAllReferenceTablesForHl7Encoding();

    List<ReferenceTables> getAllReferenceTables();

    ReferenceTables getReferenceTableByName(String tableName);

    ReferenceTables getReferenceTableByName(ReferenceTables referenceTables);

    Integer getTotalReferenceTableCount();

    List<ReferenceTables> getPageOfReferenceTables(int startingRecNo);

    Integer getTotalReferenceTablesCount();
}
