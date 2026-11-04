import { useCallback, useRef, useState } from "react";

/** How long a toast of this kind stays up, in milliseconds. */
export const notificationTimeout = (kind) => (kind !== "error" ? 2000 : 3000);

/**
 * The page-wide notification queue. Each notification gets an id when it is
 * added and is removed by that id: removing by position let two toasts that
 * closed together remove the first and then nothing, so the second stayed
 * queued and showed again beside the next notification.
 *
 * A notification leaves the queue its timeout after a dialog first shows it,
 * whether or not that dialog is still mounted. One added just before the page
 * changes still shows on the next page, and one whose dialog was hidden early
 * does not come back with the next notification.
 */
export function useNotificationList() {
  const [notifications, setNotifications] = useState([]);
  const nextId = useRef(0);
  const expiring = useRef(new Set());

  const addNotification = useCallback((notificationBody) => {
    nextId.current += 1;
    const id = nextId.current;
    setNotifications((current) => [...current, { ...notificationBody, id }]);
  }, []);

  const removeNotification = useCallback((id) => {
    expiring.current.delete(id);
    setNotifications((current) =>
      current.filter((notification) => notification.id !== id),
    );
  }, []);

  const notificationShown = useCallback(
    (notification) => {
      if (expiring.current.has(notification.id)) {
        return;
      }
      expiring.current.add(notification.id);
      setTimeout(
        () => removeNotification(notification.id),
        notificationTimeout(notification.kind),
      );
    },
    [removeNotification],
  );

  return {
    notifications,
    addNotification,
    removeNotification,
    notificationShown,
  };
}
