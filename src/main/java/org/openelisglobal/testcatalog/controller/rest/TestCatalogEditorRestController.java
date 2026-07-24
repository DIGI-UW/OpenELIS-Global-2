package org.openelisglobal.testcatalog.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testcatalog.service.CatalogHealthService;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
import org.openelisglobal.testcatalog.service.TestCatalogCreationService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;
import org.openelisglobal.testsamplehandling.service.TestSampleHandlingService;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandling;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-949 M2 / OGC-927 — unified Test Catalog editor shell backend.
 *
 * Foundation envelope only: loads the identity + which sections apply for a
 * test's domain, which the SideNav-routed editor shell hydrates from.
 * Per-section load/save lands in the section milestones (M4+). Gated by
 * ROLE_ADMIN (FR-004) — matches existing OE admin REST controllers; non-admins
 * get 403.
 *
 * Base path /rest/test-catalog avoids colliding with the existing singular
 * /rest/test/{testId}/methods namespace (research.md R10).
 */
@RestController
@RequestMapping("/rest/test-catalog")
@PreAuthorize("hasRole('ADMIN')")
public class TestCatalogEditorRestController {

    /**
     * v1 editor sections in SideNav order. Compliance (v2) is hidden entirely in
     * v1, so the v1 set is domain-independent (FR-007); the field is kept on the
     * envelope so the shell can branch once v2 lights up domain-conditional
     * visibility.
     */
    private static final List<String> V1_SECTIONS = List.of("basic-info", "sample-results", "methods", "ranges",
            "storage", "panels", "terminology", "analyzers", "display-order");

    private final TestService testService;

    private final TestResultComponentService componentService;

    private final TestResultInterpretationService interpretationService;

    private final TestResultService testResultService;

    private final ResultLimitService resultLimitService;

    private final RangeCoverageValidationService coverageService;

    private final TestSampleHandlingService handlingService;

    private final AnalyzerService analyzerService;

    private final AnalyzerTestMappingService analyzerTestMappingService;

    private final TypeOfSampleService typeOfSampleService;

    private final TypeOfSampleTestService typeOfSampleTestService;

    private final TestTerminologyMappingService terminologyService;

    private final PanelService panelService;

    private final PanelItemService panelItemService;

    // Field-injected (optional) so the existing all-args constructor used by the
    // controller's unit tests stays unchanged; only used to label dictionary
    // options.
    @Autowired(required = false)
    private DictionaryService dictionaryService;

    // Field-injected (optional) for the create-in-place flow (FR-2) and the Lab
    // Unit picker; keeps the existing test constructor unchanged.
    @Autowired(required = false)
    private TestCatalogCreationService testCatalogCreationService;

    @Autowired(required = false)
    private TestSectionService testSectionService;

    // Field-injected (optional) so the existing all-args constructor used by unit
    // tests stays unchanged; drives the FR-61–65 per-row issue tags.
    @Autowired(required = false)
    private CatalogHealthService catalogHealthService;

    // Field-injected (optional) for the FR-43 panel-create name localization.
    @Autowired(required = false)
    private LocalizationService localizationService;

    public TestCatalogEditorRestController(TestService testService, TestResultComponentService componentService,
            TestResultInterpretationService interpretationService, TestResultService testResultService,
            ResultLimitService resultLimitService, RangeCoverageValidationService coverageService,
            TestSampleHandlingService handlingService, AnalyzerService analyzerService,
            AnalyzerTestMappingService analyzerTestMappingService, TypeOfSampleService typeOfSampleService,
            TypeOfSampleTestService typeOfSampleTestService, TestTerminologyMappingService terminologyService,
            PanelService panelService, PanelItemService panelItemService) {
        this.testService = testService;
        this.componentService = componentService;
        this.interpretationService = interpretationService;
        this.testResultService = testResultService;
        this.resultLimitService = resultLimitService;
        this.coverageService = coverageService;
        this.handlingService = handlingService;
        this.analyzerService = analyzerService;
        this.analyzerTestMappingService = analyzerTestMappingService;
        this.typeOfSampleService = typeOfSampleService;
        this.typeOfSampleTestService = typeOfSampleTestService;
        this.terminologyService = terminologyService;
        this.panelService = panelService;
        this.panelItemService = panelItemService;
    }

    // ── Test List View (OGC-928) ──────────────────────────────────────────────

    public static class TestListRow {
        public String testId;
        public String name;
        public String sampleType;
        // OGC-1145 FR-9: every associated specimen, first one first; the list cell
        // renders "{first} +{n}" from this. `sampleType` stays the primary name.
        public List<String> sampleTypes = new ArrayList<>();
        public String code;
        public String domain;
        public boolean active;
        public boolean amr;
        public boolean coverageIncomplete;
        // FR-71: whether the test carries a LOINC code; drives the "No LOINC" tag.
        public boolean hasLoinc;
        // FR-61/62: catalog-health findings for this row, plus severity roll-up.
        public List<CatalogHealthService.Finding> findings = new ArrayList<>();
        public int errorCount;
        public int warningCount;
        public int infoCount;
    }

    public static class TestListPage {
        public int page;
        public int pageSize;
        public int total;
        public List<TestListRow> rows = new ArrayList<>();
        // FR-61(d): catalog-wide roll-up for the "N tests have configuration issues"
        // banner and severity counts, across the whole filtered set (not just page).
        public int totalWithIssues;
        public int totalErrors;
        public int totalWarnings;
        public int totalInfo;
    }

    @GetMapping(value = "/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public TestListPage listTests(@RequestParam(required = false) String domain,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) Boolean amr, @RequestParam(required = false) String sampleType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean issuesOnly,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "25") int pageSize) {
        // FR-61/62 findings are computed once for the whole catalog (cached) and
        // consulted both for the "issues only" filter and per-row decoration.
        Map<String, List<CatalogHealthService.Finding>> findingsByTest = catalogHealthService != null
                ? catalogHealthService.getAll()
                : Map.of();
        String searchLower = search == null ? null : search.toLowerCase(Locale.ROOT);
        // Resolve the test ids for the requested sample type once (one query),
        // rather than looking up each test's sample types while filtering.
        Set<String> sampleTypeTestIds = null;
        if (!isBlank(sampleType)) {
            sampleTypeTestIds = new HashSet<>();
            for (TypeOfSampleTest link : typeOfSampleTestService.getTypeOfSampleTestsForSampleType(sampleType)) {
                sampleTypeTestIds.add(link.getTestId());
            }
        }
        List<TestListRow> filtered = new ArrayList<>();
        for (Test test : testService.getAll()) {
            if (domain != null && !domain.isBlank() && !domain.equals(test.getDomain())) {
                continue;
            }
            boolean active = test.isActive();
            if ("active".equals(status) && !active) {
                continue;
            }
            if ("inactive".equals(status) && active) {
                continue;
            }
            boolean testAmr = Boolean.TRUE.equals(test.getAntimicrobialResistance());
            if (amr != null && amr != testAmr) {
                continue;
            }
            if (sampleTypeTestIds != null && !sampleTypeTestIds.contains(test.getId())) {
                continue;
            }
            String name = test.getName();
            if (searchLower != null && !searchLower.isBlank()
                    && (name == null || !name.toLowerCase(Locale.ROOT).contains(searchLower))) {
                continue;
            }
            List<CatalogHealthService.Finding> findings = findingsByTest.getOrDefault(test.getId(), List.of());
            if (issuesOnly && findings.isEmpty()) {
                continue;
            }
            TestListRow row = new TestListRow();
            row.testId = test.getId();
            row.name = name;
            row.code = test.getLocalCode();
            row.domain = test.getDomain();
            row.active = active;
            row.amr = testAmr;
            row.hasLoinc = !isBlank(test.getLoinc());
            // Coverage-incomplete decoration is wired with Ranges/Coverage Validation (M7).
            row.coverageIncomplete = false;
            row.findings = findings;
            for (CatalogHealthService.Finding f : findings) {
                if (f.severity == CatalogHealthService.Severity.ERROR) {
                    row.errorCount++;
                } else if (f.severity == CatalogHealthService.Severity.WARNING) {
                    row.warningCount++;
                } else {
                    row.infoCount++;
                }
            }
            filtered.add(row);
        }
        filtered.sort((a, b) -> {
            String an = a.name == null ? "" : a.name;
            String bn = b.name == null ? "" : b.name;
            return an.compareToIgnoreCase(bn);
        });

        TestListPage result = new TestListPage();
        result.total = filtered.size();
        // Catalog-wide roll-up (FR-61d) over the filtered set, for the banner counts.
        for (TestListRow r : filtered) {
            if (!r.findings.isEmpty()) {
                result.totalWithIssues++;
            }
            result.totalErrors += r.errorCount;
            result.totalWarnings += r.warningCount;
            result.totalInfo += r.infoCount;
        }
        result.pageSize = Math.max(1, pageSize);
        result.page = Math.max(1, page);
        int from = Math.min((result.page - 1) * result.pageSize, filtered.size());
        int to = Math.min(from + result.pageSize, filtered.size());
        result.rows = new ArrayList<>(filtered.subList(from, to));
        // Augment each name with its sample type — e.g. "Covid-PCR (Urine)" — using
        // the same helper the rest of the app uses (respects augmentTestNameWithType),
        // and surface the specimen in its own column (FR-39). Done on the page slice
        // only (≤ pageSize lookups).
        for (TestListRow row : result.rows) {
            Test test = testService.getTestById(row.testId);
            row.name = TestServiceImpl.getLocalizedTestNameWithType(test);
            for (TypeOfSample typeOfRow : testService.getTypeOfSamples(test)) {
                row.sampleTypes.add(typeOfRow.getLocalizedName());
            }
            row.sampleType = row.sampleTypes.isEmpty() ? null : row.sampleTypes.get(0);
        }
        return result;
    }

    public static class EditorEnvelope {
        public String testId;
        public String name;
        public String code;
        public String domain;
        public List<String> applicableSections;
    }

    // ── Create a new test (OGC-1112 FR-2..4) ──────────────────────────────────

    /** A selectable Lab Unit (test_section) for the create form. */
    public static class LabUnitOption {
        public String id;
        public String name;
    }

    @GetMapping(value = "/lab-units", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LabUnitOption> listLabUnits() {
        List<LabUnitOption> options = new ArrayList<>();
        if (testSectionService == null) {
            return options;
        }
        for (TestSection section : testSectionService.getAllTestSections()) {
            LabUnitOption option = new LabUnitOption();
            option.id = section.getId();
            option.name = section.getLocalizedName();
            options.add(option);
        }
        options.sort((a, b) -> {
            String an = a.name == null ? "" : a.name;
            String bn = b.name == null ? "" : b.name;
            return an.compareToIgnoreCase(bn);
        });
        return options;
    }

    /** Create-in-place request body (FR-2). */
    public static class CreateTestRequest {
        public String name;
        public String reportingName;
        public String code;
        public String labUnitId;
        public String sampleTypeId;
        // OGC-1145 FR-1/2: every sample type the test runs on. Takes precedence
        // over the legacy scalar when present.
        public List<String> sampleTypeIds;
        public String domain;
        public Boolean amr;
        public Boolean orderable;
        public String description;
        // Copy the source test's result components into the new test. The
        // OGC-1145 m:n model retired the specimen-variant link that used to
        // accompany this — "another specimen" is now just another sample type
        // ticked on ONE test — but component copying stays useful for creating
        // a genuinely different test modeled on an existing one.
        public String copyFromId;
    }

    public static class CreatedTest {
        public String testId;
    }

    @PostMapping(value = "/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedTest> createTest(@RequestBody CreateTestRequest body, HttpServletRequest request) {
        if (testCatalogCreationService == null) {
            return ResponseEntity.status(503).build();
        }
        List<String> desiredSampleTypes = body == null ? List.of()
                : resolveSampleTypeIds(body.sampleTypeIds, body.sampleTypeId);
        if (body == null || isBlank(body.name) || isBlank(body.reportingName) || isBlank(body.code)
                || isBlank(body.domain) || !DOMAINS.contains(body.domain) || desiredSampleTypes.isEmpty()) {
            return ResponseEntity.unprocessableEntity().build();
        }
        // D-030 domain guard (OGC-1145 FR-3): every sample type must be compatible
        // with the test's domain.
        for (String sampleTypeId : desiredSampleTypes) {
            TypeOfSample type = typeOfSampleService.get(sampleTypeId);
            if (type == null || !sampleTypeDomainCompatible(body.domain, type)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        }
        // Code uniqueness (FR-4) → 409 so the UI can flag the field.
        if (testCatalogCreationService.codeInUse(body.code)) {
            return ResponseEntity.status(409).build();
        }
        TestCatalogCreationService.CreateTestParams params = new TestCatalogCreationService.CreateTestParams();
        params.name = body.name;
        params.reportingName = body.reportingName;
        params.code = body.code;
        params.labUnitId = body.labUnitId;
        params.sampleTypeIds = desiredSampleTypes;
        params.domain = body.domain;
        params.amr = body.amr;
        params.orderable = body.orderable;
        params.description = body.description;
        String sysUserId = ControllerUtills.getSysUserId(request);
        String newId = testCatalogCreationService.createInactiveTest(params, sysUserId);
        // Copy the source's result components (incl. options/interpretations).
        // The variant-group link that used to accompany this is retired
        // (OGC-1145 Phase 3): the m:n sample-type model replaced variants.
        if (!isBlank(body.copyFromId) && testService.getTestById(body.copyFromId) != null) {
            componentService.copyComponentsFromTest(body.copyFromId, newId, sysUserId);
            invalidateHealth();
        }
        // Creating a test may have activated a previously-inactive lab unit; refresh
        // the cached section lists (which back the order-entry section filter) and
        // clear the sample-type cache so the new sample-type link is picked up once
        // the test is activated (OGC-1116). Runs post-commit — the service is done.
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
        typeOfSampleService.clearCache();
        CreatedTest created = new CreatedTest();
        created.testId = newId;
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping(value = "/tests/{testId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EditorEnvelope> getEditorEnvelope(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        EditorEnvelope envelope = new EditorEnvelope();
        envelope.testId = test.getId();
        // Name augmented with the sample type (e.g. "Covid-PCR (Urine)") so the
        // selected test is distinguishable, matching the list view.
        envelope.name = TestServiceImpl.getLocalizedTestNameWithType(test);
        envelope.code = test.getLocalCode();
        envelope.domain = test.getDomain();
        envelope.applicableSections = V1_SECTIONS;
        return ResponseEntity.ok(envelope);
    }

    // ── Localization (OGC-767) ────────────────────────────────────────────────
    // The editor's Localization section edits a test's name / reporting-name
    // translations. Those live in the generic `localization` tables (the test
    // already FK-links to them), so this only bridges testId → the backing
    // localization ids; the UI then reads/writes per-locale values through the
    // existing /rest/localizations/{id} endpoints. No per-test translation store.

    public static class LocalizationFieldRef {
        public String field;
        public String localizationId;

        public LocalizationFieldRef(String field, String localizationId) {
            this.field = field;
            this.localizationId = localizationId;
        }
    }

    public static class LocalizationRefs {
        public String testId;
        public List<LocalizationFieldRef> fields = new ArrayList<>();
    }

    @GetMapping(value = "/tests/{testId}/localization", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LocalizationRefs> getLocalizationRefs(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, String> ids = testService.getNameLocalizationIds(testId);
        LocalizationRefs refs = new LocalizationRefs();
        refs.testId = testId;
        for (String field : List.of("name", "reportingName")) {
            String localizationId = ids.get(field);
            if (localizationId != null) {
                refs.fields.add(new LocalizationFieldRef(field, localizationId));
            }
        }
        return ResponseEntity.ok(refs);
    }

    // ── LOINC integrity (OGC-1112 FR-15..18) ──────────────────────────────────
    // Analyzer / electronic-order results route by first-matching LOINC across the
    // whole active catalog (getActiveTestsByLoinc → get(0)); surface the two ways
    // that silently mis-routes: a test with no LOINC, or two active tests sharing
    // one. Warnings only — never a hard block.

    public static class TestRef {
        public String testId;
        public String name;
    }

    public static class LoincIntegrity {
        public String loinc;
        public boolean active;
        public boolean noLoinc;
        public List<TestRef> duplicates = new ArrayList<>();
    }

    @GetMapping(value = "/tests/{testId}/loinc-integrity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoincIntegrity> getLoincIntegrity(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        LoincIntegrity integrity = new LoincIntegrity();
        integrity.loinc = test.getLoinc();
        integrity.active = test.isActive();
        // A test that should receive results (active + orderable) but has no LOINC
        // can never be matched by the resolver.
        integrity.noLoinc = test.isActive() && Boolean.TRUE.equals(test.getOrderable()) && isBlank(test.getLoinc());
        if (!isBlank(test.getLoinc())) {
            for (Test other : testService.getActiveTestsByLoinc(test.getLoinc())) {
                if (other.getId() != null && !other.getId().equals(testId)) {
                    TestRef ref = new TestRef();
                    ref.testId = other.getId();
                    ref.name = TestServiceImpl.getLocalizedTestNameWithType(other);
                    integrity.duplicates.add(ref);
                }
            }
        }
        return ResponseEntity.ok(integrity);
    }

    private static final List<String> DOMAINS = List.of("CLINICAL", "ENVIRONMENTAL", "VECTOR");

    // D-030 (OGC-1145 FR-3): test.domain (CLINICAL/ENVIRONMENTAL/VECTOR) vs the
    // legacy type_of_sample.domain chars (sample_domain table: Human, Newborn,
    // Environmental, Animal). Sample types with no domain stay offerable
    // everywhere so legacy data never blocks the editor.
    private static final Map<String, Set<String>> COMPATIBLE_SAMPLE_DOMAINS = Map.of("CLINICAL", Set.of("H", "N"),
            "ENVIRONMENTAL", Set.of("E"), "VECTOR", Set.of("A"));

    private static boolean sampleTypeDomainCompatible(String testDomain, TypeOfSample type) {
        if (type == null) {
            return false;
        }
        if (isBlank(testDomain) || isBlank(type.getDomain())) {
            return true;
        }
        Set<String> allowed = COMPATIBLE_SAMPLE_DOMAINS.get(testDomain);
        return allowed == null || allowed.contains(type.getDomain());
    }

    /**
     * Desired sample-type ids for a write: the list wins when present, otherwise
     * the legacy scalar; blanks and duplicates dropped, order preserved.
     */
    private static List<String> resolveSampleTypeIds(List<String> sampleTypeIds, String sampleTypeId) {
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        if (sampleTypeIds != null) {
            for (String id : sampleTypeIds) {
                if (!isBlank(id)) {
                    resolved.add(id);
                }
            }
        } else if (!isBlank(sampleTypeId)) {
            resolved.add(sampleTypeId);
        }
        return new ArrayList<>(resolved);
    }

    /** OGC-748 Basic Info — identity + domain + AMR flag + status. */
    public static class BasicInfo {
        public String testId;
        public String name;
        public String code;
        public String description;
        public String domain;
        public String labUnitId;
        public String sampleTypeId;
        // OGC-1145 FR-1/2: all associated sample types (order preserved, primary
        // first). On write this list wins over the legacy scalar when present.
        public List<String> sampleTypeIds;
        public Boolean antimicrobialResistance;
        public Boolean active;
        public Boolean orderable;
    }

    @GetMapping(value = "/tests/{testId}/basic-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BasicInfo> getBasicInfo(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toBasicInfo(test));
    }

    @PutMapping(value = "/tests/{testId}/basic-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BasicInfo> saveBasicInfo(@PathVariable String testId, @RequestBody BasicInfo body,
            HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        if (body.domain != null && !DOMAINS.contains(body.domain)) {
            return ResponseEntity.unprocessableEntity().build();
        }
        // OGC-1145 FR-1/2/3 — validate the sample-type set up front so a rejected
        // request leaves the test untouched. Absent list + blank scalar means the
        // caller didn't send the field (partial PUT): skip reconcile entirely.
        List<String> desiredSampleTypes = resolveSampleTypeIds(body.sampleTypeIds, body.sampleTypeId);
        boolean reconcileSampleTypes = body.sampleTypeIds != null || !isBlank(body.sampleTypeId);
        if (reconcileSampleTypes) {
            boolean effectiveOrderable = body.orderable != null ? body.orderable
                    : Boolean.TRUE.equals(test.getOrderable());
            boolean effectiveActive = body.active != null ? body.active : test.isActive();
            if (desiredSampleTypes.isEmpty() && (effectiveActive || effectiveOrderable)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            String effectiveDomain = body.domain != null ? body.domain : test.getDomain();
            for (String sampleTypeId : desiredSampleTypes) {
                if (!sampleTypeDomainCompatible(effectiveDomain, typeOfSampleService.get(sampleTypeId))) {
                    return ResponseEntity.unprocessableEntity().build();
                }
            }
        }
        // The display name is localized — it is edited in the Localization section
        // (which owns the per-locale + English values), so it stays immutable here.
        // Code and description ARE editable here now (OGC-1112 dependency 8).
        if (changesImmutableField(body.name, test.getName())) {
            return ResponseEntity.unprocessableEntity().build();
        }
        if (body.code != null && !body.code.isBlank()) {
            test.setLocalCode(body.code);
        }
        if (body.description != null) {
            test.setDescription(body.description);
        }
        // Boxed flags: apply only what the caller actually sent, so a partial PUT
        // can't silently deactivate / clear AMR / un-orderable a test.
        if (body.domain != null) {
            test.setDomain(body.domain);
        }
        if (body.antimicrobialResistance != null) {
            test.setAntimicrobialResistance(body.antimicrobialResistance);
        }
        if (body.orderable != null) {
            test.setOrderable(body.orderable);
        }
        // Lab unit (test section) is editable on modify too (not just create).
        // Assigning an inactive section activates it, mirroring the create flow and
        // the legacy Test Section assignment, so the test surfaces on Add Order.
        if (!isBlank(body.labUnitId) && testSectionService != null) {
            TestSection section = testSectionService.get(body.labUnitId);
            if (section != null) {
                if ("N".equals(section.getIsActive())) {
                    section.setIsActive("Y");
                    section.setSysUserId(ControllerUtills.getSysUserId(request));
                    testSectionService.update(section);
                }
                test.setTestSection(section);
            }
        }
        // Activation (N→Y) is gated on reference-range coverage (the H-03 safety
        // gate) and must go through POST .../activate; basic-info only persists a
        // deactivation, so it cannot be used to bypass the coverage acknowledgment.
        if (body.active != null && !body.active) {
            test.setIsActive("N");
        }
        test.setSysUserId(ControllerUtills.getSysUserId(request));
        Test updated = testService.update(test);
        // OGC-1145 FR-2: reconcile the type_of_sample_test junction to the desired
        // set — delete removed links, insert added ones, and drop duplicate rows
        // for the same type (validated above, so this cannot fail mid-write).
        if (reconcileSampleTypes) {
            String sysUserId = ControllerUtills.getSysUserId(request);
            Set<String> kept = new HashSet<>();
            for (TypeOfSampleTest link : typeOfSampleTestService.getTypeOfSampleTestsForTest(testId)) {
                if (!desiredSampleTypes.contains(link.getTypeOfSampleId()) || !kept.add(link.getTypeOfSampleId())) {
                    typeOfSampleTestService.delete(link.getId(), sysUserId);
                }
            }
            for (String sampleTypeId : desiredSampleTypes) {
                if (!kept.contains(sampleTypeId)) {
                    TypeOfSampleTest link = new TypeOfSampleTest();
                    link.setTypeOfSampleId(sampleTypeId);
                    link.setTestId(testId);
                    link.setSysUserId(sysUserId);
                    typeOfSampleTestService.insert(link);
                }
            }
        }
        // Reflect active / orderable / lab-unit / sample-type changes in the cached
        // order-picker lists immediately; otherwise the change lags until an
        // unrelated refresh (same stale-cache cause as OGC-1116).
        if (body.active != null || body.orderable != null || !isBlank(body.labUnitId) || reconcileSampleTypes) {
            refreshTestCaches();
        }
        invalidateHealth();
        return ResponseEntity.ok(toBasicInfo(updated));
    }

    /**
     * Rebuild the cached order-picker + section lists after a test/section change.
     */
    private void refreshTestCaches() {
        testService.refreshTestNames();
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.ALL_TESTS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.ORDERABLE_TESTS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
        typeOfSampleService.clearCache();
    }

    /**
     * True when a non-editable field is present in the body and differs from the
     * stored value (null/blank treated as equal).
     */
    private static boolean changesImmutableField(String submitted, String current) {
        if (submitted == null) {
            return false;
        }
        return !submitted.equals(current == null ? "" : current);
    }

    private BasicInfo toBasicInfo(Test test) {
        BasicInfo info = new BasicInfo();
        info.testId = test.getId();
        info.name = test.getName();
        info.code = test.getLocalCode();
        info.description = test.getDescription();
        info.domain = test.getDomain();
        info.labUnitId = test.getTestSection() == null ? null : test.getTestSection().getId();
        info.sampleTypeIds = new ArrayList<>();
        for (TypeOfSample type : testService.getTypeOfSamples(test)) {
            info.sampleTypeIds.add(type.getId());
        }
        info.sampleTypeId = info.sampleTypeIds.isEmpty() ? null : info.sampleTypeIds.get(0);
        info.antimicrobialResistance = Boolean.TRUE.equals(test.getAntimicrobialResistance());
        info.active = test.isActive();
        info.orderable = Boolean.TRUE.equals(test.getOrderable());
        return info;
    }

    // ── Sample & Results — Result Components (OGC-749 / OGC-962) ───────────────

    /** An interpretation rule for a component (value match → text + severity). */
    public static class InterpretationDto {
        public String id;
        public String valueMatch;
        public String text;
        public String severity;
        public String color;
        public Integer displayOrder;
    }

    /** A select-list option for a (dictionary) component — a TEST_RESULT row. */
    public static class OptionDto {
        public String id;
        public String value;
        // Human-readable label for a dictionary-backed option (value holds the
        // dictionary id, which is what the save round-trip persists). Null when the
        // value isn't a resolvable dictionary id.
        public String valueName;
        public String resultType;
        public Integer sortOrder;
        public Boolean normal;
    }

    /** A labeled result field of a test (e.g. systolic, diastolic). */
    public static class ResultComponentDto {
        public String id;
        public String code;
        public String label;
        public Integer displayOrder;
        public String resultType;
        public String uomId;
        public Integer significantDigits;
        public String defaultResult;
        public Boolean allowMultipleReadings;
        // Exactly one component per test is primary — the one mirrored to the
        // legacy test columns. The service normalizes to a single primary.
        public Boolean isPrimary;
        // Per-component default for printing on the patient report (OGC-1127).
        // Null/absent = true (backward-compatible: existing components print).
        public Boolean showOnReport;
        public List<InterpretationDto> interpretations = new ArrayList<>();
        public List<OptionDto> options = new ArrayList<>();
    }

    public static class SampleResults {
        public String testId;
        public List<ResultComponentDto> components = new ArrayList<>();
    }

    @GetMapping(value = "/tests/{testId}/sample-results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SampleResults> getSampleResults(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toSampleResults(testId));
    }

    @PutMapping(value = "/tests/{testId}/sample-results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SampleResults> saveSampleResults(@PathVariable String testId, @RequestBody SampleResults body,
            HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        // Each component needs a code + label, and codes must be unique within the
        // request (the DB enforces (test_id, code) too, but reject early + cleanly).
        Set<String> codes = new HashSet<>();
        for (ResultComponentDto c : body.components) {
            if (isBlank(c.code) || isBlank(c.label)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!codes.add(c.code)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        List<TestResultComponent> desired = new ArrayList<>();
        Map<String, List<TestResultInterpretation>> interpsByCode = new HashMap<>();
        Map<String, List<TestResult>> optionsByCode = new HashMap<>();
        for (ResultComponentDto c : body.components) {
            TestResultComponent e = new TestResultComponent();
            // Set id only for an existing component, so the service inserts new ones.
            if (!isBlank(c.id)) {
                e.setId(c.id);
            }
            e.setTestId(testId);
            e.setCode(c.code);
            e.setLabel(c.label);
            e.setDisplayOrder(c.displayOrder != null ? c.displayOrder : 0);
            e.setResultType(c.resultType);
            e.setUomId(c.uomId);
            e.setSignificantDigits(c.significantDigits);
            e.setDefaultResult(c.defaultResult);
            e.setAllowMultipleReadings(Boolean.TRUE.equals(c.allowMultipleReadings));
            e.setIsPrimary(Boolean.TRUE.equals(c.isPrimary));
            e.setShowOnReport(!Boolean.FALSE.equals(c.showOnReport));
            desired.add(e);

            List<TestResultInterpretation> interps = new ArrayList<>();
            for (InterpretationDto i : c.interpretations) {
                TestResultInterpretation ie = new TestResultInterpretation();
                if (!isBlank(i.id)) {
                    ie.setId(i.id);
                }
                ie.setValueMatch(i.valueMatch);
                ie.setInterpretationText(i.text);
                ie.setSeverity(i.severity);
                ie.setColor(i.color);
                ie.setDisplayOrder(i.displayOrder != null ? i.displayOrder : 0);
                interps.add(ie);
            }
            interpsByCode.put(c.code, interps);

            List<TestResult> opts = new ArrayList<>();
            for (OptionDto o : c.options) {
                TestResult tr = new TestResult();
                if (!isBlank(o.id)) {
                    tr.setId(o.id);
                }
                tr.setValue(o.value);
                tr.setSortOrder(o.sortOrder != null ? String.valueOf(o.sortOrder) : null);
                tr.setIsNormal(Boolean.TRUE.equals(o.normal));
                // Option rows must carry the component's type ('D'/'M'/'C') —
                // result entry derives the widget from them, so a stale
                // per-option type would render the wrong control.
                tr.setTestResultType(c.resultType != null ? c.resultType : o.resultType);
                opts.add(tr);
            }
            optionsByCode.put(c.code, opts);
        }
        componentService.saveSampleResults(testId, desired, interpsByCode, optionsByCode, sysUserId);
        invalidateHealth();
        return ResponseEntity.ok(toSampleResults(testId));
    }

    /**
     * FR-65 — drop cached catalog-health findings after a write that can change
     * them.
     */
    private void invalidateHealth() {
        if (catalogHealthService != null) {
            catalogHealthService.invalidate();
        }
    }

    @PostMapping(value = "/tests/{testId}/sample-results/copy-from/{sourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SampleResults> copySampleResults(@PathVariable String testId, @PathVariable String sourceId,
            HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        componentService.copyComponentsFromTest(sourceId, testId, ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok(toSampleResults(testId));
    }

    private SampleResults toSampleResults(String testId) {
        SampleResults sr = new SampleResults();
        sr.testId = testId;
        for (TestResultComponent c : componentService.getActiveComponentsByTestId(testId)) {
            ResultComponentDto dto = new ResultComponentDto();
            dto.id = c.getId();
            dto.code = c.getCode();
            dto.label = c.getLabel();
            dto.displayOrder = c.getDisplayOrder();
            dto.resultType = c.getResultType();
            dto.uomId = c.getUomId();
            dto.significantDigits = c.getSignificantDigits();
            dto.defaultResult = c.getDefaultResult();
            dto.allowMultipleReadings = c.getAllowMultipleReadings();
            dto.isPrimary = c.getIsPrimary();
            dto.showOnReport = c.getShowOnReport();
            for (TestResultInterpretation i : interpretationService.getActiveByComponentId(c.getId())) {
                InterpretationDto idto = new InterpretationDto();
                idto.id = i.getId();
                idto.valueMatch = i.getValueMatch();
                idto.text = i.getInterpretationText();
                idto.severity = i.getSeverity();
                idto.color = i.getColor();
                idto.displayOrder = i.getDisplayOrder();
                dto.interpretations.add(idto);
            }
            for (TestResult o : testResultService.getActiveOptionsByComponentId(c.getId())) {
                OptionDto odto = new OptionDto();
                odto.id = o.getId();
                odto.value = o.getValue();
                odto.valueName = dictionaryName(o.getValue());
                odto.resultType = o.getTestResultType();
                odto.sortOrder = parseIntOrNull(o.getSortOrder());
                odto.normal = o.getIsNormal();
                dto.options.add(odto);
            }
            sr.components.add(dto);
        }
        return sr;
    }

    /** A dictionary entry for the option-search typeahead. */
    public static class DictionaryOption {
        public String id;
        public String name;
    }

    /**
     * Typeahead for select-list option values: active dictionary entries whose name
     * starts with {@code search}, capped for responsiveness. Blank search returns
     * nothing (so the control doesn't dump the whole dictionary).
     */
    @GetMapping(value = "/dictionary", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DictionaryOption> searchDictionaryOptions(@RequestParam(required = false) String search) {
        List<DictionaryOption> results = new ArrayList<>();
        if (dictionaryService == null || isBlank(search)) {
            return results;
        }
        int limit = 50;
        for (Dictionary dictionary : dictionaryService.getDictionaryEntrysByCategoryAbbreviation(search.trim(), null)) {
            if (results.size() >= limit) {
                break;
            }
            DictionaryOption option = new DictionaryOption();
            option.id = dictionary.getId();
            option.name = dictionary.getDictEntry();
            results.add(option);
        }
        return results;
    }

    /**
     * Resolve a dictionary-backed option value (a numeric dictionary id) to its
     * human label. Returns null for non-numeric / free-text values, when the id
     * doesn't resolve, or when the dictionary service isn't wired (unit tests).
     */
    private String dictionaryName(String value) {
        if (dictionaryService == null || value == null || !value.matches("\\d+")) {
            return null;
        }
        try {
            Dictionary dictionary = dictionaryService.getDictionaryById(value);
            return dictionary == null ? null : dictionary.getDictEntry();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ── Reference Ranges + Coverage Validation (OGC-969 / OGC-973) ─────────────

    private static final Set<String> RANGE_GENDERS = Set.of("M", "F");

    /**
     * A reference range row (maps to a {@link ResultLimit}). Ages are in DAYS — the
     * unit the legacy schema stores (matching {@code getDisplayAgeRange}); the
     * neonatal-bilirubin gate is inherently day-granular. Numeric bounds are
     * nullable; null means "unbounded" (serialized from / to ±Infinity).
     */
    public static class RangeDto {
        public String id;
        public String componentId;
        // OGC-1145 Phase 2: null = shared (every specimen the test runs on);
        // a value overrides this range for that sample type only.
        public String sampleTypeId;
        public String gender;
        public Double minAge;
        public Double maxAge;
        public Double lowNormal;
        public Double highNormal;
        public Double lowCritical;
        public Double highCritical;
        public Double lowValid;
        public Double highValid;
        public Double lowReporting;
        public Double highReporting;
    }

    public static class RangesResponse {
        public String testId;
        public List<RangeDto> ranges = new ArrayList<>();
        // The coverage report is computed on every load/save so the UI's per-sex
        // gap panel reflects exactly what was persisted, no separate round-trip.
        public RangeCoverageValidationService.CoverageReport coverage;
        // OGC-1145 Phase 2: the test's associated sample types, so the editor
        // can offer the per-specimen override picker without a second request.
        public List<SampleTypeOption> sampleTypes = new ArrayList<>();
    }

    @GetMapping(value = "/tests/{testId}/ranges", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RangesResponse> getRanges(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toRanges(testId));
    }

    @PutMapping(value = "/tests/{testId}/ranges", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RangesResponse> saveRanges(@PathVariable String testId, @RequestBody RangesResponse body,
            HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        // OGC-1145 Phase 2: a specimen-scoped range must target one of the
        // test's associated sample types.
        Set<String> associatedTypeIds = new HashSet<>();
        for (TypeOfSample type : testService.getTypeOfSamples(test)) {
            associatedTypeIds.add(type.getId());
        }
        for (RangeDto r : body.ranges) {
            if (r.gender != null && !r.gender.isBlank() && !RANGE_GENDERS.contains(r.gender)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            double min = r.minAge != null ? r.minAge : 0d;
            double max = r.maxAge != null ? r.maxAge : Double.POSITIVE_INFINITY;
            if (min < 0d || max <= min) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!isBlank(r.sampleTypeId) && !associatedTypeIds.contains(r.sampleTypeId)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        }
        resultLimitService.saveRangesForTest(testId, toResultLimits(body.ranges),
                ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok(toRanges(testId));
    }

    /** Maps range DTOs to ResultLimits (shared by single-test + group saves). */
    private List<ResultLimit> toResultLimits(List<RangeDto> ranges) {
        List<ResultLimit> desired = new ArrayList<>();
        for (RangeDto r : ranges) {
            ResultLimit limit = new ResultLimit();
            if (!isBlank(r.id)) {
                limit.setId(r.id);
            }
            limit.setComponentId(isBlank(r.componentId) ? null : r.componentId);
            limit.setSampleTypeId(isBlank(r.sampleTypeId) ? null : r.sampleTypeId);
            limit.setGender(isBlank(r.gender) ? null : r.gender);
            limit.setMinAge(unbox(r.minAge, 0d));
            limit.setMaxAge(unbox(r.maxAge, Double.POSITIVE_INFINITY));
            limit.setLowNormal(unbox(r.lowNormal, Double.NEGATIVE_INFINITY));
            limit.setHighNormal(unbox(r.highNormal, Double.POSITIVE_INFINITY));
            limit.setLowCritical(unbox(r.lowCritical, Double.POSITIVE_INFINITY));
            limit.setHighCritical(unbox(r.highCritical, Double.POSITIVE_INFINITY));
            limit.setLowValid(unbox(r.lowValid, Double.NEGATIVE_INFINITY));
            limit.setHighValid(unbox(r.highValid, Double.POSITIVE_INFINITY));
            // Reporting range is per-Method (not edited in this dialog); the service
            // preserves whatever the existing row already had (see saveRangesForTest).
            desired.add(limit);
        }
        return desired;
    }

    // ── Edit related tests together (OGC-1112 FR-7..14) ───────────────────────
    // A "group" is defined by the admin's selection (comma-separated ids in the
    // URL) — no stored family entity. Identity + LOINC stay per test (FR-12);
    // shared config (here: Ranges) is written to every selected test (FR-11).

    /**
     * Active tests sharing this test's name stem (the analyte's specimen siblings)
     * — the suggested set for "Edit related tests together" (FR-7). Includes self.
     */
    @GetMapping(value = "/tests/{testId}/siblings", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TestListRow> siblings(@PathVariable String testId) {
        List<TestListRow> out = new ArrayList<>();
        Test test = testService.getTestById(testId);
        if (test == null) {
            return out;
        }
        String stem = nameStem(test);
        if (stem.isEmpty()) {
            return out;
        }
        for (Test other : testService.getAllActiveTests(false)) {
            if (stem.equalsIgnoreCase(nameStem(other))) {
                TestListRow row = new TestListRow();
                row.testId = other.getId();
                row.name = TestServiceImpl.getLocalizedTestNameWithType(other);
                TypeOfSample sampleTypeOfTest = testService.getTypeOfSample(other);
                row.sampleType = sampleTypeOfTest != null ? sampleTypeOfTest.getLocalizedName() : null;
                out.add(row);
            }
        }
        return out;
    }

    /** The test name without its "(SampleType)" augmentation — the analyte stem. */
    private String nameStem(Test test) {
        String name = TestServiceImpl.getLocalizedTestNameWithType(test);
        if (name == null) {
            return "";
        }
        int paren = name.lastIndexOf('(');
        return (paren > 0 ? name.substring(0, paren) : name).trim();
    }

    public static class GroupTestSummary {
        public String testId;
        public String name;
        public String code;
        public String sampleType;
        public String loinc;
        public boolean active;
    }

    @GetMapping(value = "/group/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GroupTestSummary> groupSummary(@RequestParam String ids) {
        List<GroupTestSummary> out = new ArrayList<>();
        for (String rawId : ids.split(",")) {
            String id = rawId.trim();
            if (id.isEmpty()) {
                continue;
            }
            Test test = testService.getTestById(id);
            if (test == null) {
                continue;
            }
            GroupTestSummary summary = new GroupTestSummary();
            summary.testId = test.getId();
            summary.name = TestServiceImpl.getLocalizedTestNameWithType(test);
            summary.code = test.getLocalCode();
            summary.loinc = test.getLoinc();
            summary.active = test.isActive();
            TypeOfSample sampleTypeOfTest = testService.getTypeOfSample(test);
            summary.sampleType = sampleTypeOfTest != null ? sampleTypeOfTest.getLocalizedName() : null;
            out.add(summary);
        }
        return out;
    }

    public static class GroupRangesUpdate {
        public List<String> testIds = new ArrayList<>();
        public List<RangeDto> ranges = new ArrayList<>();
    }

    @PutMapping(value = "/group/ranges", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> saveGroupRanges(@RequestBody GroupRangesUpdate body, HttpServletRequest request) {
        if (body == null || body.testIds == null || body.testIds.isEmpty()) {
            return ResponseEntity.unprocessableEntity().build();
        }
        for (RangeDto r : body.ranges) {
            if (r.gender != null && !r.gender.isBlank() && !RANGE_GENDERS.contains(r.gender)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            double min = r.minAge != null ? r.minAge : 0d;
            double max = r.maxAge != null ? r.maxAge : Double.POSITIVE_INFINITY;
            if (min < 0d || max <= min) {
                return ResponseEntity.unprocessableEntity().build();
            }
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        for (String testId : body.testIds) {
            Test test = testService.getTestById(testId);
            if (test == null) {
                continue;
            }
            // New rows per test: the ids in the shared set belong to no single test,
            // so drop them and let each test insert its own (FR-11 per-test write).
            List<RangeDto> perTest = new ArrayList<>();
            for (RangeDto r : body.ranges) {
                RangeDto copy = new RangeDto();
                copy.componentId = r.componentId;
                copy.sampleTypeId = r.sampleTypeId;
                copy.gender = r.gender;
                copy.minAge = r.minAge;
                copy.maxAge = r.maxAge;
                copy.lowNormal = r.lowNormal;
                copy.highNormal = r.highNormal;
                copy.lowCritical = r.lowCritical;
                copy.highCritical = r.highCritical;
                copy.lowValid = r.lowValid;
                copy.highValid = r.highValid;
                perTest.add(copy);
            }
            resultLimitService.saveRangesForTest(testId, toResultLimits(perTest), sysUserId);
        }
        return ResponseEntity.ok().build();
    }

    private RangesResponse toRanges(String testId) {
        RangesResponse resp = new RangesResponse();
        resp.testId = testId;
        List<ResultLimit> limits = resultLimitService.getAllResultLimitsForTest(testId);
        for (ResultLimit l : limits) {
            RangeDto d = new RangeDto();
            d.id = l.getId();
            d.componentId = l.getComponentId();
            d.sampleTypeId = l.getSampleTypeId();
            d.gender = l.getGender();
            d.minAge = finiteOrNull(l.getMinAge());
            d.maxAge = finiteOrNull(l.getMaxAge());
            d.lowNormal = finiteOrNull(l.getLowNormal());
            d.highNormal = finiteOrNull(l.getHighNormal());
            d.lowCritical = finiteOrNull(l.getLowCritical());
            d.highCritical = finiteOrNull(l.getHighCritical());
            d.lowValid = finiteOrNull(l.getLowValid());
            d.highValid = finiteOrNull(l.getHighValid());
            d.lowReporting = finiteOrNull(l.getLowReportingRange());
            d.highReporting = finiteOrNull(l.getHighReportingRange());
            resp.ranges.add(d);
        }
        resp.coverage = coverageService.validate(limits);
        // Name the component behind each gap/overlap so the UI can say which
        // component is uncovered — only meaningful when the test has several.
        List<TestResultComponent> comps = componentService.getActiveComponentsByTestId(testId);
        if (comps.size() > 1) {
            Map<String, String> labelById = new HashMap<>();
            for (TestResultComponent c : comps) {
                labelById.put(c.getId(), isBlank(c.getLabel()) ? c.getCode() : c.getLabel());
            }
            labelCoverageComponents(resp.coverage, labelById);
        }
        // OGC-1145 Phase 2: the test's sample types feed the override picker.
        for (TypeOfSample type : testService.getTypeOfSamples(testService.getTestById(testId))) {
            SampleTypeOption option = new SampleTypeOption();
            option.id = type.getId();
            option.name = type.getLocalizedName();
            option.domain = type.getDomain();
            resp.sampleTypes.add(option);
        }
        return resp;
    }

    /** Fill in componentLabel on every gap/overlap of the coverage report. */
    private void labelCoverageComponents(RangeCoverageValidationService.CoverageReport coverage,
            Map<String, String> labelById) {
        if (coverage == null) {
            return;
        }
        for (RangeCoverageValidationService.SexCoverage sex : new RangeCoverageValidationService.SexCoverage[] {
                coverage.male, coverage.female }) {
            if (sex == null) {
                continue;
            }
            for (RangeCoverageValidationService.AgeInterval interval : sex.gaps) {
                interval.componentLabel = labelById.get(interval.componentId);
            }
            for (RangeCoverageValidationService.AgeInterval interval : sex.overlaps) {
                interval.componentLabel = labelById.get(interval.componentId);
            }
        }
    }

    /** ±Infinity / NaN → null so the bound serializes cleanly as JSON. */
    private static Double finiteOrNull(double v) {
        return Double.isFinite(v) ? v : null;
    }

    private static double unbox(Double v, double dflt) {
        return v != null ? v : dflt;
    }

    // ── Sample Storage / Handling (OGC-977..979) ──────────────────────────────

    /** Per-test storage / handling / disposal config (singleton). */
    public static class StorageDto {
        public String testId;
        public String storageCondition;
        public String storageConditionCustom;
        public Integer storageDuration;
        public String storageDurationUnit;
        public String stabilityNotes;
        public Boolean protectFromLight;
        public Boolean doNotFreeze;
        public Boolean doNotRefrigerate;
        public String disposalMethod;
        public Integer disposalTimeframe;
        public String disposalUnit;
        public String specialInstructions;
        public Boolean overrideRestricted;
    }

    @GetMapping(value = "/tests/{testId}/storage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StorageDto> getStorage(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        // No config yet → return an empty DTO (the section renders blank, not 404).
        return ResponseEntity.ok(toStorage(testId, handlingService.getByTestId(testId)));
    }

    @PutMapping(value = "/tests/{testId}/storage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StorageDto> saveStorage(@PathVariable String testId, @RequestBody StorageDto body,
            HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        TestSampleHandling saved = handlingService.saveForTest(testId, toHandling(body),
                ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok(toStorage(testId, saved));
    }

    private TestSampleHandling toHandling(StorageDto body) {
        TestSampleHandling desired = new TestSampleHandling();
        desired.setStorageCondition(isBlank(body.storageCondition) ? null : body.storageCondition);
        desired.setStorageConditionCustom(isBlank(body.storageConditionCustom) ? null : body.storageConditionCustom);
        desired.setStorageDuration(body.storageDuration);
        desired.setStorageDurationUnit(isBlank(body.storageDurationUnit) ? null : body.storageDurationUnit);
        desired.setStabilityNotes(isBlank(body.stabilityNotes) ? null : body.stabilityNotes);
        desired.setProtectFromLight(Boolean.TRUE.equals(body.protectFromLight));
        desired.setDoNotFreeze(Boolean.TRUE.equals(body.doNotFreeze));
        desired.setDoNotRefrigerate(Boolean.TRUE.equals(body.doNotRefrigerate));
        desired.setDisposalMethod(isBlank(body.disposalMethod) ? null : body.disposalMethod);
        desired.setDisposalTimeframe(body.disposalTimeframe);
        desired.setDisposalUnit(isBlank(body.disposalUnit) ? null : body.disposalUnit);
        desired.setSpecialInstructions(isBlank(body.specialInstructions) ? null : body.specialInstructions);
        desired.setOverrideRestricted(Boolean.TRUE.equals(body.overrideRestricted));
        return desired;
    }

    public static class GroupStorageUpdate {
        public List<String> testIds = new ArrayList<>();
        public StorageDto storage;
    }

    @PutMapping(value = "/group/storage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> saveGroupStorage(@RequestBody GroupStorageUpdate body, HttpServletRequest request) {
        if (body == null || body.testIds == null || body.testIds.isEmpty() || body.storage == null) {
            return ResponseEntity.unprocessableEntity().build();
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        for (String testId : body.testIds) {
            if (testService.getTestById(testId) != null) {
                handlingService.saveForTest(testId, toHandling(body.storage), sysUserId);
            }
        }
        return ResponseEntity.ok().build();
    }

    private StorageDto toStorage(String testId, TestSampleHandling h) {
        StorageDto dto = new StorageDto();
        dto.testId = testId;
        if (h == null) {
            // Empty config: explicit false flags so the UI toggles read cleanly.
            dto.protectFromLight = false;
            dto.doNotFreeze = false;
            dto.doNotRefrigerate = false;
            dto.overrideRestricted = false;
            return dto;
        }
        dto.storageCondition = h.getStorageCondition();
        dto.storageConditionCustom = h.getStorageConditionCustom();
        dto.storageDuration = h.getStorageDuration();
        dto.storageDurationUnit = h.getStorageDurationUnit();
        dto.stabilityNotes = h.getStabilityNotes();
        dto.protectFromLight = h.getProtectFromLight();
        dto.doNotFreeze = h.getDoNotFreeze();
        dto.doNotRefrigerate = h.getDoNotRefrigerate();
        dto.disposalMethod = h.getDisposalMethod();
        dto.disposalTimeframe = h.getDisposalTimeframe();
        dto.disposalUnit = h.getDisposalUnit();
        dto.specialInstructions = h.getSpecialInstructions();
        dto.overrideRestricted = h.getOverrideRestricted();
        return dto;
    }

    // ── Analyzers (read-only · OGC-959/960) ───────────────────────────────────

    /**
     * One analyzer that can run this test, derived from analyzer test-code
     * mappings. Read-only here — the source of truth is the analyzer record, edited
     * on the Analyzer configuration surface, not in this editor.
     */
    public static class AnalyzerRow {
        public String analyzerId;
        public String analyzerName;
        public String analyzerTestName;
    }

    public static class AnalyzersResponse {
        public String testId;
        public List<AnalyzerRow> analyzers = new ArrayList<>();
    }

    @GetMapping(value = "/tests/{testId}/analyzers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalyzersResponse> getAnalyzers(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        // Resolve analyzer display names in one pass (avoid an N+1 per mapping).
        Map<String, String> idToName = new HashMap<>();
        for (Analyzer a : analyzerService.getAll()) {
            idToName.put(a.getId(), a.getName());
        }
        AnalyzersResponse resp = new AnalyzersResponse();
        resp.testId = testId;
        for (AnalyzerTestMapping mapping : analyzerTestMappingService.getAllForTest(testId)) {
            AnalyzerRow row = new AnalyzerRow();
            row.analyzerId = mapping.getAnalyzerId();
            row.analyzerName = idToName.get(mapping.getAnalyzerId());
            row.analyzerTestName = mapping.getAnalyzerTestName();
            resp.analyzers.add(row);
        }
        // Stable order so the read-only table renders deterministically.
        resp.analyzers.sort((a, b) -> {
            String an = a.analyzerName == null ? "" : a.analyzerName;
            String bn = b.analyzerName == null ? "" : b.analyzerName;
            int byName = an.compareToIgnoreCase(bn);
            if (byName != 0) {
                return byName;
            }
            String at = a.analyzerTestName == null ? "" : a.analyzerTestName;
            String bt = b.analyzerTestName == null ? "" : b.analyzerTestName;
            return at.compareToIgnoreCase(bt);
        });
        return ResponseEntity.ok(resp);
    }

    // ── Display Order — tests within a sample type (OGC-983..985) ─────────────

    /** A selectable sample type for the display-order picker. */
    public static class SampleTypeOption {
        public String id;
        public String name;
        // OGC-1145 FR-3: legacy sample-domain char (H/N/E/A, may be null) so the
        // editor can enforce the D-030 domain guard client-side.
        public String domain;
    }

    /** One test's position within a sample type. */
    public static class TestOrderRow {
        public String testId;
        public String testName;
        public Integer displayOrder;
    }

    public static class DisplayOrderResponse {
        public String sampleTypeId;
        public List<TestOrderRow> tests = new ArrayList<>();
    }

    /** PUT body — the desired display order for tests within a sample type. */
    public static class TestOrderItem {
        public String testId;
        public Integer displayOrder;
    }

    public static class DisplayOrderUpdate {
        public List<TestOrderItem> items = new ArrayList<>();
    }

    @GetMapping(value = "/sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SampleTypeOption> listSampleTypes(@RequestParam(required = false) String domain) {
        List<SampleTypeOption> options = new ArrayList<>();
        for (TypeOfSample t : typeOfSampleService.getAllTypeOfSamplesSortOrdered()) {
            // OGC-1145 FR-3: an explicit test-domain filter only offers compatible
            // sample types (D-030 guard); without it all types are listed.
            if (!isBlank(domain) && !sampleTypeDomainCompatible(domain, t)) {
                continue;
            }
            SampleTypeOption o = new SampleTypeOption();
            o.id = t.getId();
            o.name = !isBlank(t.getDescription()) ? t.getDescription() : t.getLocalAbbreviation();
            o.domain = t.getDomain();
            options.add(o);
        }
        return options;
    }

    @GetMapping(value = "/sample-types/{sampleTypeId}/test-order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisplayOrderResponse> getTestOrder(@PathVariable String sampleTypeId) {
        if (typeOfSampleService.getTypeOfSampleById(sampleTypeId) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toTestOrder(sampleTypeId));
    }

    @PutMapping(value = "/sample-types/{sampleTypeId}/test-order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisplayOrderResponse> saveTestOrder(@PathVariable String sampleTypeId,
            @RequestBody DisplayOrderUpdate body, HttpServletRequest request) {
        if (typeOfSampleService.getTypeOfSampleById(sampleTypeId) == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Integer> orderByTestId = new HashMap<>();
        for (TestOrderItem item : body.items) {
            if (!isBlank(item.testId) && item.displayOrder != null) {
                orderByTestId.put(item.testId, item.displayOrder);
            }
        }
        typeOfSampleTestService.updateDisplayOrder(sampleTypeId, orderByTestId, ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok(toTestOrder(sampleTypeId));
    }

    private DisplayOrderResponse toTestOrder(String sampleTypeId) {
        DisplayOrderResponse resp = new DisplayOrderResponse();
        resp.sampleTypeId = sampleTypeId;
        for (TypeOfSampleTest junction : typeOfSampleTestService.getTypeOfSampleTestsForSampleType(sampleTypeId)) {
            TestOrderRow row = new TestOrderRow();
            row.testId = junction.getTestId();
            Test test = testService.getTestById(junction.getTestId());
            row.testName = test != null ? test.getName() : null;
            row.displayOrder = junction.getDisplayOrder();
            resp.tests.add(row);
        }
        // Sort by displayOrder (nulls last), then name — a deterministic order.
        resp.tests.sort((a, b) -> {
            int ao = a.displayOrder != null ? a.displayOrder : Integer.MAX_VALUE;
            int bo = b.displayOrder != null ? b.displayOrder : Integer.MAX_VALUE;
            if (ao != bo) {
                return Integer.compare(ao, bo);
            }
            String an = a.testName == null ? "" : a.testName;
            String bn = b.testName == null ? "" : b.testName;
            return an.compareToIgnoreCase(bn);
        });
        return resp;
    }

    // ── Terminology Mappings (OGC-957..958) ───────────────────────────────────

    private static final Set<String> TERM_SOURCES = Set.of("LOINC", "SNOMED", "CIEL", "OCL");

    private static final Set<String> TERM_RELATIONSHIPS = Set.of("SAME_AS", "BROADER_THAN", "NARROWER_THAN");

    /** One terminology mapping: a standard-terminology code for this test. */
    public static class MappingDto {
        public String id;
        public String source;
        public String code;
        public String relationship;
        // Human-readable label for the standard term (FR-69), e.g. LOINC long name.
        public String displayName;
        // Null = test-level mapping (default). Otherwise the id of a result
        // component of this test that the mapping is scoped to (OGC-1128).
        public String componentId;
        // OGC-1145 FR-13: null = shared (applies to every specimen the test
        // runs on); otherwise the mapping overrides for that sample type only.
        public String sampleTypeId;
    }

    /** A result component this test's mappings may be scoped to. */
    public static class TerminologyComponentDto {
        public String id;
        public String code;
        public String label;
    }

    public static class TerminologyResponse {
        public String testId;
        public List<MappingDto> mappings = new ArrayList<>();
        // The test's active components, so the editor can offer an "Applies to"
        // scope per mapping row without a second request.
        public List<TerminologyComponentDto> components = new ArrayList<>();
        // OGC-1145 FR-13: the test's associated sample types, so the editor can
        // offer the per-specimen override picker without a second request.
        public List<SampleTypeOption> sampleTypes = new ArrayList<>();
    }

    @GetMapping(value = "/tests/{testId}/terminology", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TerminologyResponse> getTerminology(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toTerminology(testId));
    }

    @PutMapping(value = "/tests/{testId}/terminology", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TerminologyResponse> saveTerminology(@PathVariable String testId,
            @RequestBody TerminologyResponse body, HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        // Valid scopes: null (test-level) or the id of an active component of this
        // test.
        Set<String> componentIds = new HashSet<>();
        for (TestResultComponent c : componentService.getActiveComponentsByTestId(testId)) {
            componentIds.add(c.getId());
        }
        // OGC-1145 FR-13: a specimen-scoped mapping must target one of the
        // test's associated sample types.
        Set<String> associatedTypeIds = new HashSet<>();
        for (TypeOfSample type : testService.getTypeOfSamples(test)) {
            associatedTypeIds.add(type.getId());
        }
        Set<String> seen = new HashSet<>();
        List<TestTerminologyMapping> desired = new ArrayList<>();
        for (MappingDto m : body.mappings) {
            // Source must be a known terminology; code required; relationship (if
            // present) must be a known qualifier.
            if (isBlank(m.source) || !TERM_SOURCES.contains(m.source) || isBlank(m.code)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!isBlank(m.relationship) && !TERM_RELATIONSHIPS.contains(m.relationship)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            // A scoped mapping must target a real component of this test.
            String componentId = isBlank(m.componentId) ? null : m.componentId;
            if (componentId != null && !componentIds.contains(componentId)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            String sampleTypeId = isBlank(m.sampleTypeId) ? null : m.sampleTypeId;
            if (sampleTypeId != null && !associatedTypeIds.contains(sampleTypeId)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            // (component, sample type, source, code) unique within the request — the
            // DB enforces it per test/scope, but reject early + cleanly rather than
            // surfacing a raw 500.
            if (!seen.add((componentId == null ? "" : componentId) + " " + (sampleTypeId == null ? "" : sampleTypeId)
                    + " " + m.source + " " + m.code)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            TestTerminologyMapping e = new TestTerminologyMapping();
            e.setComponentId(componentId);
            e.setSampleTypeId(sampleTypeId);
            e.setSource(m.source);
            e.setCode(m.code);
            e.setRelationship(isBlank(m.relationship) ? null : m.relationship);
            e.setDisplayName(isBlank(m.displayName) ? null : m.displayName.trim());
            desired.add(e);
        }
        terminologyService.saveMappingsForTest(testId, desired, ControllerUtills.getSysUserId(request));
        invalidateHealth();
        return ResponseEntity.ok(toTerminology(testId));
    }

    private TerminologyResponse toTerminology(String testId) {
        TerminologyResponse resp = new TerminologyResponse();
        resp.testId = testId;
        for (TestTerminologyMapping m : terminologyService.getActiveByTestId(testId)) {
            MappingDto dto = new MappingDto();
            dto.id = m.getId();
            dto.source = m.getSource();
            dto.code = m.getCode();
            dto.relationship = m.getRelationship();
            dto.displayName = m.getDisplayName();
            dto.componentId = m.getComponentId();
            dto.sampleTypeId = m.getSampleTypeId();
            resp.mappings.add(dto);
        }
        for (TestResultComponent c : componentService.getActiveComponentsByTestId(testId)) {
            TerminologyComponentDto cd = new TerminologyComponentDto();
            cd.id = c.getId();
            cd.code = c.getCode();
            cd.label = c.getLabel();
            resp.components.add(cd);
        }
        for (TypeOfSample type : testService.getTypeOfSamples(testService.getTestById(testId))) {
            SampleTypeOption option = new SampleTypeOption();
            option.id = type.getId();
            option.name = type.getLocalizedName();
            option.domain = type.getDomain();
            resp.sampleTypes.add(option);
        }
        return resp;
    }

    // ── Panels — this test's panel memberships (OGC-980..982) ─────────────────

    /** A selectable panel for the add-to-panel typeahead. */
    public static class PanelOption {
        public String id;
        public String name;
    }

    /** A panel this test belongs to, and its position within that panel. */
    public static class PanelMembership {
        public String panelId;
        public String panelName;
        public Integer position;
    }

    public static class TestPanelsResponse {
        public String testId;
        public List<PanelMembership> memberships = new ArrayList<>();
    }

    /** A test within a panel — the read-only preview for the position editor. */
    public static class PanelTestRow {
        public String testId;
        public String testName;
        public Integer position;
    }

    public static class PanelTestOrderResponse {
        public String panelId;
        public List<PanelTestRow> tests = new ArrayList<>();
    }

    public static class MembershipItem {
        public String panelId;
        public Integer position;
    }

    public static class PanelMembershipUpdate {
        public List<MembershipItem> memberships = new ArrayList<>();
    }

    @GetMapping(value = "/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PanelOption> listPanels() {
        List<PanelOption> options = new ArrayList<>();
        for (Panel p : panelService.getAllActivePanels()) {
            PanelOption o = new PanelOption();
            o.id = p.getId();
            o.name = p.getPanelName();
            options.add(o);
        }
        return options;
    }

    /**
     * Name-only inline panel create (OGC-1112 FR-43). Further config is done in
     * Panel Management.
     */
    public static class CreatePanelRequest {
        public String name;
    }

    @PostMapping(value = "/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PanelOption> createPanel(@RequestBody CreatePanelRequest body, HttpServletRequest request) {
        if (body == null || isBlank(body.name)) {
            return ResponseEntity.unprocessableEntity().build();
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        String name = body.name.trim();
        // panel.name_localization_id is NOT NULL — create the name localization
        // first, mirroring the legacy panel-add flow.
        Localization nameLocalization = LocalizationServiceImpl.createNewLocalization(name, name,
                LocalizationServiceImpl.LocalizationType.PANEL_NAME);
        nameLocalization.setSysUserId(sysUserId);
        String localizationId = localizationService.insert(nameLocalization);

        Panel panel = new Panel();
        panel.setPanelName(name);
        panel.setDescription(name);
        panel.setLocalization(localizationService.get(localizationId));
        panel.setIsActive("Y");
        panel.setSortOrderInt(Integer.MAX_VALUE);
        panel.setSysUserId(sysUserId);
        String id = panelService.insert(panel);
        PanelOption created = new PanelOption();
        created.id = id;
        created.name = panel.getPanelName();
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping(value = "/tests/{testId}/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestPanelsResponse> getTestPanels(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toTestPanels(testId));
    }

    @GetMapping(value = "/panels/{panelId}/test-order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PanelTestOrderResponse> getPanelTestOrder(@PathVariable String panelId) {
        if (panelService.getPanelById(panelId) == null) {
            return ResponseEntity.notFound().build();
        }
        PanelTestOrderResponse resp = new PanelTestOrderResponse();
        resp.panelId = panelId;
        for (PanelItem pi : panelItemService.getPanelItemsForPanel(panelId)) {
            PanelTestRow row = new PanelTestRow();
            row.testId = pi.getTest() != null ? pi.getTest().getId() : null;
            row.testName = pi.getTest() != null ? pi.getTest().getName() : null;
            row.position = parseIntOrNull(pi.getSortOrder());
            resp.tests.add(row);
        }
        resp.tests.sort((a, b) -> {
            int ao = a.position != null ? a.position : Integer.MAX_VALUE;
            int bo = b.position != null ? b.position : Integer.MAX_VALUE;
            if (ao != bo) {
                return Integer.compare(ao, bo);
            }
            String an = a.testName == null ? "" : a.testName;
            String bn = b.testName == null ? "" : b.testName;
            return an.compareToIgnoreCase(bn);
        });
        return ResponseEntity.ok(resp);
    }

    @PutMapping(value = "/tests/{testId}/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestPanelsResponse> saveTestPanels(@PathVariable String testId,
            @RequestBody PanelMembershipUpdate body, HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Integer> positionByPanelId = new HashMap<>();
        int fallback = 1;
        for (MembershipItem item : body.memberships) {
            if (!isBlank(item.panelId)) {
                // Reject an unknown panel up front rather than letting the service
                // silently drop the membership (mirrors the terminology 422 above).
                if (panelService.getPanelById(item.panelId) == null) {
                    return ResponseEntity.unprocessableEntity().build();
                }
                positionByPanelId.put(item.panelId, item.position != null ? item.position : fallback);
            }
            fallback++;
        }
        panelItemService.setMembershipsForTest(test, positionByPanelId, ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok(toTestPanels(testId));
    }

    private TestPanelsResponse toTestPanels(String testId) {
        TestPanelsResponse resp = new TestPanelsResponse();
        resp.testId = testId;
        for (PanelItem pi : panelItemService.getPanelItemByTestId(testId)) {
            PanelMembership m = new PanelMembership();
            m.panelId = pi.getPanel() != null ? pi.getPanel().getId() : null;
            m.panelName = pi.getPanel() != null ? pi.getPanel().getPanelName() : null;
            m.position = parseIntOrNull(pi.getSortOrder());
            resp.memberships.add(m);
        }
        resp.memberships.sort((a, b) -> {
            String an = a.panelName == null ? "" : a.panelName;
            String bn = b.panelName == null ? "" : b.panelName;
            return an.compareToIgnoreCase(bn);
        });
        return resp;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
