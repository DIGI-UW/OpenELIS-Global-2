import React from "react";
import { Roles } from "../utils/Utils";
import WorkplanListSelect from "./WorkplanListSelect";

export default function TestSectionSelectForm(props) {
  return (
    <WorkplanListSelect
      id="workplan-unit"
      url={"/rest/user-test-sections/" + Roles.RESULTS}
      param="testSectionId"
      placeholderId="input.placeholder.selectTestSection"
      title={props.title}
      onSelect={props.value}
    />
  );
}
