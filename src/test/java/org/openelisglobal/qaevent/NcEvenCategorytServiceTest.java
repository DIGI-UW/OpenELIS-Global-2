package org.openelisglobal.qaevent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.springframework.beans.factory.annotation.Autowired;

public class NcEvenCategorytServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NceCategoryService nceCategoryService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/nc-event-category.xml");
    }

    @Test
    public void getAll_shouldReturnAllEvents() {
        List<NceCategory> ncEvents = nceCategoryService.getAllNceCategories();
        assertEquals(3, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
        assertEquals("2", ncEvents.get(1).getId());
        assertEquals("3", ncEvents.get(2).getId());

    }

    @Test
    public void getMatching_shouldReturnAllMatching() {
        List<NceCategory> categories = nceCategoryService.getAllMatching("name", "Sample");
        assertEquals(1, categories.size());
        assertEquals("3", categories.get(0).getId());
    }

    @Test
    public void getAllMatchingGivenMap() {
        Map<String, Object> map = Map.of("name", "Sample");
        List<NceCategory> categories = nceCategoryService.getAllMatching(map);
        assertEquals(1, categories.size());
        assertEquals("3", categories.get(0).getId());

    }

    @Test
    public void getAllOrdered_shouldReturnAllOrdered() {
        List<NceCategory> ncEvents = nceCategoryService.getAllOrdered("id", false);
        assertEquals(3, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
        assertEquals("2", ncEvents.get(1).getId());
        assertEquals("3", ncEvents.get(2).getId());

    }

    @Test
    public void getAllOrderedGivenList_shouldReturnAllOrdered() {
        List<String> orderBy = List.of("id");
        List<NceCategory> ncEvents = nceCategoryService.getAllOrdered(orderBy, false);
        assertEquals(3, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
        assertEquals("2", ncEvents.get(1).getId());
        assertEquals("3", ncEvents.get(2).getId());

    }

    @Test
    public void getAllOrderedMatching_shouldReturnAllOrderdMatching() {
        List<String> orderBy = List.of("id");
        List<NceCategory> ncEvents = nceCategoryService.getAllMatchingOrdered("name", "Sample", orderBy, false);
        assertEquals(1, ncEvents.size());
        assertEquals("3", ncEvents.get(0).getId());
    }

    @Test
    public void getAllOrderedMatchingGivenMapandList_shouldReturnAllOrderdMatching() {
        Map<String, Object> map = Map.of("name", "Sample");
        List<String> orderBy = List.of("id");
        List<NceCategory> ncEvents = nceCategoryService.getAllMatchingOrdered(map, orderBy, false);
        assertEquals(1, ncEvents.size());
        assertEquals("3", ncEvents.get(0).getId());
    }

    @Test
    public void getAllOrderedMatchingGivenMap_shouldReturnAllOrderdMatching() {
        Map<String, Object> map = Map.of("name", "Sample");
        List<NceCategory> ncEvents = nceCategoryService.getAllMatchingOrdered(map, "id", false);
        assertEquals(1, ncEvents.size());
        assertEquals("3", ncEvents.get(0).getId());

    }

    @Test
    public void getPage_shouldReturnPage() {
        List<NceCategory> ncEvents = nceCategoryService.getPage(1);
        int pageExpected = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= pageExpected);

    }

    @Test
    public void getAllMatchingPage_shouldReturnPage() {
        Map<String, Object> map = Map.of("name", "Sample");
        List<NceCategory> ncEvents = nceCategoryService.getMatchingPage(map, 1);
        int pageExpected = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= pageExpected);
    }

    @Test
    public void getAllOrderedPage_shouldReturnPage() {
        List<String> orderBy = List.of("id");
        List<NceCategory> ncEvents = nceCategoryService.getOrderedPage(orderBy, false, 1);
        int pageExpected = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= pageExpected);
    }

    @Test
    public void getAllOrderedMatchingPage_shouldReturnPage() {
        Map<String, Object> map = Map.of("name", "Sample");
        List<String> orderBy = List.of("id");
        List<NceCategory> ncEvents = nceCategoryService.getMatchingOrderedPage(map, orderBy, false, 1);
        int pageExpected = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= pageExpected);
    }

    @Test
    public void getOrderedPage_shouldReturnPage() {
        List<NceCategory> ncEvents = nceCategoryService.getOrderedPage("id", false, 1);
        int pageExpected = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= pageExpected);
    }

    @Test
    public void getAllMatchingOrderedPage_shouldReturnPage() {
        Map<String, Object> map = Map.of("name", "Sample");
        List<NceCategory> ncEvents = nceCategoryService.getMatchingOrderedPage(map, "id", false, 1);
        int pageExpected = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= pageExpected);
    }

    @Test
    public void getAllMatchingOrderedPage() {
        List<NceCategory> ncEvents = nceCategoryService.getMatchingOrderedPage("name", "Sample", "id", false, 1);
        int pageExpected = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(ncEvents.size() <= pageExpected);
    }

    @Test
    public void deleteAll_shouldDeleteAllCategories() {
        List<NceCategory> ncEvents = nceCategoryService.getAllNceCategories();
        nceCategoryService.deleteAll(ncEvents);
        List<NceCategory> ncEvents2 = nceCategoryService.getAllNceCategories();
        assertEquals(0, ncEvents2.size());
    }

    @Test
    public void deleteAll_shouldDeleteCategory() {
        NceCategory nceCategory = nceCategoryService.get("1");
        nceCategoryService.delete(nceCategory);
        List<NceCategory> ncEvents = nceCategoryService.getAllNceCategories();
        assertEquals(2, ncEvents.size());
        assertEquals("2", ncEvents.get(0).getId());
        assertEquals("3", ncEvents.get(1).getId());
    }

    @Test
    public void getNext_shouldReturnNext() {
        NceCategory nceCategory = nceCategoryService.getNext("1");
        assertEquals("2", nceCategory.getId());
    }

    @Test
    public void getPrevious_shouldReturnPrevious() {
        NceCategory nceCategory = nceCategoryService.getPrevious("2");
        assertEquals("1", nceCategory.getId());
    }

    @Test
    public void getCount_shouldReturnCount() {
        int count = nceCategoryService.getCount();
        assertEquals(3, count);
    }

    @Test
    public void getCountLike_shouldReturnCountLike() {
        List<NceCategory> ncEvents = nceCategoryService.getAllMatching("name", "Sample");
        int count = nceCategoryService.getCountLike("name", "Sample");
        assertEquals(ncEvents.size(), count);
    }

    @Test
    public void getAllNceCategories_shouldReturnAllNceCategories() {
        List<NceCategory> ncEvents = nceCategoryService.getAllNceCategories();
        assertEquals(3, ncEvents.size());
        assertEquals("1", ncEvents.get(0).getId());
        assertEquals("2", ncEvents.get(1).getId());
        assertEquals("3", ncEvents.get(2).getId());
    }

}
