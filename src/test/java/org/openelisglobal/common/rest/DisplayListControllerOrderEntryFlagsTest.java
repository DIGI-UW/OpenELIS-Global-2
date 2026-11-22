package org.openelisglobal.common.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * OGC-1424: order entry learns from {@code /rest/configuration-properties}
 * whether to show fax fields, the order-level payment status and the billing
 * reference, and the billing label never breaks the endpoint when its
 * localization is not set (the shipped default is "-1").
 */
public class DisplayListControllerOrderEntryFlagsTest extends BaseWebContextSensitiveTest {

    private static final String ENDPOINT = "/rest/configuration-properties";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnnotationConfigWebApplicationContext controllerContext;

    private String showFaxBefore;
    private String trackPaymentBefore;
    private String billingLabelBefore;

    @Autowired
    private DataSource dataSource;

    @Configuration
    @EnableWebMvc
    static class TestConfig {
        @Bean
        DisplayListController displayListController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(DisplayListController.class);
        }
    }

    @Before
    public void init() throws Exception {
        ConfigurationProperties properties = ConfigurationProperties.getInstance();
        showFaxBefore = properties.getPropertyValue(Property.SHOW_FAX_FIELDS);
        trackPaymentBefore = properties.getPropertyValue(Property.TRACK_PATIENT_PAYMENT);
        billingLabelBefore = properties.getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL);
        ensureBannerLocalization();
        controllerContext = new AnnotationConfigWebApplicationContext();
        controllerContext.setParent(webApplicationContext);
        controllerContext.setServletContext(webApplicationContext.getServletContext());
        controllerContext.register(TestConfig.class);
        controllerContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(controllerContext).build();
    }

    @After
    public void tearDown() {
        ConfigurationProperties properties = ConfigurationProperties.getInstance();
        properties.setPropertyValue(Property.SHOW_FAX_FIELDS, showFaxBefore == null ? "false" : showFaxBefore);
        properties.setPropertyValue(Property.TRACK_PATIENT_PAYMENT,
                trackPaymentBefore == null ? "false" : trackPaymentBefore);
        properties.setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL,
                billingLabelBefore == null ? "-1" : billingLabelBefore);
        if (controllerContext != null) {
            controllerContext.close();
        }
    }

    @Test
    public void theOrderEntrySwitchesReachTheBrowser() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.SHOW_FAX_FIELDS, "true");
        ConfigurationProperties.getInstance().setPropertyValue(Property.TRACK_PATIENT_PAYMENT, "true");

        Map<String, Object> configs = configs();

        assertEquals("true", configs.get("SHOW_FAX_FIELDS"));
        assertEquals("true", configs.get("TRACK_PATIENT_PAYMENT"));
        assertEquals(ConfigurationProperties.getInstance().getPropertyValue(Property.USE_BILLING_REFERENCE_NUMBER),
                configs.get("USE_BILLING_REFERENCE_NUMBER"));
    }

    @Test
    public void faxFieldsAreOffUnlessSwitchedOn() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.SHOW_FAX_FIELDS, "false");

        assertEquals("false", configs().get("SHOW_FAX_FIELDS"));
    }

    @Test
    public void anUnsetBillingLabelIsBlankAndTheEndpointStillAnswers() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "-1");

        assertEquals("", configs().get("BILLING_REFERENCE_NUMBER_LABEL"));
    }

    private void ensureBannerLocalization() {
        String id = ConfigurationProperties.getInstance().getPropertyValue(Property.BANNER_TEXT);
        if (id == null || id.isBlank()) {
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer present = jdbc.queryForObject("SELECT count(*) FROM clinlims.localization WHERE id = ?", Integer.class,
                Long.valueOf(id));
        if (present != null && present > 0) {
            return;
        }
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                Long.valueOf(id), "banner heading");
    }

    private Map<String, Object> configs() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get(ENDPOINT)).andReturn().getResponse();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        return objectMapper.readValue(response.getContentAsString(), new TypeReference<Map<String, Object>>() {
        });
    }
}
