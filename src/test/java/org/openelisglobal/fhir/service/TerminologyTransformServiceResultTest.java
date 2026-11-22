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
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;

/**
 * A result's Observation carries the whole-test codings and, for a component
 * result, that component's own codings. Within one terminology system the
 * SAME_AS mapping is the equivalent concept and wins over the broader or
 * narrower ones, at the test level and at the component level alike; a mapping
 * scoped to another specimen never applies.
 */
@RunWith(MockitoJUnitRunner.class)
public class TerminologyTransformServiceResultTest {

    private static final String TEST_ID = "385";
    private static final String CT = "ct-component";
    private static final String END_POINT = "endpoint-component";
    private static final String SWAB = "39";
    private static final String SPUTUM = "32";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private TestTerminologyMappingService testTerminologyMappingService;
    @InjectMocks
    private TerminologyTransformServiceImpl service;

    private org.openelisglobal.test.valueholder.Test test;
    private TestResultComponent ct;

    @Before
    public void influenzaWithACtComponent() {
        when(fhirConfig.getOeFhirSystem()).thenReturn("https://openelis.example");
        test = new org.openelisglobal.test.valueholder.Test();
        test.setId(TEST_ID);
        test.setName("Influenza B PCR");
        ct = new TestResultComponent();
        ct.setId(CT);
        ct.setCode("Flu B Ct");
        ct.setLabel("Flu B Ct");
        ct.setIsPrimary(false);
    }

    @Test
    public void aComponentsSameAsCodeWinsOverItsBroaderAndNarrowerCodesOfTheSameSystem() {
        when(testTerminologyMappingService.getActiveByTestId(TEST_ID)).thenReturn(List.of(
                mapping("LOINC", "85478-6", "SAME_AS", null, null), mapping("LOINC", "92142-9", "BROADER_THAN", null, null),
                mapping("LOINC", "CT-BROAD", "BROADER_THAN", CT, null), mapping("LOINC", "CT-SAME", "SAME_AS", CT, null),
                mapping("SNOMED", "CT-NARROW", "NARROWER_THAN", CT, null),
                mapping("LOINC", "ENDPT-ONLY", "SAME_AS", END_POINT, null)));

        CodeableConcept concept = service.transformResultCodeableConcept(test, ct, SWAB);

        assertEquals(List.of("http://loinc.org|85478-6", "http://loinc.org|CT-SAME", "http://snomed.info/sct|CT-NARROW",
                "https://openelis.example/test_result_component|Flu B Ct"), codes(concept));
        assertEquals("Flu B Ct", concept.getText());
    }

    @Test
    public void aComponentMappingScopedToAnotherSpecimenIsLeftOut() {
        when(testTerminologyMappingService.getActiveByTestId(TEST_ID)).thenReturn(List.of(
                mapping("LOINC", "85478-6", "SAME_AS", null, null), mapping("LOINC", "CT-SPUTUM", "SAME_AS", CT, SPUTUM),
                mapping("CIEL", "CT-SWAB", "SAME_AS", CT, SWAB), mapping("SNOMED", "CT-ANY", null, CT, null)));

        CodeableConcept concept = service.transformResultCodeableConcept(test, ct, SWAB);

        assertEquals(List.of("http://loinc.org|85478-6", "https://openconceptlab.org/orgs/CIEL/sources/CIEL|CT-SWAB",
                "http://snomed.info/sct|CT-ANY", "https://openelis.example/test_result_component|Flu B Ct"),
                codes(concept));
    }

    @Test
    public void aPrimaryComponentCarriesTheTestCodingsAndItsOwnWithoutAComponentEntry() {
        TestResultComponent primary = new TestResultComponent();
        primary.setId("primary-component");
        primary.setCode("PRIMARY");
        primary.setLabel("Influenza B");
        primary.setIsPrimary(true);
        when(testTerminologyMappingService.getActiveByTestId(TEST_ID))
                .thenReturn(List.of(mapping("LOINC", "85478-6", "SAME_AS", null, null),
                        mapping("SNOMED", "407479009", "SAME_AS", "primary-component", null)));

        CodeableConcept concept = service.transformResultCodeableConcept(test, primary, SWAB);

        assertEquals(List.of("http://loinc.org|85478-6", "http://snomed.info/sct|407479009"), codes(concept));
    }

    @Test
    public void aResultWithNoComponentCarriesTheTestCodingsOnly() {
        when(testTerminologyMappingService.getActiveByTestId(TEST_ID)).thenReturn(List.of(
                mapping("LOINC", "85478-6", "SAME_AS", null, null), mapping("LOINC", "CT-SAME", "SAME_AS", CT, null)));

        CodeableConcept concept = service.transformResultCodeableConcept(test, null, null);

        assertEquals(List.of("http://loinc.org|85478-6"), codes(concept));
    }

    private static List<String> codes(CodeableConcept concept) {
        return concept.getCoding().stream().map(coding -> coding.getSystem() + "|" + coding.getCode()).toList();
    }

    private static TestTerminologyMapping mapping(String source, String code, String relationship, String componentId,
            String sampleTypeId) {
        TestTerminologyMapping mapping = new TestTerminologyMapping();
        mapping.setTestId(TEST_ID);
        mapping.setSource(source);
        mapping.setCode(code);
        mapping.setRelationship(relationship);
        mapping.setComponentId(componentId);
        mapping.setSampleTypeId(sampleTypeId);
        return mapping;
    }
}
