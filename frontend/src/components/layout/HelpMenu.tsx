import React, { useState, useEffect, useRef } from "react";
import { useIntl } from "react-intl";
import { HeaderGlobalAction, HeaderPanel } from "@carbon/react";
import { Close, Help } from "@carbon/icons-react";

import { getFromOpenElisServer } from "../utils/Utils";

type HelpUrlKey = "manual" | "tutorials" | "release-notes";

type HelpUrls = Record<HelpUrlKey, string>;

interface HelpMenuProps {
  /** Whether the help panel is currently visible. */
  helpOpen: boolean;
  /**
   * Toggle a named panel.
   * Pass `""` to close the currently-open panel;
   * pass `"help"` to open this one.
   */
  handlePanelToggle: (panel: string) => void;
}

const panelStyle: React.CSSProperties = {
  background: "#295785",
  color: "white",
};

const listStyle: React.CSSProperties = {
  listStyle: "none",
  padding: 0,
  margin: 0,
};

const buttonStyle: React.CSSProperties = {
  width: "100%",
  padding: "1rem 1.5rem",
  background: "transparent",
  border: "none",
  cursor: "pointer",
  textAlign: "left",
  transition: "all 0.2s ease",
  display: "flex",
  alignItems: "center",
  gap: "0.75rem",
  color: "white",
};

// Component

const HELP_URL_KEYS: readonly HelpUrlKey[] = [
  "manual",
  "tutorials",
  "release-notes",
] as const;

const EMPTY_URLS: HelpUrls = {
  manual: "",
  tutorials: "",
  "release-notes": "",
};

const LABEL_IDS: Record<HelpUrlKey, string> = {
  manual: "banner.menu.help.usermanual",
  tutorials: "banner.menu.help.about",
  "release-notes": "banner.menu.help.contact",
};

const PROPERTY_KEYS: Record<HelpUrlKey, string> = {
  manual: "org.openelisglobal.help.manual.url",
  tutorials: "org.openelisglobal.help.tutorials.url",
  "release-notes": "org.openelisglobal.help.release-notes.url",
};

const HelpMenu: React.FC<HelpMenuProps> = ({ helpOpen, handlePanelToggle }) => {
  const intl = useIntl();
  const [helpUrls, setHelpUrls] = useState<HelpUrls>(EMPTY_URLS);
  const [error, setError] = useState<Error | null>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  // Fetch help URLs on mount

  useEffect(() => {
    let isMounted = true;

    getFromOpenElisServer(
      "/rest/properties",
      (properties: Record<string, string> | undefined) => {
        if (!isMounted) return;

        if (!properties || typeof properties !== "object") {
          setHelpUrls(EMPTY_URLS);
          setError(new Error("Help URL configuration unavailable"));
          return;
        }

        setHelpUrls({
          manual: properties[PROPERTY_KEYS.manual] || "",
          tutorials: properties[PROPERTY_KEYS.tutorials] || "",
          "release-notes": properties[PROPERTY_KEYS["release-notes"]] || "",
        });
      },
    );

    return () => {
      isMounted = false;
    };
  }, []);

  // Click-outside handler: close the panel when clicking elsewhere

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent): void => {
      const target = event.target as Node;
      if (!helpOpen) return;

      const isClickInsidePanel = panelRef.current?.contains(target);
      const isClickOnHelpButton = buttonRef.current?.contains(target);

      const globalActionClicked =
        document.getElementById("search-Icon")?.contains(target) ||
        document.getElementById("notification-Icon")?.contains(target) ||
        document.getElementById("user-Icon")?.contains(target);

      if (!isClickInsidePanel && !isClickOnHelpButton && !globalActionClicked) {
        handlePanelToggle("");
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [helpOpen, handlePanelToggle]);

  // Opens the help URL in a new window and then closes the help panel

  const openHelp = (type: HelpUrlKey): void => {
    const url = helpUrls[type];
    if (url) {
      window.open(url, "_blank");
      handlePanelToggle("");
    }
  };

  // Render

  return (
    <>
      <HeaderGlobalAction
        ref={buttonRef}
        id="user-Help"
        aria-label="Help"
        onClick={() => {
          handlePanelToggle(helpOpen ? "" : "help");
        }}
        isActive={helpOpen}
      >
        {!helpOpen ? <Help size={20} /> : <Close size={20} />}
      </HeaderGlobalAction>
      <HeaderPanel
        ref={panelRef}
        aria-label="Help Panel"
        expanded={helpOpen}
        style={panelStyle}
      >
        <ul style={listStyle}>
          {HELP_URL_KEYS.map((type) => (
            <li key={type}>
              <button
                style={buttonStyle}
                onClick={() => openHelp(type)}
                onMouseEnter={(e: React.MouseEvent<HTMLButtonElement>) =>
                  ((e.target as HTMLButtonElement).style.background =
                    "rgba(255,255,255,0.15)")
                }
                onMouseLeave={(e: React.MouseEvent<HTMLButtonElement>) =>
                  ((e.target as HTMLButtonElement).style.background =
                    "transparent")
                }
              >
                <Help size={16} />
                {intl.formatMessage({ id: LABEL_IDS[type] })}
              </button>
            </li>
          ))}
        </ul>
      </HeaderPanel>
    </>
  );
};

export default HelpMenu;
