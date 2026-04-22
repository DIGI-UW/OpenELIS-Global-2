import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Switch, Route, useRouteMatch } from "react-router-dom";
import VectorGroupsPage from "./VectorGroupsPage";
import VectorSpeciesPage from "./VectorSpeciesPage";
import VectorTrapTypesPage from "./VectorTrapTypesPage";

function VectorSurveillanceSetup() {
  const { path } = useRouteMatch();

  return (
    <div className="adminPageContent">
      <Switch>
        <Route path={`${path}/species`} component={VectorSpeciesPage} />
        <Route path={`${path}/trap-types`} component={VectorTrapTypesPage} />
        <Route component={VectorGroupsPage} />
      </Switch>
    </div>
  );
}

export default VectorSurveillanceSetup;
