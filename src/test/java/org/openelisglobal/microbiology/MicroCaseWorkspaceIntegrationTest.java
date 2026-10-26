package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.systemuser.service.*;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroCaseWorkspaceIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroCaseActivityDAO activities;
    @Autowired
    private MicroCaseSearchDAO searchDAO;
    @Autowired
    private MicroCaseWorkspaceService workspace;
    @Autowired
    private MicroCaseMembershipService membership;
    @Autowired
    private MicroIsolateService isolates;
    @Autowired
    private SystemUserService systemUsers;
    @Autowired
    private UserService users;
    @Autowired
    private RoleService roles;
    @Autowired
    private TestService tests;
    @Autowired
    private org.openelisglobal.sampleitem.service.SampleItemService sampleItems;
    @PersistenceContext
    private EntityManager em;
    private String actor, viewer;
    private TestSection source, destination, other;
    private MicroCase own, hidden;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/system-user.xml");
        executeDataSetWithStateManagement("testdata/role.xml");
        actor = fixtures.defaultUserId();
        source = fixtures.createLabUnit();
        destination = fixtures.createLabUnit();
        other = fixtures.createLabUnit();
        SystemUser u = new SystemUser();
        u.setLoginName("m2-" + UUID.randomUUID().toString().substring(0, 12));
        u.setFirstName("M2");
        u.setLastName("Reader");
        u.setIsActive("Y");
        u.setIsEmployee("Y");
        u.setSysUserId(actor);
        viewer = systemUsers.insert(u);
        grant(source.getId());
        var sample = fixtures.createSampleWithSampleItem("M2-OWN");
        sample.setTypeOfSample(fixtures.getOrCreateActiveSampleType());
        sampleItems.update(sample);
        own = createCase(sample.getSample().getId(), sample.getTypeOfSample().getId(), source.getId());
        hidden = createCase(sample.getSample().getId(), sample.getTypeOfSample().getId(), other.getId());
        var t = fixtures.createCatalogCultureTest(fixtures.createMethodId());
        t.setTestSection(destination);
        t.setOpensMicrobiologyCase(true);
        tests.update(t);
        em.flush();
    }

    private void grant(String... ids) {
        grantRole(Constants.ROLE_RESULTS, ids);
    }

    private void grantRole(String role, String... ids) {
        var mapping = em.find(org.openelisglobal.userrole.valueholder.UserLabUnitRoles.class, Integer.valueOf(viewer));
        boolean fresh = mapping == null;
        if (fresh) {
            mapping = new org.openelisglobal.userrole.valueholder.UserLabUnitRoles();
            mapping.setId(Integer.valueOf(viewer));
        }
        Set<org.openelisglobal.userrole.valueholder.LabUnitRoleMap> assignments = new HashSet<>();
        for (String id : ids) {
            var m = new org.openelisglobal.userrole.valueholder.LabUnitRoleMap();
            m.setLabUnit(id);
            m.setRoles(Set.of(roles.getRoleByName(role).getId()));
            assignments.add(m);
        }
        mapping.setLabUnitRoleMap(assignments);
        if (fresh)
            em.persist(mapping);
        em.flush();
    }

    private MicroCase createCase(String sampleId, String typeId, String unitId) {
        MicroCase c = new MicroCase();
        c.setSampleId(sampleId);
        c.setSampleTypeId(typeId);
        c.setLabUnitId(unitId);
        c.setCreatedBy(actor);
        cases.insert(c);
        return c;
    }

    private MicroCaseSearchForm query() {
        var q = new MicroCaseSearchForm();
        q.accessionNumber = workspaceAccession();
        return q;
    }

    private String workspaceAccession() {
        return (String) searchDAO.getSummary(own.getId())[1];
    }

    private org.openelisglobal.login.valueholder.UserSessionData sessionActor() {
        var a = new org.openelisglobal.login.valueholder.UserSessionData();
        a.setSytemUserId(Integer.parseInt(viewer));
        return a;
    }

    @Test
    public void fullPatientNameSearchMatchesEitherOrderWithoutLeakingOtherUnits() {
        var patient = fixtures.createPatient("M2-NAME");
        var human = new org.openelisglobal.samplehuman.valueholder.SampleHuman();
        human.setSampleId(own.getSampleId());
        human.setPatientId(patient.getId());
        human.setSysUserId(actor);
        em.persist(human);
        em.flush();
        for (String name : List.of(patient.getPerson().getLastName() + " " + patient.getPerson().getFirstName(),
                patient.getPerson().getFirstName() + " " + patient.getPerson().getLastName())) {
            var q = new MicroCaseSearchForm();
            q.q = name;
            assertEquals(List.of(own.getId()), workspace.search(q, viewer).rows.stream().map(row -> row.id).toList());
        }
    }

    @Test
    public void accessionLookupRejectsBlanksAndPaginatesOnlyTheExactOrder() throws Exception {
        var second = createCase(own.getSampleId(), own.getSampleTypeId(), source.getId());
        em.flush();
        Set<String> found = new HashSet<>();
        for (int page = 1; page <= 2; page++) {
            var response = mockMvc
                    .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/rest/microbiology/cases/by-accession")
                            .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA,
                                    sessionActor())
                            .param("accessionNumber", workspaceAccession()).param("page", String.valueOf(page))
                            .param("pageSize", "1"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.total")
                            .value(2))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.rows.length()").value(1))
                    .andReturn().getResponse().getContentAsString();
            found.add(new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).at("/rows/0/id").asText());
        }
        assertEquals(Set.of(own.getId(), second.getId()), found);
        for (String accession : List.of("", "  "))
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/rest/microbiology/cases/by-accession")
                    .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, sessionActor())
                    .param("accessionNumber", accession)).andExpect(
                            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/rest/microbiology/cases/by-accession")
                .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, sessionActor())
                .param("accessionNumber", workspaceAccession()).param("page", "0"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void httpQueriesScopeResultsAndAllowReadOnlyDirectLinks() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/rest/microbiology/cases/search")
                .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, sessionActor())
                .param("accessionNumber", workspaceAccession()).param("pageSize", "1")
                .param("labUnitId", source.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.total").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.rows[0].id")
                        .value(own.getId()));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/rest/microbiology/cases/" + hidden.getId() + "/shell")
                .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, sessionActor()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.readOnlyAccess").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.canWrite")
                        .value(false))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.canValidate")
                        .value(false));
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/rest/microbiology/cases/search")
                        .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA,
                                sessionActor())
                        .param("from", "invalid"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void validationRightsAllowReadingButNotOrdinaryWrites() throws Exception {
        grantRole(Constants.ROLE_VALIDATION, source.getId());
        var shell = workspace.get(own.getId(), viewer);
        assertFalse(shell.canWrite);
        assertTrue(shell.canValidate);
        assertThrows(AccessDeniedException.class, () -> workspace.transfer(own.getId(), destination.getId(), viewer));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/rest/microbiology/cases/" + own.getId() + "/notes")
                .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, sessionActor())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"text\":\"unauthorized write\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    public void retainedIsolateWriteRechecksOwnershipAfterRequestPreflight() {
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setAttribute(MicroCaseWriteAccessGuard.REQUEST_SCOPE,
                new MicroCaseWriteAccessGuard.WriteScope(own.getId(), viewer, Constants.ROLE_RESULTS));
        assertTrue(workspace.get(own.getId(), viewer).canWrite);
        own.setLabUnitId(other.getId());
        cases.update(own);
        em.flush();
        org.springframework.web.context.request.RequestContextHolder
                .setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(request));
        try {
            assertThrows(AccessDeniedException.class, () -> isolates.createIsolate(own.getId(), "late write",
                    "positive", null, MicroIsolateSignificance.UNKNOWN, viewer));
            assertTrue(isolates.getIsolatesForCase(own.getId()).isEmpty());
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    public void childResourcesAllowDirectReadsButRejectOutOfUnitWrites() throws Exception {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setCaseId(hidden.getId());
        isolate.setIsolateLabel("M2-hidden");
        em.persist(isolate);
        em.flush();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/rest/microbiology/cases/" + hidden.getId() + "/isolates")
                .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, sessionActor()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.isolates[0].id").value(isolate.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.isolates[0].canIdentify").value(false))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.isolates[0].canReidentify").value(false));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/rest/microbiology/cases/" + hidden.getId() + "/isolates/" + isolate.getId() + "/identify")
                .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, sessionActor())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        assertNull(em.find(MicroIsolate.class, isolate.getId()).getOrganismId());
    }

    @Test
    public void transferCannotCrossDomainsOrMergeDestinationCases() {
        grant(source.getId(), destination.getId());
        MicroCase existing = createCase(own.getSampleId(), own.getSampleTypeId(), destination.getId());
        em.flush();
        workspace.transfer(own.getId(), destination.getId(), viewer);
        assertEquals(Set.of(own.getId(), existing.getId()),
                cases.getByOrder(own.getSampleId()).stream().filter(c -> destination.getId().equals(c.getLabUnitId()))
                        .map(MicroCase::getId).collect(java.util.stream.Collectors.toSet()));
        destination.setDomain("ENVIRONMENTAL");
        em.merge(destination);
        em.flush();
        assertThrows(IllegalArgumentException.class, () -> workspace.transfer(own.getId(), source.getId(), viewer));
    }

    @Test
    public void pendingRequestIsListedOnceForMultipleTests() {
        var req = new org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest();
        req.setSample(em.find(org.openelisglobal.sample.valueholder.Sample.class, own.getSampleId()));
        req.setTypeOfSample(
                em.find(org.openelisglobal.typeofsample.valueholder.TypeOfSample.class, own.getSampleTypeId()));
        req.setCreatedDate(java.sql.Timestamp.valueOf("2026-10-08 12:00:00"));
        em.persist(req);
        var t = fixtures.createCatalogCultureTest(fixtures.createMethodId());
        membership.ownRequest(own.getId(), req.getId(), t.getId(), MicroCaseRole.DIRECT, false, actor);
        var secondTest = fixtures.createCatalogCultureTest(fixtures.createMethodId());
        membership.ownRequest(own.getId(), req.getId(), secondTest.getId(), MicroCaseRole.DIRECT, false, actor);
        em.flush();
        var shell = workspace.get(own.getId(), viewer);
        assertEquals(1, shell.pendingSamples.size());
        assertEquals(String.valueOf(req.getId()), shell.pendingSamples.get(0).id);
        assertEquals(null, shell.pendingSamples.get(0).collectionDate);
        assertEquals(0, shell.samples.size());
    }

    @Test
    public void paginationAndCountsOnlyIncludePermittedCaseUnits() {
        MicroCase second = createCase(own.getSampleId(), own.getSampleTypeId(), source.getId());
        em.flush();
        var q = query();
        q.pageSize = 1;
        var first = workspace.search(q, viewer);
        q.page = 2;
        var next = workspace.search(q, viewer);
        assertEquals(2, first.total);
        assertEquals(1, first.rows.size());
        assertEquals(2, next.total);
        assertEquals(Set.of(own.getId(), second.getId()), Set.of(first.rows.get(0).id, next.rows.get(0).id));
    }

    @Test
    public void multipleUnitFiltersCannotBroadenPermissionsOrLoseUnitOptions() {
        var q = query();
        q.labUnitIds = List.of(source.getId(), other.getId());
        var filtered = workspace.search(q, viewer);
        assertEquals(List.of(own.getId()), filtered.rows.stream().map(r -> r.id).toList());
        assertFalse(filtered.labUnits.stream().anyMatch(u -> other.getId().equals(u.getId())));
        q.labUnitIds = List.of(other.getId());
        var inaccessible = workspace.search(q, viewer);
        assertEquals(0, inaccessible.total);
        assertEquals(0, inaccessible.rows.size());
        assertTrue(inaccessible.labUnits.stream().anyMatch(u -> source.getId().equals(u.getId())));
    }

    @Test
    public void directReadIsAllowedWhileExplicitUnitSearchRemainsScoped() {
        var direct = workspace.get(hidden.getId(), viewer);
        assertTrue(direct.readOnlyAccess);
        assertFalse(direct.canWrite);
        assertFalse(direct.canValidate);
        assertEquals(0, direct.transferLabUnits.size());
        assertThrows(AccessDeniedException.class, () -> workspace.get(hidden.getId(), null));
        var q = query();
        q.labUnitId = other.getId();
        assertThrows(AccessDeniedException.class, () -> workspace.search(q, viewer));
    }

    @Test
    public void statusIsWorkedOutFromReleasesWithAmendmentOpenAsATag() {
        assertStatus("IN_PROGRESS");
        own.setFinalReleaseState(MicroCaseFinalReleaseState.PRELIMINARY_RELEASED.name());
        assertStatus("PARTIAL");
        own.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        assertStatus("FINAL");
        var first = amendment(1, MicroCaseAmendmentStatus.OPEN);
        own.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        assertStatus("FINAL", "AMENDMENT_OPEN");
        first.setStatus(MicroCaseAmendmentStatus.RELEASED.name());
        own.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        assertStatus("AMENDED");
        amendment(2, MicroCaseAmendmentStatus.OPEN);
        own.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        assertStatus("AMENDED", "AMENDMENT_OPEN");
        own.setStatus(MicroCaseStatus.CANCELLED);
        assertEquals("CANCELLED", workspace.get(own.getId(), viewer).caseStatus);
        var q = query();
        q.caseStatus = "ACTIVE";
        assertThrows(IllegalArgumentException.class, () -> workspace.search(q, viewer));
    }

    @Test
    public void statOrderPriorityTagsTheCase() {
        assertFalse(workspace.get(own.getId(), viewer).stat);
        em.find(org.openelisglobal.sample.valueholder.Sample.class, own.getSampleId())
                .setPriority(org.openelisglobal.sample.valueholder.OrderPriority.STAT);
        em.flush();
        assertTrue(workspace.get(own.getId(), viewer).stat);
        assertTrue(workspace.search(query(), viewer).rows.get(0).stat);
    }

    private MicroCaseAmendment amendment(int sequence, MicroCaseAmendmentStatus status) {
        var a = new MicroCaseAmendment();
        a.setCaseId(own.getId());
        a.setSequenceNumber(sequence);
        a.setStatus(status.name());
        a.setReason("Corrected organism");
        a.setOpenedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        a.setOpenedBy(actor);
        em.persist(a);
        return a;
    }

    private void assertStatus(String status, String... tags) {
        em.flush();
        var shell = workspace.get(own.getId(), viewer);
        assertEquals(status, shell.caseStatus);
        assertEquals(List.of(tags), shell.statusTags);
        var row = workspace.search(query(), viewer).rows.get(0);
        assertEquals(status, row.caseStatus);
        assertEquals(List.of(tags), row.statusTags);
        for (String filter : List.of("IN_PROGRESS", "PARTIAL", "FINAL", "AMENDED", "AMENDMENT_OPEN")) {
            var q = query();
            q.caseStatus = filter;
            assertEquals(filter, filter.equals(status) || List.of(tags).contains(filter),
                    workspace.search(q, viewer).rows.stream().anyMatch(r -> r.id.equals(own.getId())));
        }
    }

    @Test
    public void blockedFinalReleaseAnswersConflictNamingWhatIsMissing() throws Exception {
        grantRole(Constants.ROLE_VALIDATION, source.getId());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/rest/microbiology/cases/" + own.getId() + "/release/final")
                .requestAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, sessionActor())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error")
                        .value("MICROBIOLOGY_RELEASE_BLOCKED"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.blockers",
                        org.hamcrest.CoreMatchers.hasItem("ISOLATE_REQUIRED")));
    }

    @Test
    public void pendingCaseHeaderLoadsBeforePhysicalSampleExists() {
        var shell = workspace.get(own.getId(), viewer);
        assertEquals(workspaceAccession(), shell.accessionNumber);
        assertEquals(source.getId(), shell.labUnitId);
        assertEquals(0, shell.samples.size());
        assertTrue(shell.canWrite);
    }

    @Test
    public void sameOrderAloneDoesNotCreateRelatedCases() {
        grant(source.getId(), other.getId());
        assertEquals(0, workspace.get(own.getId(), viewer).relatedCases.size());
    }

    @Test
    public void transferRequiresBothUnitsAndRetainsCaseIdentityAndAudit() {
        assertThrows(AccessDeniedException.class, () -> workspace.transfer(own.getId(), destination.getId(), viewer));
        grant(source.getId(), destination.getId());
        var moved = workspace.transfer(own.getId(), destination.getId(), viewer);
        assertEquals(own.getId(), moved.id);
        assertEquals(destination.getId(), moved.labUnitId);
        assertEquals(own.getSampleId(), cases.get(own.getId()).get().getSampleId());
        var audit = activities.getByCaseId(own.getId()).stream()
                .filter(a -> "CASE_TRANSFERRED".equals(a.getActivityType())).toList();
        assertEquals(1, audit.size());
        assertEquals(viewer, audit.get(0).getPerformedBy());
        assertTrue(audit.get(0).getStructuredData().contains(source.getId()));
        var q = query();
        q.labUnitId = source.getId();
        assertEquals(0, workspace.search(q, viewer).total);
        q.labUnitId = destination.getId();
        assertEquals(1, workspace.search(q, viewer).total);
    }

    @Test
    public void finalCaseCannotTransfer() {
        grant(source.getId(), destination.getId());
        own.setStage("FINAL_RELEASED");
        cases.update(own);
        em.flush();
        assertThrows(MicroCaseLockedException.class,
                () -> workspace.transfer(own.getId(), destination.getId(), viewer));
        assertEquals(source.getId(), cases.get(own.getId()).get().getLabUnitId());
    }

    @Test
    public void fourBottlesRetainTheirCollectionIdentityAfterCorrectionAndTransfer() {
        grant(source.getId(), destination.getId());
        var expected = new LinkedHashMap<String, java.sql.Timestamp>();
        for (int i = 0; i < 4; i++) {
            var bottle = fixtures.createSampleWithSampleItem("M9-BOTTLE-" + i);
            bottle.setSample(em.find(org.openelisglobal.sample.valueholder.Sample.class, own.getSampleId()));
            bottle.setTypeOfSample(fixtures.getOrCreateActiveSampleType());
            bottle.setBodySite(i < 2 ? "Left arm" : "Right arm");
            bottle.setContainer(i % 2 == 0 ? "Aerobic bottle" : "Anaerobic bottle");
            bottle.setCollector("Collector " + i);
            bottle.setCollectionDate(
                    java.sql.Timestamp.from(java.time.Instant.parse("2026-10-14T23:58:00Z").plusSeconds(i * 60)));
            sampleItems.update(bottle);
            membership.addSample(own.getId(), bottle.getId(), actor);
            expected.put(bottle.getId(), bottle.getCollectionDate());
        }
        em.flush();
        em.clear();
        var shell = workspace.get(own.getId(), viewer);
        assertEquals(4, shell.samples.size());
        assertEquals(Set.of("Left arm", "Right arm"),
                shell.samples.stream().map(row -> row.bodySite).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("Aerobic bottle", "Anaerobic bottle"),
                shell.samples.stream().map(row -> row.containerType).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("Collector 0", "Collector 1", "Collector 2", "Collector 3"),
                shell.samples.stream().map(row -> row.collector).collect(java.util.stream.Collectors.toSet()));
        shell.samples.forEach(row -> assertEquals(expected.get(row.sampleItemId), row.collectionDate));
        var corrected = sampleItems.get(expected.keySet().iterator().next());
        corrected.setBodySite("Left forearm");
        sampleItems.update(corrected);
        workspace.transfer(own.getId(), destination.getId(), viewer);
        em.flush();
        em.clear();
        var moved = workspace.get(own.getId(), viewer);
        assertEquals(destination.getId(), moved.labUnitId);
        assertEquals(4, moved.samples.size());
        assertEquals("Left forearm", moved.samples.stream().filter(row -> row.sampleItemId.equals(corrected.getId()))
                .findFirst().orElseThrow().bodySite);
        moved.samples.forEach(row -> assertEquals(expected.get(row.sampleItemId), row.collectionDate));
    }

    @Test
    public void sharedSamplesAppearWithRelatedCasesAcrossTransfers() {
        grant(source.getId(), destination.getId());
        var sample = fixtures.createSampleWithSampleItem("M2-MEMBER");
        sample.setTypeOfSample(fixtures.getOrCreateActiveSampleType());
        sampleItems.update(sample);
        MicroCase first = createCase(sample.getSample().getId(), sample.getTypeOfSample().getId(), source.getId());
        MicroCase second = createCase(sample.getSample().getId(), sample.getTypeOfSample().getId(), other.getId());
        membership.addSample(first.getId(), sample.getId(), actor);
        membership.addSample(second.getId(), sample.getId(), actor);
        em.flush();
        var shell = workspace.get(first.getId(), viewer);
        assertEquals(sample.getId(), shell.samples.get(0).sampleItemId);
        assertEquals(second.getId(), shell.relatedCases.get(0).id);
        workspace.transfer(first.getId(), destination.getId(), viewer);
        assertEquals(second.getId(), workspace.get(first.getId(), viewer).relatedCases.get(0).id);
    }
}
