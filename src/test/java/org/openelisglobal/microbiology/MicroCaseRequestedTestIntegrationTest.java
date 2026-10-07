package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseRequestedTestDAO;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseRequestedTest;
import org.openelisglobal.microbiology.valueholder.MicroCaseTestRole;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroCaseRequestedTestIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private SampleService samples;
    @Autowired
    private SampleItemService specimens;
    @Autowired
    private SampleTypeRequestService requests;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroCaseRequestedTestDAO memberships;
    @Autowired
    private org.openelisglobal.microbiology.service.MicroRequestedCaseService routing;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void requestOwnershipPersistsWithoutInventingCollectedSpecimens() {
        String actor = fixtures.defaultUserId();
        var type = fixtures.getOrCreateActiveSampleType();
        var unit = fixtures.createLabUnit();
        var test = fixtures.createCatalogMicroTest(MicroCaseTestRole.CULTURE, unit);
        var order = new Sample();
        order.setAccessionNumber("REQ" + UUID.randomUUID().toString().substring(0, 8));
        order.setEnteredDate(Date.valueOf("2026-10-07"));
        order.setReceivedTimestamp(Timestamp.valueOf("2026-10-07 08:00:00"));
        order.setStatusId(fixtures.ensureSampleEnteredStatus());
        order.setSysUserId(actor);
        samples.insertDataWithAccessionNumber(order);
        var request = new SampleTypeRequest();
        request.setSample(order);
        request.setTypeOfSample(type);
        request.setRequestedTests(test.getId());
        request.setCreatedDate(Timestamp.valueOf("2026-10-07 08:00:00"));
        request.setSysUserId(actor);
        requests.insert(request);
        var owner = new MicroCase();
        owner.setSampleId(order.getId());
        owner.setSampleTypeId(type.getId());
        owner.setTestSectionId(unit.getId());
        owner.setCreatedBy(actor);
        owner.setSysUserId(actor);
        cases.insert(owner);
        var membership = new MicroCaseRequestedTest();
        membership.setCaseId(owner.getId());
        membership.setRequestId(request.getId());
        membership.setTestId(test.getId());
        membership.setCaseRole("CULTURE");
        membership.setCollectedInSets(true);
        membership.setCreatedAt(Timestamp.valueOf("2026-10-07 08:00:00"));
        membership.setCreatedBy(actor);
        membership.setSysUserId(actor);
        memberships.insert(membership);
        entityManager.flush();
        entityManager.clear();
        var stored = memberships.getByRequestAndTest(request.getId(), test.getId());
        assertEquals(owner.getId(), stored.getCaseId());
        assertEquals("CULTURE", stored.getCaseRole());
        assertTrue(stored.isCollectedInSets());
        assertEquals(actor, stored.getCreatedBy());
        assertEquals(1, memberships.getByCaseId(owner.getId()).size());
        assertTrue(specimens.getSampleItemsBySampleId(order.getId()).isEmpty());
        assertEquals(SampleTypeRequest.Status.REQUESTED, requests.get(request.getId()).getStatus());

        // A cancelled assignment remains provenance when the same test is ordered
        // again.
        stored.setCancelledAt(Timestamp.valueOf("2026-10-07 09:00:00"));
        stored.setCancelledBy(actor);
        stored.setCancellationReason("Order corrected");
        stored.setSysUserId(actor);
        memberships.update(stored);
        var cancelledCase = cases.get(owner.getId()).orElseThrow();
        cancelledCase.setStage("CANCELLED");
        cancelledCase.setClosedAt(stored.getCancelledAt());
        cancelledCase.setClosedBy(actor);
        cancelledCase.setSysUserId(actor);
        cases.update(cancelledCase);
        entityManager.flush();
        entityManager.clear();
        routing.routeRequests(java.util.List.of(requests.get(request.getId())), actor);
        entityManager.flush();
        entityManager.clear();
        var replacement = memberships.getByRequestAndTest(request.getId(), test.getId());
        assertNotEquals(owner.getId(), replacement.getCaseId());
        assertNull(replacement.getCancelledAt());
        var history = memberships.getByCaseId(owner.getId());
        assertEquals(1, history.size());
        assertEquals("Order corrected", history.get(0).getCancellationReason());
        assertEquals(actor, history.get(0).getCancelledBy());
        assertTrue(history.get(0).isCollectedInSets());
        assertEquals("CANCELLED", cases.get(owner.getId()).orElseThrow().getStage());
        assertTrue(specimens.getSampleItemsBySampleId(order.getId()).isEmpty());
    }
}
