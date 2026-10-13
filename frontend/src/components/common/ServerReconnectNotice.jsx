import React, { useEffect, useState } from "react";
import { Button, InlineNotification, Theme } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./ServerReconnectNotice.scss";

const secondsUntil = (retryAt) =>
  Math.max(0, Math.ceil((retryAt - Date.now()) / 1000));

/**
 * Non-blocking status shown while the app shell cannot reach the server
 * (OGC-1442). It counts down to the next automatic attempt and offers an
 * immediate one; it never blocks the page or navigates away, and it sits above
 * the loading overlay so "Try now" stays clickable. It renders outside the
 * page's content area, so it sets the light theme itself.
 */
export default function ServerReconnectNotice({ retryAt, onTryNow }) {
  const intl = useIntl();
  const [seconds, setSeconds] = useState(() => secondsUntil(retryAt));

  useEffect(() => {
    setSeconds(secondsUntil(retryAt));
    const timer = setInterval(() => setSeconds(secondsUntil(retryAt)), 1000);
    return () => clearInterval(timer);
  }, [retryAt]);

  const trying = seconds === 0;

  return (
    <Theme
      theme="white"
      className="server-reconnect-notice"
      data-testid="server-reconnect"
    >
      <InlineNotification
        kind="warning"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "serverReconnect.title" })}
      >
        <span
          aria-hidden="true"
          className="server-reconnect-notice__countdown"
          data-testid="server-reconnect-countdown"
        >
          {trying ? (
            <FormattedMessage id="serverReconnect.trying" />
          ) : (
            <FormattedMessage
              id="serverReconnect.retryIn"
              values={{ seconds }}
            />
          )}
        </span>
      </InlineNotification>
      <Button
        kind="tertiary"
        size="sm"
        disabled={trying}
        onClick={onTryNow}
        data-testid="server-reconnect-try-now"
      >
        <FormattedMessage id="serverReconnect.tryNow" />
      </Button>
    </Theme>
  );
}
