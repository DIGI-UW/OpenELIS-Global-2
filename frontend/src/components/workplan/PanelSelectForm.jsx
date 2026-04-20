import React, { useEffect, useRef, useState } from "react";
import { Column, Grid, Select, SelectItem } from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";

function PanelSelectForm(props) {
  const mounted = useRef(false);
  const [panels, setPanels] = useState([]);
  const [defaultPanelId, setDefaultPanelId] = useState("");
  const [defaultPanelLabel, setDefaultPanelLabel] = useState("");

  const handleChange = (e) => {
    props.value(e.target.value, e.target.selectedOptions[0].text);
  };

  const getPanels = (res) => {
    if (mounted.current) {
      setPanels(res);
    }
  };

  const intl = useIntl();

  useEffect(() => {
    mounted.current = true;
    let panelId = new URLSearchParams(window.location.search).get("panelId");
    panelId = panelId ? panelId : "";
    getFromOpenElisServer("/rest/panels", (fetchedPanels) => {
      let panel = fetchedPanels.find((panel) => panel.id === panelId);
      let panelLabel = panel
        ? panel.value
        : intl.formatMessage({ id: "input.placeholder.selectPanel" });
      setDefaultPanelId(panelId);
      setDefaultPanelLabel(panelLabel);
      props.value(panelId, panelLabel);
      getPanels(fetchedPanels);
    });
    return () => {
      mounted.current = false;
    };
  }, []);

  return (
    <>
      <Grid fullWidth={true}>
        <Column sm={4} md={8} lg={16}>
          <Select
            defaultValue="placeholder-item"
            id="select-1"
            invalidText={
              <FormattedMessage id="workplan.panel.selection.error.msg" />
            }
            helperText={props.title}
            labelText=""
            onChange={handleChange}
          >
            <SelectItem text={defaultPanelLabel} value={defaultPanelId} />
            {panels
              .filter((item) => item.id !== defaultPanelId)
              .map((item, idx) => {
                return (
                  <SelectItem key={idx} text={item.value} value={item.id} />
                );
              })}
          </Select>
        </Column>
      </Grid>
    </>
  );
}

export default injectIntl(PanelSelectForm);
