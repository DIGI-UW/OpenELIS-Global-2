import type { ReactElement } from "react";

export const NotificationKinds: {
  readonly info: "info";
  readonly error: "error";
  readonly success: "success";
  readonly warning: "warning";
};
export const AlertDialog: () => ReactElement;
