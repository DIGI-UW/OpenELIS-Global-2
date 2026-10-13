package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.workplan.dao.WorkplanPrintRecordDAO;
import org.openelisglobal.workplan.valueholder.WorkplanPrintRecord;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroBenchPrintServiceImpl implements MicroBenchPrintService {
    private final MicroBenchService bench;
    private final MicroCultureService cultures;
    private final WorkplanPrintRecordDAO records;
    private final MicrobiologyCaseAccessService access;
    private final ObjectMapper json;

    @org.springframework.beans.factory.annotation.Autowired
    public MicroBenchPrintServiceImpl(MicroBenchService bench, MicroCultureService cultures,
            WorkplanPrintRecordDAO records, MicrobiologyCaseAccessService access) {
        this(bench, cultures, records, access, new ObjectMapper());
    }

    public MicroBenchPrintServiceImpl(MicroBenchService bench, MicroCultureService cultures,
            WorkplanPrintRecordDAO records, MicrobiologyCaseAccessService access, ObjectMapper json) {
        this.bench = bench;
        this.cultures = cultures;
        this.records = records;
        this.access = access;
        this.json = json;
    }

    @Transactional
    public MicroBenchPrintForm.SavedSheet print(MicroBenchPrintForm request, String actor) {
        requireUnit(request.labUnitId, actor);
        if (request.caseIds == null || request.caseIds.isEmpty() || request.caseIds.size() > 100)
            throw new IllegalArgumentException("MICROBIOLOGY_BENCH_SELECTION");
        var query = new MicroBenchQueryForm();
        query.labUnitId = request.labUnitId;
        query.pageSize = 100;
        query.page = 1;
        java.util.List<MicroBenchPageForm.Row> selected = new ArrayList<>();
        while (true) {
            var page = bench.search(query, actor);
            selected.addAll(page.rows.stream().filter(r -> request.caseIds.contains(r.caseId)).toList());
            if (query.page * query.pageSize >= page.total)
                break;
            query.page++;
        }
        if (selected.stream().map(r -> r.caseId).distinct().count() != new HashSet<>(request.caseIds).size())
            throw new IllegalArgumentException("MICROBIOLOGY_BENCH_SELECTION");
        java.util.List<MicroBenchPageForm.Row> expanded = new ArrayList<>();
        for (var row : selected) {
            var cultureRows = cultures.getRows(row.caseId).stream().filter(c -> request.cultureIds == null
                    || request.cultureIds.isEmpty() || request.cultureIds.contains(c.id)).toList();
            if (cultureRows.isEmpty()) {
                if (request.cultureIds == null || request.cultureIds.isEmpty())
                    expanded.add(row);
            } else
                for (var culture : cultureRows) {
                    var copy = json.convertValue(row, MicroBenchPageForm.Row.class);
                    copy.culture = culture;
                    copy.cultureId = culture.id;
                    expanded.add(copy);
                }
        }
        if (request.cultureIds != null && !request.cultureIds.isEmpty()
                && expanded.stream().filter(r -> r.cultureId != null).map(r -> r.cultureId).distinct()
                        .count() != new HashSet<>(request.cultureIds).size())
            throw new IllegalArgumentException("MICROBIOLOGY_BENCH_SELECTION");
        selected = expanded;
        var record = new WorkplanPrintRecord();
        record.setLayout("MICROBIOLOGY_BENCH");
        record.setLabUnitId(request.labUnitId);
        record.setPrintedBy(actor);
        record.setPrintedAt(Timestamp.from(Instant.now()));
        record.setSysUserId(actor);
        try {
            // Retain both semantic rows and the rendered bytes. Open sheet never consults
            // live cases.
            byte[] pdf = render(record, selected);
            record.setSnapshot(
                    json.writeValueAsString(Map.of("rows", selected, "pdf", Base64.getEncoder().encodeToString(pdf))));
            records.insert(record);
            return new MicroBenchPrintForm.SavedSheet(record.getId(), record.getLabUnitId(), record.getPrintedAt());
        } catch (Exception error) {
            throw new IllegalStateException("MICROBIOLOGY_BENCH_PRINT", error);
        }
    }

    public byte[] open(String id, String actor) {
        var record = records.get(id).orElseThrow();
        requireUnit(record.getLabUnitId(), actor);
        try {
            return Base64.getDecoder().decode(json.readTree(record.getSnapshot()).get("pdf").asText());
        } catch (Exception error) {
            throw new IllegalStateException("Stored bench sheet cannot be read", error);
        }
    }

    private void requireUnit(String unit, String actor) {
        if (unit == null || !access.canReadLabUnit(actor, unit))
            throw new AccessDeniedException("Lab unit rights required");
    }

    private byte[] render(WorkplanPrintRecord record, java.util.List<MicroBenchPageForm.Row> rows) throws Exception {
        var out = new ByteArrayOutputStream();
        var doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph(MessageUtil.getMessage("microbiology.bench.sheet") + " " + record.getId()));
        doc.add(new Paragraph(rows.get(0).labUnit + " | " + record.getPrintedAt()));
        var table = new PdfPTable(5);
        table.setWidthPercentage(100);
        for (String key : java.util.List.of("microbiology.bench.accession", "microbiology.bench.patient",
                "microbiology.bench.specimen", "microbiology.bench.medium", "microbiology.bench.incubation"))
            table.addCell(MessageUtil.getMessage(key));
        for (var row : rows) {
            table.addCell(safe(row.accessionNumber) + "\n" + safe(row.caseId));
            table.addCell(safe(row.patientName) + "\n" + safe(row.origin));
            table.addCell(safe(row.specimenType) + "\n" + row.cultureTests.stream().map(t -> t.getValue())
                    .collect(java.util.stream.Collectors.joining(", ")));
            table.addCell(
                    row.culture == null ? "" : safe(row.culture.mediumName) + " / " + safe(row.culture.lotNumber));
            table.addCell(row.culture == null ? ""
                    : String.valueOf(row.culture.inoculatedAt) + "\n" + String.valueOf(row.culture.checkIntervalHours));
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
