package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.controller.pathology.PathologyDictionaryRestController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The diagnosis search as the screen calls it: over the real dictionary, behind
 * the chain that asks for a signed-in user.
 */
public class PathologyDictionaryRestControllerTest extends BaseWebContextSensitiveTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnnotationConfigWebApplicationContext securityContext;
    private PathologyFindingsDictionary dictionary;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {

        /** Built over the parent's own services, as the other pathology controllers. */
        @Bean
        PathologyDictionaryRestController pathologyDictionaryRestController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory()
                    .createBean(PathologyDictionaryRestController.class);
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Before
    public void seedAndOpenTheChain() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        authenticateAs("technician1");
        dictionary = PathologyFindingsDictionary.seed(jdbcTemplate);
        securityContext = new AnnotationConfigWebApplicationContext();
        securityContext.setParent(webApplicationContext);
        securityContext.setServletContext(webApplicationContext.getServletContext());
        securityContext.register(TestConfig.class);
        securityContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();
    }

    @After
    public void closeTheChainAndTheDictionary() {
        if (securityContext != null) {
            securityContext.close();
        }
        dictionary.close();
    }

    /** The answer is the items, each with id, name and code, and the total. */
    @Test
    public void answersItemsAndTotal() throws Exception {
        MockHttpServletResponse response = mockMvc
                .perform(get("/rest/pathology/conclusionEntries").param("q", "aden").with(user("technician1")))
                .andReturn().getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals("one entry matches", 1, body.get("total").asInt());
        assertEquals(1, body.get("items").size());
        JsonNode item = body.get("items").get(0);
        assertEquals("9101", item.get("id").asText());
        assertEquals("Adenocarcinoma", item.get("value").asText());
        assertEquals("C80.1", item.get("code").asText());
    }
}
