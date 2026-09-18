/**
 * QIEnabledRoute (OGC-711)
 *
 * Detail-route guard for the QI cascade. Resolves an indicator's qi_config
 * (GET /rest/qi-config/resolve, gated qa.view.qi) and, if it is disabled,
 * redirects to the dashboard with a warning toast. Fail-open: a failed/absent
 * config fetch renders the detail page normally.
 */

import React, { useContext, useEffect, useState } from "react";
import { Redirect } from "react-router-dom";
import { Loading } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";

const QIEnabledRoute = ({ indicator, children }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  // "loading" | "enabled" | "disabled"
  const [state, setState] = useState("loading");

  useEffect(() => {
    getFromOpenElisServer(
      `/rest/qi-config/resolve?indicator=${indicator}`,
      (res) => {
        if (res && res.enabled === false) {
          // Fire the toast here, not in a state-driven effect: the <Redirect>
          // below unmounts this guard in a layout effect that runs before a
          // useEffect would, so a deferred toast would never show.
          addNotification({
            kind: NotificationKinds.warning,
            title: intl.formatMessage({ id: "qa.qi.disabled.toast.title" }),
            message: intl.formatMessage({ id: "qa.qi.disabled.toast.message" }),
          });
          setNotificationVisible(true);
          setState("disabled");
        } else {
          setState("enabled");
        }
      },
    );
  }, [indicator]);

  if (state === "loading") {
    return <Loading small withOverlay={false} />;
  }
  if (state === "disabled") {
    return <Redirect to="/qa/qi/dashboard" />;
  }
  return children;
};

export default QIEnabledRoute;
