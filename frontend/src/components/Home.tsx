import React from "react";
import { injectIntl, IntlShape } from "react-intl";
import HomeDashBoard from "./home/Dashboard";
import PageBreadCrumb from "./common/PageBreadCrumb";

interface HomeProps {
  intl: IntlShape;
}

const breadcrumbs = [{ label: "home.label", link: "/" }];

const Home: React.FC<HomeProps> = ({ intl }) => {
  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <div>
        <HomeDashBoard />
      </div>
    </>
  );
};

export default injectIntl(Home as React.ComponentType<any>);
