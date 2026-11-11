import React, { useContext, useEffect, useState } from "react";
import { IntlProvider, useIntl } from "react-intl";
import { ConfigurationContext } from "../components/layout/contexts";
import {
  NO_OVERRIDE,
  applyOverride,
  loadTranslationOverride,
} from "./translationOverride";

/**
 * Layers a deployment's own translations over the bundled ones.
 *
 * <p>Sits inside ConfigurationContext so the flag comes from the properties the
 * app already fetches once, and inside the app's IntlProvider so the locale and
 * the bundled messages come from the provider that already resolved them —
 * nothing here refetches or re-derives either.
 *
 * <p>Renders a nested IntlProvider only when there is something to apply. With
 * the flag off, or with no override file mounted, it adds nothing to the tree at
 * all and its children see exactly the messages they would have seen anyway.
 */
export default function TranslationOverrideProvider({ children }) {
  const { configurationProperties } = useContext(ConfigurationContext) || {};
  const { locale, defaultLocale, messages } = useIntl();
  const [override, setOverride] = useState(NO_OVERRIDE);

  const enabled =
    String(configurationProperties?.OVERRIDE_DEFAULT_TRANSLATION) === "true";

  useEffect(() => {
    if (!enabled) {
      return undefined;
    }
    let current = true;
    loadTranslationOverride(locale).then((loaded) => {
      // A language switch mid-flight must not be overwritten by the old locale.
      if (current) {
        setOverride(loaded);
      }
    });
    return () => {
      current = false;
    };
  }, [enabled, locale]);

  const overridden = enabled ? applyOverride(messages, override) : messages;
  if (overridden === messages) {
    return children;
  }

  return (
    <IntlProvider
      locale={locale}
      defaultLocale={defaultLocale}
      messages={overridden}
    >
      {children}
    </IntlProvider>
  );
}
