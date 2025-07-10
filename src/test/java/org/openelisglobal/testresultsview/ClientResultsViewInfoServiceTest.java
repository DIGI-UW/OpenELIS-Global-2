package org.openelisglobal.testresultsview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.testresultsview.service.ClientResultsViewInfoService;
import org.openelisglobal.testresultsview.valueholder.ClientResultsViewBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class ClientResultsViewInfoServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ClientResultsViewInfoService clientResultsViewInfoService;

    private JdbcTemplate jdbcTemplate;
    private List<ClientResultsViewBean> clientResultsViewInfoList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Autowired
    public void setJdbcTemplate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Before
    public void setUp() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS client_results_view_seq START WITH 1 INCREMENT BY 1");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS result (id numeric(10,0) NOT NULL,"
                + "analysis_id numeric(10,0), sort_order numeric, is_reportable character varying(1),"
                + "result_type character varying(1),value character varying(200), analyte_id numeric(10,0),"
                + "test_result_id numeric(10,0), lastupdated timestamp(6) without time zone,"
                + "min_normal double precision, max_normal double precision, parent_id numeric(10,0),"
                + "significant_digits numeric DEFAULT 0, \"grouping\" numeric DEFAULT 0);");

        jdbcTemplate.update("INSERT INTO result (id,sort_order,is_reportable,result_type) VALUES (1001, 1, 'Y','N');");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS client_results_view ("
                + "id INTEGER PRIMARY KEY DEFAULT nextval('client_results_view_seq'), password TEXT, "
                + "result_id VARCHAR(10), "
                + "CONSTRAINT fk_client_results_view FOREIGN KEY (result_id) REFERENCES result(id));");

        jdbcTemplate.update("INSERT INTO client_results_view (id, password, result_id ) "
                + "VALUES ( 7001, 'encrypted-password-string', 1001);");
        jdbcTemplate.update("INSERT INTO client_results_view (id, password, result_id ) "
                + "VALUES ( 7002, 'encryptedpassword', 1001);");

        propertyValues = new HashMap<>();
        propertyValues.put("result", 1001);
        orderProperties = new ArrayList<>();
        orderProperties.add("password");
    }

    @After
    public void cleaUp() {
        jdbcTemplate.execute("DROP TABLE result CASCADE ");
        jdbcTemplate.execute("DROP TABLE client_results_view CASCADE ");
        jdbcTemplate.execute("DROP SEQUENCE client_results_view_seq");
    }

    @Test
    public void getAll_ShouldReturnAllClientResultsViewBeans() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAll();
        assertNotNull(clientResultsViewInfoList);
        assertEquals(2, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingClientResultsViewBeans_UsingPropertyNameAndValue() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatching("password",
                "encrypted-password-string");
        assertNotNull(clientResultsViewInfoList);
        assertEquals(1, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(0).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingClientResultsViewBeans_UsingAMap() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatching(propertyValues);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(2, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedClientResultsViewBeans_UsingAnOrderProperty() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllOrdered("id", false);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(2, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7001"), clientResultsViewInfoList.get(0).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllOrdered(orderProperties, true);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(2, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedClientResultsViewBeans_UsingPropertyNameAndValueAndAnOrderProperty() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatchingOrdered("password", "encryptedpassword",
                "result", false);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(1, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedClientResultsViewBeans_UsingPropertyNameAndValueAndAList() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatchingOrdered("password", "encryptedpassword",
                orderProperties, true);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(1, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedClientResultsViewBeans_UsingAMapAndAnOrderProperty() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatchingOrdered(propertyValues, "result", true);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(2, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7001"), clientResultsViewInfoList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedClientResultsViewBeans_UsingAMapAndAList() {
        clientResultsViewInfoList = clientResultsViewInfoService.getAllMatchingOrdered(propertyValues, orderProperties,
                false);
        assertNotNull(clientResultsViewInfoList);
        assertEquals(2, clientResultsViewInfoList.size());
        assertEquals(Integer.valueOf("7002"), clientResultsViewInfoList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfClientResultsViewBeans_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfClientResultsViewBeans_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingPage("password",
                "encrypted-password-string", 1);
        assertTrue(NUMBER_OF_PAGES >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfClientResultsViewBeans_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= clientResultsViewInfoList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfClientResultsViewBeans_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getOrderedPage("id", true, 1);
        assertTrue(NUMBER_OF_PAGES >= clientResultsViewInfoList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfClientResultsViewBeans_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfClientResultsViewBeans_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingOrderedPage("password",
                "encrypted-password-string", "id", true, 1);
        assertTrue(NUMBER_OF_PAGES >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfClientResultsViewBeans_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingOrderedPage("id", "7002", orderProperties,
                true, 1);
        assertTrue(NUMBER_OF_PAGES >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfClientResultsViewBeans_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingOrderedPage(propertyValues, "password",
                false, 1);
        assertTrue(NUMBER_OF_PAGES >= clientResultsViewInfoList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfClientResultsViewBeans_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        clientResultsViewInfoList = clientResultsViewInfoService.getMatchingOrderedPage(propertyValues, orderProperties,
                false, 1);
        assertTrue(NUMBER_OF_PAGES >= clientResultsViewInfoList.size());
    }
}
