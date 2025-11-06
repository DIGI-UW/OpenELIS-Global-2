import React from "react";
import { InlineNotification } from "@carbon/react";
import { Subject } from "rxjs";
import isEmpty from "lodash-es/isEmpty";

const inlineNotificationsSubject = new Subject<InlineNotificationMeta>();
let notificationId = 0;

export interface InlineNotificationMeta extends NotificationDescriptor {
  id: number;
}

export interface NotificationProps {
  notification: InlineNotificationMeta;
}

export interface NotificationDescriptor {
  description: React.ReactNode;
  action?: React.ReactNode;
  kind?: InlineNotificationType;
  critical?: boolean;
  millis?: number;
  title?: string;
}

export type InlineNotificationType =
  | "error"
  | "info"
  | "info-square"
  | "success"
  | "warning"
  | "warning-alt";

export const Notification: React.FC<NotificationProps> = ({ notification }) => {
  const { description, action, kind, critical, title } = notification;

  return (
    <InlineNotification
      actions={action}
      kind={kind || "info"}
      lowContrast={critical}
      subtitle={description}
      title={title || ""}
    />
  );
};

/**
 * Displays an inline notification in the UI.
 * @param notification The description of the notification to display.
 */
export function showNotification(notification: NotificationDescriptor) {
  if (notification && isNotEmpty(notification.description)) {
    setTimeout(() => {
      // always use in subsequent cycle
      inlineNotificationsSubject.next({
        ...notification,
        id: notificationId++,
      });
    }, 0);
  } else {
    console.error(
      `showNotification must be called with an object having a 'description' property that is a non-empty string or object`,
    );
  }
}

function isNotEmpty(description: React.ReactNode) {
  return typeof description === "string"
    ? description.trim().length > 0
    : typeof description === "object"
      ? !isEmpty(description)
      : false;
}
