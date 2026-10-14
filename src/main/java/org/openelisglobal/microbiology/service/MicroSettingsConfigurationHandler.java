package org.openelisglobal.microbiology.service;

import java.io.InputStream;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.siteinformation.service.SiteInformationConfigurationHandler;
import org.springframework.stereotype.Component;

@Component
public class MicroSettingsConfigurationHandler implements DomainConfigurationHandler {
    private final SiteInformationConfigurationHandler settings;

    public MicroSettingsConfigurationHandler(SiteInformationConfigurationHandler settings) {
        this.settings = settings;
    }

    public String getDomainName() {
        return "microbiology-settings";
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
