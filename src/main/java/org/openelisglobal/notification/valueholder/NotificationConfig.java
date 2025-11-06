package org.openelisglobal.notification.valueholder;

import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationPersonType;

public abstract class NotificationConfig<T> extends BaseObject<Integer> {

    private static final long serialVersionUID = -7224488513935429998L;

    public abstract List<NotificationConfigOption> getOptions();

    public abstract void setOptions(List<NotificationConfigOption> options);

    public abstract NotificationConfigOption getOptionFor(NotificationNature nature, NotificationMethod methodType,
            NotificationPersonType personType);

    public abstract NotificationConfigOption getPatientEmail();

    public abstract NotificationConfigOption getPatientSMS();

    public abstract NotificationConfigOption getProviderEmail();

    public abstract NotificationConfigOption getProviderSMS();
}
