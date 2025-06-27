import React, { useState, useEffect, useRef } from "react";
import { useIntl } from "react-intl";
import { HeaderGlobalAction, HeaderPanel } from "@carbon/react";
import { Close, Help } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../utils/Utils";

const HelpMenu = ({ helpOpen, handlePanelToggle }) => {
  const intl = useIntl();
  const [helpUrls, setHelpUrls] = useState({
    manual: "",
    tutorials: "",
    "release-notes": "",
  });
  const [error, setError] = useState(null);
  const panelRef = useRef(null);
  const buttonRef = useRef(null);

  // Fetch help URLs on mount
  useEffect(() => {
    getFromOpenElisServer("/rest/properties", (properties, err) => {
      if (err) {
        setError(err);
        console.error("Failed to fetch help URLs:", err);
      } else {
        setHelpUrls({
          manual: properties["org.openelisglobal.help.manual.url"] || "",
          tutorials: properties["org.openelisglobal.help.tutorials.url"] || "",
          "release-notes":
            properties["org.openelisglobal.help.release-notes.url"] || "",
        });
      }
    });
  }, []);

  useEffect(() => {
    const handleClickOutside = (event) => {
      const target = event.target;
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
  const openHelp = (type) => {
    const url = helpUrls[type];
    if (url) {
      window.open(url, "_blank");
      handlePanelToggle("");
    }
  };

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
        style={{ background: "#295785", color: "white" }}
      >
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {["manual", "tutorials", "release-notes"].map((type) => (
            <li key={type}>
              <button
                style={{
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
                }}
                onClick={() => openHelp(type)}
                onMouseEnter={(e) =>
                  (e.target.style.background = "rgba(255,255,255,0.15)")
                }
                onMouseLeave={(e) =>
                  (e.target.style.background = "transparent")
                }
              >
                <Help size={16} />
                {type === "manual"
                  ? intl.formatMessage({ id: "banner.menu.help.usermanual" })
                  : type === "tutorials"
                    ? intl.formatMessage({ id: "banner.menu.help.about" })
                    : intl.formatMessage({ id: "banner.menu.help.contact" })}
              </button>
            </li>
          ))}
        </ul>
      </HeaderPanel>
    </>
  );
};

export default HelpMenu;
