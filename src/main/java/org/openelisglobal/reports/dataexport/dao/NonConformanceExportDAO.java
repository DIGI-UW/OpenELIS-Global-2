package org.openelisglobal.reports.dataexport.dao;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.stream.Stream;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;

public interface NonConformanceExportDAO extends BaseDAO<NcEvent, Integer> {
    record EventRow(Integer eventId, Integer linkId, String specimenId, String accession, String reason, String stage,
            String reporter, Date eventDate, Date recordedDate, String status) {
    }

    record RejectionRow(String id, String specimenId, String accession, String reason, Timestamp recordedDate) {
    }

    Stream<EventRow> events(ExportSnapshot request);

    Stream<RejectionRow> rejections(ExportSnapshot request);
}
