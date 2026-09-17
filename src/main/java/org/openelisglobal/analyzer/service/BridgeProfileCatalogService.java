package org.openelisglobal.analyzer.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface BridgeProfileCatalogService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    BridgeProfileCatalog getCatalog();

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    BridgeProfileCatalog.ProfileRevision getProfile(String profileId, int revision);
}
