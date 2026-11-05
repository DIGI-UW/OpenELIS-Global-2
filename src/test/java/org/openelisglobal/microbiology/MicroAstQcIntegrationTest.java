package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.*;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroAstQcRequestForm;
import org.openelisglobal.microbiology.form.MicroAstQcResultForm;
import org.openelisglobal.microbiology.form.MicroAstQcViewForm;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qc.builder.QCControlLotBuilder;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

@org.springframework.transaction.annotation.Transactional
public class MicroAstQcIntegrationTest extends BaseWebContextSensitiveTest {
    private static final String STRAIN = "E. coli ATCC 25922";

    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroAstQcService qc;
    @Autowired
    private MicroQcRangeService qcRanges;
    @Autowired
    private MicroAstService ast;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroIsolateDAO isolates;
    @Autowired
    private MicroAstPanelDAO panels;
    @Autowired
    private MicroAstPanelAntibioticDAO panelDrugs;
    @Autowired
    private MicroAntibioticDAO antibiotics;
    @Autowired
    private MicroBreakpointRuleDAO rules;
    @Autowired
    private MicroBreakpointAdminService breakpointAdminService;
    @Autowired
    private MicroAstPanelAssignmentService assignments;
    @Autowired
    private MicroCaseAnalysisService caseAnalyses;
    @Autowired
    private QCControlLotService controlLots;
    @Autowired
    private QCResultService qcResults;
    @Autowired
    private QCRuleViolationDAO violationDAO;
    @Autowired
    private NCEventService ncEvents;
    @Autowired
    private SiteInformationService siteInformation;
    @Autowired
    private InventoryItemService inventoryItems;
    @Autowired
    private InventoryManagementService inventory;
    @Autowired
    private InventoryLotService inventoryLots;
    @Autowired
    private TestReagentLinkService reagentLinks;
    @Autowired
    private org.openelisglobal.systemuser.service.SystemUserService users;
    @Autowired
    private org.openelisglobal.role.service.RoleService roles;
    @PersistenceContext
    private EntityManager em;

    private MicroCase c;
    private MicrobiologyTestFixtures.ReferenceData reference;
    private String writer, validator, outsider, drugB;
    private org.openelisglobal.test.valueholder.Test qcTest;
    private QCControlLot controlLot;
    private InventoryItem cards;
    private String blockingFlag;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureCultureAtmosphere();
        executeDataSetWithStateManagement("testdata/system-user.xml");
        executeDataSetWithStateManagement("testdata/role.xml");
        em.clear();
        blockingFlag = ConfigurationProperties.getInstance().getPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION);
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION, "true");
        var unit = fixtures.createLabUnit();
        writer = actor(unit.getId(), Constants.ROLE_RESULTS);
        validator = actor(unit.getId(), Constants.ROLE_VALIDATION);
        outsider = actor(fixtures.createLabUnit().getId(), Constants.ROLE_RESULTS);
        var source = fixtures.createSampleWithSampleItem("AQC");
        c = new MicroCase();
        c.setSampleId(source.getSample().getId());
        c.setSampleItemId(source.getId());
        c.setLabUnitId(unit.getId());
        c.setCreatedBy(writer);
        cases.insert(c);
        reference = fixtures.createReferenceData(fixtures.createMethodId());
        reference.panel().setPublishedAt(new Timestamp(System.currentTimeMillis()));
        reference.panel().setPublishedBy(writer);
        panels.update(reference.panel());
        var b = new MicroAntibiotic();
        b.setDisplayName("Second agent");
        b.setWhonetCode("B" + UUID.randomUUID().toString().substring(0, 7));
        antibiotics.insert(b);
        drugB = b.getId();
        var ordered = new MicroAstPanelAntibiotic();
        ordered.setPanelId(reference.panel().getId());
        ordered.setAntibioticId(drugB);
        ordered.setDisplayOrder(2);
        panelDrugs.insert(ordered);
        var rule = new MicroBreakpointRule();
        rule.setStandardId(reference.standard().getId());
        rule.setOrganismId(reference.organism().getId());
        rule.setAntibioticId(drugB);
        rule.setMethod("MIC");
        rule.setBreakpointType("MIC");
        rule.setSusceptibleValue(new BigDecimal("8"));
        rule.setResistantValue(new BigDecimal("32"));
        rules.insert(rule);
        breakpointAdminService.activate(reference.standard().getId(), new java.sql.Date(System.currentTimeMillis()),
                fixtures.defaultUserId());

        qcTest = fixtures.createCatalogTest();
        qcTest.setTestSection(unit);
        em.merge(qcTest);
        var setting = new SiteInformation();
        setting.setName(MicroAstQcService.AST_QC_TEST_SETTING);
        setting.setValueType("text");
        setting.setValue(qcTest.getId());
        setting.setSysUserId(fixtures.defaultUserId());
        siteInformation.persistData(setting, true);
        controlLot = QCControlLotBuilder.create().withId(UUID.randomUUID().toString()).withProductName(STRAIN)
                .withLotNumber("ATCC-" + UUID.randomUUID().toString().substring(0, 8)).withTestId(qcTest.getId())
                .withInstrumentId(null).asActive().build();
        controlLot.setSystemUserId(Integer.valueOf(fixtures.defaultUserId()));
        controlLots.insert(controlLot);
        qcRanges.upsert(STRAIN, reference.antibiotic().getId(), MicroAstMethod.MIC, null, reference.standard().getId(),
                new BigDecimal("1"), new BigDecimal("4"));
        qcRanges.upsert(STRAIN, drugB, MicroAstMethod.MIC, null, reference.standard().getId(), new BigDecimal("2"),
                new BigDecimal("8"));
        cards = cardItem();
        em.flush();
    }

    @After
    public void restoreBlockingFlag() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION,
                blockingFlag == null ? "false" : blockingFlag);
    }

    @Test
    public void controlWithinEveryRangePassesStoresQcResultsAndPassesThePendingLotUnderTest() {
        var run = start(isolate("ISO-1"), List.of());
        InventoryLot newLot = lot(QCStatus.PENDING);

        MicroAstQcResultForm recorded = qc.recordControl(run.getId(), request(List.of(newLot.getId()), "2", "8"),
                writer);

        assertTrue(recorded.passed);
        assertEquals(2, recorded.rows.size());
        for (var row : recorded.rows) {
            assertEquals("PASS", row.outcome);
            QCResult stored = qcResults.get(row.qcResultId);
            assertEquals(qcTest.getId(), stored.getTestId());
            assertEquals(controlLot.getId(), stored.getControlLotId());
            assertEquals("ACCEPTED", stored.getResultStatus());
        }
        var drugBRow = recorded.rows.stream().filter(r -> drugB.equals(r.antibioticId)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("2").compareTo(drugBRow.low));
        assertEquals(0, new BigDecimal("8").compareTo(drugBRow.high));
        QCResult drugBResult = qcResults.get(drugBRow.qcResultId);
        assertEquals(0, new BigDecimal("5").compareTo(drugBResult.getExpectedValue()));
        assertEquals(0, new BigDecimal("3").compareTo(drugBResult.getUncertainty()));
        em.flush();
        em.clear();
        assertEquals(QCStatus.PASSED, inventoryLots.get(newLot.getId()).getQcStatus());
    }

    @Test
    public void outOfRangeControlHoldsTheRunAndRunsThatUsedTheLotUntilItsNceIsClosed() {
        InventoryLot usedLot = lot(QCStatus.PASSED);
        var reagentLink = reagentLink();
        var analysis = fixtures.createAnalysis(
                em.find(org.openelisglobal.sampleitem.valueholder.SampleItem.class, c.getSampleItemId()),
                em.find(org.openelisglobal.test.valueholder.Test.class, reagentLink.getTestId()));
        caseAnalyses.linkAnalysis(c, analysis, null);
        var usedRun = start(isolate("ISO-USED"),
                List.of(new MicroLotSelection(analysis.getId(), reagentLink.getId(), usedLot.getId())));
        var recordedOn = start(isolate("ISO-QC"), List.of());
        var unrelated = start(isolate("ISO-OTHER"), List.of());
        var reviewedRun = start(isolate("ISO-DONE"),
                List.of(new MicroLotSelection(analysis.getId(), reagentLink.getId(), usedLot.getId())));
        for (var run : List.of(usedRun, recordedOn, unrelated, reviewedRun)) {
            enter(run, "4");
        }
        ast.reviewRun(reviewedRun.getId(), validator);
        InventoryLot newLot = lot(QCStatus.PENDING);

        MicroAstQcResultForm recorded = qc.recordControl(recordedOn.getId(),
                request(List.of(usedLot.getId(), newLot.getId()), "2", "50"), writer);

        assertFalse(recorded.passed);
        var failing = recorded.rows.stream().filter(r -> drugB.equals(r.antibioticId)).findFirst().orElseThrow();
        assertEquals("FAIL", failing.outcome);
        assertEquals("REJECTED", qcResults.get(failing.qcResultId).getResultStatus());
        em.flush();
        em.clear();
        assertEquals(QCStatus.PENDING, inventoryLots.get(newLot.getId()).getQcStatus());
        assertHeld(recordedOn);
        assertHeld(usedRun);
        assertFalse(qc.getView(c.getId(), unrelated.getId(), writer).held);
        assertFalse(qc.getView(c.getId(), reviewedRun.getId(), writer).held);
        ast.reviewRun(unrelated.getId(), validator);

        QCResult failingResult = qcResults.get(failing.qcResultId);
        var violation = new QCRuleViolation();
        violation.setId(UUID.randomUUID().toString());
        violation.setTriggeringResultId(failingResult.getId());
        violation.setRuleCode("MANUAL_FAIL");
        violation.setViolationDateTime(new Timestamp(System.currentTimeMillis()));
        violation.setSeverity("REJECTION");
        violation.setTestId(failingResult.getTestId());
        violation.setResolutionStatus("UNRESOLVED");
        violation.setSystemUserId(Integer.valueOf(writer));
        violationDAO.insert(violation);
        var nce = new NcEvent();
        nce.setStatus("Pending");
        nce.setTriggerSourceType("QC_VIOLATION");
        nce.setTriggerSourceId(violation.getId());
        ncEvents.insert(nce);
        em.flush();
        assertHeld(recordedOn);

        nce.setStatus("Closed");
        ncEvents.update(nce);
        em.flush();
        assertFalse(qc.getView(c.getId(), recordedOn.getId(), writer).held);
        ast.reviewRun(recordedOn.getId(), validator);
        ast.reviewRun(usedRun.getId(), validator);
    }

    @Test
    public void heldRunReviewsWhenTheLabDoesNotBlockValidationOnQcFailure() {
        var run = start(isolate("ISO-1"), List.of());
        enter(run, "4");
        qc.recordControl(run.getId(), request(List.of(), "2", "50"), writer);
        em.flush();
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION, "false");
        assertFalse(qc.getView(c.getId(), run.getId(), writer).held);
        ast.reviewRun(run.getId(), validator);
    }

    @Test
    public void controlIsRefusedWithoutARangeOrAValueForEveryAntibioticOrWriteAccess() {
        var run = start(isolate("ISO-1"), List.of());
        InventoryLot newLot = lot(QCStatus.PENDING);

        var outsiderError = assertThrows(AccessDeniedException.class,
                () -> qc.recordControl(run.getId(), request(List.of(newLot.getId()), "2", "8"), outsider));
        assertNotNull(outsiderError);

        var incomplete = request(List.of(newLot.getId()), "2", "8");
        incomplete.results = incomplete.results.subList(0, 1);
        assertThrows(IllegalArgumentException.class, () -> qc.recordControl(run.getId(), incomplete, writer));

        var c2 = new MicroAntibiotic();
        c2.setDisplayName("Third agent");
        c2.setWhonetCode("C" + UUID.randomUUID().toString().substring(0, 7));
        antibiotics.insert(c2);
        var third = new MicroAstPanelAntibiotic();
        third.setPanelId(reference.panel().getId());
        third.setAntibioticId(c2.getId());
        third.setDisplayOrder(3);
        panelDrugs.insert(third);
        var rangeless = start(isolate("ISO-2"), List.of());
        var missing = request(List.of(newLot.getId()), "2", "8");
        var measurement = new MicroAstQcRequestForm.Measurement();
        measurement.antibioticId = c2.getId();
        measurement.measuredValue = new BigDecimal("4");
        missing.results = new ArrayList<>(missing.results);
        missing.results.add(measurement);
        var noRange = assertThrows(MicroAstConflictException.class,
                () -> qc.recordControl(rangeless.getId(), missing, writer));
        assertEquals("AST_QC_RANGE_MISSING", noRange.getMessage());

        em.flush();
        assertEquals(0L, em.createQuery("select count(r) from QCResult r where r.controlLotId = :lot", Long.class)
                .setParameter("lot", controlLot.getId()).getSingleResult().longValue());
        em.clear();
        assertEquals(QCStatus.PENDING, inventoryLots.get(newLot.getId()).getQcStatus());
    }

    @Test
    public void viewListsLotsCandidatesRangesPerControlLotAndRecordedControlsForTheRunsCase() {
        InventoryLot usedLot = lot(QCStatus.PASSED);
        InventoryLot newLot = lot(QCStatus.PENDING);
        var reagentLink = reagentLink();
        var analysis = fixtures.createAnalysis(
                em.find(org.openelisglobal.sampleitem.valueholder.SampleItem.class, c.getSampleItemId()),
                em.find(org.openelisglobal.test.valueholder.Test.class, reagentLink.getTestId()));
        caseAnalyses.linkAnalysis(c, analysis, null);
        var run = start(isolate("ISO-1"),
                List.of(new MicroLotSelection(analysis.getId(), reagentLink.getId(), usedLot.getId())));
        var aureus = QCControlLotBuilder.create().withId(UUID.randomUUID().toString())
                .withProductName("S. aureus ATCC 29213")
                .withLotNumber("ATCC-" + UUID.randomUUID().toString().substring(0, 8)).withTestId(qcTest.getId())
                .withInstrumentId(null).asActive().build();
        aureus.setSystemUserId(Integer.valueOf(fixtures.defaultUserId()));
        controlLots.insert(aureus);
        em.flush();

        MicroAstQcViewForm view = qc.getView(c.getId(), run.getId(), writer);

        assertTrue(view.configured);
        assertTrue(view.canRecord);
        assertEquals(List.of(reference.antibiotic().getId(), drugB), view.antibiotics.stream().map(a -> a.id).toList());
        assertEquals(List.of(usedLot.getId()), view.lots.stream().map(l -> l.id).toList());
        assertEquals("PASSED", view.lots.get(0).qcStatus);
        assertTrue(view.candidateLots.stream().anyMatch(l -> newLot.getId().equals(l.id)));
        assertTrue(view.candidateLots.stream().noneMatch(l -> usedLot.getId().equals(l.id)));
        var ecoli = view.controlLots.stream().filter(l -> controlLot.getId().equals(l.id)).findFirst().orElseThrow();
        assertTrue(ecoli.label.contains(STRAIN));
        assertEquals(0, new BigDecimal("2").compareTo(ecoli.ranges.get(drugB).low));
        assertEquals(0, new BigDecimal("8").compareTo(ecoli.ranges.get(drugB).high));
        assertTrue(view.controlLots.stream().filter(l -> aureus.getId().equals(l.id)).findFirst().orElseThrow().ranges
                .isEmpty());
        assertTrue(view.controls.isEmpty());

        qc.recordControl(run.getId(), request(List.of(newLot.getId()), "2", "50"), writer);
        em.flush();
        view = qc.getView(c.getId(), run.getId(), validator);

        assertFalse(view.canRecord);
        assertEquals(1, view.controls.size());
        var control = view.controls.get(0);
        assertFalse(control.passed);
        assertTrue(control.controlLotLabel.contains(STRAIN));
        assertNotNull(control.recordedAt);
        var failing = control.rows.stream().filter(r -> drugB.equals(r.antibioticId)).findFirst().orElseThrow();
        assertEquals("FAIL", failing.outcome);
        assertEquals(0, new BigDecimal("2").compareTo(failing.low));
        assertEquals(0, new BigDecimal("8").compareTo(failing.high));
        assertEquals(0, new BigDecimal("50").compareTo(failing.measuredValue));

        var other = new MicroCase();
        other.setSampleId(c.getSampleId());
        other.setSampleItemId(fixtures.createSampleWithSampleItem("AQX").getId());
        other.setLabUnitId(c.getLabUnitId());
        other.setCreatedBy(writer);
        cases.insert(other);
        assertThrows(IllegalArgumentException.class, () -> qc.getView(other.getId(), run.getId(), writer));
    }

    private void assertHeld(MicroAstRun run) {
        assertTrue(qc.getView(c.getId(), run.getId(), writer).held);
        var error = assertThrows(MicroAstConflictException.class, () -> ast.reviewRun(run.getId(), validator));
        assertEquals("AST_QC_HOLD", error.getMessage());
    }

    private MicroAstQcRequestForm request(List<Long> lotIds, String drugAValue, String drugBValue) {
        var request = new MicroAstQcRequestForm();
        request.controlLotId = controlLot.getId();
        request.lotIds = lotIds;
        var a = new MicroAstQcRequestForm.Measurement();
        a.antibioticId = reference.antibiotic().getId();
        a.measuredValue = new BigDecimal(drugAValue);
        var b = new MicroAstQcRequestForm.Measurement();
        b.antibioticId = drugB;
        b.measuredValue = new BigDecimal(drugBValue);
        request.results = List.of(a, b);
        return request;
    }

    private MicroAstRun start(String isolateId, List<MicroLotSelection> lots) {
        return ast.startRun(isolateId, null, reference.standard().getId(), null, MicroAstTechnique.BROTH_MICRODILUTION,
                lots, writer);
    }

    private void enter(MicroAstRun run, String value) {
        ast.recordReading(run.getId(), reference.antibiotic().getId(), new BigDecimal(value), writer);
        ast.recordReading(run.getId(), drugB, new BigDecimal(value), writer);
    }

    private String isolate(String label) {
        var i = new MicroIsolate();
        i.setCaseId(c.getId());
        i.setIsolateLabel(label);
        i.setCreatedBy(writer);
        i.setOrganismId(reference.organism().getId());
        i.setIdentificationStatus("CONFIRMED");
        isolates.insert(i);
        assignments.assignDefault(i, writer);
        return i.getId();
    }

    private InventoryItem cardItem() {
        var item = new InventoryItem();
        item.setFhirUuid(UUID.randomUUID());
        item.setName("AST cards " + UUID.randomUUID().toString().substring(0, 8));
        item.setCategory("Microbiology test");
        item.setUnits("unit");
        item.setQuantityPerUnit(1);
        item.setLowStockThreshold(1);
        item.setExpirationAlertDays(30);
        item.setIsActive(IActionConstants.YES);
        item.setSysUserId(fixtures.defaultUserId());
        inventoryItems.insert(item);
        return item;
    }

    private InventoryLot lot(QCStatus status) {
        var lot = new InventoryLot();
        lot.setInventoryItem(cards);
        lot.setLotNumber("AST-" + UUID.randomUUID().toString().substring(0, 12));
        lot.setExpirationDate(Timestamp.from(Instant.now().plusSeconds(30L * 86_400L)));
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        lot.setQcStatus(status);
        lot.setStatus(LotStatus.ACTIVE);
        return inventory.receiveInventory(lot, fixtures.defaultUserId());
    }

    private TestReagentLink reagentLink() {
        var test = fixtures.createCatalogTest();
        var link = new TestReagentLink();
        link.setTestId(test.getId());
        link.setReagentId(cards.getId());
        link.setUsageType("PRIMARY");
        link.setQuantityPerTest(BigDecimal.ONE);
        link.setQuantityUnit("unit");
        link.setSysUserId(fixtures.defaultUserId());
        reagentLinks.insert(link);
        return link;
    }

    private String actor(String unit, String role) {
        var user = new org.openelisglobal.systemuser.valueholder.SystemUser();
        user.setLoginName("AQC" + UUID.randomUUID().toString().substring(0, 12));
        user.setFirstName("AQC" + UUID.randomUUID().toString().substring(0, 8));
        user.setLastName(role);
        user.setIsActive("Y");
        user.setIsEmployee("Y");
        user.setSysUserId("1");
        String id = users.insert(user);
        var assignment = new org.openelisglobal.userrole.valueholder.UserLabUnitRoles();
        assignment.setId(Integer.valueOf(id));
        var map = new org.openelisglobal.userrole.valueholder.LabUnitRoleMap();
        map.setLabUnit(unit);
        map.setRoles(Set.of(roles.getRoleByName(role).getId()));
        assignment.setLabUnitRoleMap(Set.of(map));
        em.persist(assignment);
        return id;
    }
}
