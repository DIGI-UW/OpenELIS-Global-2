/**
 * PageTitle Component
 *
 * Reusable hierarchical breadcrumb page title with optional back navigation
 * Used across analyzer pages for consistent navigation UI
 *
 * Features:
 * - Hierarchical breadcrumb display (e.g., "Analyzers > Field Mappings > Hematology Analyzer 1")
 * - Optional back arrow button
 * - Clickable breadcrumb links for navigation
 * - Internationalized separator
 */

import React from "react";
import { Button } from "@carbon/react";
import { ArrowLeft } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import "./PageTitle.css";

interface Breadcrumb {
  label: string;
  link?: string;
}

interface PageTitleProps {
  breadcrumbs: Breadcrumb[];
  showBackArrow?: boolean;
  onBack?: (() => void) | null;
  subtitle?: string | null;
}

const PageTitle = ({
  breadcrumbs,
  showBackArrow = false,
  onBack = null,
  subtitle = null,
}: PageTitleProps) => {
  const intl = useIntl();
  const history = useHistory();

  const handleBack = () => {
    if (onBack) {
      onBack();
    } else if (breadcrumbs && breadcrumbs.length > 1) {
      // Navigate to parent breadcrumb if available
      const parentBreadcrumb = breadcrumbs[breadcrumbs.length - 2];
      if (parentBreadcrumb && parentBreadcrumb.link) {
        history.push(parentBreadcrumb.link);
      } else {
        history.goBack();
      }
    } else {
      history.goBack();
    }
  };

  const separator = intl.formatMessage({
    id: "page.breadcrumb.separator",
    defaultMessage: " > ",
  });

  return (
    <div className="page-title" data-testid="page-title">
      <div className="page-title-header">
        {showBackArrow && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={ArrowLeft}
            iconDescription={intl.formatMessage({ id: "page.title.back" })}
            hasIconOnly
            onClick={handleBack}
            data-testid="page-title-back-button"
            className="page-title-back-button"
          />
        )}
        <div
          className="page-title-breadcrumbs"
          data-testid="page-title-breadcrumbs"
        >
          {breadcrumbs &&
            breadcrumbs.map((crumb, index) => (
              <React.Fragment key={index}>
                {crumb.link ? (
                  <button
                    className="page-title-breadcrumb-link"
                    onClick={() => history.push(crumb.link)}
                    data-testid={`breadcrumb-link-${index}`}
                  >
                    {crumb.label}
                  </button>
                ) : (
                  <span
                    className="page-title-breadcrumb-current"
                    data-testid={`breadcrumb-current-${index}`}
                  >
                    {crumb.label}
                  </span>
                )}
                {index < breadcrumbs.length - 1 && (
                  <span
                    className="page-title-breadcrumb-separator"
                    data-testid={`breadcrumb-separator-${index}`}
                  >
                    {separator}
                  </span>
                )}
              </React.Fragment>
            ))}
        </div>
      </div>
      {subtitle && (
        <div className="page-title-subtitle" data-testid="page-title-subtitle">
          {subtitle}
        </div>
      )}
    </div>
  );
};

export default PageTitle;
