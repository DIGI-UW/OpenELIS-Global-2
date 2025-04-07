import React, { useEffect, useState } from "react";
import { Column, Grid, Select, SelectItem } from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";

function TestSelectForm({ value, title }) {
  const [tests, setTests] = useState([]);
  const [defaultTestId, setDefaultTestId] = useState("");
  const [defaultTestLabel, setDefaultTestLabel] = useState("");

  const intl = useIntl();

  useEffect(() => {
    let isMounted = true;

    const fetchTests = async () => {
      try {
        const fetchedTests = await getFromOpenElisServer("/rest/tests");
        if (isMounted) {
          let testId = new URLSearchParams(window.location.search).get("testId") || "";
          let test = fetchedTests.find((t) => t.id === testId);
          let testLabel = test
            ? test.value
            : intl.formatMessage({ id: "input.placeholder.selectTest" });

          setDefaultTestId(testId);
          setDefaultTestLabel(testLabel);
          setTests(fetchedTests);
          value(testId, testLabel);
        }
      } catch (error) {
        console.error("Error fetching tests:", error);
      }
    };

    fetchTests();
    return () => {
      isMounted = false;
    };
  }, [value, intl]);

  return (
    <Grid fullWidth={true}>
      <Column sm={4} md={8} lg={16}>
        <Select
          defaultValue="placeholder-item"
          id="select-1"
          invalidText={<FormattedMessage id="workplan.panel.selection.error.msg" />}
          helperText={title}
          labelText=""
          onChange={(e) => value(e.target.value, e.target.selectedOptions[0].text)}
        >
          <SelectItem text={defaultTestLabel} value={defaultTestId} />
          {tests
            .filter((item) => item.id !== defaultTestId)
            .map((item, idx) => (
              <SelectItem key={idx} text={item.value} value={item.id} />
            ))}
        </Select>
      </Column>
    </Grid>
  );
}

export default injectIntl(TestSelectForm);
