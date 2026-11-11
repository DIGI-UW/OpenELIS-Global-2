import React, { useEffect, useState } from "react";
import { InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";

export const useOnline = () => {
  const [online, setOnline] = useState(() => navigator.onLine);
  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    window.addEventListener("online", update);
    window.addEventListener("offline", update);
    return () => {
      window.removeEventListener("online", update);
      window.removeEventListener("offline", update);
    };
  }, []);
  return online;
};

export default function OfflineNotice() {
  const intl = useIntl();
  return useOnline() ? null : (
    <InlineNotification
      kind="warning"
      lowContrast
      hideCloseButton
      title={intl.formatMessage({ id: "microbiology.offline.notice" })}
    />
  );
}
