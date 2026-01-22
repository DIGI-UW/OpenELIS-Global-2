import React, { useState, useEffect, useRef, useCallback, useMemo, useContext } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  Dropdown,
  TextInput,
  TextArea,
  Loading,
} from "@carbon/react";
import {
  CloudUpload,
  Checkmark,
  Renew,
  Chemistry,
  CheckmarkFilled,
  Pending,
  Edit,
  WarningAltFilled,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { useMemo as useMemoHook } from "react";
import NotificationContext, { NotificationKinds } from "../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineAuthenticationStoragePage - Page 2 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGES 2-3:
 * 1. Authentication (Stage 2) - Verify botanical identification and authentication results
 * 2. Storage & Herbarium Placement (Stage 3) - Manage physical storage and herbarium cataloging
 *
 * This page displays authenticated samples from Page 1 and provides:
 * - Authentication review and confirmation
 * - Storage location assignment (Fresh/Dried/Preserved)
 * - Herbarium specimen placement and cataloging
 * - Link to projects and ongoing research
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TraditionalMedicineAuthenticationStoragePage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } = useContext(NotificationContext);
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  // Storage modal state
  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [isApplyingStorage, setIsApplyingStorage] = useState(false);

  // Storage form fields
  const [storageCondition, setStorageCondition] = useState(null);
  const [storageLocation, setStorageLocation] = useState("");
  const [herbariumSpecimenId, setHerbariumSpecimenId] = useState("");
  const [herbariumNotes, setHerbariumNotes] = useState("");
  const [linkedProject, setLinkedProject] = useState("");
  const [storageNotes, setStorageNotes] = useState("");

  // Storage condition options (per SRS)
  const storageConditionOptions = [
    { id: "fresh", label: "Fresh Sample (Refrigerated 2-8°C)" },
    {
      id: "dried",
      label: "Dried Sample (Room Temperature, Sealed Container)",
    },
    { id: "preserved", label: "Preserved (In Fixative/Alcohol)" },
  ];

  // Storage location hierarchy (example - would come from backend in production)
  const storageLocationOptions = [
    { id: "herbarium_a", label: "Herbarium Cabinet A" },
    { id: "herbarium_b", label: "Herbarium Cabinet B" },
    { id: "cold_storage_1", label: "Cold Storage Unit 1 (4°C)" },
    { id: "cold_storage_2", label: "Cold Storage Unit 2 (-20°C)" },
    { id: "desiccant_cabinet", label: "Desiccant-Controlled Cabinet" },
  ];

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  // Check if page has a real ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples for this page
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
              status: sample.pageStatus || sample.status || "PENDING",
              // Traditional medicine specific fields
              sampleCategory: sample.data?.sampleCategory,
              localName: sample.data?.localName,
              scientificName: sample.data?.scientificName,
              species: sample.data?.species,
              // Authentication data from Page 1
              authenticationMethod: sample.data?.authenticationMethod,
              authenticationMethodLabel:
                sample.data?.authenticationMethodLabel,
              authenticationResult: sample.data?.authenticationResult,
              authenticationResultLabel:
                sample.data?.authenticationResultLabel,
              verifiedBy: sample.data?.verifiedBy,
              verificationDate: sample.data?.verificationDate,
              // Storage data (to be filled by this page)
              storageCondition: sample.data?.storageCondition,
              storageLocation: sample.data?.storageLocation,
              herbariumSpecimenId: sample.data?.herbariumSpecimenId,
              herbariumNotes: sample.data?.herbariumNotes,
              linkedProject: sample.data?.linkedProject,
              storedAt: sample.data?.storedAt,
              storedBy: sample.data?.storedBy,
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

  // Load samples on mount
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  // Reset storage form
  const resetStorageForm = useCallback(() => {
    setStorageCondition(null);
    setStorageLocation("");
    setHerbariumSpecimenId("");
    setHerbariumNotes("");
    setLinkedProject("");
    setStorageNotes("");
  }, []);

  // Open storage modal
  const openStorageModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetStorageForm();
    setStorageModalOpen(true);
  }, [selectedSampleIds, intl, resetStorageForm, notify]);

  // Apply storage assignment to selected samples
  const applyStorage = useCallback(() => {
    if (!storageCondition) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.storage.error.conditionRequired",
          defaultMessage: "Please select storage condition.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingStorage(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/storage`,
      JSON.stringify({
        sampleIds: sampleIds,
        storageCondition: storageCondition.id,
        storageConditionLabel: storageCondition.label,
        storageLocation: storageLocation || null,
        herbariumSpecimenId: herbariumSpecimenId || null,
        herbariumNotes: herbariumNotes || null,
        linkedProject: linkedProject || null,
        storageNotes: storageNotes || null,
      }),
      (response) => {
        setIsApplyingStorage(false);

        if (response && response.success) {
          // Update sample status using bulk endpoint after storage assignment
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds: sampleIds,
              status: "IN_PROGRESS",
            }),
            (statusCode) => {
              if (statusCode === 200) {
                notify({
                  kind: NotificationKinds.success,
                  title: response.message ||
                    intl.formatMessage(
                      {
                        id: "notebook.page.tradmed.storage.success",
                        defaultMessage: "Assigned storage for {count} sample(s).",
                      },
                      { count: response.updatedCount || selectedSampleIds.length },
                    ),
                });
                setStorageModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage: "Storage assigned but failed to update sample status.",
                  }),
                });
              }
            }
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.storage.error.failed",
                defaultMessage: "Failed to assign storage. Please try again.",
              }),
          });
        }
      },
    );
  }, [
    storageCondition,
    storageLocation,
    herbariumSpecimenId,
    herbariumNotes,
    linkedProject,
    storageNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // Split samples by storage status
  const unaccessibleSamples = useMemoHook(
    () =>
      samples.filter(
        (s) =>
          !s.authenticationResult ||
          s.authenticationResult !== "confirmed",
      ),
    [samples],
  );

  const storedSamples = useMemoHook(
    () => samples.filter((s) => s.storageCondition && s.status === "COMPLETED"),
    [samples],
  );

  const authenticatedNotStoredSamples = useMemoHook(
    () =>
      samples.filter(
        (s) =>
          s.authenticationResult === "confirmed" &&
          !s.storageCondition,
      ),
    [samples],
  );

  const unaccessibleCount = unaccessibleSamples.length;
  const authenticatedNotStoredCount = authenticatedNotStoredSamples.length;
  const storedCount = storedSamples.length;

  // Helper to render authentication status
  const renderAuthenticationStatus = (sample) => {
    if (!sample.authenticationMethod) {
      return (
        <Tag type="gray" size="sm" renderIcon={Pending}>
          <FormattedMessage
            id="notebook.page.tradmed.auth.notStarted"
            defaultMessage="Not Started"
          />
        </Tag>
      );
    }
    if (sample.authenticationResult === "confirmed") {
      return (
        <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
          {sample.authenticationMethodLabel || "Verified"}
        </Tag>
      );
    }
    if (sample.authenticationResult === "not_confirmed") {
      return (
        <Tag type="red" size="sm" renderIcon={WarningAltFilled}>
          <FormattedMessage
            id="notebook.page.tradmed.auth.notConfirmed"
            defaultMessage="Not Confirmed"
          />
        </Tag>
      );
    }
    return (
      <Tag type="blue" size="sm" renderIcon={Chemistry}>
        {sample.authenticationResultLabel || "In Progress"}
      </Tag>
    );
  };

  // Helper to render storage condition tag
  const renderStorageConditionTag = (sample) => {
    if (!sample.storageCondition) {
      return (
        <Tag type="gray" size="sm">
          <FormattedMessage
            id="notebook.page.tradmed.storage.notAssigned"
            defaultMessage="Not Assigned"
          />
        </Tag>
      );
    }
    return (
      <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
        {sample.storageCondition}
      </Tag>
    );
  };

  return (
    <div className="tradmed-storage-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.storage.title"
            defaultMessage="Authentication Review & Sample Storage / Herbarium Placement"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.storage.description"
            defaultMessage="Review authentication results, assign storage conditions, and catalog specimens in herbarium with species, collector, location, and date information."
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
                  id="notebook.page.tradmed.storage.readyForStorage"
                  defaultMessage="Ready for Storage"
                />
              </span>
              <span className="progress-value">{authenticatedNotStoredCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.storage.stored"
                  defaultMessage="Stored"
                />
              </span>
              <span className="progress-value">{storedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.storage.needsAuth"
                  defaultMessage="Needs Authentication"
                />
              </span>
              <span className="progress-value">{unaccessibleCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={CloudUpload}
          onClick={openStorageModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.storage.assignStorage"
            defaultMessage="Assign Storage ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.page.tradmed.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>


      {/* Needs Authentication Section */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.storage.needsAuth.title"
              defaultMessage="Samples Awaiting Authentication"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {unaccessibleCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.storage.needsAuth.description"
              defaultMessage="Samples that have not yet been authenticated on Page 1. Please complete authentication before proceeding to storage."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && unaccessibleSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.storage.needsAuth.empty"
                  defaultMessage="No samples awaiting authentication."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="unaccessible-samples"
              samples={unaccessibleSamples}
              selectedIds={[]}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                {
                  key: "authenticationStatus",
                  header: "Auth Status",
                  render: (_value, row) => renderAuthenticationStatus(row),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Authenticated Not Yet Stored Section */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.storage.readyForStorage.title"
              defaultMessage="Authenticated - Ready for Storage Assignment"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {authenticatedNotStoredCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.storage.readyForStorage.description"
              defaultMessage="Samples that have been authenticated and are ready for storage location assignment and herbarium cataloging."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && authenticatedNotStoredSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.storage.readyForStorage.empty"
                  defaultMessage="No authenticated samples awaiting storage assignment."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="ready-for-storage-samples"
              samples={authenticatedNotStoredSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                {
                  key: "authenticationStatus",
                  header: "Auth Status",
                  render: (_value, row) => renderAuthenticationStatus(row),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Stored Samples Section */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.storage.stored.title"
              defaultMessage="Stored & Catalogued Specimens"
            />
            <Tag type="green" size="sm" className="count-tag">
              {storedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.storage.stored.description"
              defaultMessage="Samples that have been assigned to storage locations and herbarium placements."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && storedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.storage.stored.empty"
                  defaultMessage="No samples have been assigned to storage yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="stored-samples"
              samples={storedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "storageCondition", header: "Storage Condition" },
                { key: "storageLocation", header: "Location" },
                { key: "herbariumSpecimenId", header: "Herbarium ID" },
              ]}
            />
          )}
        </div>
      </div>

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        onRequestClose={() => setStorageModalOpen(false)}
        onRequestSubmit={applyStorage}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.storage.modal.title",
          defaultMessage: "Assign Storage & Herbarium Placement",
        })}
        primaryButtonText={
          isApplyingStorage
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.storage.modal.assign",
                defaultMessage: "Assign Storage",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingStorage}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.storage.modal.description"
              defaultMessage="Assign storage conditions and herbarium placement for {count} selected sample(s). Per SRS requirements, specify storage condition, location, and herbarium cataloging details."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        {isApplyingStorage && <Loading withOverlay={false} small />}

        <Grid fullWidth narrow>
          <Column lg={16} md={8} sm={4}>
            <Dropdown
              id="storage-condition"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.storage.modal.condition",
                defaultMessage: "Storage Condition *",
              })}
              label={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
              items={storageConditionOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={storageCondition}
              onChange={({ selectedItem }) => setStorageCondition(selectedItem)}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Dropdown
              id="storage-location"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.storage.modal.location",
                defaultMessage: "Storage Location",
              })}
              label={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
              items={storageLocationOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={
                storageLocationOptions.find((l) => l.id === storageLocation) ||
                null
              }
              onChange={({ selectedItem }) =>
                setStorageLocation(selectedItem?.id || "")
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="herbarium-specimen-id"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.storage.modal.herbariumId",
                defaultMessage: "Herbarium Specimen ID",
              })}
              value={herbariumSpecimenId}
              onChange={(e) => setHerbariumSpecimenId(e.target.value)}
              placeholder="e.g., HB-2025-001"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="linked-project"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.storage.modal.linkedProject",
                defaultMessage: "Linked Project",
              })}
              value={linkedProject}
              onChange={(e) => setLinkedProject(e.target.value)}
              placeholder="e.g., Project Name or ID"
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="herbarium-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.storage.modal.herbariumNotes",
                defaultMessage: "Herbarium Notes",
              })}
              value={herbariumNotes}
              onChange={(e) => setHerbariumNotes(e.target.value)}
              rows={2}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.storage.modal.herbariumNotesPlaceholder",
                defaultMessage:
                  "Specimen mounting, labeling, condition notes, etc.",
              })}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="storage-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.storage.modal.storageNotes",
                defaultMessage: "Storage Notes",
              })}
              value={storageNotes}
              onChange={(e) => setStorageNotes(e.target.value)}
              rows={2}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.storage.modal.storageNotesPlaceholder",
                defaultMessage:
                  "Storage condition details, environmental requirements, etc.",
              })}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineAuthenticationStoragePage;
