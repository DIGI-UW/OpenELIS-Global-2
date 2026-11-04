import React, { useContext, useEffect } from "react";
import { ToastNotification } from "@carbon/react";
import { NotificationContext } from "../layout/Layout";
import { notificationTimeout } from "../layout/notificationList";

export const NotificationKinds = {
  info: "info",
  error: "error",
  success: "success",
  warning: "warning",
};

/**
 * The page's toasts. Each one it shows is reported to the queue, which removes
 * it when its time is up even if this dialog is hidden or the page changes
 * first.
 */
export const AlertDialog = () => {
  const { notifications, removeNotification, notificationShown } =
    useContext(NotificationContext);

  useEffect(() => {
    (notifications || []).forEach((notificationBody) =>
      notificationShown?.(notificationBody),
    );
  }, [notifications, notificationShown]);

  return (
    <div className="toastDisplay">
      {notifications &&
        notifications.map((notificationBody, index) => {
          return (
            <ToastNotification
              key={notificationBody.id ?? index}
              title={notificationBody.title}
              timeout={notificationTimeout(notificationBody.kind)}
              onClose={(event) => {
                // Remove notification from array when it closes automatically
                removeNotification(notificationBody.id);
              }}
              onCloseButtonClick={(event) => {
                removeNotification(notificationBody.id);
              }}
              lowContrast={true}
              kind={notificationBody.kind}
              subtitle={notificationBody.subtitle}
            >
              {notificationBody.message}
              <br />
              <br />
            </ToastNotification>
          );
        })}
    </div>
  );
};
