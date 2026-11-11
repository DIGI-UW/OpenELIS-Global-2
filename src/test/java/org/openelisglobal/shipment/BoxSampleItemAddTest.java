package org.openelisglobal.shipment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.shipment.dao.BoxSampleItemDAO;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.service.BoxSampleItemService;
import org.openelisglobal.shipment.service.ShippingBoxService;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Filling a box one sample at a time.
 *
 * <p>
 * Create Box posts each staged sample separately, so a box of two samples is
 * two calls against the same box row. Deliberately not {@code @Transactional}:
 * each call has to get its own transaction, the way two requests do, or the
 * conflict these tests exist for cannot happen.
 */
public class BoxSampleItemAddTest extends BaseWebContextSensitiveTest {

    private static final Integer ACTOR = 1;
    private static final String FIRST_SAMPLE_ITEM = "1";
    private static final String SECOND_SAMPLE_ITEM = "2";

    @Autowired
    private BoxSampleItemService boxSampleItemService;

    @Autowired
    private BoxSampleItemDAO boxSampleItemDAO;

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ShippingBoxService shippingBoxService;

    private Integer boxId;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        resyncSequence("clinlims.shipping_box_seq", "clinlims.shipping_box");
        resyncSequence("clinlims.box_sample_item_seq", "clinlims.box_sample_item");
        boxId = insertBox();
    }

    /**
     * The bench stages several samples and saves once. Every one of them has to be
     * in the box afterwards: a box that quietly holds fewer samples than the
     * manifest says sends a short shipment to the reference lab.
     */
    @Test
    public void everyStagedSampleLandsInTheBox() {
        boxSampleItemService.addSampleItemToBox(boxId, FIRST_SAMPLE_ITEM, ACTOR);
        boxSampleItemService.addSampleItemToBox(boxId, SECOND_SAMPLE_ITEM, ACTOR);

        List<BoxSampleItem> contents = boxSampleItemDAO.findByShippingBoxId(boxId);
        assertEquals("both samples staged for this box must be in it", 2, contents.size());
    }

    /** The count on the box row is what the box page and the manifest read. */
    @Test
    public void theBoxCountKeepsUpWithItsContents() {
        boxSampleItemService.addSampleItemToBox(boxId, FIRST_SAMPLE_ITEM, ACTOR);
        boxSampleItemService.addSampleItemToBox(boxId, SECOND_SAMPLE_ITEM, ACTOR);

        ShippingBox box = shippingBoxDAO.get(boxId).orElseThrow();
        assertEquals("the stored count must match the rows", Integer.valueOf(2), box.getActualSampleCount());
    }

    /**
     * Create Box posts its samples concurrently, so two requests read the same box
     * row and both write its sample count. Neither may be refused: the operator
     * staged both samples and there is no second chance to notice one missing once
     * the box is sealed.
     */
    @Test
    public void samplesAddedAtTheSameTimeBothLand() throws Exception {
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        Thread first = adderThread(FIRST_SAMPLE_ITEM, startTogether, failures);
        Thread second = adderThread(SECOND_SAMPLE_ITEM, startTogether, failures);
        first.start();
        second.start();
        startTogether.countDown();
        first.join(30_000);
        second.join(30_000);

        assertEquals("neither concurrent add may be refused: " + failures, 0, failures.size());
        assertEquals("both samples must be in the box", 2, boxSampleItemDAO.findByShippingBoxId(boxId).size());
        assertEquals("the stored count must match the rows", Integer.valueOf(2),
                shippingBoxDAO.get(boxId).orElseThrow().getActualSampleCount());
    }

    private Thread adderThread(String sampleItemId, CountDownLatch startTogether, List<Throwable> failures) {
        return new Thread(() -> {
            try {
                startTogether.await();
                boxSampleItemService.addSampleItemToBox(boxId, sampleItemId, ACTOR);
            } catch (Throwable t) {
                failures.add(t);
            }
        });
    }

    /**
     * Create Box used to send the number of samples it was about to add, and the
     * adds then counted them again on top of it. A new box is empty.
     */
    @Test
    public void aNewBoxStartsEmptyWhateverCountTheCallerSends() {
        ShippingBox claimsToHoldTwo = new ShippingBox();
        claimsToHoldTwo.setBoxId("BOX-FILL-0002");
        claimsToHoldTwo.setFhirUuid(UUID.randomUUID());
        claimsToHoldTwo.setDestinationFacility(organizationService.get("1"));
        claimsToHoldTwo.setSystemUserId(ACTOR);
        claimsToHoldTwo.setCapacity(25);
        claimsToHoldTwo.setActualSampleCount(2);

        ShippingBox created = shippingBoxService.createBox(claimsToHoldTwo);

        assertEquals("a box holds what was added to it, not what the caller claimed", Integer.valueOf(0),
                created.getActualSampleCount());

        boxSampleItemService.addSampleItemToBox(created.getId(), FIRST_SAMPLE_ITEM, ACTOR);
        assertEquals(Integer.valueOf(1), shippingBoxDAO.get(created.getId()).orElseThrow().getActualSampleCount());
    }

    private Integer insertBox() {
        Organization destination = organizationService.get("1");
        assertNotNull("precondition: testdata/referral.xml seeds organization 1", destination);

        ShippingBox box = new ShippingBox();
        box.setBoxId("BOX-FILL-0001");
        box.setFhirUuid(UUID.randomUUID());
        box.setDestinationFacility(destination);
        box.setState(BoxState.DRAFT);
        box.setCapacity(25);
        box.setSystemUserId(ACTOR);
        box.setArchived(false);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        box.setCreatedDate(now);
        box.setLastupdated(now);
        shippingBoxDAO.insert(box);
        return box.getId();
    }
}
