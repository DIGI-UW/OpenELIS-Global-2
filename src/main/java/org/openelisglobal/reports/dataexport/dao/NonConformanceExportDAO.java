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

    /**
     * A specimen rejected at order entry or since, with no recorded rejection
     * occurrence (OGC-1656).
     */
    record SpecimenRejectionRow(String specimenId, String accession, String rejectReasonId, Timestamp rejectedAt) {
    }

    /**
     * Rejected specimens that no rejection occurrence records, by when they were
     * rejected: the rejection-reason note order entry writes, else the specimen's
     * last change.
     */
    Stream<SpecimenRejectionRow> specimenRejections(ExportSnapshot request);

    /** The reason order entry wrote when it rejected the specimen, if any. */
    String rejectionNote(String specimenId);

    /** A dictionary entry's text, if the id names one. */
    String dictionaryEntry(String id);

    org.openelisglobal.sampleitem.valueholder.SampleItem specimen(String specimenId);

    /** The tests ordered on the specimen, in order. */
    java.util.List<org.openelisglobal.test.valueholder.Test> tests(String specimenId);
}
