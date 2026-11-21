import React from "react";
import { useIntl } from "react-intl";
import HomeDashBoard from "./home/Dashboard.tsx";
import PageBreadCrumb from "./common/PageBreadCrumb";
import PageTitle from "./common/PageTitle/PageTitle";
import { Column, Grid } from "@carbon/react";

const breadcrumbs = [{ label: "home.label", link: "/" }];

function Home() {
  const intl = useIntl();
  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <PageTitle
            breadcrumbs={[
              { label: intl.formatMessage({ id: "home.dashboard.title" }) },
            ]}
          />
        </Column>
      </Grid>

      <div>
        <HomeDashBoard />
      </div>
    </>
  );
}

export default Home;
