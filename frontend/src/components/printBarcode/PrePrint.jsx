import React, { useState, useEffect, useRef } from "react";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
import { Grid, Column, NumberInput, Button } from "@carbon/react";
import config from "../../config.json";
import { getFromOpenElisServer } from "../utils/Utils";
import { sampleTypeTestsStructure } from "../data/SampleEntryTestsForTypeProvider";
import AutoComplete from "../common/AutoComplete";
import SearchableSelect from "../common/SearchableSelect";
import SearchableCheckboxList from "../common/SearchableCheckboxList";
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
  const [handTickedTests, setHandTickedTests] = useState([]);

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
    getFromOpenElisServer(
      "/rest/displayList/SAMPLE_PATIENT_REFERRING_CLINIC",
      getSiteList,
    );
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const panelTestIds = (panel) =>
    panel.testIds
      .split(",")
      .map((id) => id.trim())
      .filter((id) => sampleTypeTests.tests.some((test) => test.id === id));

  const togglePanel = (panel, checked) => {
    const others = selectedPanels.filter((item) => item.id !== panel.id);
    const ids = panelTestIds(panel);
    if (checked) {
      setSelectedPanels([...others, panel]);
      setSelectedTests((prev) => [
        ...prev,
        ...ids.filter((id) => !prev.includes(id)),
      ]);
    } else {
      const kept = others.flatMap(panelTestIds);
      setSelectedPanels(others);
      setSelectedTests((prev) =>
        prev.filter(
          (id) =>
            !ids.includes(id) ||
            kept.includes(id) ||
            handTickedTests.includes(id),
        ),
      );
    }
  };

  const toggleTest = (test, checked) => {
    setHandTickedTests((prev) =>
      checked
        ? [...prev.filter((id) => id !== test.id), test.id]
        : prev.filter((id) => id !== test.id),
    );
    setSelectedTests((prev) =>
      checked
        ? prev.includes(test.id)
          ? prev
          : [...prev, test.id]
        : prev.filter((id) => id !== test.id),
    );
  };

  const fetchSamplesTypes = (res) => {
    setSampleTypes(res);
  };

  const fetchSampleTypeTests = (res) => {
    setSampleTypeTests(res);
  };

  const chooseSampleType = (id) => {
    setSelectedSampleTypeId(id || null);
    setSampleTypeTests(sampleTypeTestsStructure);
    setSelectedTests([]);
    setSelectedPanels([]);
    setHandTickedTests([]);
  };

  const prePrintLabels = () => {
    const selectedTestIds = selectedTests.join(",");
    const params = new URLSearchParams({
      prePrinting: "true",
      numSetsOfLabels: labelSets,
      numOrderLabelsPerSet: orderLabelsPerSet,
      numSpecimenLabelsPerSet: specimenLabelsPerSet,
      facilityName: facilityId,
      testIds: selectedTestIds,
    });
    setSource(`${config.serverBaseUrl}/LabelMakerServlet?${params.toString()}`);
    setRenderBarcode(true);
  };

  useEffect(() => {
    getFromOpenElisServer("/rest/user-sample-types", fetchSamplesTypes);
  }, []);

  useEffect(() => {
    if (selectedSampleTypeId) {
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
            <SearchableSelect
              id="selectSampleType"
              labelText={intl.formatMessage({ id: "sample.type" })}
              placeholder={intl.formatMessage({ id: "sample.select.type" })}
              options={sampleTypes ?? []}
              value={selectedSampleTypeId ?? ""}
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
                  key={`panels_${selectedSampleTypeId}`}
                  id="preprint-panels"
                  idPrefix="panel_"
                  searchLabel={intl.formatMessage({
                    id: "searchableList.search.panels",
                  })}
                  items={sampleTypeTests.panels ?? []}
                  selectedIds={selectedPanels.map((panel) => panel.id)}
                  onToggle={togglePanel}
                />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <h4>
                  <FormattedMessage id="sample.entry.available.tests" />
                </h4>
                <SearchableCheckboxList
                  key={`tests_${selectedSampleTypeId}`}
                  id="preprint-tests"
                  idPrefix="test_"
                  searchLabel={intl.formatMessage({
                    id: "searchableList.search.tests",
                  })}
                  items={sampleTypeTests.tests ?? []}
                  selectedIds={selectedTests}
                  onToggle={toggleTest}
                />
              </Column>
            </>
          )}
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
