import React, { useEffect, useRef, useState } from "react";
import { Column, Grid, Select, SelectItem } from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer, Roles } from "../utils/Utils";

function TestSectionSelectForm(props) {
  const mounted = useRef(false);
  const [testUnits, setTestUnits] = useState([]);
  const [defaultTestSectionId, setDefaultTestSectionId] = useState("");
  const [defaultTestSectionLabel, setDefaultTestSectionLabel] = useState("");

  const handleChange = (e) => {
    props.value(e.target.value, e.target.selectedOptions[0].text);
  };

  const getTestUnits = (res) => {
    if (mounted.current) {
      setTestUnits(res);
    }
  };

  const intl = useIntl();

  useEffect(() => {
    mounted.current = true;
    let testSectionId = new URLSearchParams(window.location.search).get(
      "testSectionId",
    );
    testSectionId = testSectionId ? testSectionId : "";
    getFromOpenElisServer(
      "/rest/user-test-sections/" + Roles.RESULTS,
      (fetchedTestSections) => {
        let testSection = fetchedTestSections.find(
          (testSection) => testSection.id === testSectionId,
        );
        let testSectionLabel = testSection
          ? testSection.value
          : intl.formatMessage({ id: "input.placeholder.selectTestSection" });
        setDefaultTestSectionId(testSectionId);
        setDefaultTestSectionLabel(testSectionLabel);
        props.value(testSectionId, testSectionLabel);
        getTestUnits(fetchedTestSections);
      },
    );
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
            <SelectItem
              text={defaultTestSectionLabel}
              value={defaultTestSectionId}
            />
            {testUnits
              .filter((item) => item.id !== defaultTestSectionId)
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

export default injectIntl(TestSectionSelectForm);
