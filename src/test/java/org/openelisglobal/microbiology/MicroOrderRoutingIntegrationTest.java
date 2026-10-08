package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseRequestDAO;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroCaseMembershipService;
import org.openelisglobal.microbiology.service.MicroOrderRoutingService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroOrderRoutingIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired private MicrobiologyTestFixtures fixtures;
    @Autowired private SampleService orders;
    @Autowired private SampleItemService samples;
    @Autowired private TestService tests;
    @Autowired private TestSectionService units;
    @Autowired private MicroOrderRoutingService routing;
    @Autowired private MicroCaseDAO cases;
    @Autowired private MicroCaseRequestDAO requests;
    @Autowired private MicroCaseMembershipService membership;
    @PersistenceContext private EntityManager em;
    private Sample order;
    private org.openelisglobal.test.valueholder.Test test;
    private TypeOfSample type;
    private String actor;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        actor = fixtures.defaultUserId();
        order = new Sample();
        order.setAccessionNumber("V2-" + UUID.randomUUID().toString().substring(0, 12));
        order.setEnteredDate(Date.valueOf("2026-01-02"));
        order.setStatusId(fixtures.ensureSampleEnteredStatus());
        order.setSysUserId(actor);
        orders.insert(order);
        type = fixtures.getOrCreateActiveSampleType();
        test = em.find(org.openelisglobal.test.valueholder.Test.class, fixtures.createCatalogTest().getId());
        test.setTestSection(units.getAllActiveTestSections().getFirst());
        test.setOpensMicrobiologyCase(true);
        test.setMicrobiologyCaseRole("DIRECT");
        em.flush();
    }

    private SampleTypeRequest request(String ids) {
        SampleTypeRequest r = new SampleTypeRequest();
        r.setSample(order); r.setTypeOfSample(type); r.setRequestedTests(ids);
        r.setCreatedDate(Timestamp.valueOf("2026-01-02 10:00:00"));
        em.persist(r); em.flush(); return r;
    }

    @Test
    public void savingRequestedWorkOpensOneCaseWithoutInventingSamplesOrReceipt() {
        SampleTypeRequest r = request(test.getId());
        routing.routeOrder(order, actor);
        routing.routeOrder(order, actor);
        em.flush(); em.clear();
        assertEquals(1, cases.getByOrder(order.getId()).size());
        var owner = requests.getActiveByRequestAndTest(r.getId(), test.getId());
        assertNotNull(owner);
        assertNull(owner.getSampleItemId());
        assertTrue(samples.getSampleItemsBySampleId(order.getId()).isEmpty());
        assertNull(orders.get(order.getId()).getReceivedTimestamp());
        assertNull(orders.get(order.getId()).getCollectionDate());
    }

    @Test
    public void laterCollectionAttachesToTheRequestedCaseAndRetriesKeepOneOwner() {
        SampleTypeRequest r = request(test.getId());
        routing.routeOrder(order, actor);
        String caseId = requests.getActiveByRequestAndTest(r.getId(), test.getId()).getCaseId();
        SampleItem sample = new SampleItem();
        sample.setSample(order); sample.setTypeOfSample(type); sample.setSortOrder("1");
        sample.setStatusId(fixtures.ensureSampleEnteredStatus()); sample.setSysUserId(actor);
        samples.insert(sample);
        var analysis = fixtures.createAnalysis(sample, test);
        r.setSampleItem(sample); r.setStatus(SampleTypeRequest.Status.COLLECTED);
        routing.routeOrder(order, actor);
        routing.routeOrder(order, actor);
        em.flush(); em.clear();
        assertEquals(1, cases.getByOrder(order.getId()).size());
        var owner = requests.getActiveByRequestAndTest(r.getId(), test.getId());
        assertEquals(caseId, owner.getCaseId());
        assertEquals(analysis.getId(), owner.getAnalysisId());
        assertEquals(1, membership.getCaseSamples(caseId).size());
        assertEquals(sample.getId(), owner.getSampleItemId());
    }

    @Test
    public void anOrdinaryTestOpensNoCase() {
        test.setOpensMicrobiologyCase(false); em.flush();
        request(test.getId()); routing.routeOrder(order, actor);
        assertTrue(cases.getByOrder(order.getId()).isEmpty());
    }

    @Test
    public void aCaseRoleOpensAnEmptyCaseWithoutAnAnalysis() {
        test.setMicrobiologyCaseRole("CASE"); em.flush();
        SampleTypeRequest r = request(test.getId()); routing.routeOrder(order, actor);
        var owner = requests.getActiveByRequestAndTest(r.getId(), test.getId());
        assertNotNull(owner);
        assertNull(owner.getAnalysisId());
        assertTrue(membership.getCaseSamples(owner.getCaseId()).isEmpty());
    }

    @Test
    public void setCulturesShareOneCaseAcrossRequestedSampleTypes() {
        test.setMicrobiologyCaseRole("CULTURE"); test.setCollectedInSets(true); em.flush();
        var first = request(test.getId());
        var second = request(test.getId());
        second.setTypeOfSample(fixtures.createTypeOfSample());
        routing.routeOrder(order, actor);
        assertEquals(1, cases.getByOrder(order.getId()).size());
        assertEquals(requests.getActiveByRequestAndTest(first.getId(), test.getId()).getCaseId(),
                requests.getActiveByRequestAndTest(second.getId(), test.getId()).getCaseId());
    }
}
