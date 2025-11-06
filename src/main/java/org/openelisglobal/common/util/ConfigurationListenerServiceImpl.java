package org.openelisglobal.common.util;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationListenerServiceImpl implements ConfigurationListenerService {

    @Autowired
    private List<ConfigurationListener> configurationListener;

    @Override
    public List<ConfigurationListener> getConfigurationListeners() {
        return configurationListener;
    }

    @Override
    @Async
    public void refreshConfigurations() {
        List<ConfigurationListener> configurationListeners = getConfigurationListeners();
        for (ConfigurationListener configurationListener : configurationListeners) {
            configurationListener.refreshConfiguration();
        }
    }
}
