import React, { useState, useEffect } from "react";
import { useIntl } from "react-intl";
import { Help } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../utils/Utils";

export default function SlideOverHelp() {
  const intl = useIntl();
  const [helpUrls, setHelpUrls] = useState({
    manual: "",
    tutorials: "",
    "release-notes": "",
  });
  const [error, setError] = useState(null);

  // Fetch help URLs on mount
  useEffect(() => {
    let isMounted = true;

    getFromOpenElisServer("/rest/properties", (properties) => {
      if (!isMounted) return;

      // The API helper calls callback with `undefined` when response is
      // not JSON (e.g., auth redirect HTML). Treat that as "no configured help
      // URLs" rather than crashing the entire app.
      if (!properties || typeof properties !== "object") {
        setHelpUrls({ manual: "", tutorials: "", "release-notes": "" });
        setError(new Error("Help URL configuration unavailable"));
        return;
      }

      setHelpUrls({
        manual: properties["org.openelisglobal.help.manual.url"] || "",
        tutorials: properties["org.openelisglobal.help.tutorials.url"] || "",
        "release-notes":
          properties["org.openelisglobal.help.release-notes.url"] || "",
      });
    });

    return () => {
      isMounted = false;
    };
  }, []);

  // Opens help URL in a new window
  const openHelp = (type) => {
    const url = helpUrls[type];
    if (url) {
      window.open(url, "_blank");
    }
  };

  const HelpButton = ({ type, label }) => (
    <button
      className="help-slide-button"
      onClick={() => openHelp(type)}
      disabled={!helpUrls[type]}
    >
      <Help size={16} className="help-slide-icon" />
      <span className="help-slide-text">{label}</span>
    </button>
  );

  return (
    <div className="help-slide-panel">
      <div className="help-slide-content">
        <div className="help-slide-body">
          {error ? (
            <div className="help-slide-error">
              {intl.formatMessage({ id: "help.error.unavailable" })}
            </div>
          ) : (
            <div className="help-slide-buttons">
              <HelpButton
                type="manual"
                label={intl.formatMessage({
                  id: "banner.menu.help.usermanual",
                })}
              />
              <HelpButton
                type="tutorials"
                label={intl.formatMessage({ id: "banner.menu.help.about" })}
              />
              <HelpButton
                type="release-notes"
                label={intl.formatMessage({
                  id: "banner.menu.help.contact",
                })}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
