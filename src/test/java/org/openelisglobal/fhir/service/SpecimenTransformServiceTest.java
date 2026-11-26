package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Specimen;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SpecimenTransformServiceTest {

    private static final String CONDITION_TYPE_ID = "12";
    private static final String FHIR_SYSTEM = "https://example.org/openelis";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private SampleHumanService sampleHumanService;
    @Mock
    private DictionaryService dictionaryService;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private ObservationHistoryService observationHistoryService;
    @Mock
    private IStatusService statusService;
    @Mock
    private FhirCommonTransformService common;
    @Mock
    private TerminologyTransformService terminologyTransformService;

    @InjectMocks
    private SpecimenTransformServiceImpl service;

    private SampleItem item;

    @Before
    public void setUp() {
        Sample sample = new Sample();
        sample.setId("101");
        sample.setAccessionNumber("2026000101");
        item = new SampleItem();
        item.setId("201");
        item.setSample(sample);
        item.setFhirUuid(UUID.fromString("2ca63714-bab1-4e39-9f9b-1a98c24216e4"));
        when(fhirConfig.getOeFhirSystem()).thenReturn(FHIR_SYSTEM);
        when(sampleItemService.get(item.getId())).thenReturn(item);
        when(observationHistoryService.getObservationTypeIdForType(ObservationType.INITIAL_SAMPLE_CONDITION))
                .thenReturn(CONDITION_TYPE_ID);
    }

    @Test
    public void transformToSpecimen_preservesAllInitialConditionsForThisItemOnly() {
        ObservationHistory hemolyzed = condition("301", "Hemolyzed");
        ObservationHistory clotted = condition("302", "Clotted");
        ObservationHistory sampleNature = new ObservationHistory();
        sampleNature.setObservationHistoryTypeId("13");
        sampleNature.setValueType(ValueType.LITERAL);
        sampleNature.setValue("Unrelated sample nature");
        when(observationHistoryService.getObservationHistoriesBySampleItemId(item.getId()))
                .thenReturn(List.of(hemolyzed, sampleNature, clotted));

        Specimen specimen = service.transformToSpecimen(item);

        assertEquals(item.getFhirUuidAsString(), specimen.getIdElement().getIdPart());
        assertEquals(2, specimen.getCondition().size());
        assertCondition(specimen, 0, "Hemolyzed");
        assertCondition(specimen, 1, "Clotted");
        verify(observationHistoryService).getObservationHistoriesBySampleItemId(item.getId());
    }

    @Test
    public void transformToSpecimen_withoutStoredConditionsHasNoCondition() {
        when(observationHistoryService.getObservationHistoriesBySampleItemId(item.getId())).thenReturn(List.of());

        Specimen specimen = service.transformToSpecimen(item);

        assertEquals(item.getFhirUuidAsString(), specimen.getIdElement().getIdPart());
        assertFalse(specimen.hasCondition());
    }

    @Test
    public void transformToFhirSpecimen_doesNotDuplicateAlreadyPersistedConditions() throws Exception {
        ObservationHistory observation = condition("301", "Hemolyzed");
        when(observationHistoryService.getObservationHistoriesBySampleItemId(item.getId()))
                .thenReturn(List.of(observation));

        Specimen specimen = service.transformToFhirSpecimen(orderEntry(List.of(observation)));

        assertEquals(1, specimen.getCondition().size());
        assertCondition(specimen, 0, "Hemolyzed");
    }

    @Test
    public void transformToFhirSpecimen_keepsConditionsNotYetPersisted() throws Exception {
        ObservationHistory observation = condition("301", "Hemolyzed");
        when(observationHistoryService.getObservationHistoriesBySampleItemId(item.getId())).thenReturn(List.of());

        Specimen specimen = service.transformToFhirSpecimen(orderEntry(List.of(observation)));

        assertEquals(1, specimen.getCondition().size());
        assertCondition(specimen, 0, "Hemolyzed");
    }

    @Test
    public void transformToFhirSpecimen_usesStoredConditionsWhenNoListIsSupplied() throws Exception {
        ObservationHistory observation = condition("301", "Hemolyzed");
        when(observationHistoryService.getObservationHistoriesBySampleItemId(item.getId()))
                .thenReturn(List.of(observation));

        Specimen specimen = service.transformToFhirSpecimen(orderEntry(null));

        assertEquals(1, specimen.getCondition().size());
        assertCondition(specimen, 0, "Hemolyzed");
    }

    @Test
    public void transformToFhirSpecimen_usesSuppliedListWhenStoredConditionsDiffer() throws Exception {
        ObservationHistory stored = condition("301", "Hemolyzed");
        ObservationHistory supplied = condition("302", "Clotted");
        when(observationHistoryService.getObservationHistoriesBySampleItemId(item.getId())).thenReturn(List.of(stored));

        Specimen specimen = service.transformToFhirSpecimen(orderEntry(List.of(supplied)));

        assertEquals(1, specimen.getCondition().size());
        assertCondition(specimen, 0, "Clotted");
    }

    private ObservationHistory condition(String dictionaryId, String value) {
        Dictionary dictionary = new Dictionary();
        dictionary.setDictEntry(value);
        when(dictionaryService.get(dictionaryId)).thenReturn(dictionary);
        ObservationHistory observation = new ObservationHistory();
        observation.setSampleId(item.getSample().getId());
        observation.setSampleItemId(item.getId());
        observation.setObservationHistoryTypeId(CONDITION_TYPE_ID);
        observation.setValueType(ValueType.DICTIONARY);
        observation.setValue(dictionaryId);
        return observation;
    }

    private SampleTestCollection orderEntry(List<ObservationHistory> conditions) throws Exception {
        // This data holder does not use its enclosing instance. Avoid initializing
        // SampleAddService's static Spring dependencies in this isolated unit test.
        return SampleTestCollection.class
                .getConstructor(SampleAddService.class, SampleItem.class, List.class, String.class, List.class,
                        Map.class, Map.class, ObservationHistory.class)
                .newInstance(null, item, List.of(), null, conditions, Map.of(), Map.of(), null);
    }

    private void assertCondition(Specimen specimen, int index, String expectedValue) {
        Coding coding = specimen.getCondition().get(index).getCodingFirstRep();
        assertEquals(FHIR_SYSTEM + "/sample_condition", coding.getSystem());
        assertEquals(expectedValue, coding.getCode());
        assertEquals(expectedValue, coding.getDisplay());
    }
}
