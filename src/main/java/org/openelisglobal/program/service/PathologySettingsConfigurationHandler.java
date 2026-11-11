package org.openelisglobal.program.service;

import java.io.InputStream;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.siteinformation.service.SiteInformationConfigurationHandler;
import org.springframework.stereotype.Component;

/**
 * Loads the pathology settings file, which holds the settings a laboratory may
 * change after the first start, such as whether slide labels must be scanned
 * before sectioning. The values in the file are defaults: a row is written only
 * when it is missing, so an administrator's choice survives later loads.
 */
@Component
public class PathologySettingsConfigurationHandler implements DomainConfigurationHandler {
    private final SiteInformationConfigurationHandler settings;

    public PathologySettingsConfigurationHandler(SiteInformationConfigurationHandler settings) {
        this.settings = settings;
    }

    public String getDomainName() {
        return "pathology-settings";
    }

    public String getFileExtension() {
        return "csv";
    }

    public int getLoadOrder() {
        return 15;
    }

    public void processConfiguration(InputStream input, String fileName) throws Exception {
        settings.processConfiguration(input, fileName);
    }
}
