package org.openelisglobal.testreflex.action.util;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

public class ReflexActionFactory {

    public static ReflexAction getReflexAction() {
        String reflexAction = ConfigurationProperties.getInstance().getPropertyValue(Property.ReflexAction);

        if ("RetroCI".equals(reflexAction)) {
            return new RetroCIReflexActions();
        } else {
            return new DefaultReflexActions();
        }
    }
}
