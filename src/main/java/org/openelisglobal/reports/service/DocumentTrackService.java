package org.openelisglobal.reports.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.springframework.security.access.prepost.PreAuthorize;

// DOCUMENT_TRACK records which reports have been issued for a sample. Generating
// and recording one is report administration (PRIV_REPORT_RUN, kept at type level
// and on the inherited writes via CrudPrivileges), but ASKING whether a patient
// report has already gone out is part of result entry and validation: it is how an
// amended result is recognised as a corrected result. The two lookups below are
// reached from ReportTrackingService.getReportsForSample, so widening only that
// class would still deny here.
@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
@CrudPrivileges(write = "PRIV_REPORT_RUN")
public interface DocumentTrackService extends BaseObjectService<DocumentTrack, String> {
    @PreAuthorize("hasAnyAuthority('PRIV_REPORT_RUN','PRIV_RESULT_VALIDATE','PRIV_RESULT_ENTER')")
    List<DocumentTrack> getByTypeRecordAndTableAndName(String reportTypeId, String referenceTable, String id,
            String name);

    @PreAuthorize("hasAnyAuthority('PRIV_REPORT_RUN','PRIV_RESULT_VALIDATE','PRIV_RESULT_ENTER')")
    List<DocumentTrack> getByTypeRecordAndTable(String typeId, String tableId, String recordId);
}
