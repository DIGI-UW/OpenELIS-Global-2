/**
 * QIEnabledRoute (OGC-711)
 *
 * Detail-route guard for the QI cascade. Resolves an indicator's qi_config
 * (GET /rest/qi-config/resolve, gated qa.view.qi) and, if it is disabled,
 * redirects to the dashboard with a warning toast. Fail-open: a failed/absent
 * config fetch renders the detail page normally. The resolve is the cached
 * one the wrapped page reads, so the pair shares a single cache entry.
 */

import React, { useContext, useEffect, useState } from "react";
import { Redirect } from "react-router-dom";
import { Loading } from "@carbon/react";
import { useIntl } from "react-intl";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import useQiConfig from "./useQiConfig";

const QIEnabledRoute = ({ indicator, children }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { enabled, loading } = useQiConfig(indicator);
  // Raised by the effect below, so the toast is fired while this guard is
  // still mounted: the <Redirect> unmounts it in a layout effect that would
  // otherwise run first, and the toast would never show.
  const [redirect, setRedirect] = useState(false);

  useEffect(() => {
    if (loading || enabled) {
      return;
    }
    addNotification({
      kind: NotificationKinds.warning,
      title: intl.formatMessage({ id: "qa.qi.disabled.toast.title" }),
      message: intl.formatMessage({ id: "qa.qi.disabled.toast.message" }),
    });
    setNotificationVisible(true);
    setRedirect(true);
  }, [loading, enabled]);

  if (redirect) {
    return <Redirect to="/qa/qi/dashboard" />;
  }
  if (loading || !enabled) {
    return <Loading small withOverlay={false} />;
  }
  return children;
};

export default QIEnabledRoute;
