package org.openelisglobal.common.util;

import java.util.List;

public interface ConfigurationListenerService {

    List<ConfigurationListener> getConfigurationListeners();

    void refreshConfigurations();
}
