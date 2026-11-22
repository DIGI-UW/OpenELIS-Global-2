package org.openelisglobal.configuration.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.configuration.valueholder.ConfigurationImportRun;

public interface ConfigurationImportRunService extends BaseObjectService<ConfigurationImportRun, String> {

    /** Opens a run and makes it the current one for this thread. */
    ConfigurationImportRun start(String source, String sysUserId);

    /**
     * Closes the current run with its summary and clears the thread's import state.
     */
    void finish(ConfigurationImportRun run, String summary, boolean failed);
}
