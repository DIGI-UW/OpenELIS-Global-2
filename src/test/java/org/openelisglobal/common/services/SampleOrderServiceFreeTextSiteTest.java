package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleOrderService.SampleOrderPersistenceArtifacts;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modify Order: a referring site typed as free text (no site picked from the
 * list) used to fail the save with a NumberFormatException on the empty site
 * id, and an order without a patient failed on the missing patient. The typed
 * site becomes a new organization and the order details save without a patient.
 */
@Transactional
public class SampleOrderServiceFreeTextSiteTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private SampleHumanService sampleHumanService;

    private String userId;
    private SampleItem item;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        item = fixtures.createSampleWithSampleItem("FREESITE");
    }

    @Test
    public void aTypedSiteNameWithoutAnIdBecomesANewOrganization() {
        SampleHuman link = new SampleHuman();
        link.setSampleId(item.getSample().getId());
        link.setPatientId(fixtures.createPatient("FREESITE").getId());
        link.setSysUserId(userId);
        sampleHumanService.insert(link);
        SampleOrderItem order = new SampleOrderItem();
        order.setSampleId(item.getSample().getId());
        order.setModified(true);
        order.setReceivedDateForDisplay(DateUtil.getCurrentDateAsText());
        order.setReceivedTime("09:00");
        order.setReferringSiteId("");
        order.setReferringSiteName("Walk-in Clinic Kila");

        SampleOrderPersistenceArtifacts artifacts = new SampleOrderService(order).getPersistenceArtifacts(null, userId);

        assertNotNull(artifacts.getProviderOrganization());
        assertNull("the new organization is inserted by the save", artifacts.getProviderOrganization().getId());
        assertEquals("Walk-in Clinic Kila", artifacts.getProviderOrganization().getOrganizationName());
        assertNotNull(artifacts.getSampleOrganizationRequester());
    }

    @Test
    public void anOrderWithoutAPatientCanHaveItsOrderDetailsEdited() {
        SampleOrderItem order = new SampleOrderItem();
        order.setSampleId(item.getSample().getId());
        order.setModified(true);
        order.setReceivedDateForDisplay(DateUtil.getCurrentDateAsText());
        order.setReceivedTime("09:00");
        order.setRequestDate(DateUtil.getCurrentDateAsText());

        SampleOrderPersistenceArtifacts artifacts = new SampleOrderService(order).getPersistenceArtifacts(null, userId);

        assertEquals(1, artifacts.getObservations().size());
        assertEquals(DateUtil.getCurrentDateAsText(), artifacts.getObservations().getFirst().getValue());
        assertNull(artifacts.getObservations().getFirst().getPatientId());
    }
}
