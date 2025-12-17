import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  Loading,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Checkbox,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Add, Checkmark, Microscope, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyTestingMicroscopyPage - Page 4 of the pathology workflow.
 * Purpose: Perform diagnostic or research assays on sample-derived material.
 * Who uses it: Pathologists / PIs / technicians
 */
function PathologyTestingMicroscopyPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Selection state for bulk operations
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Testing Modal state
  const [testingModalOpen, setTestingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [isBulkMode, setIsBulkMode] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Testing form state
  const [testData, setTestData] = useState({
    // Routine Stains
    routineStains: [],
    // Special Stains
    specialStains: [],
    // Advanced Techniques
    advancedTechniques: [],
    ihcMarkers: "",
    // Research Assays
    researchAssays: [],
    // Controls
    positiveControlRun: false,
    positiveControlResult: "",
    negativeControlRun: false,
    negativeControlResult: "",
    assayAccepted: false,
    // Documentation
    blockSlideId: "",
    testName: "",
    result: "",
    technicianSignature: "",
    pathologistVerification: "",
    testDate: "",
  });

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => {
              // Extract test data from sample.data if available
              const sampleData = sample.data || {};

              // Format stains for display
              const allStains = [
                ...(sampleData.routineStains || []),
                ...(sampleData.specialStains || []),
                ...(sampleData.advancedTechniques || []),
              ];
              const stainsDisplay =
                allStains.length > 0
                  ? allStains.slice(0, 3).join(", ") +
                    (allStains.length > 3 ? "..." : "")
                  : "";

              return {
                id: String(sample.id || sample.sampleItemId),
                accessionNumber: sample.accessionNumber,
                blockSlideId: sampleData.blockSlideId || sample.blockSlideId,
                specimenType:
                  sample.sampleType || sample.typeOfSample?.description,
                status: sample.pageStatus || "PENDING",
                testsPerformed: sample.testsPerformed || 0,
                resultStatus: sample.resultStatus,
                // Test result fields from sample data
                testName: sampleData.testName || "",
                result: sampleData.result || "",
                stains: stainsDisplay,
                technicianSignature: sampleData.technicianSignature || "",
                testDate: sampleData.testDate || "",
              };
            });
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setTestData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleMultiCheckbox = (fieldName, value, checked) => {
    setTestData((prev) => ({
      ...prev,
      [fieldName]: checked
        ? [...prev[fieldName], value]
        : prev[fieldName].filter((v) => v !== value),
    }));
  };

  const handleDateChange = (dates, fieldName) => {
    if (dates && dates.length > 0) {
      const date = dates[0];
      const formattedDate = date.toISOString().split("T")[0];
      setTestData((prev) => ({
        ...prev,
        [fieldName]: formattedDate,
      }));
    }
  };

  // Reset test form data
  const resetTestData = (sample = null) => {
    setTestData({
      routineStains: [],
      specialStains: [],
      advancedTechniques: [],
      ihcMarkers: "",
      researchAssays: [],
      positiveControlRun: false,
      positiveControlResult: "",
      negativeControlRun: false,
      negativeControlResult: "",
      assayAccepted: false,
      blockSlideId: sample?.blockSlideId || "",
      testName: "",
      result: "",
      technicianSignature: "",
      pathologistVerification: "",
      testDate: new Date().toISOString().split("T")[0],
    });
  };

  // Open modal for bulk operation
  const openBulkTestingModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.noSelection",
          defaultMessage: "Please select at least one sample for bulk testing.",
        }),
      );
      return;
    }

    setSelectedSample(null);
    setIsBulkMode(true);
    resetTestData();
    setTestingModalOpen(true);
  };

  // Handle selection changes
  const handleSelectionChange = (selectedRows) => {
    const ids = selectedRows.map((row) => row.id);
    setSelectedSampleIds(ids);
  };

  // Get selected samples for display
  const getSelectedSamples = () => {
    return samples.filter((s) => selectedSampleIds.includes(s.id));
  };

  const openTestingModal = (sample) => {
    setSelectedSample(sample);
    setIsBulkMode(false);
    resetTestData(sample);
    setTestingModalOpen(true);
  };

  const handleSubmitTest = () => {
    if (submitting) return;

    // Validate required fields - in bulk mode, blockSlideId is optional
    if (!testData.testName || !testData.technicianSignature) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.requiredFields",
          defaultMessage: "Please fill in Test Name and Technician Signature",
        }),
      );
      return;
    }

    // For single mode, blockSlideId is required
    if (!isBulkMode && !testData.blockSlideId) {
      setError(
        intl.formatMessage({
          id: "pathology.testing.error.blockSlideRequired",
          defaultMessage: "Please fill in Block/Slide ID",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    if (isBulkMode) {
      // Bulk submission
      const payload = {
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        pageId: pageData?.id,
        entryId: entryId,
        ...testData,
      };

      postToOpenElisServerJsonResponse(
        `/rest/notebook/pathology/testing/bulk-submit`,
        JSON.stringify(payload),
        (response) => {
          setSubmitting(false);
          if (response && response.success) {
            setTestingModalOpen(false);
            setSelectedSampleIds([]);
            setSuccessMessage(
              intl.formatMessage(
                {
                  id: "pathology.testing.success.bulkSubmit",
                  defaultMessage:
                    "Successfully submitted test results for {count} samples.",
                },
                { count: response.processedCount || selectedSampleIds.length },
              ),
            );
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(
              response?.error ||
                intl.formatMessage({
                  id: "pathology.testing.error.bulkSubmitFailed",
                  defaultMessage:
                    "Failed to submit bulk test results. Please try again.",
                }),
            );
          }
        },
      );
    } else {
      // Single sample submission
      const payload = {
        sampleId: selectedSample?.id,
        pageId: pageData?.id,
        entryId: entryId,
        ...testData,
      };

      postToOpenElisServer(
        `/rest/notebook/pathology/testing/submit`,
        JSON.stringify(payload),
        (status) => {
          setSubmitting(false);
          if (status === 200) {
            setTestingModalOpen(false);
            setSelectedSample(null);
            setSuccessMessage(
              intl.formatMessage({
                id: "pathology.testing.success.submit",
                defaultMessage: "Test results submitted successfully.",
              }),
            );
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(
              intl.formatMessage({
                id: "pathology.testing.error.submitFailed",
                defaultMessage:
                  "Failed to submit test results. Please try again.",
              }),
            );
          }
        },
      );
    }
  };

  // Calculate stats
  const testedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingReviewCount = samples.filter(
    (s) => s.resultStatus === "PENDING_REVIEW",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Table headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.table.accessionNumber",
        defaultMessage: "Accession Number",
      }),
    },
    {
      key: "blockSlideId",
      header: intl.formatMessage({
        id: "pathology.table.blockSlideId",
        defaultMessage: "Block/Slide ID",
      }),
    },
    {
      key: "specimenType",
      header: intl.formatMessage({
        id: "pathology.table.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "testName",
      header: intl.formatMessage({
        id: "pathology.table.testName",
        defaultMessage: "Test Name",
      }),
    },
    {
      key: "stains",
      header: intl.formatMessage({
        id: "pathology.table.stains",
        defaultMessage: "Stains",
      }),
    },
    {
      key: "result",
      header: intl.formatMessage({
        id: "pathology.table.result",
        defaultMessage: "Result",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "pathology.table.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "pathology.table.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  // Stain options
  const routineStainOptions = [
    "H&E",
    "Papanicolaou",
    "Romanowsky",
    "Giemsa",
    "Wright",
  ];
  const specialStainOptions = [
    "AFB",
    "GMS",
    "PAS",
    "Gram",
    "Trichrome",
    "Reticulin",
    "Iron",
  ];
  const advancedOptions = ["IHC", "ICC", "ISH", "FISH"];
  const researchAssayOptions = [
    "Laser microdissection",
    "Multiplex IHC",
    "Immunofluorescent staining",
    "RNAscope",
  ];

  return (
    <div className="pathology-testing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.testing.title"
            defaultMessage="Testing, Staining & Microscopy"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.testing.description"
            defaultMessage="Perform diagnostic or research assays on sample-derived material. Record stains, controls, and results."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.tested"
                  defaultMessage="Tested"
                />
              </span>
              <span className="progress-value">{testedCount}</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#fff8e1" }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.pendingReview"
                  defaultMessage="Pending Review"
                />
              </span>
              <span className="progress-value">{pendingReviewCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.testing.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <Grid fullWidth className="action-buttons-section">
        <Column lg={16} md={8} sm={4}>
          <div
            className="action-buttons"
            style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}
          >
            <Button
              kind="primary"
              size="md"
              renderIcon={Add}
              onClick={openBulkTestingModal}
              disabled={selectedSampleIds.length === 0}
            >
              <FormattedMessage
                id="pathology.page.testing.bulkAddTest"
                defaultMessage="Add Test to Selected ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="tertiary"
              size="md"
              renderIcon={Renew}
              onClick={loadPageSamples}
            >
              <FormattedMessage
                id="label.button.refresh"
                defaultMessage="Refresh"
              />
            </Button>
          </div>
        </Column>
      </Grid>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Success Display */}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onCloseButtonClick={() => setSuccessMessage(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Sample Table */}
      <div className="sample-grid-container">
        {loading ? (
          <Loading withOverlay={false} description="Loading samples..." />
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.testing.empty"
                defaultMessage="No samples available for testing. Samples must be processed on the previous page first."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples}
            headers={headers}
            isSortable
            render={({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              selectedRows,
            }) => {
              // Update selection state when DataTable selection changes
              if (
                selectedRows.length !== selectedSampleIds.length ||
                !selectedRows.every((r) => selectedSampleIds.includes(r.id))
              ) {
                setTimeout(() => handleSelectionChange(selectedRows), 0);
              }

              return (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const sample = samples.find((s) => s.id === row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "status" ? (
                                  <Tag
                                    type={
                                      cell.value === "COMPLETED"
                                        ? "green"
                                        : cell.value === "IN_PROGRESS"
                                          ? "blue"
                                          : "gray"
                                    }
                                    size="sm"
                                  >
                                    {cell.value}
                                  </Tag>
                                ) : cell.info.header === "actions" ? (
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={Microscope}
                                    onClick={() => openTestingModal(sample)}
                                  >
                                    <FormattedMessage
                                      id="pathology.page.testing.addTest"
                                      defaultMessage="Add Test"
                                    />
                                  </Button>
                                ) : cell.info.header === "result" ? (
                                  <span
                                    title={cell.value}
                                    style={{
                                      maxWidth: "150px",
                                      overflow: "hidden",
                                      textOverflow: "ellipsis",
                                      whiteSpace: "nowrap",
                                      display: "inline-block",
                                    }}
                                  >
                                    {cell.value
                                      ? cell.value.length > 30
                                        ? cell.value.substring(0, 30) + "..."
                                        : cell.value
                                      : "-"}
                                  </span>
                                ) : cell.info.header === "testName" ||
                                  cell.info.header === "stains" ? (
                                  <span>{cell.value || "-"}</span>
                                ) : (
                                  cell.value
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          />
        )}
      </div>

      {/* Testing Modal */}
      <Modal
        open={testingModalOpen}
        modalHeading={
          isBulkMode
            ? intl.formatMessage(
                {
                  id: "pathology.modal.testing.bulkTitle",
                  defaultMessage: "Bulk Record Test - {count} Samples",
                },
                { count: selectedSampleIds.length },
              )
            : intl.formatMessage(
                {
                  id: "pathology.modal.testing.title",
                  defaultMessage: "Record Test - {accession}",
                },
                { accession: selectedSample?.accessionNumber || "" },
              )
        }
        primaryButtonText={intl.formatMessage({
          id: "label.button.submit",
          defaultMessage: "Submit",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setTestingModalOpen(false);
          setSelectedSample(null);
          setIsBulkMode(false);
          setError(null);
        }}
        onRequestSubmit={handleSubmitTest}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <Grid fullWidth>
          {/* Show selected samples in bulk mode */}
          {isBulkMode && (
            <Column lg={16} md={8} sm={4}>
              <div style={{ marginBottom: "1.5rem" }}>
                <p style={{ marginBottom: "0.5rem", fontWeight: "bold" }}>
                  <FormattedMessage
                    id="pathology.modal.testing.selectedSamples"
                    defaultMessage="Selected Samples:"
                  />
                </p>
                <div
                  style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}
                >
                  {getSelectedSamples().map((sample) => (
                    <Tag key={sample.id} type="blue" size="sm">
                      {sample.accessionNumber || sample.id}
                    </Tag>
                  ))}
                </div>
              </div>
            </Column>
          )}

          <Column lg={16} md={8} sm={4}>
            <Accordion>
              {/* Routine Stains */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "pathology.testing.routineStains",
                  defaultMessage: "Routine Stains",
                })}
              >
                <div style={{ display: "flex", flexWrap: "wrap", gap: "1rem" }}>
                  {routineStainOptions.map((stain) => (
                    <Checkbox
                      key={stain}
                      id={`routine-${stain}`}
                      labelText={stain}
                      checked={testData.routineStains.includes(stain)}
                      onChange={(e) =>
                        handleMultiCheckbox(
                          "routineStains",
                          stain,
                          e.target.checked,
                        )
                      }
                    />
                  ))}
                </div>
              </AccordionItem>

              {/* Special Stains */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "pathology.testing.specialStains",
                  defaultMessage: "Special Stains",
                })}
              >
                <div style={{ display: "flex", flexWrap: "wrap", gap: "1rem" }}>
                  {specialStainOptions.map((stain) => (
                    <Checkbox
                      key={stain}
                      id={`special-${stain}`}
                      labelText={stain}
                      checked={testData.specialStains.includes(stain)}
                      onChange={(e) =>
                        handleMultiCheckbox(
                          "specialStains",
                          stain,
                          e.target.checked,
                        )
                      }
                    />
                  ))}
                </div>
              </AccordionItem>

              {/* Advanced Techniques */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "pathology.testing.advancedTechniques",
                  defaultMessage: "Advanced Techniques",
                })}
              >
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{ display: "flex", flexWrap: "wrap", gap: "1rem" }}
                    >
                      {advancedOptions.map((tech) => (
                        <Checkbox
                          key={tech}
                          id={`advanced-${tech}`}
                          labelText={tech}
                          checked={testData.advancedTechniques.includes(tech)}
                          onChange={(e) =>
                            handleMultiCheckbox(
                              "advancedTechniques",
                              tech,
                              e.target.checked,
                            )
                          }
                        />
                      ))}
                    </div>
                  </Column>
                  {(testData.advancedTechniques.includes("IHC") ||
                    testData.advancedTechniques.includes("ICC")) && (
                    <Column lg={16} md={8} sm={4}>
                      <TextInput
                        id="ihcMarkers"
                        name="ihcMarkers"
                        labelText={intl.formatMessage({
                          id: "pathology.testing.ihcMarkers",
                          defaultMessage: "IHC/ICC Markers",
                        })}
                        value={testData.ihcMarkers}
                        onChange={handleInputChange}
                      />
                    </Column>
                  )}
                </Grid>
              </AccordionItem>

              {/* Controls */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "pathology.testing.controls",
                  defaultMessage: "Controls & Validation",
                })}
              >
                <Grid fullWidth>
                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="positiveControlRun"
                      name="positiveControlRun"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.positiveControlRun",
                        defaultMessage: "Positive Control Run",
                      })}
                      checked={testData.positiveControlRun}
                      onChange={handleInputChange}
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="positiveControlResult"
                      name="positiveControlResult"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.positiveControlResult",
                        defaultMessage: "Positive Control Result",
                      })}
                      value={testData.positiveControlResult}
                      onChange={handleInputChange}
                      disabled={!testData.positiveControlRun}
                    >
                      <SelectItem value="" text="" />
                      <SelectItem value="Pass" text="Pass" />
                      <SelectItem value="Fail" text="Fail" />
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="negativeControlRun"
                      name="negativeControlRun"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.negativeControlRun",
                        defaultMessage: "Negative Control Run",
                      })}
                      checked={testData.negativeControlRun}
                      onChange={handleInputChange}
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="negativeControlResult"
                      name="negativeControlResult"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.negativeControlResult",
                        defaultMessage: "Negative Control Result",
                      })}
                      value={testData.negativeControlResult}
                      onChange={handleInputChange}
                      disabled={!testData.negativeControlRun}
                    >
                      <SelectItem value="" text="" />
                      <SelectItem value="Pass" text="Pass" />
                      <SelectItem value="Fail" text="Fail" />
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="assayAccepted"
                      name="assayAccepted"
                      labelText={intl.formatMessage({
                        id: "pathology.testing.assayAccepted",
                        defaultMessage: "Assay Accepted",
                      })}
                      checked={testData.assayAccepted}
                      onChange={handleInputChange}
                    />
                  </Column>
                </Grid>
              </AccordionItem>
            </Accordion>
          </Column>

          {/* Documentation */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.testing.documentation"
                defaultMessage="Documentation *"
              />
            </h5>
          </Column>

          {!isBulkMode && (
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="blockSlideId"
                name="blockSlideId"
                labelText={intl.formatMessage({
                  id: "pathology.testing.blockSlideId",
                  defaultMessage: "Block/Slide ID *",
                })}
                value={testData.blockSlideId}
                onChange={handleInputChange}
              />
            </Column>
          )}

          <Column lg={isBulkMode ? 16 : 8} md={isBulkMode ? 8 : 4} sm={4}>
            <TextInput
              id="testName"
              name="testName"
              labelText={intl.formatMessage({
                id: "pathology.testing.testName",
                defaultMessage: "Test Name *",
              })}
              value={testData.testName}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="result"
              name="result"
              labelText={intl.formatMessage({
                id: "pathology.testing.result",
                defaultMessage: "Result",
              })}
              value={testData.result}
              onChange={handleInputChange}
              rows={4}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="technicianSignature"
              name="technicianSignature"
              labelText={intl.formatMessage({
                id: "pathology.testing.technicianSignature",
                defaultMessage: "Technician Signature *",
              })}
              value={testData.technicianSignature}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="pathologistVerification"
              name="pathologistVerification"
              labelText={intl.formatMessage({
                id: "pathology.testing.pathologistVerification",
                defaultMessage: "Pathologist Verification",
              })}
              value={testData.pathologistVerification}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => handleDateChange(dates, "testDate")}
            >
              <DatePickerInput
                id="testDate"
                labelText={intl.formatMessage({
                  id: "pathology.testing.testDate",
                  defaultMessage: "Test Date",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologyTestingMicroscopyPage;
