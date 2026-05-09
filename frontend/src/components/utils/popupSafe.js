export const openPopupSafe = ({
  url,
  addNotification,
  setNotificationVisible,
  intl,
  NotificationKinds,
}) => {
  const popup = window.open(url, "_blank");

  if (!popup) {
    console.warn("window.open returned null for", url);

    addNotification({
      kind: NotificationKinds.error,
      title: intl.formatMessage({
        id: "notification.title",
      }),
      message: intl.formatMessage({
        id: "label.print.error.popupBlocked",
        defaultMessage:
          "Popup blocked. Please allow popups for this site.",
      }),
    });

    setNotificationVisible(true);

    return false;
  }

  return true;
};