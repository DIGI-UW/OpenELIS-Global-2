package org.openelisglobal.reports.dataexport.service;

import java.io.IOException;
import java.io.Writer;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.reports.dataexport.dao.NonConformanceExportDAO;
import org.openelisglobal.reports.dataexport.form.ExportRecord;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NonConformanceSource implements ReportingSource {
    private final NonConformanceExportDAO occurrences;
    private final ReportingCsvWriter csv;

    public NonConformanceSource(NonConformanceExportDAO occurrences, ReportingCsvWriter csv) {
        this.occurrences = occurrences;
        this.csv = csv;
    }

    @Override
    public String id() {
        return "NON_CONFORMANCE";
    }

    @Override
    public List<ReportingVariable> catalog() {
        return List.of(field("ncAccessionNumber", "Accession Number", "text"),
                field("rejectionReason", "Rejection Reason", "text"), field("rejectionDate", "Rejection Date", "date"),
                field("rejectionStage", "Rejection Stage", "text"), field("rejectedBy", "Rejected By", "text"),
                field("ncOccurrenceId", "Occurrence ID", "text"), field("ncStatus", "Status", "text"),
                field("ncSpecimenId", "Specimen ID", "text"), field("ncDateBasis", "Date Basis", "text"),
                field("ncEventDate", "Event Date", "date"), field("ncRecordedDate", "Recorded Date", "date"),
                field("ncRecordSource", "Record Source", "text"));
    }

    private static ReportingVariable field(String id, String label, String type) {
        return new ReportingVariable(id, label, type, "nonConformance", false, List.of("TABLE"));
    }

    @Override
    public void validateConfiguration(ReportSourceConfig configuration) {
        if (!"eventOrRecordedDate".equals(configuration.dateAnchor())
                || !configuration.layouts().equals(List.of("TABLE")) || !configuration.catalogs().isEmpty()
                || !List.of("labSectionIds").containsAll(configuration.filters()))
            throw new IllegalArgumentException("reporting.definition.unsupportedMapping");
        if (!catalog().stream().map(ReportingVariable::id).collect(Collectors.toSet())
                .containsAll(configuration.attributes()))
            throw new IllegalArgumentException("reporting.definition.attributesInvalid");
    }

    @Override
    public long write(Writer output, ExportSnapshot request) throws IOException {
        ZoneId zone = ZoneId.of(request.timezone());
        try (var events = occurrences.events(request); var rejections = occurrences.rejections(request)) {
            var records = java.util.stream.Stream.concat(events.map(row -> {
                Map<String, String> fields = common(row.accession(), row.specimenId(), row.reason());
                fields.put("rejectionStage", row.stage());
                fields.put("rejectedBy", row.reporter());
                fields.put("ncStatus", row.status());
                fields.put("rejectionDate", date(row.eventDate() == null ? row.recordedDate() : row.eventDate()));
                fields.put("ncDateBasis", row.eventDate() == null ? "Recorded date" : "Event date");
                fields.put("ncEventDate", date(row.eventDate()));
                fields.put("ncRecordedDate", date(row.recordedDate()));
                fields.put("ncRecordSource", "Non-conformance event");
                return record("event:" + row.eventId() + (row.linkId() == null ? "" : ":link:" + row.linkId()), fields);
            }), rejections.map(row -> {
                Map<String, String> fields = common(row.accession(), row.specimenId(), row.reason());
                String recorded = row.recordedDate() == null ? null
                        : row.recordedDate().toInstant().atZone(zone).toLocalDate().toString();
                fields.put("rejectionDate", recorded);
                fields.put("ncDateBasis", "Recorded rejection date");
                fields.put("ncRecordedDate", recorded);
                fields.put("ncRecordSource", "Recorded rejection");
                return record("rejection:" + row.id(), fields);
            }));
            return csv.write(output, ReportingCsvWriter.Layout.TABLE,
                    request.variables().stream().map(ReportingVariable::exportField).toList(), records.iterator());
        }
    }

    private static Map<String, String> common(String accession, String specimen, String reason) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("ncAccessionNumber", accession);
        fields.put("ncSpecimenId", specimen);
        fields.put("rejectionReason", reason);
        return fields;
    }

    private static ExportRecord record(String id, Map<String, String> fields) {
        fields.put("ncOccurrenceId", id);
        return new ExportRecord(id, id, null, fields, Map.of());
    }

    private static String date(java.sql.Date date) {
        return date == null ? null : date.toLocalDate().toString();
    }
}
