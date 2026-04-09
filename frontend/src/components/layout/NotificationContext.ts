import { createContext } from "react";
import type { OEToastNotificationBody } from "../common/notificationTypes";

export interface NotificationContextType {
  notificationVisible: boolean;
  setNotificationVisible: (visible: boolean) => void;
  notifications: OEToastNotificationBody[];
  addNotification: (notification: OEToastNotificationBody) => void;
  removeNotification: (index: number) => void;
}

export const NotificationContext = createContext<NotificationContextType>({
  notificationVisible: false,
  setNotificationVisible: () => {},
  notifications: [],
  addNotification: () => {},
  removeNotification: () => {},
});
