import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Modal,
} from "@carbon/react";
import { Add, Checkmark, Printer, Upload, TrashCan } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import config from "../../../../config.json";
import SampleGrid from "../../workflow/SampleGrid";
import PathologyManifestImportModal from "../../workflow/PathologyManifestImportModal";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologySampleCreationPage - Page 1 of the pathology workflow.
 * Purpose: Create the pathology sample and capture all metadata at once.
 * Who uses it: Data clerks / reception staff
 *
 * KEY METADATA FIELDS:
 * Patient Identification:
 *   - firstName: MANDATORY - primary name field for order acceptance
 *   - surname: OPTIONAL - not required for order acceptance
 *   - nationalId: OPTIONAL - not required for order acceptance
 *
 * Clinical Samples (sampleCategory = "Clinical diagnostic"):
 *   - patientId, requestingClinician, clinicalDetails (all required)
 *
 * Research Samples (sampleCategory = "Research"):
 *   - studyId, piName, participantAnimalId, ethicalApprovalRef (all required)
 *
 * All Samples:
 *   - receivedDateTime, receivedBy (required)
 *   - specimenType, specimenSite, collectionDateTime (required)
 *   - sourceFacility (optional)
 */
function PathologySampleCreationPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importManifestType, setImportManifestType] = useState(null); // "clinical" or "research"
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Sample types from backend
  const [sampleTypes, setSampleTypes] = useState([]);

  // New sample form state
  const [newSample, setNewSample] = useState({
    // Patient Identification (First Name MANDATORY, others optional)
    firstName: "",
    surname: "",
    nationalId: "",
    // Sample Category
    sampleCategory: "",
    // Receiving Info
    sourceFacility: "",
    receivedDateTime: "",
    receivedBy: "",
    // Specimen Info
    specimenType: "",
    collectionDateTime: "",
    specimenSite: "",
    // Clinical metadata
    patientId: "",
    requestingClinician: "",
    clinicalDetails: "",
    // Research metadata
    studyId: "",
    piName: "",
    participantAnimalId: "",
    ethicalApprovalRef: "",
  });

  // Load samples and sample types
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadSampleTypes();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadSampleTypes = useCallback(() => {
    getFromOpenElisServer("/rest/user-sample-types", (response) => {
      if (componentMounted.current && response) {
        setSampleTypes(response);
      }
    });
  }, []);

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
              externalId: sample.externalId || sample.accessionNumber,
              accessionNumber: sample.accessionNumber,
              // Patient Identification
              firstName: sample.firstName,
              surname: sample.surname,
              nationalId: sample.nationalId,
              // Sample Category
              sampleCategory: sample.sampleCategory || "Clinical diagnostic",
              // Receiving Info
              sourceFacility: sample.sourceFacility || "Alert Hospital",
              receivedDateTime: sample.receivedDateTime,
              receivedBy: sample.receivedBy,
              // Specimen Info
              sampleType:
                sample.sampleType ||
                sample.specimenType ||
                sample.typeOfSample?.description,
              specimenSite: sample.specimenSite,
              collectionDate:
                sample.collectionDate || sample.collectionDateTime,
              // Clinical metadata
              patientId: sample.patientId,
              requestingClinician: sample.requestingClinician,
              clinicalDetails: sample.clinicalDetails,
              // Research metadata
              studyId: sample.studyId,
              piName: sample.piName,
              participantAnimalId: sample.participantAnimalId,
              ethicalApprovalRef: sample.ethicalApprovalRef,
              // Status
              status: sample.pageStatus || "PENDING",
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

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setNewSample((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleDateChange = (dates, fieldName) => {
    if (dates && dates.length > 0) {
      const date = dates[0];
      const formattedDate = date.toISOString();
      setNewSample((prev) => ({
        ...prev,
        [fieldName]: formattedDate,
      }));
    }
  };

  const handleCreateSample = () => {
    if (submitting) return;

    // Validate required fields
    // First Name is MANDATORY (primary name field for order acceptance)
    // Surname and National ID are OPTIONAL
    if (
      !newSample.firstName ||
      !newSample.sampleCategory ||
      !newSample.specimenType ||
      !newSample.receivedDateTime ||
      !newSample.receivedBy
    ) {
      setError(
        intl.formatMessage({
          id: "pathology.sampleCreation.error.requiredFields",
          defaultMessage:
            "Please fill in all required fields: First Name (MANDATORY), Sample Category, Specimen Type, Received Date/Time, and Receiving Staff Name. Note: Surname and National ID are optional.",
        }),
      );
      return;
    }

    // Validate date order: Collection Date must be before or equal to Received Date
    if (newSample.collectionDateTime && newSample.receivedDateTime) {
      const collectionDate = new Date(newSample.collectionDateTime);
      const receivedDate = new Date(newSample.receivedDateTime);

      if (collectionDate > receivedDate) {
        setError(
          intl.formatMessage({
            id: "pathology.sampleCreation.error.dateOrder",
            defaultMessage:
              "Collection Date cannot be after Received Date. A sample must be collected before it can be received at the laboratory.",
          }),
        );
        return;
      }
    }

    setSubmitting(true);
    setError(null);

    const sampleData = {
      ...newSample,
      entryId: entryId,
      pageId: pageData?.id,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/pathology/sample/create`,
      JSON.stringify(sampleData),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setCreateModalOpen(false);
          resetNewSampleForm();
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          // Extract error message from server response
          const errorMessage =
            response?.error ||
            response?.message ||
            intl.formatMessage({
              id: "pathology.sampleCreation.error.createFailed",
              defaultMessage: "Failed to create sample. Please try again.",
            });
          setError(errorMessage);
        }
      },
    );
  };

  const resetNewSampleForm = () => {
    setNewSample({
      // Patient Identification (First Name MANDATORY, others optional)
      firstName: "",
      surname: "",
      nationalId: "",
      // Sample Category
      sampleCategory: "",
      // Receiving Info
      sourceFacility: "",
      receivedDateTime: "",
      receivedBy: "",
      // Specimen Info
      specimenType: "",
      collectionDateTime: "",
      specimenSite: "",
      // Clinical metadata
      patientId: "",
      requestingClinician: "",
      clinicalDetails: "",
      // Research metadata
      studyId: "",
      piName: "",
      participantAnimalId: "",
      ethicalApprovalRef: "",
    });
  };

  // Handle manifest import success
  const handleImportSuccess = useCallback(() => {
    setImportModalOpen(false);
    setImportManifestType(null);
    loadPageSamples();
    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [loadPageSamples, onProgressUpdate]);

  // Open import modal for specific manifest type
  const openImportModal = useCallback((manifestType) => {
    setImportManifestType(manifestType);
    setImportModalOpen(true);
  }, []);

  // Check if page has a real database ID (not a default synthetic ID)
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Handle bulk status change - mark as verified
  const handleBulkMarkVerified = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        "Cannot verify samples: Page not properly initialized. Please refresh the page.",
      );
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
          setError("Failed to update sample status. Please try again.");
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle individual sample status change from SampleGrid
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          "Cannot update status: Page not properly initialized. Please refresh the page.",
        );
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status. Please try again.");
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate],
  );

  // Handle bulk delete of selected samples
  const handleBulkDelete = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        "Cannot delete samples: Page not properly initialized. Please refresh the page.",
      );
      return;
    }

    setDeleting(true);

    postToOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples/delete`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      }),
      (status) => {
        setDeleting(false);
        setDeleteConfirmOpen(false);
        if (status === 200) {
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to delete samples. Please try again.");
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle print labels for selected samples
  const handlePrintLabels = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    // Get the selected samples to print labels for
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );

    // For each selected sample, open a print label window
    selectedSamples.forEach((sample) => {
      // Use the accession number or external ID for the label
      const labelCode =
        sample.accessionNumber || sample.externalId || sample.id;
      // Open the LabelMakerServlet endpoint for pathology block/slide labels
      const printUrl = `${config.serverBaseUrl}/LabelMakerServlet?labelType=block&code=${encodeURIComponent(labelCode)}`;
      window.open(printUrl, "_blank", "width=800,height=600");
    });
  }, [selectedSampleIds, samples]);

  // Calculate stats
  const verifiedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  return (
    <div className="pathology-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.sampleCreation.title"
            defaultMessage="Sample Creation & Metadata Capture"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.sampleCreation.description"
            defaultMessage="Create pathology samples and capture all metadata. First Name is MANDATORY for order acceptance (Surname and National ID are optional). Clinical samples require Patient ID, Requesting Clinician, and Clinical Details. Research samples require Study ID, PI Name, Participant/Animal ID, and Ethical Approval Reference. All samples require Receiving Date/Time and Receiving Staff Name."
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
                  id="pathology.page.sampleCreation.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.sampleCreation.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{verifiedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.sampleCreation.pending"
                  defaultMessage="Pending Verification"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Metadata Requirements Info */}
      <div
        style={{
          padding: "1rem",
          marginBottom: "1rem",
          backgroundColor: "#e0f0ff",
          borderRadius: "4px",
          border: "1px solid #0f62fe",
        }}
      >
        <h5 style={{ marginBottom: "0.5rem", color: "#0f62fe" }}>
          <FormattedMessage
            id="pathology.page.sampleCreation.metadataRequirements"
            defaultMessage="Metadata Requirements for Sample Import/Creation"
          />
        </h5>
        <div style={{ fontSize: "0.85rem", color: "#525252" }}>
          <p style={{ marginBottom: "0.5rem" }}>
            <strong>
              <FormattedMessage
                id="pathology.page.sampleCreation.patientIdNote"
                defaultMessage="Patient Identification:"
              />
            </strong>{" "}
            <FormattedMessage
              id="pathology.page.sampleCreation.patientIdDetails"
              defaultMessage="First Name is MANDATORY. Surname/Last Name and National ID are OPTIONAL."
            />
          </p>
          <p style={{ marginBottom: "0.5rem" }}>
            <strong>
              <FormattedMessage
                id="pathology.page.sampleCreation.clinicalNote"
                defaultMessage="Clinical Samples:"
              />
            </strong>{" "}
            <FormattedMessage
              id="pathology.page.sampleCreation.clinicalDetails"
              defaultMessage="Require Patient ID, Requesting Clinician, and Clinical Details/Indication."
            />
          </p>
          <p style={{ marginBottom: "0.5rem" }}>
            <strong>
              <FormattedMessage
                id="pathology.page.sampleCreation.researchNote"
                defaultMessage="Research Samples:"
              />
            </strong>{" "}
            <FormattedMessage
              id="pathology.page.sampleCreation.researchDetails"
              defaultMessage="Require Study ID, PI Name, Participant/Animal ID, and Ethical Approval Reference."
            />
          </p>
          <p>
            <strong>
              <FormattedMessage
                id="pathology.page.sampleCreation.allSamplesNote"
                defaultMessage="All Samples:"
              />
            </strong>{" "}
            <FormattedMessage
              id="pathology.page.sampleCreation.allSamplesDetails"
              defaultMessage="Require Receiving Date/Time, Receiving Staff Name, Specimen Type, Specimen Site, and Collection Date/Time."
            />
          </p>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Upload}
          onClick={() => openImportModal("clinical")}
        >
          <FormattedMessage
            id="pathology.page.sampleCreation.importClinicalManifest"
            defaultMessage="Import Clinical Manifest"
          />
        </Button>

        <Button
          kind="primary"
          size="sm"
          renderIcon={Upload}
          onClick={() => openImportModal("research")}
        >
          <FormattedMessage
            id="pathology.page.sampleCreation.importResearchManifest"
            defaultMessage="Import Research Manifest"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Add}
          onClick={() => setCreateModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.page.sampleCreation.createSample"
            defaultMessage="Create Sample"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="secondary"
              size="sm"
              renderIcon={Checkmark}
              onClick={handleBulkMarkVerified}
            >
              <FormattedMessage
                id="pathology.page.sampleCreation.markVerified"
                defaultMessage="Mark Selected as Verified ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Printer}
              onClick={handlePrintLabels}
            >
              <FormattedMessage
                id="pathology.page.sampleCreation.printLabels"
                defaultMessage="Print Labels"
              />
            </Button>
            <Button
              kind="danger--ghost"
              size="sm"
              renderIcon={TrashCan}
              onClick={() => setDeleteConfirmOpen(true)}
            >
              <FormattedMessage
                id="pathology.page.sampleCreation.deleteSelected"
                defaultMessage="Delete Selected ({count})"
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

      {/* Sample Grid with pagination - default page size 10 */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="pathology-samples"
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
        />
      </div>

      {/* Create Sample Modal */}
      <Modal
        open={createModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.modal.createSample.title",
          defaultMessage: "Create Pathology Sample",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.button.create",
          defaultMessage: "Create",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setCreateModalOpen(false);
          resetNewSampleForm();
          setError(null);
        }}
        onRequestSubmit={handleCreateSample}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        {/* Error Display inside Modal */}
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "pathology.sampleCreation.error.title",
              defaultMessage: "Error",
            })}
            subtitle={error}
            onCloseButtonClick={() => setError(null)}
            lowContrast
            style={{ marginBottom: "1rem" }}
          />
        )}

        {/* Loading indicator */}
        {submitting && (
          <div
            style={{
              padding: "1rem",
              marginBottom: "1rem",
              backgroundColor: "#e0f0ff",
              borderRadius: "4px",
              textAlign: "center",
            }}
          >
            <FormattedMessage
              id="pathology.sampleCreation.creating"
              defaultMessage="Creating sample... Please wait."
            />
          </div>
        )}

        <Grid fullWidth>
          {/* Patient Identification */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.modal.patientIdentification"
                defaultMessage="Patient Identification"
              />
            </h5>
            <p
              style={{
                fontSize: "0.85rem",
                color: "#525252",
                marginBottom: "1rem",
              }}
            >
              <FormattedMessage
                id="pathology.modal.patientIdentification.note"
                defaultMessage="First Name is MANDATORY. Surname and National ID are optional."
              />
            </p>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="firstName"
              name="firstName"
              labelText={intl.formatMessage({
                id: "pathology.field.firstName",
                defaultMessage: "First Name (MANDATORY) *",
              })}
              value={newSample.firstName}
              onChange={handleInputChange}
              required
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="surname"
              name="surname"
              labelText={intl.formatMessage({
                id: "pathology.field.surname",
                defaultMessage: "Surname/Last Name (Optional)",
              })}
              value={newSample.surname}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="nationalId"
              name="nationalId"
              labelText={intl.formatMessage({
                id: "pathology.field.nationalId",
                defaultMessage: "National ID (Optional)",
              })}
              value={newSample.nationalId}
              onChange={handleInputChange}
            />
          </Column>

          {/* Sample Identity */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.modal.sampleIdentity"
                defaultMessage="Sample Category"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="sampleCategory"
              name="sampleCategory"
              labelText={intl.formatMessage({
                id: "pathology.field.sampleCategory",
                defaultMessage: "Sample Category *",
              })}
              value={newSample.sampleCategory}
              onChange={handleInputChange}
            >
              <SelectItem value="" text="" />
              <SelectItem
                value="Clinical diagnostic"
                text={intl.formatMessage({
                  id: "pathology.category.clinical",
                  defaultMessage: "Clinical diagnostic",
                })}
              />
              <SelectItem
                value="Research"
                text={intl.formatMessage({
                  id: "pathology.category.research",
                  defaultMessage: "Research",
                })}
              />
            </Select>
          </Column>

          {/* Specimen Collection Information */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.modal.specimenCollection"
                defaultMessage="Specimen Collection Information"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="specimenType"
              name="specimenType"
              labelText={intl.formatMessage({
                id: "pathology.field.specimenType",
                defaultMessage: "Specimen Type *",
              })}
              value={newSample.specimenType}
              onChange={handleInputChange}
            >
              <SelectItem value="" text="" />
              {sampleTypes.map((type) => (
                <SelectItem key={type.id} value={type.id} text={type.value} />
              ))}
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="specimenSite"
              name="specimenSite"
              labelText={intl.formatMessage({
                id: "pathology.field.specimenSite",
                defaultMessage: "Specimen Site",
              })}
              value={newSample.specimenSite}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "collectionDateTime")
              }
            >
              <DatePickerInput
                id="collectionDateTime"
                labelText={intl.formatMessage({
                  id: "pathology.field.collectionDateTime",
                  defaultMessage: "Collection Date & Time *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="sourceFacility"
              name="sourceFacility"
              labelText={intl.formatMessage({
                id: "pathology.field.sourceFacility",
                defaultMessage: "Source Facility",
              })}
              value={newSample.sourceFacility}
              onChange={handleInputChange}
            >
              <SelectItem value="" text="" />
              <SelectItem
                value="Alert Hospital"
                text={intl.formatMessage({
                  id: "pathology.source.alertHospital",
                  defaultMessage: "Alert Hospital",
                })}
              />
              <SelectItem
                value="Research project"
                text={intl.formatMessage({
                  id: "pathology.source.researchProject",
                  defaultMessage: "Research project",
                })}
              />
              <SelectItem
                value="External clinic"
                text={intl.formatMessage({
                  id: "pathology.source.externalClinic",
                  defaultMessage: "External clinic",
                })}
              />
              <SelectItem
                value="Other"
                text={intl.formatMessage({
                  id: "pathology.source.other",
                  defaultMessage: "Other",
                })}
              />
            </Select>
          </Column>

          {/* Lab Reception Information */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.modal.receivingInfo"
                defaultMessage="Lab Reception Information"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => handleDateChange(dates, "receivedDateTime")}
            >
              <DatePickerInput
                id="receivedDateTime"
                labelText={intl.formatMessage({
                  id: "pathology.field.receivedDateTime",
                  defaultMessage: "Received Date & Time *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="receivedBy"
              name="receivedBy"
              labelText={intl.formatMessage({
                id: "pathology.field.receivedBy",
                defaultMessage: "Receiving Staff Name *",
              })}
              value={newSample.receivedBy}
              onChange={handleInputChange}
            />
          </Column>

          {/* Clinical Metadata (conditional) */}
          {newSample.sampleCategory === "Clinical diagnostic" && (
            <>
              <Column lg={16} md={8} sm={4}>
                <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="pathology.modal.clinicalMetadata"
                    defaultMessage="Clinical Metadata"
                  />
                </h5>
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="patientId"
                  name="patientId"
                  labelText={intl.formatMessage({
                    id: "pathology.field.patientId",
                    defaultMessage: "Patient ID",
                  })}
                  value={newSample.patientId}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="requestingClinician"
                  name="requestingClinician"
                  labelText={intl.formatMessage({
                    id: "pathology.field.requestingClinician",
                    defaultMessage: "Requesting Clinician",
                  })}
                  value={newSample.requestingClinician}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="clinicalDetails"
                  name="clinicalDetails"
                  labelText={intl.formatMessage({
                    id: "pathology.field.clinicalDetails",
                    defaultMessage: "Clinical Details",
                  })}
                  value={newSample.clinicalDetails}
                  onChange={handleInputChange}
                  rows={3}
                />
              </Column>
            </>
          )}

          {/* Research Metadata (conditional) */}
          {newSample.sampleCategory === "Research" && (
            <>
              <Column lg={16} md={8} sm={4}>
                <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="pathology.modal.researchMetadata"
                    defaultMessage="Research Metadata"
                  />
                </h5>
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="studyId"
                  name="studyId"
                  labelText={intl.formatMessage({
                    id: "pathology.field.studyId",
                    defaultMessage: "Study ID",
                  })}
                  value={newSample.studyId}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="piName"
                  name="piName"
                  labelText={intl.formatMessage({
                    id: "pathology.field.piName",
                    defaultMessage: "PI Name",
                  })}
                  value={newSample.piName}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="participantAnimalId"
                  name="participantAnimalId"
                  labelText={intl.formatMessage({
                    id: "pathology.field.participantAnimalId",
                    defaultMessage: "Participant/Animal ID",
                  })}
                  value={newSample.participantAnimalId}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="ethicalApprovalRef"
                  name="ethicalApprovalRef"
                  labelText={intl.formatMessage({
                    id: "pathology.field.ethicalApprovalRef",
                    defaultMessage: "Ethical Approval Reference",
                  })}
                  value={newSample.ethicalApprovalRef}
                  onChange={handleInputChange}
                />
              </Column>
            </>
          )}
        </Grid>
      </Modal>

      {/* Pathology Manifest Import Modal */}
      <PathologyManifestImportModal
        open={importModalOpen}
        onClose={() => {
          setImportModalOpen(false);
          setImportManifestType(null);
        }}
        entryId={entryId}
        manifestType={importManifestType}
        onImportSuccess={handleImportSuccess}
      />

      {/* Delete Confirmation Modal */}
      <Modal
        open={deleteConfirmOpen}
        danger
        modalHeading={intl.formatMessage({
          id: "pathology.modal.deleteConfirm.title",
          defaultMessage: "Delete Selected Samples",
        })}
        primaryButtonText={
          deleting
            ? intl.formatMessage({
                id: "label.button.deleting",
                defaultMessage: "Deleting...",
              })
            : intl.formatMessage({
                id: "label.button.delete",
                defaultMessage: "Delete",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setDeleteConfirmOpen(false)}
        onRequestSubmit={handleBulkDelete}
        primaryButtonDisabled={deleting}
        size="sm"
      >
        <p>
          <FormattedMessage
            id="pathology.modal.deleteConfirm.message"
            defaultMessage="Are you sure you want to delete {count} selected sample(s)? This action cannot be undone."
            values={{ count: selectedSampleIds.length }}
          />
        </p>
      </Modal>
    </div>
  );
}

export default PathologySampleCreationPage;
