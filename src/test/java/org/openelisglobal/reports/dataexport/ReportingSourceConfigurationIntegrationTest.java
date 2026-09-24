package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.configuration.service.ConfigurationInitializationService;
import org.openelisglobal.configuration.service.ConfigurationReloadFileResult;
import org.openelisglobal.configuration.service.ConfigurationReloadOptions;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.openelisglobal.reports.dataexport.form.ExportFilter;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ExportSubmission;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.reports.dataexport.form.SavedReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportFilters;
import org.openelisglobal.reports.dataexport.service.ReportingCatalogService;
import org.openelisglobal.reports.dataexport.service.ReportingException;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ReportingSourceConfigurationIntegrationTest extends BaseWebContextSensitiveTest {
    private ConfigurationInitializationService initializer;
    @Autowired
    private List<DomainConfigurationHandler> handlers;
    @Autowired
    private ReportDefinitionService definitions;
    @Autowired
    private ReportingCatalogService catalog;
    @Autowired
    private AnalysisService analyses;
    @Autowired
    private ResultService results;
    @Autowired
    private IStatusService statuses;
    @PersistenceContext
    private EntityManager entityManager;
    private Path configurationDirectory;
    private String json;

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/reporting-sample-testing.xml");
        try (var input = new ClassPathResource("fixtures/reporting-sources/sample-summary.json").getInputStream()) {
            json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        configurationDirectory = Files.createTempDirectory("reporting-source-configuration-");
        Files.createDirectory(configurationDirectory.resolve("reporting-sources"));
        // The shared test context deliberately excludes startup configuration.
        // Exercise the real initializer with the real Spring-managed handlers.
        initializer = org.springframework.beans.BeanUtils.instantiateClass(ConfigurationInitializationService.class);
        ReflectionTestUtils.invokeMethod(initializer, "setDomainHandlers", handlers);
        ReflectionTestUtils.setField(initializer, "configurationBaseDir", configurationDirectory.toString());
    }

    @After
    public void restoreConfiguration() throws Exception {
        if (configurationDirectory == null)
            return;
        try (var paths = Files.walk(configurationDirectory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList())
                Files.deleteIfExists(path);
        }
    }

    private void apply(String content) throws Exception {
        var handler = handlers.stream().filter(h -> h.getDomainName().equals("reporting-sources")).findFirst()
                .orElseThrow();
        handler.processConfiguration(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "sample-summary.json");
        entityManager.flush();
    }

    @Test
    public void existingInitializerLoadsAndSkipsUnchangedSourceConfiguration() throws Exception {
        Files.writeString(configurationDirectory.resolve("reporting-sources/sample-summary.json"), json);
        var options = new ConfigurationReloadOptions(Set.of("reporting-sources"), false);
        var loaded = initializer.reload(options);
        assertEquals(List.of(ConfigurationReloadFileResult.processed("reporting-sources", "sample-summary.json")),
                loaded.files());
        entityManager.flush();
        entityManager.clear();
        var stored = definitions.get("SAMPLE_SUMMARY");
        assertEquals("CSV_SOURCE", stored.getReportType());
        assertEquals("Sample summary", stored.getName());
        assertEquals(Boolean.TRUE, stored.getIsPublic());
        assertEquals(List.of(
                ConfigurationReloadFileResult.skipped("reporting-sources", "sample-summary.json", "checksum matches")),
                initializer.reload(options).files());
        assertTrue(catalog.definitions().stream().anyMatch(d -> d.id().equals("SAMPLE_SUMMARY")));
        var definition = catalog.definition("SAMPLE_SUMMARY");
        assertEquals(List.of("specimenId", "accessionNumber"), catalog.defaultColumns(definition, "SPREADSHEET"));
        assertEquals(Set.of("specimenId", "accessionNumber", "test:1", "test:2"),
                catalog.variables(definition, "SPREADSHEET").stream().map(ReportingVariable::id)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    public void emptyInitialDefaultsDoNotAllowGeneratingAnExportWithoutSelectedColumns() {
        var request = new ExportSubmission(1, "SAMPLE_TESTING", "SPREADSHEET", "empty-columns", List.of(),
                new ExportFilter("2026-08-01", "2026-08-31", List.of(), List.of(), List.of()));
        var failure = assertThrows(ReportingException.class, () -> catalog.freeze(request, TEST_SYS_USER_ID));
        assertEquals(422, failure.status());
    }

    @Test
    public void instanceConfigurationOverridesPrimaryReportDefaults() throws Exception {
        assertEquals(List.of(), catalog.defaultColumns(catalog.definition("SAMPLE_TESTING"), "SPREADSHEET"));
        apply(json.replace("SAMPLE_SUMMARY", "SAMPLE_TESTING"));
        assertEquals(List.of("specimenId", "accessionNumber"),
                catalog.defaultColumns(catalog.definition("SAMPLE_TESTING"), "SPREADSHEET"));
    }

    @Test
    public void configuredDefaultsAndSelectedResultsUseTheSameSourceInBothLayouts() throws Exception {
        apply(json);
        var analysis = analyses.get("1");
        String status = statuses.getStatusID(AnalysisStatus.Finalized);
        analysis.setStatusId(status);
        analyses.update(analysis);
        for (int repeat = 0; repeat < 2; repeat++) {
            var result = new Result();
            result.setAnalysis(analysis);
            result.setResultType("N");
            result.setValue("120");
            result.setGrouping(0);
            result.setIsReportable("Y");
            result.setSysUserId(TEST_SYS_USER_ID);
            results.insert(result);
        }
        entityManager.flush();
        var definition = catalog.definition("SAMPLE_SUMMARY");
        var filter = new ExportFilter("2023-11-15", "2023-11-15", List.of("1"), List.of("1"), List.of("FINALIZED"));
        for (String layout : definition.layouts()) {
            var selected = layout.equals("SPREADSHEET") ? List.of("specimenId", "accessionNumber", "test:1")
                    : catalog.defaultColumns(definition, layout);
            var available = catalog.variables(definition, layout).stream().collect(
                    java.util.stream.Collectors.toMap(ReportingVariable::id, java.util.function.Function.identity()));
            var snapshot = new ExportSnapshot(definition, layout, selected.stream().map(available::get).toList(),
                    filter, "UTC", List.of(status));
            StringWriter csv = new StringWriter();
            assertEquals(2, catalog.source(definition.source()).write(csv, snapshot));
            assertEquals(layout.equals("SPREADSHEET")
                    ? "\uFEFFSpecimen ID,Accession Number,Blood Test\r\n1,12345,120\r\n1,12345,120\r\n"
                    : "\uFEFFAccession Number,Result Value\r\n12345,120\r\n12345,120\r\n", csv.toString());
        }
    }

    @Test
    public void explicitDynamicDefaultsExpandInPlaceWithoutDuplicates() throws Exception {
        apply(json.replace("\"SPREADSHEET\": [\"specimenId\", \"accessionNumber\"]",
                "\"SPREADSHEET\": [\"specimenId\", \"catalog:tests\", \"accessionNumber\"]"));
        assertEquals(List.of("specimenId", "test:1", "test:2", "accessionNumber"),
                catalog.defaultColumns(catalog.definition("SAMPLE_SUMMARY"), "SPREADSHEET"));
    }

    @Test
    public void changedConfigurationRequiresANewerVersionAndPreservesIdentity() throws Exception {
        apply(json);
        var created = definitions.get("SAMPLE_SUMMARY").getLastupdated();
        apply(json);
        assertEquals(created, definitions.get("SAMPLE_SUMMARY").getLastupdated());
        String renamed = json.replace("Sample summary", "Routine sample summary");
        assertEquals("reporting.definition.versionConflict",
                assertThrows(IllegalArgumentException.class, () -> apply(renamed)).getMessage());
        assertEquals("Sample summary", catalog.definition("SAMPLE_SUMMARY").label());
        apply(renamed.replace("\"version\": 1", "\"version\": 2"));
        assertEquals(1, definitions.getAllMatching("id", "SAMPLE_SUMMARY").size());
        assertEquals(2, catalog.definition("SAMPLE_SUMMARY").version());
        assertEquals("Routine sample summary", catalog.definition("SAMPLE_SUMMARY").label());
    }

    @Test
    public void unsupportedMappingsAndWrongLayoutDefaultsCannotReplaceWorkingConfiguration() throws Exception {
        apply(json);
        String next = json.replace("\"version\": 1", "\"version\": 2");
        for (String invalid : List.of(next.replace("collectionDate", "orderDate"),
                next.replace("\"tests\"", "\"unknownCatalog\""), next.replace("testIds", "arbitraryFilter"),
                next.replace("resultValue", "unknownField"),
                next.replace("\"SPREADSHEET\": [\"specimenId\", \"accessionNumber\"]",
                        "\"SPREADSHEET\": [\"resultValue\"]"))) {
            assertThrows(IllegalArgumentException.class, () -> apply(invalid));
            assertEquals(1, catalog.definition("SAMPLE_SUMMARY").version());
            assertEquals("Sample summary", definitions.get("SAMPLE_SUMMARY").getName());
        }
    }

    @Test
    public void sourceConfigurationCannotOverwriteAnotherReportKind() {
        ReportDefinition patient = new ReportDefinition();
        patient.setId("SAMPLE_SUMMARY");
        patient.setName("Existing patient report");
        patient.setDefinitionJson("{}");
        patient.setReportType("PATIENT");
        patient.setIsActive(true);
        patient.setSysUserId(TEST_SYS_USER_ID);
        definitions.insert(patient);
        entityManager.flush();
        assertEquals("reporting.definition.identityConflict",
                assertThrows(IllegalArgumentException.class, () -> apply(json)).getMessage());
        assertEquals("Existing patient report", definitions.get("SAMPLE_SUMMARY").getName());
        assertEquals("PATIENT", definitions.get("SAMPLE_SUMMARY").getReportType());
    }

    @Test
    public void requestsAndSavedReportsCannotApplyFiltersOmittedByTheSourceDefinition() throws Exception {
        apply(json.replace("[\"labSectionIds\", \"testIds\", \"resultStatuses\"]", "[]"));
        for (var filters : List.of(new SavedReportFilters(List.of("1"), List.of(), List.of()),
                new SavedReportFilters(List.of(), List.of("1"), List.of()),
                new SavedReportFilters(List.of(), List.of(), List.of("CANCELED")))) {
            var request = new ExportSubmission(1, "SAMPLE_SUMMARY", "SPREADSHEET", "unsupported-filter",
                    List.of("accessionNumber"), new ExportFilter("2023-11-15", "2023-11-15", filters.labSectionIds(),
                            filters.testIds(), filters.resultStatuses()));
            var saved = new SavedReportDefinition(1, "SAMPLE_SUMMARY", "SPREADSHEET", List.of("accessionNumber"),
                    filters);
            var generationFailure = assertThrows(ReportingException.class,
                    () -> catalog.freeze(request, TEST_SYS_USER_ID));
            assertEquals(422, generationFailure.status());
            assertEquals("reporting.filters.unsupported", generationFailure.getMessage());
            var saveFailure = assertThrows(ReportingException.class,
                    () -> catalog.validateSaved(TEST_SYS_USER_ID, saved));
            assertEquals(422, saveFailure.status());
            assertEquals("reporting.filters.unsupported", saveFailure.getMessage());
        }
    }
}
