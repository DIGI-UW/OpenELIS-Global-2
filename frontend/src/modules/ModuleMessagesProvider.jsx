import React, { useMemo } from "react";
import { IntlProvider, useIntl } from "react-intl";
import { useModuleHost } from "./ModuleHostProvider";
import { pickModuleMessages } from "./moduleRegistry";
import { useDevModuleMessages } from "./devModuleMessages";

/**
 * Merges every started module's messages for the active locale into the
 * messages the shell renders with, so module menu labels, pages and slot
 * contributions translate through the one `IntlProvider` tree. The bundles
 * arrive with the registry (the backend reads them from the module JAR), so
 * nothing is fetched here and the labels are right on their first render.
 * Only keys under `mod.<id>.` are taken, so a module can never override a
 * core string. Each key comes from the locale's bundle, then the base
 * language's, then the English one, so a partial translation never shows
 * raw message ids. A development shell also reads the bundles a module's dev
 * server serves and layers them over the registry's, so a saved translation
 * shows without rebuilding the module (see useDevModuleMessages).
 */
export default function ModuleMessagesProvider({ children }) {
  const host = useModuleHost();
  const intl = useIntl();
  const locale = intl.locale;
  const devBundles = useDevModuleMessages(host.modules, locale);

  const merged = useMemo(() => {
    const extra = {};
    host.modules.forEach((module) => {
      const messages = pickModuleMessages(
        { ...module.messages, ...devBundles[module.id] },
        locale,
      );
      Object.keys(messages).forEach((key) => {
        if (
          key.startsWith(module.permissionPrefix) &&
          typeof messages[key] === "string"
        ) {
          extra[key] = messages[key];
        }
      });
    });
    return extra;
  }, [host.modules, locale, devBundles]);

  const messages = useMemo(
    () => ({ ...intl.messages, ...merged }),
    [intl.messages, merged],
  );

  // Always the same tree shape: wrapping only once module messages arrive
  // would remount everything below (the header, the page in progress).
  return (
    <IntlProvider
      locale={intl.locale}
      defaultLocale={intl.defaultLocale}
      messages={messages}
      onError={intl.onError}
    >
      {children}
    </IntlProvider>
  );
}
