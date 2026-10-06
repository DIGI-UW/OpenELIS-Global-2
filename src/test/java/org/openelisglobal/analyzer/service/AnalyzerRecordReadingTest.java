package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.Test;

public class AnalyzerRecordReadingTest {

    @Test
    public void aLoincFixOrAnAddedValueReadsTheSame() throws Exception {
        Set<AnalyzerMappingRowKey> alike = AnalyzerRecordReading.readAlike(
                profile("{\"test_code\":\"HBSAG\",\"loinc\":\"5196-1\",\"result_type\":\"qualitative\","
                        + "\"values\":[\"POS\"]}"),
                profile("{\"test_code\":\"HBSAG\",\"loinc\":\"5195-3\",\"result_type\":\"qualitative\","
                        + "\"values\":[\"POS\",\"NEG\"]}"));

        assertTrue(alike.contains(AnalyzerMappingRowKey.main("HBSAG")));
    }

    @Test
    public void aChangedUnitOrResultTypeReadsDifferently() throws Exception {
        assertFalse(AnalyzerRecordReading
                .readAlike(profile("{\"test_code\":\"GLU\",\"loinc\":\"2345-7\",\"unit\":\"mg/dL\"}"),
                        profile("{\"test_code\":\"GLU\",\"loinc\":\"2345-7\",\"unit\":\"mmol/L\"}"))
                .contains(AnalyzerMappingRowKey.main("GLU")));
        assertFalse(AnalyzerRecordReading
                .readAlike(profile("{\"test_code\":\"GLU\",\"loinc\":\"2345-7\",\"result_type\":\"quantitative\"}"),
                        profile("{\"test_code\":\"GLU\",\"loinc\":\"2345-7\",\"result_type\":\"qualitative\"}"))
                .contains(AnalyzerMappingRowKey.main("GLU")));
    }

    @Test
    public void aChangedPartReadsDifferentlyForTheTestAndThatPart() throws Exception {
        Set<AnalyzerMappingRowKey> alike = AnalyzerRecordReading.readAlike(
                profile("{\"test_code\":\"HIVVL\",\"loinc\":\"20447-9\",\"call_component\":\"call\","
                        + "\"components\":[{\"code\":\"call\"},{\"code\":\"ct\",\"sub_identity\":\"HIV-1&Ct\"}]}"),
                profile("{\"test_code\":\"HIVVL\",\"loinc\":\"20447-9\",\"call_component\":\"call\","
                        + "\"components\":[{\"code\":\"call\"},{\"code\":\"cycle\",\"sub_identity\":\"HIV-1&Ct\"}]}"));

        assertFalse(alike.contains(AnalyzerMappingRowKey.main("HIVVL")));
        assertFalse(alike.contains(new AnalyzerMappingRowKey("HIVVL", "HIV-1&Ct")));
    }

    @Test
    public void aRecordOnlyOneRevisionDeclaresReadsDifferently() throws Exception {
        Set<AnalyzerMappingRowKey> alike = AnalyzerRecordReading.readAlike(
                profile("{\"test_code\":\"OLD\",\"loinc\":\"2947-0\"}",
                        "{\"test_code\":\"KEPT\",\"loinc\":\"2951-2\"}"),
                profile("{\"test_code\":\"KEPT\",\"loinc\":\"2951-2\"}",
                        "{\"test_code\":\"NEW\",\"loinc\":\"2823-3\"}"));

        assertTrue(alike.contains(AnalyzerMappingRowKey.main("KEPT")));
        assertFalse(alike.contains(AnalyzerMappingRowKey.main("OLD")));
        assertFalse(alike.contains(AnalyzerMappingRowKey.main("NEW")));
    }

    private static BridgeAnalyzerProfile profile(String... tests) throws Exception {
        return BridgeAnalyzerProfile.from(new ObjectMapper().readTree("{\"profileMeta\":{\"id\":\"fixture.reading\","
                + "\"displayName\":\"Reading\"},\"catalog\":{\"revision\":1,\"revisionFingerprint\":\"sha256:"
                + "a".repeat(64) + "\",\"source\":\"SHIPPED\",\"status\":\"ACTIVE\"},\"protocol\":{\"name\":\"ASTM\"},"
                + "\"default_test_mappings\":[" + String.join(",", tests) + "]}"));
    }
}
