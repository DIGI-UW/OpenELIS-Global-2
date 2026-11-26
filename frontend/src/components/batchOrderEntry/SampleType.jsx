import React, { useState, useEffect } from "react";
import { Grid, Column } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { sampleTypeTestsStructure } from "../data/SampleEntryTestsForTypeProvider";
import SearchableSelect from "../common/SearchableSelect";
import SearchableCheckboxList from "../common/SearchableCheckboxList";
import "../Style.css";

/**
 * The batch's sample type, panels and tests, each searched by typing
 * (OGC-1515). Every change reports the whole selection to the setup form, so
 * changing the sample type clears the tests chosen for the previous one.
 */
const SampleType = ({ updateFormValues }) => {
  const intl = useIntl();
  const [sampleTypes, setSampleTypes] = useState([]);
  const [selectedSampleTypeId, setSelectedSampleTypeId] = useState("");
  const [sampleTypeTests, setSampleTypeTests] = useState(
    sampleTypeTestsStructure,
  );
  const [selectedTestIds, setSelectedTestIds] = useState([]);
  const [selectedPanels, setSelectedPanels] = useState([]);
  const [handTickedTestIds, setHandTickedTestIds] = useState([]);

  useEffect(() => {
    getFromOpenElisServer("/rest/user-sample-types", (res) =>
      setSampleTypes(Array.isArray(res) ? res : []),
    );
  }, []);

  useEffect(() => {
    if (selectedSampleTypeId) {
      getFromOpenElisServer(
        `/rest/sample-type-tests?sampleType=${selectedSampleTypeId}`,
        (res) => setSampleTypeTests(res || sampleTypeTestsStructure),
      );
    }
  }, [selectedSampleTypeId]);

  const report = (testIds, panels, sampleTypeId = selectedSampleTypeId) => {
    const sampleType = sampleTypes.find(
      (type) => String(type.id) === String(sampleTypeId),
    );
    updateFormValues({
      selectedTests: sampleTypeTests.tests
        .filter((test) => testIds.includes(test.id))
        .map((test) => ({ id: test.id, value: test.name })),
      selectedPanels: panels.map(({ id, name, testIds: ids }) => ({
        id,
        name,
        testIds: ids,
      })),
      sampleId: sampleType ? sampleType.id : "",
      sampleName: sampleType ? sampleType.value : "",
    });
  };

  const chooseSampleType = (id) => {
    setSelectedSampleTypeId(id);
    setSampleTypeTests(sampleTypeTestsStructure);
    setSelectedTestIds([]);
    setSelectedPanels([]);
    setHandTickedTestIds([]);
    updateFormValues({
      selectedTests: [],
      selectedPanels: [],
      sampleId: "",
      sampleName: "",
    });
  };

  const panelTestIds = (panel) =>
    panel.testIds
      .split(",")
      .map((id) => id.trim())
      .filter((id) => sampleTypeTests.tests.some((test) => test.id === id));

  const togglePanel = (panel, checked) => {
    const others = selectedPanels.filter((item) => item.id !== panel.id);
    const ids = panelTestIds(panel);
    const panels = checked ? [...others, panel] : others;
    const kept = others.flatMap(panelTestIds);
    const testIds = checked
      ? [
          ...selectedTestIds,
          ...ids.filter((id) => !selectedTestIds.includes(id)),
        ]
      : selectedTestIds.filter(
          (id) =>
            !ids.includes(id) ||
            kept.includes(id) ||
            handTickedTestIds.includes(id),
        );
    setSelectedPanels(panels);
    setSelectedTestIds(testIds);
    report(testIds, panels);
  };

  const toggleTest = (test, checked) => {
    const testIds = checked
      ? selectedTestIds.includes(test.id)
        ? selectedTestIds
        : [...selectedTestIds, test.id]
      : selectedTestIds.filter((id) => id !== test.id);
    setHandTickedTestIds((prev) =>
      checked
        ? [...prev.filter((id) => id !== test.id), test.id]
        : prev.filter((id) => id !== test.id),
    );
    setSelectedTestIds(testIds);
    report(testIds, selectedPanels);
  };

  return (
    <div className="orderLegendBody">
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <h4>
            <FormattedMessage id="label.button.sample" />
          </h4>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <SearchableSelect
            id="selectSampleType"
            className="selectSampleType"
            labelText={intl.formatMessage({ id: "sample.type" })}
            placeholder={intl.formatMessage({ id: "sample.select.type" })}
            options={sampleTypes}
            value={selectedSampleTypeId}
            onChange={chooseSampleType}
          />
        </Column>
        {selectedSampleTypeId && (
          <>
            <Column lg={16} md={8} sm={4}>
              <h4>
                <FormattedMessage id="sample.entry.panels" />
              </h4>
              <SearchableCheckboxList
                id="batch-panels"
                idPrefix="panel_"
                searchLabel={intl.formatMessage({
                  id: "searchableList.search.panels",
                })}
                items={sampleTypeTests.panels}
                selectedIds={selectedPanels.map((panel) => panel.id)}
                onToggle={togglePanel}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <h4>
                <FormattedMessage id="sample.entry.available.tests" />
              </h4>
              <SearchableCheckboxList
                id="batch-tests"
                idPrefix="test_"
                searchLabel={intl.formatMessage({
                  id: "searchableList.search.tests",
                })}
                items={sampleTypeTests.tests}
                selectedIds={selectedTestIds}
                onToggle={toggleTest}
              />
            </Column>
          </>
        )}
      </Grid>
    </div>
  );
};

export default SampleType;
