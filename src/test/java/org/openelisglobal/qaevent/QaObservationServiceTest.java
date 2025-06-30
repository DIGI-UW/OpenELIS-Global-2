package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.qaevent.service.QaObservationService;
import org.openelisglobal.qaevent.service.QaObservationTypeService;
import org.openelisglobal.qaevent.valueholder.QaObservation;
import org.openelisglobal.qaevent.valueholder.QaObservationType;
import org.springframework.beans.factory.annotation.Autowired;

public class QaObservationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QaObservationService qaObservationService;

    @Autowired
    private QaObservationTypeService qaObservationTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/qa-observation.xml");
    }

    @Test
    public void getAll_shouldReturnAllObservations() {
        List<QaObservation> list = qaObservationService.getAll();
        assertEquals(6, list.size());
        assertEquals("1", list.get(0).getId());
        assertEquals("2", list.get(1).getId());
        assertEquals("3", list.get(2).getId());
        assertEquals("4", list.get(3).getId());
        assertEquals("5", list.get(4).getId());
        assertEquals("6", list.get(5).getId());

    }

    @Test
    public void getQaObservationByTypeAndObserved_shouldReturnObservation() {
        QaObservation observation = qaObservationService.getQaObservationByTypeAndObserved("Sample Verification",
                "SAMPLE", "1001");
        assertEquals("1", observation.getId());
    }

    @Test
    public void deleteAll_shouldDeleteAllObservations() {
        List<QaObservation> list = qaObservationService.getAll();
        qaObservationService.deleteAll(list);
        List<QaObservation> list2 = qaObservationService.getAll();
        assertEquals(0, list2.size());
    }

    @Test
    public void delete_shouldDeleteObservation() {
        QaObservation observation = qaObservationService.get("1");
        qaObservationService.delete(observation);
        List<QaObservation> list = qaObservationService.getAll();
        assertEquals(5, list.size());
        assertEquals("2", list.get(0).getId());
        assertEquals("3", list.get(1).getId());
        assertEquals("4", list.get(2).getId());
        assertEquals("5", list.get(3).getId());
        assertEquals("6", list.get(4).getId());
    }

    @Test
    public void getAllMatching_shouldReturnMatchingObservations() {
        List<QaObservation> list = qaObservationService.getAllMatching("observedType", "SAMPLE");
        assertEquals(3, list.size());
        assertEquals("1", list.get(0).getId());
        assertEquals("3", list.get(1).getId());
        assertEquals("5", list.get(2).getId());

    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnMatchingObservations() {
        Map<String, Object> map = Map.of("observedType", "SAMPLE");
        List<QaObservation> list = qaObservationService.getAllMatching(map);
        assertEquals(3, list.size());
        assertEquals("1", list.get(0).getId());
        assertEquals("3", list.get(1).getId());
        assertEquals("5", list.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingObservations() {
        List<QaObservation> list = qaObservationService.getAllMatchingOrdered("observedType", "SAMPLE", "id", false);
        assertEquals(3, list.size());
        assertEquals("1", list.get(0).getId());
        assertEquals("3", list.get(1).getId());
        assertEquals("5", list.get(2).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnMatchingObservations() {
        Map<String, Object> map = Map.of("observedType", "SAMPLE");
        List<QaObservation> list = qaObservationService.getAllMatchingOrdered(map, "id", false);
        assertEquals(3, list.size());
        assertEquals("1", list.get(0).getId());
        assertEquals("3", list.get(1).getId());
        assertEquals("5", list.get(2).getId());
    }

    @Test
    public void getAllOrdered_returnOrderdObservations() {
        List<QaObservation> list = qaObservationService.getAllOrdered("id", false);
        assertEquals(6, list.size());
        assertEquals("1", list.get(0).getId());
        assertEquals("2", list.get(1).getId());
        assertEquals("3", list.get(2).getId());
        assertEquals("4", list.get(3).getId());
        assertEquals("5", list.get(4).getId());
        assertEquals("6", list.get(5).getId());
    }

    @Test
    public void getAllOrderedGivenList_returnOrderdObservations() {
        List<String> items = List.of("id");
        List<QaObservation> list = qaObservationService.getAllOrdered(items, false);
        assertEquals(6, list.size());
        assertEquals("1", list.get(0).getId());
        assertEquals("2", list.get(1).getId());
        assertEquals("3", list.get(2).getId());
        assertEquals("4", list.get(3).getId());
        assertEquals("5", list.get(4).getId());
        assertEquals("6", list.get(5).getId());
    }

    @Test
    public void getMatchingPageGivenMap_shouldReturnMatchingObservations() {
        Map<String, Object> map = Map.of("observedType", "SAMPLE");
        List<QaObservation> list = qaObservationService.getMatchingPage(map, 1);
        int expectedSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(list.size() <= expectedSize);

    }

    @Test
    public void getMatchingPage_shouldReturnMatchingObservations() {
        List<QaObservation> list = qaObservationService.getMatchingPage("observedType", "SAMPLE", 1);
        int expectedSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(list.size() <= expectedSize);
    }

    @Test
    public void getPage_shouldReturnPageOfObservations() {
        List<QaObservation> list = qaObservationService.getPage(1);
        int expectedSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(list.size() <= expectedSize);
    }

    @Test
    public void getMatchingOrderdPage_shouldReturnMatchingObservations() {
        List<QaObservation> list = qaObservationService.getMatchingOrderedPage("observedType", "SAMPLE", "id", false,
                1);
        int expectedSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(list.size() <= expectedSize);
    }

    @Test
    public void getMatchingOrderedPageGivenMap_shouldReturnMatchingObservations() {
        Map<String, Object> map = Map.of("observedType", "SAMPLE");
        List<QaObservation> list = qaObservationService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(list.size() <= expectedSize);
    }

    @Test
    public void getOrderedPage_shouldReturnOrderedObservations() {
        List<QaObservation> list = qaObservationService.getOrderedPage("id", false, 1);
        int expectedSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(list.size() <= expectedSize);
    }

    @Test
    public void getOrderedPageGivenList_shouldReturnOrderedObservations() {
        List<String> items = List.of("id");
        List<QaObservation> list = qaObservationService.getOrderedPage(items, false, 1);
        int expectedSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(list.size() <= expectedSize);
    }

    @Test
    public void getNext_Id_shouldReturnNextId() {

        QaObservation nextObservation = qaObservationService.getNext("1");
        assertEquals("2", nextObservation.getId());
    }

    @Test
    public void getPrevious_Id_shouldReturnPreviousId() {
        QaObservation previousObservation = qaObservationService.getPrevious("2");
        assertEquals("1", previousObservation.getId());
    }

    @Test
    public void getCount_shouldReturnCount() {
        int count = qaObservationService.getCount();
        assertEquals(6, count);
    }

    @Test
    public void getCountLike_shouldReturnCount() {
        int count = qaObservationService.getCountLike("observedType", "SAMPLE");
        assertEquals(3, count);
    }

    @Test
    public void getCountLikeGivenMap_shouldReturnCount() {
        Map<String, String> map = Map.of("observedType", "SAMPLE");
        int count = qaObservationService.getCountLike(map);
        assertEquals(3, count);
    }

    @Test
    public void getCountMatching_shouldReturnCount() {
        int count = qaObservationService.getCountMatching("observedType", "SAMPLE");
        assertEquals(3, count);
    }

    @Test
    public void getCountMatchingGivenMap_shouldReturnCount() {
        Map<String, Object> map = Map.of("observedType", "SAMPLE");
        int count = qaObservationService.getCountMatching(map);
        assertEquals(3, count);
    }

    @Test
    public void insert_shouldInsertObservation() {
        List<QaObservation> list = qaObservationService.getAll();
        QaObservationType observationType = qaObservationTypeService.get("11");
        qaObservationService.deleteAll(list);
        QaObservation observation = new QaObservation();
        observation.setObservedType("SAMPLE");
        observation.setObservedId("1444");
        observation.setValue("Sample Value");
        observation.setObservationType(observationType);
        observation.setValueType("K");
        qaObservationService.insert(observation);
        List<QaObservation> list2 = qaObservationService.getAll();
        assertEquals(1, list2.size());
    }

    @Test
    public void save_shouldSaveObservation() {
        List<QaObservation> list = qaObservationService.getAll();
        QaObservationType observationType = qaObservationTypeService.get("11");
        qaObservationService.deleteAll(list);
        QaObservation observation = new QaObservation();
        observation.setObservedType("SAMPLE");
        observation.setObservedId("1444");
        observation.setValue("Sample Value");
        observation.setObservationType(observationType);
        observation.setValueType("K");
        QaObservation observation2 = qaObservationService.save(observation);
        assertEquals("1444", observation2.getObservedId());
    }

    @Test
    public void update_shouldUpdateObservation() {
        QaObservation observation = qaObservationService.get("1");
        observation.setObservedType("ANALYSIS");
        observation.setObservedId("1444");
        observation.setValue("Sample Value");
        QaObservation observation2 = qaObservationService.update(observation);
        assertEquals("1444", observation2.getObservedId());
    }

}