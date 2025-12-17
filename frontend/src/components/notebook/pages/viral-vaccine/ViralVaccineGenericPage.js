import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  Select,
  SelectItem,
  Tag,
  TextInput,
  DatePicker,
  DatePickerInput,
  NumberInput,
  TextArea,
  Checkbox,
  FileUploaderDropContainer,
  FileUploaderItem,
} from "@carbon/react";
import { Checkmark, Edit } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * ViralVaccineGenericPage - Generic page component for Viral & Vaccine Unit workflow pages 3-13.
 * Renders form fields dynamically based on the page content JSON from the database.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data including content JSON
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ViralVaccineGenericPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state for bulk update
  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({});

  // Parse page content to get data points
  const pageContent = React.useMemo(() => {
    if (pageData?.content) {
      try {
        return typeof pageData.content === "string"
          ? JSON.parse(pageData.content)
          : pageData.content;
      } catch (e) {
        console.error("Failed to parse page content:", e);
        return {};
      }
    }
    return {};
  }, [pageData?.content]);

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
              data: sample.data || {},
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

  // Handle input changes
  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Reset form
  const resetForm = () => {
    setFormData({});
  };

  // Handle bulk update
  const handleBulkUpdate = async () => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    setIsSubmitting(true);

    const updateData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: formData,
      status: "COMPLETED",
    };

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/data`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(updateData),
        },
      );

      if (response.ok) {
        setBulkModalOpen(false);
        resetForm();
        setSelectedSampleIds([]);
        loadPageSamples();
        if (onProgressUpdate) {
          onProgressUpdate();
        }
      } else {
        const result = await response.json();
        setError(result.error || "Failed to update samples.");
      }
    } catch (err) {
      setError("Failed to update samples: " + err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
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

  // Render a form field based on its type
  const renderField = (fieldKey, fieldConfig, sectionKey) => {
    const fullKey = `${sectionKey}.${fieldKey}`;
    const value = formData[fullKey] || "";

    switch (fieldConfig.type) {
      case "text":
        return (
          <TextInput
            id={fullKey}
            labelText={`${fieldConfig.label}${fieldConfig.required ? " *" : ""}`}
            value={value}
            onChange={(e) => handleInputChange(fullKey, e.target.value)}
          />
        );

      case "number":
        return (
          <NumberInput
            id={fullKey}
            label={`${fieldConfig.label}${fieldConfig.required ? " *" : ""}`}
            value={value || 0}
            onChange={(e, { value }) => handleInputChange(fullKey, value)}
            hideSteppers
          />
        );

      case "choice":
        return (
          <Select
            id={fullKey}
            labelText={`${fieldConfig.label}${fieldConfig.required ? " *" : ""}`}
            value={value}
            onChange={(e) => handleInputChange(fullKey, e.target.value)}
          >
            <SelectItem value="" text="Select..." />
            {fieldConfig.options?.map((opt) => (
              <SelectItem key={opt} value={opt} text={opt} />
            ))}
          </Select>
        );

      case "date":
        return (
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            onChange={(dates) => {
              if (dates && dates[0]) {
                const date = dates[0];
                const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                handleInputChange(fullKey, formatted);
              }
            }}
          >
            <DatePickerInput
              id={fullKey}
              labelText={`${fieldConfig.label}${fieldConfig.required ? " *" : ""}`}
              placeholder="yyyy-mm-dd"
            />
          </DatePicker>
        );

      case "dateTime":
        return (
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            onChange={(dates) => {
              if (dates && dates[0]) {
                const date = dates[0];
                const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                handleInputChange(fullKey, formatted);
              }
            }}
          >
            <DatePickerInput
              id={fullKey}
              labelText={`${fieldConfig.label}${fieldConfig.required ? " *" : ""}`}
              placeholder="yyyy-mm-dd"
            />
          </DatePicker>
        );

      case "boolean":
        return (
          <Checkbox
            id={fullKey}
            labelText={fieldConfig.label}
            checked={value === true || value === "true"}
            onChange={(e, { checked }) => handleInputChange(fullKey, checked)}
          />
        );

      case "file":
        return (
          <div>
            <label className="cds--label">
              {fieldConfig.label}
              {fieldConfig.required ? " *" : ""}
            </label>
            <FileUploaderDropContainer
              accept={fieldConfig.accept?.split(",") || ["*"]}
              labelText="Drag and drop or click to upload"
              onAddFiles={(e, { addedFiles }) => {
                handleInputChange(fullKey, addedFiles);
              }}
            />
          </div>
        );

      default:
        return (
          <TextInput
            id={fullKey}
            labelText={`${fieldConfig.label}${fieldConfig.required ? " *" : ""}`}
            value={value}
            onChange={(e) => handleInputChange(fullKey, e.target.value)}
          />
        );
    }
  };

  // Build grid columns from page content
  const renderFormSections = () => {
    if (!pageContent.dataPoints) return null;

    return Object.entries(pageContent.dataPoints).map(
      ([sectionKey, sectionFields]) => (
        <React.Fragment key={sectionKey}>
          <Column lg={16} md={8} sm={4}>
            <h5
              style={{
                marginBottom: "1rem",
                marginTop: "1rem",
                textTransform: "capitalize",
              }}
            >
              {sectionKey.replace(/([A-Z])/g, " $1").trim()}
            </h5>
          </Column>
          {Object.entries(sectionFields).map(([fieldKey, fieldConfig]) => (
            <Column lg={4} md={4} sm={4} key={fieldKey}>
              {renderField(fieldKey, fieldConfig, sectionKey)}
            </Column>
          ))}
        </React.Fragment>
      ),
    );
  };

  // Calculate stats
  const completedCount = samples.filter(
    (s) => s.status === "COMPLETED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Get display columns based on data points
  const getDisplayColumns = () => {
    const baseColumns = [
      { key: "externalId", header: "Sample ID" },
      { key: "sampleType", header: "Sample Type" },
    ];

    // Add first few fields from data points as columns
    if (pageContent.dataPoints) {
      const allFields = [];
      Object.entries(pageContent.dataPoints).forEach(
        ([sectionKey, sectionFields]) => {
          Object.entries(sectionFields).forEach(([fieldKey, fieldConfig]) => {
            if (allFields.length < 4) {
              allFields.push({
                key: `data.${sectionKey}.${fieldKey}`,
                header: fieldConfig.label?.replace(" *", "") || fieldKey,
              });
            }
          });
        },
      );
      baseColumns.push(...allFields);
    }

    baseColumns.push({ key: "status", header: "Status" });
    return baseColumns;
  };

  return (
    <div className="viral-vaccine-generic-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>{pageData?.title || "Workflow Page"}</h4>
        <p className="page-description">
          {pageData?.instructions ||
            pageContent?.pageDescription ||
            "Complete the workflow step for selected samples."}
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">Total Samples</span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">Completed</span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">Pending</span>
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
              onClick={() => setBulkModalOpen(true)}
            >
              <FormattedMessage
                id="notebook.page.viralvaccine.recordData"
                defaultMessage="Record Data ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="secondary"
              size="sm"
              renderIcon={Checkmark}
              onClick={handleBulkMarkCompleted}
            >
              <FormattedMessage
                id="notebook.page.viralvaccine.markCompleted"
                defaultMessage="Mark as Completed ({count})"
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
          hideCloseButton={false}
          lowContrast
          onCloseButtonClick={() => setError(null)}
        />
      )}

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={getDisplayColumns()}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.viralvaccine.generic.empty"
              defaultMessage="No samples available for this step. Samples must progress through previous workflow steps first."
            />
          </p>
        </div>
      )}

      {/* Bulk Update Modal */}
      <Modal
        open={bulkModalOpen}
        onRequestClose={() => {
          setBulkModalOpen(false);
          resetForm();
          setError(null);
        }}
        modalHeading={pageData?.title || "Record Data"}
        primaryButtonText={intl.formatMessage({
          id: "label.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkUpdate}
        onSecondarySubmit={() => {
          setBulkModalOpen(false);
          resetForm();
          setError(null);
        }}
        size="lg"
        primaryButtonDisabled={isSubmitting}
      >
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="notebook.viralvaccine.bulk.applyTo"
            defaultMessage="Applying to {count} selected sample(s)"
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>{renderFormSections()}</Grid>
      </Modal>
    </div>
  );
}

export default ViralVaccineGenericPage;
