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
        profile.put("profileId", PROFILE_ID);
        profile.put("revision", PROFILE_REVISION);
        profile.put("revisionFingerprint", PROFILE_FINGERPRINT);
        profile.put("displayName", "Generic analyzer test profile");
        profile.put("status", "ACTIVE");
        profile.putArray("tests");
        return new BridgeProfileCatalog("1.0", PROFILE_FINGERPRINT,
                List.of(new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode())));
    }
}
