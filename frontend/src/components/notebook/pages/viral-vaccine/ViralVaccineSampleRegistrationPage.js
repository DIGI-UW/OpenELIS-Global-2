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
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  TextInput,
  DatePicker,
  DatePickerInput,
  TimePicker,
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import { Add, Checkmark, TrashCan } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * ViralVaccineSampleRegistrationPage - Page 1 of the Viral & Vaccine Unit workflow.
 * Handles sample creation and registration with the following data points:
 * - Sample Identity: Sample ID, Barcode (Yes/No), Batch association
 * - Sample Metadata: Source, Type, Date & time received, Received by
 * - Testing Intent: Test category (Viral/Bacterial), Intended workflow, Priority/project
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ViralVaccineSampleRegistrationPage({
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

  // State for sample types from type_of_sample table
  const [sampleTypes, setSampleTypes] = useState([]);

  // Modal state for adding new sample
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [newSample, setNewSample] = useState({
    sampleId: "",
    hasBarcode: "Yes",
    batchAssociation: "",
    sampleSource: "",
    sampleType: "",
    receivedDate: "",
    receivedTime: "",
    receivedBy: "",
    testCategory: "",
    intendedWorkflow: "",
    priority: "Routine",
    projectName: "",
  });

  // Load sample types from type_of_sample table
  useEffect(() => {
    getFromOpenElisServer("/rest/user-sample-types", (res) => {
      setSampleTypes(res || []);
    });
  }, []);

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
              receivedDate: sample.receivedDate,
              status: sample.pageStatus || "PENDING",
              // Viral & Vaccine specific fields from questionnaire responses
              sampleSource: sample.data?.sampleSource,
              testCategory: sample.data?.testCategory,
              intendedWorkflow: sample.data?.intendedWorkflow,
              priority: sample.data?.priority,
              projectName: sample.data?.projectName,
              hasBarcode: sample.data?.hasBarcode,
              batchAssociation: sample.data?.batchAssociation,
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

  // Handle input changes for new sample form
  const handleInputChange = (field, value) => {
    setNewSample((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Reset form
  const resetForm = () => {
    setNewSample({
      sampleId: "",
      hasBarcode: "Yes",
      batchAssociation: "",
      sampleSource: "",
      sampleType: "",
      receivedDate: "",
      receivedTime: "",
      receivedBy: "",
      testCategory: "",
      intendedWorkflow: "",
      priority: "Routine",
      projectName: "",
    });
  };

  // Handle sample creation
  const handleCreateSample = async () => {
    if (!entryId || !pageData?.id) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot create sample: Page not properly initialized.");
      return;
    }

    // Validate required fields
    if (
      !newSample.sampleId ||
      !newSample.sampleSource ||
      !newSample.sampleType ||
      !newSample.testCategory ||
      !newSample.intendedWorkflow
    ) {
      setError("Please fill in all required fields.");
      return;
    }

    setIsSubmitting(true);

    const sampleData = {
      externalId: newSample.sampleId,
      sampleTypeId: newSample.sampleType,
      data: {
        sampleSource: newSample.sampleSource,
        testCategory: newSample.testCategory,
        intendedWorkflow: newSample.intendedWorkflow,
        priority: newSample.priority,
        projectName: newSample.projectName,
        hasBarcode: newSample.hasBarcode,
        batchAssociation: newSample.batchAssociation,
        receivedBy: newSample.receivedBy,
        receivedDateTime:
          newSample.receivedDate && newSample.receivedTime
            ? `${newSample.receivedDate} ${newSample.receivedTime}`
            : newSample.receivedDate,
      },
    };

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/entry/${entryId}/page/${pageData.id}/sample`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(sampleData),
        },
      );

      const result = await response.json();

      if (response.ok && result.success) {
        setAddModalOpen(false);
        resetForm();
        loadPageSamples();
        if (onProgressUpdate) {
          onProgressUpdate();
        }
      } else {
        setError(result.error || "Failed to create sample.");
      }
    } catch (err) {
      setError("Failed to create sample: " + err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle bulk mark as registered
  const handleBulkMarkRegistered = useCallback(() => {
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
  const registeredCount = samples.filter(
    (s) => s.status === "COMPLETED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const viralCount = samples.filter(
    (s) => s.testCategory === "Viral",
  ).length;
  const bacterialCount = samples.filter(
    (s) => s.testCategory === "Bacterial",
  ).length;

  return (
    <div className="viral-vaccine-sample-registration-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.viralvaccine.sampleRegistration.title"
            defaultMessage="Sample Creation & Registration"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.viralvaccine.sampleRegistration.description"
            defaultMessage="Create the sample and assign it to the correct testing stream. Capture sample identity, metadata, and testing intent. System creates sample record, sets status to Registered, and routes to appropriate workflow."
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
                  id="notebook.page.viralvaccine.registered"
                  defaultMessage="Registered"
                />
              </span>
              <span className="progress-value">{registeredCount}</span>
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
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.viralvaccine.viral"
                  defaultMessage="Viral"
                />
              </span>
              <span className="progress-value">{viralCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.viralvaccine.bacterial"
                  defaultMessage="Bacterial"
                />
              </span>
              <span className="progress-value">{bacterialCount}</span>
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
            id="notebook.page.viralvaccine.addSample"
            defaultMessage="Add Sample"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkRegistered}
          >
            <FormattedMessage
              id="notebook.page.viralvaccine.markRegistered"
              defaultMessage="Mark as Registered ({count})"
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
            { key: "sampleSource", header: "Source" },
            { key: "testCategory", header: "Test Category" },
            { key: "intendedWorkflow", header: "Workflow" },
            { key: "priority", header: "Priority" },
            { key: "receivedBy", header: "Received By" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.viralvaccine.sampleRegistration.empty"
              defaultMessage="No samples have been registered yet. Click 'Add Sample' to create a new sample."
            />
          </p>
        </div>
      )}

      {/* Add Sample Modal */}
      <Modal
        open={addModalOpen}
        onRequestClose={() => {
          setAddModalOpen(false);
          resetForm();
          setError(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.viralvaccine.addSample.title",
          defaultMessage: "Register New Sample",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleCreateSample}
        onSecondarySubmit={() => {
          setAddModalOpen(false);
          resetForm();
          setError(null);
        }}
        size="lg"
        primaryButtonDisabled={isSubmitting}
      >
        <Grid fullWidth>
          {/* Sample Identity Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "0.5rem" }}>
              <FormattedMessage
                id="notebook.viralvaccine.section.sampleIdentity"
                defaultMessage="Sample Identity"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="sampleId"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.sampleId",
                defaultMessage: "Sample ID *",
              })}
              value={newSample.sampleId}
              onChange={(e) => handleInputChange("sampleId", e.target.value)}
              placeholder="e.g., VV-2024-001"
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Select
              id="hasBarcode"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.barcode",
                defaultMessage: "Barcode *",
              })}
              value={newSample.hasBarcode}
              onChange={(e) => handleInputChange("hasBarcode", e.target.value)}
            >
              <SelectItem value="Yes" text="Yes" />
              <SelectItem value="No" text="No" />
            </Select>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="batchAssociation"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.batch",
                defaultMessage: "Batch Association",
              })}
              value={newSample.batchAssociation}
              onChange={(e) =>
                handleInputChange("batchAssociation", e.target.value)
              }
            />
          </Column>

          {/* Sample Metadata Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.viralvaccine.section.sampleMetadata"
                defaultMessage="Sample Metadata"
              />
            </h5>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Select
              id="sampleSource"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.source",
                defaultMessage: "Sample Source *",
              })}
              value={newSample.sampleSource}
              onChange={(e) =>
                handleInputChange("sampleSource", e.target.value)
              }
            >
              <SelectItem value="" text="Select source..." />
              <SelectItem value="Human" text="Human" />
              <SelectItem value="Animal" text="Animal" />
              <SelectItem value="Environmental" text="Environmental" />
              <SelectItem value="Lab-derived" text="Lab-derived" />
            </Select>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Select
              id="sampleType"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.sampleType",
                defaultMessage: "Sample Type *",
              })}
              value={newSample.sampleType}
              onChange={(e) => handleInputChange("sampleType", e.target.value)}
            >
              <SelectItem value="" text="Select type..." />
              {sampleTypes.map((st) => (
                <SelectItem key={st.id} value={st.id} text={st.value} />
              ))}
            </Select>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              onChange={(dates) => {
                if (dates && dates[0]) {
                  const date = dates[0];
                  const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                  handleInputChange("receivedDate", formatted);
                }
              }}
            >
              <DatePickerInput
                id="receivedDate"
                labelText={intl.formatMessage({
                  id: "notebook.viralvaccine.field.receivedDate",
                  defaultMessage: "Date Received",
                })}
                placeholder="yyyy-mm-dd"
              />
            </DatePicker>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <TimePicker
              id="receivedTime"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.receivedTime",
                defaultMessage: "Time Received",
              })}
              value={newSample.receivedTime}
              onChange={(e) =>
                handleInputChange("receivedTime", e.target.value)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="receivedBy"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.receivedBy",
                defaultMessage: "Received By",
              })}
              value={newSample.receivedBy}
              onChange={(e) => handleInputChange("receivedBy", e.target.value)}
            />
          </Column>

          {/* Testing Intent Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.viralvaccine.section.testingIntent"
                defaultMessage="Testing Intent"
              />
            </h5>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Select
              id="testCategory"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.testCategory",
                defaultMessage: "Test Category *",
              })}
              value={newSample.testCategory}
              onChange={(e) =>
                handleInputChange("testCategory", e.target.value)
              }
            >
              <SelectItem value="" text="Select category..." />
              <SelectItem value="Viral" text="Viral" />
              <SelectItem value="Bacterial" text="Bacterial" />
            </Select>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Select
              id="intendedWorkflow"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.workflow",
                defaultMessage: "Intended Workflow *",
              })}
              value={newSample.intendedWorkflow}
              onChange={(e) =>
                handleInputChange("intendedWorkflow", e.target.value)
              }
            >
              <SelectItem value="" text="Select workflow..." />
              <SelectItem value="Virus culture" text="Virus culture" />
              <SelectItem
                value="Vaccine development"
                text="Vaccine development"
              />
              <SelectItem value="Bacteriology" text="Bacteriology" />
            </Select>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Select
              id="priority"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.priority",
                defaultMessage: "Priority",
              })}
              value={newSample.priority}
              onChange={(e) => handleInputChange("priority", e.target.value)}
            >
              <SelectItem value="Routine" text="Routine" />
              <SelectItem value="Urgent" text="Urgent" />
              <SelectItem value="STAT" text="STAT" />
            </Select>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="projectName"
              labelText={intl.formatMessage({
                id: "notebook.viralvaccine.field.project",
                defaultMessage: "Project Name",
              })}
              value={newSample.projectName}
              onChange={(e) => handleInputChange("projectName", e.target.value)}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default ViralVaccineSampleRegistrationPage;