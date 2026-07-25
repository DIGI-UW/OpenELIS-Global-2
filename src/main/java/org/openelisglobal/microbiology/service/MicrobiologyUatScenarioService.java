package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions deterministic review scenarios through normal application
 * services. The HTTP entry point is disabled unless the UAT scenario property
 * is explicitly enabled.
 */
@Service
public class MicrobiologyUatScenarioService {

    private static final String BACTERIOLOGY = MicroWorkflowType.BACTERIOLOGY.name();
    private static final String WORKLIST_SCENARIO = "WORKLIST";

    private final MethodService methodService;
    private final SampleService sampleService;
    private final SampleItemService sampleItemService;
    private final IStatusService statusService;
    private final MicrobiologyConfigurationService configurationService;
    private final MicroCaseService caseService;

    public MicrobiologyUatScenarioService(MethodService methodService, SampleService sampleService,
            SampleItemService sampleItemService, IStatusService statusService,
            MicrobiologyConfigurationService configurationService, MicroCaseService caseService) {
        this.methodService = methodService;
        this.sampleService = sampleService;
        this.sampleItemService = sampleItemService;
        this.statusService = statusService;
        this.configurationService = configurationService;
        this.caseService = caseService;
    }

    @Transactional
    public MicrobiologyUatScenarioForm provision(MicrobiologyUatScenarioRequestForm request, String performedBy) {
        String scenario = normalizeScenario(request == null ? null : request.scenario);
        String scenarioKey = normalizeScenarioKey(request == null ? null : request.scenarioKey);
        String suffix = deterministicSuffix(scenarioKey);
        String accessionNumber = "UATMICRO" + suffix;

        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        SampleItem sampleItem;
        if (sample == null) {
            sample = createSample(accessionNumber, performedBy);
            sampleItem = createSampleItem(sample, performedBy);
        } else {
            List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sample.getId());
            if (items.isEmpty()) {
                sampleItem = createSampleItem(sample, performedBy);
            } else {
                sampleItem = items.get(0);
            }
        }
        createAstReferenceData();

        Method method = firstActiveMethod();
        MicroCase microCase = caseService.createOrGetCase(sampleItem.getId(), MicroWorkflowType.BACTERIOLOGY,
                method.getId(), performedBy);
        MicroCase sibling = null;
        if (WORKLIST_SCENARIO.equals(scenario)) {
            sibling = caseService.createOrGetCase(sampleItem.getId(), MicroWorkflowType.MYCOBACTERIOLOGY_TB,
                    method.getId(), performedBy);
        }

        MicrobiologyUatScenarioForm form = new MicrobiologyUatScenarioForm();
        form.scenario = scenario;
        form.scenarioKey = scenarioKey;
        form.accessionNumber = accessionNumber;
        form.sampleId = sample.getId();
        form.sampleItemId = sampleItem.getId();
        form.caseId = microCase.getId();
        form.siblingCaseId = sibling == null ? null : sibling.getId();
        return form;
    }

    private Sample createSample(String accessionNumber, String performedBy) {
        Date today = new Date(System.currentTimeMillis());
        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        sample.setEnteredDate(today);
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(statusService.getStatusID(OrderStatus.Entered));
        sample.setSysUserId(performedBy);
        sampleService.insert(sample);
        return sample;
    }

    private SampleItem createSampleItem(Sample sample, String performedBy) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusService.getStatusID(OrderStatus.Entered));
        sampleItem.setSysUserId(performedBy);
        sampleItemService.insert(sampleItem);
        return sampleItem;
    }

    private void createAstReferenceData() {
        MicroAntibiotic antibiotic = configurationService.getOrCreateAntibiotic("Ciprofloxacin (UAT)", "CIPUAT",
                "Fluoroquinolone");
        MicroAstPanel panel = configurationService.getOrCreateAstPanel("Gram negative AST panel (UAT)", BACTERIOLOGY,
                "GRAM_NEGATIVE");
        configurationService.getOrCreatePanelAntibiotic(panel.getId(), antibiotic.getId(), 1);

        MicroBreakpointStandard standard = configurationService.getOrCreateBreakpointStandard("CLSI", "2026",
                new Date(System.currentTimeMillis()));
        MicroBreakpointRule rule = new MicroBreakpointRule();
        rule.setStandardId(standard.getId());
        rule.setAntibioticId(antibiotic.getId());
        rule.setMethod("MIC");
        rule.setBreakpointType("MIC");
        rule.setSusceptibleValue(new BigDecimal("8"));
        rule.setIntermediateLowerValue(new BigDecimal("16"));
        rule.setIntermediateUpperValue(new BigDecimal("16"));
        rule.setResistantValue(new BigDecimal("32"));
        configurationService.getOrCreateBreakpointRule(rule);
    }

    private Method firstActiveMethod() {
        List<Method> methods = methodService.getAllActiveMethods();
        if (methods == null || methods.isEmpty()) {
            throw new IllegalStateException("No active method is available for a microbiology UAT scenario");
        }
        return methods.get(0);
    }

    private String normalizeScenario(String scenario) {
        String normalized = scenario == null ? "MVP" : scenario.trim().toUpperCase(Locale.ROOT);
        if (!"CASE".equals(normalized) && !"MVP".equals(normalized) && !WORKLIST_SCENARIO.equals(normalized)) {
            throw new IllegalArgumentException("scenario must be CASE, MVP, or WORKLIST");
        }
        return normalized;
    }

    private String normalizeScenarioKey(String scenarioKey) {
        if (scenarioKey == null || scenarioKey.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return scenarioKey.trim();
    }

    private String deterministicSuffix(String scenarioKey) {
        UUID uuid = UUID.nameUUIDFromBytes(scenarioKey.getBytes(StandardCharsets.UTF_8));
        return uuid.toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }
}
