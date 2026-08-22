package org.openelisglobal.analyzer.service;

public interface BridgeProfileCatalogService {

    BridgeProfileCatalog getCatalog();

    BridgeProfileCatalog.ProfileRevision getProfile(String profileId, int revision);
}
