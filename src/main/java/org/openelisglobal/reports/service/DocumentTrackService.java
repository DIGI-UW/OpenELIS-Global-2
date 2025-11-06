package org.openelisglobal.reports.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.reports.valueholder.DocumentTrack;

public interface DocumentTrackService extends BaseObjectService<DocumentTrack, String> {
    List<DocumentTrack> getByTypeRecordAndTableAndName(String reportTypeId, String referenceTable, String id,
            String name);

    List<DocumentTrack> getByTypeRecordAndTable(String typeId, String tableId, String recordId);
}
