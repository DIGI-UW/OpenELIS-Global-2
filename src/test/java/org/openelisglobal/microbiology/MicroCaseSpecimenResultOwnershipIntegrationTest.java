package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroCaseTimelineService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroOrderRoutingService;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCaseTestRole;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroCaseSpecimenResultOwnershipIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroOrderRoutingService routing;
    @Autowired
    private MicroCaseSpecimenDAO members;
    @Autowired
    private SampleItemService samples;
    @Autowired
    private ResultService results;
    @Autowired
    private MicroIsolateService isolates;
    @Autowired
    private MicroIsolateDAO isolateDAO;
    @Autowired
    private MicroCaseStateService stateService;
    @Autowired
    private MicroCaseActivityDAO activities;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCaseTimelineService timeline;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void orderedAnalysesAndAnotherMemberDoNotCreateResultsOnTheSelectedSample() {
        var own = groupedCase();
        assertFalse(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertFalse(members.hasRecordedResults(own.caseId(), own.second().getId()));
    }

    @Test
    public void ordinaryResultsOutsideTheCaseDoNotBlockItsNoResultSplit() {
        var own = groupedCase();
        // A shared ordinary test is deliberately outside micro case membership.
        var ordinary = fixtures.createAnalysis(own.first(), fixtures.createCatalogTest());
        Result result = new Result();
        result.setAnalysis(ordinary);
        result.setResultType("N");
        result.setValue("4.2");
        result.setIsReportable("N");
        result.setSysUserId(fixtures.defaultUserId());
        String resultId = results.insert(result);
        entityManager.flush();
        entityManager.clear();
        assertFalse(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertFalse(members.hasRecordedResults(own.caseId(), own.second().getId()));
        assertEquals("4.2", results.get(resultId).getValue());
        assertEquals("N", results.get(resultId).getIsReportable());
    }

    @Test
    public void unreportedMicroResultsStillBelongToTheirActualCaseMember() {
        var own = groupedCase();
        var result = recordResult(own.firstAnalysis(), "4.2");
        entityManager.flush();
        entityManager.clear();
        assertTrue(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertFalse(members.hasRecordedResults(own.caseId(), own.second().getId()));
        assertEquals("4.2", results.get(result.getId()).getValue());
        assertEquals("N", results.get(result.getId()).getIsReportable());
    }

    @Test
    public void anotherCasesResultOnTheSameSampleDoesNotBlockTheSelectedCase() {
        var own = groupedCase();
        var other = otherCase(own.first());
        var result = recordResult(other.analysis(), "8.4");
        entityManager.flush();
        entityManager.clear();
        assertFalse(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertTrue(members.hasRecordedResults(other.caseId(), own.first().getId()));
        assertEquals(other.analysis().getId(), results.get(result.getId()).getAnalysis().getId());
        assertEquals("8.4", results.get(result.getId()).getValue());
    }

    @Test
    public void anotherCasesIsolateOnTheSameSampleDoesNotBlockTheSelectedCase() {
        var own = groupedCase();
        var other = otherCase(own.first());
        var isolate = isolates.createIsolate(other.caseId(), own.first().getId(), "OTHER-ISO", "Gram negative rods",
                "Colonies", MicroIsolateSignificance.UNKNOWN, fixtures.defaultUserId());
        entityManager.flush();
        entityManager.clear();
        assertFalse(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertTrue(members.hasRecordedResults(other.caseId(), own.first().getId()));
        assertEquals(other.caseId(), isolateDAO.get(isolate.getId()).orElseThrow().getCaseId());
    }

    @Test
    public void anotherCasesCultureObservationOnTheSameSampleDoesNotBlockTheSelectedCase() {
        var own = groupedCase();
        var other = otherCase(own.first());
        stateService.advanceStage(other.caseId(), MicroCaseStage.SETUP_RECORDED, fixtures.defaultUserId(),
                "Other setup");
        stateService.advanceStage(other.caseId(), MicroCaseStage.INCUBATING, fixtures.defaultUserId(),
                "Other incubation");
        stateService.advanceStage(other.caseId(), MicroCaseStage.NO_GROWTH_READY, fixtures.defaultUserId(),
                "Other case no growth", List.of(), own.first().getId());
        entityManager.flush();
        entityManager.clear();
        assertFalse(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertTrue(members.hasRecordedResults(other.caseId(), own.first().getId()));
    }

    @Test
    public void recordedIsolateObservationsBelongOnlyToTheSourceSample() {
        var own = groupedCase();
        var isolate = isolates.createIsolate(own.caseId(), own.first().getId(), "ISO-1", "Gram negative rods",
                "Lactose fermenting", MicroIsolateSignificance.UNKNOWN, fixtures.defaultUserId());
        entityManager.flush();
        entityManager.clear();
        assertTrue(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertFalse(members.hasRecordedResults(own.caseId(), own.second().getId()));
        assertEquals(own.first().getId(), isolateDAO.get(isolate.getId()).orElseThrow().getSourceSampleItemId());
    }

    @Test
    public void cancellingIsolateWorkDoesNotEraseItsRecordedObservations() {
        var own = groupedCase();
        var isolate = isolates.createIsolate(own.caseId(), own.first().getId(), "ISO-1", "Gram negative rods",
                "Colonies", MicroIsolateSignificance.UNKNOWN, fixtures.defaultUserId());
        isolate.setCancelledAt(new Timestamp(System.currentTimeMillis()));
        isolateDAO.update(isolate);
        entityManager.flush();
        entityManager.clear();
        assertTrue(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertFalse(members.hasRecordedResults(own.caseId(), own.second().getId()));
        assertEquals("Gram negative rods", isolateDAO.get(isolate.getId()).orElseThrow().getGramStain());
    }

    @Test
    public void cultureObservationBelongsOnlyToItsExplicitSampleAfterReload() {
        var own = incubatingCase();
        stateService.advanceStage(own.caseId(), MicroCaseStage.NO_GROWTH_READY, fixtures.defaultUserId(),
                "No growth in second bottle", List.of(), own.second().getId());
        entityManager.flush();
        entityManager.clear();
        assertTrue(members.hasRecordedResults(own.caseId(), own.second().getId()));
        assertFalse(members.hasRecordedResults(own.caseId(), own.first().getId()));
        var recorded = activities.getByCaseId(own.caseId()).stream()
                .filter(activity -> "No growth in second bottle".equals(activity.getNote())).findFirst().orElseThrow();
        assertEquals(own.second().getId(), recorded.getResultSourceSampleItemId());
        assertEquals(fixtures.defaultUserId(), recorded.getPerformedBy());
        assertEquals(own.second().getId(),
                caseService.getCaseDetail(own.caseId()).activities.stream()
                        .filter(activity -> recorded.getId().equals(activity.id)).findFirst()
                        .orElseThrow().resultSourceSampleItemId);
        assertEquals(own.second().getId(),
                timeline.getTimeline(own.caseId()).stream().filter(activity -> recorded.getId().equals(activity.id))
                        .findFirst().orElseThrow().resultSourceSampleItemId);
    }

    @Test
    public void pendingCultureSetupIsNotARecordedResult() {
        var own = incubatingCase();
        entityManager.flush();
        entityManager.clear();
        assertFalse(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertFalse(members.hasRecordedResults(own.caseId(), own.second().getId()));
    }

    @Test
    public void caseOnlyCultureObservationCannotGuessTheSample() {
        var own = incubatingCase();
        assertThrows(IllegalArgumentException.class, () -> stateService.advanceStage(own.caseId(),
                MicroCaseStage.POSITIVE_SIGNAL, fixtures.defaultUserId(), "No sample chosen"));
        assertFalse(members.hasRecordedResults(own.caseId(), own.first().getId()));
        assertFalse(members.hasRecordedResults(own.caseId(), own.second().getId()));
    }

    @Test
    public void cultureObservationCannotUseAnotherCasesSample() {
        var own = incubatingCase();
        var foreign = fixtures.createSampleWithSampleItem("V2FOREIGNOBS");
        assertThrows(IllegalArgumentException.class,
                () -> stateService.advanceStage(own.caseId(), MicroCaseStage.POSITIVE_SIGNAL, fixtures.defaultUserId(),
                        "Foreign sample", List.of(), foreign.getId()));
        assertFalse(members.hasRecordedResults(own.caseId(), foreign.getId()));
    }

    private OwnCase incubatingCase() {
        var own = groupedCase();
        stateService.advanceStage(own.caseId(), MicroCaseStage.SETUP_RECORDED, fixtures.defaultUserId(),
                "Pending setup");
        stateService.advanceStage(own.caseId(), MicroCaseStage.INCUBATING, fixtures.defaultUserId(),
                "Pending incubation");
        return own;
    }

    private OwnCase groupedCase() {
        SampleItem first = fixtures.createSampleWithSampleItem("V2SPLITRESULT");
        SampleItem second = new SampleItem();
        second.setSample(first.getSample());
        second.setTypeOfSample(first.getTypeOfSample());
        second.setSortOrder("2");
        second.setStatusId(first.getStatusId());
        second.setSysUserId(fixtures.defaultUserId());
        second.setId(samples.insert(second));
        var test = fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, fixtures.createLabUnit());
        var firstAnalysis = fixtures.createAnalysis(first, test);
        var secondAnalysis = fixtures.createAnalysis(second, test);
        String firstCase = routing.routeAnalysesForSampleItem(first, List.of(firstAnalysis), fixtures.defaultUserId())
                .get(0).getId();
        String secondCase = routing
                .routeAnalysesForSampleItem(second, List.of(secondAnalysis), fixtures.defaultUserId()).get(0).getId();
        assertEquals(firstCase, secondCase);
        return new OwnCase(firstCase, first, second, firstAnalysis);
    }

    private Result recordResult(Analysis analysis, String value) {
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setResultType("N");
        result.setValue(value);
        result.setIsReportable("N");
        result.setSysUserId(fixtures.defaultUserId());
        result.setId(results.insert(result));
        return result;
    }

    private OtherCase otherCase(SampleItem sample) {
        var test = fixtures.createCatalogMicroTest(MicroCaseTestRole.DIRECT, fixtures.createLabUnit());
        var analysis = fixtures.createAnalysis(sample, test);
        String caseId = routing.routeAnalysesForSampleItem(sample, List.of(analysis), fixtures.defaultUserId()).get(0)
                .getId();
        return new OtherCase(caseId, analysis);
    }

    private record OwnCase(String caseId, SampleItem first, SampleItem second, Analysis firstAnalysis) {
    }

    private record OtherCase(String caseId, Analysis analysis) {
    }
}
