import React, { useEffect, useState } from "react";
import { Column, Grid, Select, SelectItem } from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer, Roles } from "../utils/Utils";

function TestSectionSelectForm({ value, title }) {
  const [testUnits, setTestUnits] = useState([]);
  const [defaultTestSectionId, setDefaultTestSectionId] = useState("");
  const [defaultTestSectionLabel, setDefaultTestSectionLabel] = useState("");

  const intl = useIntl();

  useEffect(() => {
    let isMounted = true;

    const fetchTestSections = async () => {
      try {
        let testSectionId = new URLSearchParams(window.location.search).get("testSectionId") || "";
        const fetchedTestSections = await getFromOpenElisServer(`/rest/user-test-sections/${Roles.RESULTS}`);

        if (isMounted) {
          let testSection = fetchedTestSections.find((t) => t.id === testSectionId);
          let testSectionLabel = testSection
            ? testSection.value
            : intl.formatMessage({ id: "input.placeholder.selectTestSection" });

          setDefaultTestSectionId(testSectionId);
          setDefaultTestSectionLabel(testSectionLabel);
          setTestUnits(fetchedTestSections);
          value(testSectionId, testSectionLabel);
        }
      } catch (error) {
        console.error("Error fetching test sections:", error);
      }
    };

    fetchTestSections();
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
          <SelectItem text={defaultTestSectionLabel} value={defaultTestSectionId} />
          {testUnits
            .filter((item) => item.id !== defaultTestSectionId)
            .map((item, idx) => (
              <SelectItem key={idx} text={item.value} value={item.id} />
            ))}
        </Select>
      </Column>
    </Grid>
  );
}

export default injectIntl(TestSectionSelectForm);
