import React, { useState } from "react";
import { Button, Select, SelectItem, Tag, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const SIGNIFICANCE_OPTIONS = [
  { value: "UNKNOWN", labelId: "microbiology.isolate.unknown" },
  {
    value: "CLINICALLY_SIGNIFICANT",
    labelId: "microbiology.isolate.significant",
  },
  { value: "CONTAMINANT", labelId: "microbiology.isolate.contaminant" },
  { value: "NORMAL_FLORA", labelId: "microbiology.isolate.normalFlora" },
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
    <section
      className="microbiology-card"
      data-testid="microbiology-isolates-card"
      aria-labelledby="microbiology-isolates-heading"
    >
      <div className="microbiology-card__header">
        <div>
          <h3 id="microbiology-isolates-heading">
            {intl.formatMessage({ id: "microbiology.case.isolates" })}
          </h3>
          <p className="microbiology-card__hint">
            {intl.formatMessage({ id: "microbiology.case.isolates.hint" })}
          </p>
        </div>
        <Tag type={isolates.length > 0 ? "green" : "cool-gray"}>
          {isolates.length}
        </Tag>
      </div>
      <div>
        {isolates.length === 0 ? (
          <p>
            {intl.formatMessage({ id: "microbiology.case.isolates.empty" })}
          </p>
        ) : (
          <ul className="microbiology-list">
            {isolates.map((isolate) => (
              <li className="microbiology-list__row" key={isolate.id}>
                <strong>{isolate.isolateLabel}</strong>
                {isolate.preliminaryOrganismText
                  ? `: ${isolate.preliminaryOrganismText}`
                  : ""}
                <div className="microbiology-list__meta">
                  {formatMicrobiologyEnum(isolate.significance)}
                </div>
              </li>
            ))}
          </ul>
        )}
        <div className="microbiology-form-grid microbiology-form-grid--three">
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
              <SelectItem
                key={option.value}
                value={option.value}
                text={intl.formatMessage({ id: option.labelId })}
              />
            ))}
          </Select>
          <div>
            <Button onClick={submit} disabled={saving || !isolateLabel.trim()}>
              {intl.formatMessage({ id: "microbiology.case.createIsolate" })}
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
};

export default IsolatePanel;
