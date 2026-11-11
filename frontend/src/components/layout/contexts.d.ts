import type { Context, Dispatch, ReactNode, SetStateAction } from "react";

export interface Notification {
  kind: "info" | "error" | "success" | "warning";
  title: string;
  message: ReactNode;
  subtitle?: ReactNode;
  /** Set when the notification is added; it is removed by this id. */
  id?: number;
}

export interface NotificationContextValue {
  notificationVisible: boolean;
  setNotificationVisible: Dispatch<SetStateAction<boolean>>;
  notifications: Notification[];
  addNotification: (notification: Notification) => void;
  removeNotification: (id: number) => void;
  /** Starts the time a shown notification has left in the queue. */
  notificationShown?: (notification: Notification) => void;
}

export const NotificationContext: Context<NotificationContextValue | null>;
export const ConfigurationContext: Context<{
  configurationProperties: Record<string, unknown>;
  reloadConfiguration: () => void;
  supportedLocales: Array<{
    id: string;
    localeCode: string;
    displayName: string;
    active: boolean;
    fallback: boolean;
    sortOrder: number;
  }>;
  enabledLanguages: Record<
    string,
    {
      label: string;
      messages: Record<string, string>;
      fallback?: boolean;
    }
  >;
} | null>;
