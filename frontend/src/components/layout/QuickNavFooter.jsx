import React, { useContext, useMemo } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import { Button } from "@carbon/react";
import {
  Home,
  User,
  UserFollow,
  Barcode,
  DocumentAdd,
  ChartLineData,
  CheckmarkOutline,
  Document,
  TaskView,
  Archive,
  Package,
  Settings,
  DocumentMultiple_01,
  Edit,
  WarningAlt,
  Microscope,
} from "@carbon/icons-react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import quickNavConfig from "../../config/quickNavLinks.json";
import "./QuickNavFooter.css";

const iconMap = {
  Home,
  User,
  UserFollow,
  Barcode,
  DocumentAdd,
  ChartLineData,
  CheckmarkOutline,
  Document,
  TaskView,
  Archive,
  Package,
  Settings,
  DocumentMultiple_01,
  Edit,
  WarningAlt,
  Microscope,
};

const QuickNavFooter = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const userRoles = useMemo(() => {
    if (!userSessionDetails?.authenticated) return [];
    return userSessionDetails.roles || [];
  }, [userSessionDetails]);

  const filteredNavItems = useMemo(() => {
    if (!userSessionDetails?.authenticated) {
      return [];
    }

    const accessibleItems = quickNavConfig.navigationItems.filter((item) => {
      // Items with empty roles array are accessible to all authenticated users
      if (!item.roles || item.roles.length === 0) {
        return true;
      }
      // Check if user has any of the required roles
      return item.roles.some((requiredRole) =>
        userRoles.includes(requiredRole),
      );
    });

    // If user has access to more than 10 items, limit to first 5
    if (accessibleItems.length > 10) {
      return accessibleItems.slice(0, 5);
    }

    return accessibleItems;
  }, [userRoles, userSessionDetails]);

  const handleNavigation = (path) => {
    history.push(path);
  };

  // Don't render footer if user is not authenticated or has no accessible items
  if (!userSessionDetails?.authenticated || filteredNavItems.length === 0) {
    return null;
  }

  return (
    <footer
      className="quick-nav-footer"
      role="navigation"
      aria-label={intl.formatMessage({ id: "nav.quickAccess" })}
    >
      <nav className="quick-nav-container" role="navigation">
        {filteredNavItems.map((item) => {
          const IconComponent = iconMap[item.icon];
          const isActive = location.pathname === item.path;
          const label = intl.formatMessage({ id: item.labelKey });

          return (
            <Button
              key={item.id}
              kind="ghost"
              size="sm"
              className={`quick-nav-item ${isActive ? "quick-nav-item--active" : ""}`}
              onClick={() => handleNavigation(item.path)}
              renderIcon={IconComponent}
              iconDescription={label}
              hasIconOnly={false}
              aria-label={label}
              aria-current={isActive ? "page" : undefined}
              title={label}
            >
              <span className="quick-nav-label">{label}</span>
            </Button>
          );
        })}
      </nav>
    </footer>
  );
};

export default QuickNavFooter;
