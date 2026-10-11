package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.hibernate.Hibernate;
import org.junit.*;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/** Exercises persisted relationships and authorization against PostgreSQL. */
@org.springframework.transaction.annotation.Transactional
public class MicroIsolateWorkspaceIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroIsolateWorkspaceService workspace;
    @Autowired
    private MicrobiologyCaseAccessService access;
    @Autowired
    private MicroReceivedIsolateService receivedService;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroIsolateDAO isolates;
    @Autowired
    private MicroCaseInoculationDAO cultures;
    @Autowired
    private MicroCaseActivityDAO activities;
    @Autowired
    private MicroIsolateIdentificationEventDAO history;
    @Autowired
    private MicroCaseMembershipService membership;
    @Autowired
    private MicroCaseAmendmentService amendments;
    @Autowired
    private MicroReportVersionService reports;
    @Autowired
    private MicroCaseReadinessService readiness;
    @Autowired
    private MicroAstService ast;
    @Autowired
    private MicroOrderRoutingService routing;
    @Autowired
    private org.openelisglobal.test.service.TestSectionService units;
    @Autowired
    private org.openelisglobal.sampleitem.service.SampleItemService sampleItems;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService sampleTypes;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService sampleTests;
    @Autowired
    private org.openelisglobal.test.service.TestService tests;
    @Autowired
    private org.openelisglobal.organization.service.OrganizationService organizations;
    @Autowired
    private org.openelisglobal.referral.service.ReferralReasonService reasons;
    @Autowired
    private org.openelisglobal.referral.dao.ReferralDAO referrals;
    @Autowired
    private org.openelisglobal.systemuser.service.SystemUserService users;
    @Autowired
    private org.openelisglobal.role.service.RoleService roles;
    @PersistenceContext
    private EntityManager em;
    private MicroCase c;
    private SampleItem source;
    private TypeOfSample isolateType;
    private String writer, validator, outsider, organismA, organismB, method;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/system-user.xml");
        executeDataSetWithStateManagement("testdata/role.xml");
        em.clear();
        assertNotEquals(roles.getRoleByName(Constants.ROLE_RESULTS).getId(),
                roles.getRoleByName(Constants.ROLE_VALIDATION).getId());
        var unit = fixtures.createLabUnit();
        writer = actor(unit.getId(), Constants.ROLE_RESULTS);
        validator = actor(unit.getId(), Constants.ROLE_VALIDATION);
        outsider = actor(fixtures.createLabUnit().getId(), Constants.ROLE_VALIDATION);
        source = fixtures.createSampleWithSampleItem("M5");
        source.setTypeOfSample(fixtures.createTypeOfSample());
        sampleItems.update(source);
        isolateType = fixtures.createTypeOfSample();
        isolateType.setMicrobiologyIsolate(true);
        sampleTypes.update(isolateType);
        c = new MicroCase();
        c.setSampleId(source.getSample().getId());
        c.setSampleItemId(source.getId());
        c.setSampleTypeId(source.getTypeOfSampleId());
        c.setLabUnitId(unit.getId());
        c.setCreatedBy(writer);
        cases.insert(c);
        membership.addSample(c.getId(), source.getId(), writer);
        method = fixtures.createMethodId();
        organismA = fixtures.createOrganism("M5 organism A " + UUID.randomUUID(), "M5", null).getId();
        organismB = fixtures.createOrganism("M5 organism B " + UUID.randomUUID(), "M5", null).getId();
        em.flush();
    }

    private String actor(String unit, String role) {
        var u = new SystemUser();
        u.setLoginName("M5" + UUID.randomUUID().toString().substring(0, 12));
        u.setFirstName("Isolate" + UUID.randomUUID().toString().substring(0, 8));
        u.setLastName("Operator");
        u.setIsActive("Y");
        u.setIsEmployee("Y");
        u.setSysUserId("1");
        String id = users.insert(u);
        var a = new org.openelisglobal.userrole.valueholder.UserLabUnitRoles();
        a.setId(Integer.valueOf(id));
        var map = new org.openelisglobal.userrole.valueholder.LabUnitRoleMap();
        map.setLabUnit(unit);
        map.setRoles(Set.of(roles.getRoleByName(role).getId()));
        a.setLabUnitRoleMap(Set.of(map));
        em.persist(a);
        return id;
    }

    private MicroCaseInoculation culture(String outcome) {
        var a = new MicroCaseActivity();
        a.setCaseId(c.getId());
        a.setActivityType("CULTURE_INOCULATED");
        a.setPerformedBy(writer);
        activities.insert(a);
        var row = new MicroCaseInoculation();
        row.setCaseId(c.getId());
        row.setActivityId(a.getId());
        row.setSourceSampleItemId(source.getId());
        row.setContainerIdentifier("M5-" + UUID.randomUUID());
        row.setMedia("Stored culture");
        row.setOutcome(outcome);
        row.setPerformedBy(writer);
        cultures.insert(row);
        return row;
    }

    private String pick() {
        var r = new MicroIsolateWorkspaceRequestForm();
        r.cultureId = culture("GROWTH").getId();
        r.gramStain = "Gram negative";
        r.colonyMorphology = "Smooth colonies";
        r.significance = "COLONIZER";
        return workspace.pick(c.getId(), r, writer).isolates.getLast().id();
    }

    private MicroIsolateWorkspaceForm reload() {
        em.flush();
        em.clear();
        return workspace.get(c.getId(), writer);
    }

    private MicroIsolateWorkspaceRequestForm identification(String id, String organism) {
        var r = new MicroIsolateWorkspaceRequestForm();
        r.version = workspace.get(c.getId(), writer).isolates.stream().filter(i -> i.id().equals(id)).findFirst()
                .orElseThrow().version();
        r.organismId = organism;
        r.identificationStatus = "FINAL";
        r.identificationMethod = method;
        r.significance = "UNKNOWN";
        r.identifiedAt = Timestamp
                .from(Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        r.confidence = new BigDecimal("97.50");
        r.reason = "Confirmatory identification";
        return r;
    }

    @Test
    public void picksNumberedIsolatesWithOriginAndNeverReusesCancelledLabels() {
        String first = pick();
        var stored = isolates.get(first).orElseThrow();
        stored.setCancelledAt(Timestamp.from(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)));
        isolates.update(stored);
        String second = pick();
        var form = reload();
        assertEquals(1, form.isolates.size());
        var i = form.isolates.getFirst();
        assertEquals(second, i.id());
        assertEquals("ISO-2", i.label());
        assertEquals(source.getId(), i.sourceSampleItemId());
        assertEquals("COLONIZER", i.significance());
        assertEquals(writer, i.createdBy());
        assertEquals("Smooth colonies", i.colonyMorphology());
        assertNull(i.sampleItemId());
        assertNotNull(i.cultureId());
    }

    @Test
    public void requiresFormalGrowthOutcome() {
        for (String outcome : Arrays.asList(null, "NO_GROWTH", "CONTAMINATED")) {
            var r = new MicroIsolateWorkspaceRequestForm();
            r.cultureId = culture(outcome).getId();
            assertThrows(IllegalArgumentException.class, () -> workspace.pick(c.getId(), r, writer));
        }
        assertTrue(reload().isolates.isEmpty());
    }

    @Test
    public void createsExactlyOneChildSampleUnderTheOriginalAccession() {
        String id = pick();
        var r = new MicroIsolateWorkspaceRequestForm();
        r.sampleTypeId = isolateType.getId();
        r.purpose = "LABEL";
        workspace.act(c.getId(), id, "sample-item", r, writer);
        String child = reload().isolates.getFirst().sampleItemId();
        r.purpose = "TESTING";
        workspace.act(c.getId(), id, "sample-item", r, writer);
        assertEquals(child, reload().isolates.getFirst().sampleItemId());
        var item = sampleItems.get(child);
        assertEquals(source.getId(), item.getParentSampleItem().getId());
        assertEquals(source.getSample().getId(), item.getSample().getId());
        assertEquals(source.getSample().getAccessionNumber(), item.getSample().getAccessionNumber());
        assertEquals(isolateType.getId(), item.getTypeOfSampleId());
        assertEquals(2, sampleItems.getSampleItemsBySampleId(source.getSample().getId()).size());
        assertTrue(cultures.getSources(c.getId()).stream().anyMatch(s -> child.equals(s.getId())));
    }

    @Test
    public void appendsIdentificationsWithOriginalUserAndTime() {
        String id = pick();
        var initial = identification(id, organismA);
        initial.identificationStatus = "PRELIMINARY";
        workspace.act(c.getId(), id, "identify", initial, writer);
        reload();
        var change = identification(id, organismB);
        workspace.act(c.getId(), id, "reidentify", change, validator);
        var i = reload().isolates.getFirst();
        assertEquals(organismB, i.organismId());
        assertEquals("FINAL", i.status());
        assertEquals(2, i.history().size());
        var first = i.history().getFirst();
        assertEquals(organismA, first.organismId());
        assertEquals(writer, first.by());
        assertEquals(initial.identifiedAt, first.identifiedAt());
        assertEquals(validator, i.history().getLast().by());
        assertEquals("Confirmatory identification", i.history().getLast().reason());
    }

    @Test
    public void rejectsStaleIdentificationWithoutAppendingHistory() {
        String id = pick();
        var stale = identification(id, organismB);
        workspace.act(c.getId(), id, "identify", identification(id, organismA), writer);
        reload();
        assertThrows(ResponseStatusException.class, () -> workspace.act(c.getId(), id, "reidentify", stale, validator));
        assertEquals(1, reload().isolates.getFirst().history().size());
    }

    @Test
    public void requiresCurrentUnitValidatorAndReasonForReidentification() {
        String id = pick();
        workspace.act(c.getId(), id, "identify", identification(id, organismA), writer);
        reload();
        var r = identification(id, organismB);
        assertFalse(
                "Results-only user unexpectedly has Validation: "
                        + access.permittedLabUnitIds(writer, Constants.ROLE_VALIDATION) + " stored roles="
                        + em.find(org.openelisglobal.userrole.valueholder.UserLabUnitRoles.class,
                                Integer.valueOf(writer)).getLabUnitRoleMap().stream().map(m -> m.getRoles().toString())
                                .toList()
                        + " validator role=" + roles.getRoleByName(Constants.ROLE_VALIDATION).getId(),
                access.hasLabUnitRole(writer, c.getLabUnitId(), Constants.ROLE_VALIDATION));
        assertThrows(AccessDeniedException.class, () -> workspace.act(c.getId(), id, "reidentify", r, writer));
        assertThrows(AccessDeniedException.class, () -> workspace.act(c.getId(), id, "reidentify", r, outsider));
        r.reason = " ";
        assertThrows(IllegalArgumentException.class, () -> workspace.act(c.getId(), id, "reidentify", r, validator));
        assertEquals(organismA, reload().isolates.getFirst().organismId());
    }

    @Test
    public void blocksChildSamplesOwnedByAnotherCase() {
        var child = new SampleItem();
        child.setSample(source.getSample());
        child.setParentSampleItem(source);
        child.setTypeOfSample(source.getTypeOfSample());
        child.setSortOrder("2");
        child.setStatusId(source.getStatusId());
        child.setSysUserId(writer);
        sampleItems.insert(child);
        var other = new MicroCase();
        other.setSampleItemId(child.getId());
        other.setSampleId(source.getSample().getId());
        other.setSampleTypeId(source.getTypeOfSampleId());
        other.setLabUnitId(c.getLabUnitId());
        cases.insert(other);
        membership.addSample(other.getId(), child.getId(), writer);
        var row = culture("GROWTH");
        row.setSourceSampleItemId(child.getId());
        cultures.update(row);
        em.flush();
        em.clear();
        var r = new MicroIsolateWorkspaceRequestForm();
        r.cultureId = row.getId();
        assertEquals("MICROBIOLOGY_ISOLATE_SOURCE",
                assertThrows(IllegalArgumentException.class, () -> workspace.pick(c.getId(), r, writer)).getMessage());
        r.sampleItemId = child.getId();
        assertEquals("MICROBIOLOGY_ISOLATE_SOURCE",
                assertThrows(IllegalArgumentException.class, () -> workspace.refer(c.getId(), r, writer)).getMessage());
        assertTrue(reload().isolates.isEmpty());
    }

    @Test
    public void automaticallyReceivesOneIsolateAndRetainsSenderIdentificationSeparately() {
        source = sampleItems.get(source.getId());
        source.setTypeOfSample(isolateType);
        sampleItems.update(source);
        receivedService.receive(c.getId(), source, writer);
        receivedService.receive(c.getId(), source, writer);
        var i = reload().isolates.getFirst();
        assertEquals("ISO-1", i.label());
        assertTrue(i.received());
        assertEquals(source.getId(), i.sampleItemId());
        assertEquals(1, cultures.getRows(c.getId()).size());
        assertEquals("GROWTH", cultures.getRows(c.getId()).getFirst().getOutcome());
        var facility = new org.openelisglobal.organization.valueholder.Organization();
        facility.setOrganizationName("M5 sender " + UUID.randomUUID());
        facility.setIsActive("Y");
        facility.setMlsSentinelLabFlag("N");
        facility.setSysUserId(writer);
        organizations.insert(facility);
        var origin = new MicroIsolateWorkspaceRequestForm();
        origin.sendingOrganizationId = facility.getId();
        origin.reportedOrganismId = organismA;
        workspace.act(c.getId(), i.id(), "received-origin", origin, writer);
        reload();
        workspace.act(c.getId(), i.id(), "identify", identification(i.id(), organismB), writer);
        i = reload().isolates.getFirst();
        assertEquals(organismA, i.reportedOrganismId());
        assertEquals(organismB, i.organismId());
        assertTrue(i.mismatch());
        assertTrue(readiness.getReadiness(c.getId()).blockers.contains("REPORTED_IDENTIFICATION_MISMATCH"));
        var ack = identification(i.id(), organismB);
        ack.reason = "Sender result reviewed";
        final String id = i.id();
        assertThrows(AccessDeniedException.class, () -> workspace.act(c.getId(), id, "acknowledge", ack, writer));
        workspace.act(c.getId(), id, "acknowledge", ack, validator);
        assertEquals(validator, reload().isolates.getFirst().mismatchAcknowledgedBy());
        assertFalse(readiness.getReadiness(c.getId()).blockers.contains("REPORTED_IDENTIFICATION_MISMATCH"));
        workspace.act(c.getId(), id, "reidentify", identification(id, organismB), validator);
        assertNull(reload().isolates.getFirst().mismatchAcknowledgedAt());
    }

    private void release() {
        reports.recordInitialFinal(c.getId(), new MicroReportProjectionResult("Final identification", false, List.of()),
                validator);
        var current = cases.get(c.getId()).orElseThrow();
        current.setStage("FINAL_RELEASED");
        current.setFinalReleaseState("FINAL_RELEASED");
        cases.update(current);
        em.flush();
    }

    @Test
    public void requiresAmendmentAfterFinalReleaseAndRestoresEveryIdentificationField() {
        String id = pick();
        workspace.act(c.getId(), id, "identify", identification(id, organismA), writer);
        reload();
        Timestamp initial = isolates.get(id).orElseThrow().getIdentifiedAt();
        release();
        var r = identification(id, organismB);
        assertThrows(MicroCaseLockedException.class, () -> workspace.act(c.getId(), id, "reidentify", r, validator));
        var amendment = amendments.openAmendment(c.getId(), "Correct identification", validator);
        workspace.act(c.getId(), id, "reidentify", identification(id, organismB), validator);
        reload();
        workspace.act(c.getId(), id, "reidentify", identification(id, organismA), validator);
        reload();
        var changes = history.getByAmendmentId(amendment.getId());
        var same = Timestamp.from(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        for (var event : changes) {
            event.setChangedAt(same);
            history.update(event);
        }
        em.flush();
        em.clear();
        amendments.cancelAmendment(c.getId(), "Cancelled correction", validator);
        var i = reload().isolates.getFirst();
        assertEquals(organismA, i.organismId());
        assertEquals(initial, i.identifiedAt());
        assertEquals(5, i.history().size());
        assertEquals("FINAL_RELEASED", cases.get(c.getId()).orElseThrow().getStage());
        assertEquals(organismB, i.history().get(1).organismId());
    }

    @Test
    public void receivingDuringAmendmentPreservesStageAndCancelsTheNewIsolate() {
        release();
        var amendment = amendments.openAmendment(c.getId(), "Incoming isolate", validator);
        source = sampleItems.get(source.getId());
        source.setTypeOfSample(isolateType);
        sampleItems.update(source);
        receivedService.receive(c.getId(), source, writer);
        var id = reload().isolates.getFirst().id();
        assertEquals(amendment.getId(), isolates.get(id).orElseThrow().getAmendmentId());
        assertEquals("AMENDED", cases.get(c.getId()).orElseThrow().getStage());
        amendments.cancelAmendment(c.getId(), "Withdraw incoming isolate", validator);
        assertTrue(reload().isolates.isEmpty());
    }

    @Test
    public void eagerlyFetchesOriginAndHistoryForDetachedWorkspace() {
        String id = pick();
        workspace.act(c.getId(), id, "identify", identification(id, organismA), writer);
        em.flush();
        em.clear();
        var i = isolates.getWorkspace(c.getId()).getFirst();
        em.detach(i);
        assertTrue(Hibernate.isInitialized(i.getSourceCulture()));
        assertTrue(Hibernate.isInitialized(i.getIdentificationHistory()));
        assertEquals(1, i.getIdentificationHistory().size());
        assertNotNull(i.getSourceCulture().getContainerIdentifier());
    }

    @Test
    public void sameTestOnTwoIsolatesProducesTwoCorrectlyLinkedReferrals() {
        String first = pick(), second = pick();
        var test = fixtures.createCatalogTest();
        test.setTestSection(org.openelisglobal.spring.util.SpringContext
                .getBean(org.openelisglobal.test.service.TestSectionService.class).get(c.getLabUnitId()));
        tests.update(test);
        var mapping = new org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest();
        mapping.setTypeOfSampleId(isolateType.getId());
        mapping.setTestId(test.getId());
        mapping.setSysUserId(writer);
        sampleTests.insert(mapping);
        var facility = new org.openelisglobal.organization.valueholder.Organization();
        facility.setOrganizationName("M5 reference " + UUID.randomUUID());
        facility.setIsActive("Y");
        facility.setMlsSentinelLabFlag("N");
        facility.setSysUserId(writer);
        organizations.insert(facility);
        var reason = new org.openelisglobal.referral.valueholder.ReferralReason();
        reason.setName("M5 " + UUID.randomUUID().toString().substring(0, 12));
        reason.setSysUserId(writer);
        reasons.insert(reason);
        for (String id : List.of(first, second)) {
            var r = new MicroIsolateWorkspaceRequestForm();
            r.sampleTypeId = isolateType.getId();
            r.testId = test.getId();
            r.referral = new ReferralItem();
            r.referral.setReferredInstituteId(facility.getId());
            r.referral.setReferralReasonId(reason.getId());
            r.referral.setReferredSendDate(org.openelisglobal.common.util.DateUtil.getCurrentDateAsText());
            r.referral.setSubcontractNotes("Transport at room temperature");
            workspace.act(c.getId(), id, "referral", r, writer);
            reload();
        }
        var form = reload();
        assertTrue(form.referred);
        assertEquals(2, form.referrals.size());
        Set<String> childIds = new HashSet<>(
                form.isolates.stream().map(MicroIsolateWorkspaceForm.Isolate::sampleItemId).toList());
        var linked = referrals.getWorkspaceReferrals(new ArrayList<>(childIds));
        assertEquals(2, linked.size());
        assertEquals(childIds,
                new HashSet<>(linked.stream().map(f -> f.getAnalysis().getSampleItem().getId()).toList()));
        assertEquals(2, linked.stream().map(f -> f.getAnalysis().getId()).distinct().count());
        for (var ref : linked) {
            ref.setStatus(org.openelisglobal.referral.valueholder.ReferralStatus.COMPLETED);
            referrals.update(ref);
        }
        assertFalse(reload().referred);
    }

    @Test
    public void reidentificationLeavesReviewedSusceptibilityReadingsAndSelectionUntouched() {
        String id = pick();
        var ref = fixtures.createReferenceData(method);
        workspace.act(c.getId(), id, "identify", identification(id, ref.organism().getId()), writer);
        var run = ast.startRun(id, ref.panel().getId(), ref.standard().getId(), writer);
        ast.recordReading(run.getId(), ref.antibiotic().getId(), MicroAstMethod.MIC, new BigDecimal("4"), writer);
        ast.reviewRun(run.getId(), validator);
        reload();
        var before = ast.getReadingsForRun(run.getId()).getFirst();
        String readingId = before.getId(), interpretation = before.getInterpretation();
        BigDecimal raw = before.getRawValue();
        boolean selected = ast.getRunsForIsolate(id).getFirst().isReportable();
        workspace.act(c.getId(), id, "reidentify", identification(id, organismB), validator);
        reload();
        var after = ast.getReadingsForRun(run.getId()).getFirst();
        assertEquals(readingId, after.getId());
        assertEquals(interpretation, after.getInterpretation());
        assertEquals(raw, after.getRawValue());
        var stored = ast.getRunsForIsolate(id).getFirst();
        assertEquals(run.getId(), stored.getId());
        assertEquals("REVIEWED", stored.getStatus());
        assertEquals(selected, stored.isReportable());
    }

    @Test
    public void routingMultipleAnalysesAndRetriesReceivesExactlyOneIsolate() {
        source = sampleItems.get(source.getId());
        source.setTypeOfSample(isolateType);
        sampleItems.update(source);
        for (int n = 0; n < 2; n++) {
            var test = fixtures.createCatalogCultureTest(method);
            test.setTestSection(units.get(c.getLabUnitId()));
            test.setOpensMicrobiologyCase(true);
            test.setMicrobiologyCaseRole("CULTURE");
            tests.update(test);
            var a = fixtures.createAnalysis(source, test);
            routing.routeAnalysis(a, writer);
            routing.routeAnalysis(a, writer);
        }
        var form = reload();
        assertEquals(1, form.isolates.size());
        assertEquals("ISO-1", form.isolates.getFirst().label());
        assertEquals(source.getId(), form.isolates.getFirst().sampleItemId());
        assertEquals(1, cultures.getRows(c.getId()).size());
        assertEquals("RECEIVED_ISOLATE", cultures.getRows(c.getId()).getFirst().getPositiveSource());
    }

    @Test
    public void cancelsTimestampTiesInCausalOrderRatherThanRandomIdentifierOrder() {
        String id = pick();
        workspace.act(c.getId(), id, "identify", identification(id, organismA), writer);
        reload();
        var original = isolates.get(id).orElseThrow();
        Timestamp baseline = original.getIdentifiedAt();
        Timestamp acknowledged = Timestamp
                .from(Instant.now().minusSeconds(10).truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        original.setMismatchAcknowledgedAt(acknowledged);
        original.setMismatchAcknowledgedBy(validator);
        isolates.update(original);
        release();
        var amendment = amendments.openAmendment(c.getId(), "Correct organism", validator);
        String organismC = fixtures.createOrganism("M5 organism C " + UUID.randomUUID(), "M5", null).getId();
        Timestamp tie = Timestamp.from(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        var first = restorationEvent(id, amendment.getId(), "ffffffff-ffff-ffff-ffff-ffffffffffff", 2, organismA,
                organismB, tie);
        first.setPreviousIdentifiedAt(baseline);
        first.setPreviousMismatchAcknowledgedBy(validator);
        first.setPreviousMismatchAcknowledgedAt(acknowledged);
        history.insert(first);
        var second = restorationEvent(id, amendment.getId(), "00000000-0000-0000-0000-000000000000", 3, organismB,
                organismC, tie);
        second.setPreviousIdentifiedAt(tie);
        history.insert(second);
        var current = isolates.get(id).orElseThrow();
        current.setOrganismId(organismC);
        current.setIdentifiedAt(tie);
        current.setMismatchAcknowledgedBy(null);
        current.setMismatchAcknowledgedAt(null);
        isolates.update(current);
        reload();
        amendments.cancelAmendment(c.getId(), "Cancel both corrections", validator);
        var restored = reload().isolates.getFirst();
        assertEquals(organismA, restored.organismId());
        assertEquals(baseline, restored.identifiedAt());
        assertEquals(validator, restored.mismatchAcknowledgedBy());
        assertEquals(acknowledged, restored.mismatchAcknowledgedAt());
        assertEquals(organismB, history.get(first.getId()).orElseThrow().getNewOrganismId());
        assertEquals(organismC, history.get(second.getId()).orElseThrow().getNewOrganismId());
    }

    @Test
    public void cancellingReceivedIdentificationAmendmentRestoresAcknowledgmentAndAllFields() {
        source = sampleItems.get(source.getId());
        source.setTypeOfSample(isolateType);
        sampleItems.update(source);
        receivedService.receive(c.getId(), source, writer);
        String id = reload().isolates.getFirst().id();
        var facility = new org.openelisglobal.organization.valueholder.Organization();
        facility.setOrganizationName("M5 amendment sender " + UUID.randomUUID());
        facility.setIsActive("Y");
        facility.setMlsSentinelLabFlag("N");
        facility.setSysUserId(writer);
        organizations.insert(facility);
        var origin = new MicroIsolateWorkspaceRequestForm();
        origin.sendingOrganizationId = facility.getId();
        origin.reportedOrganismId = organismA;
        workspace.act(c.getId(), id, "received-origin", origin, writer);
        reload();
        var initial = identification(id, organismB);
        initial.identificationStatus = "PRELIMINARY";
        initial.significance = "COLONIZER";
        workspace.act(c.getId(), id, "identify", initial, writer);
        reload();
        var ack = identification(id, organismB);
        ack.reason = "Reviewed sender mismatch";
        workspace.act(c.getId(), id, "acknowledge", ack, validator);
        var baseline = reload().isolates.getFirst();
        assertNotNull(baseline.mismatchAcknowledgedAt());
        release();
        amendments.openAmendment(c.getId(), "Confirm organism", validator);
        var change = identification(id, organismA);
        change.identificationMethod = fixtures.createMethodId();
        change.confidence = new BigDecimal("99");
        change.significance = "CONTAMINANT";
        workspace.act(c.getId(), id, "reidentify", change, validator);
        reload();
        amendments.cancelAmendment(c.getId(), "Withdraw confirmation", validator);
        var restored = reload().isolates.getFirst();
        assertEquals(baseline.organismId(), restored.organismId());
        assertEquals(baseline.status(), restored.status());
        assertEquals(baseline.method(), restored.method());
        assertEquals(baseline.confidence(), restored.confidence());
        assertEquals(baseline.significance(), restored.significance());
        assertEquals(baseline.identifiedAt(), restored.identifiedAt());
        assertEquals(baseline.mismatchAcknowledgedBy(), restored.mismatchAcknowledgedBy());
        assertEquals(baseline.mismatchAcknowledgedAt(), restored.mismatchAcknowledgedAt());
        assertFalse(readiness.getReadiness(c.getId()).blockers.contains("REPORTED_IDENTIFICATION_MISMATCH"));
    }

    private MicroIsolateIdentificationEvent restorationEvent(String isolateId, String amendmentId, String id,
            long sequence, String before, String after, Timestamp at) {
        var e = new MicroIsolateIdentificationEvent();
        e.setId(id);
        e.setIsolateId(isolateId);
        e.setAmendmentId(amendmentId);
        e.setEventSequence(sequence);
        e.setEventType("REIDENTIFIED");
        e.setPreviousOrganismId(before);
        e.setNewOrganismId(after);
        e.setPreviousSignificance("UNKNOWN");
        e.setNewSignificance("UNKNOWN");
        e.setPreviousIdentificationStatus("CONFIRMED");
        e.setNewIdentificationStatus("CONFIRMED");
        e.setIdentifiedAt(at);
        e.setChangedAt(at);
        e.setChangedBy(validator);
        e.setReason("Correction fixture");
        return e;
    }
}
