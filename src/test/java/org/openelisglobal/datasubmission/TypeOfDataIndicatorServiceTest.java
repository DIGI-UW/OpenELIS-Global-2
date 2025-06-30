package org.openelisglobal.datasubmission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.datasubmission.service.TypeOfDataIndicatorService;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;
import org.springframework.beans.factory.annotation.Autowired;

public class TypeOfDataIndicatorServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TypeOfDataIndicatorService typeOfDataIndicatorService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/type-of-data-indicator.xml");

    }

    @Test
    public void getData_shouldReturnTypeOfDataIndicator() {
        TypeOfDataIndicator typeOfDataIndicator = typeOfDataIndicatorService.getTypeOfDataIndicator("1");
        typeOfDataIndicatorService.getData(typeOfDataIndicator);
        assertEquals("Cases", typeOfDataIndicator.getName());
        assertEquals("Number of cases", typeOfDataIndicator.getDescription());
    }

    @Test
    public void getAll_shouldReturnAllTypeOfIndicators() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAll();
        assertNotNull(typeOfDataIndicators);
        assertEquals(4, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("2", typeOfDataIndicators.get(1).getId());
        assertEquals("3", typeOfDataIndicators.get(2).getId());
        assertEquals("4", typeOfDataIndicators.get(3).getId());
    }

    @Test
    public void getAllTypeOfDataIndicator_shouldReturnAllTypeOfDataIndicators() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllTypeOfDataIndicator();
        assertNotNull(typeOfDataIndicators);
        assertEquals(4, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("2", typeOfDataIndicators.get(1).getId());
        assertEquals("3", typeOfDataIndicators.get(2).getId());
        assertEquals("4", typeOfDataIndicators.get(3).getId());
    }

    @Test
    public void getTypeOfDataIndicator_shouldReturnTypeOfDataIndicator() {
        TypeOfDataIndicator typeOfDataIndicator = typeOfDataIndicatorService.getTypeOfDataIndicator("1");
        assertNotNull(typeOfDataIndicator);
        assertEquals("1", typeOfDataIndicator.getId());
        assertEquals("Cases", typeOfDataIndicator.getName());
        assertEquals("Number of cases", typeOfDataIndicator.getDescription());
    }

    @Test
    public void get_shouldReturnTypeOfDataIndicator() {
        TypeOfDataIndicator typeOfDataIndicator = typeOfDataIndicatorService.get("1");
        typeOfDataIndicatorService.getData(typeOfDataIndicator);
        assertEquals("Cases", typeOfDataIndicator.getName());
        assertEquals("Number of cases", typeOfDataIndicator.getDescription());

    }

    @Test
    public void getPage_shouldReturnPageOfTypeOfIndicators() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(typeOfDataIndicators.size() <= expectedPages);
    }

    @Test
    public void getAllMatching_shouldReturnMatchingIndicatorTypes() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllMatching("name", "Cases");
        assertNotNull(typeOfDataIndicators);
        assertEquals(1, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("Cases", typeOfDataIndicators.get(0).getName());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnMatchingIndicatorTypes() {
        Map<String, Object> properties = Map.of("name", "Cases");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllMatching(properties);
        assertNotNull(typeOfDataIndicators);
        assertEquals(1, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("Cases", typeOfDataIndicators.get(0).getName());
    }

    @Test
    public void getAllOrdered_shouldReturnAllOrderedTypeOfDataIndicators() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllOrdered("id", false);
        assertNotNull(typeOfDataIndicators);
        assertEquals(4, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("2", typeOfDataIndicators.get(1).getId());
        assertEquals("3", typeOfDataIndicators.get(2).getId());
        assertEquals("4", typeOfDataIndicators.get(3).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnAllOrderedTypeOfDataIndicators() {
        List<String> orderBy = List.of("id");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllOrdered(orderBy, false);
        assertNotNull(typeOfDataIndicators);
        assertEquals(4, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("2", typeOfDataIndicators.get(1).getId());
        assertEquals("3", typeOfDataIndicators.get(2).getId());
        assertEquals("4", typeOfDataIndicators.get(3).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnAllMatchingOrderedTypeOfDataIndicators() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllMatchingOrdered("name",
                "Cases", "id", false);
        assertNotNull(typeOfDataIndicators);
        assertEquals(1, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("Cases", typeOfDataIndicators.get(0).getName());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnAllMatchingOrderedTypeOfDataIndicators() {
        Map<String, Object> properties = Map.of("name", "Cases");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllMatchingOrdered(properties,
                "id", false);
        assertNotNull(typeOfDataIndicators);
        assertEquals(1, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("Cases", typeOfDataIndicators.get(0).getName());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnAllMatchingOrderedTypeOfDataIndicators() {
        List<String> orderBy = List.of("id");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllMatchingOrdered("name",
                "Cases", orderBy, false);
        assertNotNull(typeOfDataIndicators);
        assertEquals(1, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("Cases", typeOfDataIndicators.get(0).getName());
    }

    @Test
    public void getAllMatchingOrderedGivenMapAndList_shouldReturnAllMatchingOrderedTypeOfDataIndicators() {
        Map<String, Object> properties = Map.of("name", "Cases");
        List<String> orderBy = List.of("id");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllMatchingOrdered(properties,
                orderBy, false);
        assertNotNull(typeOfDataIndicators);
        assertEquals(1, typeOfDataIndicators.size());
        assertEquals("1", typeOfDataIndicators.get(0).getId());
        assertEquals("Cases", typeOfDataIndicators.get(0).getName());
    }

    @Test
    public void getMatchingPage_shouldReturnPageOfAllMatchingTypeOfDataIndicators() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getMatchingPage("name", "Cases", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(typeOfDataIndicators);
        assertTrue(typeOfDataIndicators.size() <= expectedPages);

    }

    @Test
    public void getMatchingPageGivenMap_shouldReturnPageOfAllMatchingTypeOfDataIndicators() {
        Map<String, Object> properties = Map.of("name", "Cases");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getMatchingPage(properties, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(typeOfDataIndicators);
        assertTrue(typeOfDataIndicators.size() <= expectedPages);
    }

    @Test
    public void getOrderedPage_shouldReturnPageOfAllOrderedTypeOfDataIndicators() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(typeOfDataIndicators);
        assertTrue(typeOfDataIndicators.size() <= expectedPages);
    }

    @Test
    public void getOrderedPageGivenList_shouldReturnPageOfAllOrderedTypeOfDataIndicators() {
        List<String> orderBy = List.of("id");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getOrderedPage(orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(typeOfDataIndicators);
        assertTrue(typeOfDataIndicators.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnPageOfAllOrderedMatchingTypeOfDataIndicators() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getMatchingOrderedPage("name",
                "Cases", "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(typeOfDataIndicators);
        assertTrue(typeOfDataIndicators.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenMap_shouldReturnPageOfAllOrderedMatchingTypeOfDataIndicators() {
        Map<String, Object> properties = Map.of("name", "Cases");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getMatchingOrderedPage(properties,
                "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(typeOfDataIndicators);
        assertTrue(typeOfDataIndicators.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenList_shouldReturnPageOfAllOrderedMatchingTypeOfDataIndicators() {
        List<String> orderBy = List.of("id");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getMatchingOrderedPage("name",
                "Cases", orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(typeOfDataIndicators);
        assertTrue(typeOfDataIndicators.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenMapAndList_shouldReturnPageOfAllOrderedMatchingTypeOfDataIndicators() {
        Map<String, Object> properties = Map.of("name", "Cases");
        List<String> orderBy = List.of("id");
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getMatchingOrderedPage(properties,
                orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(typeOfDataIndicators);
        assertTrue(typeOfDataIndicators.size() <= expectedPages);
    }

    @Test
    public void getCount_shouldReturnCountOfAllTypeOfDataIndicators() {
        int count = typeOfDataIndicatorService.getCount();
        assertEquals(4, count);
    }

    @Test
    public void getCountLike_shouldReturnCountOfAllTypeOfDataIndicatorsLike() {
        int count = typeOfDataIndicatorService.getCountLike("name", "Cases");
        assertEquals(1, count);
    }

    @Test
    public void getNext_shouldReturnNextDataIndicatorType() {
        TypeOfDataIndicator typeOfDataIndicator = typeOfDataIndicatorService.getNext("1");
        assertNotNull(typeOfDataIndicator);
        assertEquals("2", typeOfDataIndicator.getId());
        assertEquals("Tests", typeOfDataIndicator.getName());
    }

    @Test
    public void getPrevious_shouldReturnPreviousDataIndicatorType() {
        TypeOfDataIndicator typeOfDataIndicator = typeOfDataIndicatorService.getPrevious("2");
        assertNotNull(typeOfDataIndicator);
        assertEquals("1", typeOfDataIndicator.getId());
        assertEquals("Cases", typeOfDataIndicator.getName());
    }

    @Test
    public void deleteAll_shouldDeleteAllDataIndicatorsTypes() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllTypeOfDataIndicator();
        typeOfDataIndicatorService.deleteAll(typeOfDataIndicators);
        List<TypeOfDataIndicator> deletedTypeOfDataIndicators = typeOfDataIndicatorService.getAllTypeOfDataIndicator();
        assertNotNull(deletedTypeOfDataIndicators);
        assertEquals(0, deletedTypeOfDataIndicators.size());
    }

    @Test
    public void delete_shouldDeleteTypeOfDataIndicator() {
        TypeOfDataIndicator typeOfDataIndicator = typeOfDataIndicatorService.getTypeOfDataIndicator("1");
        assertNotNull(typeOfDataIndicator);
        typeOfDataIndicatorService.delete(typeOfDataIndicator);
        TypeOfDataIndicator deletedTypeOfDataIndicator = typeOfDataIndicatorService.getTypeOfDataIndicator("1");
        assertEquals(null, deletedTypeOfDataIndicator);
    }

    @Test
    public void insert_shouldInsertDataIndicatorType() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllTypeOfDataIndicator();
        typeOfDataIndicatorService.deleteAll(typeOfDataIndicators);
        TypeOfDataIndicator newTypeOfDataIndicator = new TypeOfDataIndicator();
        newTypeOfDataIndicator.setId("1");
        newTypeOfDataIndicator.setName("Providers");
        String insertedId = typeOfDataIndicatorService.insert(newTypeOfDataIndicator);
        TypeOfDataIndicator insertedTypeOfDataIndicator = typeOfDataIndicatorService.getTypeOfDataIndicator(insertedId);
        assertNotNull(insertedTypeOfDataIndicator);
        assertEquals(insertedId, insertedTypeOfDataIndicator.getId());
        assertEquals("Providers", insertedTypeOfDataIndicator.getName());
    }

    @Test
    public void update_shouldUpdateTypeOfDataIndicator() {
        TypeOfDataIndicator typeOfDataIndicator = typeOfDataIndicatorService.getTypeOfDataIndicator("1");
        assertNotNull(typeOfDataIndicator);
        typeOfDataIndicator.setName("Updated Cases");
        TypeOfDataIndicator indicatorType1 = typeOfDataIndicatorService.update(typeOfDataIndicator);
        TypeOfDataIndicator updatedTypeOfDataIndicator = typeOfDataIndicatorService.getTypeOfDataIndicator("1");
        assertNotNull(updatedTypeOfDataIndicator);
        assertEquals(indicatorType1.getName(), updatedTypeOfDataIndicator.getName());
    }

    @Test
    public void save_shouldSaveTypeOfDataIndicator() {
        List<TypeOfDataIndicator> typeOfDataIndicators = typeOfDataIndicatorService.getAllTypeOfDataIndicator();
        typeOfDataIndicatorService.deleteAll(typeOfDataIndicators);
        TypeOfDataIndicator newTypeOfDataIndicator = new TypeOfDataIndicator();
        newTypeOfDataIndicator.setName("Providers");
        TypeOfDataIndicator savedTypeOfDataIndicator = typeOfDataIndicatorService.save(newTypeOfDataIndicator);
        TypeOfDataIndicator updatedTypeOfDataIndicator = typeOfDataIndicatorService
                .getTypeOfDataIndicator(savedTypeOfDataIndicator.getId());
        assertNotNull(updatedTypeOfDataIndicator);
        assertEquals(savedTypeOfDataIndicator.getName(), updatedTypeOfDataIndicator.getName());
    }

}
