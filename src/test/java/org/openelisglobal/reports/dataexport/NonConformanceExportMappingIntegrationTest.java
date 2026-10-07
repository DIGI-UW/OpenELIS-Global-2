package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.reports.dataexport.dao.NonConformanceExportDAO;
import org.openelisglobal.reports.dataexport.form.ExportFilter;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.service.ReportingCatalogService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class NonConformanceExportMappingIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private NonConformanceExportDAO occurrences;
    @Autowired
    private ReportingCatalogService catalog;
    @PersistenceContext
    private EntityManager entityManager;

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/reporting-sample-testing.xml");
    }

    @Test
    public void bundledDefinitionUsesTheCommonCatalogWithExplicitColumnSelection() {
        var definition = catalog.definition("NON_CONFORMANCE");
        assertEquals("eventOrRecordedDate", definition.dateAnchor());
        assertEquals(List.of("TABLE"), definition.layouts());
        assertTrue(catalog.defaultColumns(definition, "TABLE").isEmpty());
        var fields = catalog.variables(definition, "TABLE").stream().map(field -> field.id()).toList();
        assertTrue(fields.containsAll(List.of("ncAccessionNumber", "rejectionReason", "rejectionDate", "rejectionStage",
                "rejectedBy", "ncDateBasis", "ncEventDate", "ncRecordedDate", "ncStatus")));
    }

    @Test
    public void knownEventDateWinsAndRecordedOnlyEventsRemainEligible() {
        var known = event("2026-05-05", "2026-05-08");
        var recorded = event(null, "2026-05-05");
        var outside = event("2026-05-04", "2026-05-05");
        var undated = event(null, null);
        entityManager.flush();
        try (var rows = occurrences.events(request("2026-05-05", "2026-05-05"))) {
            var ids = rows.map(NonConformanceExportDAO.EventRow::eventId).toList();
            assertTrue(ids.containsAll(List.of(known.getId(), recorded.getId())));
            assertFalse(ids.contains(outside.getId()));
            assertFalse(ids.contains(undated.getId()));
        }
    }

    @Test
    public void rangeIncludesBothDaysAndDoesNotCollapseEqualEvents() {
        var first = event("2026-05-05", null);
        var last = event(null, "2026-05-06");
        var equal = event(null, "2026-05-06");
        var before = event(null, "2026-05-04");
        var after = event("2026-05-07", null);
        entityManager.flush();
        try (var rows = occurrences.events(request("2026-05-05", "2026-05-06"))) {
            var ids = rows.map(NonConformanceExportDAO.EventRow::eventId).toList();
            assertTrue(ids.containsAll(List.of(first.getId(), last.getId(), equal.getId())));
            assertFalse(ids.contains(before.getId()));
            assertFalse(ids.contains(after.getId()));
        }
    }

    @Test
    public void specimenReasonUsesItsOwnCatalogAndLegacyOccurrenceStaysSeparate() throws Exception {
        executeDataSetWithStateManagement("testdata/reporting-nonconformance.xml");
        var specimen = entityManager.find(SampleItem.class, "1");
        specimen.setRejected(true);
        specimen.setRejectReasonId("90001");
        var first = event(null, "2026-05-05");
        var repeated = event(null, "2026-05-05");
        for (var event : List.of(first, repeated)) {
            event.setNceTypeId(90001);
            var link = new NceSpecimen();
            link.setNceId(event.getId());
            link.setSampleItemId(1);
            entityManager.persist(link);
        }
        entityManager.flush();
        try (var rows = occurrences.events(request("2026-05-05", "2026-05-05"))) {
            var matched = rows.filter(row -> List.of(first.getId(), repeated.getId()).contains(row.eventId())).toList();
            assertEquals(2, matched.size());
            assertNotEquals(matched.get(0).linkId(), matched.get(1).linkId());
            for (var row : matched) {
                assertEquals("1", row.specimenId());
                assertEquals("12345", row.accession());
                assertEquals("Haemolysed", row.reason());
            }
        }
        try (var rows = occurrences.rejections(request("2026-05-05", "2026-05-05"))) {
            var legacy = rows.toList();
            assertEquals(1, legacy.size());
            assertEquals("90001", legacy.get(0).id());
            assertEquals("Haemolysed", legacy.get(0).reason());
            assertEquals("1", legacy.get(0).specimenId());
        }
    }

    private NcEvent event(String happened, String recorded) {
        var event = new NcEvent();
        event.setDateOfEvent(happened == null ? null : Date.valueOf(happened));
        event.setReportDate(recorded == null ? null : Date.valueOf(recorded));
        event.setReportingUnitId(1);
        event.setName("Synthetic reporting date fixture");
        entityManager.persist(event);
        return event;
    }

    private ExportSnapshot request(String from, String to) {
        var definition = new ReportSourceConfig("NON_CONFORMANCE", 1, "Non-Conformance", "NON_CONFORMANCE",
                "eventOrRecordedDate", List.of("TABLE"), List.of("ncOccurrenceId"), List.of(), List.of("labSectionIds"),
                Map.of("TABLE", List.of()));
        return new ExportSnapshot(definition, "TABLE", List.of(),
                new ExportFilter(from, to, List.of("1"), List.of(), List.of()), "UTC", List.of());
    }
}
