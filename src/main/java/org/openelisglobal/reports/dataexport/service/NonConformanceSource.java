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
    private final ExportLocations locations;

    public NonConformanceSource(NonConformanceExportDAO occurrences, ReportingCsvWriter csv,
            ExportLocations locations) {
        this.occurrences = occurrences;
        this.csv = csv;
        this.locations = locations;
    }

    /**
     * The date anchor of a report type that lists every non-conformance: events,
     * recorded occurrences and rejected specimens.
     */
    static final String EVENTS_AND_REJECTIONS = "eventOrRecordedDate";

    /**
     * The date anchor of a report type that lists rejected specimens only, as the
     * CSV Rejection Report does (OGC-1656): no event, and no occurrence on a
     * specimen that was not rejected.
     */
    static final String REJECTIONS_ONLY = "rejectionDate";

    @Override
    public String id() {
        return "NON_CONFORMANCE";
    }

    @Override
    public List<ReportingVariable> catalog() {
        List<ReportingVariable> fields = new java.util.ArrayList<>(nonConformanceFields());
        fields.addAll(locations.catalog(List.of("TABLE")));
        return fields;
    }

    private static List<ReportingVariable> nonConformanceFields() {
        return List.of(field("ncAccessionNumber", "Accession Number", "text"),
                field("rejectionReason", "Rejection Reason", "text"), field("rejectionDate", "Rejection Date", "date"),
                field("rejectionStage", "Rejection Stage", "text"), field("rejectedBy", "Rejected By", "text"),
                field("ncOccurrenceId", "Occurrence ID", "text"), field("ncStatus", "Status", "text"),
                field("ncSpecimenId", "Specimen ID", "text"), field("ncDateBasis", "Date Basis", "text"),
                field("ncEventDate", "Event Date", "date"), field("ncRecordedDate", "Recorded Date", "date"),
                field("ncRecordSource", "Record Source", "text"), field("ncRequestDate", "Request Date", "date"),
                field("ncSampleType", "Sample Type", "text"), field("ncTest", "Test", "text"));
    }

    private static ReportingVariable field(String id, String label, String type) {
        return new ReportingVariable(id, label, type, "nonConformance", false, List.of("TABLE"));
    }

    @Override
    public void validateConfiguration(ReportSourceConfig configuration) {
        if (!List.of(EVENTS_AND_REJECTIONS, REJECTIONS_ONLY).contains(configuration.dateAnchor())
                || !configuration.layouts().equals(List.of("TABLE"))
                || !List.of(ExportLocations.GROUP).containsAll(configuration.catalogs())
                || !List.of("labSectionIds").containsAll(configuration.filters()))
            throw new IllegalArgumentException("reporting.definition.unsupportedMapping");
        if (!catalog().stream().map(ReportingVariable::id).collect(Collectors.toSet())
                .containsAll(configuration.attributes()))
            throw new IllegalArgumentException("reporting.definition.attributesInvalid");
    }

    @Override
    public long write(Writer output, ExportSnapshot request) throws IOException {
        ZoneId zone = ZoneId.of(request.timezone());
        java.util.Locale locale = request.userLocale();
        boolean byTest = request.variables().stream().anyMatch(field -> "ncTest".equals(field.id()));
        boolean rejectionsOnly = REJECTIONS_ONLY.equals(request.definition().dateAnchor());
        List<String> sections = request.filterSpec().labSectionIds();
        Map<String, Map<String, String>> locationCache = new java.util.HashMap<>();
        Map<String, Map<String, String>> specimenCache = new java.util.HashMap<>();
        try (var events = rejectionsOnly ? java.util.stream.Stream.<NonConformanceExportDAO.EventRow>empty()
                : occurrences.events(request);
                var rejections = occurrences.rejections(request, rejectionsOnly);
                var rejectedSpecimens = occurrences.specimenRejections(request)) {
            var eventRecords = events.map(row -> {
                Map<String, String> fields = common(row.accession(), row.specimenId(), row.reason());
                fields.putAll(locations.forAccession(row.accession(), locationCache));
                fields.putAll(specimenFields(row.specimenId(), zone, locale, specimenCache));
                fields.put("rejectionStage", row.stage());
                fields.put("rejectedBy", row.reporter());
                fields.put("ncStatus", row.status());
                fields.put("rejectionDate", date(row.eventDate() == null ? row.recordedDate() : row.eventDate()));
                fields.put("ncDateBasis", row.eventDate() == null ? "Recorded date" : "Event date");
                fields.put("ncEventDate", date(row.eventDate()));
                fields.put("ncRecordedDate", date(row.recordedDate()));
                fields.put("ncRecordSource", "Non-conformance event");
                return record("event:" + row.eventId() + (row.linkId() == null ? "" : ":link:" + row.linkId()), fields);
            });
            var rejectionRecords = rejections.flatMap(row -> {
                Map<String, String> fields = common(row.accession(), row.specimenId(), row.reason());
                fields.putAll(locations.forAccession(row.accession(), locationCache));
                fields.putAll(specimenFields(row.specimenId(), zone, locale, specimenCache));
                String recorded = localDate(row.recordedDate(), zone);
                fields.put("rejectionDate", recorded);
                fields.put("ncDateBasis", "Recorded rejection date");
                fields.put("ncRecordedDate", recorded);
                fields.put("ncRecordSource", "Recorded rejection");
                return perTest(byTest, sections, "rejection:" + row.id(), row.specimenId(), fields, locale);
            });
            var specimenRecords = rejectedSpecimens.flatMap(row -> {
                String reason = occurrences.rejectionNote(row.specimenId());
                Map<String, String> fields = common(row.accession(), row.specimenId(),
                        reason != null ? reason : occurrences.dictionaryEntry(row.rejectReasonId()));
                fields.putAll(locations.forAccession(row.accession(), locationCache));
                fields.putAll(specimenFields(row.specimenId(), zone, locale, specimenCache));
                String rejected = localDate(row.rejectedAt(), zone);
                fields.put("rejectionDate", rejected);
                fields.put("ncDateBasis", "Recorded rejection date");
                fields.put("ncRecordedDate", rejected);
                fields.put("ncRecordSource", "Rejected specimen");
                return perTest(byTest, sections, "specimen:" + row.specimenId(), row.specimenId(), fields, locale);
            });
            var records = java.util.stream.Stream.concat(java.util.stream.Stream.concat(eventRecords, rejectionRecords),
                    specimenRecords);
            return csv.write(output, ReportingCsvWriter.Layout.TABLE,
                    request.variables().stream().map(ReportingVariable::exportField).toList(), records.iterator());
        }
    }

    /**
     * One row per rejected test when the Test column is chosen, else one row for
     * the occurrence (OGC-1656, v1.6 BR-012).
     */
    private java.util.stream.Stream<ExportRecord> perTest(boolean byTest, List<String> sections, String id,
            String specimenId, Map<String, String> fields, java.util.Locale locale) {
        var tests = byTest && specimenId != null ? occurrences.tests(specimenId, sections)
                : java.util.List.<org.openelisglobal.test.valueholder.Test>of();
        if (tests.isEmpty()) {
            return java.util.stream.Stream.of(record(id, fields));
        }
        return java.util.stream.IntStream.range(0, tests.size()).mapToObj(index -> {
            Map<String, String> row = new LinkedHashMap<>(fields);
            row.put("ncTest", ExportTestNames.name(tests.get(index), locale));
            return record(id + ":test:" + (index + 1), row);
        });
    }

    /** The specimen's sample type and when its order was received (OGC-1656). */
    private Map<String, String> specimenFields(String specimenId, ZoneId zone, java.util.Locale locale,
            Map<String, Map<String, String>> cache) {
        if (specimenId == null) {
            return Map.of();
        }
        return cache.computeIfAbsent(specimenId, id -> {
            Map<String, String> fields = new LinkedHashMap<>();
            var specimen = occurrences.specimen(id);
            if (specimen != null) {
                fields.put("ncRequestDate",
                        localDate(SampleTestingSource.received(specimen, specimen.getSample()), zone));
                fields.put("ncSampleType", SampleTestingSource.sampleTypeName(specimen.getTypeOfSample(), locale));
            }
            return fields;
        });
    }

    private static String localDate(java.sql.Timestamp value, ZoneId zone) {
        return value == null ? null : value.toInstant().atZone(zone).toLocalDate().toString();
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
