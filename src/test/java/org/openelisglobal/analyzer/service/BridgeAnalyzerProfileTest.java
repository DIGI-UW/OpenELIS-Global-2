package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Test;

public class BridgeAnalyzerProfileTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void preservesEveryEstablishedMappingFieldWithoutCollapsingSharedIdentity() throws Exception {
        JsonNode document = objectMapper.readTree("""
                {
                  "profileMeta":{"id":"site.mock-analyzer","displayName":"Mock Analyzer"},
                  "protocol":{"name":"ASTM","version":"LIS2-A2"},
                  "communication":{"mode":"ANALYZER_INITIATED","supports_lis_initiated":true},
                  "configDefaults":{"connectionRole":"SERVER","aggregationMode":"PER_MESSAGE"},
                  "catalog":{
                    "revision":2,
                    "revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "source":"SITE",
                    "status":"ACTIVE"
                  },
                  "default_test_mappings":[
                    {
                      "test_code":"RAW-A",
                      "aliases":["RAW-A1","RAW-A2"],
                      "test_name_hint":"First result",
                      "specimen_type_hint":"Plasma",
                      "result_value_hints":{"POS":"Detected"},
                      "loinc":"94500-6",
                      "unit":"copies/mL",
                      "result_type":"qualitative",
                      "values":["POS","NEG"],
                      "value_codes":{
                        "POS":[{"system":"http://loinc.org","code":"LA6576-8","display":"Detected"}],
                        "NEG":[{"system":"http://loinc.org","code":"LA6577-6"}]
                      },
                      "normalized_coding":{
                        "system":"https://loinc.org",
                        "code":"94500-6",
                        "display":"SARS-CoV-2 RNA"
                      }
                    },
                    {
                      "test_code":"RAW-B",
                      "test_name_hint":"Second result",
                      "loinc":"94500-6",
                      "unit":"copies/mL",
                      "result_type":"quantitative"
                    }
                  ]
                }
                """);

        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(document);

        assertEquals(2, profile.testDefinitions().size());
        BridgeAnalyzerProfile.TestDefinition first = profile.testDefinitions().get(0);
        assertEquals("RAW-A", first.analyzerCode());
        assertEquals(List.of("RAW-A1", "RAW-A2"), first.aliases());
        assertEquals("First result", first.testNameHint());
        assertEquals("94500-6", first.loinc());
        assertEquals("copies/mL", first.unit());
        assertEquals("qualitative", first.resultType());
        assertEquals(List.of("POS", "NEG"), first.resultValues());
        assertEquals("LA6576-8", first.valueCodes().get("POS").get(0).code());
        assertEquals("http://loinc.org", first.valueCodes().get("POS").get(0).system());
        assertEquals("Detected", first.valueCodes().get("POS").get(0).display());
        assertEquals("LA6577-6", first.valueCodes().get("NEG").get(0).code());
        assertEquals(java.util.Map.of(), profile.testDefinitions().get(1).valueCodes());
        assertEquals("https://loinc.org", first.normalizedCoding().system());
        assertEquals("94500-6", first.normalizedCoding().code());
        assertEquals("SARS-CoV-2 RNA", first.normalizedCoding().display());
        assertEquals("RAW-B", profile.testDefinitions().get(1).analyzerCode());
        assertEquals("94500-6", profile.testDefinitions().get(1).loinc());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAValueCodeForAValueTheTestDoesNotDeclare() throws Exception {
        BridgeAnalyzerProfile.from(objectMapper.readTree("""
                {
                  "profileMeta":{"id":"site.mock-analyzer","displayName":"Mock Analyzer"},
                  "protocol":{"name":"ASTM"},
                  "catalog":{
                    "revision":1,
                    "revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "source":"SITE",
                    "status":"ACTIVE"
                  },
                  "default_test_mappings":[
                    {
                      "test_code":"RAW-A",
                      "loinc":"94500-6",
                      "result_type":"qualitative",
                      "values":["POS"],
                      "value_codes":{"NEG":[{"system":"http://loinc.org","code":"LA6577-6"}]}
                    }
                  ]
                }
                """));
    }
}
