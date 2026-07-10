package org.openelisglobal.common.util;

import java.util.List;
import org.openelisglobal.common.security.SystemInitFlag;
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
        // Cache rebuilds run on an async executor thread that carries neither the
        // caller's Authentication nor the startup thread's SystemInitFlag; the
        // listeners hit @PreAuthorize-gated services, so scope system context to
        // this maintenance work.
        boolean wasSet = SystemInitFlag.enter();
        try {
            List<ConfigurationListener> configurationListeners = getConfigurationListeners();
            for (ConfigurationListener configurationListener : configurationListeners) {
                configurationListener.refreshConfiguration();
            }
        } finally {
            SystemInitFlag.exit(wasSet);
        }
    }
}
