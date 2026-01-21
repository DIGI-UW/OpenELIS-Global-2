import React, { useState, useCallback, useEffect, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextArea,
  Tag,
  TextInput,
  Checkbox,
  Dropdown,
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
  TableToolbar,
  TableToolbarContent,
  TableBatchActions,
  TableBatchAction,
  Loading,
} from "@carbon/react";
import { Checkmark, Edit, Renew, WarningAlt } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

/**
 * QC Checklist items for Biorepository sample inspection
 * Based on ISO 20387:2018 quality control requirements
 */
const QC_CHECKLIST = [
  {
    id: "samplePresent",
    labelId: "biorepository.qc.samplePresent",
    defaultLabel: "Sample physically present in location",
  },
  {
    id: "labelIntegrity",
    labelId: "biorepository.qc.labelIntegrity",
    defaultLabel: "Label intact and legible",
  },
  {
    id: "containerIntegrity",
    labelId: "biorepository.qc.containerIntegrity",
    defaultLabel: "Container intact (no cracks/leaks)",
  },
  {
    id: "volumeAppearanceAcceptable",
    labelId: "biorepository.qc.volumeAppearance",
    defaultLabel: "Volume and appearance acceptable",
  },
  {
    id: "correctPosition",
    labelId: "biorepository.qc.correctPosition",
    defaultLabel: "Sample in correct storage position",
  },
];

/**
 * Discrepancy types for failed QC
 */
const DISCREPANCY_TYPES = [
  { id: "MISSING_SAMPLE", label: "Missing Sample" },
  { id: "DAMAGED_LABEL", label: "Damaged/Illegible Label" },
  { id: "MISPLACED_ITEM", label: "Misplaced Item (Wrong Position)" },
  { id: "CONTAINER_DAMAGE", label: "Container Damage" },
  { id: "VOLUME_DISCREPANCY", label: "Volume Discrepancy" },
  { id: "OTHER", label: "Other" },
];

/**
 * BiorepositoryQCInspectionPage - QC Inspection workflow for stored samples
 *
 * Allows technicians to perform visual inspection and verification of samples
 * in storage against their expected location and condition.
 *
 * Features:
 * - Load samples with workflowStatus = STORED
 * - Display storage coordinates (freezer/shelf/rack/box/position)
 * - 5-point QC checklist (presence, label, container, volume, position)
 * - Auto-calculate QC result (all pass = VERIFIED, any fail = DISCREPANCY_FOUND)
 * - Record discrepancy details (type + corrective action)
 * - Bulk apply QC to multiple samples
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function BiorepositoryQCInspectionPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();

  // State for samples
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Bulk apply modal state
  const [bulkApplyModalOpen, setBulkApplyModalOpen] = useState(false);
  const [isBulkApplying, setIsBulkApplying] = useState(false);
  const [selectedForBulkApply, setSelectedForBulkApply] = useState([]); // Capture selection when modal opens

  // Bulk apply form values
  const [bulkApplyValues, setBulkApplyValues] = useState({
    inspectorName: "",
    inspectionDate: new Date().toISOString().slice(0, 16),
    // QC Checklist - 5 boolean criteria
    qcChecklist: QC_CHECKLIST.reduce((acc, item) => {
      acc[item.id] = false;
      return acc;
    }, {}),
    // Auto-calculated QC result
    qcResult: "",
    // Discrepancy details (only for failed QC)
    discrepancyType: "",
    correctiveAction: "",
    remarks: "",
  });

  // Load stored samples for QC
  const loadStoredSamples = useCallback(() => {
    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/biorepository/qc-inspection/samples`,
      (response) => {
        setLoading(false);
        if (response && Array.isArray(response)) {
          // Transform API response to component state
          const transformedSamples = response.map((sample) => ({
            id: sample.bioSampleId,
            sampleItemId: sample.sampleItemId,
            externalId: sample.externalId || "-",
            accessionNumber: sample.accessionNumber || "-",
            sampleType: sample.sampleType || "-",
            locationPath: sample.locationPath || "Not Assigned",
            storageLocation: sample.storageLocation, // Full location object
            biosafetyLevel: sample.biosafetyLevel || "-",
            workflowStatus: sample.workflowStatus,
            lastQCInspection: sample.lastQCInspection, // Most recent inspection record
          }));
          setSamples(transformedSamples);
        } else {
          setSamples([]);
        }
      },
    );
  }, []);

  // Load samples on mount
  useEffect(() => {
    loadStoredSamples();
  }, [loadStoredSamples]);

  // Reset bulk apply values
  const resetBulkApplyValues = () => {
    setBulkApplyValues({
      inspectorName: "",
      inspectionDate: new Date().toISOString().slice(0, 16),
      qcChecklist: QC_CHECKLIST.reduce((acc, item) => {
        acc[item.id] = false;
        return acc;
      }, {}),
      qcResult: "",
      discrepancyType: "",
      correctiveAction: "",
      remarks: "",
    });
  };

  // Calculate QC result based on checklist
  const calculateQCResult = (checklist) => {
    const allPassed = Object.values(checklist).every((v) => v === true);
    return allPassed ? "VERIFIED" : "DISCREPANCY_FOUND";
  };

  // Handle checklist change
  const handleChecklistChange = (criteriaId, checked) => {
    setBulkApplyValues((prev) => {
      const newChecklist = { ...prev.qcChecklist, [criteriaId]: checked };
      const autoResult = calculateQCResult(newChecklist);
      return {
        ...prev,
        qcChecklist: newChecklist,
        qcResult: autoResult,
        // Clear fail-related fields if now passing
        discrepancyType: autoResult === "VERIFIED" ? "" : prev.discrepancyType,
        correctiveAction:
          autoResult === "VERIFIED" ? "" : prev.correctiveAction,
      };
    });
  };

  // Handle "Check All" for QC criteria
  const handleCheckAll = () => {
    setBulkApplyValues((prev) => ({
      ...prev,
      qcChecklist: QC_CHECKLIST.reduce((acc, item) => {
        acc[item.id] = true;
        return acc;
      }, {}),
      qcResult: "VERIFIED",
      discrepancyType: "",
      correctiveAction: "",
    }));
  };

  // Handle "Clear All" for QC criteria
  const handleClearAll = () => {
    setBulkApplyValues((prev) => ({
      ...prev,
      qcChecklist: QC_CHECKLIST.reduce((acc, item) => {
        acc[item.id] = false;
        return acc;
      }, {}),
      qcResult: "DISCREPANCY_FOUND",
    }));
  };

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedForBulkApply.length === 0) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noSelection",
          defaultMessage: "Please select samples to apply QC to.",
        }),
      );
      return;
    }

    // Validate: Inspector name required
    if (!bulkApplyValues.inspectorName.trim()) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noInspector",
          defaultMessage: "Please enter inspector name.",
        }),
      );
      return;
    }

    // Validate: QC result must be set
    if (!bulkApplyValues.qcResult) {
      setError(
        intl.formatMessage({
          id: "biorepository.qc.error.noResult",
          defaultMessage:
            "Please complete the QC checklist to determine pass/fail status.",
        }),
      );
      return;
    }

    // Validate: If discrepancy found, must have discrepancy type and corrective action
    if (bulkApplyValues.qcResult === "DISCREPANCY_FOUND") {
      if (!bulkApplyValues.discrepancyType) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noDiscrepancyType",
            defaultMessage: "Please select a discrepancy type.",
          }),
        );
        return;
      }
      if (!bulkApplyValues.correctiveAction.trim()) {
        setError(
          intl.formatMessage({
            id: "biorepository.qc.error.noCorrectiveAction",
            defaultMessage: "Please describe the corrective action taken.",
          }),
        );
        return;
      }
    }

    setIsBulkApplying(true);
    setError(null);

    // Prepare request payload
    const payload = {
      bioSampleIds: selectedForBulkApply.map((id) => parseInt(id, 10)),
      inspectorName: bulkApplyValues.inspectorName.trim(),
      inspectionDate: bulkApplyValues.inspectionDate
        ? new Date(bulkApplyValues.inspectionDate).toISOString()
        : null,
      samplePresent: bulkApplyValues.qcChecklist.samplePresent,
      labelIntegrity: bulkApplyValues.qcChecklist.labelIntegrity,
      containerIntegrity: bulkApplyValues.qcChecklist.containerIntegrity,
      volumeAppearanceAcceptable:
        bulkApplyValues.qcChecklist.volumeAppearanceAcceptable,
      correctPosition: bulkApplyValues.qcChecklist.correctPosition,
      discrepancyType:
        bulkApplyValues.qcResult === "DISCREPANCY_FOUND"
          ? bulkApplyValues.discrepancyType
          : null,
      correctiveAction:
        bulkApplyValues.qcResult === "DISCREPANCY_FOUND"
          ? bulkApplyValues.correctiveAction
          : null,
      remarks: bulkApplyValues.remarks || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/biorepository/qc-inspection/bulk-apply`,
      JSON.stringify(payload),
      (response) => {
        setIsBulkApplying(false);
        if (response && !response.error) {
          const count = response.count || selectedForBulkApply.length;
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "biorepository.qc.success.applied",
                defaultMessage: "QC inspection applied to {count} sample(s).",
              },
              { count },
            ),
          );
          setBulkApplyModalOpen(false);
          resetBulkApplyValues();
          setSelectedForBulkApply([]); // Clear captured selection
          loadStoredSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "biorepository.qc.error.apply",
                defaultMessage:
                  "Failed to apply QC inspection. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedForBulkApply,
    bulkApplyValues,
    intl,
    loadStoredSamples,
    onProgressUpdate,
  ]);

  // Calculate stats
  const totalSamples = samples.length;
  const verifiedCount = samples.filter(
    (s) => s.lastQCInspection && s.lastQCInspection.qcResult === "VERIFIED",
  ).length;
  const discrepanciesCount = samples.filter(
    (s) =>
      s.lastQCInspection && s.lastQCInspection.qcResult === "DISCREPANCY_FOUND",
  ).length;
  const pendingCount = samples.filter((s) => !s.lastQCInspection).length;

  // Count checked criteria
  const checkedCount = useMemo(() => {
    return Object.values(bulkApplyValues.qcChecklist).filter((v) => v).length;
  }, [bulkApplyValues.qcChecklist]);

  // Get QC result tag
  const getQCTag = (qcResult) => {
    if (!qcResult) return <Tag type="gray">Pending</Tag>;
    if (qcResult === "VERIFIED") return <Tag type="green">Verified</Tag>;
    if (qcResult === "DISCREPANCY_FOUND")
      return <Tag type="red">Discrepancy</Tag>;
    return <Tag type="gray">{qcResult}</Tag>;
  };

  // Table headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "biorepository.sample.accessionNumber",
        defaultMessage: "Sample Number",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "biorepository.sample.type",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "locationPath",
      header: intl.formatMessage({
        id: "biorepository.sample.storageLocation",
        defaultMessage: "Storage Location",
      }),
    },
    {
      key: "biosafetyLevel",
      header: intl.formatMessage({
        id: "biorepository.sample.bsl",
        defaultMessage: "BSL",
      }),
    },
    {
      key: "lastQCInspection",
      header: intl.formatMessage({
        id: "biorepository.qc.lastInspection",
        defaultMessage: "Last QC Inspection",
      }),
    },
  ];

  return (
    <div className="biorepository-qc-inspection-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="biorepository.qc.title"
            defaultMessage="Quality Control Inspection"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="biorepository.qc.description"
            defaultMessage="Perform visual inspection and verification of stored samples. Use the QC checklist to assess physical presence, label integrity, container condition, volume/appearance, and storage position. Samples with discrepancies require corrective action documentation."
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
                  id="biorepository.qc.totalStored"
                  defaultMessage="Total Stored"
                />
              </span>
              <span className="progress-value">{totalSamples}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="biorepository.qc.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{verifiedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="biorepository.qc.discrepancies"
                  defaultMessage="Discrepancies"
                />
              </span>
              <span className="progress-value">{discrepanciesCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="biorepository.qc.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Messages */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

      {/* Samples Table */}
      <div className="sample-table-section" style={{ marginTop: "1rem" }}>
        {loading ? (
          <div style={{ padding: "2rem", textAlign: "center" }}>
            <Loading withOverlay={false} />
          </div>
        ) : samples.length === 0 ? (
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "biorepository.qc.noSamples",
              defaultMessage: "No Stored Samples",
            })}
            subtitle={intl.formatMessage({
              id: "biorepository.qc.noSamples.message",
              defaultMessage:
                "No samples with STORED status available for QC inspection.",
            })}
            lowContrast
            hideCloseButton
          />
        ) : (
          <DataTable
            rows={samples.map((sample) => ({
              id: sample.id.toString(),
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType,
              locationPath: sample.locationPath,
              biosafetyLevel: sample.biosafetyLevel,
              lastQCInspection: sample.lastQCInspection,
              _raw: sample,
            }))}
            headers={headers}
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              getBatchActionProps,
              selectedRows,
            }) => {
              // Get selected IDs from Carbon's DataTable state (read-only, no setState in render)
              const currentSelectedIds = selectedRows.map((r) => r.id);

              return (
                <TableContainer>
                  <TableToolbar>
                    <TableBatchActions {...getBatchActionProps()}>
                      <TableBatchAction
                        renderIcon={Edit}
                        iconDescription={intl.formatMessage({
                          id: "biorepository.qc.bulkApply",
                          defaultMessage: "Bulk Apply QC",
                        })}
                        onClick={() => {
                          // Capture selection when modal opens
                          setSelectedForBulkApply(currentSelectedIds);
                          resetBulkApplyValues();
                          setBulkApplyModalOpen(true);
                        }}
                      >
                        <FormattedMessage
                          id="biorepository.qc.bulkApply"
                          defaultMessage="Bulk Apply QC"
                        />
                      </TableBatchAction>
                    </TableBatchActions>
                    <TableToolbarContent>
                      <Button
                        kind="ghost"
                        size="sm"
                        renderIcon={Renew}
                        iconDescription={intl.formatMessage({
                          id: "label.refresh",
                          defaultMessage: "Refresh",
                        })}
                        hasIconOnly
                        onClick={loadStoredSamples}
                        disabled={loading}
                      />
                    </TableToolbarContent>
                  </TableToolbar>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
                        {headers.map((header) => (
                          <TableHeader
                            {...getHeaderProps({ header })}
                            key={header.key}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const sample = samples.find(
                          (s) => s.id.toString() === row.id,
                        );
                        return (
                          <TableRow {...getRowProps({ row })} key={row.id}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => {
                              if (cell.info.header === "biosafetyLevel") {
                                let bslColor = "gray";
                                if (cell.value === "BSL_1") bslColor = "green";
                                else if (cell.value === "BSL_2")
                                  bslColor = "teal";
                                else if (cell.value === "BSL_3")
                                  bslColor = "purple";
                                else if (cell.value === "BSL_4")
                                  bslColor = "red";
                                return (
                                  <TableCell key={cell.id}>
                                    <Tag type={bslColor}>{cell.value}</Tag>
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "lastQCInspection") {
                                if (!sample.lastQCInspection) {
                                  return (
                                    <TableCell key={cell.id}>
                                      <Tag type="gray">Never Inspected</Tag>
                                    </TableCell>
                                  );
                                }
                                const qc = sample.lastQCInspection;
                                return (
                                  <TableCell key={cell.id}>
                                    <div
                                      style={{
                                        display: "flex",
                                        gap: "0.5rem",
                                        alignItems: "center",
                                      }}
                                    >
                                      {getQCTag(qc.qcResult)}
                                      <span
                                        style={{
                                          fontSize: "0.75rem",
                                          color: "#525252",
                                        }}
                                      >
                                        {new Date(
                                          qc.inspectionDate,
                                        ).toLocaleDateString()}
                                      </span>
                                    </div>
                                  </TableCell>
                                );
                              }
                              return (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              );
                            })}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          </DataTable>
        )}
      </div>

      {/* Bulk Apply QC Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "biorepository.qc.bulkApply.title",
          defaultMessage: "Bulk QC Inspection",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : bulkApplyValues.qcResult === "VERIFIED"
              ? intl.formatMessage({
                  id: "biorepository.qc.action.verify",
                  defaultMessage: "Record Verification",
                })
              : bulkApplyValues.qcResult === "DISCREPANCY_FOUND"
                ? intl.formatMessage({
                    id: "biorepository.qc.action.recordDiscrepancy",
                    defaultMessage: "Record Discrepancy",
                  })
                : intl.formatMessage({
                    id: "label.apply",
                    defaultMessage: "Apply",
                  })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkApply}
        onSecondarySubmit={() => setBulkApplyModalOpen(false)}
        size="md"
        primaryButtonDisabled={isBulkApplying || !bulkApplyValues.qcResult}
        danger={bulkApplyValues.qcResult === "DISCREPANCY_FOUND"}
      >
        <div className="qc-bulk-apply-modal">
          <p className="modal-description">
            <FormattedMessage
              id="biorepository.qc.bulkApply.description"
              defaultMessage="Apply QC inspection to {count} selected sample(s)."
              values={{ count: selectedForBulkApply.length }}
            />
          </p>

          {/* Inspector Information */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="biorepository.qc.section.inspector"
                defaultMessage="Inspector Information"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="inspectorName"
                  labelText={intl.formatMessage({
                    id: "biorepository.qc.inspectorName",
                    defaultMessage: "Inspector Name (Required)",
                  })}
                  value={bulkApplyValues.inspectorName}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      inspectorName: e.target.value,
                    }))
                  }
                  placeholder={intl.formatMessage({
                    id: "biorepository.qc.inspectorName.placeholder",
                    defaultMessage: "Enter inspector name",
                  })}
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <div className="cds--form-item">
                  <label className="cds--label">
                    <FormattedMessage
                      id="biorepository.qc.inspectionDate"
                      defaultMessage="Inspection Date & Time"
                    />
                  </label>
                  <input
                    type="datetime-local"
                    className="cds--text-input"
                    value={bulkApplyValues.inspectionDate}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        inspectionDate: e.target.value,
                      }))
                    }
                  />
                </div>
              </Column>
            </Grid>
          </div>

          {/* QC Checklist Section */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="biorepository.qc.section.checklist"
                defaultMessage="QC Checklist"
              />
              <span className="qc-checklist-count">
                ({checkedCount}/{QC_CHECKLIST.length})
              </span>
            </h5>
            <div className="qc-checklist-actions">
              <Button kind="ghost" size="sm" onClick={handleCheckAll}>
                <FormattedMessage
                  id="biorepository.qc.checkAll"
                  defaultMessage="Check All (Verify)"
                />
              </Button>
              <Button kind="ghost" size="sm" onClick={handleClearAll}>
                <FormattedMessage
                  id="biorepository.qc.clearAll"
                  defaultMessage="Clear All"
                />
              </Button>
            </div>
            <div className="qc-checklist-items">
              {QC_CHECKLIST.map((criteria) => (
                <Checkbox
                  key={criteria.id}
                  id={`qc-${criteria.id}`}
                  labelText={intl.formatMessage({
                    id: criteria.labelId,
                    defaultMessage: criteria.defaultLabel,
                  })}
                  checked={bulkApplyValues.qcChecklist[criteria.id]}
                  onChange={(_, { checked }) =>
                    handleChecklistChange(criteria.id, checked)
                  }
                />
              ))}
            </div>
          </div>

          {/* QC Result Indicator */}
          <div className="qc-section qc-decision-section">
            <h5 className="qc-section-header">
              <FormattedMessage
                id="biorepository.qc.section.result"
                defaultMessage="QC Result"
              />
            </h5>
            <div
              className={`qc-result-indicator ${bulkApplyValues.qcResult === "VERIFIED" ? "pass" : bulkApplyValues.qcResult === "DISCREPANCY_FOUND" ? "fail" : ""}`}
            >
              {bulkApplyValues.qcResult === "VERIFIED" && (
                <Tag type="green" size="md">
                  <Checkmark size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="biorepository.qc.result.verified"
                    defaultMessage="QC VERIFIED - All checks passed"
                  />
                </Tag>
              )}
              {bulkApplyValues.qcResult === "DISCREPANCY_FOUND" && (
                <Tag type="red" size="md">
                  <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="biorepository.qc.result.discrepancy"
                    defaultMessage="DISCREPANCY FOUND - Corrective action required"
                  />
                </Tag>
              )}
              {!bulkApplyValues.qcResult && (
                <Tag type="gray" size="md">
                  <FormattedMessage
                    id="biorepository.qc.result.pending"
                    defaultMessage="Complete checklist to determine result"
                  />
                </Tag>
              )}
            </div>
          </div>

          {/* Discrepancy Details - Only show if QC result is DISCREPANCY_FOUND */}
          {bulkApplyValues.qcResult === "DISCREPANCY_FOUND" && (
            <div className="qc-section">
              <h5 className="qc-section-header">
                <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="biorepository.qc.section.discrepancy"
                  defaultMessage="Discrepancy Details"
                />
              </h5>
              <Grid fullWidth>
                <Column lg={16} md={8} sm={4}>
                  <Dropdown
                    id="discrepancyType"
                    titleText={intl.formatMessage({
                      id: "biorepository.qc.discrepancyType",
                      defaultMessage: "Discrepancy Type (Required)",
                    })}
                    label={intl.formatMessage({
                      id: "biorepository.qc.discrepancyType.placeholder",
                      defaultMessage: "Select discrepancy type",
                    })}
                    items={DISCREPANCY_TYPES}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItem={DISCREPANCY_TYPES.find(
                      (d) => d.id === bulkApplyValues.discrepancyType,
                    )}
                    onChange={({ selectedItem }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        discrepancyType: selectedItem?.id || "",
                      }))
                    }
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="correctiveAction"
                    labelText={intl.formatMessage({
                      id: "biorepository.qc.correctiveAction",
                      defaultMessage: "Corrective Action Taken (Required)",
                    })}
                    value={bulkApplyValues.correctiveAction}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        correctiveAction: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "biorepository.qc.correctiveAction.placeholder",
                      defaultMessage:
                        "Describe the corrective action taken to resolve the discrepancy...",
                    })}
                    rows={3}
                  />
                </Column>
              </Grid>
            </div>
          )}

          {/* Remarks (Optional) */}
          <div className="qc-section">
            <Grid fullWidth>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="remarks"
                  labelText={intl.formatMessage({
                    id: "biorepository.qc.remarks",
                    defaultMessage: "Remarks (Optional)",
                  })}
                  value={bulkApplyValues.remarks}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      remarks: e.target.value,
                    }))
                  }
                  placeholder={intl.formatMessage({
                    id: "biorepository.qc.remarks.placeholder",
                    defaultMessage: "Additional observations or notes...",
                  })}
                  rows={2}
                />
              </Column>
            </Grid>
          </div>
        </div>
      </Modal>
    </div>
  );
}

BiorepositoryQCInspectionPage.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
  progress: PropTypes.object,
  onProgressUpdate: PropTypes.func,
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

export default BiorepositoryQCInspectionPage;
