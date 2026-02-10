import React, { useEffect, useRef, useState } from "react";
import {
  Checkbox,
  Select,
  SelectItem,
  TextInput,
  DatePicker,
  DatePickerInput,
  RadioButtonGroup,
  RadioButton,
  Column,
  Grid,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const EQASampleEntry = ({ orderFormValues, setOrderFormValues }) => {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const [eqaPrograms, setEqaPrograms] = useState([]);
  const [providerOrganizations, setProviderOrganizations] = useState([]);

  const isEQA = orderFormValues?.sampleOrderItems?.isEQASample || false;

  useEffect(() => {
    componentMounted.current = true;
    if (isEQA) {
      getFromOpenElisServer("/rest/eqa/programs", (data) => {
        if (componentMounted.current && Array.isArray(data)) {
          setEqaPrograms(data);
        }
      });
      getFromOpenElisServer("/rest/organizations", (data) => {
        if (componentMounted.current && Array.isArray(data)) {
          setProviderOrganizations(data);
        }
      });
    }
    return () => {
      componentMounted.current = false;
    };
  }, [isEQA]);

  const handleEQAToggle = (checked) => {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        isEQASample: checked,
        ...(checked
          ? {}
          : {
              eqaProgramId: "",
              eqaProviderOrganizationId: "",
              eqaProviderSampleId: "",
              eqaParticipantId: "",
              eqaDeadline: "",
              eqaPriority: "STANDARD",
            }),
      },
    });
  };

  const handleFieldChange = (field, value) => {
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        [field]: value,
      },
    });
  };

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Checkbox
          id="eqa-sample-checkbox"
          labelText={intl.formatMessage({ id: "eqa.sample.checkbox" })}
          checked={isEQA}
          onChange={(_, { checked }) => handleEQAToggle(checked)}
          data-testid="eqa-sample-checkbox"
        />
      </Column>

      {isEQA && (
        <>
          <Column lg={16} md={8} sm={4}>
            <h4>
              <FormattedMessage id="eqa.section.title" />
            </h4>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="eqa-program-select"
              labelText={intl.formatMessage({ id: "eqa.program.label" })}
              value={orderFormValues.sampleOrderItems.eqaProgramId}
              onChange={(e) =>
                handleFieldChange("eqaProgramId", e.target.value)
              }
              data-testid="eqa-program-select"
            >
              <SelectItem
                value=""
                text={intl.formatMessage({ id: "eqa.program.select" })}
              />
              {eqaPrograms.map((program) => (
                <SelectItem
                  key={program.id}
                  value={String(program.id)}
                  text={program.name}
                />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="eqa-provider-select"
              labelText={intl.formatMessage({ id: "eqa.provider.label" })}
              value={orderFormValues.sampleOrderItems.eqaProviderOrganizationId}
              onChange={(e) =>
                handleFieldChange("eqaProviderOrganizationId", e.target.value)
              }
              data-testid="eqa-provider-select"
            >
              <SelectItem
                value=""
                text={intl.formatMessage({ id: "eqa.provider.select" })}
              />
              {providerOrganizations.map((org) => (
                <SelectItem
                  key={org.id}
                  value={String(org.id)}
                  text={org.value || org.organizationName || org.name}
                />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="eqa-provider-sample-id"
              labelText={intl.formatMessage({ id: "eqa.provider.sampleId" })}
              value={orderFormValues.sampleOrderItems.eqaProviderSampleId}
              onChange={(e) =>
                handleFieldChange("eqaProviderSampleId", e.target.value)
              }
              data-testid="eqa-provider-sample-id"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="eqa-participant-id"
              labelText={intl.formatMessage({ id: "eqa.participant.id" })}
              value={orderFormValues.sampleOrderItems.eqaParticipantId}
              onChange={(e) =>
                handleFieldChange("eqaParticipantId", e.target.value)
              }
              data-testid="eqa-participant-id"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={([date]) => {
                if (date) {
                  const formatted = date.toLocaleDateString("en-CA");
                  handleFieldChange("eqaDeadline", formatted);
                }
              }}
              value={orderFormValues.sampleOrderItems.eqaDeadline}
            >
              <DatePickerInput
                id="eqa-deadline"
                labelText={intl.formatMessage({ id: "eqa.deadline.label" })}
                placeholder="yyyy-mm-dd"
                data-testid="eqa-deadline"
              />
            </DatePicker>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({ id: "eqa.priority.label" })}
              name="eqa-priority"
              valueSelected={
                orderFormValues.sampleOrderItems.eqaPriority || "STANDARD"
              }
              onChange={(value) => handleFieldChange("eqaPriority", value)}
            >
              <RadioButton
                id="eqa-priority-standard"
                labelText={intl.formatMessage({
                  id: "eqa.priority.standard",
                })}
                value="STANDARD"
              />
              <RadioButton
                id="eqa-priority-urgent"
                labelText={intl.formatMessage({ id: "eqa.priority.urgent" })}
                value="URGENT"
              />
              <RadioButton
                id="eqa-priority-critical"
                labelText={intl.formatMessage({
                  id: "eqa.priority.critical",
                })}
                value="CRITICAL"
              />
            </RadioButtonGroup>
          </Column>
        </>
      )}
    </Grid>
  );
};

export default EQASampleEntry;
