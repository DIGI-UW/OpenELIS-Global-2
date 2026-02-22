import { Breadcrumb, BreadcrumbItem, Column, Grid } from "@carbon/react";
import React, { memo } from "react";
import { useIntl } from "react-intl";

interface BreadcrumbData {
  label: string;
  link: string;
}

interface PageBreadCrumbProps {
  breadcrumbs: BreadcrumbData[];
}

const PageBreadCrumb: React.FC<PageBreadCrumbProps> = ({ breadcrumbs }) => {
  const intl = useIntl();

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Breadcrumb>
          {breadcrumbs.map((breadcrumb, index) => {
            return (
              <BreadcrumbItem key={index} href={breadcrumb.link}>
                {intl.formatMessage({ id: `${breadcrumb.label}` })}
              </BreadcrumbItem>
            );
          })}
        </Breadcrumb>
      </Column>
    </Grid>
  );
};

export default memo(PageBreadCrumb);
