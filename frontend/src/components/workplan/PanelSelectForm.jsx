import React from "react";
import WorkplanListSelect from "./WorkplanListSelect";

export default function PanelSelectForm(props) {
  return (
    <WorkplanListSelect
      id="workplan-panel"
      url="/rest/displayList/PANELS"
      param="panelId"
      placeholderId="input.placeholder.selectPanel"
      title={props.title}
      onSelect={props.value}
    />
  );
}
