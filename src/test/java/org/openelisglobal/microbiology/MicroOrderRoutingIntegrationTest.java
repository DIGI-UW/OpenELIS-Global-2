package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroCaseAnalysisService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroOrderDraftGrouping;
import org.openelisglobal.microbiology.service.MicroOrderRoutingService;
import org.openelisglobal.microbiology.valueholder.MicroCaseTestRole;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroOrderRoutingIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroOrderRoutingService routingService;
    @Autowired
    private MicroCaseService caseService;
    @Autowired
    private MicroCaseAnalysisService caseAnalysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private MicroCaseDAO caseDAO;
    @Autowired
    private SampleItemService sampleItemService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void draftGroupingMatchesSavedBottleAndSplitUnitMembershipWithoutWritingCases() {
        var first = fixtures.createSampleWithSampleItem("AMRV2PREVIEW");
        var second = new SampleItem();
        second.setSample(first.getSample());
        second.setTypeOfSample(fixtures.createTypeOfSample());
        second.setSortOrder("2");
        second.setStatusId(first.getStatusId());
        second.setSysUserId(fixtures.defaultUserId());
        sampleItemService.insert(second);
        var unit = fixtures.createLabUnit();
        var otherUnit = fixtures.createLabUnit();
        var sets = fixtures.createCatalogCultureTest(fixtures.createMethodId(), unit);
        sets.setCollectedInSets(true);
        testService.update(sets);
        var direct = fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, unit);
        var other = fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, otherUnit);
        var ordinary = fixtures.createCatalogTest();
        List<SampleItem> items = List.of(first, second);
        var testLists = List.of(List.of(direct, sets, ordinary), List.of(direct, sets, other));
        List<MicroOrderDraftGrouping.Selection> draft = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            SampleItem unsaved = new SampleItem();
            unsaved.setTypeOfSample(items.get(i).getTypeOfSample());
            draft.add(new MicroOrderDraftGrouping.Selection(unsaved, testLists.get(i)));
        }

        var preview = routingService.previewNewOrder(draft);
        assertEquals(2, preview.size());
        assertEquals(List.of(0, 1), preview.get(0).specimenIndexes());
        assertEquals(List.of(1), preview.get(1).specimenIndexes());
        assertEquals(List.of(sets.getId(), direct.getId()), preview.get(0).testIds());
        assertEquals(0, caseDAO.getByOrder(first.getSample().getId()).size());

        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            var analyses = testLists.get(i).stream().map(test -> fixtures.createAnalysis(item, test)).toList();
            routingService.routeAnalysesForSampleItem(item, analyses, fixtures.defaultUserId());
        }
        var saved = caseDAO.getByOrder(first.getSample().getId());
        assertEquals(preview.size(), saved.size());
        for (var group : preview) {
            var owner = saved.stream().filter(c -> c.getTestSectionId().equals(group.key().testSectionId())).findFirst()
                    .orElseThrow();
            Set<String> expectedMembers = group.specimenIndexes().stream().map(i -> items.get(i).getId())
                    .collect(Collectors.toSet());
            assertEquals(expectedMembers, Set.copyOf(caseService.getSpecimenIds(owner.getId())));
            assertEquals(owner.getTestSectionId().equals(unit.getId()) ? 4 : 1,
                    caseAnalysisService.getCaseAnalyses(owner.getId()).size());
        }
        var withoutOtherUnit = List.of(draft.get(0),
                new MicroOrderDraftGrouping.Selection(draft.get(1).specimen(), List.of(direct, sets)));
        assertEquals(1, routingService.previewNewOrder(withoutOtherUnit).size());
        assertEquals(2, caseDAO.getByOrder(first.getSample().getId()).size());
    }

    @Test
    public void ordinaryTestsOpenNoCaseAndMicroTestsGroupByCatalogLabUnit() {
        var item = fixtures.createSampleWithSampleItem("AMRV2GROUP");
        var firstUnit = fixtures.createLabUnit();
        var secondUnit = fixtures.createLabUnit();
        var ordinary = fixtures.createAnalysis(item, fixtures.createCatalogTest());
        var culture = fixtures.createAnalysis(item,
                fixtures.createCatalogCultureTest(fixtures.createMethodId(), firstUnit));
        var direct = fixtures.createAnalysis(item,
                fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, firstUnit));
        var otherUnit = fixtures.createAnalysis(item,
                fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, secondUnit));
        String actor = fixtures.defaultUserId();

        assertEquals(0, routingService.routeAnalysesForSampleItem(item, List.of(ordinary), actor).size());
        assertEquals(0, caseService.getSiblingCases(item.getId()).size());
        var first = routingService.routeAnalysesForSampleItem(item, List.of(culture, direct), actor);
        assertEquals(1, first.size());
        assertEquals(firstUnit.getId(), first.get(0).getTestSectionId());
        assertEquals(2, caseAnalysisService.getCaseAnalyses(first.get(0).getId()).size());
        var second = routingService.routeAnalysesForSampleItem(item, List.of(otherUnit), actor);
        assertEquals(secondUnit.getId(), second.get(0).getTestSectionId());
        assertEquals(2, caseService.getSiblingCases(item.getId()).size());
    }

    @Test
    public void persistedDirectOnlyTestOpensCaseWithExplicitMemberAndAnalysisOwner() {
        var item = fixtures.createSampleWithSampleItem("AMRV2DIRECT");
        var unit = fixtures.createLabUnit();
        var test = fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, unit);
        test.setAntimicrobialResistance(false);
        testService.update(test);
        var analysis = fixtures.createAnalysis(item, test);
        var routed = routingService.routeAnalysesForSampleItem(item, List.of(analysis), fixtures.defaultUserId());

        assertEquals(1, routed.size());
        var microCase = routed.get(0);
        assertEquals(item.getSample().getId(), microCase.getSampleId());
        assertEquals(item.getTypeOfSampleId(), microCase.getSampleTypeId());
        assertEquals(unit.getId(), microCase.getTestSectionId());
        assertEquals(List.of(item.getId()), caseService.getSpecimenIds(microCase.getId()));
        assertEquals(microCase.getId(), caseAnalysisService.getAnalysisLink(analysis.getId()).getCaseId());
        assertEquals("DIRECT", caseAnalysisService.getAnalysisLink(analysis.getId()).getCaseRole());
        assertNull(caseService.getCaseDetail(microCase.getId()).orderDetail);
    }

    @Test
    public void addingTestsUsesActualMemberCaseBeforeAnOlderGroupMatch() {
        var firstItem = fixtures.createSampleWithSampleItem("AMRV2MEMBER");
        var secondItem = new SampleItem();
        secondItem.setSample(firstItem.getSample());
        secondItem.setTypeOfSample(firstItem.getTypeOfSample());
        secondItem.setSortOrder("2");
        secondItem.setStatusId(firstItem.getStatusId());
        secondItem.setSysUserId(fixtures.defaultUserId());
        sampleItemService.insert(secondItem);
        var destinationUnit = fixtures.createLabUnit();
        var originalUnit = fixtures.createLabUnit();
        var firstAnalysis = fixtures.createAnalysis(firstItem,
                fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, destinationUnit));
        var secondAnalysis = fixtures.createAnalysis(secondItem,
                fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, originalUnit));
        var olderCase = routingService
                .routeAnalysesForSampleItem(firstItem, List.of(firstAnalysis), fixtures.defaultUserId()).get(0);
        var memberCase = routingService
                .routeAnalysesForSampleItem(secondItem, List.of(secondAnalysis), fixtures.defaultUserId()).get(0);
        // Model two separate cases now in the same unit. This is a routing
        // regression, not acceptance evidence for the transfer operation.
        olderCase.setCreatedAt(Timestamp.valueOf("2026-10-01 12:00:00"));
        caseDAO.update(olderCase);
        memberCase.setCreatedAt(Timestamp.valueOf("2026-10-02 12:00:00"));
        memberCase.setTestSectionId(destinationUnit.getId());
        caseDAO.update(memberCase);
        var added = fixtures.createAnalysis(secondItem,
                fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, destinationUnit));

        var routed = routingService.routeAnalysesForSampleItem(secondItem, List.of(added), fixtures.defaultUserId());

        assertEquals(memberCase.getId(), routed.get(0).getId());
        assertEquals(memberCase.getId(), caseAnalysisService.getAnalysisLink(added.getId()).getCaseId());
        assertEquals(memberCase.getId(), caseAnalysisService.getAnalysisLink(secondAnalysis.getId()).getCaseId());
        assertEquals(olderCase.getId(), caseAnalysisService.getAnalysisLink(firstAnalysis.getId()).getCaseId());
        assertEquals(List.of(firstItem.getId()), caseService.getSpecimenIds(olderCase.getId()));
        assertEquals(List.of(secondItem.getId()), caseService.getSpecimenIds(memberCase.getId()));
        assertEquals(2, caseDAO.getByOrder(firstItem.getSample().getId()).size());
    }

    @Test
    public void caseCompletionTestKeepsItsRoleWithoutCreatingACultureProtocol() {
        var item = fixtures.createSampleWithSampleItem("AMRV2CASE");
        var unit = fixtures.createLabUnit();
        var test = fixtures.createCatalogMicroTest(MicroCaseTestRole.CASE, unit);
        var analysis = fixtures.createAnalysis(item, test);
        var routed = routingService.routeAnalysesForSampleItem(item, List.of(analysis), fixtures.defaultUserId());

        assertEquals(1, routed.size());
        assertEquals("CASE", caseAnalysisService.getAnalysisLink(analysis.getId()).getCaseRole());
        assertEquals(List.of(item.getId()), caseService.getSpecimenIds(routed.get(0).getId()));
    }
}
