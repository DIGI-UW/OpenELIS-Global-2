package org.openelisglobal.ocl;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;

public class OclAnswerCodesTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    public void aCielAnswerCarriesItsCielConceptAndItsLoincAndSnomedMappings() throws Exception {
        JsonNode root = JSON.readTree(
                """
                        {"concepts":[{"id":"703","source":"CIEL"}],
                         "mappings":[
                           {"from_concept_code":"703","map_type":"SAME-AS","to_source_name":"LOINC","to_concept_code":"LA6576-8"},
                           {"from_concept_code":"703","map_type":"SAME-AS","to_source_name":"SNOMED-CT","to_concept_code":"10828004"},
                           {"from_concept_code":"703","map_type":"NARROWER-THAN","to_source_name":"SNOMED CT","to_concept_code":"260373001"},
                           {"from_concept_code":"703","map_type":"SAME-AS","to_source_name":"ICD-10-WHO","to_concept_code":"X"},
                           {"from_concept_code":"664","map_type":"SAME-AS","to_source_name":"LOINC","to_concept_code":"LA6577-6"}]}
                        """);

        List<DictionaryTerminologyMapping> codes = OclToOpenElisMapper.answerCodes(root, root.path("concepts").get(0));

        assertEquals(
                List.of("CIEL|703|SAME_AS", "LOINC|LA6576-8|SAME_AS", "SNOMED|10828004|SAME_AS",
                        "SNOMED|260373001|NARROWER_THAN"),
                codes.stream().map(code -> code.getSource() + "|" + code.getCode() + "|" + code.getRelationship())
                        .toList());
    }

    @Test
    public void anAnswerFromAnotherSourceKeepsItsStandardCodesButIsNotACielConcept() throws Exception {
        JsonNode root = JSON.readTree(
                """
                        {"concepts":[{"id":"AAA0002","source":"GLS"}],
                         "mappings":[{"from_concept_code":"AAA0002","map_type":"SAME-AS","to_source_name":"LOINC","to_concept_code":"28596-5"}]}
                        """);

        List<DictionaryTerminologyMapping> codes = OclToOpenElisMapper.answerCodes(root, root.path("concepts").get(0));

        assertEquals(List.of("LOINC|28596-5|SAME_AS"), codes.stream()
                .map(code -> code.getSource() + "|" + code.getCode() + "|" + code.getRelationship()).toList());
    }
}
