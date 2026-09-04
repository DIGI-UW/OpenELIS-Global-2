package org.openelisglobal.common.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DatabaseCleanService {

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    void cleanDatabase();
}
