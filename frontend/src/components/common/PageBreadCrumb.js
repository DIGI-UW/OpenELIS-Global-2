import { Breadcrumb, BreadcrumbItem, Column, Grid } from "@carbon/react";
import React from "react";
import { useIntl } from "react-intl";

const PageBreadCrumb = ({ breadcrumbs }) => {
  const intl = useIntl();

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Breadcrumb>
          {breadcrumbs.map((breadcrumb, index) => {
            const labelConfig =
              typeof breadcrumb.label === "string"
                ? { id: breadcrumb.label }
                : breadcrumb.label;

            const message = intl.formatMessage(
              {
                id: labelConfig.id,
                defaultMessage: labelConfig.defaultMessage || labelConfig.id,
              },
              labelConfig.values,
            );

            return (
              <BreadcrumbItem key={index} href={breadcrumb.link}>
                {message}
              </BreadcrumbItem>
            );
          })}
        </Breadcrumb>
      </Column>
    </Grid>
  );
};

export default PageBreadCrumb;
