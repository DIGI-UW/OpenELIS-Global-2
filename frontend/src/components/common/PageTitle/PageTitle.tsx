/**
 * PageTitle Component
 *
 * Reusable page heading with optional back navigation
 * Used across analyzer pages for consistent page titles
 *
 * Features:
 * - Renders the current page name as the page heading (h1)
 * - Optional back arrow button
 * - Optional subtitle
 *
 * The parent segments of the hierarchy are navigation, not a title, so they are
 * left to the Carbon breadcrumb that every page using this component renders
 * directly above it.
 */

import React from "react";
import { Button } from "@carbon/react";
import { ArrowLeft } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import "./PageTitle.css";

export interface Breadcrumb {
  label: string;
  link?: string;
}

interface PageTitleProps {
  breadcrumbs: Breadcrumb[];
  showBackArrow?: boolean;
  onBack?: () => void;
  subtitle?: string;
}

const PageTitle = ({
  breadcrumbs,
  showBackArrow = false,
  onBack,
  subtitle,
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

  const title = breadcrumbs?.[breadcrumbs.length - 1]?.label;

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
        {title && (
          <h1 className="page-title-heading" data-testid="page-title-heading">
            {title}
          </h1>
        )}
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
