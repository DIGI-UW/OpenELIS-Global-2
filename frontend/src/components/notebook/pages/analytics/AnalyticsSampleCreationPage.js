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
  TimePicker,
  RadioButtonGroup,
  RadioButton,
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
import { Add, Checkmark, Printer } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * AnalyticsSampleCreationPage - Page 1 of the Analytics Laboratory workflow.
 * Purpose: Create the analytical sample as the primary entity and register it
 * for bioanalytical or pharmaceutical analysis.
 *
 * Data Points:
 * A. Sample Identity: Sample Identifier, Barcode/QR code, Sample Source
 * B. Sample Category: Bioanalytical, Pharmaceutical analysis
 * C. Sample Type: API, Tablet, Capsule, Suspension, Injection, Cream/ointment, etc.
 * D. Sample Context: Requesting unit, Requested tests (multi-select), Study/project ID
 * E. Storage & Handling: Storage condition, Received date & time, Received by
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function AnalyticsSampleCreationPage({
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

  // Modal state for adding new sample
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form state for new sample
  const [formData, setFormData] = useState({
    sampleIdentifier: "",
    barcodeQR: "",
    sampleSource: "",
    sampleCategory: "",
    sampleType: "",
    requestingUnit: "",
    requestedTests: [],
    studyProjectId: "",
    storageCondition: "",
    receivedDate: "",
    receivedTime: "",
    receivedBy: "",
  });

  // Options for dropdowns (hardcoded as per requirements)
  const sampleSourceOptions = [
    {
      value: "MEDICAL_LAB",
      label: "Medical Laboratory (processed biological sample)",
    },
    { value: "RESEARCHER", label: "Researcher" },
    {
      value: "EXTERNAL_CLIENT",
      label: "External client (outsourced analysis)",
    },
  ];

  const sampleCategoryOptions = [
    { value: "BIOANALYTICAL", label: "Bioanalytical" },
    {
      value: "PHARMACEUTICAL",
      label: "Pharmaceutical analysis (physicochemical / R&D)",
    },
  ];

  const sampleTypeOptions = [
    { value: "API", label: "API" },
    { value: "TABLET", label: "Tablet" },
    { value: "CAPSULE", label: "Capsule" },
    { value: "SUSPENSION", label: "Suspension" },
    { value: "INJECTION", label: "Injection" },
    { value: "CREAM_OINTMENT", label: "Cream / ointment" },
    {
      value: "PROCESSED_BIOLOGICAL",
      label: "Processed biological sample (plasma, serum, urine)",
    },
    { value: "OTHER", label: "Other" },
  ];

  const requestedTestOptions = [
    { id: "ASSAY", label: "Assay" },
    { id: "DISSOLUTION", label: "Dissolution" },
    { id: "DISINTEGRATION", label: "Disintegration" },
    { id: "FRIABILITY", label: "Friability" },
    { id: "HARDNESS", label: "Hardness" },
    { id: "IDENTITY_TEST", label: "Identity test" },
    { id: "BIOANALYTICAL_QUANT", label: "Bioanalytical quantification" },
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
              // Analytics-specific fields from data/questionnaire responses
              sampleSource: sample.data?.sampleSource,
              sampleCategory: sample.data?.sampleCategory,
              requestingUnit: sample.data?.requestingUnit,
              requestedTests: sample.data?.requestedTests,
              studyProjectId: sample.data?.studyProjectId,
              storageCondition: sample.data?.storageCondition,
              receivedDateTime: sample.data?.receivedDateTime,
              receivedBy: sample.data?.receivedBy,
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

  // Handle multi-select change for requested tests
  const handleTestsChange = ({ selectedItems }) => {
    setFormData((prev) => ({
      ...prev,
      requestedTests: selectedItems,
    }));
  };

  // Reset form
  const resetForm = () => {
    setFormData({
      sampleIdentifier: "",
      barcodeQR: "",
      sampleSource: "",
      sampleCategory: "",
      sampleType: "",
      requestingUnit: "",
      requestedTests: [],
      studyProjectId: "",
      storageCondition: "",
      receivedDate: "",
      receivedTime: "",
      receivedBy: "",
    });
  };

  // Submit new sample
  const handleSubmitSample = async () => {
    if (!entryId || !pageData?.id) {
      setError("Cannot create sample: Page not properly initialized.");
      return;
    }

    // Validate required fields
    if (
      !formData.sampleIdentifier ||
      !formData.sampleSource ||
      !formData.sampleCategory ||
      !formData.sampleType ||
      !formData.requestingUnit ||
      formData.requestedTests.length === 0 ||
      !formData.receivedDate ||
      !formData.receivedBy
    ) {
      setError("Please fill in all required fields.");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const receivedDateTime = formData.receivedTime
      ? `${formData.receivedDate} ${formData.receivedTime}`
      : formData.receivedDate;

    const sampleData = {
      externalId: formData.sampleIdentifier,
      barcodeQR: formData.barcodeQR,
      sampleSource: formData.sampleSource,
      sampleCategory: formData.sampleCategory,
      sampleType: formData.sampleType,
      requestingUnit: formData.requestingUnit,
      requestedTests: formData.requestedTests.map((t) => t.id),
      studyProjectId: formData.studyProjectId,
      storageCondition: formData.storageCondition,
      receivedDateTime: receivedDateTime,
      receivedBy: formData.receivedBy,
      pageStatus: "CREATED",
    };

    postToOpenElisServer(
      `/rest/notebook/analytics/entry/${entryId}/page/${pageData.id}/samples/create`,
      JSON.stringify(sampleData),
      (status, response) => {
        setIsSubmitting(false);
        if (status === 200 || status === 201) {
          setAddModalOpen(false);
          resetForm();
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error || "Failed to create sample. Please try again.",
          );
        }
      },
    );
  };

  // Handle bulk mark as created
  const handleBulkMarkCreated = useCallback(() => {
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

  // Print barcode
  const handlePrintBarcode = (accessionNumber) => {
    const barcodesPdf =
      config.serverBaseUrl +
      `/LabelMakerServlet?labNo=${encodeURIComponent(accessionNumber)}&type=order&quantity=1`;
    window.open(barcodesPdf);
  };

  // Calculate stats
  const createdCount = samples.filter(
    (s) => s.status === "COMPLETED" || s.status === "CREATED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Table headers
  const tableHeaders = [
    { key: "externalId", header: "Sample Identifier" },
    { key: "sampleType", header: "Sample Type" },
    { key: "sampleSource", header: "Source" },
    { key: "sampleCategory", header: "Category" },
    { key: "requestingUnit", header: "Requesting Unit" },
    { key: "receivedBy", header: "Received By" },
    { key: "status", header: "Status" },
    { key: "actions", header: "Actions" },
  ];

  // Get source label
  const getSourceLabel = (value) => {
    const option = sampleSourceOptions.find((o) => o.value === value);
    return option ? option.label : value;
  };

  // Get category label
  const getCategoryLabel = (value) => {
    const option = sampleCategoryOptions.find((o) => o.value === value);
    return option ? option.label : value;
  };

  // Get type label
  const getTypeLabel = (value) => {
    const option = sampleTypeOptions.find((o) => o.value === value);
    return option ? option.label : value;
  };

  return (
    <div className="analytics-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.analytics.sampleCreation.title"
            defaultMessage="Sample Creation & Full Metadata Capture"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.analytics.sampleCreation.description"
            defaultMessage="Create the analytical sample as the primary entity and register it for bioanalytical or pharmaceutical analysis. Capture all sample metadata including identity, category, type, context, and storage handling."
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
                  id="notebook.page.analytics.created"
                  defaultMessage="Created"
                />
              </span>
              <span className="progress-value">{createdCount}</span>
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
        <Button
          kind="primary"
          size="sm"
          renderIcon={Add}
          onClick={() => setAddModalOpen(true)}
        >
          <FormattedMessage
            id="notebook.page.analytics.addSample"
            defaultMessage="Add New Sample"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkCreated}
          >
            <FormattedMessage
              id="notebook.page.analytics.markCreated"
              defaultMessage="Mark as Created ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
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
                id="notebook.page.analytics.sampleCreation.empty"
                defaultMessage="No samples have been added yet. Use 'Add New Sample' to create samples for analysis."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples.map((sample) => ({
              ...sample,
              sampleSourceDisplay: getSourceLabel(sample.sampleSource),
              sampleCategoryDisplay: getCategoryLabel(sample.sampleCategory),
              sampleTypeDisplay: getTypeLabel(sample.sampleType),
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
                    {rows.map((row) => {
                      const sample = samples.find((s) => s.id === row.id);
                      return (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          <TableSelectRow
                            {...getSelectionProps({ row })}
                            checked={selectedSampleIds.includes(row.id)}
                            onSelect={() => {
                              if (selectedSampleIds.includes(row.id)) {
                                setSelectedSampleIds(
                                  selectedSampleIds.filter(
                                    (id) => id !== row.id,
                                  ),
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
                                    cell.value === "CREATED"
                                      ? "green"
                                      : cell.value === "PENDING"
                                        ? "gray"
                                        : "blue"
                                  }
                                  size="sm"
                                >
                                  {cell.value}
                                </Tag>
                              ) : cell.info.header === "actions" ? (
                                sample?.accessionNumber && (
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={Printer}
                                    iconDescription="Print Barcode"
                                    hasIconOnly
                                    onClick={() =>
                                      handlePrintBarcode(sample.accessionNumber)
                                    }
                                  />
                                )
                              ) : cell.info.header === "sampleSource" ? (
                                getSourceLabel(cell.value)
                              ) : cell.info.header === "sampleCategory" ? (
                                getCategoryLabel(cell.value)
                              ) : cell.info.header === "sampleType" ? (
                                getTypeLabel(cell.value)
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
            )}
          </DataTable>
        )}
      </div>

      {/* Add Sample Modal */}
      <Modal
        open={addModalOpen}
        onRequestClose={() => {
          setAddModalOpen(false);
          resetForm();
          setError(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.analytics.addSample.title",
          defaultMessage: "Add New Analytical Sample",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.create",
          defaultMessage: "Create Sample",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSubmitSample}
        onSecondarySubmit={() => {
          setAddModalOpen(false);
          resetForm();
          setError(null);
        }}
        size="lg"
        primaryButtonDisabled={isSubmitting}
      >
        <Grid fullWidth>
          {/* Section A: Sample Identity */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.sampleIdentity"
                defaultMessage="A. Sample Identity"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="sampleIdentifier"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.sampleIdentifier",
                defaultMessage: "Sample Identifier *",
              })}
              placeholder="Enter sample ID (retained or newly assigned)"
              value={formData.sampleIdentifier}
              onChange={(e) =>
                handleFormChange("sampleIdentifier", e.target.value)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="barcodeQR"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.barcodeQR",
                defaultMessage: "Barcode / QR code",
              })}
              placeholder="Enter barcode or QR code"
              value={formData.barcodeQR}
              onChange={(e) => handleFormChange("barcodeQR", e.target.value)}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="sampleSource"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.sampleSource",
                defaultMessage: "Sample Source *",
              })}
              value={formData.sampleSource}
              onChange={(e) => handleFormChange("sampleSource", e.target.value)}
            >
              <SelectItem value="" text="Select source..." />
              {sampleSourceOptions.map((option) => (
                <SelectItem
                  key={option.value}
                  value={option.value}
                  text={option.label}
                />
              ))}
            </Select>
          </Column>

          {/* Section B: Sample Category */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.sampleCategory"
                defaultMessage="B. Sample Category"
              />
            </h5>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.analytics.field.sampleCategory",
                defaultMessage: "Sample Category *",
              })}
              name="sampleCategory"
              valueSelected={formData.sampleCategory}
              onChange={(value) => handleFormChange("sampleCategory", value)}
            >
              {sampleCategoryOptions.map((option) => (
                <RadioButton
                  key={option.value}
                  id={option.value}
                  labelText={option.label}
                  value={option.value}
                />
              ))}
            </RadioButtonGroup>
          </Column>

          {/* Section C: Sample Type */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.sampleType"
                defaultMessage="C. Sample Type"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="sampleType"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.sampleType",
                defaultMessage: "Sample Type *",
              })}
              value={formData.sampleType}
              onChange={(e) => handleFormChange("sampleType", e.target.value)}
            >
              <SelectItem value="" text="Select type..." />
              {sampleTypeOptions.map((option) => (
                <SelectItem
                  key={option.value}
                  value={option.value}
                  text={option.label}
                />
              ))}
            </Select>
          </Column>

          {/* Section D: Sample Context & Request */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.sampleContext"
                defaultMessage="D. Sample Context & Request"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="requestingUnit"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.requestingUnit",
                defaultMessage: "Requesting unit / researcher / client *",
              })}
              placeholder="Enter requesting party"
              value={formData.requestingUnit}
              onChange={(e) =>
                handleFormChange("requestingUnit", e.target.value)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="studyProjectId"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.studyProjectId",
                defaultMessage: "Study / project ID",
              })}
              placeholder="Enter study or project ID if applicable"
              value={formData.studyProjectId}
              onChange={(e) =>
                handleFormChange("studyProjectId", e.target.value)
              }
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <FilterableMultiSelect
              id="requestedTests"
              titleText={intl.formatMessage({
                id: "notebook.analytics.field.requestedTests",
                defaultMessage: "Requested tests (multi-select) *",
              })}
              items={requestedTestOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItems={formData.requestedTests}
              onChange={handleTestsChange}
              placeholder="Select requested tests..."
            />
          </Column>

          {/* Section E: Storage & Handling at Receipt */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.storageHandling"
                defaultMessage="E. Storage & Handling at Receipt"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="storageCondition"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.storageCondition",
                defaultMessage: "Storage condition prior to testing",
              })}
              placeholder="e.g., 2-8°C, Room temperature"
              value={formData.storageCondition}
              onChange={(e) =>
                handleFormChange("storageCondition", e.target.value)
              }
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => {
                if (dates && dates[0]) {
                  const date = dates[0];
                  const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                  handleFormChange("receivedDate", formatted);
                }
              }}
            >
              <DatePickerInput
                id="receivedDate"
                labelText={intl.formatMessage({
                  id: "notebook.analytics.field.receivedDate",
                  defaultMessage: "Received date *",
                })}
                placeholder="yyyy-mm-dd"
              />
            </DatePicker>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <TimePicker
              id="receivedTime"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.receivedTime",
                defaultMessage: "Received time",
              })}
              value={formData.receivedTime}
              onChange={(e) => handleFormChange("receivedTime", e.target.value)}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="receivedBy"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.receivedBy",
                defaultMessage: "Received by (name / initials) *",
              })}
              placeholder="Enter receiver name or initials"
              value={formData.receivedBy}
              onChange={(e) => handleFormChange("receivedBy", e.target.value)}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default AnalyticsSampleCreationPage;
