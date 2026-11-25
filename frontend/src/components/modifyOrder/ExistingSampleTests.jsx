import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  Checkbox,
  DismissibleTag,
  FormGroup,
  InlineLoading,
  Layer,
  Search,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

/** The entries whose name contains the term, ignoring case. */
const matchingByName = (entries, term, nameOf) => {
  const query = (term || "").toLowerCase();
  return (entries || []).filter((entry) =>
    (nameOf(entry) || "").toLowerCase().includes(query),
  );
};

const testName = (test) => test.testName;
const panelName = (panel) => panel.name;

/** The sample's available tests in alphabetical order. */
export const sortedByName = (tests) =>
  [...(tests || [])].sort((a, b) =>
    (a.testName || "").localeCompare(b.testName || ""),
  );

/** The ids of the panel's tests that this sample can still take. */
export const panelTestIdsOn = (panel, tests) => {
  const available = new Set((tests || []).map((test) => test.testId));
  return String(panel?.testIds || "")
    .split(",")
    .map((id) => id.trim())
    .filter((id) => id !== "" && available.has(id));
};

/**
 * The tests that can be added to one existing sample of the order, laid out
 * like a new sample on Enter Order: the chosen tests as removable tags, a
 * search box that filters by name as the user types, the sample type's
 * panels, and the full alphabetical checkbox list.
 *
 * `tests` are the order form's possibleTests rows for this sample item;
 * `onAddChange(testIds, add)` marks those rows on the form.
 */
const ExistingSampleTests = (props) => {
  const { sampleItemId, accessionNumber, sampleType, sampleTypeId, tests } =
    props;
  const onAddChange = props.onAddChange || (() => {});

  const intl = useIntl();
  const componentMounted = useRef(false);

  const [testSearchTerm, setTestSearchTerm] = useState("");
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [panels, setPanels] = useState([]);
  const [panelsLoading, setPanelsLoading] = useState(false);

  useEffect(() => {
    componentMounted.current = true;
    if (!sampleTypeId) {
      setPanels([]);
      return () => {
        componentMounted.current = false;
      };
    }
    setPanelsLoading(true);
    getFromOpenElisServer(
      `/rest/sample-type-tests?sampleType=${sampleTypeId}`,
      (res) => {
        if (!componentMounted.current) {
          return;
        }
        setPanelsLoading(false);
        setPanels(
          Array.isArray(res?.panels)
            ? res.panels.filter((panel) => panel && panel.name !== "")
            : [],
        );
      },
    );
    return () => {
      componentMounted.current = false;
    };
  }, [sampleTypeId]);

  const availableTests = useMemo(() => sortedByName(tests), [tests]);
  const addedTests = useMemo(
    () => availableTests.filter((test) => test.add),
    [availableTests],
  );
  const addedIds = useMemo(
    () => new Set(addedTests.map((test) => test.testId)),
    [addedTests],
  );
  const searchBoxTests = useMemo(
    () =>
      testSearchTerm
        ? matchingByName(
            availableTests.filter((test) => !test.add),
            testSearchTerm,
            testName,
          )
        : [],
    [availableTests, testSearchTerm],
  );
  const searchBoxPanels = useMemo(
    () =>
      panelSearchTerm ? matchingByName(panels, panelSearchTerm, panelName) : [],
    [panels, panelSearchTerm],
  );

  /**
   * A panel is chosen when every test of it that this sample can still take
   * is ticked. The choice is read from the form rows rather than kept here,
   * so it survives Back and Next, which remount this component.
   */
  const panelTestIds = (panel) => panelTestIdsOn(panel, tests);
  const panelIsSelected = (panel) => {
    const ids = panelTestIds(panel);
    return ids.length > 0 && ids.every((id) => addedIds.has(id));
  };
  const selectedPanels = panels.filter(panelIsSelected);

  const addTest = (test) => {
    setTestSearchTerm("");
    if (!test.add) {
      onAddChange([test.testId], true);
    }
  };

  const removeTest = (test) => {
    onAddChange([test.testId], false);
  };

  const addPanel = (panel) => {
    setPanelSearchTerm("");
    const ids = panelTestIds(panel).filter((id) => !addedIds.has(id));
    if (ids.length) {
      onAddChange(ids, true);
    }
  };

  const removePanel = (panel) => {
    onAddChange(panelTestIds(panel), false);
  };

  const idSuffix = String(sampleItemId);
  const removeLabel = (name) =>
    intl.formatMessage(
      { id: "common.removeSelection", defaultMessage: "Remove {name}" },
      { name },
    );

  return (
    <div
      className="sampleType existingSampleTests"
      data-testid={"existing-sample-tests-" + idSuffix}
    >
      <h4>
        {accessionNumber}
        {sampleType ? ` (${sampleType})` : ""}
      </h4>
      {panels.length > 0 && (
        <div className="testPanels">
          <div className="cds--col">
            <h5>
              <FormattedMessage id="sample.label.orderpanel" />
            </h5>
            <div
              className="searchTestText"
              style={{ marginBottom: "1.188rem" }}
            >
              {selectedPanels.map((panel) => (
                <DismissibleTag
                  key={"existingPanelTag_" + idSuffix + "_" + panel.id}
                  type="green"
                  text={panel.name}
                  title={removeLabel(panel.name)}
                  dismissTooltipLabel={removeLabel(panel.name)}
                  onClose={() => removePanel(panel)}
                  style={{ marginRight: "0.5rem" }}
                />
              ))}
            </div>
            <FormGroup
              legendText={
                <FormattedMessage id="sample.search.panel.legend.text" />
              }
            >
              <Search
                size="lg"
                id={"existing_panels_search_" + idSuffix}
                labelText={
                  <FormattedMessage id="label.search.availablepanel" />
                }
                placeholder={intl.formatMessage({
                  id: "choose.availablepanel",
                })}
                onChange={(event) => setPanelSearchTerm(event.target.value)}
                value={panelSearchTerm}
              />
              {panelSearchTerm &&
                (searchBoxPanels.length ? (
                  <ul className="searchTestsList">
                    {searchBoxPanels.map((panel) => (
                      <li
                        role="menuitem"
                        className="singleTest"
                        key={"existingPanelFilter_" + idSuffix + "_" + panel.id}
                        onClick={() => addPanel(panel)}
                      >
                        {panel.name}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <Layer>
                    <Tile className="emptyFilterTests">
                      <span>
                        <FormattedMessage id="sample.panel.search.error.msg" />{" "}
                        <strong>&quot;{panelSearchTerm}&quot;</strong>
                      </span>
                    </Tile>
                  </Layer>
                ))}
            </FormGroup>
            {panels.map((panel) => (
              <Checkbox
                onChange={() =>
                  panelIsSelected(panel) ? removePanel(panel) : addPanel(panel)
                }
                labelText={panel.name}
                id={"existing_panel_" + idSuffix + "_" + panel.id}
                key={"existingPanel_" + idSuffix + "_" + panel.id}
                checked={panelIsSelected(panel)}
                disabled={panelTestIds(panel).length === 0}
              />
            ))}
          </div>
        </div>
      )}
      <div className="cds--col">
        {panelsLoading && (
          <InlineLoading
            description={intl.formatMessage({ id: "sample.tests.loading" })}
          />
        )}
        <div className="searchTestText" style={{ marginBottom: "1.188rem" }}>
          {addedTests.map((test) => (
            <DismissibleTag
              key={"existingTestTag_" + idSuffix + "_" + test.testId}
              type="red"
              text={test.testName}
              title={removeLabel(test.testName)}
              dismissTooltipLabel={removeLabel(test.testName)}
              onClose={() => removeTest(test)}
              style={{ marginRight: "0.5rem" }}
            />
          ))}
        </div>
        <FormGroup
          legendText={intl.formatMessage({
            id: "legend.search.availabletests",
          })}
        >
          <Search
            size="lg"
            id={"existing_tests_search_" + idSuffix}
            labelText={
              <FormattedMessage id="label.search.available.targetest" />
            }
            placeholder={intl.formatMessage({
              id: "holder.choose.availabletest",
            })}
            onChange={(event) => setTestSearchTerm(event.target.value)}
            value={testSearchTerm}
          />
          {testSearchTerm &&
            (searchBoxTests.length ? (
              <ul className="searchTestsList">
                {searchBoxTests.map((test) => (
                  <li
                    role="menuitem"
                    className="singleTest"
                    key={"existingTestFilter_" + idSuffix + "_" + test.testId}
                    onClick={() => addTest(test)}
                  >
                    {test.testName}
                  </li>
                ))}
              </ul>
            ) : (
              <Layer>
                <Tile className="emptyFilterTests">
                  <span>
                    <FormattedMessage id="title.notestfoundmatching" />
                    <strong> &quot;{testSearchTerm}&quot;</strong>
                  </span>
                </Tile>
              </Layer>
            ))}
        </FormGroup>
        {availableTests.map((test) => (
          <Checkbox
            onChange={(event) =>
              event.currentTarget.checked ? addTest(test) : removeTest(test)
            }
            labelText={test.testName}
            name="add"
            id={"existing_test_" + idSuffix + "_" + test.testId}
            key={"existingTest_" + idSuffix + "_" + test.testId}
            checked={Boolean(test.add)}
          />
        ))}
      </div>
    </div>
  );
};

export default ExistingSampleTests;
