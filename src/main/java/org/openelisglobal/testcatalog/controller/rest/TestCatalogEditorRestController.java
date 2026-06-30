package org.openelisglobal.testcatalog.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
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
        public String code;
        public String domain;
        public boolean active;
        public boolean amr;
        public boolean coverageIncomplete;
    }

    public static class TestListPage {
        public int page;
        public int pageSize;
        public int total;
        public List<TestListRow> rows = new ArrayList<>();
    }

    @GetMapping(value = "/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public TestListPage listTests(@RequestParam(required = false) String domain,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) Boolean amr, @RequestParam(required = false) String sampleType,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize) {
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
            TestListRow row = new TestListRow();
            row.testId = test.getId();
            row.name = name;
            row.code = test.getLocalCode();
            row.domain = test.getDomain();
            row.active = active;
            row.amr = testAmr;
            // Coverage-incomplete decoration is wired with Ranges/Coverage Validation (M7).
            row.coverageIncomplete = false;
            filtered.add(row);
        }
        filtered.sort((a, b) -> {
            String an = a.name == null ? "" : a.name;
            String bn = b.name == null ? "" : b.name;
            return an.compareToIgnoreCase(bn);
        });

        TestListPage result = new TestListPage();
        result.total = filtered.size();
        result.pageSize = Math.max(1, pageSize);
        result.page = Math.max(1, page);
        int from = Math.min((result.page - 1) * result.pageSize, filtered.size());
        int to = Math.min(from + result.pageSize, filtered.size());
        result.rows = new ArrayList<>(filtered.subList(from, to));
        // Augment each name with its sample type — e.g. "Covid-PCR (Urine)" — using
        // the same helper the rest of the app uses (respects augmentTestNameWithType).
        // Done on the page slice only (≤ pageSize lookups).
        for (TestListRow row : result.rows) {
            row.name = TestServiceImpl.getLocalizedTestNameWithType(row.testId);
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

    private static final List<String> DOMAINS = List.of("CLINICAL", "ENVIRONMENTAL", "VECTOR");

    /** OGC-748 Basic Info — identity + domain + AMR flag + status. */
    public static class BasicInfo {
        public String testId;
        public String name;
        public String code;
        public String description;
        public String domain;
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
        // Name/code/description are not editable here (deferred to OGC-950) — reject
        // an attempt to change them rather than silently dropping the edit.
        if (changesImmutableField(body.name, test.getName()) || changesImmutableField(body.code, test.getLocalCode())
                || changesImmutableField(body.description, test.getDescription())) {
            return ResponseEntity.unprocessableEntity().build();
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
        // Activation (N→Y) is gated on reference-range coverage (the H-03 safety
        // gate) and must go through POST .../activate; basic-info only persists a
        // deactivation, so it cannot be used to bypass the coverage acknowledgment.
        if (body.active != null && !body.active) {
            test.setIsActive("N");
        }
        test.setSysUserId(ControllerUtills.getSysUserId(request));
        Test updated = testService.update(test);
        return ResponseEntity.ok(toBasicInfo(updated));
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
                tr.setTestResultType(o.resultType != null ? o.resultType : c.resultType);
                opts.add(tr);
            }
            optionsByCode.put(c.code, opts);
        }
        componentService.saveSampleResults(testId, desired, interpsByCode, optionsByCode, sysUserId);
        return ResponseEntity.ok(toSampleResults(testId));
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
        public String gender;
        public Double minAge;
        public Double maxAge;
        public Double lowNormal;
        public Double highNormal;
        public Double lowCritical;
        public Double highCritical;
        public Double lowReporting;
        public Double highReporting;
    }

    public static class RangesResponse {
        public String testId;
        public List<RangeDto> ranges = new ArrayList<>();
        // The coverage report is computed on every load/save so the UI's per-sex
        // gap panel reflects exactly what was persisted, no separate round-trip.
        public RangeCoverageValidationService.CoverageReport coverage;
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
        List<ResultLimit> desired = new ArrayList<>();
        for (RangeDto r : body.ranges) {
            ResultLimit limit = new ResultLimit();
            if (!isBlank(r.id)) {
                limit.setId(r.id);
            }
            limit.setComponentId(isBlank(r.componentId) ? null : r.componentId);
            limit.setGender(isBlank(r.gender) ? null : r.gender);
            limit.setMinAge(unbox(r.minAge, 0d));
            limit.setMaxAge(unbox(r.maxAge, Double.POSITIVE_INFINITY));
            limit.setLowNormal(unbox(r.lowNormal, Double.NEGATIVE_INFINITY));
            limit.setHighNormal(unbox(r.highNormal, Double.POSITIVE_INFINITY));
            limit.setLowCritical(unbox(r.lowCritical, Double.POSITIVE_INFINITY));
            limit.setHighCritical(unbox(r.highCritical, Double.POSITIVE_INFINITY));
            // Valid / reporting ranges are not edited here; the service preserves
            // whatever the existing row already had (see saveRangesForTest).
            desired.add(limit);
        }
        resultLimitService.saveRangesForTest(testId, desired, ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok(toRanges(testId));
    }

    private RangesResponse toRanges(String testId) {
        RangesResponse resp = new RangesResponse();
        resp.testId = testId;
        List<ResultLimit> limits = resultLimitService.getAllResultLimitsForTest(testId);
        for (ResultLimit l : limits) {
            RangeDto d = new RangeDto();
            d.id = l.getId();
            d.componentId = l.getComponentId();
            d.gender = l.getGender();
            d.minAge = finiteOrNull(l.getMinAge());
            d.maxAge = finiteOrNull(l.getMaxAge());
            d.lowNormal = finiteOrNull(l.getLowNormal());
            d.highNormal = finiteOrNull(l.getHighNormal());
            d.lowCritical = finiteOrNull(l.getLowCritical());
            d.highCritical = finiteOrNull(l.getHighCritical());
            d.lowReporting = finiteOrNull(l.getLowReportingRange());
            d.highReporting = finiteOrNull(l.getHighReportingRange());
            resp.ranges.add(d);
        }
        resp.coverage = coverageService.validate(limits);
        return resp;
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
        TestSampleHandling saved = handlingService.saveForTest(testId, desired, ControllerUtills.getSysUserId(request));
        return ResponseEntity.ok(toStorage(testId, saved));
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
    public List<SampleTypeOption> listSampleTypes() {
        List<SampleTypeOption> options = new ArrayList<>();
        for (TypeOfSample t : typeOfSampleService.getAllTypeOfSamplesSortOrdered()) {
            SampleTypeOption o = new SampleTypeOption();
            o.id = t.getId();
            o.name = !isBlank(t.getDescription()) ? t.getDescription() : t.getLocalAbbreviation();
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
    }

    public static class TerminologyResponse {
        public String testId;
        public List<MappingDto> mappings = new ArrayList<>();
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
            // (source, code) unique within the request — the DB enforces it per test,
            // but reject early + cleanly rather than surfacing a raw 500.
            if (!seen.add(m.source + " " + m.code)) {
                return ResponseEntity.unprocessableEntity().build();
            }
            TestTerminologyMapping e = new TestTerminologyMapping();
            e.setSource(m.source);
            e.setCode(m.code);
            e.setRelationship(isBlank(m.relationship) ? null : m.relationship);
            desired.add(e);
        }
        terminologyService.saveMappingsForTest(testId, desired, ControllerUtills.getSysUserId(request));
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
            resp.mappings.add(dto);
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
