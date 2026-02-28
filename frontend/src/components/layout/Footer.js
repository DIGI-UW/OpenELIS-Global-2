import React, { useState, useEffect } from "react";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

function Footer() {
  const currentYear = new Date().getFullYear();
  const [helpUrls, setHelpUrls] = useState({
    manual: "",
    tutorials: "",
    "release-notes": "",
  });

  useEffect(() => {
    let isMounted = true;
    getFromOpenElisServer("/rest/properties", (properties) => {
      if (isMounted && properties) {
        setHelpUrls({
          manual: properties["org.openelisglobal.help.manual.url"] || "",
          tutorials: properties["org.openelisglobal.help.tutorials.url"] || "",
          "release-notes":
            properties["org.openelisglobal.help.release-notes.url"] || "",
        });
      }
    });
    return () => {
      isMounted = false;
    };
  }, []);

  const openLink = (url) => {
    if (url) {
      window.open(url, "_blank");
    }
  };

  return (
    <footer className="oe-footer py-4 mt-auto">
      <div className="oe-footer-content">
        <div className="footer-links">
          <span
            className={`footer-link-item ${!helpUrls.manual ? "disabled" : ""}`}
            onClick={() => openLink(helpUrls.manual)}
          >
            <FormattedMessage id="banner.menu.help.usermanual" />
          </span>
          <span className="footer-link-separator">|</span>
          <span
            className={`footer-link-item ${!helpUrls.tutorials ? "disabled" : ""}`}
            onClick={() => openLink(helpUrls.tutorials)}
          >
            <FormattedMessage id="banner.menu.help.about" />
          </span>
          <span className="footer-link-separator">|</span>
          <span
            className={`footer-link-item ${!helpUrls["release-notes"] ? "disabled" : ""}`}
            onClick={() => openLink(helpUrls["release-notes"])}
          >
            <FormattedMessage id="banner.menu.help.contact" />
          </span>
        </div>
        <p className="footer-copyright">
          <FormattedMessage
            id="footer.copyright"
            values={{ year: currentYear }}
          />
        </p>
      </div>
    </footer>
  );
}

export default Footer;
