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
                      "loinc":"94500-6",
                      "unit":"copies/mL",
                      "result_type":"qualitative",
                      "values":["POS","NEG"],
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
        assertEquals("https://loinc.org", first.normalizedCoding().system());
        assertEquals("94500-6", first.normalizedCoding().code());
        assertEquals("SARS-CoV-2 RNA", first.normalizedCoding().display());
        assertEquals("RAW-B", profile.testDefinitions().get(1).analyzerCode());
        assertEquals("94500-6", profile.testDefinitions().get(1).loinc());
    }
}
