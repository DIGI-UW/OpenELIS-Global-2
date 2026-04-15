import React from "react";
import { injectIntl } from "react-intl";
import HomeDashBoard from "./home/Dashboard.tsx";

function Home() {
  return (
    <>
      <div>
        <HomeDashBoard />
      </div>
    </>
  );
}

export default injectIntl(Home);
