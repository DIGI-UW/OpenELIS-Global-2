import React from "react";
import { injectIntl } from "react-intl";
import HomeDashBoard from "./home/Dashboard.tsx";
import PageBreadCrumb from "./common/PageBreadCrumb.js";
import LocationReportingPrompt from "./common/LocationReportingPrompt";

let breadcrumbs = [{ label: "home.label", link: "/" }];

function Home() {
  return (
    <>
      <LocationReportingPrompt />
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <div>
        <HomeDashBoard />
      </div>
    </>
  );
}

export default injectIntl(Home);
