import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Select,
  SelectItem,
  Section,
  Heading,
  Button,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

function TestModifyFilters({
  sampleTypeList,
  labUnitList,
  selectedSampleType,
  selectedTestSection,
  onFilterChange,
  onClearFilters,
}) {
  const intl = useIntl();

  const handleSampleTypeChange = (e) => {
    const newValue = e.target.value;
    onFilterChange(newValue, "");
  };

  const handleTestSectionChange = (e) => {
    const newValue = e.target.value;
    onFilterChange("", newValue);
  };

  const clearFilters = () => {
    onFilterChange("", "");
    // Notify parent to clear test results
    if (onClearFilters) {
      onClearFilters();
    }
  };

  return (
    <Section>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Heading>
            <FormattedMessage id="filters.label" />
          </Heading>
        </Column>
      </Grid>
      <br />
      <Grid fullWidth={true}>
        <Column lg={5} md={4} sm={2}>
          <Select
            id="sampleTypeFilter"
            labelText={intl.formatMessage({ id: "field.sampleType" })}
            value={selectedSampleType}
            onChange={handleSampleTypeChange}
          >
            <SelectItem
              text={intl.formatMessage({ id: "sample.select.type" })}
              value=""
            />
            {sampleTypeList?.map((sampleType) => (
              <SelectItem
                key={sampleType.id}
                text={sampleType.value}
                value={sampleType.id}
              />
            ))}
          </Select>
        </Column>
        <Column lg={5} md={4} sm={2}>
          <Select
            id="testSectionFilter"
            labelText={intl.formatMessage({ id: "field.testSection" })}
            value={selectedTestSection}
            onChange={handleTestSectionChange}
          >
            <SelectItem
              text={intl.formatMessage({
                id: "input.placeholder.selectTestSection",
              })}
              value=""
            />
            {labUnitList?.map((testSection) => (
              <SelectItem
                key={testSection.id}
                text={testSection.value}
                value={testSection.id}
              />
            ))}
          </Select>
        </Column>
        <Column lg={3} md={2} sm={1}>
          <Button
            kind="secondary"
            onClick={clearFilters}
            style={{ marginTop: "1rem" }}
          >
            <FormattedMessage id="label.clear" />
          </Button>
        </Column>
        <Column lg={3} md={2} sm={1}>
          <Section
            style={{ marginTop: "1rem", display: "flex", alignItems: "end" }}
          >
            <small>
              <FormattedMessage id="configuration.test.modify.filter.instruction" />
            </small>
          </Section>
        </Column>
      </Grid>
    </Section>
  );
}

export default TestModifyFilters;
