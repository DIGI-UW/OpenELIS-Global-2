import { Breadcrumb, BreadcrumbItem, Column, Grid } from "@carbon/react";
import React from "react";
import { useIntl } from "react-intl";
import { Link } from "react-router-dom";

// Carbon's BreadcrumbItem with an `href` prop renders a plain <a>, which
// triggers a full page reload on click. Passing a React Router <Link>
// as the BreadcrumbItem child preserves SPA navigation.
//
// Current-page semantics: each breadcrumb entry may set `isCurrentPage`
// explicitly. If no entry sets it AND there are multiple entries, the
// last one is treated as the current page for backward compatibility
// with existing callers that pass `[{Home}, {SubPage}]`. A *single* crumb
// is always rendered as a link — pages that pass just `[{Home}]` (e.g.
// reports/Index.jsx) expect that Home stays clickable.
const PageBreadCrumb = ({ breadcrumbs }) => {
  const intl = useIntl();
  const anyExplicitCurrent = breadcrumbs.some((b) => b.isCurrentPage === true);
  const lastIndex = breadcrumbs.length - 1;
  const defaultsLastAsCurrent = !anyExplicitCurrent && breadcrumbs.length > 1;

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Breadcrumb>
          {breadcrumbs.map((breadcrumb, index) => {
            const label = intl.formatMessage({ id: breadcrumb.label });
            const isCurrent =
              breadcrumb.isCurrentPage === true ||
              (defaultsLastAsCurrent && index === lastIndex);
            return (
              <BreadcrumbItem
                key={index}
                isCurrentPage={isCurrent}
                aria-current={isCurrent ? "page" : undefined}
              >
                {isCurrent ? (
                  <span>{label}</span>
                ) : (
                  <Link to={breadcrumb.link}>{label}</Link>
                )}
              </BreadcrumbItem>
            );
          })}
        </Breadcrumb>
      </Column>
    </Grid>
  );
};

export default PageBreadCrumb;
