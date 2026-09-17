import React from "react";
import { Switch, Route, useRouteMatch, Redirect } from "react-router-dom";
import SampleAcceptanceChecklistDomain from "./SampleAcceptanceChecklistDomain";

/**
 * S-09 (OGC-580) Sample Acceptance Checklist admin, reached at
 * Admin → General Configuration → Order Entry Configuration → Sample Acceptance
 * Checklist. One screen per navigation target (All domains / Clinical /
 * Environmental / Vector), selected by the SideNav submenu items.
 */
function SampleAcceptanceChecklistSetup() {
  const { path } = useRouteMatch();

  return (
    <div className="adminPageContent">
      <Switch>
        <Route
          path={`${path}/all`}
          render={() => <SampleAcceptanceChecklistDomain domain="ALL" />}
        />
        <Route
          path={`${path}/clinical`}
          render={() => <SampleAcceptanceChecklistDomain domain="CLINICAL" />}
        />
        <Route
          path={`${path}/environmental`}
          render={() => (
            <SampleAcceptanceChecklistDomain domain="ENVIRONMENTAL" />
          )}
        />
        <Route
          path={`${path}/vector`}
          render={() => <SampleAcceptanceChecklistDomain domain="VECTOR" />}
        />
        <Redirect to={`${path}/all`} />
      </Switch>
    </div>
  );
}

export default SampleAcceptanceChecklistSetup;
