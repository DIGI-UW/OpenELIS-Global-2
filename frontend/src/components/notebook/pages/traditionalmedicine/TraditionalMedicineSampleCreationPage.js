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
  const { setNotificationVisible, addNotification } = useContext(NotificationContext);
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
    "Principal Investigator"
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
  const pagePermissionLevel = getPagePermissionLevel("Sample Intake & Registration");
  const canImportSamples = canRegisterData(pagePermissionLevel);
  const canEditMetadata = canSaveData(pagePermissionLevel);
  const canSaveDataLocal = canEditMetadata; // Alias for button conditions
  const canAuthenticateSamples = canApproveData(pagePermissionLevel);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  // Check if page has a real ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Debug logging can be enabled if needed
  // console.log("TMMRD Sample Creation Page - Permission Debug:", {
  //   pagePermissionLevel,
  //   canImportSamples,
  //   canEditMetadata,
  //   canSaveDataLocal,
  //   canAuthenticateSamples,
  //   hasRealPageId
  // });

  const [importModalOpen, setImportModalOpen] = useState(false);

  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [isApplyingAuth, setIsApplyingAuth] = useState(false);

  const [authMethod, setAuthMethod] = useState(null);
  const [authResult, setAuthResult] = useState(null);
  const [verifiedBy, setVerifiedBy] = useState("");
  const [verificationDate, setVerificationDate] = useState(
    new Date().toISOString().slice(0, 10),
  );
  const [authNotes, setAuthNotes] = useState("");

  // Authentication method options per SRS
  const authMethodOptions = [
    { id: "botanical_verification", label: "Botanical Verification" },
    { id: "expert_identification", label: "Expert Identification" },
    { id: "morphological_analysis", label: "Morphological Analysis" },
    { id: "molecular_identification", label: "Molecular Identification (DNA)" },
    { id: "chemical_profiling", label: "Chemical Profiling" },
    { id: "reference_comparison", label: "Reference Specimen Comparison" },
  ];

  // Authentication result options
  const authResultOptions = [
    { id: "confirmed", label: "Confirmed / Authenticated" },
    { id: "not_confirmed", label: "Not Confirmed" },
    { id: "inconclusive", label: "Inconclusive - Further Testing Required" },
    { id: "partial", label: "Partially Confirmed" },
  ];

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  // Bulk operations following bioanalytical pattern
  const bulkApplyMetadata = useCallback(async (sampleIds, data) => {
    if (!hasRealPageId) return false;

    try {
      const response = await postToOpenElisServerJsonResponse(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify({
          sampleIds: sampleIds.map(id => parseInt(id, 10)),
          data: data
        })
      );

      if (response.success) {
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.bulk.apply.success" }),
          message: intl.formatMessage(
            { id: "notification.bulk.apply.samples.count" },
            { count: response.updatedCount }
          )
        });
        return true;
      }
      return false;
    } catch (error) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.bulk.apply.error" }),
        message: error.message || "Failed to apply metadata to samples"
      });
      return false;
    }
  }, [hasRealPageId, pageData?.id, notify, intl]);

  const bulkAdvanceSamples = useCallback(async (sampleIds, targetPageIndex = 2) => {
    try {
      const response = await postToOpenElisServerJsonResponse(
        `/rest/notebook/${entryId}/samples/advance`,
        JSON.stringify({
          sampleIds: sampleIds.map(id => parseInt(id, 10)),
          fromPageId: pageData.id,
          toPageIndex: targetPageIndex
        })
      );

      if (response.success) {
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.samples.advanced" }),
          message: intl.formatMessage(
            { id: "notification.samples.advanced.count" },
            { count: response.advancedCount, stage: targetPageIndex }
          )
        });
        return true;
      }
      return false;
    } catch (error) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.samples.advance.error" }),
        message: error.message || "Failed to advance samples to next stage"
      });
      return false;
    }
  }, [entryId, pageData?.id, notify, intl]);

  const markSamplesCompleted = useCallback(async (sampleIds) => {
    if (!hasRealPageId) return false;

    try {
      const response = await postToOpenElisServerJsonResponse(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: sampleIds.map(id => parseInt(id, 10)),
          status: "COMPLETED"
        })
      );

      if (response.success) {
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.samples.completed" }),
          message: intl.formatMessage(
            { id: "notification.samples.completed.count" },
            { count: response.updatedCount }
          )
        });
        return true;
      }
      return false;
    } catch (error) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.samples.complete.error" }),
        message: error.message || "Failed to mark samples as completed"
      });
      return false;
    }
  }, [hasRealPageId, pageData?.id, notify, intl]);

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
              // Collection date from manifest is stored in data JSON, fallback to sample level
              collectionDate:
                sample.data?.collectionDate || sample.collectionDate,
              status: sample.pageStatus || sample.status || "PENDING",
              // Sample arrival tracking
              receivedDate: sample.data?.receivedDate || sample.receivedDate,
              receivedBy: sample.data?.receivedBy,
              // Traditional medicine specific fields from data JSON
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
              // Authentication data (SRS requirement - logged on Page 1)
              authenticationMethod: sample.data?.authenticationMethod,
              authenticationMethodLabel: sample.data?.authenticationMethodLabel,
              authenticationResult: sample.data?.authenticationResult,
              authenticationResultLabel: sample.data?.authenticationResultLabel,
              verifiedBy: sample.data?.verifiedBy,
              verificationDate: sample.data?.verificationDate,
              authenticationNotes: sample.data?.authenticationNotes,
              authenticatedAt: sample.data?.authenticatedAt,
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

  const handleImportSuccess = useCallback(() => {
    setImportModalOpen(false);
    loadPageSamples();
    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [loadPageSamples, onProgressUpdate]);

  // Reset authentication form
  const resetAuthForm = useCallback(() => {
    setAuthMethod(null);
    setAuthResult(null);
    setVerifiedBy("");
    setVerificationDate(new Date().toISOString().slice(0, 10));
    setAuthNotes("");
  }, []);

  // Open authentication modal
  const openAuthModal = useCallback(() => {
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
    resetAuthForm();
    setAuthModalOpen(true);
  }, [selectedSampleIds, intl, resetAuthForm, notify]);

  // Apply authentication to selected samples using dedicated authentication endpoint
  // Per SRS: LMS logs authentication method and result
  // When authentication is "confirmed", backend automatically marks samples as COMPLETED to advance to next stage
  const applyAuthentication = useCallback(() => {
    if (!authMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.authMethodRequired",
          defaultMessage: "Please select an authentication method.",
        }),
      });
      return;
    }

    if (!authResult) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.authResultRequired",
          defaultMessage: "Please select an authentication result.",
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

    setIsApplyingAuth(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    // Use dedicated authentication endpoint
    // Backend handles: logging method/result, updating status, advancing to next stage if confirmed
    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/authenticate`,
      JSON.stringify({
        sampleIds: sampleIds,
        authenticationMethod: authMethod.id,
        authenticationResult: authResult.id,
        verifiedBy: verifiedBy || null,
        verificationDate: verificationDate || null,
        authenticationNotes: authNotes || null,
      }),
      (response) => {
        setIsApplyingAuth(false);

        if (response && response.success) {
          // Backend returns different message based on result (confirmed vs other)
          const message =
            response.message ||
            (authResult.id === "confirmed"
              ? intl.formatMessage(
                  {
                    id: "notebook.page.tradmed.success.authConfirmed",
                    defaultMessage:
                      "Authenticated {count} sample(s). Samples are now ready for the next stage (Storage/Preparation).",
                  },
                  { count: response.updatedCount || selectedSampleIds.length },
                )
              : intl.formatMessage(
                  {
                    id: "notebook.page.tradmed.success.authApplied",
                    defaultMessage:
                      "Applied authentication to {count} sample(s).",
                  },
                  { count: response.updatedCount || selectedSampleIds.length },
                ));

          notify({
            kind: NotificationKinds.success,
            title: message,
          });
          setAuthModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.authFailed",
                defaultMessage:
                  "Failed to apply authentication. Please try again.",
              }),
          });
        }
      },
    );
  }, [
    authMethod,
    authResult,
    verifiedBy,
    verificationDate,
    authNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

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

  // Helper to render authentication status for a sample
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
    if (sample.authenticationResult === "inconclusive") {
      return (
        <Tag type="purple" size="sm" renderIcon={Renew}>
          <FormattedMessage
            id="notebook.page.tradmed.auth.inconclusive"
            defaultMessage="Inconclusive"
          />
        </Tag>
      );
    }
    return (
      <Tag type="blue" size="sm" renderIcon={Chemistry}>
        {sample.authenticationResultLabel || "Partial"}
      </Tag>
    );
  };

  return (
    <div className="tradmed-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.sampleCreation.title"
            defaultMessage="Sample Intake, Registration & Authentication"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.sampleCreation.description"
            defaultMessage="Import traditional medicine samples, record metadata (origin, species, collector), and authenticate via botanical verification or expert identification."
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
                  id="notebook.page.tradmed.pendingAuth"
                  defaultMessage="Pending / In Progress"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.authenticated"
                  defaultMessage="Authenticated"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
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
          renderIcon={Edit}
          onClick={openAuthModal}
          disabled={!canAuthenticateSamples || selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.authenticate"
            defaultMessage="Authenticate ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Checkmark}
          onClick={markAsRegistered}
          disabled={!canSaveDataLocal || selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tradmed.markAsRegistered"
            defaultMessage="Mark as Registered ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {/* Bulk operations following bioanalytical pattern */}
        <Button
          kind="secondary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={async () => {
            const success = await markSamplesCompleted(selectedSampleIds);
            if (success) {
              loadPageSamples();
              setSelectedSampleIds([]);
            }
          }}
          disabled={!canSaveDataLocal || selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.markCompleted"
            defaultMessage="Mark Completed ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Chemistry}
          onClick={async () => {
            const success = await bulkAdvanceSamples(selectedSampleIds, 2);
            if (success) {
              loadPageSamples();
              setSelectedSampleIds([]);
              // Notify parent to refresh progress
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            }
          }}
          disabled={!canSaveDataLocal || selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.advanceToAuth"
            defaultMessage="Advance to Authentication ({count})"
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
              defaultMessage="Pending Authentication"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.pendingSamples.description"
              defaultMessage="Samples awaiting authentication verification. Select samples and authenticate them using botanical verification, expert identification, or other methods per SRS requirements."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.pendingSamples.empty"
                  defaultMessage="No pending samples. Import a manifest to add traditional medicine samples."
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
                // Authentication Status (SRS: LMS logs method and result)
                {
                  key: "authenticationStatus",
                  header: "Authentication",
                  render: (_value, row) => renderAuthenticationStatus(row),
                },
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
              defaultMessage="Authenticated & Registered"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.registeredSamples.description"
              defaultMessage="Samples that have been authenticated and are ready to proceed to the next workflow stage (Storage & Herbarium Placement)."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && registeredSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.registeredSamples.empty"
                  defaultMessage="No authenticated samples yet. Select pending samples and authenticate them."
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
                // Authentication Status (SRS: LMS logs method and result)
                {
                  key: "authenticationStatus",
                  header: "Authentication",
                  render: (_value, row) => renderAuthenticationStatus(row),
                },
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

      {/* Authentication Modal */}
      <Modal
        open={authModalOpen}
        onRequestClose={() => setAuthModalOpen(false)}
        onRequestSubmit={applyAuthentication}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.authModal.title",
          defaultMessage: "Sample Authentication",
        })}
        primaryButtonText={
          isApplyingAuth
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.authModal.apply",
                defaultMessage: "Apply Authentication",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingAuth}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.authModal.description"
              defaultMessage="Record authentication details for {count} selected sample(s). Per SRS requirements, the system logs the authentication method and result."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        {isApplyingAuth && <Loading withOverlay={false} small />}

        <Grid fullWidth narrow>
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="auth-method"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.authModal.method",
                defaultMessage: "Authentication Method *",
              })}
              label={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
              items={authMethodOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={authMethod}
              onChange={({ selectedItem }) => setAuthMethod(selectedItem)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="auth-result"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.authModal.result",
                defaultMessage: "Authentication Result *",
              })}
              label={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
              items={authResultOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={authResult}
              onChange={({ selectedItem }) => setAuthResult(selectedItem)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="verified-by"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.authModal.verifiedBy",
                defaultMessage: "Verified By (Expert/Botanist)",
              })}
              value={verifiedBy}
              onChange={(e) => setVerifiedBy(e.target.value)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="verification-date"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.authModal.date",
                defaultMessage: "Verification Date",
              })}
              type="date"
              value={verificationDate}
              onChange={(e) => setVerificationDate(e.target.value)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="auth-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.authModal.notes",
                defaultMessage: "Authentication Notes",
              })}
              value={authNotes}
              onChange={(e) => setAuthNotes(e.target.value)}
              rows={3}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.authModal.notesPlaceholder",
                defaultMessage:
                  "Additional notes about the authentication process, reference materials used, etc.",
              })}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineSampleCreationPage;
