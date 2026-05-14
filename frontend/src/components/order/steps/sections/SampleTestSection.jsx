import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  Select,
  SelectItem,
  Button,
  Checkbox,
  Tag,
  Search,
  Link,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * SampleTestSection - Sample type and test/panel selection
 *
 * Implements:
 * - ORD-7: Test/panel selection with pagination and filters
 * - ORD-1b: Tests/panels optional at this step
 * - Multiple samples support
 */

const SampleTestSection = ({
  samples,
  setSamples,
  orderData,
  setOrderData,
  isReadOnly,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Sample types data (shared)
  const [sampleTypes, setSampleTypes] = useState([]);

  // Tests/panels data PER SAMPLE (keyed by sample index)
  const [testsPerSample, setTestsPerSample] = useState({});
  const [panelsPerSample, setPanelsPerSample] = useState({});

  // Filter state PER SAMPLE
  const [testSearchTerms, setTestSearchTerms] = useState({});
  const [panelSearchTerms, setPanelSearchTerms] = useState({});

  // Loading state
  const [isLoading, setIsLoading] = useState(true);
  const [loadingPerSample, setLoadingPerSample] = useState({});

  // Fetch sample types on mount
  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);

    // Fetch sample types
    getFromOpenElisServer("/rest/user-sample-types", (response) => {
      if (componentMounted.current && response) {
        setSampleTypes(response);
        setIsLoading(false);
      }
    });

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Track which sample types we've already fetched tests for (per sample index)
  const fetchedSampleTypesRef = useRef({});

  // Fetch tests/panels when samples are loaded (e.g., from an existing order)
  useEffect(() => {
    samples.forEach((sample, index) => {
      const sampleTypeId = sample?.sampleTypeId;
      if (
        sampleTypeId &&
        fetchedSampleTypesRef.current[index] !== sampleTypeId
      ) {
        fetchedSampleTypesRef.current[index] = sampleTypeId;
        fetchTestsForSampleType(index, sampleTypeId);
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [samples]);

  // Fetch tests and panels for a specific sample
  const fetchTestsForSampleType = (sampleIndex, sampleTypeId) => {
    if (!sampleTypeId) {
      setTestsPerSample((prev) => ({ ...prev, [sampleIndex]: [] }));
      setPanelsPerSample((prev) => ({ ...prev, [sampleIndex]: [] }));
      return;
    }

    setLoadingPerSample((prev) => ({ ...prev, [sampleIndex]: true }));
    getFromOpenElisServer(
      `/rest/sample-type-tests?sampleType=${sampleTypeId}`,
      (response) => {
        if (componentMounted.current && response) {
          // Set panels for this sample
          const panels = response.panels || [];
          setPanelsPerSample((prev) => ({ ...prev, [sampleIndex]: panels }));

          // Set tests for this sample
          const tests = response.tests || [];
          setTestsPerSample((prev) => ({ ...prev, [sampleIndex]: tests }));
          setLoadingPerSample((prev) => ({ ...prev, [sampleIndex]: false }));
        }
      },
    );
  };

  // Get filtered tests for a sample
  const getFilteredTests = (sampleIndex) => {
    const tests = testsPerSample[sampleIndex] || [];
    const searchTerm = testSearchTerms[sampleIndex] || "";
    if (!searchTerm) return tests;
    const term = searchTerm.toLowerCase();
    return tests.filter((test) => test.name?.toLowerCase().includes(term));
  };

  // Get filtered panels for a sample
  const getFilteredPanels = (sampleIndex) => {
    const panels = panelsPerSample[sampleIndex] || [];
    const searchTerm = panelSearchTerms[sampleIndex] || "";
    if (!searchTerm) return panels;
    const term = searchTerm.toLowerCase();
    return panels.filter((panel) => panel.name?.toLowerCase().includes(term));
  };

  // Add new sample
  const handleAddSample = () => {
    const newSample = {
      index: samples.length,
      sampleRejected: false,
      rejectionReason: "",
      sampleTypeId: "",
      sampleXML: null,
      panels: [],
      tests: [],
      requestReferralEnabled: false,
      referralItems: [],
    };
    setSamples([...samples, newSample]);
  };

  // Remove sample
  const handleRemoveSample = (index) => {
    const updated = samples.filter((_, i) => i !== index);
    setSamples(updated);
  };

  // Update sample type and fetch tests/panels
  const handleSampleTypeChange = (sampleIndex, sampleTypeId) => {
    const updated = [...samples];
    const currentSampleType = updated[sampleIndex]?.sampleTypeId;

    // Only clear panels/tests if user is changing to a DIFFERENT sample type
    const shouldClearSelections =
      currentSampleType && currentSampleType !== sampleTypeId;

    updated[sampleIndex] = {
      ...updated[sampleIndex],
      sampleTypeId,
      // Clear previous selections only when sample type actually changes
      ...(shouldClearSelections ? { panels: [], tests: [] } : {}),
    };
    setSamples(updated);

    // Fetch tests and panels for the selected sample type (for this specific sample)
    if (sampleTypeId !== fetchedSampleTypesRef.current[sampleIndex]) {
      fetchedSampleTypesRef.current[sampleIndex] = sampleTypeId;
      fetchTestsForSampleType(sampleIndex, sampleTypeId);
    }
  };

  // Toggle panel selection - also adds/removes all tests from the panel
  const handlePanelToggle = (sampleIndex, panel, isSelected) => {
    const updated = [...samples];
    const currentPanels = updated[sampleIndex].panels || [];
    const currentTests = updated[sampleIndex].tests || [];
    const availableTests = testsPerSample[sampleIndex] || [];

    // Get test IDs from panel (comma-separated string)
    const panelTestIds = panel.testIds
      ? panel.testIds.split(",").map((id) => id.trim())
      : [];

    if (isSelected) {
      // Add panel with testIds
      updated[sampleIndex].panels = [
        ...currentPanels,
        { id: panel.id, name: panel.name, testIds: panel.testIds },
      ];

      // Add tests from panel that aren't already selected
      const testsToAdd = panelTestIds
        .filter((testId) => !currentTests.some((t) => t.id === testId))
        .map((testId) => {
          // Find test name from available tests for this sample
          const test = availableTests.find((t) => t.id === testId);
          return { id: testId, name: test?.name || testId };
        });

      updated[sampleIndex].tests = [...currentTests, ...testsToAdd];
    } else {
      // Remove panel
      updated[sampleIndex].panels = currentPanels.filter(
        (p) => p.id !== panel.id,
      );

      // Remove tests that belong to this panel (unless they belong to another selected panel)
      const otherPanelTestIds = new Set();
      updated[sampleIndex].panels.forEach((p) => {
        if (p.testIds) {
          p.testIds
            .split(",")
            .forEach((id) => otherPanelTestIds.add(id.trim()));
        }
      });

      updated[sampleIndex].tests = currentTests.filter(
        (t) => !panelTestIds.includes(t.id) || otherPanelTestIds.has(t.id),
      );
    }

    setSamples(updated);
  };

  // Toggle test selection
  const handleTestToggle = (sampleIndex, test, isSelected) => {
    const updated = [...samples];
    const currentTests = updated[sampleIndex].tests || [];

    if (isSelected) {
      // Add test
      updated[sampleIndex].tests = [
        ...currentTests,
        { id: test.id, name: test.name },
      ];
    } else {
      // Remove test
      updated[sampleIndex].tests = currentTests.filter((t) => t.id !== test.id);
    }

    setSamples(updated);
  };

  // Remove panel tag - also removes tests from that panel
  const handleRemovePanel = (sampleIndex, panelId) => {
    const updated = [...samples];
    const currentPanels = updated[sampleIndex].panels || [];
    const currentTests = updated[sampleIndex].tests || [];

    // Find the panel being removed to get its testIds
    const panelToRemove = currentPanels.find((p) => p.id === panelId);
    const panelTestIds = panelToRemove?.testIds
      ? panelToRemove.testIds.split(",").map((id) => id.trim())
      : [];

    // Remove the panel
    updated[sampleIndex].panels = currentPanels.filter((p) => p.id !== panelId);

    // Get test IDs from remaining panels
    const remainingPanelTestIds = new Set();
    updated[sampleIndex].panels.forEach((p) => {
      if (p.testIds) {
        p.testIds
          .split(",")
          .forEach((id) => remainingPanelTestIds.add(id.trim()));
      }
    });

    // Remove tests that belong to the removed panel (unless they belong to another panel)
    updated[sampleIndex].tests = currentTests.filter(
      (t) => !panelTestIds.includes(t.id) || remainingPanelTestIds.has(t.id),
    );

    setSamples(updated);
  };

  // Remove test tag
  const handleRemoveTest = (sampleIndex, testId) => {
    const updated = [...samples];
    updated[sampleIndex].tests = updated[sampleIndex].tests.filter(
      (t) => t.id !== testId,
    );
    setSamples(updated);
  };

  // Toggle referral
  const handleReferralToggle = (sampleIndex, enabled) => {
    const updated = [...samples];
    updated[sampleIndex].requestReferralEnabled = enabled;
    setSamples(updated);
  };

  // Check if panel is selected for a sample
  const isPanelSelected = (sampleIndex, panelId) => {
    return samples[sampleIndex]?.panels?.some((p) => p.id === panelId) || false;
  };

  // Check if test is selected for a sample
  const isTestSelected = (sampleIndex, testId) => {
    return samples[sampleIndex]?.tests?.some((t) => t.id === testId) || false;
  };

  return (
    <Tile className="order-section sample-test-section">
      <h4 className="section-title">
        <FormattedMessage id="label.button.sample" defaultMessage="Sample" />
      </h4>

      {/* Sample Cards */}
      {samples.map((sample, sampleIndex) => (
        <div key={sampleIndex} className="sample-card">
          <div className="sample-card-header">
            <h5>
              <FormattedMessage
                id="label.button.sample"
                defaultMessage="Sample"
              />{" "}
              {sampleIndex + 1}
            </h5>
            {samples.length > 1 && (
              <Link
                onClick={() => handleRemoveSample(sampleIndex)}
                disabled={isReadOnly}
              >
                <FormattedMessage
                  id="sample.remove.action"
                  defaultMessage="Remove Sample"
                />
              </Link>
            )}
          </div>

          <Grid>
            {/* Sample Type and Lab Unit Filter */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id={`sampleType-${sampleIndex}`}
                labelText={intl.formatMessage({
                  id: "sample.type",
                  defaultMessage: "Sample Type",
                })}
                value={sample.sampleTypeId || ""}
                onChange={(e) =>
                  handleSampleTypeChange(sampleIndex, e.target.value)
                }
                disabled={isReadOnly}
              >
                <SelectItem value="" text="" />
                {sampleTypes.map((type) => (
                  <SelectItem key={type.id} value={type.id} text={type.value} />
                ))}
              </Select>
            </Column>
            {/* Panels Section - show only when sample type is selected */}
            {sample.sampleTypeId ? (
              <Column lg={16} md={8} sm={4}>
                <div className="panels-section">
                  <h6>
                    <FormattedMessage
                      id="sample.orderPanels"
                      defaultMessage="Order Panels"
                    />
                  </h6>

                  {/* Selected Panels Tags */}
                  <div className="selected-tags">
                    {sample.panels?.map((panel) => (
                      <Tag
                        key={panel.id}
                        type="blue"
                        filter
                        onClose={() => handleRemovePanel(sampleIndex, panel.id)}
                        disabled={isReadOnly}
                      >
                        {panel.name}
                      </Tag>
                    ))}
                  </div>

                  {getFilteredPanels(sampleIndex).length > 0 ? (
                    <>
                      {/* Panel Search */}
                      <Search
                        id={`panelSearch-${sampleIndex}`}
                        labelText=""
                        placeholder={intl.formatMessage({
                          id: "panel.search.placeholder",
                          defaultMessage: "Search panels...",
                        })}
                        value={panelSearchTerms[sampleIndex] || ""}
                        onChange={(e) =>
                          setPanelSearchTerms((prev) => ({
                            ...prev,
                            [sampleIndex]: e.target.value,
                          }))
                        }
                        disabled={isReadOnly}
                        size="sm"
                      />

                      {/* Panel Checkboxes */}
                      <div className="checkbox-list">
                        {getFilteredPanels(sampleIndex).map((panel) => (
                          <Checkbox
                            key={panel.id}
                            id={`panel-${sampleIndex}-${panel.id}`}
                            labelText={panel.name}
                            checked={isPanelSelected(sampleIndex, panel.id)}
                            onChange={(_, { checked }) =>
                              handlePanelToggle(sampleIndex, panel, checked)
                            }
                            disabled={isReadOnly}
                          />
                        ))}
                      </div>
                    </>
                  ) : loadingPerSample[sampleIndex] ? (
                    <p className="no-items-message">Loading panels...</p>
                  ) : (
                    <p className="no-items-message">
                      <FormattedMessage
                        id="sample.noPanels"
                        defaultMessage="No panels available for this sample type"
                      />
                    </p>
                  )}
                </div>
              </Column>
            ) : null}

            {/* Tests Section - show only when sample type is selected */}
            {sample.sampleTypeId ? (
              <Column lg={16} md={8} sm={4}>
                <div className="tests-section">
                  <h6>
                    <FormattedMessage
                      id="sample.orderTests"
                      defaultMessage="Order Tests"
                    />
                  </h6>

                  {/* Selected Tests Tags */}
                  <div className="selected-tags">
                    {sample.tests?.map((test) => (
                      <Tag
                        key={test.id}
                        type="teal"
                        filter
                        onClose={() => handleRemoveTest(sampleIndex, test.id)}
                        disabled={isReadOnly}
                      >
                        {test.name}
                      </Tag>
                    ))}
                  </div>

                  {getFilteredTests(sampleIndex).length > 0 ? (
                    <>
                      {/* Test Search */}
                      <Search
                        id={`testSearch-${sampleIndex}`}
                        labelText=""
                        placeholder={intl.formatMessage({
                          id: "test.search.placeholder",
                          defaultMessage: "Search tests...",
                        })}
                        value={testSearchTerms[sampleIndex] || ""}
                        onChange={(e) =>
                          setTestSearchTerms((prev) => ({
                            ...prev,
                            [sampleIndex]: e.target.value,
                          }))
                        }
                        disabled={isReadOnly}
                        size="sm"
                      />

                      {/* Test Checkboxes - show all tests without pagination */}
                      <div className="checkbox-list checkbox-list-scrollable">
                        {getFilteredTests(sampleIndex).map((test) => (
                          <Checkbox
                            key={test.id}
                            id={`test-${sampleIndex}-${test.id}`}
                            labelText={test.name}
                            checked={isTestSelected(sampleIndex, test.id)}
                            onChange={(_, { checked }) =>
                              handleTestToggle(sampleIndex, test, checked)
                            }
                            disabled={isReadOnly}
                          />
                        ))}
                      </div>

                      {/* Show total count for reference */}
                      <span className="test-count-info">
                        <FormattedMessage
                          id="sample.testsCount"
                          defaultMessage="{count} tests available"
                          values={{
                            count: getFilteredTests(sampleIndex).length,
                          }}
                        />
                      </span>
                    </>
                  ) : loadingPerSample[sampleIndex] ? (
                    <p className="no-items-message">Loading tests...</p>
                  ) : (
                    <p className="no-items-message">
                      <FormattedMessage
                        id="sample.noTests"
                        defaultMessage="No tests available for this sample type"
                      />
                    </p>
                  )}
                </div>
              </Column>
            ) : (
              <Column lg={16} md={8} sm={4}>
                <p className="select-sample-type-message">
                  <FormattedMessage
                    id="sample.selectType.first"
                    defaultMessage="Select a sample type above to see available panels and tests"
                  />
                </p>
              </Column>
            )}

            {/* Referral Option */}
            <Column lg={16} md={8} sm={4}>
              <Checkbox
                id={`referral-${sampleIndex}`}
                labelText={intl.formatMessage({
                  id: "sample.referToReferenceLab",
                  defaultMessage: "Refer test to a reference lab",
                })}
                checked={sample.requestReferralEnabled || false}
                onChange={(_, { checked }) =>
                  handleReferralToggle(sampleIndex, checked)
                }
                disabled={isReadOnly}
              />
            </Column>
          </Grid>
        </div>
      ))}

      {/* Add Sample Button */}
      <Button
        kind="tertiary"
        size="md"
        onClick={handleAddSample}
        disabled={isReadOnly}
        renderIcon={Add}
      >
        <FormattedMessage id="sample.add.action" defaultMessage="Add Sample" />
      </Button>
    </Tile>
  );
};

export default SampleTestSection;
