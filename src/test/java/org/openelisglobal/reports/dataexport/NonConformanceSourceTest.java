package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.StringWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Test;
import org.openelisglobal.reports.dataexport.dao.NonConformanceExportDAO;
import org.openelisglobal.reports.dataexport.dao.NonConformanceExportDAO.EventRow;
import org.openelisglobal.reports.dataexport.dao.NonConformanceExportDAO.RejectionRow;
import org.openelisglobal.reports.dataexport.form.ExportFilter;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.service.NonConformanceSource;
import org.openelisglobal.reports.dataexport.service.ReportingCsvWriter;

public class NonConformanceSourceTest {
    @Test
    public void csvRetainsDatesBasisAndIndependentEqualOccurrences() throws Exception {
        var dao = mock(NonConformanceExportDAO.class);
        var source = new NonConformanceSource(dao, new ReportingCsvWriter());
        List<String> ids = List.of("ncOccurrenceId", "ncAccessionNumber", "rejectionDate", "ncDateBasis", "ncEventDate",
                "ncRecordedDate", "rejectionReason");
        var fields = ids.stream()
                .map(id -> source.catalog().stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow()).toList();
        var definition = new ReportSourceConfig("NON_CONFORMANCE", 1, "Non-Conformance", "NON_CONFORMANCE",
                "eventOrRecordedDate", List.of("TABLE"), ids, List.of(), List.of("labSectionIds"),
                Map.of("TABLE", List.of()));
        var request = new ExportSnapshot(definition, "TABLE", fields,
                new ExportFilter("2026-05-05", "2026-05-05", List.of("1"), List.of(), List.of()), "America/Los_Angeles",
                List.of());
        source.validateConfiguration(definition);
        when(dao.events(request)).thenReturn(Stream.of(
                new EventRow(1, 10, "20", "PUBLIC-NC", "Haemolysed", "Reception", "Demo", Date.valueOf("2026-05-05"),
                        Date.valueOf("2026-05-08"), "OPEN"),
                new EventRow(2, 11, "20", "PUBLIC-NC", "Haemolysed", "Reception", "Demo", null,
                        Date.valueOf("2026-05-05"), "OPEN"),
                new EventRow(3, 12, "20", "PUBLIC-NC", "Haemolysed", "Reception", "Demo", null,
                        Date.valueOf("2026-05-05"), "OPEN")));
        when(dao.rejections(request)).thenReturn(Stream.of(new RejectionRow("1", "20", "PUBLIC-NC", "Haemolysed",
                Timestamp.from(Instant.parse("2026-05-06T06:30:00Z")))));
        var output = new StringWriter();
        assertEquals(4, source.write(output, request));
        String csv = output.toString();
        assertTrue(csv.contains("event:1:link:10,PUBLIC-NC,2026-05-05,Event date,2026-05-05,2026-05-08,Haemolysed"));
        assertTrue(csv.contains("event:2:link:11,PUBLIC-NC,2026-05-05,Recorded date,,2026-05-05,Haemolysed"));
        assertTrue(csv.contains("event:3:link:12,PUBLIC-NC,2026-05-05,Recorded date,,2026-05-05,Haemolysed"));
        assertTrue(csv.contains("rejection:1,PUBLIC-NC,2026-05-05,Recorded rejection date,,2026-05-05,Haemolysed"));
    }
}
