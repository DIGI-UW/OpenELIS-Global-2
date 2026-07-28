import React, { type ReactNode } from "react";
import { Breadcrumb, BreadcrumbItem, Button } from "@carbon/react";
import { ArrowLeft } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { Link, useHistory } from "react-router-dom";
import "./PageHeader.css";

export interface PageBreadcrumb {
  label: string;
  link?: string;
}

interface PageHeaderProps {
  breadcrumbs: PageBreadcrumb[];
  title?: string;
  subtitle?: string;
  actions?: ReactNode;
  showBackArrow?: boolean;
  onBack?: () => void;
}

const PageHeader = ({
  breadcrumbs,
  title,
  subtitle,
  actions,
  showBackArrow = false,
  onBack,
}: PageHeaderProps) => {
  const intl = useIntl();
  const history = useHistory();
  const heading = title || breadcrumbs.at(-1)?.label || "";

  const handleBack = () => {
    if (onBack) {
      onBack();
      return;
    }

    const parent = breadcrumbs.at(-2);
    if (parent?.link) {
      history.push(parent.link);
      return;
    }
    history.goBack();
  };

  return (
    <header className="page-header" data-testid="page-header">
      <div className="page-header__navigation">
        {showBackArrow && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={ArrowLeft}
            iconDescription={intl.formatMessage({ id: "page.title.back" })}
            hasIconOnly
            onClick={handleBack}
            data-testid="page-header-back-button"
          />
        )}
        <Breadcrumb noTrailingSlash data-testid="page-header-breadcrumbs">
          {breadcrumbs.map((crumb, index) => {
            const isCurrent = index === breadcrumbs.length - 1;
            return (
              <BreadcrumbItem
                key={`${crumb.label}-${index}`}
                isCurrentPage={isCurrent}
              >
                {crumb.link && !isCurrent ? (
                  <Link to={crumb.link}>{crumb.label}</Link>
                ) : (
                  crumb.label
                )}
              </BreadcrumbItem>
            );
          })}
        </Breadcrumb>
      </div>
      <div className="page-header__content">
        <div className="page-header__copy">
          <h1>{heading}</h1>
          {subtitle && <p>{subtitle}</p>}
        </div>
        {actions && <div className="page-header__actions">{actions}</div>}
      </div>
    </header>
  );
};

export default PageHeader;
