package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionaryterminology.service.DictionaryTerminologyMappingService;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;

@RunWith(MockitoJUnitRunner.class)
public class TerminologyTransformServiceAnswerTest {

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private DictionaryTerminologyMappingService answerTerminology;
    @InjectMocks
    private TerminologyTransformServiceImpl service;

    private Dictionary positive;

    @Before
    public void answer() {
        when(fhirConfig.getOeFhirSystem()).thenReturn("https://openelis.example");
        positive = new Dictionary();
        positive.setId("501");
        positive.setDictEntry("Positive");
    }

    @Test
    public void aCodedResultCarriesEverySystemItsAnswerHasThenItsOwnEntry() {
        when(answerTerminology.getActiveByDictionaryId("501")).thenReturn(List.of(
                mapping("SNOMED", "10828004", "BROADER_THAN"), mapping("LOINC", "LA6576-8", "SAME_AS"),
                mapping("CIEL", "703", "SAME_AS")));

        CodeableConcept concept = service.transformAnswerToCodeableConcept(positive);

        assertEquals(List.of("http://loinc.org|LA6576-8", "https://openconceptlab.org/orgs/CIEL/sources/CIEL|703",
                "http://snomed.info/sct|10828004", "https://openelis.example/dictionary_entry|Positive"),
                concept.getCoding().stream().map(coding -> coding.getSystem() + "|" + coding.getCode()).toList());
    }

    @Test
    public void anAnswerKnownOnlyByItsLegacyLoincCodeKeepsIt() {
        positive.setLoincCode("LA6576-8");
        when(answerTerminology.getActiveByDictionaryId("501")).thenReturn(List.of());

        CodeableConcept concept = service.transformAnswerToCodeableConcept(positive);

        assertEquals(List.of("http://loinc.org|LA6576-8", "https://openelis.example/dictionary_entry|Positive"),
                concept.getCoding().stream().map(coding -> coding.getSystem() + "|" + coding.getCode()).toList());
    }

    private static DictionaryTerminologyMapping mapping(String source, String code, String relationship) {
        DictionaryTerminologyMapping mapping = new DictionaryTerminologyMapping();
        mapping.setDictionaryId("501");
        mapping.setSource(source);
        mapping.setCode(code);
        mapping.setRelationship(relationship);
        return mapping;
    }
}
