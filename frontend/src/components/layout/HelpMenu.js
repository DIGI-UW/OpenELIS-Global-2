import React, { useState, useEffect, useRef } from "react";
import { useIntl } from "react-intl";
import { HeaderGlobalAction, HeaderPanel } from "@carbon/react";
import { Help } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../utils/Utils";

const HelpMenu = () => {
  const intl = useIntl();
  const [isExpanded, setIsExpanded] = useState(false);
  const [helpUrls, setHelpUrls] = useState({
    manual: "",
    tutorials: "",
    "release-notes": "",
  });
  const [error, setError] = useState(null);
  const panelRef = useRef(null);
  const buttonRef = useRef(null);

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
      if (
        isExpanded &&
        panelRef.current &&
        !panelRef.current.contains(event.target) &&
        buttonRef.current &&
        !buttonRef.current.contains(event.target)
      ) {
        setIsExpanded(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isExpanded]);

  const togglePanel = () => setIsExpanded(!isExpanded);

  const openHelp = (type) => {
    const url = helpUrls[type];
    if (url) {
      window.open(url, "_blank");
      setIsExpanded(false);
    }
  };

  return (
    <>
      <HeaderGlobalAction
        ref={buttonRef}
        id="user-Help"
        aria-label="Help"
        onClick={togglePanel}
        isActive={isExpanded}
      >
        <Help size={20} />
      </HeaderGlobalAction>
      <HeaderPanel
        ref={panelRef}
        aria-label="Help Panel"
        expanded={isExpanded}
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
