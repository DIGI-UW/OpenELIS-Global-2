import React, { useState, useEffect } from "react";
import { Switch, Route, useHistory } from "react-router-dom";
import LabUnitsManagement from "./LabUnitsManagement";
import LabUnitEditor from "../../labunit/LabUnitEditor";
import { getFromOpenElisServer } from "../../utils/Utils";

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
        render={(props) => {
          const [unit, setUnit] = useState(null);

          useEffect(() => {
            if (props.match.params.id) {
              getFromOpenElisServer(
                `/rest/api/lab-units/${props.match.params.id}`,
                (response) => {
                  if (response) {
                    setUnit(response);
                  }
                },
              );
            }
          }, [props.match.params.id]);

          return (
            <LabUnitEditor
              unit={unit}
              onBack={() => history.push("/MasterListsPage/labUnitManagement")}
            />
          );
        }}
      />
    </Switch>
  );
};

export default LabUnitRoutes;
