package org.openelisglobal.analyzer;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.openelisglobal.analyzer.service.BridgeProfileCatalog;

public final class AnalyzerTestProfileCatalog {

    public static final String PROFILE_ID = "test.generic-analyzer";
    public static final int PROFILE_REVISION = 1;
    public static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private AnalyzerTestProfileCatalog() {
    }

    public static BridgeProfileCatalog catalog() {
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
        profile.putObject("configDefaults").put("connectionRole", "SERVER").put("aggregationMode", "PER_MESSAGE");
        ObjectNode catalog = profile.putObject("catalog");
        catalog.put("revision", PROFILE_REVISION);
        catalog.put("revisionFingerprint", PROFILE_FINGERPRINT);
        catalog.put("source", "SHIPPED");
        catalog.put("status", "ACTIVE");
        return new BridgeProfileCatalog("1.0", PROFILE_FINGERPRINT,
                List.of(new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode())));
    }
}
