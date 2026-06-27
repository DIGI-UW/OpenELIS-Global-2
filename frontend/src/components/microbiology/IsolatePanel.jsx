import React, { useState } from "react";
import { Button, Select, SelectItem, Stack, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";

const SIGNIFICANCE_OPTIONS = [
  "UNKNOWN",
  "CLINICALLY_SIGNIFICANT",
  "CONTAMINANT",
  "NORMAL_FLORA",
];

const IsolatePanel = ({ caseId, isolates = [], onCreateIsolate, saving }) => {
  const intl = useIntl();
  const [isolateLabel, setIsolateLabel] = useState("ISO-1");
  const [preliminaryOrganismText, setPreliminaryOrganismText] = useState("");
  const [significance, setSignificance] = useState("CLINICALLY_SIGNIFICANT");

  const submit = () => {
    onCreateIsolate({
      caseId,
      isolateLabel,
      preliminaryOrganismText,
      significance,
    });
    setPreliminaryOrganismText("");
  };

  return (
    <section aria-labelledby="microbiology-isolates-heading">
      <Stack gap={5}>
        <h3 id="microbiology-isolates-heading">
          {intl.formatMessage({ id: "microbiology.case.isolates" })}
        </h3>
        {isolates.length === 0 ? (
          <p>
            {intl.formatMessage({ id: "microbiology.case.isolates.empty" })}
          </p>
        ) : (
          <ul>
            {isolates.map((isolate) => (
              <li key={isolate.id}>
                <strong>{isolate.isolateLabel}</strong>
                {isolate.preliminaryOrganismText
                  ? `: ${isolate.preliminaryOrganismText}`
                  : ""}
              </li>
            ))}
          </ul>
        )}
        <TextInput
          id="microbiology-isolate-label"
          labelText={intl.formatMessage({
            id: "microbiology.case.isolateLabel",
          })}
          value={isolateLabel}
          onChange={(event) => setIsolateLabel(event.target.value)}
        />
        <TextInput
          id="microbiology-preliminary-organism"
          labelText={intl.formatMessage({
            id: "microbiology.case.preliminaryOrganism",
          })}
          value={preliminaryOrganismText}
          onChange={(event) => setPreliminaryOrganismText(event.target.value)}
        />
        <Select
          id="microbiology-isolate-significance"
          labelText={intl.formatMessage({
            id: "microbiology.case.significance",
          })}
          value={significance}
          onChange={(event) => setSignificance(event.target.value)}
        >
          {SIGNIFICANCE_OPTIONS.map((option) => (
            <SelectItem key={option} value={option} text={option} />
          ))}
        </Select>
        <Button onClick={submit} disabled={saving || !isolateLabel.trim()}>
          {intl.formatMessage({ id: "microbiology.case.createIsolate" })}
        </Button>
      </Stack>
    </section>
  );
};

export default IsolatePanel;
