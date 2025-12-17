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
import { Add, Checkmark, Print, Upload, TrashCan } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import config from "../../../../config.json";
import SampleGrid from "../../workflow/SampleGrid";
import PathologyManifestImportModal from "../../workflow/PathologyManifestImportModal";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologySampleCreationPage - Page 1 of the pathology workflow.
 * Purpose: Create the pathology sample and capture all metadata at once.
 * Who uses it: Data clerks / reception staff
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
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Sample types from backend
  const [sampleTypes, setSampleTypes] = useState([]);

  // New sample form state
  const [newSample, setNewSample] = useState({
    sampleCategory: "",
    sourceFacility: "",
    receivedDateTime: "",
    receivedBy: "",
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
              sampleCategory: sample.sampleCategory || "Clinical diagnostic",
              sourceFacility: sample.sourceFacility || "Alert Hospital",
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientId: sample.patientId,
              receivedDateTime: sample.receivedDateTime,
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
    if (
      !newSample.sampleCategory ||
      !newSample.sourceFacility ||
      !newSample.specimenType
    ) {
      setError(
        intl.formatMessage({
          id: "pathology.sampleCreation.error.requiredFields",
          defaultMessage:
            "Please fill in all required fields: Sample Category, Source Facility, and Specimen Type",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const sampleData = {
      ...newSample,
      entryId: entryId,
      pageId: pageData?.id,
    };

    postToOpenElisServer(
      `/rest/notebook/pathology/sample/create`,
      JSON.stringify(sampleData),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setCreateModalOpen(false);
          resetNewSampleForm();
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "pathology.sampleCreation.error.createFailed",
              defaultMessage: "Failed to create sample. Please try again.",
            }),
          );
        }
      },
    );
  };

  const resetNewSampleForm = () => {
    setNewSample({
      sampleCategory: "",
      sourceFacility: "",
      receivedDateTime: "",
      receivedBy: "",
      specimenType: "",
      collectionDateTime: "",
      specimenSite: "",
      patientId: "",
      requestingClinician: "",
      clinicalDetails: "",
      studyId: "",
      piName: "",
      participantAnimalId: "",
      ethicalApprovalRef: "",
    });
  };

  // Handle manifest import success
  const handleImportSuccess = useCallback(
    (result) => {
      setImportModalOpen(false);
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    },
    [loadPageSamples, onProgressUpdate],
  );

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
            defaultMessage="Create pathology samples and capture all metadata. Import samples from a manifest file or create them individually. Mark samples as verified when received."
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

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Upload}
          onClick={() => setImportModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.page.sampleCreation.importManifest"
            defaultMessage="Import from Manifest"
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
              renderIcon={Print}
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
        <Grid fullWidth>
          {/* Sample Identity */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.modal.sampleIdentity"
                defaultMessage="Sample Identity"
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

          {/* Sample Source */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.modal.sampleSource"
                defaultMessage="Sample Source"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="sourceFacility"
              name="sourceFacility"
              labelText={intl.formatMessage({
                id: "pathology.field.sourceFacility",
                defaultMessage: "Source Facility *",
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
            </Select>
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
                defaultMessage: "Received By (Staff Name) *",
              })}
              value={newSample.receivedBy}
              onChange={handleInputChange}
            />
          </Column>

          {/* Specimen Type */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.modal.specimenType"
                defaultMessage="Specimen Type & Material"
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
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
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
