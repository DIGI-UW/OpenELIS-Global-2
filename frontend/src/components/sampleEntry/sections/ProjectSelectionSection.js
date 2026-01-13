import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  RadioButtonGroup,
  RadioButton,
  Grid,
  Column,
} from "@carbon/react";

const ProjectSelectionSection = ({
  selectedProject,
  onProjectChange,
  formLists,
}) => {
  const intl = useIntl();

  // Define study form types
  const studyForms = [
    {
      id: "ARV_INITIAL",
      value: "ARV_INITIAL",
      labelId: "sample.entry.project.arv.initial",
      defaultLabel: "Initial ARV",
    },
    {
      id: "ARV_FOLLOWUP",
      value: "ARV_FOLLOWUP",
      labelId: "sample.entry.project.arv.followup",
      defaultLabel: "Follow-up ARV",
    },
    {
      id: "RTN",
      value: "RTN",
      labelId: "sample.entry.project.rtn",
      defaultLabel: "RTN",
    },
    {
      id: "EID",
      value: "EID",
      labelId: "sample.entry.project.eid",
      defaultLabel: "EID",
    },
    {
      id: "INDETERMINATE",
      value: "INDETERMINATE",
      labelId: "sample.entry.project.indeterminate",
      defaultLabel: "Indeterminate",
    },
    {
      id: "SPECIAL_REQUEST",
      value: "SPECIAL_REQUEST",
      labelId: "sample.entry.project.special.request",
      defaultLabel: "Special Request",
    },
    {
      id: "ARV_VIRAL_LOAD",
      value: "ARV_VIRAL_LOAD",
      labelId: "sample.entry.project.arv.viral.load",
      defaultLabel: "ARV - Viral Load",
    },
    {
      id: "RECENCY_TESTING",
      value: "RECENCY_TESTING",
      labelId: "sample.entry.project.recency.testing",
      defaultLabel: "Recency Testing",
    },
    {
      id: "HPV_TESTING",
      value: "HPV_TESTING",
      labelId: "sample.entry.project.hpv.testing",
      defaultLabel: "HPV Testing",
    },
  ];

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.project.selection"
          defaultMessage="Project / Study Form Selection"
        />
      </Heading>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "sample.entry.project.select.form",
              defaultMessage: "Select Study Form",
            })}
            name="project-selection"
            valueSelected={selectedProject}
            onChange={onProjectChange}
            orientation="vertical"
          >
            {studyForms.map((form) => (
              <RadioButton
                key={form.id}
                id={form.id}
                labelText={intl.formatMessage({
                  id: form.labelId,
                  defaultMessage: form.defaultLabel,
                })}
                value={form.value}
              />
            ))}
          </RadioButtonGroup>
        </Column>
      </Grid>
    </Section>
  );
};

export default ProjectSelectionSection;
