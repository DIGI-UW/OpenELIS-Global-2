import React from "react";
import { useIntl } from "react-intl";
import HomeDashBoard from "./home/Dashboard.tsx";
import PageBreadCrumb from "./common/PageBreadCrumb";
import PageTitle from "./common/PageTitle/PageTitle";

const breadcrumbs = [{ label: "home.label", link: "/" }];

function Home() {
  const intl = useIntl();
  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <PageTitle
        breadcrumbs={[
          { label: intl.formatMessage({ id: "home.dashboard.title" }) },
        ]}
      />

      <div>
        <HomeDashBoard />
      </div>
    </>
  );
}

export default Home;
