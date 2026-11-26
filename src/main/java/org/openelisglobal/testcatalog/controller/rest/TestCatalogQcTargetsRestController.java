package org.openelisglobal.testcatalog.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcatalog.service.TestQcPolicyService;
import org.openelisglobal.testcatalog.service.TestQcTargetService;
import org.openelisglobal.testcatalog.valueholder.TestQcPolicy;
import org.openelisglobal.testcatalog.valueholder.TestQcTarget;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1148 — the Test Catalog editor's QC Targets section: one target per
 * control level (Low / Normal / High) for the primary component, each with
 * optional per-lot overrides, plus the target-resolution read Results Entry
 * control capture prefills from (lot override, else level default, else blank).
 * OGC-1200 FR-H1: the section also holds the test's QC policy, whether each run
 * must carry a control and what a missing one does, saved with the targets.
 * Same /rest/test-catalog base and ROLE_ADMIN gate as the other sections.
 */
@RestController
@RequestMapping("/rest/test-catalog")
@PreAuthorize("hasRole('ADMIN')")
public class TestCatalogQcTargetsRestController {

    static final List<String> LEVELS = List.of(TestQcTarget.LEVEL_LOW, TestQcTarget.LEVEL_NORMAL,
            TestQcTarget.LEVEL_HIGH);

    private final TestService testService;
    private final TestResultComponentService componentService;
    private final TestResultService testResultService;
    private final TestQcTargetService targetService;
    private final QCControlLotService controlLotService;
    private final DictionaryService dictionaryService;
    private final UnitOfMeasureService unitOfMeasureService;
    private final TestQcPolicyService policyService;

    public TestCatalogQcTargetsRestController(TestService testService, TestResultComponentService componentService,
            TestResultService testResultService, TestQcTargetService targetService,
            QCControlLotService controlLotService, DictionaryService dictionaryService,
            UnitOfMeasureService unitOfMeasureService, TestQcPolicyService policyService) {
        this.testService = testService;
        this.componentService = componentService;
        this.testResultService = testResultService;
        this.targetService = targetService;
        this.controlLotService = controlLotService;
        this.dictionaryService = dictionaryService;
        this.unitOfMeasureService = unitOfMeasureService;
        this.policyService = policyService;
    }

    public static class QcTargetDto {
        public String id;
        public String componentId;
        public String controlLevel;
        public String qcControlLotId;
        public String lotLabel;
        public BigDecimal expectedValue;
        public BigDecimal uncertainty;
        public String expectedDictResultId;
        public String expectedDictResultName;
        public boolean active = true;
    }

    /**
     * A control lot of this test the admin may link an override to (never created
     * here).
     */
    public static class LotOption {
        public String id;
        public String label;
        public String lotNumber;
        public String productName;
        public String controlLevel;
        public String status;
        public Timestamp activationDate;
        public Timestamp expirationDate;
    }

    public static class DictionaryOption {
        public String id;
        public String name;
    }

    /**
     * FR-H1: whether every run of the test must carry a control, a blank and a
     * duplicate, whether a missing one warns (WARN) or holds the test's rows
     * (HOLD), and whether a failed blank or duplicate holds or warns. A failed
     * control always holds. Beside the block, the blank ceiling and the duplicate
     * RPD limit (D-058): left out to keep them, empty to clear them.
     */
    public static class QcPolicyDto {
        public boolean controlRequired;
        public String controlWhenMissing = TestQcPolicy.WhenMissing.WARN.name();
        public boolean blankRequired;
        public String blankWhenMissing = TestQcPolicy.WhenMissing.WARN.name();
        public String blankWhenFails = TestQcPolicy.WhenMissing.HOLD.name();
        public boolean duplicateRequired;
        public String duplicateWhenMissing = TestQcPolicy.WhenMissing.WARN.name();
        public String duplicateWhenFails = TestQcPolicy.WhenMissing.HOLD.name();
        public String blankCeiling;
        public String rpdLimit;
    }

    public static class QcTargets {
        public String testId;
        public QcPolicyDto policy;
        public boolean quantitative;
        public String unit;
        public List<String> levels = LEVELS;
        public List<QcTargetDto> targets = new ArrayList<>();
        public List<LotOption> lots = new ArrayList<>();
        public List<DictionaryOption> dictionaryOptions = new ArrayList<>();
    }

    /** FR-B4: where the effective target came from. */
    public static class EffectiveTarget {
        public String controlLevel;
        public String qcControlLotId;
        public String source;
        public QcTargetDto target;
    }

    @GetMapping(value = "/tests/{testId}/qc-targets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QcTargets> getTargets(@PathVariable String testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toQcTargets(testId));
    }

    @PutMapping(value = "/tests/{testId}/qc-targets", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> saveTargets(@PathVariable String testId, @RequestBody QcTargets body,
            HttpServletRequest request) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        if (body == null || body.targets == null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("targets are required");
        }
        TestResultComponent primary = primaryComponent(testId);
        boolean quantitative = isQuantitative(primary);
        Set<String> componentIds = new HashSet<>();
        for (TestResultComponent component : componentService.getActiveComponentsByTestId(testId)) {
            componentIds.add(component.getId());
        }
        Set<String> lotIds = new HashSet<>();
        for (QCControlLot lot : lotsOfTest(testId)) {
            lotIds.add(lot.getId());
        }
        Map<String, QcTargetDto> activeByKey = new HashMap<>();
        List<TestQcTarget> desired = new ArrayList<>();
        for (QcTargetDto dto : body.targets) {
            if (isUnsavedBlankInactive(dto)) {
                continue;
            }
            String problem = validate(dto, quantitative, componentIds, lotIds);
            if (problem != null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
            }
            if (dto.active) {
                String key = key(dto.componentId, dto.controlLevel, dto.qcControlLotId);
                if (activeByKey.put(key, dto) != null) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body("Only one active target per control level and lot");
                }
            }
            desired.add(toEntity(dto));
        }
        Set<String> submittedIds = new HashSet<>();
        for (QcTargetDto dto : body.targets) {
            if (!GenericValidator.isBlankOrNull(dto.id)) {
                submittedIds.add(dto.id);
            }
        }
        for (TestQcTarget existing : targetService.getByTestId(testId)) {
            if (existing.isActive() && !submittedIds.contains(existing.getId()) && activeByKey.containsKey(
                    key(existing.getComponentId(), existing.getControlLevel(), existing.getQcControlLotId()))) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("An active target already exists for that control level and lot");
            }
        }
        TestQcPolicy policy = null;
        if (body.policy != null) {
            policy = toPolicy(testId, body.policy);
            if (policy == null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("Each when missing and when it fails must be WARN or HOLD");
            }
            if (!isLimit(body.policy.blankCeiling) || !isLimit(body.policy.rpdLimit)) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body("The blank ceiling and RPD limit must be numbers of zero or more");
            }
        }
        String sysUserId = ControllerUtills.getSysUserId(request);
        policyService.saveSection(testId, desired, policy, sysUserId);
        if (body.policy != null && (body.policy.blankCeiling != null || body.policy.rpdLimit != null)) {
            saveLimits(testId, body.policy, sysUserId);
        }
        return ResponseEntity.ok(toQcTargets(testId));
    }

    @GetMapping(value = "/tests/{testId}/qc-targets/effective", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> effectiveTarget(@PathVariable String testId,
            @RequestParam(name = "controlLevel") String controlLevel,
            @RequestParam(name = "qcControlLotId", required = false) String qcControlLotId,
            @RequestParam(name = "componentId", required = false) String componentId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        if (GenericValidator.isBlankOrNull(controlLevel) || !LEVELS.contains(controlLevel.toUpperCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("controlLevel must be LOW, NORMAL or HIGH");
        }
        EffectiveTarget effective = new EffectiveTarget();
        effective.controlLevel = controlLevel.toUpperCase();
        effective.qcControlLotId = GenericValidator.isBlankOrNull(qcControlLotId) ? null : qcControlLotId;
        TestQcTarget target = targetService.resolveEffectiveTarget(testId, componentId, effective.controlLevel,
                qcControlLotId);
        if (target == null) {
            effective.source = "NONE";
        } else {
            effective.source = GenericValidator.isBlankOrNull(target.getQcControlLotId()) ? "LEVEL" : "LOT";
            effective.target = toDto(target, lotLabels(testId));
        }
        return ResponseEntity.ok(effective);
    }

    private String validate(QcTargetDto dto, boolean quantitative, Set<String> componentIds, Set<String> lotIds) {
        if (dto == null || GenericValidator.isBlankOrNull(dto.controlLevel)
                || !LEVELS.contains(dto.controlLevel.toUpperCase())) {
            return "controlLevel must be LOW, NORMAL or HIGH";
        }
        dto.controlLevel = dto.controlLevel.toUpperCase();
        if (!GenericValidator.isBlankOrNull(dto.componentId) && !componentIds.contains(dto.componentId)) {
            return "componentId is not a result component of this test";
        }
        if (!GenericValidator.isBlankOrNull(dto.qcControlLotId) && !lotIds.contains(dto.qcControlLotId)) {
            return "qcControlLotId is not a control lot of this test";
        }
        if (dto.uncertainty != null && dto.uncertainty.signum() < 0) {
            return "uncertainty must be a non-negative number";
        }
        if (tooLarge(dto.expectedValue) || tooLarge(dto.uncertainty)) {
            return "expectedValue and uncertainty hold at most 10 digits before the decimal point";
        }
        if (!GenericValidator.isBlankOrNull(dto.expectedDictResultId)
                && (!TestCatalogNumericIdGuard.isNumericId(dto.expectedDictResultId)
                        || dictionaryService.getDictionaryById(dto.expectedDictResultId.trim()) == null)) {
            return "expectedDictResultId is not a dictionary answer";
        }
        if (!dto.active) {
            return null;
        }
        if (quantitative) {
            if (dto.expectedValue == null || dto.uncertainty == null) {
                return "expectedValue and uncertainty are required for a quantitative target";
            }
        } else if (GenericValidator.isBlankOrNull(dto.expectedDictResultId)) {
            return "expectedDictResultId is required for a qualitative target";
        }
        return null;
    }

    /**
     * A row added and switched off before anything was entered: there is nothing to
     * keep, so it is not stored (OGC-1462).
     */
    private static boolean isUnsavedBlankInactive(QcTargetDto dto) {
        return dto != null && !dto.active && GenericValidator.isBlankOrNull(dto.id) && dto.expectedValue == null
                && dto.uncertainty == null && GenericValidator.isBlankOrNull(dto.expectedDictResultId);
    }

    /** The columns are numeric(15,5): 10 digits before the point (OGC-1467). */
    private static boolean tooLarge(BigDecimal value) {
        return value != null && value.abs().compareTo(BigDecimal.TEN.pow(10)) >= 0;
    }

    private static String key(String componentId, String level, String lotId) {
        return (GenericValidator.isBlankOrNull(componentId) ? "" : componentId) + "|" + level.toUpperCase() + "|"
                + (GenericValidator.isBlankOrNull(lotId) ? "" : lotId);
    }

    /**
     * The policy asked for, or null when its "when missing" is neither WARN nor
     * HOLD.
     */
    private static TestQcPolicy toPolicy(String testId, QcPolicyDto dto) {
        TestQcPolicy.WhenMissing controlMissing = actionOf(dto.controlWhenMissing, TestQcPolicy.WhenMissing.WARN);
        TestQcPolicy.WhenMissing blankMissing = actionOf(dto.blankWhenMissing, TestQcPolicy.WhenMissing.WARN);
        TestQcPolicy.WhenMissing blankFails = actionOf(dto.blankWhenFails, TestQcPolicy.WhenMissing.HOLD);
        TestQcPolicy.WhenMissing duplicateMissing = actionOf(dto.duplicateWhenMissing, TestQcPolicy.WhenMissing.WARN);
        TestQcPolicy.WhenMissing duplicateFails = actionOf(dto.duplicateWhenFails, TestQcPolicy.WhenMissing.HOLD);
        if (controlMissing == null || blankMissing == null || blankFails == null || duplicateMissing == null
                || duplicateFails == null) {
            return null;
        }
        TestQcPolicy policy = TestQcPolicy.none(testId);
        policy.setControlRequired(dto.controlRequired);
        policy.setControlWhenMissing(controlMissing);
        policy.setBlankRequired(dto.blankRequired);
        policy.setBlankWhenMissing(blankMissing);
        policy.setBlankWhenFails(blankFails);
        policy.setDuplicateRequired(dto.duplicateRequired);
        policy.setDuplicateWhenMissing(duplicateMissing);
        policy.setDuplicateWhenFails(duplicateFails);
        return policy;
    }

    /** WARN or HOLD, the default when left out, null when it is neither. */
    private static TestQcPolicy.WhenMissing actionOf(String value, TestQcPolicy.WhenMissing fallback) {
        if (GenericValidator.isBlankOrNull(value)) {
            return fallback;
        }
        try {
            return TestQcPolicy.WhenMissing.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isLimit(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            return new java.math.BigDecimal(value.trim()).signum() >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static java.math.BigDecimal limitOf(String value) {
        return value == null || value.isBlank() ? null : new java.math.BigDecimal(value.trim());
    }

    /** The blank ceiling and RPD limit sent, the recovery window kept as it was. */
    private void saveLimits(String testId, QcPolicyDto dto, String sysUserId) {
        org.openelisglobal.qc.valueholder.TestQcThreshold current = testService.getQcThreshold(testId).orElse(null);
        java.math.BigDecimal blank = dto.blankCeiling != null ? limitOf(dto.blankCeiling)
                : current == null ? null : current.getBlankThreshold();
        java.math.BigDecimal rpd = dto.rpdLimit != null ? limitOf(dto.rpdLimit)
                : current == null ? null : current.getRpdThreshold();
        testService.saveQcThreshold(testId, blank, rpd, current == null ? null : current.getRecoveryWindowPct(),
                sysUserId);
    }

    private TestQcTarget toEntity(QcTargetDto dto) {
        TestQcTarget target = new TestQcTarget();
        target.setId(GenericValidator.isBlankOrNull(dto.id) ? null : dto.id);
        target.setComponentId(dto.componentId);
        target.setControlLevel(dto.controlLevel);
        target.setQcControlLotId(dto.qcControlLotId);
        target.setExpectedValue(dto.expectedValue);
        target.setUncertainty(dto.uncertainty);
        target.setExpectedDictResultId(dto.expectedDictResultId);
        target.setActive(dto.active);
        return target;
    }

    private QcTargets toQcTargets(String testId) {
        QcTargets out = new QcTargets();
        out.testId = testId;
        TestQcPolicy policy = policyService.policyOf(testId);
        out.policy = new QcPolicyDto();
        out.policy.controlRequired = policy.isControlRequired();
        out.policy.controlWhenMissing = policy.getControlWhenMissing().name();
        out.policy.blankRequired = policy.isBlankRequired();
        out.policy.blankWhenMissing = policy.getBlankWhenMissing().name();
        out.policy.blankWhenFails = policy.getBlankWhenFails().name();
        out.policy.duplicateRequired = policy.isDuplicateRequired();
        out.policy.duplicateWhenMissing = policy.getDuplicateWhenMissing().name();
        out.policy.duplicateWhenFails = policy.getDuplicateWhenFails().name();
        org.openelisglobal.qc.valueholder.TestQcThreshold limits = testService.getQcThreshold(testId).orElse(null);
        out.policy.blankCeiling = limits == null || limits.getBlankThreshold() == null ? ""
                : limits.getBlankThreshold().stripTrailingZeros().toPlainString();
        out.policy.rpdLimit = limits == null || limits.getRpdThreshold() == null ? ""
                : limits.getRpdThreshold().stripTrailingZeros().toPlainString();
        TestResultComponent primary = primaryComponent(testId);
        out.quantitative = isQuantitative(primary);
        out.unit = unitName(primary);
        Map<String, String> lotLabels = new HashMap<>();
        for (QCControlLot lot : lotsOfTest(testId)) {
            LotOption option = new LotOption();
            option.id = lot.getId();
            option.lotNumber = lot.getLotNumber();
            option.productName = lot.getProductName();
            option.controlLevel = lot.getControlLevel();
            option.status = lot.getStatus();
            option.activationDate = lot.getActivationDate();
            option.expirationDate = lot.getExpirationDate();
            option.label = lotLabel(lot);
            out.lots.add(option);
            lotLabels.put(lot.getId(), option.label);
        }
        if (!out.quantitative && primary != null) {
            for (TestResult option : testResultService.getActiveOptionsByComponentId(primary.getId())) {
                DictionaryOption dictionaryOption = new DictionaryOption();
                dictionaryOption.id = option.getValue();
                dictionaryOption.name = dictionaryName(option.getValue());
                out.dictionaryOptions.add(dictionaryOption);
            }
        }
        for (TestQcTarget target : targetService.getByTestId(testId)) {
            out.targets.add(toDto(target, lotLabels));
        }
        return out;
    }

    private QcTargetDto toDto(TestQcTarget target, Map<String, String> lotLabels) {
        QcTargetDto dto = new QcTargetDto();
        dto.id = target.getId();
        dto.componentId = target.getComponentId();
        dto.controlLevel = target.getControlLevel();
        dto.qcControlLotId = target.getQcControlLotId();
        dto.lotLabel = target.getQcControlLotId() == null ? null : lotLabels.get(target.getQcControlLotId());
        dto.expectedValue = target.getExpectedValue();
        dto.uncertainty = target.getUncertainty();
        dto.expectedDictResultId = target.getExpectedDictResultId();
        dto.expectedDictResultName = dictionaryName(target.getExpectedDictResultId());
        dto.active = target.isActive();
        return dto;
    }

    private Map<String, String> lotLabels(String testId) {
        Map<String, String> labels = new HashMap<>();
        for (QCControlLot lot : lotsOfTest(testId)) {
            labels.put(lot.getId(), lotLabel(lot));
        }
        return labels;
    }

    private static String lotLabel(QCControlLot lot) {
        String product = GenericValidator.isBlankOrNull(lot.getProductName()) ? "" : lot.getProductName().trim();
        String number = GenericValidator.isBlankOrNull(lot.getLotNumber()) ? "" : lot.getLotNumber().trim();
        return (product + " " + number).trim();
    }

    private List<QCControlLot> lotsOfTest(String testId) {
        List<QCControlLot> lots = new ArrayList<>();
        for (QCControlLot lot : controlLotService.getAllControlLots()) {
            if (lot != null && testId.equals(lot.getTestId())) {
                lots.add(lot);
            }
        }
        return lots;
    }

    private TestResultComponent primaryComponent(String testId) {
        TestResultComponent first = null;
        for (TestResultComponent component : componentService.getActiveComponentsByTestId(testId)) {
            if (component.getIsPrimary()) {
                return component;
            }
            if (first == null) {
                first = component;
            }
        }
        return first;
    }

    private static boolean isQuantitative(TestResultComponent primary) {
        return primary == null || !TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(primary.getResultType());
    }

    private String unitName(TestResultComponent primary) {
        if (primary == null || GenericValidator.isBlankOrNull(primary.getUomId())) {
            return null;
        }
        UnitOfMeasure unit = unitOfMeasureService.getUnitOfMeasureById(primary.getUomId());
        return unit == null ? null : unit.getUnitOfMeasureName();
    }

    private String dictionaryName(String dictionaryId) {
        if (GenericValidator.isBlankOrNull(dictionaryId)) {
            return null;
        }
        Dictionary dictionary = dictionaryService.getDictionaryById(dictionaryId);
        return dictionary == null ? null : dictionary.getDictEntry();
    }
}
