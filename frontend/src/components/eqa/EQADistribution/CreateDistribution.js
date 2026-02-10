import React, { useState, useEffect } from "react";
import {
  Button,
  TextInput,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  ProgressIndicator,
  ProgressStep,
  FilterableMultiSelect,
  Grid,
  Column,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const CreateDistribution = ({ onCreate }) => {
  const intl = useIntl();
  const [currentStep, setCurrentStep] = useState(0);
  const [name, setName] = useState("");
  const [programId, setProgramId] = useState("");
  const [deadline, setDeadline] = useState("");
  const [selectedOrgs, setSelectedOrgs] = useState([]);
  const [programs, setPrograms] = useState([]);
  const [organizations, setOrganizations] = useState([]);

  useEffect(() => {
    getFromOpenElisServer("/rest/eqa/programs", (data) => {
      if (data && Array.isArray(data)) {
        setPrograms(data);
      }
    });
    getFromOpenElisServer("/rest/organizations", (data) => {
      if (data && Array.isArray(data)) {
        setOrganizations(data);
      }
    });
  }, []);

  const handleSubmit = () => {
    const payload = JSON.stringify({
      distributionName: name,
      programId: Number(programId),
      deadline: deadline,
      participantOrganizationIds: selectedOrgs.map((o) => o.id),
    });

    postToOpenElisServerJsonResponse(
      "/rest/eqa/distributions",
      payload,
      (response) => {
        if (response && !response.error) {
          if (onCreate) onCreate(response);
        }
      },
    );
  };

  const canAdvanceFromStep0 = name && programId && deadline;
  const canAdvanceFromStep1 = selectedOrgs.length >= 2;

  return (
    <div className="create-distribution">
      <ProgressIndicator currentIndex={currentStep} spaceEqually>
        <ProgressStep
          label={intl.formatMessage({ id: "eqa.distribution.step.details" })}
        />
        <ProgressStep
          label={intl.formatMessage({
            id: "eqa.distribution.step.participants",
          })}
        />
        <ProgressStep
          label={intl.formatMessage({
            id: "eqa.distribution.step.confirmation",
          })}
        />
      </ProgressIndicator>

      {currentStep === 0 && (
        <Grid condensed style={{ marginTop: "1rem" }}>
          <Column lg={8} md={8} sm={4}>
            <TextInput
              id="distribution-name"
              labelText={intl.formatMessage({ id: "eqa.distribution.name" })}
              placeholder={intl.formatMessage({
                id: "eqa.distribution.name.placeholder",
              })}
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </Column>
          <Column lg={8} md={8} sm={4}>
            <Select
              id="distribution-program"
              labelText={intl.formatMessage({ id: "eqa.distribution.program" })}
              value={programId}
              onChange={(e) => setProgramId(e.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "eqa.distribution.program.select",
                })}
              />
              {programs.map((p) => (
                <SelectItem key={p.id} value={String(p.id)} text={p.name} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={8} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={([date]) => {
                if (date) {
                  const y = date.getFullYear();
                  const m = String(date.getMonth() + 1).padStart(2, "0");
                  const d = String(date.getDate()).padStart(2, "0");
                  setDeadline(`${y}-${m}-${d}`);
                }
              }}
            >
              <DatePickerInput
                id="distribution-deadline"
                labelText={intl.formatMessage({
                  id: "eqa.distribution.deadline",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
            <Button
              disabled={!canAdvanceFromStep0}
              onClick={() => setCurrentStep(1)}
            >
              {intl.formatMessage({ id: "eqa.distribution.step.participants" })}
            </Button>
          </Column>
        </Grid>
      )}

      {currentStep === 1 && (
        <Grid condensed style={{ marginTop: "1rem" }}>
          <Column lg={16} md={8} sm={4}>
            <FilterableMultiSelect
              id="distribution-participants"
              titleText={intl.formatMessage({
                id: "eqa.distribution.participants",
              })}
              placeholder={intl.formatMessage({
                id: "eqa.distribution.participants.select",
              })}
              items={organizations}
              itemToString={(item) => (item ? item.name || item.id : "")}
              onChange={({ selectedItems }) => setSelectedOrgs(selectedItems)}
              selectionFeedback="top-after-reopen"
            />
            {selectedOrgs.length > 0 && selectedOrgs.length < 2 && (
              <p style={{ color: "#da1e28", marginTop: "0.5rem" }}>
                {intl.formatMessage({
                  id: "eqa.distribution.participants.min",
                })}
              </p>
            )}
          </Column>
          <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
            <Button kind="secondary" onClick={() => setCurrentStep(0)}>
              Back
            </Button>
            <Button
              disabled={!canAdvanceFromStep1}
              onClick={() => setCurrentStep(2)}
              style={{ marginLeft: "0.5rem" }}
            >
              {intl.formatMessage({
                id: "eqa.distribution.step.confirmation",
              })}
            </Button>
          </Column>
        </Grid>
      )}

      {currentStep === 2 && (
        <Grid condensed style={{ marginTop: "1rem" }}>
          <Column lg={16} md={8} sm={4}>
            <h4>
              {intl.formatMessage({ id: "eqa.distribution.step.confirmation" })}
            </h4>
            <p>
              <strong>
                {intl.formatMessage({ id: "eqa.distribution.name" })}:
              </strong>{" "}
              {name}
            </p>
            <p>
              <strong>
                {intl.formatMessage({ id: "eqa.distribution.deadline" })}:
              </strong>{" "}
              {deadline}
            </p>
            <p>
              <strong>
                {intl.formatMessage({ id: "eqa.distribution.participants" })}:
              </strong>{" "}
              {selectedOrgs.length} organizations
            </p>
          </Column>
          <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
            <Button kind="secondary" onClick={() => setCurrentStep(1)}>
              Back
            </Button>
            <Button onClick={handleSubmit} style={{ marginLeft: "0.5rem" }}>
              {intl.formatMessage({ id: "eqa.distribution.create" })}
            </Button>
          </Column>
        </Grid>
      )}
    </div>
  );
};

export default CreateDistribution;
