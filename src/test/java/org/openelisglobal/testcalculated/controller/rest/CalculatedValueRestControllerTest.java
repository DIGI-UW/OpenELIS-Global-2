package org.openelisglobal.testcalculated.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
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
}
