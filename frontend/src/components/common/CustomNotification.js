import React, { useContext } from "react";
import { ToastNotification } from "@carbon/react";
import { NotificationContext } from "../layout/Layout";

export const NotificationKinds = {
  info: "info",
  error: "error",
  success: "success",
  warning: "warning",
};

export const AlertDialog = () => {
  const { notifications, removeNotification } = useContext(NotificationContext);

  return (
    <div className="toastDisplay">
      {notifications &&
        notifications.map((notificationBody, index) => {
          return (
            <ToastNotification
              key={index}
              title={notificationBody.title}
              timeout={
                notificationBody.kind !== NotificationKinds.error ? 2000 : 3000
              }
              onClose={(event) => {
                return false;
              }}
              onCloseButtonClick={(event) => {
                removeNotification(index);
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
