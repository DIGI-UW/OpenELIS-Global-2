package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;

@org.springframework.transaction.annotation.Transactional
public class MicroEnvironmentalWorkspaceIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroCaseSearchDAO search;
    @Autowired
    private MicroSurveillanceDAO surveillance;
    @Autowired
    private MicroCaseContextService context;
    @Autowired
    private MicroCaseActivityDAO activities;
    @Autowired
    private MicroCaseMembershipService membership;
    @Autowired
    private org.openelisglobal.sample.service.SampleService samples;
    @Autowired
    private org.openelisglobal.sampleitem.service.SampleItemService sampleItems;
    @Autowired
    private org.openelisglobal.test.service.TestSectionService units;
    @Autowired
    private org.openelisglobal.vector.service.VectorSamplingSiteService sites;
    @PersistenceContext
    private EntityManager em;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/system-user.xml");
        executeDataSetWithStateManagement("testdata/role.xml");
    }

    @Test
    public void siteAndSamplingPointSearchReturnOnlyTheAllowedLabAndContextSurvivesSessionClose() {
        var source = fixtures.createSampleWithSampleItem("M8");
        var order = source.getSample();
        order.setDomain("E");
        samples.update(order);
        source.setTypeOfSample(fixtures.createTypeOfSample());
        String point = "tap-" + UUID.randomUUID();
        source.setLocationDetails(point);
        sampleItems.update(source);
        var site = new org.openelisglobal.vector.valueholder.VectorSamplingSite();
        site.setCode(UUID.randomUUID().toString().substring(0, 20));
        site.setName("Environmental " + point);
        site.setActive(true);
        site.setGpsLatitude("-6.2");
        site.setSysUserId("1");
        sites.insert(site);
        var unit = fixtures.createLabUnit();
        unit.setDomain("ENVIRONMENTAL");
        units.update(unit);
        var c = new MicroCase();
        c.setSampleId(order.getId());
        c.setSampleItemId(source.getId());
        c.setSampleTypeId(source.getTypeOfSampleId());
        c.setSiteId(site.getId().toString());
        c.setLabUnitId(unit.getId());
        c.setCreatedBy("1");
        cases.insert(c);
        membership.addSample(c.getId(), source.getId(), "1");
        var activity = new MicroCaseActivity();
        activity.setCaseId(c.getId());
        activity.setActivityType("MIGRATION_REVIEW");
        activity.setPerformedBy("1");
        activities.insert(activity);
        em.flush();
        em.clear();
        for (String text : new String[] { point, site.getCode(), site.getName() }) {
            var query = new MicroCaseSearchForm();
            query.q = text;
            query.migrationReview = true;
            assertEquals(1, search.count(query, Set.of(unit.getId())));
            assertEquals(c.getId(), ((MicroCase) search.search(query, Set.of(unit.getId())).get(0)[0]).getId());
            assertEquals(0, search.count(query, Set.of("-1")));
        }
        var shell = new MicroCaseShellForm();
        context.compile(cases.get(c.getId()).orElseThrow(), shell);
        var metadata = surveillance.contexts(java.util.List.of(c.getId()));
        assertEquals("E", metadata.get(0)[1]);
        assertEquals(site.getCode(), metadata.get(0)[3]);
        assertTrue(surveillance.originalSpecimens(java.util.List.of(c.getId())).isEmpty());
        assertTrue(surveillance.externalSampleIds(java.util.List.of(c.getId())).isEmpty());
        assertTrue(surveillance.outcomes(java.util.List.of(c.getId())).isEmpty());
        em.clear();
        assertEquals("ENVIRONMENTAL", shell.domain);
        assertEquals(site.getName(), shell.siteName);
        assertEquals("ROUTINE_MONITORING", shell.culturePurpose);
        assertNull(shell.patientId);
        assertEquals(4, shell.purposeOptions.size());
    }
}
