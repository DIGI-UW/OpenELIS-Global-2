import React, { useContext } from "react";
import { ToastNotification } from "@carbon/react";
import { NotificationContext } from "../layout/NotificationContext";

type ToastNotificationKind = React.ComponentProps<
  typeof ToastNotification
>["kind"];

export const OEToastNotificationKinds: Record<string, ToastNotificationKind> = {
  info: "info",
  error: "error",
  success: "success",
  warning: "warning",
  infoSquare: "info-square",
  successAlt: "success-alt",
  warningAlt: "warning-alt",
};

export interface OEToastNotificationBody {
  title: string;
  kind: ToastNotificationKind;
  subtitle?: React.ReactNode;
  message?: React.ReactNode;
}

export const OEToastNotification: React.FC = () => {
  const { notifications, removeNotification } = useContext(NotificationContext);

  return (
    <div className="toastDisplay">
      {notifications?.map((notificationBody, index) => (
        <ToastNotification
          key={index}
          title={notificationBody.title}
          timeout={notificationBody.kind !== "error" ? 2000 : 3000}
          onClose={() => false}
          onCloseButtonClick={() => {
            removeNotification(index);
          }}
          lowContrast
          kind={notificationBody.kind}
          subtitle={notificationBody.subtitle}
        >
          {notificationBody.message}
          <br />
          <br />
        </ToastNotification>
      ))}
    </div>
  );
};
