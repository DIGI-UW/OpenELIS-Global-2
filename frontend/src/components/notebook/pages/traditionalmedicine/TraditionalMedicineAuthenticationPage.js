import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  useContext,
} from "react";
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
  Renew,
  CheckmarkFilled,
  Edit,
  Pending,
  WarningAltFilled,
  Chemistry,
  Archive,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import { Permissions } from "../../../../constants/roles";
import PermissionGate from "../../../security/PermissionGate";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineAuthenticationPage - Page 2 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 2: Authentication & Verification
 * - Botanical verification or expert identification
 * - Methods: Morphological examination, Microscopic analysis, Molecular identification (DNA barcoding)
 * - Document method used, result (species confirmed/uncertain/misidentified), authenticator name and date
 * - Link to herbarium specimen if prepared
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TraditionalMedicineAuthenticationPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [isApplyingAuth, setIsApplyingAuth] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  const [authMethod, setAuthMethod] = useState(null);
  const [authResult, setAuthResult] = useState(null);
  const [verifiedBy, setVerifiedBy] = useState("");
  const [verificationDate, setVerificationDate] = useState(
    new Date().toISOString().slice(0, 10),
  );
  const [authNotes, setAuthNotes] = useState("");

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Authentication method options per SRS
  const authMethodOptions = [
    { id: "morphological_examination", label: "Morphological examination" },
    { id: "microscopic_analysis", label: "Microscopic analysis" },
    {
      id: "molecular_identification",
      label: "Molecular identification (DNA barcoding: ITS, rbcL, matK genes)",
    },
  ];

  // Authentication result options
  const authResultOptions = [
    { id: "confirmed", label: "Confirmed / Authenticated" },
    { id: "not_confirmed", label: "Not Confirmed" },
    { id: "inconclusive", label: "Inconclusive - Further Testing Required" },
    { id: "partial", label: "Partially Confirmed" },
  ];

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          let samplesToProcess = [];

          // Handle both array and object responses from API
          if (response) {
            if (Array.isArray(response)) {
              samplesToProcess = response;
            } else if (response.samples && Array.isArray(response.samples)) {
              samplesToProcess = response.samples;
            }
          }

          setSamples(
            samplesToProcess.length > 0
              ? samplesToProcess.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  localName: s.data?.localName,
                  scientificName: s.data?.scientificName,
                  // Authentication data
                  authenticationMethod: s.data?.authenticationMethod,
                  authenticationMethodLabel: s.data?.authenticationMethodLabel,
                  authenticationResult: s.data?.authenticationResult,
                  authenticationResultLabel: s.data?.authenticationResultLabel,
                  verifiedBy: s.data?.verifiedBy,
                  verificationDate: s.data?.verificationDate,
                  authenticationNotes: s.data?.authenticationNotes,
                  authenticatedAt: s.data?.authenticatedAt,
                }))
              : [],
          );
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
  }, [entryId, pageData?.id, loadPageSamples]);

  const resetAuthForm = useCallback(() => {
    setAuthMethod(null);
    setAuthResult(null);
    setVerifiedBy("");
    setVerificationDate(new Date().toISOString().slice(0, 10));
    setAuthNotes("");
  }, []);

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
  // Backend marks samples as COMPLETED to advance to next stage regardless of authentication result
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
          const message =
            response.message ||
            intl.formatMessage(
              {
                id: "notebook.page.tradmed.success.authCompleted",
                defaultMessage:
                  "Authentication completed for {count} sample(s). Result: {result}. Samples are now ready for the next stage (Storage & Herbarium Placement).",
              },
              {
                count: response.updatedCount || selectedSampleIds.length,
                result: authResult.label,
              },
            );

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
            title:
              response?.error ||
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

  // Handle marking authenticated samples complete (moving to next page)
  const handleMarkComplete = useCallback(() => {
    // Filter samples that can be marked complete: selected, authenticated, and not already completed
    const samplesToComplete = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.authenticationMethod &&
        s.status !== "COMPLETED",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.auth.noEligibleSamples",
          defaultMessage:
            "Selected samples must be authenticated before completing.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setIsCompleting(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.tradmed.auth.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount || sampleIds.length },
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
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.tradmed.auth.completeFailed",
                defaultMessage: "Failed to mark samples complete.",
              }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Split samples: pending authentication vs authenticated (IN_PROGRESS) vs authenticated (COMPLETED)
  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );
  const authenticatedCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

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
    <div className="tradmed-authentication-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.authentication.title"
            defaultMessage="Botanical Authentication & Verification"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.authentication.description"
            defaultMessage="Perform botanical verification or expert identification using morphological examination, microscopic analysis, or molecular identification (DNA barcoding). Document method, result, and authenticator details."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.authentication.pending"
                  defaultMessage="Awaiting Authentication"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.authentication.verified"
                  defaultMessage="Authenticated"
                />
              </span>
              <span className="progress-value">
                {authenticatedCompletedSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <PermissionGate
          roles={Permissions.PROCESS_SAMPLES}
          disabledTooltip={intl.formatMessage({
            id: "notebook.tradmed.tooltip.authenticatePermission",
            defaultMessage: "Insufficient permissions to authenticate samples",
          })}
        >
          <Button
            kind="primary"
            size="sm"
            renderIcon={Edit}
            onClick={openAuthModal}
            disabled={selectedSampleIds.length === 0 || !hasRealPageId}
            title={
              selectedSampleIds.length === 0
                ? intl.formatMessage({
                    id: "notebook.tradmed.tooltip.selectSamples",
                    defaultMessage: "Select samples to authenticate",
                  })
                : ""
            }
          >
            <FormattedMessage
              id="notebook.page.tradmed.authenticate"
              defaultMessage="Authenticate ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </PermissionGate>

        {selectedSampleIds.length > 0 &&
          pendingSamples.some((s) => selectedSampleIds.includes(s.id)) && (
            <PermissionGate
              roles={Permissions.VALIDATE_RESULTS}
              disabledTooltip={intl.formatMessage({
                id: "notebook.tradmed.tooltip.markCompletePermission",
                defaultMessage:
                  "Insufficient permissions to mark samples complete",
              })}
            >
              <Button
                kind="tertiary"
                size="sm"
                renderIcon={CheckmarkFilled}
                onClick={handleMarkComplete}
                disabled={isCompleting || !hasRealPageId}
              >
                <FormattedMessage
                  id="notebook.tradmed.auth.markComplete"
                  defaultMessage="Mark Complete ({count})"
                  values={{ count: selectedSampleIds.length }}
                />
              </Button>
            </PermissionGate>
          )}

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

      {/* Pending Authentication Samples Section */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.authentication.pending.title"
              defaultMessage="Samples Awaiting Authentication"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingSamples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.authentication.pending.description"
              defaultMessage="Registered samples ready for botanical verification or expert identification per SRS Stage 2 requirements."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.authentication.pending.empty"
                  defaultMessage="No samples awaiting authentication. Samples must first be registered in Stage 1."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-authentication"
              samples={pendingSamples}
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
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
                { key: "authenticationMethodLabel", header: "Method" },
                { key: "verifiedBy", header: "Verified By" },
                { key: "verificationDate", header: "Date" },
                {
                  key: "authenticationStatus",
                  header: "Result",
                  render: (_value, row) => renderAuthenticationStatus(row),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Authenticated Samples Section - COMPLETED */}
      {authenticatedCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.authentication.completed.title"
                defaultMessage="Authentication Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {authenticatedCompletedSamples.length}
              </Tag>
            </h5>
            <p className="table-section-description">
              <FormattedMessage
                id="notebook.page.tradmed.authentication.verified.completed.description"
                defaultMessage="Samples with completed authentication, ready to proceed to Stage 3 (Storage & Herbarium Placement)."
              />
            </p>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="authenticated-completed-samples"
              samples={authenticatedCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
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
                { key: "authenticationMethodLabel", header: "Method" },
                { key: "verifiedBy", header: "Verified By" },
                { key: "verificationDate", header: "Date" },
                {
                  key: "authenticationStatus",
                  header: "Result",
                  render: (_value, row) => renderAuthenticationStatus(row),
                },
              ]}
            />
          </div>
        </div>
      )}

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
              defaultMessage="Record authentication details for {count} selected sample(s). Per SRS Stage 2 requirements, the system logs the authentication method and result."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        {isApplyingAuth && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
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
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
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
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
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
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
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
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
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

export default TraditionalMedicineAuthenticationPage;
