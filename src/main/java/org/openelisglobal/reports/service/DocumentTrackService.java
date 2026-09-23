package org.openelisglobal.reports.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
public interface DocumentTrackService extends BaseObjectService<DocumentTrack, String> {
    List<DocumentTrack> getByTypeRecordAndTableAndName(String reportTypeId, String referenceTable, String id,
            String name);

    List<DocumentTrack> getByTypeRecordAndTable(String typeId, String tableId, String recordId);
}
