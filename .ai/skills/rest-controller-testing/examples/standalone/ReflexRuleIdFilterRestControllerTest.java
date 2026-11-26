package org.openelisglobal.testcatalog.controller.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.controller.rest.TestReflexRuleRestController;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The Test Catalog editor's Reflex &amp; Calc section links to one reflex rule,
 * so GET /rest/reflexrules has to be able to answer with that rule alone
 * instead of the whole collection the link was followed from.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReflexRuleIdFilterRestControllerTest {

    @Mock
    private TestReflexService reflexService;

    @Mock
    private DictionaryService dictionaryService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @InjectMocks
    private TestReflexRuleRestController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static ReflexRule rule(int id, String name) {
        ReflexRule rule = new ReflexRule();
        rule.setId(id);
        rule.setRuleName(name);
        rule.setConditions(new HashSet<>());
        rule.setActions(new HashSet<>());
        return rule;
    }

    /** The id arrives as a query string, while the rule's own id is numeric. */
    @Test
    public void getWithId_returnsOnlyThatRule() throws Exception {
        when(reflexService.getAllReflexRules()).thenReturn(Arrays.asList(rule(11, "wanted"), rule(12, "other")));

        mockMvc.perform(get("/rest/reflexrules?id=11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].ruleName").value("wanted"));
    }

    @Test
    public void getWithoutId_returnsEveryRule() throws Exception {
        when(reflexService.getAllReflexRules()).thenReturn(Arrays.asList(rule(11, "one"), rule(12, "two")));

        mockMvc.perform(get("/rest/reflexrules")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /** A blank id is not a filter — it must not empty the list. */
    @Test
    public void getWithBlankId_returnsEveryRule() throws Exception {
        when(reflexService.getAllReflexRules()).thenReturn(Arrays.asList(rule(11, "one"), rule(12, "two")));

        mockMvc.perform(get("/rest/reflexrules?id=")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /** An id that matches nothing yields an empty list, not the whole collection. */
    @Test
    public void getWithUnknownId_returnsNothing() throws Exception {
        when(reflexService.getAllReflexRules()).thenReturn(Arrays.asList(rule(11, "one")));

        mockMvc.perform(get("/rest/reflexrules?id=999")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
