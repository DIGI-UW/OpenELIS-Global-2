package org.openelisglobal.configuration.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface ConfigurationReloadService {

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    ConfigurationReloadResult reload(ConfigurationReloadOptions options);
}
