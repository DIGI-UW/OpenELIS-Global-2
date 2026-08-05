package org.openelisglobal.microbiology.fixture;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.service.MicrobiologyConfigurationService;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestService;
import org.springframework.stereotype.Component;

/**
 * Service-backed microbiology integration fixtures. Test code never owns
 * primary keys and never writes around application persistence behavior.
 */
@Component
public class MicrobiologyTestFixtures {

    private final MethodService methodService;
    private final SampleService sampleService;
    private final SampleItemService sampleItemService;
    private final TestService testService;
    private final IStatusService statusService;
    private final StatusOfSampleService statusOfSampleService;
    private final SystemUserService systemUserService;
    private final MicrobiologyConfigurationService configurationService;

    public MicrobiologyTestFixtures(MethodService methodService, SampleService sampleService,
            SampleItemService sampleItemService, TestService testService, IStatusService statusService,
            StatusOfSampleService statusOfSampleService, SystemUserService systemUserService,
            MicrobiologyConfigurationService configurationService) {
        this.methodService = methodService;
        this.sampleService = sampleService;
        this.sampleItemService = sampleItemService;
        this.testService = testService;
        this.statusService = statusService;
        this.statusOfSampleService = statusOfSampleService;
        this.systemUserService = systemUserService;
        this.configurationService = configurationService;
    }

    public String defaultUserId() {
        return systemUserService.getAllSystemUsers().stream().filter(user -> "Y".equalsIgnoreCase(user.getIsActive()))
                .sorted(Comparator.comparing(SystemUser::getId)).findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No active system user is available for microbiology tests"))
                .getId();
    }

    public String firstMethodId() {
        List<Method> methods = methodService.getAllActiveMethods();
        if (methods != null && !methods.isEmpty()) {
            return methods.get(0).getId();
        }

        Method method = new Method();
        method.setMethodName("Microbiology test");
        method.setDescription("Service-created microbiology integration test method");
        method.setCode("MCR" + uniqueSuffix());
        method.setIsActive(IActionConstants.YES);
        method.setSysUserId(defaultUserId());
        return methodService.insert(method);
    }

    public SampleItem createSampleWithSampleItem(String accessionPrefix) {
        String accessionNumber = uniqueValue(accessionPrefix, 20);
        Date today = new Date(System.currentTimeMillis());
        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        sample.setEnteredDate(today);
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(ensureSampleEnteredStatus());
        sample.setSysUserId(defaultUserId());
        sampleService.insert(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(ensureSampleEnteredStatus());
        sampleItem.setSysUserId(defaultUserId());
        sampleItemService.insert(sampleItem);
        return sampleItem;
    }

    public ReferenceData createReferenceData(String methodId) {
        String suffix = uniqueSuffix();

        MicroOrganism organism = new MicroOrganism();
        organism.setDisplayName("Escherichia coli " + suffix);
        organism.setWhonetCode("ECO" + suffix);
        organism.setOrganismGroup("Enterobacterales");
        configurationService.createOrganism(organism);

        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setDisplayName("Ampicillin " + suffix);
        antibiotic.setWhonetCode("AMP" + suffix);
        antibiotic.setAntibioticClass("Penicillins");
        configurationService.createAntibiotic(antibiotic);

        MicroAstPanel panel = new MicroAstPanel();
        panel.setName("Enterobacterales panel " + suffix);
        panel.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        panel.setOrganismGroup("Enterobacterales");
        configurationService.createAstPanel(panel);

        MicroAstPanelAntibiotic panelAntibiotic = new MicroAstPanelAntibiotic();
        panelAntibiotic.setPanelId(panel.getId());
        panelAntibiotic.setAntibioticId(antibiotic.getId());
        panelAntibiotic.setDisplayOrder(1);
        configurationService.addAntibioticToPanel(panelAntibiotic);

        MicroBreakpointStandard standard = configurationService.getOrCreateBreakpointStandard("CLSI", "2026",
                new Date(System.currentTimeMillis()));

        MicroBreakpointRule rule = breakpointRule(standard.getId(), organism.getId(), antibiotic.getId(),
                new BigDecimal("8.0000"), new BigDecimal("32.0000"));
        configurationService.createBreakpointRule(rule);

        MicroCultureSetup setup = new MicroCultureSetup();
        setup.setMethodId(methodId);
        setup.setName("Urine culture " + suffix);
        setup.setWorkflowType(MicroWorkflowType.BACTERIOLOGY.name());
        setup.setMediaDefaults("Blood agar");
        setup.setIncubationDefaults("18-24h");
        setup.setAtmosphereDefaults("Ambient");
        configurationService.createCultureSetup(setup);

        return new ReferenceData(organism, antibiotic, panel, panelAntibiotic, standard, rule, setup);
    }

    public AlternativeBreakpointData createAlternativeBreakpoint(ReferenceData referenceData) {
        String suffix = uniqueSuffix();
        MicroBreakpointStandard standard = configurationService.getOrCreateBreakpointStandard("EUCAST",
                "2026-" + suffix, new Date(System.currentTimeMillis()));
        MicroBreakpointRule rule = breakpointRule(standard.getId(), referenceData.organism().getId(),
                referenceData.antibiotic().getId(), new BigDecimal("2.0000"), new BigDecimal("8.0000"));
        configurationService.createBreakpointRule(rule);
        return new AlternativeBreakpointData(standard, rule);
    }

    public MicroCultureSetup createTbCultureSetup(String methodId) {
        MicroCultureSetup setup = new MicroCultureSetup();
        setup.setMethodId(methodId);
        setup.setName("TB culture " + uniqueSuffix());
        setup.setWorkflowType(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name());
        setup.setMediaDefaults("MGIT");
        setup.setIncubationDefaults("up to 42 days");
        setup.setAtmosphereDefaults("Ambient");
        return configurationService.createCultureSetup(setup);
    }

    public org.openelisglobal.test.valueholder.Test createCatalogTest() {
        String suffix = uniqueSuffix();
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setName("MicroCatalogIT " + suffix);
        test.setDescription("MicroCatalogIT " + suffix);
        test.setIsActive(IActionConstants.YES);
        test.setGuid(UUID.randomUUID().toString());
        test.setDomain("CLINICAL");
        test.setAntimicrobialResistance(true);
        test.setOrderable(true);
        test.setSysUserId(defaultUserId());
        testService.insert(test);
        return test;
    }

    private MicroBreakpointRule breakpointRule(String standardId, String organismId, String antibioticId,
            BigDecimal susceptibleValue, BigDecimal resistantValue) {
        MicroBreakpointRule rule = new MicroBreakpointRule();
        rule.setStandardId(standardId);
        rule.setOrganismId(organismId);
        rule.setOrganismGroup("Enterobacterales");
        rule.setAntibioticId(antibioticId);
        rule.setMethod("MIC");
        rule.setBreakpointType("MIC");
        rule.setSusceptibleValue(susceptibleValue);
        rule.setResistantValue(resistantValue);
        return rule;
    }

    private String uniqueValue(String prefix, int maxLength) {
        String value = prefix + uniqueSuffix();
        return value.substring(0, Math.min(value.length(), maxLength));
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    public String ensureSampleEnteredStatus() {
        String statusId = statusService.getStatusID(SampleStatus.Entered);
        if (!"-1".equals(statusId) && statusOfSampleService.getMatch("id", statusId).isPresent()) {
            return statusId;
        }

        StatusOfSample existing = statusOfSampleService.getAllStatusOfSamples().stream()
                .filter(status -> "SAMPLE".equals(status.getStatusType()))
                .filter(status -> "SampleEntered".equals(status.getStatusOfSampleName())).findFirst().orElse(null);
        if (existing != null) {
            statusService.refreshCache();
            return existing.getId();
        }

        StatusOfSample entered = new StatusOfSample();
        entered.setStatusOfSampleName("SampleEntered");
        entered.setDescription("The sample has been entered into the system");
        entered.setCode(nextAvailableStatusCode("SAMPLE"));
        entered.setStatusType("SAMPLE");
        entered.setNameKey("status.sample.entered");
        entered.setIsActive(IActionConstants.YES);
        entered.setSysUserId(defaultUserId());
        String generatedId = statusOfSampleService.insert(entered);
        statusService.refreshCache();

        statusId = statusService.getStatusID(SampleStatus.Entered);
        if (!"-1".equals(statusId)) {
            return statusId;
        }
        if (generatedId != null && !generatedId.isBlank()) {
            return generatedId;
        }
        throw new IllegalStateException("Unable to provision SampleStatus.Entered for microbiology tests");
    }

    public void ensureRequiredWorkflowStatuses() {
        ensureSampleEnteredStatus();
        ensureAnalysisNotStartedStatus();
        ensureAnalysisFinalizedStatus();
    }

    public String ensureAnalysisNotStartedStatus() {
        String statusId = statusService.getStatusID(AnalysisStatus.NotStarted);
        if (!"-1".equals(statusId) && statusOfSampleService.getMatch("id", statusId).isPresent()) {
            return statusId;
        }

        StatusOfSample notStarted = new StatusOfSample();
        notStarted.setStatusOfSampleName("Not Tested");
        notStarted.setDescription("This test has not yet been done");
        notStarted.setCode(nextAvailableStatusCode("ANALYSIS"));
        notStarted.setStatusType("ANALYSIS");
        notStarted.setNameKey("status.test.notStarted");
        notStarted.setIsActive(IActionConstants.YES);
        statusOfSampleService.insert(notStarted);
        statusService.refreshCache();

        statusId = statusService.getStatusID(AnalysisStatus.NotStarted);
        if ("-1".equals(statusId)) {
            throw new IllegalStateException("Unable to provision AnalysisStatus.NotStarted for microbiology tests");
        }
        return statusId;
    }

    public String ensureAnalysisFinalizedStatus() {
        String statusId = statusService.getStatusID(AnalysisStatus.Finalized);
        if (!"-1".equals(statusId) && statusOfSampleService.getMatch("id", statusId).isPresent()) {
            return statusId;
        }

        StatusOfSample finalized = new StatusOfSample();
        finalized.setStatusOfSampleName("Finalized");
        finalized.setDescription("The results of the analysis are final");
        finalized.setCode(nextAvailableStatusCode("ANALYSIS"));
        finalized.setStatusType("ANALYSIS");
        finalized.setNameKey("status.test.valid");
        finalized.setIsActive(IActionConstants.YES);
        statusOfSampleService.insert(finalized);
        statusService.refreshCache();

        statusId = statusService.getStatusID(AnalysisStatus.Finalized);
        if ("-1".equals(statusId)) {
            throw new IllegalStateException("Unable to provision AnalysisStatus.Finalized for microbiology tests");
        }
        return statusId;
    }

    private String nextAvailableStatusCode(String statusType) {
        Set<String> usedCodes = new HashSet<>();
        for (StatusOfSample status : statusOfSampleService.getAllStatusOfSamples()) {
            if (statusType.equalsIgnoreCase(status.getStatusType()) && status.getCode() != null) {
                usedCodes.add(status.getCode());
            }
        }
        for (int code = 900; code <= 999; code++) {
            String candidate = Integer.toString(code);
            if (!usedCodes.contains(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No test-only " + statusType + " status code is available");
    }

    private <T> T first(List<T> values, String message) {
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException(message);
        }
        return values.get(0);
    }

    public record ReferenceData(MicroOrganism organism, MicroAntibiotic antibiotic, MicroAstPanel panel,
            MicroAstPanelAntibiotic panelAntibiotic, MicroBreakpointStandard standard, MicroBreakpointRule rule,
            MicroCultureSetup cultureSetup) {
    }

    public record AlternativeBreakpointData(MicroBreakpointStandard standard, MicroBreakpointRule rule) {
    }
}
