package org.openelisglobal.systemModule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.systemmodule.service.SystemModuleParamService;
import org.openelisglobal.systemmodule.valueholder.SystemModuleParam;
import org.springframework.beans.factory.annotation.Autowired;

public class SystemModuleParamServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SystemModuleParamService systemModuleParamService;

    private List<SystemModuleParam> systemModuleParams;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/system-module-param.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("name", "maxUploadSizeMB");
        orderProperties = new ArrayList<>();
        orderProperties.add("value");
    }

    @Test
    public void getAll_ShouldReturnAllSystemModuleParams() {
        systemModuleParams = systemModuleParamService.getAll();
        assertNotNull(systemModuleParams);
        assertEquals(5, systemModuleParams.size());
        assertEquals("3", systemModuleParams.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSystemModuleParams_UsingPropertyNameAndValue() {
        systemModuleParams = systemModuleParamService.getAllMatching("name", "maxUploadSizeMB");
        assertNotNull(systemModuleParams);
        assertEquals(3, systemModuleParams.size());
        assertEquals("5", systemModuleParams.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSystemModuleParams_UsingAMap() {
        systemModuleParams = systemModuleParamService.getAllMatching(propertyValues);
        assertNotNull(systemModuleParams);
        assertEquals(3, systemModuleParams.size());
        assertEquals("4", systemModuleParams.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSystemModuleParams_UsingAnOrderProperty() {
        systemModuleParams = systemModuleParamService.getAllOrdered("value", false);
        assertNotNull(systemModuleParams);
        assertEquals(5, systemModuleParams.size());
        assertEquals("5", systemModuleParams.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        systemModuleParams = systemModuleParamService.getAllOrdered(orderProperties, true);
        assertNotNull(systemModuleParams);
        assertEquals(5, systemModuleParams.size());
        assertEquals("1", systemModuleParams.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSystemModuleParams_UsingPropertyNameAndValueAndAnOrderProperty() {
        systemModuleParams = systemModuleParamService.getAllMatchingOrdered("name", "defaultLanguage", "id", true);
        assertNotNull(systemModuleParams);
        assertEquals(1, systemModuleParams.size());
        assertEquals("3", systemModuleParams.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSystemModuleParams_UsingPropertyNameAndValueAndAList() {
        systemModuleParams = systemModuleParamService.getAllMatchingOrdered("name", "maxUploadSizeMB", orderProperties,
                true);
        assertNotNull(systemModuleParams);
        assertEquals(3, systemModuleParams.size());
        assertEquals("2", systemModuleParams.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSystemModuleParams_UsingAMapAndAnOrderProperty() {
        systemModuleParams = systemModuleParamService.getAllMatchingOrdered(propertyValues, "id", true);
        assertNotNull(systemModuleParams);
        assertEquals(3, systemModuleParams.size());
        assertEquals("2", systemModuleParams.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSystemModuleParams_UsingAMapAndAList() {
        systemModuleParams = systemModuleParamService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(systemModuleParams);
        assertEquals(3, systemModuleParams.size());
        assertEquals("5", systemModuleParams.get(2).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfSystemModuleParams_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemModuleParams = systemModuleParamService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= systemModuleParams.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSystemModuleParams_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemModuleParams = systemModuleParamService.getMatchingPage("value", "en", 1);
        assertTrue(NUMBER_OF_PAGES >= systemModuleParams.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfSystemModuleParams_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemModuleParams = systemModuleParamService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= systemModuleParams.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSystemModuleParams_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemModuleParams = systemModuleParamService.getOrderedPage("value", true, 1);
        assertTrue(NUMBER_OF_PAGES >= systemModuleParams.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfSystemModuleParams_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemModuleParams = systemModuleParamService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= systemModuleParams.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSystemModuleParams_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemModuleParams = systemModuleParamService.getMatchingOrderedPage("name", "enableFeatureX", "value", true,
                1);
        assertTrue(NUMBER_OF_PAGES >= systemModuleParams.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSystemModuleParams_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemModuleParams = systemModuleParamService.getMatchingOrderedPage("name", "enableFeatureX", orderProperties,
                true, 1);
        assertTrue(NUMBER_OF_PAGES >= systemModuleParams.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSystemModuleParams_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemModuleParams = systemModuleParamService.getMatchingOrderedPage(propertyValues, "value", false, 1);
        assertTrue(NUMBER_OF_PAGES >= systemModuleParams.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfSystemModuleParams_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        systemModuleParams = systemModuleParamService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= systemModuleParams.size());
    }

    @Test
    public void delete_ShouldDeleteAPatientType() {
        systemModuleParams = systemModuleParamService.getAll();
        assertEquals(5, systemModuleParams.size());
        SystemModuleParam systemModuleParam = systemModuleParamService.get("2");
        systemModuleParamService.delete(systemModuleParam);
        List<SystemModuleParam> newSystemModuleParams = systemModuleParamService.getAll();
        assertEquals(4, newSystemModuleParams.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSystemModuleParams() {
        systemModuleParams = systemModuleParamService.getAll();
        assertEquals(5, systemModuleParams.size());
        systemModuleParamService.deleteAll(systemModuleParams);
        List<SystemModuleParam> updatedSystemModuleParams = systemModuleParamService.getAll();
        assertTrue(updatedSystemModuleParams.isEmpty());
    }

}
