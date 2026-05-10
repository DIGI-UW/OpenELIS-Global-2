import { Breadcrumb, BreadcrumbItem, Column, Grid } from "@carbon/react";
import React from "react";
import { useIntl } from "react-intl";
import { Link } from "react-router-dom";

const PageBreadCrumb = ({ breadcrumbs }) => {
  const intl = useIntl();

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Breadcrumb>
          {breadcrumbs.map((breadcrumb, index) => {
            const isInternal =
              breadcrumb.link && breadcrumb.link.startsWith("/");
            return (
              <BreadcrumbItem key={index}>
                {isInternal ? (
                  <Link to={breadcrumb.link}>
                    {intl.formatMessage({ id: `${breadcrumb.label}` })}
                  </Link>
                ) : (
                  <a href={breadcrumb.link}>
                    {intl.formatMessage({ id: `${breadcrumb.label}` })}
                  </a>
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
