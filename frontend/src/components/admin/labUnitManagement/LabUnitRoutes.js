import React, { useState, useEffect } from "react";
import { Switch, Route, useHistory } from "react-router-dom";
import LabUnitsManagement from "./LabUnitsManagement";
import LabUnitEditor from "../../labunit/LabUnitEditor";
import { getFromOpenElisServer } from "../../utils/Utils";

const LabUnitEditRoute = ({ match }) => {
  const [unit, setUnit] = useState(null);
  const history = useHistory();

  useEffect(() => {
    if (match.params.id) {
      getFromOpenElisServer(
        `/rest/api/lab-units/${match.params.id}`,
        (response) => {
          if (response) {
            setUnit(response);
          }
        },
      );
    }
  }, [match.params.id]);

  return (
    <LabUnitEditor
      unit={unit}
      onBack={() => history.push("/MasterListsPage/labUnitManagement")}
    />
  );
};

const LabUnitRoutes = () => {
  const history = useHistory();

  return (
    <Switch>
      <Route path="/labUnitManagement" exact component={LabUnitsManagement} />
      <Route
        path="/labUnitManagement/add"
        exact
        render={() => (
          <LabUnitEditor
            unit={null}
            onBack={() => history.push("/MasterListsPage/labUnitManagement")}
          />
        )}
      />
      <Route
        path="/labUnitManagement/edit/:id"
        exact
        component={LabUnitEditRoute}
      />
    </Switch>
  );
};

export default LabUnitRoutes;
