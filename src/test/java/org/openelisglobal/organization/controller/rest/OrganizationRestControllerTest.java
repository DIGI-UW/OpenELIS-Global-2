package org.openelisglobal.organization.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class OrganizationRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String ORG_ID = "9981";
    // Microseconds, as now() stores them; the form sends back milliseconds.
    private static final String SEEDED_LASTUPDATED = "2026-08-19 13:30:13.405194";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    private JdbcTemplate jdbc;

    private MockMvc organizationMvc;

    @Before
    public void seedOrganization() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        super.setUp();
        // The controller constructor needs these; other fixtures replace address_part.
        for (String part : List.of("commune", "village")) {
            jdbc.update(
                    "INSERT INTO clinlims.address_part (id, part_name)"
                            + " SELECT nextval('clinlims.address_part_seq'), ?"
                            + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.address_part WHERE part_name = ?)",
                    part, part);
        }
        // The shared test context does not scan the organization controllers.
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        organizationMvc = MockMvcBuilders.standaloneSetup(beanFactory.createBean(OrganizationRestController.class))
                .setValidator(validator).build();
        jdbc.update(
                "INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                        + " VALUES (?, 'Seeded lab', 'N', 'Y', ?::timestamp)",
                Long.valueOf(ORG_ID), SEEDED_LASTUPDATED);
    }

    @After
    public void removeOrganization() {
        jdbc.update("DELETE FROM clinlims.organization_organization_type WHERE org_id = ?", Long.valueOf(ORG_ID));
        jdbc.update("DELETE FROM clinlims.organization WHERE id = ?", Long.valueOf(ORG_ID));
    }

    @Test
    public void aRowSeededWithMicrosecondsCanBeSavedFromTheForm() throws Exception {
        save("Section 12, Lot 34, Boram Road").andExpect(status().isOk());

        assertEquals("Section 12, Lot 34, Boram Road", jdbc.queryForObject(
                "SELECT street_address FROM clinlims.organization WHERE id = ?", String.class, Long.valueOf(ORG_ID)));
    }

    @Test
    public void aStreetAddressLongerThanItsColumnIsRefusedAsInvalid() throws Exception {
        save("Section 12, Lot 34, Boram Road, Wewak").andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions save(String streetAddress) throws Exception {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("id", ORG_ID);
        form.put("organizationName", "Seeded lab");
        form.put("isActive", "Y");
        form.put("mlsSentinelLabFlag", "N");
        form.put("streetAddress", streetAddress);
        form.put("selectedTypes", List.of());
        form.put("lastupdated", Timestamp.valueOf(SEEDED_LASTUPDATED).getTime());
        return organizationMvc.perform(post("/rest/Organization").param("ID", ORG_ID)
                .contentType(MediaType.APPLICATION_JSON).content(mapToJson(form)).session(authenticatedSession()));
    }

    private MockHttpSession authenticatedSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN").build();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return session;
    }
}
