package org.openelisglobal.microbiology.service;

import java.io.InputStream;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dictionary.service.DictionaryConfigurationHandler;
import org.springframework.stereotype.Component;

/**
 * Uses the dictionary importer with an independently overridable catalog
 * domain.
 */
@Component
public class MicroCultureConfigurationHandler implements DomainConfigurationHandler {
    private final DictionaryConfigurationHandler dictionaries;

    public MicroCultureConfigurationHandler(DictionaryConfigurationHandler dictionaries) {
        this.dictionaries = dictionaries;
    }

    public String getDomainName() {
        return "microbiology-cultures";
    }

    public String getFileExtension() {
        return "csv";
    }

    public int getLoadOrder() {
        return 300;
    }

    public void processConfiguration(InputStream input, String fileName) throws Exception {
        dictionaries.processConfiguration(input, fileName);
    }
}
