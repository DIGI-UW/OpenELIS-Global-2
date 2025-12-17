import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  TextInput,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  FilterableMultiSelect,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Tag,
} from "@carbon/react";
import { Checkmark, Edit } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * AnalyticsTestAssignmentPage - Page 2 of the Analytics Laboratory workflow.
 * Purpose: Assign responsibility and select analytical methods per sample.
 *
 * Data Points:
 * A. Personnel Assignment: Assigned role, Analyst name, Assignment date
 * B. Method Selection: Analytical methodology (multi-select)
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function AnalyticsTestAssignmentPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state for assignment
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [isBulkAssignment, setIsBulkAssignment] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form state for assignment
  const [formData, setFormData] = useState({
    assignedRole: "",
    assignedAnalystName: "",
    assignmentDate: "",
    analyticalMethodology: [],
  });

  // Options for dropdowns (hardcoded as per requirements)
  const assignedRoleOptions = [
    { value: "CHEMICAL_ANALYST", label: "Chemical Analyst" },
    { value: "PHARMACIST", label: "Pharmacist" },
    { value: "RESEARCHER", label: "Researcher" },
  ];

  const analyticalMethodologyOptions = [
    { id: "HPLC", label: "HPLC" },
    { id: "UV_VIS", label: "UV-Vis" },
    { id: "LC_MS_MS", label: "LC-MS/MS" },
    { id: "DISSOLUTION_USP", label: "Dissolution (USP I / USP II)" },
    { id: "HARDNESS_TEST", label: "Hardness test" },
    { id: "FRIABILITY_TEST", label: "Friability test" },
    { id: "DISINTEGRATION_TEST", label: "Disintegration test" },
    { id: "IDENTITY_TEST", label: "Identity test" },
  ];

  // Load samples for this page
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
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              status: sample.pageStatus || "PENDING",
              // Assignment-specific fields from data
              assignedRole: sample.data?.assignedRole,
              assignedAnalystName: sample.data?.assignedAnalystName,
              assignmentDate: sample.data?.assignmentDate,
              analyticalMethodology: sample.data?.analyticalMethodology || [],
              // Carry over context from previous page
              requestingUnit: sample.data?.requestingUnit,
              requestedTests: sample.data?.requestedTests,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Handle form input changes
  const handleFormChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Handle multi-select change for methodology
  const handleMethodologyChange = ({ selectedItems }) => {
    setFormData((prev) => ({
      ...prev,
      analyticalMethodology: selectedItems,
    }));
  };

  // Reset form
  const resetForm = () => {
    setFormData({
      assignedRole: "",
      assignedAnalystName: "",
      assignmentDate: "",
      analyticalMethodology: [],
    });
  };

  // Open assignment modal for single or bulk assignment
  const openAssignModal = (isBulk = false) => {
    setIsBulkAssignment(isBulk);
    setAssignModalOpen(true);
  };

  // Submit assignment
  const handleSubmitAssignment = async () => {
    if (!entryId || !pageData?.id) {
      setError("Cannot assign: Page not properly initialized.");
      return;
    }

    // Validate required fields
    if (
      !formData.assignedRole ||
      !formData.assignedAnalystName ||
      !formData.assignmentDate ||
      formData.analyticalMethodology.length === 0
    ) {
      setError("Please fill in all required fields.");
      return;
    }

    if (isBulkAssignment && selectedSampleIds.length === 0) {
      setError("Please select at least one sample for bulk assignment.");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const assignmentData = {
      sampleIds: isBulkAssignment
        ? selectedSampleIds.map((id) => parseInt(id, 10))
        : selectedSampleIds.map((id) => parseInt(id, 10)),
      assignedRole: formData.assignedRole,
      assignedAnalystName: formData.assignedAnalystName,
      assignmentDate: formData.assignmentDate,
      analyticalMethodology: formData.analyticalMethodology.map((m) => m.id),
      status: "ASSIGNED",
    };

    postToOpenElisServer(
      `/rest/notebook/analytics/entry/${entryId}/page/${pageData.id}/samples/assign`,
      JSON.stringify(assignmentData),
      (status, response) => {
        setIsSubmitting(false);
        if (status === 200 || status === 201) {
          setAssignModalOpen(false);
          resetForm();
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error || "Failed to assign samples. Please try again.",
          );
        }
      },
    );
  };

  // Handle bulk mark as assigned
  const handleBulkMarkAssigned = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status.");
        }
      },
    );
  }, [selectedSampleIds, pageData?.id, loadPageSamples, onProgressUpdate]);

  // Calculate stats
  const assignedCount = samples.filter(
    (s) => s.status === "COMPLETED" || s.status === "ASSIGNED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Table headers
  const tableHeaders = [
    { key: "externalId", header: "Sample Identifier" },
    { key: "sampleType", header: "Sample Type" },
    { key: "requestingUnit", header: "Requesting Unit" },
    { key: "assignedRole", header: "Assigned Role" },
    { key: "assignedAnalystName", header: "Analyst" },
    { key: "assignmentDate", header: "Assignment Date" },
    { key: "methodologyDisplay", header: "Methodology" },
    { key: "status", header: "Status" },
  ];

  // Get role label
  const getRoleLabel = (value) => {
    const option = assignedRoleOptions.find((o) => o.value === value);
    return option ? option.label : value;
  };

  // Get methodology labels
  const getMethodologyLabels = (methods) => {
    if (!methods || !Array.isArray(methods)) return "";
    return methods
      .map((m) => {
        const option = analyticalMethodologyOptions.find((o) => o.id === m);
        return option ? option.label : m;
      })
      .join(", ");
  };

  return (
    <div className="analytics-test-assignment-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.analytics.testAssignment.title"
            defaultMessage="Test Assignment & Preparation"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.analytics.testAssignment.description"
            defaultMessage="Assign responsibility and select analytical methods per sample. Select samples and assign analyst role, name, and methodology to proceed to analysis."
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
                  id="notebook.page.analytics.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.analytics.assigned"
                  defaultMessage="Assigned"
                />
              </span>
              <span className="progress-value">{assignedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.analytics.pendingAssignment"
                  defaultMessage="Pending Assignment"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Edit}
              onClick={() => openAssignModal(true)}
            >
              <FormattedMessage
                id="notebook.page.analytics.bulkAssign"
                defaultMessage="Assign Selected ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>

            <Button
              kind="secondary"
              size="sm"
              renderIcon={Checkmark}
              onClick={handleBulkMarkAssigned}
            >
              <FormattedMessage
                id="notebook.page.analytics.markAssigned"
                defaultMessage="Mark as Ready ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
          </>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {/* Samples Table */}
      <div className="sample-grid-container">
        {loading ? (
          <Loading description="Loading samples..." withOverlay={false} />
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="notebook.page.analytics.testAssignment.empty"
                defaultMessage="No samples available for assignment. Samples must be created in the Sample Creation page first."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples.map((sample) => ({
              ...sample,
              assignedRoleDisplay: getRoleLabel(sample.assignedRole),
              methodologyDisplay: getMethodologyLabels(
                sample.analyticalMethodology,
              ),
            }))}
            headers={tableHeaders}
            isSortable
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableSelectAll
                        {...getSelectionProps()}
                        onSelect={() => {
                          if (selectedSampleIds.length === samples.length) {
                            setSelectedSampleIds([]);
                          } else {
                            setSelectedSampleIds(samples.map((s) => s.id));
                          }
                        }}
                        checked={selectedSampleIds.length === samples.length}
                        indeterminate={
                          selectedSampleIds.length > 0 &&
                          selectedSampleIds.length < samples.length
                        }
                      />
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
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        <TableSelectRow
                          {...getSelectionProps({ row })}
                          checked={selectedSampleIds.includes(row.id)}
                          onSelect={() => {
                            if (selectedSampleIds.includes(row.id)) {
                              setSelectedSampleIds(
                                selectedSampleIds.filter((id) => id !== row.id),
                              );
                            } else {
                              setSelectedSampleIds([
                                ...selectedSampleIds,
                                row.id,
                              ]);
                            }
                          }}
                        />
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "status" ? (
                              <Tag
                                type={
                                  cell.value === "COMPLETED" ||
                                  cell.value === "ASSIGNED"
                                    ? "green"
                                    : cell.value === "PENDING"
                                      ? "gray"
                                      : "blue"
                                }
                                size="sm"
                              >
                                {cell.value === "ASSIGNED"
                                  ? "Ready for Analysis"
                                  : cell.value}
                              </Tag>
                            ) : cell.info.header === "assignedRole" ? (
                              getRoleLabel(cell.value) || "-"
                            ) : (
                              cell.value || "-"
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}
      </div>

      {/* Assignment Modal */}
      <Modal
        open={assignModalOpen}
        onRequestClose={() => {
          setAssignModalOpen(false);
          resetForm();
          setError(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.analytics.assignSamples.title",
          defaultMessage: isBulkAssignment
            ? `Assign ${selectedSampleIds.length} Sample(s)`
            : "Assign Sample",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.assign",
          defaultMessage: "Assign",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSubmitAssignment}
        onSecondarySubmit={() => {
          setAssignModalOpen(false);
          resetForm();
          setError(null);
        }}
        size="md"
        primaryButtonDisabled={isSubmitting}
      >
        <Grid fullWidth>
          {/* Section A: Personnel Assignment */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.personnelAssignment"
                defaultMessage="A. Personnel Assignment"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="assignedRole"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.assignedRole",
                defaultMessage: "Assigned role *",
              })}
              value={formData.assignedRole}
              onChange={(e) => handleFormChange("assignedRole", e.target.value)}
            >
              <SelectItem value="" text="Select role..." />
              {assignedRoleOptions.map((option) => (
                <SelectItem
                  key={option.value}
                  value={option.value}
                  text={option.label}
                />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="assignedAnalystName"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.assignedAnalystName",
                defaultMessage: "Assigned analyst name *",
              })}
              placeholder="Enter analyst name"
              value={formData.assignedAnalystName}
              onChange={(e) =>
                handleFormChange("assignedAnalystName", e.target.value)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => {
                if (dates && dates[0]) {
                  const date = dates[0];
                  const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                  handleFormChange("assignmentDate", formatted);
                }
              }}
            >
              <DatePickerInput
                id="assignmentDate"
                labelText={intl.formatMessage({
                  id: "notebook.analytics.field.assignmentDate",
                  defaultMessage: "Assignment date *",
                })}
                placeholder="yyyy-mm-dd"
              />
            </DatePicker>
          </Column>

          {/* Section B: Method Selection */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.methodSelection"
                defaultMessage="B. Method Selection (Sample-Linked)"
              />
            </h5>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <FilterableMultiSelect
              id="analyticalMethodology"
              titleText={intl.formatMessage({
                id: "notebook.analytics.field.analyticalMethodology",
                defaultMessage: "Analytical methodology *",
              })}
              items={analyticalMethodologyOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItems={formData.analyticalMethodology}
              onChange={handleMethodologyChange}
              placeholder="Select analytical methods..."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default AnalyticsTestAssignmentPage;
