import React, { useContext } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import quickNavConfig from "../../config/quickNavLinks.json";
import * as CarbonIcons from "@carbon/icons-react";

import "../Style.css";

function QuickNavigationFooter() {
  const history = useHistory();
  const { t } = useTranslation();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  console.log(" userSessionDetails:", userSessionDetails);

  const isLoggedIn = userSessionDetails?.authenticated;
  const userRoles = userSessionDetails?.roles || [];

  if (!isLoggedIn || userRoles.length === 0) return null;

  console.log("Resolved Roles:", userRoles);

  let allowedLinks = quickNavConfig.filter((link) =>
    link.roles.some((role) => userRoles.includes(role))
  );

  if (allowedLinks.length > 5) {
    allowedLinks = allowedLinks.slice(0, 5);
  }

  console.log("FINAL ALLOWED LINKS:", allowedLinks);

  return (
    <footer
      className="oe-quick-footer"
      role="navigation"
      aria-label={t("footer.quickNavigation")}
    >
      <div className="footer-nav-container">
        {allowedLinks.map((item) => {
          const IconComponent = CarbonIcons[item.icon];

          if (!IconComponent) {
            console.error(" Missing Icon:", item.icon);
            return null;
          }

          return (
            <div
              key={item.route}
              className="footer-nav-item"
              role="button"
              tabIndex="0"
              aria-label={t(item.labelKey)}
              onClick={() => history.push(item.route)}
              onKeyDown={(e) => e.key === "Enter" && history.push(item.route)}
            >
              <IconComponent size={20} aria-hidden="true" />
              <span>
                                    {t(item.labelKey)
                                        .replace(/^nav\./, '')
                                        .replace(/^\w/, c => c.toUpperCase())}
                                </span>
            </div>
          );
        })}
      </div>
    </footer>
  );
}

export default QuickNavigationFooter;
