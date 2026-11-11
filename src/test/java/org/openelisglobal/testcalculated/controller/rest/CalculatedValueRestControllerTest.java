package org.openelisglobal.testcalculated.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.testcalculated.service.CalculationRuleViolation;
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * OGC-655 — lock the round-trip contract for the Toggle Rule: - POST
 * /rest/deactivate-test-calculation/{id} flips active → false - POST
 * /rest/activate-test-calculation/{id} flips active → true - Both return proper
 * HTTP status (no more silent empty-catch) - GET /rest/test-calculations seeds
 * `toggled` from `active`
 */
@RunWith(MockitoJUnitRunner.class)
public class CalculatedValueRestControllerTest {

    @Mock
    private TestCalculationService testCalculationService;

    @InjectMocks
    private CalculatedValueRestController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void deactivate_persistsActiveFalse_returns200() throws Exception {
        Calculation existing = new Calculation();
        existing.setId(7);
        existing.setActive(true);
        when(testCalculationService.get(7)).thenReturn(existing);

        mockMvc.perform(post("/rest/deactivate-test-calculation/7")).andExpect(status().isOk());

        ArgumentCaptor<Calculation> captor = ArgumentCaptor.forClass(Calculation.class);
        verify(testCalculationService).update(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().getActive());
    }

    @Test
    public void activate_persistsActiveTrue_returns200() throws Exception {
        Calculation existing = new Calculation();
        existing.setId(7);
        existing.setActive(false);
        when(testCalculationService.get(7)).thenReturn(existing);

        mockMvc.perform(post("/rest/activate-test-calculation/7")).andExpect(status().isOk());

        ArgumentCaptor<Calculation> captor = ArgumentCaptor.forClass(Calculation.class);
        verify(testCalculationService).update(captor.capture());
        assertEquals(Boolean.TRUE, captor.getValue().getActive());
    }

    @Test
    public void deactivate_unknownRule_returns404_doesNotUpdate() throws Exception {
        when(testCalculationService.get(99)).thenReturn(null);

        mockMvc.perform(post("/rest/deactivate-test-calculation/99")).andExpect(status().isNotFound());

        verify(testCalculationService, never()).update(any(Calculation.class));
    }

    /**
     * Previously the deactivate endpoint had an empty {@code catch (Exception e)}
     * that swallowed every failure and returned 200. The FE silently believed the
     * rule was deactivated when the BE never persisted the change.
     */
    @Test
    public void deactivate_persistenceFailure_returns500() throws Exception {
        Calculation existing = new Calculation();
        existing.setId(7);
        existing.setActive(true);
        when(testCalculationService.get(7)).thenReturn(existing);
        doThrow(new RuntimeException("db down")).when(testCalculationService).update(any(Calculation.class));

        mockMvc.perform(post("/rest/deactivate-test-calculation/7")).andExpect(status().isInternalServerError());
    }

    @Test
    public void getReflexRules_seedsToggledFromActive() throws Exception {
        Calculation activeRule = new Calculation();
        activeRule.setId(1);
        activeRule.setActive(true);
        activeRule.setOperations(new ArrayList<>());
        Calculation inactiveRule = new Calculation();
        inactiveRule.setId(2);
        inactiveRule.setActive(false);
        inactiveRule.setOperations(new ArrayList<>());
        when(testCalculationService.getAll()).thenReturn(Arrays.asList(activeRule, inactiveRule));

        mockMvc.perform(get("/rest/test-calculations")).andExpect(status().isOk());

        // Captured calls happened — assert the entities now carry toggled mirroring
        // active.
        verify(testCalculationService, times(1)).getAll();
        assertEquals(Boolean.TRUE, activeRule.getToggled());
        assertEquals(Boolean.FALSE, inactiveRule.getToggled());
    }

    /**
     * A link from the Test Editor's Reflex &amp; Calc section names one
     * calculation, so the endpoint must answer with that calculation alone rather
     * than the whole collection the editor was opened from.
     */
    @Test
    public void getWithId_returnsOnlyThatCalculation() throws Exception {
        Calculation wanted = new Calculation();
        wanted.setId(7);
        wanted.setActive(true);
        wanted.setOperations(new ArrayList<>());
        Calculation other = new Calculation();
        other.setId(8);
        other.setActive(true);
        other.setOperations(new ArrayList<>());
        when(testCalculationService.getAll()).thenReturn(new ArrayList<>(Arrays.asList(wanted, other)));

        mockMvc.perform(get("/rest/test-calculations?id=7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(7));
    }

    /** No id keeps the existing behaviour: the whole collection. */
    @Test
    public void getWithoutId_returnsEveryCalculation() throws Exception {
        Calculation one = new Calculation();
        one.setId(7);
        one.setActive(true);
        one.setOperations(new ArrayList<>());
        Calculation two = new Calculation();
        two.setId(8);
        two.setActive(false);
        two.setOperations(new ArrayList<>());
        when(testCalculationService.getAll()).thenReturn(new ArrayList<>(Arrays.asList(one, two)));

        mockMvc.perform(get("/rest/test-calculations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /** A blank id is not a filter — it must not empty the list. */
    @Test
    public void getWithBlankId_returnsEveryCalculation() throws Exception {
        Calculation one = new Calculation();
        one.setId(7);
        one.setActive(true);
        one.setOperations(new ArrayList<>());
        when(testCalculationService.getAll()).thenReturn(new ArrayList<>(Arrays.asList(one)));

        mockMvc.perform(get("/rest/test-calculations?id=")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    /**
     * OGC-1587: the rule the list serves, posted back unchanged, saves. The list
     * carries the derived scopedSampleTypeId, which the save used to refuse as an
     * unreadable request.
     */
    @Test
    public void aRuleReadFromTheListSavesBackUnchanged() throws Exception {
        Calculation stored = new Calculation();
        stored.setId(7);
        stored.setName("Urea from creatinine");
        stored.setTestId(411);
        stored.setActive(true);
        Operation operand = new Operation();
        operand.setOrder(0);
        operand.setType(Operation.OperationType.TEST_RESULT);
        operand.setValue("160");
        operand.setSampleId(2);
        stored.setOperations(new ArrayList<>(List.of(operand)));
        when(testCalculationService.getAll()).thenReturn(new ArrayList<>(List.of(stored)));
        when(testCalculationService.get(7)).thenReturn(stored);
        String served = mockMvc.perform(get("/rest/test-calculations?id=7")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].operations[0].scopedSampleTypeId").value("2")).andReturn().getResponse()
                .getContentAsString();
        String rule = served.substring(1, served.length() - 1);

        mockMvc.perform(post("/rest/test-calculation").contentType(MediaType.APPLICATION_JSON).content(rule))
                .andExpect(status().isOk());

        ArgumentCaptor<Calculation> captor = ArgumentCaptor.forClass(Calculation.class);
        verify(testCalculationService).update(captor.capture());
        assertEquals(Integer.valueOf(2), captor.getValue().getOperations().get(0).getSampleId());
    }

    @Test
    public void aRefusedRuleAnswers400WithTheReasonAndIsNotSaved() throws Exception {
        doThrow(new CalculationRuleViolation("calculation.error.cycle", Map.of("cycle", "A → B → A")))
                .when(testCalculationService).validateRule(any(Calculation.class), eq(true));

        mockMvc.perform(post("/rest/test-calculation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"testId\":1,\"active\":true,\"operations\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.messageKey").value("calculation.error.cycle"))
                .andExpect(jsonPath("$.messageArgs.cycle").value("A → B → A"));

        verify(testCalculationService, never()).save(any(Calculation.class));
        verify(testCalculationService, never()).update(any(Calculation.class));
    }

    /** A rule saved switched off is checked as one that will not run. */
    @Test
    public void anInactiveRuleIsCheckedAsInactive() throws Exception {
        mockMvc.perform(post("/rest/test-calculation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"testId\":1,\"active\":false,\"operations\":[]}"))
                .andExpect(status().isOk());

        verify(testCalculationService).validateRule(any(Calculation.class), eq(false));
        verify(testCalculationService).save(any(Calculation.class));
    }

    /** Saving over a rule that no longer exists used to answer 200 and save nothing. */
    @Test
    public void savingOverAMissingRuleAnswers404() throws Exception {
        when(testCalculationService.get(99)).thenReturn(null);

        mockMvc.perform(post("/rest/test-calculation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":99,\"name\":\"A\",\"testId\":1,\"active\":true,\"operations\":[]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messageKey").value("calculation.error.notFound"));

        verify(testCalculationService, never()).update(any(Calculation.class));
        verify(testCalculationService, never()).save(any(Calculation.class));
    }

    /**
     * The service answers a missing id by throwing, as the real one does; that
     * used to come back as a 500 on save, activate and deactivate alike.
     */
    @Test
    public void aMissingRuleIsNotFoundWhenTheServiceThrows() throws Exception {
        when(testCalculationService.get(99)).thenThrow(new ObjectNotFoundException(99, Calculation.class.getName()));

        mockMvc.perform(post("/rest/test-calculation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":99,\"name\":\"A\",\"testId\":1,\"active\":true,\"operations\":[]}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/rest/activate-test-calculation/99")).andExpect(status().isNotFound());
        mockMvc.perform(post("/rest/deactivate-test-calculation/99")).andExpect(status().isNotFound());

        verify(testCalculationService, never()).update(any(Calculation.class));
        verify(testCalculationService, never()).save(any(Calculation.class));
    }

    @Test
    public void switchingOnARefusedRuleAnswers400AndLeavesItOff() throws Exception {
        Calculation existing = new Calculation();
        existing.setId(7);
        existing.setActive(false);
        when(testCalculationService.get(7)).thenReturn(existing);
        doThrow(new CalculationRuleViolation("calculation.error.duplicateDestination",
                Map.of("test", "Urea", "other", "Urea x2"))).when(testCalculationService).validateRule(existing, true);

        mockMvc.perform(post("/rest/activate-test-calculation/7")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messageKey").value("calculation.error.duplicateDestination"));

        verify(testCalculationService, never()).update(any(Calculation.class));
        assertEquals(Boolean.FALSE, existing.getActive());
    }

    /**
     * Switching a rule off never needs a check: a rule that does not run conflicts
     * with nothing.
     */
    @Test
    public void switchingOffIsNeverRefused() throws Exception {
        Calculation existing = new Calculation();
        existing.setId(7);
        existing.setActive(true);
        when(testCalculationService.get(7)).thenReturn(existing);

        mockMvc.perform(post("/rest/deactivate-test-calculation/7")).andExpect(status().isOk());

        verify(testCalculationService, never()).validateRule(any(Calculation.class), anyBoolean());
    }
}
