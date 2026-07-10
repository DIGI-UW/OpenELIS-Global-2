package org.openelisglobal.notifications.service;

import org.openelisglobal.common.service.CrossDomainService;

/**
 * OGC-949 — pushes persistent notifications into the in-app header notification
 * bell (the {@code notifications} table read by NotificationRestController).
 * Used by the Alerts runtime (OGC-763) and by reflex/calculated-test creation
 * (OGC-764) so events surface beyond a transient browser toast.
 */
@CrossDomainService(callers = "Alerts runtime during result save (PRIV_RESULT_ENTER context) and"
        + " reflex/calculated-test creation in the catalog editor (PRIV_TEST_CONFIGURE context);"
        + " a notification sink has no privilege domain of its own")
public interface HeaderNotificationService {

    /** Create a header notification for a single user (no-op if user missing). */
    void notifyUser(String userId, String message);

    /** Create a header notification for every user holding the given role. */
    void notifyRole(String roleName, String message);
}
