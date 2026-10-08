package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.service.MicroCaseMembershipService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional ownership behavior on the shared application test context.
 * Registered-schema upgrade and rollback are independently exercised by
 * MicrobiologyV2CaseStructureLiquibaseRollbackTest without Hibernate DDL.
 */
@Transactional
public class MicroCaseMembershipIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroCaseRequestDAO requests;
    @Autowired
    private MicroCaseSampleDAO members;
    @Autowired
    private MicroCaseSplitDAO splits;
    @Autowired
    private MicroCaseAnalysisDAO links;
    @Autowired
    private MicroCaseMembershipService membership;
    @Autowired
    private MicroCaseService caseService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TestSectionService testSections;
    @PersistenceContext
    private EntityManager em;
    private SampleItem sample;
    private SampleTypeRequest request;
    private MicroCase microCase;
    private org.openelisglobal.test.valueholder.Test test;
    private String actor;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        actor = fixtures.defaultUserId();
        sample = fixtures.createSampleWithSampleItem("V2-MEMBERS");
        TypeOfSample type = fixtures.getOrCreateActiveSampleType();
        sample.setTypeOfSample(type);
        sampleItemService.update(sample);
        test = fixtures.createCatalogCultureTest(fixtures.createMethodId());
        request = new SampleTypeRequest();
        request.setSample(sample.getSample());
        request.setTypeOfSample(type);
        request.setRequestedTests(test.getId());
        request.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        em.persist(request);
        em.flush();
        microCase = pendingCase();
    }

    private MicroCase pendingCase() {
        MicroCase c = new MicroCase();
        c.setSampleId(sample.getSample().getId());
        c.setSampleTypeId(sample.getTypeOfSample().getId());
        c.setLabUnitId(testSections.getAllActiveTestSections().getFirst().getId());
        c.setCreatedBy(actor);
        cases.insert(c);
        return c;
    }

    @Test
    public void requestedOwnershipRoundTripsWithoutPhysicalMembershipOrAnalysis() {
        long samplesBefore = em.createQuery("select count(s) from SampleItem s", Long.class).getSingleResult();
        MicroCaseRequest owned = membership.ownRequest(microCase.getId(), request.getId(), test.getId(),
                MicroCaseRole.DIRECT, false, actor);
        em.clear();
        MicroCaseRequest stored = requests.get(owned.getId()).orElseThrow();
        assertNull(stored.getSampleItemId());
        assertNull(stored.getAnalysisId());
        assertNull(cases.get(microCase.getId()).orElseThrow().getSampleItemId());
        assertEquals(Long.valueOf(samplesBefore),
                em.createQuery("select count(s) from SampleItem s", Long.class).getSingleResult());
        assertTrue(membership.getCaseSamples(microCase.getId()).isEmpty());
        assertEquals(1, membership.getCaseRequests(microCase.getId()).size());
    }

    @Test
    public void collectionRetriesKeepOneMemberAndOneAnalysisOwner() {
        MicroCaseRequest owned = membership.ownRequest(microCase.getId(), request.getId(), test.getId(),
                MicroCaseRole.DIRECT, false, actor);
        Analysis analysis = fixtures.createAnalysis(sample, test);
        membership.fulfillRequest(owned.getId(), sample.getId(), analysis.getId(), actor);
        membership.fulfillRequest(owned.getId(), sample.getId(), analysis.getId(), actor);
        em.clear();
        assertEquals(1, members.getByCaseId(microCase.getId()).size());
        assertEquals(1, links.getByCaseId(microCase.getId()).size());
        assertEquals(MicroCaseRole.DIRECT, links.getByCaseId(microCase.getId()).getFirst().getCaseRole());
        assertNull(sampleItemService.get(sample.getId()).getCollectionDate());
    }

    @Test
    public void cancelledOwnershipRemainsHistoryAndReorderingUsesNewAnalysis() {
        MicroCaseRequest first = membership.ownRequest(microCase.getId(), request.getId(), test.getId(),
                MicroCaseRole.DIRECT, false, actor);
        Analysis oldAnalysis = fixtures.createAnalysis(sample, test);
        membership.fulfillRequest(first.getId(), sample.getId(), oldAnalysis.getId(), actor);
        membership.cancelRequest(first.getId(), "Order changed", actor);
        MicroCaseRequest second = membership.ownRequest(microCase.getId(), request.getId(), test.getId(),
                MicroCaseRole.DIRECT, false, actor);
        assertNotEquals(first.getId(), second.getId());
        Analysis newAnalysis = fixtures.createAnalysis(sample, test);
        membership.fulfillRequest(second.getId(), sample.getId(), newAnalysis.getId(), actor);
        em.clear();
        assertEquals(2, requests.getByCaseId(microCase.getId()).size());
        assertEquals(1, links.getByCaseId(microCase.getId()).size());
        assertNull(links.getActiveByAnalysisId(oldAnalysis.getId()));
        assertEquals(newAnalysis.getId(), links.getByCaseId(microCase.getId()).getFirst().getAnalysisId());
        assertEquals("Order changed", requests.get(first.getId()).orElseThrow().getCancellationReason());
        assertNotNull(em.find(Analysis.class, oldAnalysis.getId()));
    }

    @Test
    public void relatedCasesRemainSeparateInSameLabUnitAndThroughSplitLineage() {
        MicroCase other = pendingCase();
        membership.addSample(microCase.getId(), sample.getId(), actor);
        MicroCaseSample moved = membership.addSample(other.getId(), sample.getId(), actor);
        assertEquals(List.of(other.getId()), membership.getRelatedCaseIds(microCase.getId()));
        moved.setSplitOutAt(new Timestamp(System.currentTimeMillis()));
        moved.setSplitOutBy(actor);
        moved.setSplitReason("Separate work");
        members.update(moved);
        assertTrue(membership.getRelatedCaseIds(microCase.getId()).isEmpty());
        MicroCaseSplit split = new MicroCaseSplit();
        split.setSourceCaseId(microCase.getId());
        split.setResultCaseId(other.getId());
        split.setSampleItemId(sample.getId());
        split.setOccurredAt(new Timestamp(System.currentTimeMillis()));
        split.setPerformedBy(actor);
        split.setReason("Separate work");
        splits.insert(split);
        em.clear();
        assertEquals(List.of(other.getId()), membership.getRelatedCaseIds(microCase.getId()));
        assertEquals(List.of(microCase.getId()), membership.getRelatedCaseIds(other.getId()));
        assertEquals(cases.get(microCase.getId()).orElseThrow().getLabUnitId(),
                cases.get(other.getId()).orElseThrow().getLabUnitId());
    }

    @Test
    public void sharedOrderAloneDoesNotMakeCasesRelated() {
        MicroCase other = pendingCase();
        assertTrue(membership.getRelatedCaseIds(microCase.getId()).isEmpty());
        assertNotEquals(other.getId(), microCase.getId());
    }

    @Test
    public void retainedCaseDetailStillCompilesFromStoredSpecimenAnchor() {
        MicroCase old = caseService.createOrGetCase(sample.getId(), test.getMethod().getId(), actor);
        old.setSampleId(null);
        old.setSampleTypeId(null);
        old.setLabUnitId(null);
        old.setProgramId(null);
        old.setStatus(null);
        cases.update(old);
        em.clear();
        MicroCaseDetailForm detail = caseService.getCaseDetail(old.getId());
        assertEquals(old.getId(), detail.id);
        assertEquals(sample.getId(), detail.sampleItemId);
        assertEquals(sample.getSample().getAccessionNumber(), detail.accessionNumber);
        assertFalse(detail.activities.isEmpty());
        assertNull(cases.get(old.getId()).orElseThrow().getLabUnitId());
    }
}
