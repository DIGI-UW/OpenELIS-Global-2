import React, { useState, useEffect } from "react";
import {
  Checkbox,
  Select,
  SelectItem,
  Grid,
  Column,
  Tag,
  Search,
  Section,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { sampleTypeTestsStructure } from "../data/SampleEntryTestsForTypeProvider";
import "../Style.css";

const SampleType = ({ updateFormValues }) => {
  const intl = useIntl();
  const [sampleTypes, setSampleTypes] = useState([]);
  const [selectedSampleTypeId, setSelectedSampleTypeId] = useState(null);
  const [sampleTypeTests, setSampleTypeTests] = useState(
    sampleTypeTestsStructure,
  );
  const [selectedTests, setSelectedTests] = useState([]);
  const [selectedPanels, setSelectedPanels] = useState([]);
  const [testSearchTerm, setTestSearchTerm] = useState("");
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [filteredTests, setFilteredTests] = useState([]);
  const [filteredPanels, setFilteredPanels] = useState([]);
  const [isSampleSelected, setIsSampleSelected] = useState(false);
  const [selectedType, setSelectedType] = useState();

  const handleTestSearchChange = (e) => {
    const term = e.target.value.toLowerCase();
    setTestSearchTerm(term);
    const filtered = sampleTypeTests.tests.filter((test) =>
      test.name.toLowerCase().includes(term),
    );
    setFilteredTests(filtered);
  };

  const handlePanelSearchChange = (e) => {
    const term = e.target.value.toLowerCase();
    setPanelSearchTerm(term);
    const filtered = sampleTypeTests.panels.filter((panel) =>
      panel.name.toLowerCase().includes(term),
    );
    setFilteredPanels(filtered);
  };

  const fetchSamplesTypes = (res) => {
    setSampleTypes(res);
  };

  const fetchSampleTypeTests = (res) => {
    setSampleTypeTests(res);
    setFilteredTests(res.tests);
    setFilteredPanels(res.panels);
  };

  const handleFetchSampleTypeTests = (e) => {
    const { value } = e.target;
    setSelectedSampleTypeId(value);
    setSelectedTests([]);
    setSelectedPanels([]);
    setIsSampleSelected(!!value);
    const selectedId = value;
    setSelectedType(sampleTypes.find((type) => type.id === selectedId));
  };

  useEffect(() => {
    getFromOpenElisServer("/rest/user-sample-types", fetchSamplesTypes);
  }, []);

  useEffect(() => {
    if (selectedSampleTypeId !== null) {
      getFromOpenElisServer(
        `/rest/sample-type-tests?sampleType=${selectedSampleTypeId}`,
        fetchSampleTypeTests,
      );
    }
  }, [selectedSampleTypeId]);

  const handlePanelCheckbox = (e, panel) => {
    const isChecked = e.currentTarget.checked;
    let updatedPanels;
    if (isChecked) {
      updatedPanels = [
        ...selectedPanels,
        { id: panel.id, name: panel.name, testIds: panel.testIds },
      ];
    } else {
      updatedPanels = selectedPanels.filter((item) => item.id !== panel.id);
    }
    setSelectedPanels(updatedPanels);

    var updatedTests = [...selectedTests];

    const testMapIds = panel.testIds.split(",");
    if (isChecked) {
      testMapIds.forEach((testId) => {
        const isTestSelected = updatedTests.some((test) => test.id === testId);
        if (!isTestSelected) {
          const test = sampleTypeTests.tests.find((test) => test.id === testId);
          if (test) {
            updatedTests.push({ id: test.id, value: test.name });
          }
        }
      });
    } else {
      const removeTests = [];
      testMapIds.forEach((testId) => {
        const isTestSelected = updatedTests.some((test) => test.id === testId);
        if (isTestSelected) {
          const test = sampleTypeTests.tests.find((test) => test.id === testId);
          if (test) {
            removeTests.push({ id: test.id, value: test.name });
          }
        }
      });
      updatedTests = selectedTests.filter((test) => {
        return !removeTests.some((subItem) => subItem.id === test.id);
      });
    }

    setSelectedTests(updatedTests);
    updateFormValues({
      selectedTests: updatedTests,
      selectedPanels: updatedPanels,
      sampleId: selectedType.id,
      sampleName: selectedType.value,
    });
  };

  const handleTestCheckbox = (e, test) => {
    const isChecked = e.currentTarget.checked;
    let updatedTests;
    if (isChecked) {
      updatedTests = [...selectedTests, { id: test.id, value: test.name }];
    } else {
      updatedTests = selectedTests.filter((item) => item.id !== test.id);
    }
    setSelectedTests(updatedTests);

    updateFormValues({
      selectedTests: updatedTests,
      sampleId: selectedType.id,
      sampleName: selectedType.value,
    });
  };

  return (
    <>
      <div className="orderLegendBody">
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <h4>
              <FormattedMessage id="label.button.sample" />
            </h4>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <Select
              id="selectSampleType"
              className="selectSampleType"
              labelText="Sample Type"
              onChange={handleFetchSampleTypeTests}
            >
              {selectedSampleTypeId === null && (
                <SelectItem
                  text={intl.formatMessage({ id: "sample.select.type" })}
                  value=""
                />
              )}
              {sampleTypes?.map((sampleType, i) => (
                <SelectItem
                  text={sampleType.value}
                  value={sampleType.id}
                  key={i}
                />
              ))}
            </Select>
          </Column>
          {isSampleSelected && (
            <>
              <Column lg={16} md={8} sm={4}>
                <h4>
                  <FormattedMessage id="sample.entry.panels" />
                </h4>
                <Section>
                  {selectedPanels.map((panel) => (
                    <Tag key={panel.id} type="green">
                      {panel.name}
                    </Tag>
                  ))}
                </Section>
                <Search
                  labelText="Search Panels"
                  placeholder="Search panels..."
                  onChange={handlePanelSearchChange}
                />
                {filteredPanels.map((panel) => (
                  <Checkbox
                    key={panel.id}
                    id={`panel_${panel.id}`}
                    labelText={panel.name}
                    checked={selectedPanels.some(
                      (item) => item.id === panel.id,
                    )}
                    onChange={(e) => handlePanelCheckbox(e, panel)}
                  />
                ))}
              </Column>
              <Column lg={16} md={8} sm={4}>
                <h4>
                  <FormattedMessage id="sample.entry.available.tests" />
                </h4>
                <div>
                  {selectedTests.map((test) => (
                    <Tag key={test.id} type="red">
                      {test.value}
                    </Tag>
                  ))}
                </div>
                <Search
                  labelText="Search Tests"
                  placeholder="Search tests..."
                  onChange={handleTestSearchChange}
                />
                {filteredTests.map((test) => (
                  <Checkbox
                    key={test.id}
                    id={`test_${test.id}`}
                    labelText={test.name}
                    checked={selectedTests.some((item) => item.id === test.id)}
                    onChange={(e) => handleTestCheckbox(e, test)}
                  />
                ))}
              </Column>
            </>
          )}
        </Grid>
      </div>
    </>
  );
};

export default SampleType;
