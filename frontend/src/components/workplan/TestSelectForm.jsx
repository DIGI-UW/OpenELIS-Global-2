import React, { useEffect, useRef, useState } from "react";
import { Column, Grid, Select, SelectItem } from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";

function TestSelectForm(props) {
  const mounted = useRef(false);
  const [tests, setTests] = useState([]);
  const [defaultTestId, setDefaultTestId] = useState("");
  const [defaultTestLabel, setDefaultTestLabel] = useState("");

  const handleChange = (e) => {
    props.value(e.target.value, e.target.selectedOptions[0].text);
  };

  const getTests = (res) => {
    if (mounted.current) {
      setTests(res);
    }
  };

  const intl = useIntl();

  useEffect(() => {
    mounted.current = true;
    let testId = new URLSearchParams(window.location.search).get("testId");
    testId = testId ? testId : "";
    // The tests this user may work on, narrowed on the server to their Results
    // lab units, the same list the results search offers. The full catalogue
    // used to be offered here, so a user could pick a test they hold no unit
    // for and get an empty workplan.
    getFromOpenElisServer("/rest/test-list", (response) => {
      const fetchedTests = Array.isArray(response) ? response : [];
      let test = fetchedTests.find((test) => test.id === testId);
      let testLabel = test
        ? test.value
        : intl.formatMessage({ id: "input.placeholder.selectTest" });
      setDefaultTestId(testId);
      setDefaultTestLabel(testLabel);
      props.value(testId, testLabel);
      getTests(fetchedTests);
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
            <SelectItem text={defaultTestLabel} value={defaultTestId} />
            {tests
              .filter((item) => item.id !== defaultTestId)
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

export default injectIntl(TestSelectForm);
