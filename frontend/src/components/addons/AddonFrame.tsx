import React, { useCallback, useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import { InlineLoading, InlineNotification } from "@carbon/react";
import config from "../../config.json";
import { apiBase, requestToken, uiUrl, type AddonExtension } from "./addonApi";
import {
  clampHeight,
  contextMessage,
  parseFrameMessage,
  stateKey,
} from "./bridge";

/**
 * One add-on extension in a sandboxed iframe: `sandbox="allow-scripts
 * allow-forms"` without allow-same-origin, and the backend serves the page with
 * `Content-Security-Policy: sandbox`, so the frame has an opaque origin and
 * cannot read the OpenELIS session, cookies or storage. It gets its context only
 * through postMessage, with a short-lived token scoped to this add-on.
 */
export default function AddonFrame({
  extension,
  state,
  fill = false,
}: {
  extension: AddonExtension;
  state?: Record<string, unknown>;
  fill?: boolean;
}) {
  const intl = useIntl();
  const frame = useRef<HTMLIFrameElement>(null);
  const [height, setHeight] = useState(extension.height || 240);
  const [ready, setReady] = useState(false);
  const [failed, setFailed] = useState(false);
  const key = stateKey(state);

  const sendContext = useCallback(async () => {
    const target = frame.current?.contentWindow;
    if (!target) return;
    try {
      const issued = await requestToken(
        extension.addonId,
        extension.slot,
        state || {},
      );
      setFailed(false);
      target.postMessage(
        contextMessage({
          slot: extension.slot,
          extension: extension.name,
          context: issued.context,
          token: issued.token,
          expiresIn: issued.expiresIn,
          scopes: issued.scopes,
          locale: intl.locale,
          apiBase: apiBase(extension.addonId),
        }),
        "*", // the sandboxed frame's origin is "null"; the target window is fixed
      );
    } catch {
      setFailed(true);
    }
  }, [extension.addonId, extension.slot, extension.name, key, intl.locale]);

  useEffect(() => {
    const onMessage = (event: MessageEvent) => {
      if (!frame.current || event.source !== frame.current.contentWindow) {
        return;
      }
      const message = parseFrameMessage(event.data);
      if (!message) return;
      if (message.type === "ready") {
        setReady(true);
        sendContext();
      } else if (message.type === "token-refresh") {
        sendContext();
      } else if (message.type === "resize" && !fill) {
        setHeight(clampHeight(message.height));
      }
    };
    window.addEventListener("message", onMessage);
    return () => window.removeEventListener("message", onMessage);
  }, [sendContext, fill]);

  // the page state changed (another order selected): new token and context
  useEffect(() => {
    if (ready) sendContext();
  }, [key]);

  return (
    <section
      className="addon-frame"
      data-addon-id={extension.addonId}
      data-extension-name={extension.name}
      aria-label={extension.title}
    >
      <header className="addon-frame__title">
        <span>{extension.title}</span>
        <small>
          {extension.addonName} {extension.version}
        </small>
      </header>
      {!ready && !failed && (
        <InlineLoading
          description={intl.formatMessage({ id: "addon.frame.loading" })}
        />
      )}
      {failed && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "addon.frame.error" })}
        />
      )}
      <iframe
        ref={frame}
        title={extension.title}
        src={uiUrl(config.serverBaseUrl, extension)}
        sandbox="allow-scripts allow-forms"
        referrerPolicy="no-referrer"
        style={{ height: fill ? "calc(100vh - 220px)" : height }}
      />
    </section>
  );
}
