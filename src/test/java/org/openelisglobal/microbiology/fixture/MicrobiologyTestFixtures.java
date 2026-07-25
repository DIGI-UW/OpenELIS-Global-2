package org.openelisglobal.microbiology.fixture;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.method.service.MethodService;
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
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestService;
import org.springframework.stereotype.Component;

/**
 * Service-backed microbiology integration fixtures. Test code never owns
 * primary keys and never writes around application persistence behavior.
 */
@Component
public class MicrobiologyTestFixtures {

    private static final String DEFAULT_USER_LOGIN = "admin";

    private final MethodService methodService;
    private final SampleService sampleService;
    private final SampleItemService sampleItemService;
    private final TestService testService;
    private final IStatusService statusService;
    private final SystemUserService systemUserService;
    private final MicrobiologyConfigurationService configurationService;

    public MicrobiologyTestFixtures(MethodService methodService, SampleService sampleService,
            SampleItemService sampleItemService, TestService testService, IStatusService statusService,
            SystemUserService systemUserService, MicrobiologyConfigurationService configurationService) {
        this.methodService = methodService;
        this.sampleService = sampleService;
        this.sampleItemService = sampleItemService;
        this.testService = testService;
        this.statusService = statusService;
        this.systemUserService = systemUserService;
        this.configurationService = configurationService;
    }

    public String defaultUserId() {
        return systemUserService.getMatch("loginName", DEFAULT_USER_LOGIN)
                .orElseThrow(() -> new IllegalStateException("No active admin user is available for tests")).getId();
    }

    public String firstMethodId() {
        return first(methodService.getAllActiveMethods(), "No active method is available for microbiology tests")
                .getId();
    }

    public SampleItem createSampleWithSampleItem(String accessionPrefix) {
        String accessionNumber = uniqueValue(accessionPrefix, 20);
        Date today = new Date(System.currentTimeMillis());
        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        sample.setEnteredDate(today);
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(statusService.getStatusID(OrderStatus.Entered));
        sample.setSysUserId(defaultUserId());
        sampleService.insert(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusService.getStatusID(OrderStatus.Entered));
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
