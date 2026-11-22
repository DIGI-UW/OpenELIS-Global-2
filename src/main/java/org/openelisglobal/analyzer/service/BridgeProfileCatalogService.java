package org.openelisglobal.analyzer.service;

public interface BridgeProfileCatalogService {

    BridgeProfileCatalog getCatalog();

    BridgeProfileCatalog.ProfileRevision getProfile(String profileId, int revision);

    /** Whether OpenELIS has an Analyzer Bridge address to ask for profiles. */
    default boolean isConfigured() {
        return true;
    }
}
