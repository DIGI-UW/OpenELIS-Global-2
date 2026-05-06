import React from "react";
import { Switch, Route, useRouteMatch, Redirect } from "react-router-dom";
import VectorSpeciesPage from "./VectorSpeciesPage";
import VectorTrapTypesPage from "./VectorTrapTypesPage";
import VectorSamplingSitesPage from "./VectorSamplingSitesPage";

function VectorSurveillanceSetup() {
  const { path } = useRouteMatch();

  return (
    <div className="adminPageContent">
      <Switch>
        <Route path={`${path}/species`} component={VectorSpeciesPage} />
        <Route path={`${path}/trap-types`} component={VectorTrapTypesPage} />
        <Route
          path={`${path}/sampling-sites`}
          component={VectorSamplingSitesPage}
        />
        <Redirect to={`${path}/species`} />
      </Switch>
    </div>
  );
}

export default VectorSurveillanceSetup;
