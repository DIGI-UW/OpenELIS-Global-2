import React from "react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import "./AnalyzerPageHeader.css";

export interface AnalyzerBreadcrumb {
  label: string;
  link?: string;
  isCurrentPage?: boolean;
}

/**
 * The one page header every analyzer page uses: the breadcrumb, the title
 * with its subtitle, and the page's actions on the title line.
 */
const AnalyzerPageHeader = ({
  breadcrumbs,
  title,
  subtitle,
  actions,
  testId,
  titleId,
}: {
  breadcrumbs: AnalyzerBreadcrumb[];
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  actions?: React.ReactNode;
  testId?: string;
  titleId?: string;
}) => (
  <header className="analyzer-page-header" data-testid={testId}>
    <PageBreadCrumb breadcrumbs={breadcrumbs} noTrailingSlash />
    <div className="analyzer-page-header__row">
      <div className="analyzer-page-header__title">
        <h1 id={titleId}>{title}</h1>
        {subtitle && (
          <p className="analyzer-page-header__subtitle">{subtitle}</p>
        )}
      </div>
      {actions && (
        <div className="analyzer-page-header__actions">{actions}</div>
      )}
    </div>
  </header>
);

export default AnalyzerPageHeader;
