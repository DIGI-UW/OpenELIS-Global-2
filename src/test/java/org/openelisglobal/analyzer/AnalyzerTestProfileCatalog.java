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

    private AnalyzerTestProfileCatalog() {
    }

    public static BridgeProfileCatalog catalog() {
        return new BridgeProfileCatalog("1.0", PROFILE_FINGERPRINT, List.of(astmProfile(), hl7Profile()));
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
        profile.putObject("communication").put("mode", "ANALYZER_INITIATED").put("supports_lis_initiated", false);
        profile.putArray("default_test_mappings");
        profile.putObject("configDefaults").put("connectionRole", "SERVER").put("defaultTransport", "TCP/IP")
                .put("defaultPort", 9100).put("aggregationMode", "PER_MESSAGE");
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
        profile.putObject("communication").put("mode", "ANALYZER_INITIATED").put("supports_lis_initiated", false);
        profile.putArray("default_test_mappings");
        profile.putObject("configDefaults").put("connectionRole", "SERVER").put("defaultTransport", "MLLP")
                .put("aggregationMode", "PER_MESSAGE");
        ObjectNode catalog = profile.putObject("catalog");
        catalog.put("revision", HL7_PROFILE_REVISION);
        catalog.put("revisionFingerprint", HL7_PROFILE_FINGERPRINT);
        catalog.put("source", "SHIPPED");
        catalog.put("status", "ACTIVE");
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode());
    }
}
