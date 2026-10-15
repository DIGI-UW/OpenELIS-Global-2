package org.openelisglobal.testcatalog.controller.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerTestCapability;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.StaleSaveGuard;
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
import org.openelisglobal.qc.valueholder.TestQcThreshold;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testcatalog.service.CatalogHealthService;
import org.openelisglobal.testcatalog.service.LoincIntegrityService;
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
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
            "qc-targets", "storage", "panels", "terminology", "analyzers", "display-order");

    private final TestService testService;

    private final TestResultComponentService componentService;

    private final TestResultInterpretationService interpretationService;

    private final TestResultService testResultService;

    private final ResultLimitService resultLimitService;

    private final RangeCoverageValidationService coverageService;

    private final TestSampleHandlingService handlingService;

    private final AnalyzerService analyzerService;

    private final TypeOfSampleService typeOfSampleService;

    private final TypeOfSampleTestService typeOfSampleTestService;

    private final TestTerminologyMappingService terminologyService;

    private final PanelService panelService;

    private final PanelItemService panelItemService;

    // Field-injected (optional) so the existing all-args constructor used by the
    // controller's unit tests stays unchanged; only used to label dictionary
    // options.
    @Autowired
    private StaleSaveGuard staleSaveGuard;

    @Autowired(required = false)
    private DictionaryService dictionaryService;

    // Field-injected (optional) for the create-in-place flow (FR-2) and the Lab
    // Unit picker; keeps the existing test constructor unchanged.
    @Autowired(required = false)
    private TestCatalogCreationService testCatalogCreationService;

    @Autowired(required = false)
    private org.openelisglobal.unitofmeasure.service.UnitOfMeasureService unitOfMeasureService;

    @Autowired(required = false)
    private TestSectionService testSectionService;

    // Field-injected (optional) so the existing all-args constructor used by unit
    // tests stays unchanged; drives the FR-61–65 per-row issue tags.
    @Autowired(required = false)
    private CatalogHealthService catalogHealthService;

    // Field-injected (optional) for the FR-43 panel-create name localization.
    @Autowired(required = false)
    private LocalizationService localizationService;

    // Field-injected (optional) for the OGC-224 SAMPLETYPE_PANEL sync on
    // membership writes (order entry reads that junction).
    @Autowired(required = false)
    private org.openelisglobal.typeofsample.service.TypeOfSamplePanelService typeOfSamplePanelService;

    // Field-injected (optional) for the OGC-224 panel Terminology section.
    @Autowired(required = false)
    private org.openelisglobal.panelterminology.service.PanelTerminologyMappingService panelTerminologyService;

    // Field-injected (optional) for an answer's terminology.
    @Autowired(required = false)
    private org.openelisglobal.dictionaryterminology.service.DictionaryTerminologyMappingService answerTerminologyService;

    public TestCatalogEditorRestController(TestService testService, TestResultComponentService componentService,
            TestResultInterpretationService interpretationService, TestResultService testResultService,
            ResultLimitService resultLimitService, RangeCoverageValidationService coverageService,
            TestSampleHandlingService handlingService, AnalyzerService analyzerService,
            TypeOfSampleService typeOfSampleService, TypeOfSampleTestService typeOfSampleTestService,
            TestTerminologyMappingService terminologyService, PanelService panelService,
            PanelItemService panelItemService) {
        this.testService = testService;
        this.componentService = componentService;
        this.interpretationService = interpretationService;
        this.testResultService = testResultService;
        this.resultLimitService = resultLimitService;
        this.coverageService = coverageService;
        this.handlingService = handlingService;
        this.analyzerService = analyzerService;
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
            @RequestParam(required = false) String labUnit, @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean issuesOnly,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "25") int pageSize) {
        // FR-61/62 findings are computed once for the whole catalog (cached) and
        // consulted both for the "issues only" filter and per-row decoration.
        Map<String, List<CatalogHealthService.Finding>> findingsByTest = catalogHealthService != null
                ? catalogHealthService.getAll()
                : Map.of();
        // A test is LOINC-identifiable through the legacy column OR any active LOINC
        // mapping — including one scoped to a component or a single specimen. Resolved
        // in one query so decorating the rows below stays a set lookup.
        Set<String> loincMappedTestIds = terminologyService.getTestIdsWithActiveSource("LOINC");
        String searchLower = search == null ? null : search.toLowerCase(Locale.ROOT);
        // Resolve the test ids for the requested sample type once (one query),
        // rather than looking up each test's sample types while filtering.
        Set<String> sampleTypeTestIds = null;
        if (!isBlank(sampleType)) {
            sampleTypeTestIds = new HashSet<>();
            List<TypeOfSampleTest> links = TestCatalogNumericIdGuard.isNumericId(sampleType)
                    ? typeOfSampleTestService.getTypeOfSampleTestsForSampleType(sampleType.trim())
                    : List.of();
            for (TypeOfSampleTest link : links) {
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
            if (!isBlank(labUnit)
                    && (test.getTestSection() == null || !labUnit.equals(test.getTestSection().getId()))) {
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
            row.hasLoinc = !isBlank(test.getLoinc()) || loincMappedTestIds.contains(test.getId());
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
        int from = (int) Math.min((long) (result.page - 1) * result.pageSize, filtered.size());
        int to = (int) Math.min((long) from + result.pageSize, filtered.size());
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
        /**
         * OGC-189 (M2): whether the lab unit is active. This is a <em>chooser</em>, so
         * the client offers only active units as new choices — but the full list is
         * still returned so a test already assigned to a deactivated unit can keep
         * showing its current value instead of rendering blank and silently writing
         * that blank back on save (the OGC-1191 data-loss class).
         */
        public boolean isActive;
    }

    /**
     * The lab units to choose from. With {@code includeInactive} the deactivated
     * ones come too, marked inactive, so the test editor can keep showing a test's
     * deactivated unit instead of a blank picker (OGC-189 M2, OGC-1459); every
     * other chooser keeps the active-only list.
     */
    @GetMapping(value = "/lab-units", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LabUnitOption> listLabUnits(@RequestParam(defaultValue = "false") boolean includeInactive) {
        List<LabUnitOption> options = new ArrayList<>();
        if (testSectionService == null) {
            return options;
        }
        List<TestSection> sections = new ArrayList<>(testSectionService.getAllActiveTestSections());
        if (includeInactive) {
            for (TestSection section : testSectionService.getAllInActiveTestSections()) {
                if (!TestSectionService.USER_SENTINEL_SECTION_NAME.equals(section.getTestSectionName())) {
                    sections.add(section);
                }
            }
        }
        for (TestSection section : sections) {
            LabUnitOption option = new LabUnitOption();
            option.id = section.getId();
            option.name = section.getLocalizedName();
            option.isActive = "Y".equals(section.getIsActive());
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
        // Set only on a 409 body: "description" when the derived description (the
        // name, unless an explicit description was sent) is already another
        // test's. The code-in-use 409 stays bodyless, as released clients expect.
        public String conflict;
        /** Set only on a 422 body: the field whose value was refused. */
        public String invalidField;
    }

    @PostMapping(value = "/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedTest> createTest(@RequestBody CreateTestRequest body, HttpServletRequest request) {
        if (testCatalogCreationService == null) {
            return ResponseEntity.status(503).build();
        }
        if (body != null) {
            body.name = trimToNull(body.name);
            body.reportingName = trimToNull(body.reportingName);
            body.code = trimToNull(body.code);
            body.description = trimToNull(body.description);
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
            TypeOfSample type = TestCatalogNumericIdGuard.isNumericId(sampleTypeId)
                    ? typeOfSampleService.getTypeOfSampleById(sampleTypeId.trim())
                    : null;
            if (type == null || !sampleTypeDomainCompatible(body.domain, type)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        }
        if (!isBlank(body.labUnitId)
                && (!TestCatalogNumericIdGuard.isNumericId(body.labUnitId) || testSectionService == null
                        || testSectionService.getTestSectionById(body.labUnitId.trim()) == null)) {
            return ResponseEntity.unprocessableEntity().build();
        }
        if (labUnitIsInactive(body.labUnitId)) {
            CreatedTest refused = new CreatedTest();
            refused.invalidField = "labUnitId";
            return ResponseEntity.unprocessableEntity().body(refused);
        }
        if (!isBlank(body.copyFromId) && (!TestCatalogNumericIdGuard.isNumericId(body.copyFromId)
                || testService.getTestById(body.copyFromId.trim()) == null)) {
            return ResponseEntity.unprocessableEntity().build();
        }
        // Code uniqueness (FR-4) → 409 so the UI can flag the field.
        if (testCatalogCreationService.codeInUse(body.code)) {
            return ResponseEntity.status(409).build();
        }
        // TEST.description is unique (test_desc_uk) and the create derives it from
        // the name when no description is sent, so a duplicate name used to reach
        // the constraint and come back as a bare 500 (OGC-1180).
        String effectiveDescription = isBlank(body.description) ? body.name : body.description;
        if (descriptionInUse(effectiveDescription, null)) {
            CreatedTest conflictBody = new CreatedTest();
            conflictBody.conflict = "description";
            return ResponseEntity.status(409).body(conflictBody);
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
        String newId;
        try {
            newId = testCatalogCreationService.createInactiveTest(params, sysUserId);
        } catch (TestCatalogCreationService.CodeInUseException e) {
            return ResponseEntity.status(409).build();
        }
        // Copy the source's result components (incl. options/interpretations).
        // The variant-group link that used to accompany this is retired
        // (OGC-1145 Phase 3): the m:n sample-type model replaced variants.
        if (!isBlank(body.copyFromId)) {
            componentService.copyComponentsFromTest(body.copyFromId.trim(), newId, sysUserId);
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

    /**
     * A new test may not go into a deactivated unit: creating it there used to
     * switch the unit back on (OGC-1459), which only reactivating the unit may do.
     */
    private boolean labUnitIsInactive(String labUnitId) {
        if (isBlank(labUnitId) || testSectionService == null || !TestCatalogNumericIdGuard.isNumericId(labUnitId)) {
            return false;
        }
        TestSection unit = testSectionService.getTestSectionById(labUnitId.trim());
        return unit != null && "N".equals(unit.getIsActive());
    }

    @GetMapping(value = "/tests/{testId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EditorEnvelope> getEditorEnvelope(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        EditorEnvelope envelope = new EditorEnvelope();
        envelope.testId = test.getId();
        // Every specimen named in full. The list view abbreviates to "(first +n)"
        // for readability; on the editor the whole configuration should be visible.
        envelope.name = TestServiceImpl.getLocalizedTestNameWithAllTypes(test);
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

    /**
     * The same evaluation activation re-surfaces (OGC-1119 FR-18), so the two
     * surfaces can never disagree; see {@link LoincIntegrityService}.
     */
    @GetMapping(value = "/tests/{testId}/loinc-integrity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoincIntegrityService.LoincIntegrity> getLoincIntegrity(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(SpringContext.getBean(LoincIntegrityService.class).check(test));
    }

    private static final List<String> DOMAINS = java.util.Arrays.stream(Domain.values()).map(Enum::name)
            .collect(java.util.stream.Collectors.toList());

    // D-030 (OGC-1145 FR-3): a test's domain (CLINICAL/ENVIRONMENTAL/VECTOR) vs
    // the sample type's domain. The single source of truth for interpreting a
    // sample-type domain — legacy one-character code or migrated enum value — is
    // Domain.normalize; both this guard and the value emitted to
    // the client run through it. Sample types with no (or unknown) domain stay
    // offerable everywhere so legacy data never blocks the editor.
    private static boolean sampleTypeDomainCompatible(String testDomain, TypeOfSample type) {
        if (type == null) {
            return false;
        }
        Domain typeDomain = Domain.fromRaw(type.getDomain());
        // Blank test domain, or a sample type with no/unknown domain, stays
        // offerable everywhere so legacy data never blocks the editor.
        if (isBlank(testDomain) || typeDomain == null) {
            return true;
        }
        return typeDomain.name().equals(testDomain);
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
        public Boolean opensMicrobiologyCase;
        public String microbiologyCaseRole;
        public Boolean collectedInSets;

        public Boolean active;
        public Boolean orderable;
        // Minutes a sample stays valid after collection; blank clears it.
        public String timeHolding;
        // Kept off the patient report.
        public Boolean inLabOnly;
        public Boolean notifyResults;
        // Environmental QC limits; all three blank removes the test's threshold row.
        public String qcBlankThreshold;
        public String qcRpdThreshold;
        public String qcRecoveryWindowPct;
        // Set only on a 422 body, naming the field whose value was refused.
        public String invalidField;
        // Set only on a 409 body, naming what conflicted ("description" or
        // "activation") — this endpoint answers 409 for two unrelated reasons and
        // the client needs to tell them apart (OGC-1180).
        public String conflict;
        // The test's version when the editor loaded it; a save against a newer one
        // is refused with conflict "stale" and the message below (OGC-1376).
        public String lastupdated;
        public String messageKey;
        public Map<String, Object> messageArgs;
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
        if (StaleSaveGuard.isStale(body.lastupdated, test.getLastupdated())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(staleConflict(test));
        }
        if (body.domain != null && !DOMAINS.contains(body.domain)) {
            return ResponseEntity.unprocessableEntity().build();
        }
        String role = body.microbiologyCaseRole == null ? test.getMicrobiologyCaseRole() : body.microbiologyCaseRole;
        boolean opensCase = body.opensMicrobiologyCase == null ? test.isOpensMicrobiologyCase()
                : body.opensMicrobiologyCase;
        boolean sets = body.collectedInSets == null ? test.isCollectedInSets() : body.collectedInSets;
        if (!List.of("CULTURE", "DIRECT", "CASE").contains(role) || (sets && (!opensCase || !"CULTURE".equals(role)))) {
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
        String invalidField = firstInvalidField(body);
        if (invalidField == null && labUnitRefused(test, body.labUnitId)) {
            invalidField = "labUnitId";
        }
        if (invalidField != null) {
            BasicInfo invalid = new BasicInfo();
            invalid.invalidField = invalidField;
            return ResponseEntity.unprocessableEntity().body(invalid);
        }
        // TEST.description is unique across tests (test_desc_uk). Without this
        // guard a duplicate reached the constraint and surfaced as a bare 500 with
        // an empty body — on exactly the create-then-edit flow the editor steers
        // users into (OGC-1180). Same-test no-op saves stay 200.
        if (body.description != null) {
            body.description = body.description.trim();
        }
        if (body.description != null && !body.description.equals(test.getDescription())
                && descriptionInUse(body.description, testId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(descriptionConflict());
        }
        if (changesCode(body.code, test.getLocalCode()) && testCatalogCreationService != null
                && testCatalogCreationService.codeInUse(body.code, testId)) {
            BasicInfo conflict = new BasicInfo();
            conflict.conflict = "code";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflict);
        }
        if (body.code != null && !body.code.isBlank()) {
            test.setLocalCode(body.code.trim());
        }
        if (body.description != null) {
            test.setDescription(body.description);
        }
        // Boxed flags: apply only what the caller actually sent, so a partial PUT
        // can't silently deactivate / clear AMR / un-orderable a test.
        if (body.domain != null) {
            test.setDomain(body.domain);
        }
        test.setOpensMicrobiologyCase(opensCase);
        test.setMicrobiologyCaseRole(role);
        test.setCollectedInSets(sets);
        if (body.antimicrobialResistance != null) {
            test.setAntimicrobialResistance(body.antimicrobialResistance);
        }
        if (body.orderable != null) {
            test.setOrderable(body.orderable);
        }
        if (body.timeHolding != null) {
            test.setTimeHolding(isBlank(body.timeHolding) ? null : body.timeHolding.trim());
        }
        if (body.inLabOnly != null) {
            test.setInLabOnly(body.inLabOnly);
        }
        if (body.notifyResults != null) {
            test.setNotifyResults(body.notifyResults);
        }
        // Lab unit (test section) is editable on modify too (not just create). A
        // save never changes the unit's status (OGC-1459): keeping a test in its
        // deactivated unit leaves the unit off, and moving it into an inactive one
        // was refused above.
        if (!isBlank(body.labUnitId) && testSectionService != null) {
            TestSection section = testSectionService.getTestSectionById(body.labUnitId.trim());
            if (section != null) {
                test.setTestSection(section);
            }
        }
        // Activation (N→Y) is gated on reference-range coverage (the H-03 safety
        // gate) and must go through POST .../activate; basic-info only persists a
        // deactivation, so it cannot be used to bypass the coverage acknowledgment.
        // Asking for it here is refused rather than answered 200 and dropped, which
        // told the caller the activation had been saved when it had not. Sending
        // active=true for an already-active test is not a change, so it still passes.
        if (Boolean.TRUE.equals(body.active) && !test.isActive()) {
            BasicInfo conflictBody = new BasicInfo();
            conflictBody.conflict = "activation";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictBody);
        }
        if (body.active != null && !body.active) {
            test.setIsActive("N");
        }
        test.setSysUserId(ControllerUtills.getSysUserId(request));
        Test updated;
        try {
            updated = testService.update(test);
        } catch (RuntimeException e) {
            // The in-use check above races with concurrent writes; when the unique
            // index still fires, answer the same 409 rather than a bare 500.
            if (isDescriptionConstraintViolation(e)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(descriptionConflict());
            }
            throw e;
        }
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
        if (body.qcBlankThreshold != null || body.qcRpdThreshold != null || body.qcRecoveryWindowPct != null) {
            testService.saveQcThreshold(testId, parseThreshold(body.qcBlankThreshold),
                    parseThreshold(body.qcRpdThreshold), parseThreshold(body.qcRecoveryWindowPct),
                    ControllerUtills.getSysUserId(request));
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
     * The first new Basic Info field whose value cannot be stored: the holding time
     * must be a whole, non-negative number of minutes, and each QC threshold a
     * non-negative number. Blank clears either.
     */
    private static String firstInvalidField(BasicInfo body) {
        if (!isBlank(body.timeHolding) && !body.timeHolding.trim().matches("\\d{1,9}")) {
            return "timeHolding";
        }
        if (!validThreshold(body.qcBlankThreshold)) {
            return "qcBlankThreshold";
        }
        if (!validThreshold(body.qcRpdThreshold)) {
            return "qcRpdThreshold";
        }
        if (!validThreshold(body.qcRecoveryWindowPct)) {
            return "qcRecoveryWindowPct";
        }
        return null;
    }

    /**
     * True when the save names a lab unit it cannot use: one that does not exist
     * (or is not an id), or a different, inactive one.
     */
    private boolean labUnitRefused(Test test, String labUnitId) {
        if (isBlank(labUnitId) || testSectionService == null) {
            return false;
        }
        if (!TestCatalogNumericIdGuard.isNumericId(labUnitId)) {
            return true;
        }
        TestSection target = testSectionService.getTestSectionById(labUnitId.trim());
        if (target == null) {
            return true;
        }
        TestSection current = test.getTestSection();
        boolean sameUnit = current != null && target.getId().equals(current.getId());
        return !sameUnit && "N".equals(target.getIsActive());
    }

    private static boolean validThreshold(String value) {
        if (isBlank(value)) {
            return true;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            return parsed.signum() >= 0 && parsed.precision() - parsed.scale() <= 10 && parsed.scale() <= 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static BigDecimal parseThreshold(String value) {
        return isBlank(value) ? null : new BigDecimal(value.trim());
    }

    private static String thresholdText(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * True when another test (any status) already holds this exact description.
     * TEST.description carries a case-sensitive unique index (test_desc_uk), so the
     * comparison is exact-match — mirroring what the database will enforce — and
     * {@code excludeTestId} lets a test keep its own description on save.
     */
    /**
     * Compared trimmed (OGC-1464) and case-sensitive, like the test_desc_uk index
     * it fronts (OGC-1180).
     */
    private boolean descriptionInUse(String description, String excludeTestId) {
        if (isBlank(description)) {
            return false;
        }
        String wanted = description.trim();
        for (Test other : testService.getAllTests(false)) {
            if (other.getDescription() != null && wanted.equals(other.getDescription().trim())
                    && !other.getId().equals(excludeTestId)) {
                return true;
            }
        }
        return false;
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * True when a Basic Info save changes the test's code. A legacy code another
     * test shares stays saveable as long as it is not changed (OGC-1464).
     */
    private static boolean changesCode(String requested, String stored) {
        if (isBlank(requested)) {
            return false;
        }
        return stored == null || !requested.trim().equalsIgnoreCase(stored.trim());
    }

    private static BasicInfo descriptionConflict() {
        BasicInfo body = new BasicInfo();
        body.conflict = "description";
        return body;
    }

    /** Walks the cause chain for the description unique index by name. */
    private static boolean isDescriptionConstraintViolation(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("test_desc_uk")) {
                return true;
            }
        }
        return false;
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
        info.opensMicrobiologyCase = test.isOpensMicrobiologyCase();
        info.microbiologyCaseRole = test.getMicrobiologyCaseRole();
        info.collectedInSets = test.isCollectedInSets();
        info.active = test.isActive();
        info.orderable = Boolean.TRUE.equals(test.getOrderable());
        info.timeHolding = test.getTimeHolding() == null ? "" : test.getTimeHolding();
        info.inLabOnly = test.isInLabOnly();
        info.notifyResults = Boolean.TRUE.equals(test.isNotifyResults());
        TestQcThreshold threshold = testService.getQcThreshold(test.getId()).orElse(null);
        info.qcBlankThreshold = thresholdText(threshold == null ? null : threshold.getBlankThreshold());
        info.qcRpdThreshold = thresholdText(threshold == null ? null : threshold.getRpdThreshold());
        info.qcRecoveryWindowPct = thresholdText(threshold == null ? null : threshold.getRecoveryWindowPct());
        info.lastupdated = StaleSaveGuard.token(test.getLastupdated());
        return info;
    }

    @SuppressWarnings("unchecked")
    private BasicInfo staleConflict(Test test) {
        Map<String, Object> described = staleSaveGuard.conflictBody("error.testCatalog.staleSave", "TEST", test.getId(),
                test.getLastupdated());
        BasicInfo conflict = new BasicInfo();
        conflict.testId = test.getId();
        conflict.conflict = "stale";
        conflict.messageKey = (String) described.get("messageKey");
        conflict.messageArgs = (Map<String, Object>) described.get("messageArgs");
        conflict.lastupdated = (String) described.get("lastupdated");
        return conflict;
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
        // Choosing this option opens a text box for a qualifying value at result entry.
        public Boolean qualifiable;
        // A value the admin typed rather than picked from the dictionary. It is
        // stored as a dictionary entry with that text, even when the text is a
        // number that happens to be another entry's id (OGC-1458).
        public Boolean freeText;
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
        // Detection limits of a quantitative component (OGC-1148), both optional.
        public java.math.BigDecimal lod;
        public java.math.BigDecimal loq;
        public List<InterpretationDto> interpretations = new ArrayList<>();
        public List<OptionDto> options = new ArrayList<>();
    }

    public static class SampleResults {
        public String testId;
        public List<ResultComponentDto> components = new ArrayList<>();
        /** Set only on a 422 body: the rule that refused the save (OGC-1458). */
        public String refusal;
        /** The component the refusal is about, when there is one. */
        public String refusedComponent;
        /**
         * The version of the components the editor loaded; a save against a newer one
         * is refused with conflict "stale" (OGC-1463).
         */
        public String version;
        /** Set only on a 409 body: "stale". */
        public String conflict;
        public String messageKey;
    }

    private static final String STALE_SECTION = "error.staleSave.section";

    private static SampleResults staleSampleResults(SampleResults current) {
        SampleResults body = new SampleResults();
        body.testId = current.testId;
        body.version = current.version;
        body.conflict = "stale";
        body.messageKey = STALE_SECTION;
        return body;
    }

    /**
     * True when the save names a component of this test that was removed: only a
     * page loaded before the removal still holds it (OGC-1463).
     */
    private boolean namesRemovedComponent(String testId, List<ResultComponentDto> components) {
        Set<String> removed = new HashSet<>();
        for (TestResultComponent component : componentService.getComponentsByTestId(testId)) {
            if (!"Y".equals(component.getIsActive())) {
                removed.add(component.getId());
            }
        }
        return components.stream().anyMatch(c -> !isBlank(c.id) && removed.contains(c.id));
    }

    /**
     * A component's label as a refusal names it, cut where it would flood the
     * message.
     */
    private static String shortName(String label) {
        return label == null || label.length() <= 60 ? label : label.substring(0, 60) + "…";
    }

    private static ResponseEntity<SampleResults> refusedSampleResults(String testId, String refusal, String component) {
        SampleResults body = new SampleResults();
        body.testId = testId;
        body.refusal = refusal;
        body.refusedComponent = component;
        return ResponseEntity.unprocessableEntity().body(body);
    }

    /**
     * True when a component holds text longer than its column: code 50, label 255,
     * default result 80; an option's value 80; an interpretation's match 80, text
     * 255, severity and color 20 (OGC-1467).
     */
    private static boolean componentTooLong(ResultComponentDto c) {
        if (longerThan(c.code, 50) || longerThan(c.label, 255) || longerThan(c.defaultResult, 80)
                || c.options.stream().anyMatch(o -> longerThan(o.value, 80))) {
            return true;
        }
        return c.interpretations.stream().anyMatch(i -> longerThan(i.valueMatch, 80) || longerThan(i.text, 255)
                || longerThan(i.severity, 20) || longerThan(i.color, 20));
    }

    private boolean knownUnit(String uomId) {
        if (!TestCatalogNumericIdGuard.isNumericId(uomId)) {
            return false;
        }
        return unitOfMeasureService == null || unitOfMeasureService.getUnitOfMeasureById(uomId.trim()) != null;
    }

    private static boolean knownResultType(String type) {
        if (isBlank(type)) {
            return false;
        }
        for (TypeOfTestResultServiceImpl.ResultType resultType : TypeOfTestResultServiceImpl.ResultType.values()) {
            if (resultType.getCharacterValue().equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when an option holds text the admin typed: flagged as such, or a new
     * option whose shown name is not the name of the dictionary entry its value
     * would resolve to. A bare dictionary id with no name stays a dictionary pick.
     */
    private boolean isFreeTextOption(OptionDto option) {
        if (Boolean.TRUE.equals(option.freeText)) {
            return true;
        }
        if (!isBlank(option.id) || isBlank(option.valueName) || isBlank(option.value)) {
            return false;
        }
        return !option.valueName.trim().equals(dictionaryName(option.value.trim()));
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
        if (body == null || body.components == null
                || body.components.stream()
                        .anyMatch(c -> c == null || c.options == null || c.interpretations == null
                                || c.options.stream().anyMatch(Objects::isNull)
                                || c.interpretations.stream().anyMatch(Objects::isNull))) {
            return ResponseEntity.badRequest().build();
        }
        SampleResults current = toSampleResults(testId);
        if (StaleSaveGuard.isStaleVersion(body.version, current.version)
                || namesRemovedComponent(testId, body.components)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(staleSampleResults(current));
        }
        if (body.components.isEmpty() && test.isActive()) {
            return refusedSampleResults(testId, "components.activeNeedsComponent", null);
        }
        // Each component needs a code + label, and codes must be unique within the
        // request (the DB enforces (test_id, code) too, but reject early + cleanly).
        Set<String> codes = new HashSet<>();
        for (ResultComponentDto c : body.components) {
            if (isBlank(c.code) || isBlank(c.label)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!knownResultType(c.resultType)) {
                return refusedSampleResults(testId, "resultType.unknown", c.label);
            }
            if (!codes.add(c.code)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (componentTooLong(c)) {
                return refusedSampleResults(testId, "value.tooLong", shortName(c.label));
            }
            if (!isBlank(c.uomId) && !knownUnit(c.uomId)) {
                return refusedSampleResults(testId, "unit.unknown", c.label);
            }
            // FR-C2 (OGC-1148): detection limits are non-negative and LOD <= LOQ.
            if ((c.lod != null && c.lod.signum() < 0) || (c.loq != null && c.loq.signum() < 0)
                    || (c.lod != null && c.loq != null && c.lod.compareTo(c.loq) > 0)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            // Result entry lists every option row, so a repeated value shows twice.
            Set<String> optionValues = new HashSet<>();
            for (OptionDto o : c.options) {
                if (!isBlank(o.value) && !optionValues.add(o.value.trim())) {
                    return ResponseEntity.unprocessableEntity().build();
                }
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
            e.setLod(c.lod);
            e.setLoq(c.loq);
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
                tr.setFreeText(isFreeTextOption(o));
                tr.setSortOrder(o.sortOrder != null ? String.valueOf(o.sortOrder) : null);
                tr.setIsNormal(Boolean.TRUE.equals(o.normal));
                tr.setIsQuantifiable(Boolean.TRUE.equals(o.qualifiable));
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
        // Free-text options are materialized into "Test Result" dictionary entries
        // during the save; refresh the cached list the legacy Test Add /
        // select-list pages read so the new entries show without a restart.
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.DICTIONARY_TEST_RESULTS);
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

    /** The primary component's id: the flagged one, else the first. */
    private static String primaryComponentId(List<TestResultComponent> components) {
        for (TestResultComponent component : components) {
            if (component.getIsPrimary()) {
                return component.getId();
            }
        }
        return components.isEmpty() ? null : components.get(0).getId();
    }

    private SampleResults toSampleResults(String testId) {
        SampleResults sr = new SampleResults();
        sr.testId = testId;
        List<TestResultComponent> activeComponents = componentService.getActiveComponentsByTestId(testId);
        String primaryId = primaryComponentId(activeComponents);
        Map<String, String> normalValueByComponent = new HashMap<>();
        for (ResultLimit limit : resultLimitService.getAllResultLimitsForTest(testId)) {
            String componentId = limit.getComponentId() != null ? limit.getComponentId() : primaryId;
            if (componentId != null && !isBlank(limit.getDictionaryNormalId())) {
                normalValueByComponent.putIfAbsent(componentId, limit.getDictionaryNormalId().trim());
            }
        }
        Test test = testService.getTestById(testId);
        TestResult legacyDefault = test == null ? null : test.getDefaultTestResult();
        for (TestResultComponent c : activeComponents) {
            ResultComponentDto dto = new ResultComponentDto();
            dto.id = c.getId();
            dto.code = c.getCode();
            dto.label = c.getLabel();
            dto.displayOrder = c.getDisplayOrder();
            dto.resultType = c.getResultType();
            dto.uomId = c.getUomId();
            dto.significantDigits = c.getSignificantDigits();
            dto.defaultResult = c.getDefaultResult();
            if (isBlank(dto.defaultResult) && c.getId().equals(primaryId) && legacyDefault != null
                    && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(c.getResultType())) {
                dto.defaultResult = legacyDefault.getValue();
            }
            dto.allowMultipleReadings = c.getAllowMultipleReadings();
            dto.isPrimary = c.getIsPrimary();
            dto.showOnReport = c.getShowOnReport();
            dto.lod = c.getLod();
            dto.loq = c.getLoq();
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
                odto.normal = Boolean.TRUE.equals(o.getIsNormal())
                        || (o.getValue() != null && o.getValue().trim().equals(normalValueByComponent.get(c.getId())));
                odto.qualifiable = Boolean.TRUE.equals(o.getIsQuantifiable());
                dto.options.add(odto);
            }
            sr.components.add(dto);
        }
        sr.version = StaleSaveGuard.versionOf(sr.components);
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
        // Read-only: the component's code, the same across a test's specimen
        // siblings, so a group edit can compare ranges across tests (OGC-1238).
        public String componentCode;
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
        // Whether the request carried the reporting bounds at all; a row that omits
        // them keeps the stored ones.
        @JsonIgnore
        public boolean reportingSent;

        public void setLowReporting(Double lowReporting) {
            this.lowReporting = lowReporting;
            reportingSent = true;
        }

        public void setHighReporting(Double highReporting) {
            this.highReporting = highReporting;
            reportingSent = true;
        }
    }

    public static class RangesResponse {
        public String testId;
        /** No default: a save that leaves the list out is refused (OGC-1467). */
        public List<RangeDto> ranges;
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
        if (body.ranges == null || body.ranges.stream().anyMatch(Objects::isNull)) {
            return ResponseEntity.badRequest().build();
        }
        // FR-19 (OGC-1119): a range constrains one of this test's own result
        // components, never a component of another test.
        Set<String> componentIds = new HashSet<>();
        for (TestResultComponent component : componentService.getActiveComponentsByTestId(testId)) {
            componentIds.add(component.getId());
        }
        for (RangeDto r : body.ranges) {
            if (r.gender != null && !r.gender.isBlank() && !RANGE_GENDERS.contains(r.gender)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            double min = r.minAge != null ? r.minAge : 0d;
            double max = r.maxAge != null ? r.maxAge : Double.POSITIVE_INFINITY;
            if (min < 0d || max <= min || reportingRangeInverted(r)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!isBlank(r.sampleTypeId) && !associatedTypeIds.contains(r.sampleTypeId)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!isBlank(r.componentId) && !componentIds.contains(r.componentId)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        }
        keepStoredReportingBounds(testId, body.ranges);
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
            limit.setLowReportingRange(unbox(r.lowReporting, Double.NEGATIVE_INFINITY));
            limit.setHighReportingRange(unbox(r.highReporting, Double.POSITIVE_INFINITY));
            desired.add(limit);
        }
        return desired;
    }

    private void keepStoredReportingBounds(String testId, List<RangeDto> ranges) {
        Map<String, ResultLimit> storedById = new HashMap<>();
        for (ResultLimit limit : resultLimitService.getNumericRangesForTest(testId)) {
            storedById.put(limit.getId(), limit);
        }
        for (RangeDto r : ranges) {
            ResultLimit stored = isBlank(r.id) ? null : storedById.get(r.id);
            if (!r.reportingSent && stored != null) {
                r.lowReporting = finiteOrNull(stored.getLowReportingRange());
                r.highReporting = finiteOrNull(stored.getHighReportingRange());
            }
        }
    }

    private static boolean reportingRangeInverted(RangeDto r) {
        return r.lowReporting != null && r.highReporting != null && r.lowReporting > r.highReporting;
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
    public ResponseEntity<List<GroupTestSummary>> groupSummary(@RequestParam String ids) {
        List<GroupTestSummary> out = new ArrayList<>();
        for (String rawId : ids.split(",")) {
            String id = rawId.trim();
            if (id.isEmpty()) {
                continue;
            }
            if (!TestCatalogNumericIdGuard.isNumericId(id)) {
                return ResponseEntity.badRequest().build();
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
        return ResponseEntity.ok(out);
    }

    public static class GroupRangesUpdate {
        public List<String> testIds = new ArrayList<>();
        /**
         * No default: a request that leaves the ranges out is refused (400) rather than
         * read as "clear every test's ranges"; an explicit empty list still clears
         * them.
         */
        public List<RangeDto> ranges;
    }

    @PutMapping(value = "/group/ranges", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> saveGroupRanges(@RequestBody GroupRangesUpdate body, HttpServletRequest request) {
        if (body == null || body.testIds == null || body.testIds.isEmpty()) {
            return ResponseEntity.unprocessableEntity().build();
        }
        if (body.ranges == null || body.ranges.stream().anyMatch(Objects::isNull)) {
            return ResponseEntity.badRequest().build();
        }
        List<Test> tests = new ArrayList<>();
        for (String testId : body.testIds) {
            if (!TestCatalogNumericIdGuard.isNumericId(testId)) {
                return ResponseEntity.badRequest().build();
            }
            Test test = testService.getTestById(testId.trim());
            if (test == null) {
                return ResponseEntity.notFound().build();
            }
            tests.add(test);
        }
        for (RangeDto r : body.ranges) {
            if (r.gender != null && !r.gender.isBlank() && !RANGE_GENDERS.contains(r.gender)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            double min = r.minAge != null ? r.minAge : 0d;
            double max = r.maxAge != null ? r.maxAge : Double.POSITIVE_INFINITY;
            if (min < 0d || max <= min || reportingRangeInverted(r)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!isBlank(r.componentId) && componentService.getMatch("id", r.componentId).isEmpty()) {
                return ResponseEntity.unprocessableEntity().build();
            }
        }
        Map<String, List<ResultLimit>> desiredByTestId = new LinkedHashMap<>();
        for (Test test : tests) {
            String testId = test.getId();
            // New rows per test: the ids in the shared set belong to no single test,
            // so drop them and let each test insert its own (FR-11 per-test write).
            // The component and specimen ids in the shared set belong to the test
            // the set was seeded from, so each target test gets its own equivalent
            // (FR-19): the component with the same code, else its primary, and a
            // specimen scope only when that specimen is one of its own (OGC-1118).
            List<TestResultComponent> targetComponents = componentService.getActiveComponentsByTestId(testId);
            Set<String> targetTypeIds = new HashSet<>();
            for (TypeOfSample type : testService.getTypeOfSamples(test)) {
                targetTypeIds.add(type.getId());
            }
            List<RangeDto> perTest = new ArrayList<>();
            for (RangeDto r : body.ranges) {
                RangeDto copy = new RangeDto();
                copy.componentId = equivalentComponentId(r.componentId, targetComponents);
                copy.sampleTypeId = targetTypeIds.contains(r.sampleTypeId) ? r.sampleTypeId : null;
                copy.gender = r.gender;
                copy.minAge = r.minAge;
                copy.maxAge = r.maxAge;
                copy.lowNormal = r.lowNormal;
                copy.highNormal = r.highNormal;
                copy.lowCritical = r.lowCritical;
                copy.highCritical = r.highCritical;
                copy.lowValid = r.lowValid;
                copy.highValid = r.highValid;
                copy.lowReporting = r.lowReporting;
                copy.highReporting = r.highReporting;
                perTest.add(copy);
            }
            desiredByTestId.put(testId, toResultLimits(perTest));
        }
        resultLimitService.saveRangesForTests(desiredByTestId, ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok().build();
    }

    /**
     * The target test's own component for a range seeded from another test's
     * component: the same id when it is already the target's, else the component
     * sharing the source's code, else the target's primary. Null when the range is
     * not component-scoped or the target has no components.
     */
    private String equivalentComponentId(String sourceComponentId, List<TestResultComponent> targetComponents) {
        if (isBlank(sourceComponentId) || targetComponents.isEmpty()) {
            return null;
        }
        for (TestResultComponent component : targetComponents) {
            if (sourceComponentId.equals(component.getId())) {
                return component.getId();
            }
        }
        TestResultComponent source = componentService.getMatch("id", sourceComponentId).orElse(null);
        if (source != null && !isBlank(source.getCode())) {
            for (TestResultComponent component : targetComponents) {
                if (source.getCode().equalsIgnoreCase(component.getCode())) {
                    return component.getId();
                }
            }
        }
        for (TestResultComponent component : targetComponents) {
            if (component.getIsPrimary()) {
                return component.getId();
            }
        }
        return targetComponents.get(0).getId();
    }

    private RangesResponse toRanges(String testId) {
        RangesResponse resp = new RangesResponse();
        resp.testId = testId;
        resp.ranges = new ArrayList<>();
        List<ResultLimit> limits = resultLimitService.getAllResultLimitsForTest(testId);
        List<TestResultComponent> comps = componentService.getActiveComponentsByTestId(testId);
        Map<String, String> codeById = new HashMap<>();
        for (TestResultComponent c : comps) {
            codeById.put(c.getId(), c.getCode());
        }
        for (ResultLimit l : resultLimitService.getNumericRangesForTest(testId)) {
            RangeDto d = new RangeDto();
            d.id = l.getId();
            d.componentId = l.getComponentId();
            d.componentCode = l.getComponentId() == null ? null : codeById.get(l.getComponentId());
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
            option.domain = Domain.normalize(type.getDomain());
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
        /** Set only on a 422 body: the field whose value was refused (OGC-1465). */
        public String invalidField;
    }

    /** The condition codes the Storage form offers (FR-86). */
    static final Set<String> STORAGE_CONDITIONS = Set.of("AMBIENT", "CONTROLLED_ROOM_TEMPERATURE", "COOL_ROOM",
            "COLD_ROOM", "REFRIGERATED", "FROZEN", "DEEP_FROZEN", "ULTRA_LOW_FREEZER", "WARM_INCUBATOR");

    /**
     * The field a storage save would store wrongly: a condition code the form does
     * not offer, text longer than its column, or a negative duration (OGC-1465). A
     * condition the test already holds (an imported code) stays saveable.
     */
    static String invalidStorageField(StorageDto body, String storedCondition) {
        if (!isBlank(body.storageCondition) && !STORAGE_CONDITIONS.contains(body.storageCondition)
                && !body.storageCondition.equals(storedCondition)) {
            return "storageCondition";
        }
        if (longerThan(body.storageConditionCustom, 200)) {
            return "storageConditionCustom";
        }
        if (body.storageDuration != null && body.storageDuration < 0) {
            return "storageDuration";
        }
        if (longerThan(body.storageDurationUnit, 20)) {
            return "storageDurationUnit";
        }
        if (longerThan(body.disposalMethod, 100)) {
            return "disposalMethod";
        }
        if (body.disposalTimeframe != null && body.disposalTimeframe < 0) {
            return "disposalTimeframe";
        }
        if (longerThan(body.disposalUnit, 20)) {
            return "disposalUnit";
        }
        return null;
    }

    /** Measures the value as it is stored: the save does not trim it. */
    private static boolean longerThan(String value, int max) {
        return value != null && value.length() > max;
    }

    private static ResponseEntity<StorageDto> invalidStorage(String field) {
        StorageDto refused = new StorageDto();
        refused.invalidField = field;
        return ResponseEntity.unprocessableEntity().body(refused);
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
        TestSampleHandling stored = handlingService.getByTestId(testId);
        String invalidField = body == null ? "storage"
                : invalidStorageField(body, stored == null ? null : stored.getStorageCondition());
        if (invalidField != null) {
            return invalidStorage(invalidField);
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
        // The StorageDto fields the admin changed. Only these are written; every
        // test keeps its own value for the rest. Null writes the whole form.
        public List<String> fields;
    }

    private static final Map<String, java.util.function.BiConsumer<StorageDto, StorageDto>> STORAGE_FIELDS = Map
            .ofEntries(Map.entry("storageCondition", (t, s) -> t.storageCondition = s.storageCondition),
                    Map.entry("storageConditionCustom", (t, s) -> t.storageConditionCustom = s.storageConditionCustom),
                    Map.entry("storageDuration", (t, s) -> t.storageDuration = s.storageDuration),
                    Map.entry("storageDurationUnit", (t, s) -> t.storageDurationUnit = s.storageDurationUnit),
                    Map.entry("stabilityNotes", (t, s) -> t.stabilityNotes = s.stabilityNotes),
                    Map.entry("protectFromLight", (t, s) -> t.protectFromLight = s.protectFromLight),
                    Map.entry("doNotFreeze", (t, s) -> t.doNotFreeze = s.doNotFreeze),
                    Map.entry("doNotRefrigerate", (t, s) -> t.doNotRefrigerate = s.doNotRefrigerate),
                    Map.entry("disposalMethod", (t, s) -> t.disposalMethod = s.disposalMethod),
                    Map.entry("disposalTimeframe", (t, s) -> t.disposalTimeframe = s.disposalTimeframe),
                    Map.entry("disposalUnit", (t, s) -> t.disposalUnit = s.disposalUnit),
                    Map.entry("specialInstructions", (t, s) -> t.specialInstructions = s.specialInstructions),
                    Map.entry("overrideRestricted", (t, s) -> t.overrideRestricted = s.overrideRestricted));

    @PutMapping(value = "/group/storage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> saveGroupStorage(@RequestBody GroupStorageUpdate body, HttpServletRequest request) {
        if (body == null || body.testIds == null || body.testIds.isEmpty() || body.storage == null) {
            return ResponseEntity.unprocessableEntity().build();
        }
        if (body.fields != null && !STORAGE_FIELDS.keySet().containsAll(body.fields)) {
            return ResponseEntity.unprocessableEntity().build();
        }
        if (invalidStorageField(body.storage, null) != null) {
            return ResponseEntity.unprocessableEntity().build();
        }
        for (String testId : body.testIds) {
            if (!TestCatalogNumericIdGuard.isNumericId(testId)) {
                return ResponseEntity.badRequest().build();
            }
            if (testService.getTestById(testId.trim()) == null) {
                return ResponseEntity.notFound().build();
            }
        }
        if (body.fields != null && body.fields.isEmpty()) {
            return ResponseEntity.ok().build();
        }
        Map<String, TestSampleHandling> desiredByTestId = new LinkedHashMap<>();
        for (String rawId : body.testIds) {
            String testId = rawId.trim();
            StorageDto desired = body.storage;
            if (body.fields != null) {
                desired = toStorage(testId, handlingService.getByTestId(testId));
                for (String field : body.fields) {
                    STORAGE_FIELDS.get(field).accept(desired, body.storage);
                }
            }
            desiredByTestId.put(testId, toHandling(desired));
        }
        handlingService.saveForTests(desiredByTestId, ControllerUtills.getSysUserId(request));
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
        AnalyzersResponse resp = new AnalyzersResponse();
        resp.testId = testId;
        for (AnalyzerTestCapability capability : analyzerService.getCapabilitiesForTest(testId)) {
            AnalyzerRow row = new AnalyzerRow();
            row.analyzerId = capability.analyzerId();
            row.analyzerName = capability.analyzerName();
            row.analyzerTestName = capability.analyzerTestCode();
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
            o.name = t.getLocalizedName();
            o.domain = Domain.normalize(t.getDomain());
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
        if (body.items == null || body.items.stream().anyMatch(Objects::isNull)) {
            return ResponseEntity.badRequest().build();
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
        /** No default: a save that leaves the list out is refused (OGC-1467). */
        public List<MappingDto> mappings;
        // The test's active components, so the editor can offer an "Applies to"
        // scope per mapping row without a second request.
        public List<TerminologyComponentDto> components = new ArrayList<>();
        // OGC-1145 FR-13: the test's associated sample types, so the editor can
        // offer the per-specimen override picker without a second request.
        public List<SampleTypeOption> sampleTypes = new ArrayList<>();
        /**
         * The version of the mappings the editor loaded; a save against a newer one is
         * refused with conflict "stale" (OGC-1463).
         */
        public String version;
        /** Set only on a 409 body: "stale". */
        public String conflict;
        public String messageKey;
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
        if (body == null || body.mappings == null || body.mappings.stream().anyMatch(Objects::isNull)) {
            return ResponseEntity.badRequest().build();
        }
        TerminologyResponse current = toTerminology(testId);
        if (StaleSaveGuard.isStaleVersion(body.version, current.version)) {
            TerminologyResponse stale = new TerminologyResponse();
            stale.testId = testId;
            stale.version = current.version;
            stale.conflict = "stale";
            stale.messageKey = STALE_SECTION;
            return ResponseEntity.status(HttpStatus.CONFLICT).body(stale);
        }
        Set<String> seen = new HashSet<>();
        List<TestTerminologyMapping> desired = new ArrayList<>();
        for (MappingDto m : body.mappings) {
            // Source must be a known terminology; code required; relationship (if
            // present) must be a known qualifier.
            if (longerThan(m.code, 80) || longerThan(m.displayName, 255)) {
                TerminologyResponse refused = new TerminologyResponse();
                refused.testId = testId;
                refused.messageKey = "error.testCatalog.terminology.tooLong";
                return ResponseEntity.unprocessableEntity().body(refused);
            }
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
                    + " " + m.source + " " + m.code.trim())) {
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
        resp.mappings = new ArrayList<>();
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
            option.domain = Domain.normalize(type.getDomain());
            resp.sampleTypes.add(option);
        }
        resp.version = StaleSaveGuard.versionOf(resp.mappings);
        return resp;
    }

    // ── Panels — this test's panel memberships (OGC-980..982) ─────────────────

    /**
     * A selectable panel for the add-to-panel typeahead — and, with the management
     * fields (OGC-224), one row of the Panels list. Additive: the typeahead keeps
     * reading {id, name} only.
     */
    public static class PanelOption {
        public String id;
        public String name;
        public String description;
        public String loinc;
        /** CLINICAL / ENVIRONMENTAL / VECTOR — never null (defaults CLINICAL). */
        public String domain;
        public boolean active;
        public Integer sortOrder;
        public int testCount;
        /** Derived from the member tests — panels store no sample types. */
        public List<String> sampleTypes = new ArrayList<>();
        /**
         * The derived sample types whose own domain is not this panel's, so the list
         * can say so on the row. Empty when the panel is consistent.
         */
        public List<String> sampleTypesOutsideDomain = new ArrayList<>();
        /**
         * Set only on a 422 body: the rule that refused a Basic Info save —
         * {@code name.required}, {@code name.tooLong}, {@code description.tooLong},
         * {@code domain.unknown}, {@code domain.conflict} or
         * {@code activation.needsTest}.
         */
        public String refusal;
        /** Set only on a 422 body: why the domain guard refused this write. */
        public DomainConflict domainConflict;
    }

    /** A member test the domain guard refused, with the domain it actually has. */
    public static class ConflictingTest {
        public String testId;
        public String name;
        public String domain;
    }

    /**
     * OGC-1232 — the body of a domain-guard 422. A panel never mixes domains, so a
     * write that would leave member tests outside the panel's domain is refused;
     * this names the domain in force and every test that stands in the way, so the
     * operator knows which tests to re-domain or remove before trying again.
     */
    public static class DomainConflict {
        /** The domain the panel has, or was asked to take. */
        public String domain;
        /** Set on the test-side membership write: the panel that refused the test. */
        public String panelId;
        public String panelName;
        /** The tests whose own domain is not {@link #domain}. */
        public List<ConflictingTest> tests = new ArrayList<>();
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
        /** Set only on a 422 body: why the domain guard refused this write. */
        public DomainConflict domainConflict;
        /** Set only on a 422 body: the rule that refused the write (OGC-1456). */
        public String refusal;
        /** With {@code panel.activeNeedsTest}: the active panels left empty. */
        public List<PanelMembership> refusedPanels = new ArrayList<>();
    }

    /** A test within a panel — the read-only preview for the position editor. */
    public static class PanelTestRow {
        public String testId;
        public String testName;
        public String code;
        public Integer position;
    }

    public static class PanelTestOrderResponse {
        public String panelId;
        public List<PanelTestRow> tests = new ArrayList<>();
        /**
         * The version of the member list the editor loaded; a member save sent with a
         * newer one is refused as stale (OGC-1463).
         */
        public String version;
    }

    public static class MembershipItem {
        public String panelId;
        public Integer position;
    }

    public static class PanelMembershipUpdate {
        /**
         * No default: an omitted list is refused, never read as "leave every panel".
         */
        public List<MembershipItem> memberships;
    }

    @GetMapping(value = "/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PanelOption> listPanels(@RequestParam(defaultValue = "false") boolean includeInactive) {
        List<Panel> panels = includeInactive ? panelService.getAllPanels() : panelService.getAllActivePanels();
        List<PanelOption> options = new ArrayList<>();
        for (Panel p : panels) {
            options.add(toPanelOption(p));
        }
        return options;
    }

    private PanelOption toPanelOption(Panel p) {
        PanelOption o = new PanelOption();
        o.id = p.getId();
        o.name = p.getPanelName();
        o.description = p.getDescription();
        o.loinc = p.getLoinc();
        o.domain = Domain.normalize(p.getDomain());
        o.active = "Y".equals(p.getIsActive());
        o.sortOrder = p.getSortOrderInt();
        // Sample types are DERIVED from the member tests (FRS v2.2) — the panel
        // stores none; SAMPLETYPE_PANEL is a backend-synced junction, never the
        // display source.
        Set<String> derivedTypes = new LinkedHashSet<>();
        Set<String> outsideDomain = new LinkedHashSet<>();
        List<PanelItem> items = panelItemService.getPanelItemsForPanel(p.getId());
        o.testCount = items.size();
        for (PanelItem item : items) {
            if (item.getTest() == null) {
                continue;
            }
            // getTypeOfSampleForTest returns null (not empty) for a test with
            // no specimen association — the fresh member test case.
            List<TypeOfSample> types = typeOfSampleService.getTypeOfSampleForTest(item.getTest().getId());
            if (types == null) {
                continue;
            }
            for (TypeOfSample type : types) {
                derivedTypes.add(type.getLocalizedName());
                if (!sampleTypeDomainCompatible(o.domain, type)) {
                    outsideDomain.add(type.getLocalizedName());
                }
            }
        }
        o.sampleTypes.addAll(derivedTypes);
        o.sampleTypesOutsideDomain.addAll(outsideDomain);
        return o;
    }

    /**
     * Inline panel create (OGC-1112 FR-43). `active` is optional: absent keeps the
     * historic Y (the test-side inline create adds its test immediately —
     * OGC-1140's Active-on-create); the panel editor passes false so a blank panel
     * honors the "never active with zero tests" rule (OGC-224).
     */
    public static class CreatePanelRequest {
        public String name;
        public Boolean active;
        /**
         * OGC-1140 — a panel created inline from a test inherits that test's domain.
         * Absent defaults to CLINICAL (the launch scope).
         */
        public String domain;
    }

    /** OGC-224 — Basic Info save; null fields are left unchanged. */
    public static class PanelBasicInfoRequest {
        public String name;
        public String description;
        public String domain;
        public Boolean active;
    }

    /** getPanelById throws on unknown AND non-numeric ids — both are a 404. */
    private Panel findPanel(String panelId) {
        try {
            return panelService.getPanelById(panelId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void refreshPanelDisplayLists() {
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.PANELS_INACTIVE);
    }

    // ── Panel terminology (OGC-224 C4) — parity with the sample-type mapper,
    // including WHONET; the LOINC mapping is the panel's primary identifier ──

    private static final Set<String> PANEL_TERM_SOURCES = Set.of("LOINC", "SNOMED", "CIEL", "OCL", "WHONET");

    public static class PanelTerminologyMappingDto {
        public String id;
        public String source;
        public String code;
        public String relationship;

        // no entity-arg constructor: a second public constructor reads as an
        // implicit Jackson creator under the strict mapper and breaks request
        // binding (400)
        static PanelTerminologyMappingDto of(
                org.openelisglobal.panelterminology.valueholder.PanelTerminologyMapping m) {
            PanelTerminologyMappingDto dto = new PanelTerminologyMappingDto();
            dto.id = m.getId();
            dto.source = m.getSource();
            dto.code = m.getCode();
            dto.relationship = m.getRelationship();
            return dto;
        }
    }

    public static class PanelTerminologyResponse {
        public String panelId;
        /**
         * No default: on a save an omitted list is refused, never read as "clear every
         * mapping" (OGC-1456).
         */
        public List<PanelTerminologyMappingDto> mappings;
        /** Set only on a 422 body: the rule that refused the write (OGC-1456). */
        public String refusal;
        /** The code the refusal is about. */
        public String refusedCode;
    }

    /** panel_terminology_mapping.code is VARCHAR(80). */
    private static final int PANEL_TERM_CODE_MAX_LENGTH = 80;

    /** A LOINC code is at most 10 characters (panel.loinc routes e-orders). */
    private static final int LOINC_MAX_LENGTH = 10;

    private static ResponseEntity<PanelTerminologyResponse> refusedTerminology(String panelId, String refusal,
            String code) {
        PanelTerminologyResponse body = new PanelTerminologyResponse();
        body.panelId = panelId;
        body.refusal = refusal;
        body.refusedCode = code;
        return ResponseEntity.unprocessableEntity().body(body);
    }

    @GetMapping(value = "/panels/{panelId}/terminology", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PanelTerminologyResponse> getPanelTerminology(@PathVariable String panelId) {
        if (findPanel(panelId) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toPanelTerminology(panelId));
    }

    @PutMapping(value = "/panels/{panelId}/terminology", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PanelTerminologyResponse> savePanelTerminology(@PathVariable String panelId,
            @RequestBody PanelTerminologyResponse body, HttpServletRequest request) {
        if (findPanel(panelId) == null) {
            return ResponseEntity.notFound().build();
        }
        if (body == null || body.mappings == null || body.mappings.stream().anyMatch(Objects::isNull)) {
            return ResponseEntity.badRequest().build();
        }
        // (source, code) unique within the request — the DB enforces it per
        // panel, but reject early + cleanly rather than surfacing a raw 500.
        Set<String> seen = new HashSet<>();
        List<org.openelisglobal.panelterminology.valueholder.PanelTerminologyMapping> desired = new ArrayList<>();
        for (PanelTerminologyMappingDto m : body.mappings) {
            if (isBlank(m.source) || !PANEL_TERM_SOURCES.contains(m.source) || isBlank(m.code)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!isBlank(m.relationship) && !TERM_RELATIONSHIPS.contains(m.relationship)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!seen.add(m.source + " " + m.code.trim())) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (m.code.trim().length() > PANEL_TERM_CODE_MAX_LENGTH) {
                return refusedTerminology(panelId, "code.tooLong", m.code.trim());
            }
            if ("LOINC".equals(m.source) && m.code.trim().length() > LOINC_MAX_LENGTH) {
                return refusedTerminology(panelId, "loinc.tooLong", m.code.trim());
            }
            org.openelisglobal.panelterminology.valueholder.PanelTerminologyMapping e = new org.openelisglobal.panelterminology.valueholder.PanelTerminologyMapping();
            e.setSource(m.source);
            e.setCode(m.code.trim());
            e.setRelationship(isBlank(m.relationship) ? null : m.relationship);
            desired.add(e);
        }
        panelTerminologyService.saveMappingsForPanel(panelId, desired, ControllerUtills.getSysUserId(request));
        // panel.loinc may have changed (denormalized primary) — order entry lists
        // show it
        refreshPanelDisplayLists();
        return ResponseEntity.ok(toPanelTerminology(panelId));
    }

    private PanelTerminologyResponse toPanelTerminology(String panelId) {
        PanelTerminologyResponse resp = new PanelTerminologyResponse();
        resp.mappings = new ArrayList<>();
        resp.panelId = panelId;
        for (org.openelisglobal.panelterminology.valueholder.PanelTerminologyMapping m : panelTerminologyService
                .getActiveByPanelId(panelId)) {
            resp.mappings.add(PanelTerminologyMappingDto.of(m));
        }
        return resp;
    }

    /** An answer's codes come from the standard systems only. */
    private static final Set<String> ANSWER_TERM_SOURCES = Set.of("LOINC", "SNOMED", "CIEL", "OCL");

    public static class AnswerTerminologyMappingDto {
        public String id;
        public String source;
        public String code;
        public String relationship;
        public String displayName;

        // no entity-arg constructor: a second public constructor reads as an
        // implicit Jackson creator under the strict mapper and breaks request
        // binding (400)
        static AnswerTerminologyMappingDto of(
                org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping m) {
            AnswerTerminologyMappingDto dto = new AnswerTerminologyMappingDto();
            dto.id = m.getId();
            dto.source = m.getSource();
            dto.code = m.getCode();
            dto.relationship = m.getRelationship();
            dto.displayName = m.getDisplayName();
            return dto;
        }
    }

    public static class AnswerTerminologyResponse {
        public String dictionaryId;
        public List<AnswerTerminologyMappingDto> mappings = new ArrayList<>();
    }

    @GetMapping(value = "/answers/{dictionaryId}/terminology", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnswerTerminologyResponse> getAnswerTerminology(@PathVariable String dictionaryId) {
        if (findAnswer(dictionaryId) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toAnswerTerminology(dictionaryId));
    }

    @PutMapping(value = "/answers/{dictionaryId}/terminology", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnswerTerminologyResponse> saveAnswerTerminology(@PathVariable String dictionaryId,
            @RequestBody AnswerTerminologyResponse body, HttpServletRequest request) {
        if (findAnswer(dictionaryId) == null) {
            return ResponseEntity.notFound().build();
        }
        Set<String> seen = new HashSet<>();
        List<org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping> desired = new ArrayList<>();
        for (AnswerTerminologyMappingDto m : body.mappings) {
            if (isBlank(m.source) || !ANSWER_TERM_SOURCES.contains(m.source) || isBlank(m.code)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!isBlank(m.relationship) && !TERM_RELATIONSHIPS.contains(m.relationship)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            if (!seen.add(m.source + " " + m.code.trim())) {
                return ResponseEntity.unprocessableEntity().build();
            }
            org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping e = new org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping();
            e.setSource(m.source);
            e.setCode(m.code.trim());
            e.setRelationship(isBlank(m.relationship) ? null : m.relationship);
            e.setDisplayName(isBlank(m.displayName) ? null : m.displayName.trim());
            desired.add(e);
        }
        answerTerminologyService.saveMappingsForDictionary(dictionaryId, desired,
                ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok(toAnswerTerminology(dictionaryId));
    }

    private Dictionary findAnswer(String dictionaryId) {
        if (dictionaryService == null || answerTerminologyService == null || dictionaryId == null
                || !dictionaryId.matches("\\d+")) {
            return null;
        }
        return dictionaryService.getDictionaryById(dictionaryId);
    }

    private AnswerTerminologyResponse toAnswerTerminology(String dictionaryId) {
        AnswerTerminologyResponse resp = new AnswerTerminologyResponse();
        resp.dictionaryId = dictionaryId;
        for (org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping m : answerTerminologyService
                .getActiveByDictionaryId(dictionaryId)) {
            resp.mappings.add(AnswerTerminologyMappingDto.of(m));
        }
        return resp;
    }

    /**
     * The localization records behind a panel's name, for the editor's Localization
     * section.
     *
     * <p>
     * Same bridge as a test's: a panel already FK-links to a row in the generic
     * localization tables, so this hands back the backing id and the UI reads and
     * writes per-locale values through /rest/localizations/{id}. A panel carries
     * one localized field — its name — where a test carries two.
     */
    @GetMapping(value = "/panels/{panelId}/localization", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LocalizationRefs> getPanelLocalizationRefs(@PathVariable String panelId) {
        Panel panel = findPanel(panelId);
        if (panel == null) {
            return ResponseEntity.notFound().build();
        }
        LocalizationRefs refs = new LocalizationRefs();
        refs.testId = panelId;
        Localization localization = panel.getLocalization();
        if (localization != null && localization.getId() != null) {
            refs.fields.add(new LocalizationFieldRef("name", localization.getId()));
        }
        return ResponseEntity.ok(refs);
    }

    @GetMapping(value = "/panels/{panelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PanelOption> getPanel(@PathVariable String panelId) {
        Panel panel = findPanel(panelId);
        if (panel == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toPanelOption(panel));
    }

    private static final int PANEL_NAME_MAX_LENGTH = 20;

    private static final int PANEL_DESCRIPTION_MAX_LENGTH = 60;

    /**
     * OGC-224 — Basic Info save with the FRS rules: name required (also updates the
     * display localization so order entry follows the rename); domain must be a
     * real Domain and cannot change while member tests of another domain exist;
     * activation requires ≥1 member test ("never active with zero tests"); editing
     * never auto-flips the active state — only this explicit toggle.
     * <p>
     * OGC-1232 — every rule is checked before anything is written, so a refused
     * save leaves the panel exactly as it was (a rename used to reach the display
     * localization before the domain guard refused), and every refusal says why:
     * {@link PanelOption#refusal} names the rule and
     * {@link PanelOption#domainConflict} the member tests in the way. A name the
     * panel already carries is never a reason to refuse, so a panel created
     * elsewhere with a longer name stays editable here; the localization is only
     * rewritten on an actual rename.
     * <p>
     * OGC-1234: a rename onto another panel's name is refused as
     * {@code name.duplicate} instead of failing with a 500; a description is free
     * text and may repeat another panel's.
     */
    @PutMapping(value = "/panels/{panelId}/basic-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PanelOption> savePanelBasicInfo(@PathVariable String panelId,
            @RequestBody PanelBasicInfoRequest body, HttpServletRequest request) {
        Panel panel = findPanel(panelId);
        if (panel == null) {
            return ResponseEntity.notFound().build();
        }
        String sysUserId = ControllerUtills.getSysUserId(request);

        String name = body.name == null ? null : body.name.trim();
        boolean renamed = name != null && !name.equals(panel.getPanelName());
        if (name != null && name.isEmpty()) {
            return refused(panel, "name.required");
        }
        if (renamed && name.length() > PANEL_NAME_MAX_LENGTH) {
            return refused(panel, "name.tooLong");
        }
        if (renamed && panelNameTakenByAnother(name, panel.getId())) {
            return refused(panel, "name.duplicate");
        }
        String description = body.description == null ? null : body.description.trim();
        if (description != null && description.length() > PANEL_DESCRIPTION_MAX_LENGTH) {
            return refused(panel, "description.tooLong");
        }
        List<PanelItem> members = panelItemService.getPanelItemsForPanel(panel.getId());
        Domain requested = null;
        if (body.domain != null) {
            try {
                requested = Domain.valueOf(body.domain);
            } catch (IllegalArgumentException e) {
                return refused(panel, "domain.unknown");
            }
            // domain-guard: a panel never mixes domains — the domain cannot move
            // away from its member tests, and the refusal names them
            List<Test> outside = new ArrayList<>();
            for (PanelItem member : members) {
                if (member.getTest() != null
                        && !requested.name().equals(Domain.normalize(member.getTest().getDomain()))) {
                    outside.add(member.getTest());
                }
            }
            if (!outside.isEmpty()) {
                PanelOption refusal = toPanelOption(panel);
                refusal.refusal = "domain.conflict";
                refusal.domainConflict = domainConflict(requested.name(), outside);
                return ResponseEntity.unprocessableEntity().body(refusal);
            }
        }
        if (Boolean.TRUE.equals(body.active) && members.isEmpty()) {
            // FRS activation rule: never active with zero tests
            return refused(panel, "activation.needsTest");
        }

        if (renamed) {
            panel.setPanelName(name);
            Localization localization = panel.getLocalization();
            if (localization != null) {
                localization.setEnglish(name);
                localization.setSysUserId(sysUserId);
                localizationService.update(localization);
            }
        }
        if (description != null) {
            // the DESCRIPTION column is NOT NULL — a cleared field falls back
            // to the panel's name, matching the create flow
            panel.setDescription(description.isEmpty() ? panel.getPanelName() : description);
        }
        if (requested != null) {
            panel.setDomain(requested.name());
        }
        if (body.active != null) {
            panel.setIsActive(body.active ? "Y" : "N");
        }
        panel.setSysUserId(sysUserId);
        panelService.update(panel);
        refreshPanelDisplayLists();
        return ResponseEntity.ok(toPanelOption(panelService.getPanelById(panel.getId())));
    }

    /** Same rule as the panel DAO's duplicate check: trimmed, case-insensitive. */
    private boolean panelNameTakenByAnother(String name, String panelId) {
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        for (Panel other : panelService.getAllPanels()) {
            if (!other.getId().equals(panelId) && other.getPanelName() != null
                    && other.getPanelName().trim().toLowerCase(Locale.ROOT).equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<PanelOption> refused(Panel panel, String refusal) {
        PanelOption body = toPanelOption(panel);
        body.refusal = refusal;
        return ResponseEntity.unprocessableEntity().body(body);
    }

    @PostMapping(value = "/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PanelOption> createPanel(@RequestBody CreatePanelRequest body, HttpServletRequest request) {
        if (body == null || isBlank(body.name)) {
            return ResponseEntity.unprocessableEntity().build();
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        String name = body.name.trim();

        // Create-if-not-exists: a name that already belongs to a panel returns
        // that panel (200) instead of minting a duplicate — the service layer
        // would otherwise throw LIMSDuplicateRecordException out as a blank 500,
        // which read as "the panel was not created" in the editor.
        Panel existing = panelService.getPanelByName(name);
        if (existing != null) {
            PanelOption option = new PanelOption();
            option.id = existing.getId();
            option.name = existing.getPanelName();
            return ResponseEntity.ok(option);
        }
        if (name.length() > PANEL_NAME_MAX_LENGTH) {
            PanelOption refusal = new PanelOption();
            refusal.name = name;
            refusal.refusal = "name.tooLong";
            return ResponseEntity.unprocessableEntity().body(refusal);
        }

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
        panel.setIsActive(Boolean.FALSE.equals(body.active) ? "N" : "Y");
        // OGC-1140: inherit the originating test's domain (Domain.normalize
        // defaults an absent/unknown value to CLINICAL, the launch scope)
        panel.setDomain(Domain.normalize(body.domain));
        panel.setSortOrderInt(Integer.MAX_VALUE);
        panel.setSysUserId(sysUserId);
        try {
            String id = panelService.insert(panel);
            refreshPanelDisplayLists();
            PanelOption created = new PanelOption();
            created.id = id;
            created.name = panel.getPanelName();
            return ResponseEntity.status(201).body(created);
        } catch (org.openelisglobal.common.exception.LIMSDuplicateRecordException e) {
            // race-safe fallback: someone created it between the lookup and the
            // insert, or the description collides — resolve to the existing row
            Panel raced = panelService.getPanelByName(name);
            if (raced != null) {
                PanelOption option = new PanelOption();
                option.id = raced.getId();
                option.name = raced.getPanelName();
                return ResponseEntity.ok(option);
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
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
        return ResponseEntity.ok(panelTestOrder(panelId));
    }

    private PanelTestOrderResponse panelTestOrder(String panelId) {
        PanelTestOrderResponse resp = new PanelTestOrderResponse();
        resp.panelId = panelId;
        for (PanelItem pi : panelItemService.getPanelItemsForPanel(panelId)) {
            PanelTestRow row = new PanelTestRow();
            row.testId = pi.getTest() != null ? pi.getTest().getId() : null;
            row.testName = pi.getTest() != null ? pi.getTest().getName() : null;
            row.code = pi.getTest() != null ? pi.getTest().getLocalCode() : null;
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
        resp.version = StaleSaveGuard
                .versionOf(resp.tests.stream().map(row -> row.testId + "@" + row.position).toList());
        return resp;
    }

    @PutMapping(value = "/tests/{testId}/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestPanelsResponse> saveTestPanels(@PathVariable String testId,
            @RequestBody PanelMembershipUpdate body, HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        if (body == null || body.memberships == null || body.memberships.stream().anyMatch(Objects::isNull)) {
            return ResponseEntity.badRequest().build();
        }
        // OGC-224: SAMPLETYPE_PANEL must follow every membership write — panels
        // this test leaves need a sync just as much as panels it joins.
        Set<String> affectedPanelIds = new LinkedHashSet<>();
        for (PanelItem pi : panelItemService.getPanelItemByTestId(testId)) {
            if (pi.getPanel() != null) {
                affectedPanelIds.add(pi.getPanel().getId());
            }
        }
        Map<String, Integer> positionByPanelId = new HashMap<>();
        int fallback = 1;
        for (MembershipItem item : body.memberships) {
            if (!isBlank(item.panelId)) {
                // Reject an unknown panel up front rather than letting the service
                // silently drop the membership (mirrors the terminology 422 above).
                Panel panel = findPanel(item.panelId);
                if (panel == null) {
                    return ResponseEntity.unprocessableEntity().build();
                }
                // OGC-224 domain guard — a panel never mixes domains, from
                // either side of the one model; the refusal names the panel
                // and this test (OGC-1232).
                if (!Domain.normalize(panel.getDomain()).equals(Domain.normalize(test.getDomain()))) {
                    TestPanelsResponse refused = new TestPanelsResponse();
                    refused.testId = testId;
                    refused.domainConflict = domainConflict(Domain.normalize(panel.getDomain()), List.of(test));
                    refused.domainConflict.panelId = panel.getId();
                    refused.domainConflict.panelName = panel.getPanelName();
                    return ResponseEntity.unprocessableEntity().body(refused);
                }
                positionByPanelId.put(item.panelId, item.position != null ? item.position : fallback);
            }
            fallback++;
        }
        TestPanelsResponse emptied = new TestPanelsResponse();
        for (String leftPanelId : affectedPanelIds) {
            if (positionByPanelId.containsKey(leftPanelId)) {
                continue;
            }
            Panel left = findPanel(leftPanelId);
            if (left != null && "Y".equals(left.getIsActive())
                    && panelItemService.getPanelItemsForPanel(leftPanelId).size() <= 1) {
                PanelMembership refusedPanel = new PanelMembership();
                refusedPanel.panelId = left.getId();
                refusedPanel.panelName = left.getPanelName();
                emptied.refusedPanels.add(refusedPanel);
            }
        }
        if (!emptied.refusedPanels.isEmpty()) {
            emptied.testId = testId;
            emptied.refusal = "panel.activeNeedsTest";
            return ResponseEntity.unprocessableEntity().body(emptied);
        }
        panelItemService.setMembershipsForTest(test, positionByPanelId, ControllerUtills.getSysUserId(request));
        affectedPanelIds.addAll(positionByPanelId.keySet());
        // null only in hand-constructed unit-style tests (field-injected)
        if (typeOfSamplePanelService != null) {
            for (String panelId : affectedPanelIds) {
                typeOfSamplePanelService.syncPanelSampleTypes(panelId, ControllerUtills.getSysUserId(request));
            }
        }
        refreshPanelDisplayLists();
        return ResponseEntity.ok(toTestPanels(testId));
    }

    /**
     * OGC-224 — the ordered member list write for the panel editor's Tests section.
     */
    public static class PanelTestItem {
        public String testId;
        public Integer position;
    }

    public static class PanelTestsUpdate {
        /**
         * No default: an omitted list is refused, never read as "remove every member".
         */
        public List<PanelTestItem> tests;
        /**
         * Create-flow only: a newly created panel defaults to Active when its first
         * test is added. Honored only when the panel is inactive and had zero members
         * before this write — editing never auto-flips.
         */
        public Boolean autoActivate;
        /** The member-list version the editor loaded (OGC-1463). */
        public String version;
    }

    public static class PanelTestsResponse {
        public PanelOption panel;
        public List<PanelTestRow> tests = new ArrayList<>();
        /** Set only on a 422 body: why the domain guard refused this write. */
        public DomainConflict domainConflict;
        /** Set only on a 422 body: the rule that refused the write (OGC-1456). */
        public String refusal;
        /** With {@code tests.inactive}: the inactive tests the write tried to add. */
        public List<ConflictingTest> refusedTests = new ArrayList<>();
        /** The member-list version after the write, or the current one on a 409. */
        public String version;
        /** Set only on a 409 body: "stale". */
        public String conflict;
        public String messageKey;
    }

    @PutMapping(value = "/panels/{panelId}/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PanelTestsResponse> savePanelTests(@PathVariable String panelId,
            @RequestBody PanelTestsUpdate body, HttpServletRequest request) {
        Panel panel = findPanel(panelId);
        if (panel == null) {
            return ResponseEntity.notFound().build();
        }
        if (body == null || body.tests == null || body.tests.stream().anyMatch(Objects::isNull)) {
            return ResponseEntity.badRequest().build();
        }
        String currentVersion = panelTestOrder(panelId).version;
        if (StaleSaveGuard.isStaleVersion(body.version, currentVersion)) {
            PanelTestsResponse stale = new PanelTestsResponse();
            stale.version = currentVersion;
            stale.conflict = "stale";
            stale.messageKey = STALE_SECTION;
            return ResponseEntity.status(HttpStatus.CONFLICT).body(stale);
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        Map<String, Integer> positionByTestId = new LinkedHashMap<>();
        List<Test> outside = new ArrayList<>();
        List<Test> inactive = new ArrayList<>();
        List<PanelItem> priorMembers = panelItemService.getPanelItemsForPanel(panel.getId());
        Set<String> priorMemberIds = new HashSet<>();
        for (PanelItem member : priorMembers) {
            if (member.getTest() != null) {
                priorMemberIds.add(member.getTest().getId());
            }
        }
        int fallback = 1;
        for (PanelTestItem item : body.tests) {
            if (!TestCatalogNumericIdGuard.isNumericId(item.testId)
                    || positionByTestId.containsKey(item.testId.trim())) {
                // blank, malformed or duplicate member — reject the whole request
                return ResponseEntity.unprocessableEntity().build();
            }
            Test test = testService.getTestById(item.testId.trim());
            if (test == null) {
                return ResponseEntity.unprocessableEntity().build();
            }
            // OGC-224 domain guard — only tests in the panel's domain are
            // accepted; a panel never mixes domains. Every offender is
            // collected so the refusal can name them all (OGC-1232).
            if (!Domain.normalize(panel.getDomain()).equals(Domain.normalize(test.getDomain()))) {
                outside.add(test);
            }
            if (!test.isActive() && !priorMemberIds.contains(test.getId())) {
                inactive.add(test);
            }
            positionByTestId.put(test.getId(), item.position != null ? item.position : fallback);
            fallback++;
        }
        if (!outside.isEmpty()) {
            PanelTestsResponse refused = new PanelTestsResponse();
            refused.domainConflict = domainConflict(Domain.normalize(panel.getDomain()), outside);
            return ResponseEntity.unprocessableEntity().body(refused);
        }
        if (!inactive.isEmpty()) {
            PanelTestsResponse refused = new PanelTestsResponse();
            refused.refusal = "tests.inactive";
            refused.refusedTests = domainConflict(Domain.normalize(panel.getDomain()), inactive).tests;
            return ResponseEntity.unprocessableEntity().body(refused);
        }
        if (positionByTestId.isEmpty() && "Y".equals(panel.getIsActive())) {
            PanelTestsResponse refused = new PanelTestsResponse();
            refused.refusal = "tests.activeNeedsTest";
            return ResponseEntity.unprocessableEntity().body(refused);
        }
        int priorCount = priorMembers.size();
        panelItemService.setMembershipsForPanel(panel, positionByTestId, sysUserId);
        // SAMPLETYPE_PANEL follows the membership write (order entry reads it);
        // null only in hand-constructed unit-style tests (field-injected)
        if (typeOfSamplePanelService != null) {
            typeOfSamplePanelService.syncPanelSampleTypes(panel.getId(), sysUserId);
        }
        // FRS activation rule: create → add first test → active. Only the
        // panel's first-ever test at creation (client sends autoActivate on the
        // create flow); editing never auto-flips.
        if (Boolean.TRUE.equals(body.autoActivate) && priorCount == 0 && !positionByTestId.isEmpty()
                && !"Y".equals(panel.getIsActive())) {
            panel.setIsActive("Y");
            panel.setSysUserId(sysUserId);
            panelService.update(panel);
        }
        refreshPanelDisplayLists();
        PanelTestsResponse resp = new PanelTestsResponse();
        resp.panel = toPanelOption(panelService.getPanelById(panel.getId()));
        PanelTestOrderResponse saved = panelTestOrder(panel.getId());
        resp.tests = saved.tests;
        resp.version = saved.version;
        return ResponseEntity.ok(resp);
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

    private static DomainConflict domainConflict(String domain, List<Test> outside) {
        DomainConflict conflict = new DomainConflict();
        conflict.domain = domain;
        for (Test test : outside) {
            ConflictingTest offender = new ConflictingTest();
            offender.testId = test.getId();
            offender.name = TestServiceImpl.getLocalizedTestNameWithType(test);
            offender.domain = Domain.normalize(test.getDomain());
            conflict.tests.add(offender);
        }
        return conflict;
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
