import React, { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { useIntl } from "react-intl";
import { useLocation } from "react-router-dom";
import { HeaderGlobalAction, Theme } from "@carbon/react";
import {
  Application,
  Bot,
  Chat,
  Close,
  Help,
  Idea,
  MagicWand,
} from "@carbon/icons-react";
import AddonFrame from "./AddonFrame";
import { useSlot } from "./ExtensionSlot";
import { pageOfPath, SLOT_TOP_NAV_ACTIONS, titleFor } from "./slots";

const ICONS: Record<string, React.ComponentType<{ size?: number }>> = {
  application: Application,
  "magic-wand": MagicWand,
  chat: Chat,
  help: Help,
  idea: Idea,
  bot: Bot,
};

/**
 * top-nav-actions-slot: one header button per extension, opening a panel on the
 * right with the extension's page. The page is told only which screen is open
 * (`page`), never the data on it; extensions that need the orders on screen use
 * the side-panel slots of those pages. Without extensions nothing is rendered.
 */
export default function HeaderActionsSlot() {
  const intl = useIntl();
  const extensions = useSlot(SLOT_TOP_NAV_ACTIONS);
  const location = useLocation();
  const [open, setOpen] = useState<string | null>(null);
  const keyOf = (e: { addonId: string; name: string }) =>
    `${e.addonId}/${e.name}`;
  const current = extensions.find((e) => keyOf(e) === open);

  // an extension that was switched off closes its panel
  useEffect(() => {
    if (open && !current) setOpen(null);
  }, [open, current]);

  if (!extensions.length) return null;
  return (
    <>
      {extensions.map((e) => {
        const Icon = ICONS[e.icon || "application"] || Application;
        const active = open === keyOf(e);
        return (
          <HeaderGlobalAction
            key={keyOf(e)}
            aria-label={titleFor(e, intl.locale)}
            isActive={active}
            data-extension-slot-name={SLOT_TOP_NAV_ACTIONS}
            data-addon-id={e.addonId}
            onClick={() => setOpen(active ? null : keyOf(e))}
          >
            {active ? <Close size={20} /> : <Icon size={20} />}
          </HeaderGlobalAction>
        );
      })}
      {current &&
        createPortal(
          <Theme theme="white">
            <aside
              className="addon-header-panel"
              aria-label={titleFor(current, intl.locale)}
              data-testid="addon-header-panel"
            >
              <header className="addon-header-panel__head">
                <span>{titleFor(current, intl.locale)}</span>
                <button
                  type="button"
                  className="addon-header-panel__close"
                  aria-label={intl.formatMessage(
                    { id: "addon.header.close" },
                    { title: titleFor(current, intl.locale) },
                  )}
                  onClick={() => setOpen(null)}
                >
                  <Close size={20} />
                </button>
              </header>
              <AddonFrame
                key={keyOf(current)}
                extension={current}
                state={{ page: pageOfPath(location.pathname) }}
                fill
                showTitle={false}
                frameHeight="calc(100vh - 3rem - 3rem)"
              />
            </aside>
          </Theme>,
          document.body,
        )}
    </>
  );
}
