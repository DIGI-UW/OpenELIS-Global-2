import React from "react";
import WorkplanListSelect from "./WorkplanListSelect";

/**
 * The tests this user may work on, narrowed on the server to their Results
 * lab units, the same list the results search offers. The full catalogue used
 * to be offered here, so a user could pick a test they hold no unit for and
 * get an empty workplan.
 */
export default function TestSelectForm(props) {
  return (
    <WorkplanListSelect
      id="workplan-test"
      url="/rest/test-list"
      param="testId"
      placeholderId="input.placeholder.selectTest"
      title={props.title}
      onSelect={props.value}
    />
  );
}
