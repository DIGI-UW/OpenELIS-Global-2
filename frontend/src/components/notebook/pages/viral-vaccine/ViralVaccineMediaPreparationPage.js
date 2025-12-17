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
  FilterableMultiSelect,
  Checkbox,
} from "@carbon/react";
import { Add, Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * ViralVaccineMediaPreparationPage - Page 2 of the Viral & Vaccine Unit workflow.
 * Handles media preparation for virus culture with the following data points:
 * - Media type
 * - Reagents selected
 * - Equipment used
 * - Prepared by
 * - Preparation date
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ViralVaccineMediaPreparationPage({
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
  const [mediaData, setMediaData] = useState({
    mediaType: "",
    mediaBatchNumber: "",
    reagents: [],
    reagentLotNumbers: "",
    equipmentUsed: [],
    preparedBy: "",
    preparationDate: "",
    expirationDate: "",
    storageConditions: "",
    sterilityCheck: "Pending",
    phValue: "",
    notes: "",
  });

  // Equipment options
  const equipmentOptions = [
    { id: "biosafety-cabinet", label: "Biosafety cabinet" },
    { id: "autoclave", label: "Autoclave" },
    { id: "incubator", label: "Incubator" },
    { id: "water-bath", label: "Water bath" },
    { id: "ph-meter", label: "pH meter" },
    { id: "balance", label: "Balance" },
    { id: "magnetic-stirrer", label: "Magnetic stirrer" },
  ];

  // Media type options
  const mediaTypes = [
    "Cell culture media",
    "Transport media",
    "Enrichment media",
    "Selective media",
    "Maintenance media",
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
              // Media preparation specific fields
              mediaType: sample.data?.mediaType,
              mediaBatchNumber: sample.data?.mediaBatchNumber,
              preparedBy: sample.data?.preparedBy,
              preparationDate: sample.data?.preparationDate,
              sterilityCheck: sample.data?.sterilityCheck,
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

  // Handle input changes for media data form
  const handleInputChange = (field, value) => {
    setMediaData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Reset form
  const resetForm = () => {
    setMediaData({
      mediaType: "",
      mediaBatchNumber: "",
      reagents: [],
      reagentLotNumbers: "",
      equipmentUsed: [],
      preparedBy: "",
      preparationDate: "",
      expirationDate: "",
      storageConditions: "",
      sterilityCheck: "Pending",
      phValue: "",
      notes: "",
    });
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

    if (!mediaData.mediaType || !mediaData.preparedBy) {
      setError("Please fill in required fields (Media Type, Prepared By).");
      return;
    }

    setIsSubmitting(true);

    const updateData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: {
        mediaType: mediaData.mediaType,
        mediaBatchNumber: mediaData.mediaBatchNumber,
        reagents: mediaData.reagents.map((r) => r.label || r).join(", "),
        reagentLotNumbers: mediaData.reagentLotNumbers,
        equipmentUsed: mediaData.equipmentUsed
          .map((e) => e.label || e)
          .join(", "),
        preparedBy: mediaData.preparedBy,
        preparationDate: mediaData.preparationDate,
        expirationDate: mediaData.expirationDate,
        storageConditions: mediaData.storageConditions,
        sterilityCheck: mediaData.sterilityCheck,
        phValue: mediaData.phValue,
        notes: mediaData.notes,
      },
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

  // Calculate stats
  const completedCount = samples.filter(
    (s) => s.status === "COMPLETED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  return (
    <div className="viral-vaccine-media-preparation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.viralvaccine.mediaPreparation.title"
            defaultMessage="Media Preparation"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.viralvaccine.mediaPreparation.description"
            defaultMessage="Prepare culture media for virus growth linked to the sample. Record media type, reagents, equipment, and preparation details. System logs materials and links media batch to sample."
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
                  id="notebook.page.viralvaccine.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.viralvaccine.mediaPrepared"
                  defaultMessage="Media Prepared"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.viralvaccine.pending"
                  defaultMessage="Pending"
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
              renderIcon={Add}
              onClick={() => setBulkModalOpen(true)}
            >
              <FormattedMessage
                id="notebook.page.viralvaccine.recordMediaPrep"
                defaultMessage="Record Media Preparation ({count})"
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
          columns={[
            { key: "externalId", header: "Sample ID" },
            { key: "sampleType", header: "Sample Type" },
            { key: "mediaType", header: "Media Type" },
            { key: "mediaBatchNumber", header: "Batch #" },
            { key: "preparedBy", header: "Prepared By" },
            { key: "preparationDate", header: "Prep Date" },
            { key: "sterilityCheck", header: "Sterility" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.viralvaccine.mediaPreparation.empty"
              defaultMessage="No samples available for media preparation. Samples must be registered in the Sample Registration page first."
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
        modalHeading={intl.formatMessage({
          id: "notebook.viralvaccine.mediaPrep.title",
          defaultMessage: "Record Media Preparation",
        })}
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
            id="notebook.viralvaccine.mediaPrep.applyTo"
            defaultMessage="Applying to {count} selected sample(s)"
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Media Details Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.viralvaccine.section.mediaDetails"
                defaultMessage="Media Details"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="mediaType"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.mediaType",
                defaultMessage: "Media Type *",
              })}
              value={mediaData.mediaType}
              onChange={(e) => handleInputChange("mediaType", e.target.value)}
            >
              <SelectItem value="" text="Select media type..." />
              {mediaTypes.map((type) => (
                <SelectItem key={type} value={type} text={type} />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="mediaBatchNumber"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.batchNumber",
                defaultMessage: "Media Batch Number",
              })}
              value={mediaData.mediaBatchNumber}
              onChange={(e) =>
                handleInputChange("mediaBatchNumber", e.target.value)
              }
            />
          </Column>

          {/* Equipment Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.viralvaccine.section.equipment"
                defaultMessage="Equipment & Reagents"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <FilterableMultiSelect
              id="equipmentUsed"
              titleText={intl.formatMessage({
                id: "notebook.viralvaccine.field.equipment",
                defaultMessage: "Equipment Used",
              })}
              items={equipmentOptions}
              itemToString={(item) => (item ? item.label : "")}
              onChange={({ selectedItems }) =>
                handleInputChange("equipmentUsed", selectedItems)
              }
              selectionFeedback="top-after-reopen"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="reagentLotNumbers"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.reagentLots",
                defaultMessage: "Reagent Lot Numbers",
              })}
              value={mediaData.reagentLotNumbers}
              onChange={(e) =>
                handleInputChange("reagentLotNumbers", e.target.value)
              }
            />
          </Column>

          {/* Preparation Metadata Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.viralvaccine.section.preparation"
                defaultMessage="Preparation Details"
              />
            </h5>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="preparedBy"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.preparedBy",
                defaultMessage: "Prepared By *",
              })}
              value={mediaData.preparedBy}
              onChange={(e) => handleInputChange("preparedBy", e.target.value)}
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              onChange={(dates) => {
                if (dates && dates[0]) {
                  const date = dates[0];
                  const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                  handleInputChange("preparationDate", formatted);
                }
              }}
            >
              <DatePickerInput
                id="preparationDate"
                labelText={intl.formatMessage({
                  id: "notebook.viralvaccine.field.prepDate",
                  defaultMessage: "Preparation Date",
                })}
                placeholder="yyyy-mm-dd"
              />
            </DatePicker>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              onChange={(dates) => {
                if (dates && dates[0]) {
                  const date = dates[0];
                  const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                  handleInputChange("expirationDate", formatted);
                }
              }}
            >
              <DatePickerInput
                id="expirationDate"
                labelText={intl.formatMessage({
                  id: "notebook.viralvaccine.field.expDate",
                  defaultMessage: "Expiration Date",
                })}
                placeholder="yyyy-mm-dd"
              />
            </DatePicker>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Select
              id="storageConditions"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.storage",
                defaultMessage: "Storage Conditions",
              })}
              value={mediaData.storageConditions}
              onChange={(e) =>
                handleInputChange("storageConditions", e.target.value)
              }
            >
              <SelectItem value="" text="Select..." />
              <SelectItem value="Room temperature" text="Room temperature" />
              <SelectItem value="2-8C" text="2-8C" />
              <SelectItem value="-20C" text="-20C" />
              <SelectItem value="-80C" text="-80C" />
            </Select>
          </Column>

          {/* QC Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.viralvaccine.section.qc"
                defaultMessage="Quality Control"
              />
            </h5>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Select
              id="sterilityCheck"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.sterility",
                defaultMessage: "Sterility Check",
              })}
              value={mediaData.sterilityCheck}
              onChange={(e) =>
                handleInputChange("sterilityCheck", e.target.value)
              }
            >
              <SelectItem value="Pending" text="Pending" />
              <SelectItem value="Passed" text="Passed" />
              <SelectItem value="Failed" text="Failed" />
            </Select>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="phValue"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.ph",
                defaultMessage: "pH Value",
              })}
              value={mediaData.phValue}
              onChange={(e) => handleInputChange("phValue", e.target.value)}
              type="number"
              step="0.1"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="notes"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.notes",
                defaultMessage: "Notes",
              })}
              value={mediaData.notes}
              onChange={(e) => handleInputChange("notes", e.target.value)}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default ViralVaccineMediaPreparationPage;
