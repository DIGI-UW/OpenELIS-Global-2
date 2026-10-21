package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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

    @Test
    public void anAliasOrARunFailureValueIsPartOfHowATestIsRead() throws Exception {
        assertFalse(AnalyzerRecordReading
                .readAlike(profile("{\"test_code\":\"GLU\",\"loinc\":\"2345-7\"}"),
                        profile("{\"test_code\":\"GLU\",\"loinc\":\"2345-7\",\"aliases\":[\"GLUC\"]}"))
                .contains(AnalyzerMappingRowKey.main("GLU")));
        assertFalse(
                AnalyzerRecordReading
                        .readAlike(profile("{\"test_code\":\"MTB\",\"loinc\":\"38379-4\",\"values\":[\"DETECTED\"]}"),
                                profile("{\"test_code\":\"MTB\",\"loinc\":\"38379-4\",\"values\":[\"DETECTED\"],"
                                        + "\"run_failure_values\":[\"ERROR\"]}"))
                        .contains(AnalyzerMappingRowKey.main("MTB")));
    }

    @Test
    public void eachDeclaredValueIsARowReadByTheSpellingsTranslatedToIt() throws Exception {
        Map<AnalyzerMappingSourceRow, Object> before = AnalyzerRecordReading
                .rowReadings(profile("{\"test_code\":\"HBSAG\",\"loinc\":\"5196-1\",\"result_type\":\"qualitative\","
                        + "\"values\":[\"POS\",\"NEG\"],\"translations\":{\"POS\":[\"POSITIF\"],"
                        + "\"NEG\":[\"NEGATIF\"]}}"));
        Map<AnalyzerMappingSourceRow, Object> after = AnalyzerRecordReading
                .rowReadings(profile("{\"test_code\":\"HBSAG\",\"loinc\":\"5196-1\",\"result_type\":\"qualitative\","
                        + "\"values\":[\"POS\",\"NEG\",\"IND\"],\"translations\":{\"POS\":[\"NEGATIF\"],"
                        + "\"NEG\":[\"NEGATIF\"]}}"));
        AnalyzerMappingSourceRow test = new AnalyzerMappingSourceRow("HBSAG", null, "");
        AnalyzerMappingSourceRow positive = new AnalyzerMappingSourceRow("HBSAG", "POS", "");
        AnalyzerMappingSourceRow negative = new AnalyzerMappingSourceRow("HBSAG", "NEG", "");

        assertEquals("an added value does not change the test", before.get(test), after.get(test));
        assertEquals(before.get(negative), after.get(negative));
        assertNotEquals("a value translated from other spellings reads differently", before.get(positive),
                after.get(positive));
        assertTrue(after.containsKey(new AnalyzerMappingSourceRow("HBSAG", "IND", "")));
    }

    @Test
    public void aSpellingMovedToAnotherValueReadsDifferently() throws Exception {
        Map<AnalyzerMappingSourceRow, Object> before = AnalyzerRecordReading
                .rowReadings(profile("{\"test_code\":\"HBSAG\",\"loinc\":\"5196-1\",\"result_type\":\"qualitative\","
                        + "\"values\":[\"POS\",\"NEG\"],\"translations\":{\"NEG\":[\"NEGATIF\"]}}"));
        Map<AnalyzerMappingSourceRow, Object> after = AnalyzerRecordReading
                .rowReadings(profile("{\"test_code\":\"HBSAG\",\"loinc\":\"5196-1\",\"result_type\":\"qualitative\","
                        + "\"values\":[\"POS\",\"NEG\"],\"translations\":{\"POS\":[\"NEGATIF\"]}}"));
        AnalyzerMappingSourceRow spelling = new AnalyzerMappingSourceRow("HBSAG", "NEGATIF", "");

        assertTrue(before.containsKey(spelling));
        assertNotEquals(before.get(spelling), after.get(spelling));
    }

    @Test
    public void aTranslatedRunFailureSpellingIsPartOfHowATestAndItsPartsAreRead() throws Exception {
        String test = "{\"test_code\":\"MTB\",\"loinc\":\"38379-4\",\"run_failure_values\":[\"ERROR\"],"
                + "\"translations\":{\"ERROR\":[%s]},\"components\":[{\"code\":\"ct\",\"sub_identity\":\"MTB&Ct\","
                + "\"run_failure_values\":[%s]}]}";
        Set<AnalyzerMappingRowKey> alike = AnalyzerRecordReading.readAlike(
                profile(String.format(test, "\"ERREUR\"", "\"INVALID\"")),
                profile(String.format(test, "\"NO RESULT\"", "\"INVALID\",\"NA\"")));

        assertFalse(alike.contains(AnalyzerMappingRowKey.main("MTB")));
        assertFalse(alike.contains(new AnalyzerMappingRowKey("MTB", "MTB&Ct")));
    }

    @Test
    public void aRecordReadsAlikeOnlyWhenTheValuesBothDeclareReadAlike() throws Exception {
        Set<AnalyzerMappingRowKey> alike = AnalyzerRecordReading.readAlike(profile(
                "{\"test_code\":\"HBSAG\",\"loinc\":\"5196-1\",\"values\":[\"POS\",\"NEG\"],\"translations\":{\"NEG\":[\"NEGATIF\"]}}"),
                profile("{\"test_code\":\"HBSAG\",\"loinc\":\"5196-1\",\"values\":[\"POS\",\"NEG\"],\"translations\":{\"POS\":[\"NEGATIF\"]}}"));

        assertFalse(alike.contains(AnalyzerMappingRowKey.main("HBSAG")));
    }

    @Test
    public void whereAndHowARecordWritesItsValueIsPartOfTheTypeReading() throws Exception {
        BridgeAnalyzerProfile base = typeProfile("\"configDefaults\":{\"numberFormat\":\".\","
                + "\"extractionOverrides\":{\"resultParts\":{\"value\":{\"field\":3}}}}");
        BridgeAnalyzerProfile moved = typeProfile("\"configDefaults\":{\"numberFormat\":\".\","
                + "\"extractionOverrides\":{\"resultParts\":{\"value\":{\"field\":4}}}}");
        BridgeAnalyzerProfile comma = typeProfile("\"configDefaults\":{\"numberFormat\":\",\","
                + "\"extractionOverrides\":{\"resultParts\":{\"value\":{\"field\":3}}}}");

        assertNotEquals(AnalyzerRecordReading.typeReading(base), AnalyzerRecordReading.typeReading(moved));
        assertNotEquals(AnalyzerRecordReading.typeReading(base), AnalyzerRecordReading.typeReading(comma));
    }

    @Test
    public void theTypeReadingIsItsRoutingParsingAndRecognitionNotItsNamesOrConnectionDefaults() throws Exception {
        BridgeAnalyzerProfile base = typeProfile(
                "\"identifier_pattern\":\"GENEXPERT\",\"configDefaults\":{\"port\":1}");
        BridgeAnalyzerProfile renamed = typeProfile(
                "\"identifier_pattern\":\"GENEXPERT\",\"configDefaults\":{\"port\":2},\"model\":\"GX-IV\"");
        BridgeAnalyzerProfile rerouted = typeProfile(
                "\"identifier_pattern\":\"CEPHEID\",\"configDefaults\":{\"port\":1}");

        assertEquals(AnalyzerRecordReading.typeReading(base), AnalyzerRecordReading.typeReading(renamed));
        assertNotEquals(AnalyzerRecordReading.typeReading(base), AnalyzerRecordReading.typeReading(rerouted));
    }

    private static BridgeAnalyzerProfile typeProfile(String fields) throws Exception {
        return BridgeAnalyzerProfile.from(new ObjectMapper().readTree("{\"profileMeta\":{\"id\":\"fixture.reading\","
                + "\"displayName\":\"Reading\"},\"catalog\":{\"revision\":1,\"revisionFingerprint\":\"sha256:"
                + "a".repeat(64) + "\",\"source\":\"SHIPPED\",\"status\":\"ACTIVE\"},\"protocol\":{\"name\":\"ASTM\"},"
                + fields + ",\"default_test_mappings\":[{\"test_code\":\"GLU\",\"loinc\":\"2345-7\"}]}"));
    }

    private static BridgeAnalyzerProfile profile(String... tests) throws Exception {
        return BridgeAnalyzerProfile.from(new ObjectMapper().readTree("{\"profileMeta\":{\"id\":\"fixture.reading\","
                + "\"displayName\":\"Reading\"},\"catalog\":{\"revision\":1,\"revisionFingerprint\":\"sha256:"
                + "a".repeat(64) + "\",\"source\":\"SHIPPED\",\"status\":\"ACTIVE\"},\"protocol\":{\"name\":\"ASTM\"},"
                + "\"default_test_mappings\":[" + String.join(",", tests) + "]}"));
    }
}
