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
import useVirologyLabPermissions from "../../../../hooks/useVirologyLabPermissions";
import { NotificationContext } from "../../../layout/Layout";
import {
  postToOpenElisServer,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import { NotificationKinds } from "../../../../components/common/CustomNotification";
import VirologyLabManifestImportModal from "../../workflow/VirologyLabManifestImportModal";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";

/**
 * VirologyLabSampleReceptionPage - STAGE 1: Sample Reception & Registration
 *
 * Comprehensive sample creation page following Virology Lab pattern.
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
export const VirologyLabSampleReceptionPageEnhanced = ({
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
  const { getPagePermissionLevel, canSaveData, canAccessSampleReception } =
    useVirologyLabPermissions();

  const allowedRoles = [
    "VirologyLab Lab Technician",
    "VirologyLab Bioinformatician",
    "VirologyLab Manager",
    "VirologyLab PI",
  ];

  const canAccessPage = canAccessSampleReception();
  const pagePermissionLevel = getPagePermissionLevel(
    "Sample Reception & Registration",
  );
  const canCreateSamples = canSaveData(pagePermissionLevel);

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
  }, [loadPageSamples]);

  const handleManifestImportSuccess = useCallback((result) => {
    if (result && result.createdSampleIds) {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({
          id: "notebook.virologylab.sampleReception.manifestImportSuccess",
          defaultMessage: "Manifest Import Successful",
        }),
        message: intl.formatMessage(
          {
            id: "notebook.virologylab.sampleReception.manifestImportSuccessMessage",
            defaultMessage:
              "Successfully created {count} samples from manifest",
          },
          { count: result.totalCreated || result.createdSampleIds.length },
        ),
      });

      // Refresh samples and close modal
      loadPageSamples();
      setIsManifestModalOpen(false);
      if (onSampleUpdate) {
        onSampleUpdate();
      }
    }
  }, [intl, addNotification, loadPageSamples, onSampleUpdate]);

  const handleMarkReceived = useCallback(() => {
    if (!selectedSampleIds.length || !entryId) return;

    const payload = {
      sampleIds: selectedSampleIds,
      status: "received",
      pageId: pageData?.id,
    };

    postToOpenElisServer(
      `/rest/notebook/entry/${entryId}/samples/mark-received`,
      JSON.stringify(payload),
      (response) => {
        if (response && !response.error) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notebook.virologylab.sampleReception.markReceivedSuccess",
              defaultMessage: "Samples Marked as Received",
            }),
            message: intl.formatMessage(
              {
                id: "notebook.virologylab.sampleReception.markReceivedSuccessMessage",
                defaultMessage: "Successfully marked {count} samples as received",
              },
              { count: selectedSampleIds.length },
            ),
          });

          setSelectedSampleIds([]);
          loadPageSamples();
          if (onSampleStatusChange) {
            onSampleStatusChange();
          }
        }
      },
    );
  }, [
    selectedSampleIds,
    entryId,
    pageData?.id,
    intl,
    addNotification,
    loadPageSamples,
    onSampleStatusChange,
  ]);

  const sampleStateCounts = useMemo(() => {
    const counts = { pending: 0, received: 0, total: 0 };

    (pageSamples || samples).forEach((sample) => {
      counts.total++;
      if (sample.status === "received") {
        counts.received++;
      } else {
        counts.pending++;
      }
    });

    return counts;
  }, [pageSamples, samples]);

  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        message={intl.formatMessage({
          id: "notebook.virologylab.sampleReception.accessDenied",
          defaultMessage:
            "You do not have permission to access VirologyLab Sample Reception. Required roles: VirologyLab Lab Technician, VirologyLab Manager, or VirologyLab PI.",
        })}
        allowedRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="notebook-page-content virologylab-sample-reception">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header-section">
            <div className="page-title-row">
              <h4>
                <FormattedMessage
                  id="notebook.virologylab.sampleReception.title"
                  defaultMessage="VirologyLab Sample Reception & Registration"
                />
              </h4>
              <div className="page-status-indicators">
                <Tag type="blue" size="sm">
                  <FormattedMessage
                    id="notebook.virologylab.sampleReception.stage"
                    defaultMessage="Stage 1"
                  />
                </Tag>
                <Tag type="outline" size="sm">
                  {sampleStateCounts.total}{" "}
                  <FormattedMessage
                    id="label.samples"
                    defaultMessage="samples"
                  />
                </Tag>
              </div>
            </div>

            <div className="progress-summary-tiles">
              <Tile className="progress-tile pending-tile">
                <div className="tile-content">
                  <div className="tile-value">{sampleStateCounts.pending}</div>
                  <div className="tile-label">
                    <Pending size={16} />
                    <FormattedMessage
                      id="notebook.virologylab.sampleReception.pendingReception"
                      defaultMessage="Pending Reception"
                    />
                  </div>
                </div>
              </Tile>

              <Tile className="progress-tile received-tile">
                <div className="tile-content">
                  <div className="tile-value">{sampleStateCounts.received}</div>
                  <div className="tile-label">
                    <CheckmarkFilled size={16} />
                    <FormattedMessage
                      id="notebook.virologylab.sampleReception.received"
                      defaultMessage="Received"
                    />
                  </div>
                </div>
              </Tile>
            </div>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="action-section">
        <Column lg={16} md={8} sm={4}>
          <div className="action-buttons">
            <Button
              kind="primary"
              size="sm"
              renderIcon={Upload}
              onClick={() => setIsManifestModalOpen(true)}
              disabled={!canCreateSamples || isLoading}
            >
              <FormattedMessage
                id="notebook.virologylab.sampleReception.importManifest"
                defaultMessage="Import Manifest"
              />
            </Button>

            {selectedSampleIds.length > 0 && (
              <Button
                kind="secondary"
                size="sm"
                renderIcon={Checkmark}
                onClick={handleMarkReceived}
                disabled={!canCreateSamples || isLoading}
              >
                <FormattedMessage
                  id="notebook.virologylab.sampleReception.markReceived"
                  defaultMessage="Mark as Received ({count})"
                  values={{ count: selectedSampleIds.length }}
                />
              </Button>
            )}

            <Button
              kind="ghost"
              size="sm"
              renderIcon={Renew}
              onClick={loadPageSamples}
              disabled={isLoading}
            >
              <FormattedMessage
                id="label.refresh"
                defaultMessage="Refresh"
              />
            </Button>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="samples-grid-section">
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            samples={pageSamples || samples}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            isLoading={isLoading}
            showReceiveActions={canCreateSamples}
            pageContext="virologylab-reception"
            entryId={entryId}
            onSampleUpdate={onSampleUpdate}
          />
        </Column>
      </Grid>

      {isManifestModalOpen && (
        <VirologyLabManifestImportModal
          open={isManifestModalOpen}
          onClose={() => setIsManifestModalOpen(false)}
          entryId={entryId}
          onImportSuccess={handleManifestImportSuccess}
        />
      )}
    </div>
  );
};

export default VirologyLabSampleReceptionPageEnhanced;