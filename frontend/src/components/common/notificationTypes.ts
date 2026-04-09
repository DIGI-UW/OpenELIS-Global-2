import type React from "react";
import type { ToastNotification } from "@carbon/react";

type ToastNotificationKind = React.ComponentProps<
  typeof ToastNotification
>["kind"];

export type { ToastNotificationKind };

export interface OEToastNotificationBody {
  title: string;
  kind: ToastNotificationKind;
  subtitle?: React.ReactNode;
  message?: React.ReactNode;
}
