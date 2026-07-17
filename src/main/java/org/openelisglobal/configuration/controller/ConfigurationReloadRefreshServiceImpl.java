package org.openelisglobal.configuration.controller;

import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationReloadRefreshServiceImpl implements ConfigurationReloadRefreshService {

    @Autowired
    private DisplayListService displayListService;

    @Override
    public void refreshAfterDomainReload() {
        ConfigurationProperties.loadDBValuesIntoConfiguration();
        displayListService.refreshLists();
    }
}
