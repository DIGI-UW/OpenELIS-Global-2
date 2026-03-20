import React, { useState } from "react";
import {
  Select,
  SelectItem,
  NumberInput,
  Button,
  Grid,
  Column,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

const ManualResultEntry = ({
  distributionId,
  organizations,
  tests,
  onSubmit,
}) => {
  const intl = useIntl();
  const [orgId, setOrgId] = useState("");
  const [testId, setTestId] = useState("");
  const [resultValue, setResultValue] = useState("");

  const handleSubmit = () => {
    const payload = JSON.stringify({
      organizationId: Number(orgId),
      testId: Number(testId),
      resultValue: Number(resultValue),
    });

    postToOpenElisServerJsonResponse(
      `/rest/eqa/distributions/${distributionId}/results`,
      payload,
      (response) => {
        if (response && !response.error) {
          setOrgId("");
          setTestId("");
          setResultValue("");
          if (onSubmit) onSubmit(response);
        }
      },
    );
  };

  return (
    <div>
      <h4>{intl.formatMessage({ id: "eqa.results.manual.entry" })}</h4>
      <Grid condensed>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="result-org"
            labelText={intl.formatMessage({ id: "eqa.results.organization" })}
            value={orgId}
            onChange={(e) => setOrgId(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "eqa.results.organization.select",
              })}
            />
            {(organizations || []).map((o) => (
              <SelectItem
                key={o.id}
                value={String(o.id)}
                text={o.name || String(o.id)}
              />
            ))}
          </Select>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="result-test"
            labelText={intl.formatMessage({ id: "eqa.results.test" })}
            value={testId}
            onChange={(e) => setTestId(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({ id: "eqa.results.test.select" })}
            />
            {(tests || []).map((t) => (
              <SelectItem
                key={t.id}
                value={String(t.id)}
                text={t.name || String(t.id)}
              />
            ))}
          </Select>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <NumberInput
            id="result-value"
            label={intl.formatMessage({ id: "eqa.results.value" })}
            value={resultValue}
            onChange={(e) => setResultValue(e.target.value)}
            step={0.01}
            allowEmpty
          />
        </Column>
        <Column
          lg={4}
          md={4}
          sm={4}
          style={{ display: "flex", alignItems: "flex-end" }}
        >
          <Button
            disabled={!orgId || !testId || !resultValue}
            onClick={handleSubmit}
          >
            {intl.formatMessage({ id: "eqa.results.submit" })}
          </Button>
        </Column>
      </Grid>
    </div>
  );
};

export default ManualResultEntry;
