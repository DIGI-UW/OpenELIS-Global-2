package org.openelisglobal.dashboard.management.antibiogram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AntibiogramServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDate COLLECTED = LocalDate.of(2026, 3, 3);
    private static final ScopeRange SCOPE = new ScopeRange(COLLECTED, COLLECTED, COLLECTED.minusDays(1),
            COLLECTED.minusDays(1));
    private static final BigDecimal SUSCEPTIBLE = new BigDecimal("4");
    private static final BigDecimal RESISTANT = new BigDecimal("32");

    @Autowired
    private AntibiogramService antibiogramService;
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicrobiologyUatScenarioService uatScenarioService;
    @Autowired
    private MicroIsolateService isolateService;
    @Autowired
    private MicroAstService astService;
    @Autowired
    private MicroCaseOrderDetailService orderDetailService;
    @Autowired
    private MicroReportReleaseService reportReleaseService;
    @Autowired
    private SampleItemService sampleItemService;
    @PersistenceContext
    private EntityManager entityManager;

    private String user;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureRequiredWorkflowStatuses();
        user = fixtures.defaultUserId();
    }

    // One new patient per call. Readings follow the M4 panel order and null skips
    // that antibiotic; only the read antibiotics are ordered on the run.
    private MicrobiologyUatScenarioForm isolate(String organismId, MicroAstTechnique technique,
            BigDecimal... readings) {
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "M4";
        request.scenarioKey = "antibiogram-" + UUID.randomUUID();
        MicrobiologyUatScenarioForm scenario = uatScenarioService.provision(request, user);
        MicroCaseOrderDetailRequestForm purpose = new MicroCaseOrderDetailRequestForm();
        purpose.culturePurpose = "CLINICAL_DIAGNOSTIC";
        orderDetailService.saveOrderDetail(scenario.caseId, purpose, user);

        SampleItem sampleItem = sampleItemService.get(scenario.sampleItemId);
        sampleItem.setCollectionDate(Timestamp.valueOf(COLLECTED.atTime(9, 0)));
        sampleItem.setSysUserId(user);
        sampleItemService.update(sampleItem);

        MicroIsolate isolate = isolateService.createIsolate(scenario.caseId, "ISO-1", "Gram negative rods",
                "Lactose fermenting colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, user);
        isolateService.updateIdentification(isolate.getId(), organismId == null ? scenario.organismId : organismId,
                "Antibiogram organism", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT,
                MicroIsolateIdentificationStatus.CONFIRMED, "MALDI_TOF", new BigDecimal("99.5"), user);
        List<String> panel = panel(scenario);
        List<String> ordered = IntStream.range(0, readings.length).filter(i -> readings[i] != null).mapToObj(panel::get)
                .toList();
        MicroAstRun run = astService.startRun(isolate.getId(), scenario.astPanelId, scenario.activeBreakpointStandardId,
                "Antibiogram fixture", technique, List.of(), ordered, user);
        for (int i = 0; i < readings.length; i++) {
            if (readings[i] != null) {
                astService.recordReading(run.getId(), panel.get(i), technique.measurementType(), readings[i], user);
            }
        }
        astService.reviewRun(run.getId(), user);
        reportReleaseService.releaseFinal(scenario.caseId, user);
        // workflow_type has no application writer; the WHONET selection still filters
        // on it.
        entityManager.createQuery("update MicroCase c set c.workflowType = 'BACTERIOLOGY' where c.id = :id")
                .setParameter("id", scenario.caseId).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return scenario;
    }

    private MicrobiologyUatScenarioForm isolate(String organismId, BigDecimal... readings) {
        return isolate(organismId, MicroAstTechnique.BROTH_MICRODILUTION, readings);
    }

    private List<String> panel(MicrobiologyUatScenarioForm scenario) {
        return astService.getPanelAntibiotics(scenario.astPanelId).stream().map(ordered -> ordered.getAntibioticId())
                .toList();
    }

    private static AntibiogramResponse.Cell cell(AntibiogramResponse response, String antibioticId) {
        return response.cells().stream().filter(c -> c.antibioticId().equals(antibioticId)).findFirst().orElseThrow();
    }

    @Test
    public void aCellNeedsTenIsolatesTestedAndCountsResistantOnesAmongThem() {
        MicrobiologyUatScenarioForm first = isolate(null, RESISTANT, SUSCEPTIBLE);
        for (int i = 1; i < 10; i++) {
            isolate(null, i < 3 ? RESISTANT : SUSCEPTIBLE, i < 9 ? SUSCEPTIBLE : null);
        }
        List<String> panel = panel(first);

        AntibiogramResponse response = antibiogramService.getAntibiogram(SCOPE);

        assertEquals(1, response.organisms().size());
        assertEquals(first.organismId, response.organisms().get(0).id());
        assertEquals(10, response.organisms().get(0).isolates());
        assertEquals(10, cell(response, panel.get(0)).n());
        assertEquals(30.0, cell(response, panel.get(0)).pctR(), 0.001);
        assertEquals(9, cell(response, panel.get(1)).n());
        assertNull(cell(response, panel.get(1)).pctR());
        assertEquals(2, response.antibiotics().size());
        assertFalse(response.fallback());
    }

    @Test
    public void onlyTheFiveOrganismsWithTheMostFirstIsolatesAreShown() {
        List<String> names = List.of("Antibiogram A", "Antibiogram B", "Antibiogram C", "Antibiogram D",
                "Antibiogram E", "Antibiogram F");
        List<String> ids = names.stream().map(name -> fixtures.createOrganism(name, "GRAM_NEGATIVE", null).getId())
                .toList();
        isolate(ids.get(5), SUSCEPTIBLE);
        isolate(ids.get(5), SUSCEPTIBLE);
        for (int i = 0; i < 5; i++) {
            isolate(ids.get(i), SUSCEPTIBLE);
        }

        List<AntibiogramResponse.Organism> organisms = antibiogramService.getAntibiogram(SCOPE).organisms();

        assertEquals(List.of("Antibiogram F", "Antibiogram A", "Antibiogram B", "Antibiogram C", "Antibiogram D"),
                organisms.stream().map(AntibiogramResponse.Organism::name).toList());
        assertEquals(2, organisms.get(0).isolates());
    }
}
