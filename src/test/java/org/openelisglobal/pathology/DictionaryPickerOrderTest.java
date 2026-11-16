package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.rest.DisplayListController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The pathology pickers, the stain on a slide and the tissue in a cassette, are
 * filled from {@code /rest/dictionary/category/{name}}.
 *
 * <p>
 * The endpoint ordered every category A to Z, so the stain list the laboratory
 * is shipped in bench order, H&E first, came out alphabetically, and nothing
 * read the sort order the shipped file fills. These tests pin the category's
 * own sort order first, entries with the same order or none ordered by name as
 * the endpoint always ordered them and those with none last, so a category that
 * carries no sort order reads as it did, and retired entries left out.
 *
 * <p>
 * AppTestConfig does not component-scan {@code org.openelisglobal.common.rest},
 * so the controller is built here against the parent's real services, as
 * {@code DisplayListControllerStageFlagsTest} does. The dictionary rows are
 * written directly rather than through a dataset, which would truncate the
 * dictionary for later test classes.
 */
public class DictionaryPickerOrderTest extends BaseWebContextSensitiveTest {

    private static final int SHIPPED_CATEGORY = 9660;
    private static final String SHIPPED_NAME = "pathology_test_shipped_order";
    private static final int UNSORTED_CATEGORY = 9670;
    private static final String UNSORTED_NAME = "pathology_test_unsorted";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnnotationConfigWebApplicationContext securityContext;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {

        @Bean
        DisplayListController displayListController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(DisplayListController.class);
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Before
    public void seedCategoriesAndBuildTheController() {
        removeRows();
        DictionaryRows.insertCategory(jdbcTemplate, SHIPPED_CATEGORY, SHIPPED_NAME, SHIPPED_NAME, "PTShip");
        DictionaryRows.insertEntry(jdbcTemplate, 9661, SHIPPED_CATEGORY, "Zeta", "PT9661", true, 2);
        DictionaryRows.insertEntry(jdbcTemplate, 9662, SHIPPED_CATEGORY, "Alpha", "PT9662", true, null);
        DictionaryRows.insertEntry(jdbcTemplate, 9663, SHIPPED_CATEGORY, "Beta", "PT9663", true, 1);
        DictionaryRows.insertEntry(jdbcTemplate, 9664, SHIPPED_CATEGORY, "Mu", "PT9664", true, 2);
        DictionaryRows.insertEntry(jdbcTemplate, 9665, SHIPPED_CATEGORY, "Omega", "PT9665", true, null);
        DictionaryRows.insertEntry(jdbcTemplate, 9666, SHIPPED_CATEGORY, "Retired first", "PT9666", false, 0);
        DictionaryRows.insertCategory(jdbcTemplate, UNSORTED_CATEGORY, UNSORTED_NAME, UNSORTED_NAME, "PTUns");
        DictionaryRows.insertEntry(jdbcTemplate, 9671, UNSORTED_CATEGORY, "Skin", "PT9671", true, null);
        DictionaryRows.insertEntry(jdbcTemplate, 9672, UNSORTED_CATEGORY, "Bone", "PT9672", true, null);
        DictionaryRows.insertEntry(jdbcTemplate, 9673, UNSORTED_CATEGORY, "Lymph node", "PT9673", true, null);
        DictionaryRows.insertEntry(jdbcTemplate, 9674, UNSORTED_CATEGORY, "10 mm core", "PT9674", true, null);
        DictionaryRows.insertEntry(jdbcTemplate, 9675, UNSORTED_CATEGORY, "2 mm core", "PT9675", true, null);

        securityContext = new AnnotationConfigWebApplicationContext();
        securityContext.setParent(webApplicationContext);
        securityContext.setServletContext(webApplicationContext.getServletContext());
        securityContext.register(TestConfig.class);
        securityContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();
    }

    @After
    public void tearDown() {
        removeRows();
        if (securityContext != null) {
            securityContext.close();
        }
    }

    @Test
    public void aShippedCategory_isServedInItsSortOrder_tiesAndUnsortedByName() throws Exception {
        assertEquals("sort order first, a tie by name, entries with no order last by name, retired left out",
                List.of("Beta", "Mu", "Zeta", "Alpha", "Omega"), namesServedFor(SHIPPED_NAME));
    }

    @Test
    public void aCategoryWithNoSortOrder_isServedInTheOrderByNameItHad() throws Exception {
        assertEquals("a laboratory's own list with no order reads as it did, a leading number compared as a number",
                List.of("2 mm core", "10 mm core", "Bone", "Lymph node", "Skin"), namesServedFor(UNSORTED_NAME));
    }

    @Test
    public void anUnknownCategory_isServedEmpty() throws Exception {
        assertEquals("a category nobody created has nothing to offer", List.of(),
                namesServedFor("pathology_test_no_such_category"));
    }

    // helpers

    private List<String> namesServedFor(String categoryName) throws Exception {
        MockHttpServletResponse response = mockMvc
                .perform(get("/rest/dictionary/category/" + categoryName).with(user("admin"))).andReturn()
                .getResponse();
        assertEquals("the picker list is served", 200, response.getStatus());
        List<Map<String, Object>> entries = objectMapper.readValue(response.getContentAsString(),
                new TypeReference<List<Map<String, Object>>>() {
                });
        return entries.stream().map(entry -> (String) entry.get("value")).collect(Collectors.toList());
    }

    private void removeRows() {
        DictionaryRows.removeEntries(jdbcTemplate, 9661, 9662, 9663, 9664, 9665, 9666, 9671, 9672, 9673, 9674, 9675);
        DictionaryRows.removeCategory(jdbcTemplate, SHIPPED_CATEGORY);
        DictionaryRows.removeCategory(jdbcTemplate, UNSORTED_CATEGORY);
    }
}
