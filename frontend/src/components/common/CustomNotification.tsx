import React, { useContext } from "react";
import { ToastNotification } from "@carbon/react";
import { NotificationContext } from "../layout/Layout";

export const NotificationKinds = {
  info: "info",
  error: "error",
  success: "success",
  warning: "warning",
};

export type NotificationKind = keyof typeof NotificationKinds;

export interface NotificationBody {
  title: string;
  kind: "error" | "info" | "success" | "warning";
  subtitle?: React.ReactNode;
  message?: React.ReactNode;
}

export const CustomNotification: React.FC = () => {
  const { notifications, removeNotification } = useContext(
    NotificationContext,
  ) as {
    notifications: NotificationBody[];
    removeNotification: (index: number) => void;
  };

  return (
    <div className="toastDisplay">
      {notifications &&
        notifications.map(
          (notificationBody: NotificationBody, index: number) => {
            return (
              <ToastNotification
                key={index}
                title={notificationBody.title}
                timeout={
                  notificationBody.kind !== NotificationKinds.error
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
