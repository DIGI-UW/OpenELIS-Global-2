package org.openelisglobal.analyzer;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.openelisglobal.analyzer.service.BridgeProfileCatalog;

public final class AnalyzerTestProfileCatalog {

    public static final String RECOGNITION_FINGERPRINT = "sha256:" + "c".repeat(64);
    public static final String PROFILE_ID = "test.generic-analyzer";
    public static final int PROFILE_REVISION = 1;
    public static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    public static final String HL7_PROFILE_ID = "test.generic-hl7-analyzer";
    public static final int HL7_PROFILE_REVISION = 1;
    public static final String HL7_PROFILE_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    public static final String RECEIPT_PROFILE_ID = "test.receipt-analyzer";
    public static final String CONTROL_RECOGNITION_FINGERPRINT = "sha256:" + "2".repeat(64);

    private AnalyzerTestProfileCatalog() {
    }

    public static BridgeProfileCatalog catalog() {
        return new BridgeProfileCatalog("1.0", PROFILE_FINGERPRINT,
                List.of(astmProfile(), hl7Profile(), receiptProfile()));
    }

    // Matches the recognition evidence in the Bridge's patient/control contract
    // fixtures. Local Tests, bindings and confirmations are created by real
    // services.
    private static BridgeProfileCatalog.ProfileRevision receiptProfile() {
        ObjectNode profile = (ObjectNode) astmProfile().profile().deepCopy();
        ((ObjectNode) profile.get("profileMeta")).put("id", RECEIPT_PROFILE_ID).put("displayName",
                "Receipt integration analyzer");
        ((ObjectNode) profile.get("catalog")).put("revisionFingerprint", "sha256:" + "d".repeat(64));
        profile.withArray("default_test_mappings").addObject().put("test_code", "WBC").put("loinc", "6690-2")
                .put("result_type", "N").put("unit", "10*3/uL");
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode(),
                new BridgeProfileCatalog.ControlRecognitionSummary(CONTROL_RECOGNITION_FINGERPRINT, "RULES",
                        "Specimen ID starts with QC-", false,
                        List.of(new BridgeProfileCatalog.ControlRecognitionSummary.Condition("control-prefix",
                                "SPECIMEN_ID_STARTS_WITH", "Specimen ID", "QC-", "Specimen ID starts with QC-",
                                "NORMAL", null))));
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
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode(),
                new BridgeProfileCatalog.ControlRecognitionSummary(RECOGNITION_FINGERPRINT, "NONE",
                        "No automated control recognition", true, List.of()));
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
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode(),
                new BridgeProfileCatalog.ControlRecognitionSummary(RECOGNITION_FINGERPRINT, "NONE",
                        "No automated control recognition", true, List.of()));
    }
}
