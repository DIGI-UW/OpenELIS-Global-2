package org.openelisglobal.county;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.dbunit.DatabaseUnitException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.county.service.CountyService;
import org.openelisglobal.county.valueholder.County;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class CountyServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CountyService countyService;

    private JdbcTemplate jdbcTemplate;
    private List<County> countyList;
    private static int NUMBER_OF_PAGES = 0;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Before
    public void setUp() {
        jdbcTemplate.update("INSERT INTO region(id, region) VALUES (1001, 'Northern Province');");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS county (id NUMERIC(10) PRIMARY KEY, "
                + "lastupdated TIMESTAMP, region_id VARCHAR(10),county VARCHAR(75) NOT NULL)");
        jdbcTemplate.update("INSERT INTO county (id, lastupdated, region_id, county)"
                + "VALUES (2001, CURRENT_TIMESTAMP, '1001', 'Greenfield');");
        jdbcTemplate.update("INSERT INTO county (id, lastupdated, region_id, county)"
                + "VALUES (2002, '2024-06-10 11:45:00', '1001', 'Marryland');");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("county");
    }

    @After
    public void cleanUp() throws SQLException, DatabaseUnitException {
        cleanRowsInCurrentConnection(new String[] { "county", "region" });
    }

    @Test
    public void getAll_ShouldReturnAllCounties() {
        countyList = countyService.getAll();
        assertNotNull(countyList);
        assertEquals(2, countyList.size());
        assertEquals("2002", countyList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingCounties_UsingPropertyNameAndValue() {
        countyList = countyService.getAllMatching("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00"));
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingCounties_UsingAMap() {
        countyList = countyService.getAllMatching(propertyValues);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedCounties_UsingAnOrderProperty() {
        countyList = countyService.getAllOrdered("county", false);
        assertNotNull(countyList);
        assertEquals(2, countyList.size());
        assertEquals("2001", countyList.get(0).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        countyList = countyService.getAllOrdered(orderProperties, true);
        assertNotNull(countyList);
        assertEquals(2, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCounties_UsingPropertyNameAndValueAndAnOrderProperty() {
        countyList = countyService.getAllMatchingOrdered("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00"),
                "lastupdated", true);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCounties_UsingPropertyNameAndValueAndAList() {
        countyList = countyService.getAllMatchingOrdered("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00"),
                orderProperties, true);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCounties_UsingAMapAndAnOrderProperty() {
        countyList = countyService.getAllMatchingOrdered(propertyValues, "county", true);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedCounties_UsingAMapAndAList() {
        countyList = countyService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(countyList);
        assertEquals(1, countyList.size());
        assertEquals("2002", countyList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfCounties_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfCounties_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingPage("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00"), 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfCounties_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfCounties_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getOrderedPage("lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfCounties_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCounties_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingOrderedPage("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00"),
                "lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCounties_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingOrderedPage("lastupdated", Timestamp.valueOf("2024-06-10 11:45:00"),
                orderProperties, true, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCounties_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingOrderedPage(propertyValues, "county", false, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfCounties_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        countyList = countyService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= countyList.size());
    }

    @Test
    public void delete_ShouldDeleteACounty() {
        countyList = countyService.getAll();
        assertEquals(2, countyList.size());
        County county = countyService.get("2001");
        countyService.delete(county);
        List<County> newCountyList = countyService.getAll();
        assertEquals(1, newCountyList.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllCounties() {
        countyList = countyService.getAll();
        assertEquals(2, countyList.size());
        countyService.deleteAll(countyList);
        List<County> updateCountyList = countyService.getAll();
        assertTrue(updateCountyList.isEmpty());
    }
}
