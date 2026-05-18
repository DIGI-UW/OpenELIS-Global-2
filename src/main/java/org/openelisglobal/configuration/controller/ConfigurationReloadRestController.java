package org.openelisglobal.configuration.controller;

import java.util.Set;
import org.openelisglobal.configuration.service.ConfigurationReloadOptions;
import org.openelisglobal.configuration.service.ConfigurationReloadResult;
import org.openelisglobal.configuration.service.ConfigurationReloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/configuration")
public class ConfigurationReloadRestController {

    @Autowired
    private ConfigurationReloadService configurationInitializationService;

    @Autowired
    private ConfigurationReloadRefreshService refreshService;

    @PostMapping("/domains/reload")
    public ConfigurationReloadResult reloadDomains(@RequestBody(required = false) ConfigurationReloadRequest request) {
        ConfigurationReloadRequest reloadRequest = request == null ? new ConfigurationReloadRequest(Set.of(), false)
                : request;
        ConfigurationReloadResult result = configurationInitializationService
                .reload(new ConfigurationReloadOptions(reloadRequest.domains(), reloadRequest.force()));
        if (!result.hasErrors()) {
            refreshService.refreshAfterDomainReload();
        }
        return result;
    }

    public record ConfigurationReloadRequest(Set<String> domains, boolean force) {
    }
}
