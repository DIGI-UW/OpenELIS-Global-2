import type { Context, Dispatch, ReactNode, SetStateAction } from "react";

export interface Notification {
  kind: "info" | "error" | "success" | "warning";
  title: string;
  message: ReactNode;
  subtitle?: ReactNode;
}

export interface NotificationContextValue {
  notificationVisible: boolean;
  setNotificationVisible: Dispatch<SetStateAction<boolean>>;
  notifications: Notification[];
  addNotification: (notification: Notification) => void;
  removeNotification: (index: number) => void;
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
      fallback?: boolean;
    }
  >;
} | null>;
