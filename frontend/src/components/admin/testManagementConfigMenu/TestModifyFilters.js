import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Select,
  SelectItem,
  Section,
  Heading,
  Button
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

function TestModifyFilters({
  sampleTypeList,
  labUnitList,
  testCatBeanList,
  onFilterChange
}) {
  const intl = useIntl();
  const [selectedSampleType, setSelectedSampleType] = useState("");
  const [selectedTestSection, setSelectedTestSection] = useState("");

  useEffect(() => {
    filterTests();
  }, [selectedSampleType, selectedTestSection, testCatBeanList]);

  const filterTests = () => {
    if (!testCatBeanList || testCatBeanList.length === 0) {
      onFilterChange([]);
      return;
    }

    // If no filters are selected, don't show any tests
    if (!selectedSampleType && !selectedTestSection) {
      onFilterChange([]);
      return;
    }

    let filtered = testCatBeanList;

    // Filter by sample type if selected
    if (selectedSampleType) {
      const sampleTypeName = sampleTypeList.find(st => st.id === selectedSampleType)?.value;
      if (sampleTypeName) {
        filtered = filtered.filter(test => test.sampleType === sampleTypeName);
      }
    }

    // Filter by test section if selected
    if (selectedTestSection) {
      const testSectionName = labUnitList.find(ts => ts.id === selectedTestSection)?.value;
      if (testSectionName) {
        filtered = filtered.filter(test => test.testUnit === testSectionName);
      }
    }

    // Convert to the format expected by the UI (matching testList structure)
    const testListFormat = filtered.map(test => ({
      id: test.id,
      value: test.localization?.english || test.localization?.french || 'Unknown Test'
    }));

    onFilterChange(testListFormat);
  };

  const handleSampleTypeChange = (e) => {
    setSelectedSampleType(e.target.value);
  };

  const handleTestSectionChange = (e) => {
    setSelectedTestSection(e.target.value);
  };

  const clearFilters = () => {
    setSelectedSampleType("");
    setSelectedTestSection("");
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
              text={intl.formatMessage({ id: "input.placeholder.selectTestSection" })}
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
            style={{ marginTop: '1rem' }}
          >
            <FormattedMessage id="label.clear" />
          </Button>
        </Column>
        <Column lg={3} md={2} sm={1}>
          <Section style={{ marginTop: '1rem', display: 'flex', alignItems: 'end' }}>
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
