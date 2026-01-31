import React, {
  useState,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Grid, Column, Button, Tile, Tag } from "@carbon/react";
import {
  Upload,
  Edit,
  Checkmark,
  Renew,
  CheckmarkFilled,
  Pending,
} from "@carbon/icons-react";
import useGBDPermissions from "../../../../hooks/useGBDPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import { NotificationContext } from "../../../layout/Layout";
import {
  postToOpenElisServer,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import { NotificationKinds } from "../../../../components/common/CustomNotification";
import GBDManifestImportModal from "../../workflow/GBDManifestImportModal";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";

/**
 * GBDSampleReceptionPage - STAGE 1: Sample Reception & Registration
 *
 * Comprehensive sample creation page following Bioanalytical Lab pattern.
 *
 * STAGE 1 Process:
 * ● Receive samples (DNA, RNA, tissues, isolates)
 * ● Register in LMIS with metadata
 * ● Assign to appropriate workflow (extraction, PCR, library prep, sequencing)
 * ● Track volume/concentration and quality metrics if pre-assessed
 *
 * Features:
 * - Single sample creation via form
 * - Bulk sample creation via CSV manifest import
 * - Progress tracking with counts
 * - Sample grid display with bulk selection
 * - Mark received (transition to workflow)
 * - Edit metadata for received samples
 */
export const GBDSampleReceptionPageEnhanced = ({
  samples = [],
  pageData = {},
  entryId,
  onSampleUpdate,
  onSampleStatusChange,
  isLoading = false,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const {
    getPagePermissionLevel,
    canSaveData,
    canRegisterData,
    canPerformWork,
    hasFullControl,
    isReadOnly,
    canAccessRegistration,
    GBD_ROLES,
    GBD_PAGES,
  } = useGBDPermissions();

  // Page access check
  const canAccessPage = canAccessRegistration();

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel(GBD_PAGES.REGISTRATION);

  // Function-level permissions per permission matrix
  // Matrix: Lab Technicians (Yes), Bioinformaticians (View), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
  const canCreateSamples = canRegisterData(pagePermissionLevel); // Lab Technicians (Yes), Lab Manager (Full)
  const canModifyData = canSaveData(pagePermissionLevel);
  const canMarkReceived = canPerformWork(pagePermissionLevel);
  const isViewOnly = isReadOnly(pagePermissionLevel); // Bioinformaticians (View), Principal Investigator (View)

  const componentMounted = useRef(false);
  const [isManifestModalOpen, setIsManifestModalOpen] = useState(false);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [pageSamples, setPageSamples] = useState(samples || []);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      return;
    }

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setPageSamples(response);
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
  }, [pageData?.id, loadPageSamples]);

  const pendingSamples = useMemo(
    () =>
      pageSamples.filter(
        (s) => s.status === "PENDING" || s.status === "AWAITING",
      ),
    [pageSamples],
  );

  const receivedSamples = useMemo(
    () =>
      pageSamples.filter(
        (s) => s.status === "IN_PROGRESS" || s.status === "COMPLETED",
      ),
    [pageSamples],
  );

  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.gbd.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm">
            <FormattedMessage
              id="notebook.gbd.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm" renderIcon={Pending}>
            <FormattedMessage
              id="notebook.gbd.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  const handleManifestImport = useCallback(
    async (importResult) => {
      try {
        if (importResult && importResult.success) {
          setNotificationVisible(true);
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notebook.gbd.manifest.imported",
              defaultMessage: "Manifest Imported",
            }),
            message: intl.formatMessage(
              {
                id: "notebook.gbd.manifest.importedMessage",
                defaultMessage:
                  "{count} samples have been created successfully",
              },
              { count: importResult.totalCreated || 0 },
            ),
          });

          setIsManifestModalOpen(false);

          loadPageSamples();

          if (onSampleUpdate) {
            onSampleUpdate();
          }
        }
      } catch (error) {
        console.error("Error handling manifest import result:", error);
        setNotificationVisible(true);
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notebook.gbd.manifest.importError",
            defaultMessage: "Import Error",
          }),
          message: error.message,
        });
      }
    },
    [
      intl,
      setNotificationVisible,
      addNotification,
      onSampleUpdate,
      loadPageSamples,
    ],
  );

  const handleMarkComplete = useCallback(async () => {
    if (selectedSampleIds.length === 0) {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notebook.gbd.noSamplesSelected.title",
          defaultMessage: "No Samples Selected",
        }),
      });
      return;
    }

    try {
      await postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
          status: "COMPLETED",
        }),
      );

      setSelectedSampleIds([]);
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({
          id: "notebook.gbd.reception.samplesCompleted",
          defaultMessage: "Samples Completed",
        }),
        message: intl.formatMessage(
          {
            id: "notebook.gbd.reception.samplesCompletedMessage",
            defaultMessage:
              "{count} sample(s) marked as complete and moved to the next workflow step",
          },
          { count: selectedSampleIds.length },
        ),
      });

      setTimeout(() => {
        loadPageSamples();
        if (onSampleStatusChange) {
          onSampleStatusChange();
        }
      }, 500);
    } catch (error) {
      console.error("Error marking samples complete:", error);
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.reception.error",
          defaultMessage: "Error",
        }),
        message: error.message,
      });
    }
  }, [
    selectedSampleIds,
    pageData.id,
    intl,
    setNotificationVisible,
    addNotification,
    loadPageSamples,
    onSampleStatusChange,
  ]);

  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Sample Reception & Registration"
        reason="This page requires specific GBD laboratory roles to access."
        requiredRoles={[
          GBD_ROLES.LAB_TECHNICIAN,
          GBD_ROLES.BIOINFORMATICIAN,
          GBD_ROLES.MANAGER,
          GBD_ROLES.PRINCIPAL_INVESTIGATOR,
        ]}
      />
    );
  }

  const isReadOnlyAccess = isViewOnly;

  return (
    <div className="gbd-sample-reception-page">
      {/* Page Section Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.gbd.reception.title"
            defaultMessage="Sample Reception & Registration"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.gbd.reception.description"
            defaultMessage="Register incoming samples and assign to appropriate workflow"
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
                  id="notebook.gbd.reception.awaitingReception"
                  defaultMessage="Awaiting Reception"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.reception.samplesReceived"
                  defaultMessage="Samples Received"
                />
              </span>
              <span className="progress-value">{receivedSamples.length}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="secondary"
          size="sm"
          renderIcon={Upload}
          onClick={() => setIsManifestModalOpen(true)}
          disabled={!canCreateSamples || isViewOnly}
          title={
            !canCreateSamples
              ? intl.formatMessage({
                  id: "notebook.gbd.reception.insufficientPermissions.import",
                  defaultMessage: "Insufficient permissions to import samples. Only Lab Technicians and Lab Manager (with appropriate permissions) can create samples.",
                })
              : isViewOnly
                ? intl.formatMessage({
                    id: "notebook.gbd.reception.viewOnlyAccess",
                    defaultMessage: "You have view-only access to this page.",
                  })
                : undefined
          }
        >
          <FormattedMessage
            id="notebook.gbd.reception.importManifest"
            defaultMessage="Import Manifest"
          />
        </Button>
        <Button
          kind="secondary"
          size="sm"
          renderIcon={Checkmark}
          onClick={handleMarkComplete}
          disabled={
            !canMarkReceived ||
            isViewOnly ||
            selectedSampleIds.length === 0
          }
          title={
            !canMarkReceived
              ? intl.formatMessage({
                  id: "notebook.gbd.reception.insufficientPermissions.complete",
                  defaultMessage: "Insufficient permissions to mark samples complete. Only users with work permissions can complete samples.",
                })
              : isViewOnly
                ? intl.formatMessage({
                    id: "notebook.gbd.reception.viewOnlyAccess",
                    defaultMessage: "You have view-only access to this page.",
                  })
                : undefined
          }
        >
          <FormattedMessage
            id="notebook.gbd.reception.markComplete"
            defaultMessage="Mark as Complete ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.gbd.reception.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Awaiting Reception Section */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.gbd.reception.awaitingReception"
              defaultMessage="Awaiting Reception"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingSamples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.gbd.reception.awaitingDescription"
              defaultMessage="Samples awaiting reception verification. Select samples and mark as received to move them to the completed section."
            />
          </p>
        </div>

        {pendingSamples.length === 0 ? (
          <div className="empty-table-state">
            <FormattedMessage
              id="notebook.gbd.reception.noSamplesAwaiting"
              defaultMessage="No samples awaiting reception"
            />
          </div>
        ) : (
          <>
            <SampleGrid
              gridId="gbd-reception-pending"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              columns={[
                {
                  key: "externalId",
                  header: intl.formatMessage({
                    id: "notebook.gbd.reception.sampleId",
                    defaultMessage: "Sample ID",
                  }),
                },
                {
                  key: "sampleType",
                  header: intl.formatMessage({
                    id: "notebook.gbd.reception.sampleType",
                    defaultMessage: "Sample Type",
                  }),
                  render: (_v, sample) => sample.data?.sampleType || "-",
                },
                {
                  key: "source",
                  header: intl.formatMessage({
                    id: "notebook.gbd.reception.source",
                    defaultMessage: "Source",
                  }),
                  render: (_v, sample) => sample.data?.source || "-",
                },
                {
                  key: "collectionDate",
                  header: intl.formatMessage({
                    id: "notebook.gbd.reception.collectionDate",
                    defaultMessage: "Collection Date",
                  }),
                  render: (_v, sample) => sample.data?.collectionDate || "-",
                },
                {
                  key: "volumeConcentration",
                  header: intl.formatMessage({
                    id: "notebook.gbd.reception.volumeConcentration",
                    defaultMessage: "Concentration",
                  }),
                  render: (_v, sample) =>
                    sample.data?.volumeConcentration || "-",
                },
                {
                  key: "operator",
                  header: intl.formatMessage({
                    id: "notebook.gbd.reception.operator",
                    defaultMessage: "Operator",
                  }),
                  render: (_v, sample) =>
                    sample.data?.operator ||
                    sample.data?.processingMetadata?.operator ||
                    "-",
                },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.gbd.reception.status",
                    defaultMessage: "Status",
                  }),
                  render: (_v, sample) => renderStatus(sample),
                },
              ]}
            />
          </>
        )}
      </div>

      {/* Received Samples Section */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.gbd.reception.samplesReceived"
              defaultMessage="Samples Received"
            />
            <Tag type="green" size="sm" className="count-tag">
              {receivedSamples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.gbd.reception.receivedDescription"
              defaultMessage="Samples that have been received and are ready for the next workflow step."
            />
          </p>
        </div>

        {receivedSamples.length === 0 ? (
          <div className="empty-table-state">
            <FormattedMessage
              id="notebook.gbd.reception.noSamplesReceived"
              defaultMessage="No received samples yet"
            />
          </div>
        ) : (
          <SampleGrid
            gridId="gbd-reception-received"
            samples={receivedSamples}
            selectedIds={[]}
            onSelectionChange={() => {}}
            showSelection={false}
            columns={[
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.gbd.reception.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.gbd.reception.sampleType",
                  defaultMessage: "Sample Type",
                }),
                render: (_v, sample) => sample.data?.sampleType || "-",
              },
              {
                key: "source",
                header: intl.formatMessage({
                  id: "notebook.gbd.reception.source",
                  defaultMessage: "Source",
                }),
                render: (_v, sample) => sample.data?.source || "-",
              },
              {
                key: "collectionDate",
                header: intl.formatMessage({
                  id: "notebook.gbd.reception.collectionDate",
                  defaultMessage: "Collection Date",
                }),
                render: (_v, sample) => sample.data?.collectionDate || "-",
              },
              {
                key: "volumeConcentration",
                header: intl.formatMessage({
                  id: "notebook.gbd.reception.volumeConcentration",
                  defaultMessage: "Concentration",
                }),
                render: (_v, sample) => sample.data?.volumeConcentration || "-",
              },
              {
                key: "operator",
                header: intl.formatMessage({
                  id: "notebook.gbd.reception.operator",
                  defaultMessage: "Operator",
                }),
                render: (_v, sample) =>
                  sample.data?.operator ||
                  sample.data?.processingMetadata?.operator ||
                  "-",
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.gbd.reception.status",
                  defaultMessage: "Status",
                }),
                render: (_v, sample) => renderStatus(sample),
              },
            ]}
          />
        )}
      </div>

      {/* Manifest Import Modal */}
      <GBDManifestImportModal
        open={isManifestModalOpen}
        onClose={() => setIsManifestModalOpen(false)}
        entryId={entryId}
        onImportSuccess={handleManifestImport}
      />
    </div>
  );
};
