package org.openelisglobal.reports.dataexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.reports.dataexport.form.SavedReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportFilters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The shared saved reports Custom Data Export ships with (OGC-1652): "Referrals
 * by location" and "Non-conformance by location". Each lists the report type's
 * usual columns and every Location column. The location columns follow the
 * organizations above facilities in Organization Management, which can change
 * at any time, so a shipped report always opens with the columns of the day and
 * is rewritten when the application starts. A report somebody changed or
 * removed is left as they left it.
 */
@Service
public class PredefinedExportReports {

    public static final String REFERRALS_ID = "CSV-PREDEFINED-REFERRALS-BY-LOCATION";
    public static final String NON_CONFORMANCE_ID = "CSV-PREDEFINED-NC-BY-LOCATION";
    public static final String ROUTINE_ID = "CSV-PREDEFINED-ROUTINE-SAMPLES";
    private static final String TYPE = "CSV_SAVED";
    private static final List<String> NON_CONFORMANCE_COLUMNS = List.of("ncAccessionNumber", "rejectionReason",
            "rejectionDate", "rejectionStage", "ncStatus");
    /**
     * The Routine CSV Report's columns, in its order (OGC-1655): Lab No, Lab Unit,
     * Identifier, Sex, Birth Date, Age (Y/M/W), Date Received, Date Entered, Date
     * Collected, Referrer Code, Referrer, Program, Status (the order's); then every
     * test.
     */
    private static final List<String> ROUTINE_COLUMNS = List.of("accessionNumber", "labUnit", "nationalId", "sex",
            "dateOfBirth", "ageYears", "ageMonths", "ageWeeks", "receivedDate", "enteredDate", "collectionDate",
            "referringSiteCode", "referringSite", "program", "orderStatus");

    private record Predefined(String id, String name, String reportType, String layout, String dateBasis,
            boolean byLocation) {
    }

    private static final List<Predefined> REPORTS = List.of(
            new Predefined(REFERRALS_ID, "Referrals by location", "REFERRALS", "TABLE", null, true),
            new Predefined(NON_CONFORMANCE_ID, "Non-conformance by location", "NON_CONFORMANCE", "TABLE", null, true),
            new Predefined(ROUTINE_ID, "Routine sample export", "SAMPLE_TESTING", "SPREADSHEET", "orderEnteredDate",
                    false));

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ReportDefinitionService definitions;
    @Autowired
    private ReportingCatalogService catalog;
    @Autowired
    private ExportLocations locations;
    @Autowired
    @Qualifier("daemonSysUserId")
    private String daemonSysUserId;

    /** The number of reports written: created, or rewritten to current columns. */
    @Transactional
    public int ensure() {
        int written = 0;
        for (Predefined report : REPORTS) {
            written += ensure(report);
        }
        return written;
    }

    /**
     * The definition a saved report opens with: for a shipped report nobody has
     * changed, its columns as of now, the test columns limited to the viewer's lab
     * sections; otherwise as stored.
     */
    public SavedReportDefinition current(ReportDefinition stored, SavedReportDefinition saved, String viewer) {
        Optional<Predefined> report = REPORTS.stream().filter(item -> item.id().equals(stored.getId())).findFirst();
        if (report.isEmpty() || !shipped(stored)) {
            return saved;
        }
        SavedReportDefinition current = definition(report.get());
        return new SavedReportDefinition(current.schemaVersion(), current.reportType(), current.layout(),
                catalog.permittedFor(viewer, current.selectedVariables()), current.filters());
    }

    private boolean shipped(ReportDefinition stored) {
        return daemonSysUserId.equals(stored.getUpdatedBy());
    }

    private SavedReportDefinition definition(Predefined report) {
        var source = catalog.definition(report.reportType());
        List<String> columns = switch (report.id()) {
        case NON_CONFORMANCE_ID -> NON_CONFORMANCE_COLUMNS;
        case ROUTINE_ID -> routineColumns(source);
        default -> catalog.defaultColumns(source, report.layout());
        };
        LinkedHashSet<String> selected = new LinkedHashSet<>(columns);
        if (report.byLocation()) {
            selected.addAll(locations.fieldIds());
        }
        return new SavedReportDefinition(1, report.reportType(), report.layout(), new ArrayList<>(selected),
                new SavedReportFilters(List.of(), List.of(), List.of(), report.dateBasis()));
    }

    private List<String> routineColumns(ReportSourceConfig source) {
        List<String> columns = new ArrayList<>(ROUTINE_COLUMNS);
        catalog.variables(source, "SPREADSHEET").stream().filter(field -> "tests".equals(field.group()))
                .map(ReportingVariable::id).forEach(columns::add);
        return columns;
    }

    private int ensure(Predefined report) {
        String json = write(definition(report));
        Timestamp now = Timestamp.from(Instant.now().truncatedTo(ChronoUnit.MICROS));
        Optional<ReportDefinition> existing = definitions.getAllMatching("reportType", TYPE).stream()
                .filter(definition -> report.id().equals(definition.getId())).findFirst();
        if (existing.isPresent()) {
            ReportDefinition stored = existing.get();
            if (!Boolean.TRUE.equals(stored.getIsActive()) || !shipped(stored)
                    || json.equals(stored.getDefinitionJson())) {
                return 0;
            }
            stored.setDefinitionJson(json);
            stored.setLastupdated(now);
            stored.setSysUserId(daemonSysUserId);
            definitions.update(stored);
            return 1;
        }
        ReportDefinition created = new ReportDefinition();
        created.setId(report.id());
        created.setName(report.name());
        created.setDescription("Predefined configurable CSV report");
        created.setCategory("Data export");
        created.setDefinitionJson(json);
        created.setCreatedBy(daemonSysUserId);
        created.setUpdatedBy(daemonSysUserId);
        created.setCreatedDate(now);
        created.setLastupdated(now);
        created.setIsActive(true);
        created.setIsPublic(true);
        created.setReportType(TYPE);
        created.setSysUserId(daemonSysUserId);
        definitions.insert(created);
        return 1;
    }

    private String write(SavedReportDefinition definition) {
        try {
            return mapper.writeValueAsString(definition);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("A predefined export report could not be written", e);
        }
    }
}
