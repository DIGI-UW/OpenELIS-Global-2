package org.openelisglobal.fhir.facade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirFacadeConfig {

    @Value("${fhir.facade.enabled:true}")
    private boolean facadeEnabled;

    @Value("${fhir.facade.path:/fhir}")
    private String fhirPath;

    @Value("${fhir.facade.server.name:OpenELIS FHIR Facade}")
    private String serverName;

    @Value("${fhir.facade.server.version:1.0.0}")
    private String serverVersion;

    @Value("${fhir.facade.default.page.size:20}")
    private int defaultPageSize;

    @Value("${fhir.facade.max.page.size:100}")
    private int maxPageSize;

    public boolean isFacadeEnabled() {
        return facadeEnabled;
    }

    public String getFhirPath() {
        return fhirPath;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }
}
