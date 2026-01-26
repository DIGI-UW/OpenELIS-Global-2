import { useState, useEffect, useRef, useCallback, useContext } from "react";
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
  Upload,
  Checkmark,
  Renew,
  Chemistry,
  CheckmarkFilled,
  Pending,
  Edit,
  WarningAltFilled,
  Archive,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { useMemo } from "react";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import TraditionalMedicineManifestImportModal from "../../workflow/TraditionalMedicineManifestImportModal";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useTMMRDPermissions } from "../../../../hooks/useTMMRDPermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineSampleCreationPage - Page 1 of the Traditional Medicine workflow.
 *
 * SRS Requirements - Sample Intake and Registration:
 * 1. Sample Arrival - Includes plant materials or other traditional medicine sources
 * 2. Registration & Labeling - Assign unique sample ID, record metadata (origin, species, collector, date/time)
 * 3. Authentication - Botanical verification or expert identification, LMS logs method and result
 *
 * This page captures full metadata at sample creation including:
 * - Sample identity (accession number, external ID)
 * - Source information (category, source type, origin location)
 * - Taxonomy (local name, scientific name, species)
 * - Collection details (collector, date, site, condition)
 * - Authentication (method, result, verified by, date)
 * - Intended use and notes
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TraditionalMedicineSampleCreationPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);
  const { hasAnyRole } = usePermissions();

  // TMMRD permissions per SRS Section 11
  const {
    getPagePermissionLevel,
    canRegisterData,
    canSaveData,
    canApproveData,
    hasFullControl,
    isReadOnly,
    canAccessStage1,
  } = useTMMRDPermissions();

  // STAGE 1 allowed roles per TMMRD SRS Section 11
  const allowedRoles = [
    "Lab Technician",
    "Researcher",
    "Pharmacognosist",
    "Lab Manager",
    "Principal Investigator",
  ];

  const canAccessPage = hasAnyRole(allowedRoles);

  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Sample Intake & Registration"
        reason="This page requires specific Traditional Medicine laboratory roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel(
    "Sample Intake & Registration",
  );
  const canImportSamples = canRegisterData(pagePermissionLevel);
  const canEditMetadata = canSaveData(pagePermissionLevel);
  const canSaveDataLocal = canEditMetadata; // Alias for button conditions
  const canAuthenticateSamples = canApproveData(pagePermissionLevel);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const [importModalOpen, setImportModalOpen] = useState(false);
  const [qcInspectionModalOpen, setQcInspectionModalOpen] = useState(false);
  const [qcInspectionData, setQcInspectionData] = useState({
    contaminationFree: false,
    adequateCondition: false,
    visualAppearance: "",
    microbialCheck: false,
    pesticideCheck: false,
    moldCheck: false,
    qcNotes: "",
    inspectedBy: "",
    inspectionDate: new Date().toISOString().split("T")[0],
    qcResult: "", // PASS or FAIL - set by inspector via radio buttons
  });

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const bulkApplyMetadata = useCallback(
    async (sampleIds, data) => {
      if (!hasRealPageId) return false;

      try {
        const response = await postToOpenElisServerJsonResponse(
          `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
          JSON.stringify({
            sampleIds: sampleIds.map((id) => parseInt(id, 10)),
            data: data,
          }),
        );

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.bulk.apply.success",
            }),
            message: intl.formatMessage(
              { id: "notification.bulk.apply.samples.count" },
              { count: response.updatedCount },
            ),
          });
          return true;
        }
        return false;
      } catch (error) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.bulk.apply.error" }),
          message: error.message || "Failed to apply metadata to samples",
        });
        return false;
      }
    },
    [hasRealPageId, pageData?.id, notify, intl],
  );

  const bulkAdvanceSamples = useCallback(
    async (sampleIds, targetPageIndex = 2) => {
      try {
        const response = await postToOpenElisServerJsonResponse(
          `/rest/notebook/${entryId}/samples/advance`,
          JSON.stringify({
            sampleIds: sampleIds.map((id) => parseInt(id, 10)),
            fromPageId: pageData.id,
            toPageIndex: targetPageIndex,
          }),
        );

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.samples.advanced" }),
            message: intl.formatMessage(
              { id: "notification.samples.advanced.count" },
              { count: response.advancedCount, stage: targetPageIndex },
            ),
          });
          return true;
        }
        return false;
      } catch (error) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notification.samples.advance.error",
          }),
          message: error.message || "Failed to advance samples to next stage",
        });
        return false;
      }
    },
    [entryId, pageData?.id, notify, intl],
  );

  const markSamplesCompleted = useCallback(
    async (sampleIds) => {
      if (!hasRealPageId) return false;

      try {
        const response = await postToOpenElisServerJsonResponse(
          `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
          JSON.stringify({
            sampleIds: sampleIds.map((id) => parseInt(id, 10)),
            status: "COMPLETED",
          }),
        );

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.samples.completed" }),
            message: intl.formatMessage(
              { id: "notification.samples.completed.count" },
              { count: response.updatedCount },
            ),
          });
          return true;
        }
        return false;
      } catch (error) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notification.samples.complete.error",
          }),
          message: error.message || "Failed to mark samples as completed",
        });
        return false;
      }
    },
    [hasRealPageId, pageData?.id, notify, intl],
  );

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
              collectionDate:
                sample.data?.collectionDate || sample.collectionDate,
              status: sample.pageStatus || sample.status || "PENDING",
              receivedDate: sample.data?.receivedDate || sample.receivedDate,
              receivedBy: sample.data?.receivedBy,
              sampleCategory: sample.data?.sampleCategory,
              sourceType: sample.data?.sourceType,
              originLocation: sample.data?.originLocation,
              collectionSite: sample.data?.collectionSite,
              collectedBy: sample.data?.collectedBy,
              localName: sample.data?.localName,
              scientificName: sample.data?.scientificName,
              species: sample.data?.species,
              plantPart: sample.data?.plantPart,
              sampleCondition: sample.data?.sampleCondition,
              intendedUse: sample.data?.intendedUse,
              notes: sample.data?.notes,
              authenticationMethod: sample.data?.authenticationMethod,
              authenticationMethodLabel: sample.data?.authenticationMethodLabel,
              authenticationResult: sample.data?.authenticationResult,
              authenticationResultLabel: sample.data?.authenticationResultLabel,
              verifiedBy: sample.data?.verifiedBy,
              verificationDate: sample.data?.verificationDate,
              authenticationNotes: sample.data?.authenticationNotes,
              authenticatedAt: sample.data?.authenticatedAt,
              // Include full data object for QC checking
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

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const handleImportSuccess = useCallback(() => {
    setImportModalOpen(false);
    loadPageSamples();
    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [loadPageSamples, onProgressUpdate]);

  const markAsRegistered = useCallback(() => {
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

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.page.tradmed.success.registered",
                defaultMessage: "Marked {count} sample(s) as Registered.",
              },
              { count: selectedSampleIds.length },
            ),
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.page.tradmed.error.status",
              defaultMessage: "Failed to register samples. Please try again.",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
    notify,
  ]);

  // Plant Material QC Inspection handlers
  const openQcInspectionModal = useCallback(() => {
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
    setQcInspectionModalOpen(true);
  }, [selectedSampleIds, intl, notify]);

  const resetQcInspectionForm = useCallback(() => {
    setQcInspectionData({
      contaminationFree: false,
      adequateCondition: false,
      visualAppearance: "",
      microbialCheck: false,
      pesticideCheck: false,
      moldCheck: false,
      qcNotes: "",
      inspectedBy: "",
      inspectionDate: new Date().toISOString().split("T")[0],
      qcResult: "",
    });
  }, []);

  const applyQcInspection = useCallback(async () => {
    // Validate that QC Result is selected
    if (!qcInspectionData.qcResult) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.qc.error.noResult",
          defaultMessage: "QC Result required",
        }),
        message: intl.formatMessage({
          id: "notebook.page.tradmed.qc.error.selectPassFail",
          defaultMessage: "Please select Pass or Fail for QC inspection.",
        }),
      });
      return;
    }

    const success = await bulkApplyMetadata(selectedSampleIds, {
      qcInspection: {
        contaminationFree: qcInspectionData.contaminationFree,
        adequateCondition: qcInspectionData.adequateCondition,
        visualAppearance: qcInspectionData.visualAppearance,
        microbialCheck: qcInspectionData.microbialCheck,
        pesticideCheck: qcInspectionData.pesticideCheck,
        moldCheck: qcInspectionData.moldCheck,
        qcNotes: qcInspectionData.qcNotes,
        inspectedBy: qcInspectionData.inspectedBy,
        inspectionDate: qcInspectionData.inspectionDate,
        qcResult: qcInspectionData.qcResult, // Use the selected radio button value
      },
    });

    if (success) {
      setQcInspectionModalOpen(false);
      resetQcInspectionForm();
      setSelectedSampleIds([]);
    }
  }, [
    qcInspectionData,
    selectedSampleIds,
    bulkApplyMetadata,
    resetQcInspectionForm,
    intl,
    notify,
  ]);

  // Split samples: pending (not yet authenticated) vs registered (authenticated and ready for next stage)
  // Per SRS: Registration/advance to next stage happens after authentication is confirmed
  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );
  const registeredSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  const pendingCount = pendingSamples.length;
  const completedCount = registeredSamples.length;

  // Authentication status rendering moved to Stage 2 (TraditionalMedicineAuthenticationPage)

  // Helper to render sample status - simple status display matching API response
  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.tradmed.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm" renderIcon={Archive}>
            <FormattedMessage
              id="notebook.tradmed.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      case "SKIPPED":
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.tradmed.status.skipped"
              defaultMessage="Skipped"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm" renderIcon={Pending}>
            <FormattedMessage
              id="notebook.tradmed.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  return (
    <div className="tradmed-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.sampleCreation.title"
            defaultMessage="Sample Intake & Registration"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.sampleCreation.description"
            defaultMessage="Import traditional medicine samples and record complete metadata including origin, species, collector, date/time, and sample condition. Authentication is handled in Stage 2."
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
                  id="notebook.page.tradmed.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.pending"
                  defaultMessage="Pending Registration"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.registered"
                  defaultMessage="Registered"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons - SRS Stage 1: Sample Intake & Registration Only */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Upload}
          onClick={() => setImportModalOpen(true)}
          disabled={!canImportSamples}
        >
          <FormattedMessage
            id="notebook.page.tradmed.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Chemistry}
          onClick={openQcInspectionModal}
          disabled={!canEditMetadata || selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tradmed.qcInspection"
            defaultMessage="QC Inspection ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Checkmark}
          onClick={markAsRegistered}
          disabled={
            !canSaveDataLocal ||
            selectedSampleIds.length === 0 ||
            !selectedSampleIds.every((id) => {
              const sample = samples.find((s) => s.id === id);
              return sample?.data?.qcInspection?.qcResult === "PASS";
            })
          }
        >
          <FormattedMessage
            id="notebook.page.tradmed.markAsRegistered"
            defaultMessage="Mark as Registered ({count})"
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

      {/* Pending / In Progress Samples Section */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.pendingSamples.title"
              defaultMessage="Pending Registration"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.pendingSamples.description"
              defaultMessage="Imported samples awaiting registration. Select samples and mark them as registered to advance them to Stage 2 (Authentication)."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.pendingSamples.empty"
                  defaultMessage="No pending samples. Import a manifest to add traditional medicine samples with complete metadata."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                // Registration & Labeling - Unique Sample ID
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                // Sample Category
                { key: "sampleCategory", header: "Category" },
                // Taxonomy - Species identification
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
                // Source & Origin
                { key: "sourceType", header: "Source Type" },
                { key: "originLocation", header: "Origin" },
                { key: "collectedBy", header: "Collector" },
                { key: "collectionDate", header: "Collection Date" },
                // Sample details
                { key: "plantPart", header: "Plant Part" },
                { key: "sampleCondition", header: "Condition" },
                // Intended Use
                { key: "intendedUse", header: "Intended Use" },
                // Authentication Status moved to Stage 2 (TraditionalMedicineAuthenticationPage)
              ]}
            />
          )}
        </div>
      </div>

      {/* Registered / Authenticated Samples Section */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.registeredSamples.title"
              defaultMessage="Registered Samples"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.registeredSamples.description"
              defaultMessage="Samples that have been registered and are ready to proceed to Stage 2 (Authentication & Verification)."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && registeredSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.registeredSamples.empty"
                  defaultMessage="No registered samples yet. Select pending samples and mark them as registered."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="registered-samples"
              samples={registeredSamples}
              showSelection={false}
              loading={loading}
              columns={[
                // Registration & Labeling - Unique Sample ID
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                // Sample Category
                { key: "sampleCategory", header: "Category" },
                // Taxonomy - Species identification
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
                // Source & Origin
                { key: "sourceType", header: "Source Type" },
                { key: "originLocation", header: "Origin" },
                { key: "collectedBy", header: "Collector" },
                { key: "collectionDate", header: "Collection Date" },
                // Sample details
                { key: "plantPart", header: "Plant Part" },
                { key: "sampleCondition", header: "Condition" },
                // Intended Use
                { key: "intendedUse", header: "Intended Use" },
                // Authentication Status moved to Stage 2 (TraditionalMedicineAuthenticationPage)
              ]}
            />
          )}
        </div>
      </div>

      {/* Global Empty state - only show when no samples at all */}
      {!loading && samples.length === 0 && (
        <div className="empty-state global-empty">
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.empty"
              defaultMessage="No samples have been added yet. Import a manifest to add traditional medicine samples with complete metadata."
            />
          </p>
        </div>
      )}

      {/* Manifest Import Modal */}
      <TraditionalMedicineManifestImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
        onImportSuccess={handleImportSuccess}
      />

      {/* Plant Material QC Inspection Modal - SRS Section 5: Quality Control Parameters */}
      <Modal
        open={qcInspectionModalOpen}
        onRequestClose={() => {
          setQcInspectionModalOpen(false);
          resetQcInspectionForm();
        }}
        onRequestSubmit={applyQcInspection}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.qcInspection.title",
          defaultMessage: "Plant Material QC Inspection",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.page.tradmed.qcInspection.apply",
          defaultMessage: "Apply QC Inspection",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        size="lg"
      >
        <Grid narrow>
          {/* QC Inspection Requirements from SRS Section 5 */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <div
              style={{
                padding: "1rem",
                backgroundColor: "var(--cds-layer-01)",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <h5 style={{ margin: "0 0 0.5rem 0" }}>
                <FormattedMessage
                  id="notebook.page.tradmed.qc.requirements"
                  defaultMessage="Plant Material QC Requirements"
                />
              </h5>
              <ul
                style={{
                  margin: "0.5rem 0",
                  paddingLeft: "1.5rem",
                  fontSize: "0.875rem",
                }}
              >
                <li>
                  <FormattedMessage
                    id="notebook.page.tradmed.qc.correct_species"
                    defaultMessage="Correct species (authenticated)"
                  />
                </li>
                <li>
                  <FormattedMessage
                    id="notebook.page.tradmed.qc.free_contamination"
                    defaultMessage="Free from contamination (microbial, pesticides)"
                  />
                </li>
                <li>
                  <FormattedMessage
                    id="notebook.page.tradmed.qc.adequate_condition"
                    defaultMessage="Adequate condition (not spoiled, moldy)"
                  />
                </li>
              </ul>
            </div>
          </Column>

          {/* Contamination-Free Checkbox */}
          <Column lg={8} md={4} sm={2} style={{ marginBottom: "1rem" }}>
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <input
                type="checkbox"
                id="contaminationFree"
                checked={qcInspectionData.contaminationFree}
                onChange={(e) =>
                  setQcInspectionData((prev) => ({
                    ...prev,
                    contaminationFree: e.target.checked,
                  }))
                }
              />
              <label
                htmlFor="contaminationFree"
                style={{ margin: "0", fontSize: "0.875rem" }}
              >
                <FormattedMessage
                  id="notebook.page.tradmed.qc.free_contamination"
                  defaultMessage="Free from contamination"
                />
              </label>
            </div>
          </Column>

          {/* Adequate Condition Checkbox */}
          <Column lg={8} md={4} sm={2} style={{ marginBottom: "1rem" }}>
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <input
                type="checkbox"
                id="adequateCondition"
                checked={qcInspectionData.adequateCondition}
                onChange={(e) =>
                  setQcInspectionData((prev) => ({
                    ...prev,
                    adequateCondition: e.target.checked,
                  }))
                }
              />
              <label
                htmlFor="adequateCondition"
                style={{ margin: "0", fontSize: "0.875rem" }}
              >
                <FormattedMessage
                  id="notebook.page.tradmed.qc.adequate_condition"
                  defaultMessage="Adequate condition"
                />
              </label>
            </div>
          </Column>

          {/* Visual Appearance Dropdown */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="visualAppearance"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.qc.visual_appearance",
                defaultMessage: "Visual Appearance",
              })}
              label="Select appearance..."
              items={[
                { id: "normal", text: "Normal/Good" },
                { id: "discolored", text: "Discolored" },
                { id: "moldy", text: "Moldy/Fungal Growth" },
                { id: "insect_damage", text: "Insect Damage" },
                { id: "damaged", text: "Physically Damaged" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                qcInspectionData.visualAppearance
                  ? [
                      { id: "normal", text: "Normal/Good" },
                      { id: "discolored", text: "Discolored" },
                      { id: "moldy", text: "Moldy/Fungal Growth" },
                      { id: "insect_damage", text: "Insect Damage" },
                      { id: "damaged", text: "Physically Damaged" },
                    ].find((i) => i.id === qcInspectionData.visualAppearance) ||
                    null
                  : null
              }
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  setQcInspectionData((prev) => ({
                    ...prev,
                    visualAppearance: selectedItem.id,
                  }));
                }
              }}
            />
          </Column>

          {/* Microbial Check */}
          <Column lg={8} md={4} sm={2}>
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <input
                type="checkbox"
                id="microbialCheck"
                checked={qcInspectionData.microbialCheck}
                onChange={(e) =>
                  setQcInspectionData((prev) => ({
                    ...prev,
                    microbialCheck: e.target.checked,
                  }))
                }
              />
              <label
                htmlFor="microbialCheck"
                style={{ margin: "0", fontSize: "0.875rem" }}
              >
                <FormattedMessage
                  id="notebook.page.tradmed.qc.microbial_check"
                  defaultMessage="Microbial test required"
                />
              </label>
            </div>
          </Column>

          {/* Pesticide Check */}
          <Column lg={8} md={4} sm={2}>
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <input
                type="checkbox"
                id="pesticideCheck"
                checked={qcInspectionData.pesticideCheck}
                onChange={(e) =>
                  setQcInspectionData((prev) => ({
                    ...prev,
                    pesticideCheck: e.target.checked,
                  }))
                }
              />
              <label
                htmlFor="pesticideCheck"
                style={{ margin: "0", fontSize: "0.875rem" }}
              >
                <FormattedMessage
                  id="notebook.page.tradmed.qc.pesticide_check"
                  defaultMessage="Pesticide test required"
                />
              </label>
            </div>
          </Column>

          {/* Mold Check */}
          <Column lg={8} md={4} sm={2} style={{ marginBottom: "1rem" }}>
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <input
                type="checkbox"
                id="moldCheck"
                checked={qcInspectionData.moldCheck}
                onChange={(e) =>
                  setQcInspectionData((prev) => ({
                    ...prev,
                    moldCheck: e.target.checked,
                  }))
                }
              />
              <label
                htmlFor="moldCheck"
                style={{ margin: "0", fontSize: "0.875rem" }}
              >
                <FormattedMessage
                  id="notebook.page.tradmed.qc.mold_check"
                  defaultMessage="Mold test required"
                />
              </label>
            </div>
          </Column>

          {/* Inspected By */}
          <Column lg={8} md={4} sm={2} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="inspectedBy"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.qc.inspected_by",
                defaultMessage: "Inspected By",
              })}
              value={qcInspectionData.inspectedBy}
              onChange={(e) =>
                setQcInspectionData((prev) => ({
                  ...prev,
                  inspectedBy: e.target.value,
                }))
              }
              placeholder="Inspector name or ID"
            />
          </Column>

          {/* Inspection Date */}
          <Column lg={8} md={4} sm={2} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="inspectionDate"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.qc.inspection_date",
                defaultMessage: "Inspection Date",
              })}
              type="date"
              value={qcInspectionData.inspectionDate}
              onChange={(e) =>
                setQcInspectionData((prev) => ({
                  ...prev,
                  inspectionDate: e.target.value,
                }))
              }
            />
          </Column>

          {/* QC Notes */}
          <Column lg={16} md={16} sm={4}>
            <TextArea
              id="qcNotes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.qc.notes",
                defaultMessage: "QC Inspection Notes",
              })}
              value={qcInspectionData.qcNotes}
              onChange={(e) =>
                setQcInspectionData((prev) => ({
                  ...prev,
                  qcNotes: e.target.value,
                }))
              }
              rows={3}
              placeholder="Record any observations, anomalies, or special conditions..."
            />
          </Column>

          {/* QC Result Decision - Radio Buttons for Pass/Fail */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <fieldset
              style={{
                border: "1px solid var(--cds-border-subtle)",
                borderRadius: "4px",
                padding: "1rem",
                margin: "0",
              }}
            >
              <legend
                style={{
                  fontSize: "0.875rem",
                  fontWeight: "600",
                  padding: "0 0.5rem",
                }}
              >
                <FormattedMessage
                  id="notebook.page.tradmed.qc.result"
                  defaultMessage="QC Inspection Result"
                />
              </legend>
              <div
                style={{
                  display: "flex",
                  gap: "2rem",
                  marginTop: "0.5rem",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
                  <input
                    type="radio"
                    id="qcResultPass"
                    name="qcResult"
                    value="PASS"
                    checked={qcInspectionData.qcResult === "PASS"}
                    onChange={(e) =>
                      setQcInspectionData((prev) => ({
                        ...prev,
                        qcResult: e.target.value,
                      }))
                    }
                  />
                  <label
                    htmlFor="qcResultPass"
                    style={{ margin: "0", fontSize: "0.875rem" }}
                  >
                    <FormattedMessage
                      id="notebook.page.tradmed.qc.result.pass"
                      defaultMessage="Pass"
                    />
                  </label>
                </div>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
                  <input
                    type="radio"
                    id="qcResultFail"
                    name="qcResult"
                    value="FAIL"
                    checked={qcInspectionData.qcResult === "FAIL"}
                    onChange={(e) =>
                      setQcInspectionData((prev) => ({
                        ...prev,
                        qcResult: e.target.value,
                      }))
                    }
                  />
                  <label
                    htmlFor="qcResultFail"
                    style={{ margin: "0", fontSize: "0.875rem" }}
                  >
                    <FormattedMessage
                      id="notebook.page.tradmed.qc.result.fail"
                      defaultMessage="Fail"
                    />
                  </label>
                </div>
              </div>
            </fieldset>
          </Column>
        </Grid>
      </Modal>

      {/* Authentication Modal removed - now handled on Stage 2 (TraditionalMedicineAuthenticationPage) */}
    </div>
  );
}

export default TraditionalMedicineSampleCreationPage;
