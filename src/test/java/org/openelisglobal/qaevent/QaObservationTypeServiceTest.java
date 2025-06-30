package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.qaevent.service.QaObservationTypeService;
import org.openelisglobal.qaevent.valueholder.QaObservationType;
import org.springframework.beans.factory.annotation.Autowired;

public class QaObservationTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QaObservationTypeService observationTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/qa-observation-type.xml");

    }

    @Test
    public void getAll_shouldReturnAllObservationsTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getAll();
        assertEquals(4, observationTypes.size());
        assertEquals("10", observationTypes.get(0).getId());
        assertEquals("11", observationTypes.get(1).getId());
        assertEquals("12", observationTypes.get(2).getId());
        assertEquals("13", observationTypes.get(3).getId());
    }

    @Test
    public void getByName_shouldReturnObservationType() {
        QaObservationType observationType1 = observationTypeService.getQaObservationTypeByName("Sample Verification");
        assertEquals("10", observationType1.getId());
        QaObservationType observationType2 = observationTypeService.getQaObservationTypeByName("Result Check");
        assertEquals("11", observationType2.getId());

    }

    @Test
    public void getAllMatching_shouldReturnMatchingObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getAllMatching("name", "Sample Verification");
        assertEquals(1, observationTypes.size());
        assertEquals("10", observationTypes.get(0).getId());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnMatchingObservationTypes() {
        Map<String, Object> map = Map.of("name", "Sample Verification");
        List<QaObservationType> observationTypes = observationTypeService.getAllMatching(map);
        assertEquals(1, observationTypes.size());
        assertEquals("10", observationTypes.get(0).getId());
    }

    @Test
    public void getAllMatchingPage_Size_shouldReturnMatchingObservationTypes() {
        Map<String, Object> map = Map.of("name", "Sample Verification");
        List<QaObservationType> observationTypes = observationTypeService.getMatchingPage(map, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(observationTypes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPage_shouldReturnPageOfMatchingObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getMatchingPage("name", "Sample Verification",
                1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(observationTypes.size() <= expectedPages);
    }

    @Test
    public void getAllOrderedBy_shouldReturnOrderedObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getAllOrdered("id", false);
        assertEquals(4, observationTypes.size());
        assertEquals("10", observationTypes.get(0).getId());
        assertEquals("11", observationTypes.get(1).getId());
        assertEquals("12", observationTypes.get(2).getId());
        assertEquals("13", observationTypes.get(3).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnOrderedObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getAllOrdered(List.of("id"), false);
        assertEquals(4, observationTypes.size());
        assertEquals("10", observationTypes.get(0).getId());
        assertEquals("11", observationTypes.get(1).getId());
        assertEquals("12", observationTypes.get(2).getId());
        assertEquals("13", observationTypes.get(3).getId());
    }

    @Test
    public void getAllOrderedPage_Size_shouldReturnOrderedObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(observationTypes.size() <= expectedPages);
    }

    @Test
    public void getAllOrderedPage_shouldReturnPageOfOrderedObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getOrderedPage(List.of("id"), false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(observationTypes.size() <= expectedPages);
    }

    @Test
    public void getAllMartchingOrdered_shouldReturnOrderedMatchingObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getAllMatchingOrdered("name",
                "Sample Verification", "id", false);
        assertEquals(1, observationTypes.size());
        assertEquals("10", observationTypes.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnOrderedMatchingObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getAllMatchingOrdered("name",
                "Sample Verification", List.of("id"), false);
        assertEquals(1, observationTypes.size());
        assertEquals("10", observationTypes.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedPage_Size_shouldReturnOrderedMatchingObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getMatchingOrderedPage("name",
                "Sample Verification", "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(observationTypes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPage_shouldReturnPageOfOrderedMatchingObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getMatchingOrderedPage("name",
                "Sample Verification", List.of("id"), false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(observationTypes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnOrderedMatchingObservationTypes() {
        Map<String, Object> map = Map.of("name", "Sample Verification");
        List<QaObservationType> observationTypes = observationTypeService.getAllMatchingOrdered(map, List.of("id"),
                false);
        assertEquals(1, observationTypes.size());
        assertEquals("10", observationTypes.get(0).getId());

    }

    @Test
    public void getAllMatchingOrderedPage_SizeGivenMap_shouldReturnOrderedMatchingObservationTypes() {
        Map<String, Object> map = Map.of("name", "Sample Verification");
        List<QaObservationType> observationTypes = observationTypeService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(observationTypes.size() <= expectedPages);

    }

    @Test
    public void getAllMatchingOrderedPageGivenMap_shouldReturnPageOfOrderedMatchingObservationTypes() {
        Map<String, Object> map = Map.of("name", "Sample Verification");
        List<QaObservationType> observationTypes = observationTypeService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(observationTypes.size() <= expectedPages);
    }

    @Test
    public void deleteAll_shouldDeleteAllObservationTypes() {
        List<QaObservationType> observationTypes = observationTypeService.getAll();
        assertEquals(4, observationTypes.size());
        observationTypeService.deleteAll(observationTypes);
        List<QaObservationType> observationTypes2 = observationTypeService.getAll();
        assertEquals(0, observationTypes2.size());
    }

    @Test
    public void delete_shouldDeleteObservationType() {
        QaObservationType observationType = observationTypeService.get("10");
        observationTypeService.delete(observationType);
        List<QaObservationType> observationTypes2 = observationTypeService.getAll();
        assertEquals(3, observationTypes2.size());
    }

}
