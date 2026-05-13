import { Breadcrumb, BreadcrumbItem, Column, Grid } from "@carbon/react";
import React from "react";
import { useIntl } from "react-intl";
import { Link } from "react-router-dom";

// Carbon's BreadcrumbItem with an `href` prop renders a plain <a>, which
// triggers a full page reload on click. Passing a React Router <Link>
// as the BreadcrumbItem child preserves SPA navigation. Last crumb is
// the current page — rendered as a non-interactive <span> with the
// `isCurrentPage` flag for accessibility.
const PageBreadCrumb = ({ breadcrumbs }) => {
  const intl = useIntl();
  const lastIndex = breadcrumbs.length - 1;

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Breadcrumb>
          {breadcrumbs.map((breadcrumb, index) => {
            const label = intl.formatMessage({ id: breadcrumb.label });
            const isCurrent = index === lastIndex;
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
