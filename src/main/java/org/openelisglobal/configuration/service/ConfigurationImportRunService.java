package org.openelisglobal.configuration.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.configuration.valueholder.ConfigurationImportRun;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ConfigurationImportRunService extends BaseObjectService<ConfigurationImportRun, String> {

    /** Opens a run and makes it the current one for this thread. */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    ConfigurationImportRun start(String source, String sysUserId);

    /**
     * Closes the current run with its summary and clears the thread's import state.
     */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void finish(ConfigurationImportRun run, String summary, boolean failed);
}
