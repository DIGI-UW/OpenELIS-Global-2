import { React, useState, useEffect, useRef } from "react";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
import {
  Checkbox,
  Select,
  SelectItem,
  Grid,
  Column,
  TextInput,
  NumberInput,
  Button,
} from "@carbon/react";
import { getFromOpenElisServer } from "../utils/Utils";
import { sampleTypeTestsStructure } from "../data/SampleEntryTestsForTypeProvider";
import AutoComplete from "../common/AutoComplete";
import "../Style.css";

const PrePrint = () => {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [selectedSampleTypeId, setSelectedSampleTypeId] = useState(null);

  const [sampleTypeTests, setSampleTypeTests] = useState(
    sampleTypeTestsStructure,
  );
  const [selectedTests, setSelectedTests] = useState([]);
  const [selectedPanels, setSelectedPanels] = useState([]);

  const [labelSets, setLabelSets] = useState(1);
  const [orderLabelsPerSet, setOrderLabelsPerSet] = useState(1);
  const [specimenLabelsPerSet, setSpecimenLabelsPerSet] = useState(1);
  const [facilityId, setFacilityId] = useState("");

  const [source, setSource] = useState("about:blank");
  const [renderBarcode, setRenderBarcode] = useState(false);
  const [siteNames, setSiteNames] = useState([]);

  const getSiteList = (response) => {
    if (componentMounted.current) {
      setSiteNames(response);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/site-names", getSiteList);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  function findTestById(testId) {
    return sampleTypeTests.tests.find((test) => test.id === testId);
  }
  function findTestIndex(testId) {
    return sampleTypeTests.tests.findIndex((test) => test.id === testId);
  }

  function updateSampleTypeTests(test, userBenchChoice = false) {
    let tests = [...sampleTypeTests.tests];
    let testIndex = findTestIndex(test.id);
    tests[testIndex].userBenchChoice = userBenchChoice;
    setSampleTypeTests({ ...sampleTypeTests, tests: tests });
  }

  const triggerPanelCheckBoxChange = (isChecked, testIds) => {
    const testIdsList = testIds.split(",").map((id) => id.trim());
    testIdsList.map((testId) => {
      let testIndex = findTestIndex(testId);
      let test = findTestById(testId);
      if (testIndex !== -1) {
        updateSampleTypeTests(test, isChecked);
        if (isChecked) {
          setSelectedTests((prevState) => {
            return [...prevState, { id: test.id, name: test.name }];
          });
        } else {
          removeTestFromSelectedTests(test);
        }
      }
    });
  };

  const addPanelToSelectedPanels = (panel) => {
    setSelectedPanels([
      ...selectedPanels,
      { id: panel.id, name: panel.name, testIds: panel.testIds },
    ]);
    triggerPanelCheckBoxChange(true, panel.testIds);
  };

  const removePanelFromSelectedPanels = (panel) => {
    let index = 0;
    for (let i in selectedPanels) {
      if (selectedPanels[i].id === panel.id) {
        triggerPanelCheckBoxChange(false, selectedPanels[i].testIds);
        const newPanels = selectedPanels;
        newPanels.splice(index, 1);
        setSelectedPanels([...newPanels]);
        break;
      }
      index++;
    }
  };

  const removeTestFromSelectedTests = (test) => {
    let index = 0;
    for (let i in selectedTests) {
      if (selectedTests[i].id === test.id) {
        const newTests = selectedTests;
        newTests.splice(index, 1);
        setSelectedTests([...newTests]);
        break;
      }
      index++;
    }
  };

  function addTestToSelectedTests(test) {
    setSelectedTests([...selectedTests, { id: test.id, name: test.name }]);
  }
  const handlePanelCheckbox = (e, panel) => {
    if (e.currentTarget.checked) {
      addPanelToSelectedPanels(panel);
    } else {
      removePanelFromSelectedPanels(panel);
    }
  };
  const handleTestCheckbox = (e, test) => {
    if (e.currentTarget.checked) {
      addTestToSelectedTests(test);
    } else {
      removeTestFromSelectedTests(test);
    }
  };
  const fetchSamplesTypes = (res) => {
    setSampleTypes(res);
  };

  const fetchSampleTypeTests = (res) => {
    setSampleTypeTests(res);
  };

  const handleFetchSampleTypeTests = (e) => {
    const { value } = e.target;
    setSelectedSampleTypeId(value);
    setSelectedTests([]);
    setSelectedPanels([]);
  };

  const prePrintLabels = () => {
    console.log(selectedPanels);
    const selectedTestIds = selectedTests
      .map((selectedTest) => selectedTest.id)
      .join(",");
    const params = new URLSearchParams({
      prePrinting: "true",
      numSetsOfLabels: labelSets,
      numOrderLabelsPerSet: orderLabelsPerSet,
      numSpecimenLabelsPerSet: specimenLabelsPerSet,
      facilityName: facilityId,
      testIds: selectedTestIds,
    });
    setSource(`LabelMakerServlet?${params.toString()}`);
    setRenderBarcode(true);
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

  return (
    <>
      <div className="orderLegendBody">
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <h4>
              <FormattedMessage id="barcode.print.preprint" />
            </h4>
          </Column>
          <Column lg={8} md={8} sm={4}>
            <NumberInput
              min={1}
              max={100}
              defaultValue={1}
              value={labelSets}
              onChange={(_, state) => setLabelSets(state.value)}
              label={intl.formatMessage({
                id: "label.barcode.labelsets",
              })}
              id="labelSets"
            />
          </Column>
          <Column lg={8} md={8} sm={4}></Column>
          <Column lg={16} md={8} sm={4}>
            {" "}
            <br></br>
          </Column>
          <Column lg={8} md={8} sm={4}>
            <NumberInput
              min={1}
              max={100}
              defaultValue={1}
              value={orderLabelsPerSet}
              onChange={(_, state) => setOrderLabelsPerSet(state.value)}
              label={intl.formatMessage({
                id: "label.barcode.orderlabel",
              })}
              id="orderLabelsPerSet"
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            {" "}
            <br></br>
          </Column>
          <Column lg={8} md={8} sm={4}>
            <NumberInput
              min={1}
              max={100}
              defaultValue={1}
              value={specimenLabelsPerSet}
              onChange={(_, state) => setSpecimenLabelsPerSet(state.value)}
              label={intl.formatMessage({
                id: "label.barcode.specimenlabel",
              })}
              id="specimenLabelsPerSet"
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            {" "}
            <br></br>
          </Column>
          <Column lg={8} md={8} sm={4}>
            <NumberInput
              readOnly
              value={labelSets * (orderLabelsPerSet + specimenLabelsPerSet)}
              label={intl.formatMessage({
                id: "label.barcode.totallabel",
              })}
              id="totalLabelsToPrint"
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            {" "}
            <br></br>
          </Column>
          <Column lg={8} md={8} sm={4}>
            <AutoComplete
              name="siteName"
              id="siteName"
              allowFreeText={false}
              value={facilityId}
              onSelect={(id) => setFacilityId(id)}
              label={
                <>
                  <FormattedMessage id="order.search.site.name" />{" "}
                </>
              }
              style={{ width: "!important 100%" }}
              suggestions={siteNames.length > 0 ? siteNames : []}
            />
          </Column>
        </Grid>
      </div>
      <div className="orderLegendBody">
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <h4>
              <FormattedMessage id="label.button.sample" />
            </h4>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="selectSampleType"
              labelText="Sample Type"
              onChange={(e) => {
                handleFetchSampleTypeTests(e);
              }}
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
          <Column lg={16} md={8} sm={4}>
            {selectedSampleTypeId && (
              <h4>
                <FormattedMessage id="sample.entry.panels" />
              </h4>
            )}
            {sampleTypeTests.panels !== null &&
              sampleTypeTests.panels.map((panel) => {
                return panel.name === "" ? (
                  ""
                ) : (
                  <Checkbox
                    onChange={(e) => handlePanelCheckbox(e, panel)}
                    labelText={panel.name}
                    id={"panel_" + panel.id}
                    key={panel.id}
                    checked={
                      selectedPanels.filter((item) => item.id === panel.id)
                        .length > 0
                    }
                  />
                );
              })}
          </Column>
          <Column lg={16} md={8} sm={4}>
            {selectedSampleTypeId && (
              <h4>
                <FormattedMessage id="sample.entry.available.tests" />
              </h4>
            )}
            {sampleTypeTests.tests != null &&
              sampleTypeTests.tests.map((test) => {
                return test.name === "" ? (
                  ""
                ) : (
                  <Checkbox
                    onChange={(e) => handleTestCheckbox(e, test)}
                    labelText={test.name}
                    id={"test_" + test.id}
                    key={test.id}
                    checked={
                      selectedTests.filter((item) => item.id === test.id)
                        .length > 0
                    }
                  />
                );
              })}
          </Column>
          <Column lg={16} md={8} sm={4}>
            {" "}
            <br></br>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <FormattedMessage id="barcode.print.preprint.note" />
          </Column>
          <Column lg={16} md={8} sm={4}>
            {" "}
            <br></br>
          </Column>
          <Column lg={8} md={8} sm={4}>
            <Button
              data-cy="pre-Print"
              disabled={selectedTests.length == 0}
              onClick={prePrintLabels}
            >
              <FormattedMessage id="barcode.print.preprint.button" />
            </Button>
          </Column>
        </Grid>
      </div>
      {renderBarcode && (
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <h4>
                <FormattedMessage id="barcode.header" />
              </h4>
            </Column>
          </Grid>
          <iframe src={source} width="100%" height="500px" />
        </div>
      )}
    </>
  );
};
export default injectIntl(PrePrint);
