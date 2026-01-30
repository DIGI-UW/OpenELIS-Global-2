import React, { useState, useEffect, useCallback, useMemo } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Select,
  SelectItem,
  Checkbox,
  Search,
  InlineNotification,
  Loading,
  Tag,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../utils/Utils";

/**
 * AddTestsModal - Modal component for adding tests to selected sample items.
 *
 * Features:
 * - Sample type selection to filter available tests
 * - Panel selection with automatic test selection
 * - Individual test selection with search filtering
 * - Displays selected tests as tags
 * - Handles bulk test addition to multiple samples
 * - Shows success/error notifications
 * - React Intl for internationalization
 *
 * Props:
 * - open: boolean - whether modal is open
 * - onClose: () => void - callback when modal closes
 * - selectedSampleIds: string[] - array of selected sample item IDs
 * - selectedSamples: object[] - array of selected sample objects (to get sample type)
 * - onSuccess: (response) => void - callback on successful test addition
 *
 * Related: Feature 001-sample-management, User Story 2, Task T048
 */
function AddTestsModal({
  open,
  onClose,
  selectedSampleIds = [],
  selectedSamples = [],
  onSuccess,
}) {
  const intl = useIntl();

  // State for sample types
  const [sampleTypes, setSampleTypes] = useState([]);
  const [selectedSampleTypeId, setSelectedSampleTypeId] = useState("");
  const [loadingSampleTypes, setLoadingSampleTypes] = useState(false);

  // State for tests and panels
  const [sampleTypeTests, setSampleTypeTests] = useState({
    panels: [],
    tests: [],
  });
  const [selectedTests, setSelectedTests] = useState([]);
  const [selectedPanels, setSelectedPanels] = useState([]);
  const [loadingTests, setLoadingTests] = useState(false);

  // State for search
  const [testSearchTerm, setTestSearchTerm] = useState("");

  // State for submission
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  /**
   * Compute aliquot count from selected samples for display.
   */
  const aliquotCount = useMemo(() => {
    return selectedSamples.filter((sample) => sample.isAliquot).length;
  }, [selectedSamples]);

  /**
   * Compute samples with no remaining quantity (all volume dispensed).
   * These samples cannot have tests added.
   */
  const dispensedSamples = useMemo(() => {
    return selectedSamples.filter((sample) => !sample.hasRemainingQuantity);
  }, [selectedSamples]);

  /**
   * Compute samples that are eligible for test addition (have remaining quantity).
   */
  const eligibleSamples = useMemo(() => {
    return selectedSamples.filter((sample) => sample.hasRemainingQuantity);
  }, [selectedSamples]);

  /**
   * Fetch sample types when modal opens.
   */
  useEffect(() => {
    if (open) {
      setLoadingSampleTypes(true);
      setError(null);

      // Try to get sample type from selected samples
      if (selectedSamples.length > 0 && selectedSamples[0].sampleTypeId) {
        // Use the sample type from the first selected sample
        setSelectedSampleTypeId(selectedSamples[0].sampleTypeId);
      }

      getFromOpenElisServer("/rest/user-sample-types", (response) => {
        setLoadingSampleTypes(false);
        if (response && Array.isArray(response)) {
          setSampleTypes(response);
        } else {
          setError(
            intl.formatMessage({
              id: "sample.management.addTests.error.loadSampleTypes",
            }),
          );
        }
      });
    }
  }, [open, intl, selectedSamples]);

  /**
   * Fetch tests and panels when sample type changes.
   */
  useEffect(() => {
    if (selectedSampleTypeId) {
      setLoadingTests(true);
      setError(null);

      getFromOpenElisServer(
        `/rest/sample-type-tests?sampleType=${selectedSampleTypeId}`,
        (response) => {
          setLoadingTests(false);
          if (response) {
            setSampleTypeTests({
              panels: response.panels || [],
              tests: response.tests || [],
            });
          } else {
            setError(
              intl.formatMessage({
                id: "sample.management.addTests.error.loadTests",
              }),
            );
          }
        },
      );
    } else {
      setSampleTypeTests({ panels: [], tests: [] });
    }
  }, [selectedSampleTypeId, intl]);

  /**
   * Reset state when modal closes.
   */
  useEffect(() => {
    if (!open) {
      setSelectedTests([]);
      setSelectedPanels([]);
      setSelectedSampleTypeId("");
      setTestSearchTerm("");
      setError(null);
    }
  }, [open]);

  /**
   * Handle sample type selection change.
   */
  const handleSampleTypeChange = useCallback((e) => {
    const value = e.target.value;
    setSelectedSampleTypeId(value);
    // Clear selections when sample type changes
    setSelectedTests([]);
    setSelectedPanels([]);
    setTestSearchTerm("");
  }, []);

  /**
   * Handle panel checkbox change.
   * When a panel is selected, all its tests are automatically selected.
   */
  const handlePanelCheckbox = useCallback(
    (e, panel) => {
      const isChecked = e.target.checked;

      // Update selected panels
      let updatedPanels;
      if (isChecked) {
        updatedPanels = [
          ...selectedPanels,
          { id: panel.id, name: panel.name, testIds: panel.testIds },
        ];
      } else {
        updatedPanels = selectedPanels.filter((p) => p.id !== panel.id);
      }
      setSelectedPanels(updatedPanels);

      // Update selected tests based on panel selection
      const testIdsList = panel.testIds
        ? panel.testIds.split(",").map((id) => id.trim())
        : [];
      let updatedTests = [...selectedTests];

      if (isChecked) {
        // Add tests from panel if not already selected
        testIdsList.forEach((testId) => {
          const isTestSelected = updatedTests.some((t) => t.id === testId);
          if (!isTestSelected) {
            const test = sampleTypeTests.tests.find((t) => t.id === testId);
            if (test) {
              updatedTests.push({ id: test.id, name: test.name });
            }
          }
        });
      } else {
        // Remove tests from panel
        updatedTests = updatedTests.filter((t) => !testIdsList.includes(t.id));
      }
      setSelectedTests(updatedTests);
    },
    [selectedPanels, selectedTests, sampleTypeTests.tests],
  );

  /**
   * Handle individual test checkbox change.
   */
  const handleTestCheckbox = useCallback((e, test) => {
    const isChecked = e.target.checked;

    if (isChecked) {
      setSelectedTests((prev) => [...prev, { id: test.id, name: test.name }]);
    } else {
      setSelectedTests((prev) => prev.filter((t) => t.id !== test.id));
    }
  }, []);

  /**
   * Handle test search change.
   */
  const handleTestSearchChange = useCallback((e) => {
    setTestSearchTerm(e.target.value);
  }, []);

  /**
   * Filter tests based on search term.
   */
  const filteredTests = useMemo(() => {
    if (!testSearchTerm) {
      return sampleTypeTests.tests;
    }
    return sampleTypeTests.tests.filter((test) =>
      test.name.toLowerCase().includes(testSearchTerm.toLowerCase()),
    );
  }, [sampleTypeTests.tests, testSearchTerm]);

  /**
   * Check if a test is selected.
   */
  const isTestSelected = useCallback(
    (testId) => {
      return selectedTests.some((t) => t.id === testId);
    },
    [selectedTests],
  );

  /**
   * Check if a panel is selected.
   */
  const isPanelSelected = useCallback(
    (panelId) => {
      return selectedPanels.some((p) => p.id === panelId);
    },
    [selectedPanels],
  );

  /**
   * Remove a test from selection.
   */
  const handleRemoveTest = useCallback((testId) => {
    setSelectedTests((prev) => prev.filter((t) => t.id !== testId));
  }, []);

  /**
   * Handle form submission.
   */
  const handleSubmit = useCallback(() => {
    // Validate selection
    if (selectedTests.length === 0) {
      setError(
        intl.formatMessage({ id: "sample.management.addTests.error.noTests" }),
      );
      return;
    }

    // Validate that we have eligible samples (with remaining quantity)
    if (eligibleSamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "sample.management.addTests.error.noEligibleSamples",
        }),
      );
      return;
    }

    setLoading(true);
    setError(null);

    // Only submit eligible sample IDs (those with remaining quantity)
    const eligibleSampleIds = eligibleSamples.map((sample) => sample.id);
    const payload = {
      sampleItemIds: eligibleSampleIds,
      testIds: selectedTests.map((test) => test.id),
    };

    postToOpenElisServerFullResponse(
      "/rest/sample-management/add-tests",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.ok) {
          response.json().then((data) => {
            if (onSuccess) {
              onSuccess(data);
            }
            onClose();
          });
        } else {
          response
            .json()
            .then((errorData) => {
              setError(
                errorData.message ||
                  intl.formatMessage({
                    id: "sample.management.addTests.error.addFailed",
                  }),
              );
            })
            .catch(() => {
              setError(
                intl.formatMessage({
                  id: "sample.management.addTests.error.addFailed",
                }),
              );
            });
        }
      },
    );
  }, [selectedTests, eligibleSamples, onSuccess, onClose, intl]);

  /**
   * Clear error notification.
   */
  const handleDismissError = useCallback(() => {
    setError(null);
  }, []);

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      size="lg"
      style={{ maxWidth: "900px" }}
    >
      <ModalHeader
        title={intl.formatMessage({
          id: "sample.management.addTests.modal.title",
        })}
        label={intl.formatMessage(
          { id: "sample.management.addTests.modal.subtitle" },
          { count: selectedSampleIds.length },
        )}
      />
      <ModalBody
        style={{ minHeight: "500px", maxHeight: "70vh", overflow: "auto" }}
      >
        {/* Show warning banner when some samples have no remaining quantity */}
        {dispensedSamples.length > 0 && (
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "sample.management.addTests.warning.dispensedSamples.title",
            })}
            subtitle={intl.formatMessage(
              { id: "sample.management.addTests.warning.dispensedSamples" },
              {
                count: dispensedSamples.length,
                eligible: eligibleSamples.length,
              },
            )}
            hideCloseButton
            style={{ marginBottom: "1rem" }}
          />
        )}

        {/* Show aliquot info banner when adding tests to aliquots */}
        {aliquotCount > 0 && eligibleSamples.length > 0 && (
          <div
            style={{
              marginBottom: "1rem",
              padding: "0.75rem",
              backgroundColor: "#e0f0ff",
              borderRadius: "4px",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
            }}
          >
            <Tag type="blue" size="sm">
              {aliquotCount}{" "}
              {intl.formatMessage({
                id: "sample.management.addTests.modal.aliquotsIncluded",
              })}
            </Tag>
            <span style={{ fontSize: "0.875rem", color: "#0043ce" }}>
              {intl.formatMessage({
                id: "sample.management.addTests.modal.bulkAliquotNote",
              })}
            </span>
          </div>
        )}

        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "sample.management.error.title",
            })}
            subtitle={error}
            onClose={handleDismissError}
            style={{ marginBottom: "1rem" }}
          />
        )}

        {loadingSampleTypes ? (
          <Loading
            description={intl.formatMessage({
              id: "sample.management.search.loading",
            })}
            withOverlay={false}
          />
        ) : (
          <>
            {/* Sample Type Selection */}
            <div style={{ marginBottom: "1.5rem" }}>
              <Select
                id="sample-type-select"
                labelText={intl.formatMessage({
                  id: "sample.management.addTests.modal.selectSampleType",
                })}
                value={selectedSampleTypeId}
                onChange={handleSampleTypeChange}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "sample.management.addTests.modal.selectSampleTypePlaceholder",
                  })}
                />
                {sampleTypes.map((type) => (
                  <SelectItem key={type.id} value={type.id} text={type.value} />
                ))}
              </Select>
            </div>

            {/* Tests and Panels Section */}
            {selectedSampleTypeId && (
              <>
                {loadingTests ? (
                  <Loading
                    description={intl.formatMessage({
                      id: "sample.management.search.loading",
                    })}
                    withOverlay={false}
                  />
                ) : (
                  <div style={{ display: "flex", gap: "2rem" }}>
                    {/* Left Column: Panels and Tests */}
                    <div style={{ flex: "1", minWidth: "0" }}>
                      {/* Panels Section */}
                      {sampleTypeTests.panels.length > 0 && (
                        <Accordion>
                          <AccordionItem
                            title={intl.formatMessage({
                              id: "sample.management.addTests.modal.panels",
                            })}
                            open
                          >
                            <div
                              style={{
                                display: "flex",
                                flexDirection: "column",
                                gap: "0.5rem",
                                maxHeight: "200px",
                                overflowY: "auto",
                              }}
                            >
                              {sampleTypeTests.panels.map((panel) => (
                                <Checkbox
                                  key={panel.id}
                                  id={`panel-${panel.id}`}
                                  labelText={panel.name}
                                  checked={isPanelSelected(panel.id)}
                                  onChange={(e) =>
                                    handlePanelCheckbox(e, panel)
                                  }
                                />
                              ))}
                            </div>
                          </AccordionItem>
                        </Accordion>
                      )}

                      {/* Tests Section */}
                      <div style={{ marginTop: "1rem" }}>
                        <h5 style={{ marginBottom: "0.5rem" }}>
                          {intl.formatMessage({
                            id: "sample.management.addTests.modal.tests",
                          })}
                        </h5>
                        <Search
                          id="test-search"
                          placeholder={intl.formatMessage({
                            id: "sample.management.addTests.modal.searchTestsPlaceholder",
                          })}
                          value={testSearchTerm}
                          onChange={handleTestSearchChange}
                          size="sm"
                          style={{ marginBottom: "0.5rem" }}
                        />
                        <div
                          style={{
                            display: "flex",
                            flexDirection: "column",
                            gap: "0.25rem",
                            maxHeight: "250px",
                            overflowY: "auto",
                            border: "1px solid #e0e0e0",
                            borderRadius: "4px",
                            padding: "0.5rem",
                          }}
                        >
                          {filteredTests.length > 0 ? (
                            filteredTests.map((test) => (
                              <Checkbox
                                key={test.id}
                                id={`test-${test.id}`}
                                labelText={test.name}
                                checked={isTestSelected(test.id)}
                                onChange={(e) => handleTestCheckbox(e, test)}
                              />
                            ))
                          ) : (
                            <div
                              style={{ color: "#6f6f6f", fontStyle: "italic" }}
                            >
                              {intl.formatMessage({
                                id: "sample.management.addTests.modal.noTestsAvailable",
                              })}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Right Column: Selected Tests */}
                    <div
                      style={{
                        width: "280px",
                        borderLeft: "1px solid #e0e0e0",
                        paddingLeft: "1.5rem",
                      }}
                    >
                      <h5 style={{ marginBottom: "0.5rem" }}>
                        {intl.formatMessage({
                          id: "sample.management.addTests.modal.selectedTests",
                        })}{" "}
                        ({selectedTests.length})
                      </h5>
                      <div
                        style={{
                          display: "flex",
                          flexWrap: "wrap",
                          gap: "0.5rem",
                          maxHeight: "400px",
                          overflowY: "auto",
                        }}
                      >
                        {selectedTests.length > 0 ? (
                          selectedTests.map((test) => (
                            <Tag
                              key={test.id}
                              type="blue"
                              filter
                              onClose={() => handleRemoveTest(test.id)}
                            >
                              {test.name}
                            </Tag>
                          ))
                        ) : (
                          <div
                            style={{
                              color: "#6f6f6f",
                              fontStyle: "italic",
                            }}
                          >
                            {intl.formatMessage({
                              id: "sample.management.addTests.modal.noTestsSelected",
                            })}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </>
            )}

            {/* Prompt to select sample type */}
            {!selectedSampleTypeId && !loadingSampleTypes && (
              <div
                style={{
                  textAlign: "center",
                  padding: "3rem",
                  color: "#6f6f6f",
                }}
              >
                {intl.formatMessage({
                  id: "sample.management.addTests.modal.selectSampleTypeFirst",
                })}
              </div>
            )}
          </>
        )}
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose} disabled={loading}>
          {intl.formatMessage({
            id: "sample.management.addTests.modal.cancel",
          })}
        </Button>
        <Button
          kind="primary"
          onClick={handleSubmit}
          disabled={
            loading ||
            loadingTests ||
            selectedTests.length === 0 ||
            eligibleSamples.length === 0
          }
        >
          {loading
            ? intl.formatMessage({
                id: "sample.management.addTests.modal.adding",
              })
            : eligibleSamples.length === 0
              ? intl.formatMessage({
                  id: "sample.management.addTests.modal.noEligibleSamples",
                })
              : intl.formatMessage(
                  { id: "sample.management.addTests.modal.addCount" },
                  { count: selectedTests.length },
                )}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
}

export default AddTestsModal;
