package org.openelisglobal.analyzer;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.openelisglobal.analyzer.service.BridgeProfileCatalog;

public final class AnalyzerTestProfileCatalog {

    public static final String PROFILE_ID = "test.generic-analyzer";
    public static final int PROFILE_REVISION = 1;
    public static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    public static final String HL7_PROFILE_ID = "test.generic-hl7-analyzer";
    public static final int HL7_PROFILE_REVISION = 1;
    public static final String HL7_PROFILE_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    /**
     * A profile with two revisions, for adoption: revision 2 fixes ADOPT-A's LOINC,
     * reports ADOPT-D in another unit, adds ADOPT-B and drops ADOPT-C.
     */
    public static final String ADOPTABLE_PROFILE_ID = "test.adoptable";
    public static final String ADOPTABLE_RECOGNITION_FINGERPRINT = "sha256:" + "6".repeat(64);

    private AnalyzerTestProfileCatalog() {
    }

    public static BridgeProfileCatalog catalog() {
        return new BridgeProfileCatalog("1.0", PROFILE_FINGERPRINT, List.of(astmProfile(), hl7Profile(),
                unknownCapableProfile(), adoptableProfile(1), adoptableProfile(2)));
    }

    public static String adoptableFingerprint(int revision) {
        return "sha256:" + String.valueOf(revision).repeat(64);
    }

    private static BridgeProfileCatalog.ProfileRevision adoptableProfile(int revision) {
        ObjectNode profile = ((ObjectNode) astmProfile().profile()).deepCopy();
        ((ObjectNode) profile.path("profileMeta")).put("id", ADOPTABLE_PROFILE_ID);
        ((ObjectNode) profile.path("catalog")).put("revision", revision).put("revisionFingerprint",
                adoptableFingerprint(revision));
        var tests = profile.putArray("default_test_mappings");
        tests.addObject().put("test_code", "ADOPT-A").put("loinc", revision == 1 ? "2345-7" : "2339-0")
                .put("result_type", "quantitative");
        tests.addObject().put("test_code", "ADOPT-D").put("loinc", "2160-0").put("result_type", "quantitative")
                .put("unit", revision == 1 ? "mg/dL" : "umol/L");
        if (revision == 1) {
            tests.addObject().put("test_code", "ADOPT-C").put("loinc", "2947-0").put("result_type", "quantitative");
        }
        if (revision > 1) {
            tests.addObject().put("test_code", "ADOPT-B").put("loinc", "2951-2").put("result_type", "quantitative");
        }
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode(),
                new BridgeProfileCatalog.ControlRecognitionSummary(ADOPTABLE_RECOGNITION_FINGERPRINT, "RULES", null,
                        false, List.of()));
    }

    private static BridgeProfileCatalog.ProfileRevision unknownCapableProfile() {
        ObjectNode profile = ((ObjectNode) astmProfile().profile()).deepCopy();
        ((ObjectNode) profile.path("profileMeta")).put("id", "site.unknown-capable");
        ((ObjectNode) profile.path("catalog")).put("revision", 3).put("revisionFingerprint",
                "sha256:" + "1".repeat(64));
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode(),
                new BridgeProfileCatalog.ControlRecognitionSummary("sha256:" + "2".repeat(64), "RULES", null, false,
                        List.of()));
    }

    private static BridgeProfileCatalog.ProfileRevision astmProfile() {
        ObjectNode profile = JsonNodeFactory.instance.objectNode();
        profile.put("schemaVersion", "1.0");
        ObjectNode profileMeta = profile.putObject("profileMeta");
        profileMeta.put("id", PROFILE_ID);
        profileMeta.put("version", "1.0.0");
        profileMeta.put("displayName", "Generic analyzer test profile");
        profileMeta.put("confidence", "VALIDATED");
        profile.putObject("protocol").put("name", "ASTM").put("version", "LIS2-A2");
        profile.putArray("transport").add("TCP/IP");
        profile.putObject("communication").put("mode", "ANALYZER_INITIATED").put("supports_lis_initiated", false);
        profile.putObject("capabilities").put("inboundResults", true).put("outboundOrders", false).put("connectionTest",
                true);
        profile.putArray("default_test_mappings");
        profile.putObject("configDefaults").put("connectionRole", "SERVER").put("transport", "TCP/IP").put("port", 9100)
                .put("aggregationMode", "PER_MESSAGE");
        ObjectNode catalog = profile.putObject("catalog");
        catalog.put("revision", PROFILE_REVISION);
        catalog.put("revisionFingerprint", PROFILE_FINGERPRINT);
        catalog.put("source", "SHIPPED");
        catalog.put("status", "ACTIVE");
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode());
    }

    private static BridgeProfileCatalog.ProfileRevision hl7Profile() {
        ObjectNode profile = JsonNodeFactory.instance.objectNode();
        profile.put("schemaVersion", "1.0");
        ObjectNode profileMeta = profile.putObject("profileMeta");
        profileMeta.put("id", HL7_PROFILE_ID);
        profileMeta.put("version", "1.0.0");
        profileMeta.put("displayName", "Generic HL7 analyzer test profile");
        profileMeta.put("confidence", "VALIDATED");
        profile.putObject("protocol").put("name", "HL7").put("version", "HL7 v2.3.1");
        profile.putArray("transport").add("MLLP");
        profile.putObject("communication").put("mode", "ANALYZER_INITIATED").put("supports_lis_initiated", false);
        profile.putObject("capabilities").put("inboundResults", true).put("outboundOrders", false).put("connectionTest",
                true);
        profile.putArray("default_test_mappings");
        profile.putObject("configDefaults").put("connectionRole", "SERVER").put("transport", "MLLP")
                .put("aggregationMode", "PER_MESSAGE");
        ObjectNode catalog = profile.putObject("catalog");
        catalog.put("revision", HL7_PROFILE_REVISION);
        catalog.put("revisionFingerprint", HL7_PROFILE_FINGERPRINT);
        catalog.put("source", "SHIPPED");
        catalog.put("status", "ACTIVE");
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode());
    }
}
