package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroDstForm;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;

public class MicroMolecularDstCatalogServiceTest {
    @Mock
    private MicroMolecularDstBindingDAO bindings;
    @Mock
    private MicroAntibioticDAO antibiotics;
    @Mock
    private TestService tests;
    @Mock
    private TestResultComponentService components;
    @Mock
    private TestResultService options;
    @Mock
    private DictionaryService dictionaries;
    @InjectMocks
    private MicroMolecularDstCatalogService service;
    private MicroDstForm.Binding request;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        var test = new org.openelisglobal.test.valueholder.Test();
        test.setId("test");
        test.setName("Molecular test");
        when(tests.getAllActiveTests(false)).thenReturn(List.of(test));
        var component = new org.openelisglobal.testresultcomponent.valueholder.TestResultComponent();
        component.setId("component");
        component.setLabel("Drug result");
        when(components.getActiveComponentsByTestId("test")).thenReturn(List.of(component));
        var susceptible = new org.openelisglobal.testresult.valueholder.TestResult();
        susceptible.setValue("S-code");
        susceptible.setTestResultType("A");
        var resistant = new org.openelisglobal.testresult.valueholder.TestResult();
        resistant.setValue("R-code");
        resistant.setTestResultType("A");
        when(options.getActiveOptionsByComponentId("component")).thenReturn(List.of(susceptible, resistant));
        var drug = new MicroAntibiotic();
        drug.setId("drug");
        drug.setResistanceRole("RIFAMPICIN");
        when(antibiotics.getActiveAntibiotics()).thenReturn(List.of(drug));
        request = new MicroDstForm.Binding();
        request.testId = "test";
        request.componentId = "component";
        request.antibioticId = "drug";
        request.susceptibleValue = "S-code";
        request.resistantValue = "R-code";
    }

    @Test
    public void persistedMappingUsesExactComponentCodesAndActor() {
        service.save(request, "admin");
        var captured = ArgumentCaptor.forClass(MicroMolecularDstBinding.class);
        verify(bindings).insert(captured.capture());
        assertEquals("S-code", captured.getValue().getSusceptibleValue());
        assertEquals("R-code", captured.getValue().getResistantValue());
        assertEquals("component", captured.getValue().getComponentId());
        assertEquals("admin", captured.getValue().getSysUserId());
    }

    @Test
    public void anotherTestsComponentCannotBeBound() {
        request.componentId = "foreign-component";
        assertEquals("DST_BINDING_COMPONENT_INVALID",
                assertThrows(IllegalArgumentException.class, () -> service.save(request, "admin")).getMessage());
        verify(bindings, never()).insert(any());
    }

    @Test
    public void valuesMustBeDistinctAndBelongToTheConfiguredComponent() {
        request.resistantValue = "S-code";
        assertThrows(IllegalArgumentException.class, () -> service.save(request, "admin"));
        request.resistantValue = "foreign-code";
        assertThrows(IllegalArgumentException.class, () -> service.save(request, "admin"));
        verify(bindings, never()).insert(any());
    }
}
