import React, { useContext } from "react";
import { ToastNotification } from "@carbon/react";
import { NotificationContext } from "../layout/Layout";

export const OEToastNotificationKinds = {
  info: "info",
  error: "error",
  success: "success",
  warning: "warning",
};

export type OEToastNotificationKind = keyof typeof OEToastNotificationKinds;

export interface OEToastNotificationBody {
  title: string;
  kind:
    | "error"
    | "info"
    | "success"
    | "warning"
    | "info-square"
    | "success-alt"
    | "warning-alt";
  subtitle?: React.ReactNode;
  message?: React.ReactNode;
}

export const OEToastNotification: React.FC = () => {
  const { notifications, removeNotification } = useContext(
    NotificationContext,
  ) as {
    notifications: OEToastNotificationBody[];
    removeNotification: (index: number) => void;
  };

  return (
    <div className="toastDisplay">
      {notifications &&
        notifications.map(
          (notificationBody: OEToastNotificationBody, index: number) => {
            return (
              <ToastNotification
                key={index}
                title={notificationBody.title}
                timeout={
                  notificationBody.kind !== OEToastNotificationKinds.error
                    ? 2000
                    : 3000
                }
                onClose={() => false}
                onCloseButtonClick={() => {
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
          },
        )}
    </div>
  );
};
