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
import org.openelisglobal.testcatalog.service.TestQcTargetService;
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

    public TestCatalogQcTargetsRestController(TestService testService, TestResultComponentService componentService,
            TestResultService testResultService, TestQcTargetService targetService,
            QCControlLotService controlLotService, DictionaryService dictionaryService,
            UnitOfMeasureService unitOfMeasureService) {
        this.testService = testService;
        this.componentService = componentService;
        this.testResultService = testResultService;
        this.targetService = targetService;
        this.controlLotService = controlLotService;
        this.dictionaryService = dictionaryService;
        this.unitOfMeasureService = unitOfMeasureService;
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

    public static class QcTargets {
        public String testId;
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
        targetService.saveTargetsForTest(testId, desired, ControllerUtills.getSysUserId(request));
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
        if (quantitative) {
            if (dto.expectedValue == null || dto.uncertainty == null) {
                return "expectedValue and uncertainty are required for a quantitative target";
            }
        } else if (GenericValidator.isBlankOrNull(dto.expectedDictResultId)) {
            return "expectedDictResultId is required for a qualitative target";
        }
        return null;
    }

    private static String key(String componentId, String level, String lotId) {
        return (GenericValidator.isBlankOrNull(componentId) ? "" : componentId) + "|" + level.toUpperCase() + "|"
                + (GenericValidator.isBlankOrNull(lotId) ? "" : lotId);
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
