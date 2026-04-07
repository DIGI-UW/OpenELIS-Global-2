import React from "react";
import { useIntl } from "react-intl";
import HomeDashBoard from "./home/Dashboard.tsx";
import PageBreadCrumb from "./common/PageBreadCrumb";

let breadcrumbs = [{ label: "home.label", link: "/" }];

function Home() {
  const intl = useIntl();
  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <div>
        <HomeDashBoard />
      </div>
    </>
  );
}

export default Home;
