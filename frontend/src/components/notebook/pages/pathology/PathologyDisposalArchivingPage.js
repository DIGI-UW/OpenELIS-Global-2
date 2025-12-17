import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  Dropdown,
  TextInput,
  Tag,
  Loading,
  ProgressBar,
  Checkbox,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
} from "@carbon/react";
import { Archive, CheckmarkFilled, DocumentExport } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import TraceabilityChecklist from "../../workflow/TraceabilityChecklist";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyDisposalArchivingPage - Page 7 of the pathology workflow.
 * Handles end-of-project archiving, biorepository transfer, and notebook finalization.
 *
 * Purpose: Close the sample lifecycle in compliance with policy.
 * Who uses it: Lab manager / quality officer
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PathologyDisposalArchivingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Archiving progress state
  const [archivingProgress, setArchivingProgress] = useState(null);
  const [archivableSamples, setArchivableSamples] = useState({
    tissue: [],
    fluid: [],
  });

  // Traceability state
  const [traceabilityResult, setTraceabilityResult] = useState(null);
  const [verifyingTraceability, setVerifyingTraceability] = useState(false);

  // Transfer modal state
  const [transferModalOpen, setTransferModalOpen] = useState(false);
  const [transferring, setTransferring] = useState(false);

  // Finalization modal state
  const [finalizeModalOpen, setFinalizeModalOpen] = useState(false);
  const [finalizing, setFinalizing] = useState(false);
  const [confirmFinalize, setConfirmFinalize] = useState(false);

  // Storage location selection (for biorepository)
  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [transferNotes, setTransferNotes] = useState("");

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadPageData();
    loadRooms();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageData = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);

    // Load archiving progress
    getFromOpenElisServer(
      `/rest/notebook/${entryId}/archive/progress`,
      (response) => {
        if (componentMounted.current) {
          setArchivingProgress(response);
        }
      },
    );

    // Load archivable samples
    getFromOpenElisServer(
      `/rest/notebook/${entryId}/archive/samples`,
      (response) => {
        if (componentMounted.current) {
          setArchivableSamples(response || { tissue: [], fluid: [] });
          // Build sample list for display
          const allSamples = [
            ...(response?.tissue || []).map((id) => ({
              sampleItemId: id,
              type: "tissue",
            })),
            ...(response?.fluid || []).map((id) => ({
              sampleItemId: id,
              type: "fluid",
            })),
          ];
          setSamples(allSamples);
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  const loadRooms = () => {
    getFromOpenElisServer("/rest/storage/rooms", (response) => {
      if (componentMounted.current) {
        const roomOptions =
          response?.map((room) => ({
            id: room.id.toString(),
            label: room.name,
          })) || [];
        setRooms(roomOptions);
      }
    });
  };

  const loadDevices = (roomId) => {
    if (!roomId) {
      setDevices([]);
      return;
    }
    getFromOpenElisServer(
      `/rest/storage/devices?roomId=${roomId}`,
      (response) => {
        if (componentMounted.current) {
          const deviceOptions =
            response?.map((device) => ({
              id: device.id.toString(),
              label: device.name,
            })) || [];
          setDevices(deviceOptions);
        }
      },
    );
  };

  const handleRoomChange = ({ selectedItem }) => {
    setSelectedRoom(selectedItem);
    setSelectedDevice(null);
    if (selectedItem) {
      loadDevices(selectedItem.id);
    } else {
      setDevices([]);
    }
  };

  const handleVerifyTraceability = async () => {
    setVerifyingTraceability(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/archive/verify-traceability`,
      {},
      (response) => {
        if (componentMounted.current) {
          setTraceabilityResult(response);
          setVerifyingTraceability(false);
        }
      },
      () => {
        if (componentMounted.current) {
          setError(
            intl.formatMessage({
              id: "notebook.archive.verifyError",
              defaultMessage: "Failed to verify traceability",
            }),
          );
          setVerifyingTraceability(false);
        }
      },
    );
  };

  const handleTransferToBiorepository = async () => {
    if (!selectedDevice) {
      setError(
        intl.formatMessage({
          id: "notebook.archive.selectLocation",
          defaultMessage: "Please select a biorepository location",
        }),
      );
      return;
    }

    setTransferring(true);
    setError(null);

    const sampleIds =
      selectedSampleIds.length > 0
        ? selectedSampleIds
        : [...archivableSamples.tissue, ...archivableSamples.fluid];

    const requestBody = {
      sampleItemIds: sampleIds,
      locationId: selectedDevice.id,
      locationType: "device",
      notes: transferNotes || "End of project transfer to biorepository",
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/archive/transfer`,
      JSON.stringify(requestBody),
      (response) => {
        if (componentMounted.current) {
          if (response.success) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "notebook.archive.transferSuccess",
                  defaultMessage:
                    "{count} samples transferred to biorepository",
                },
                { count: response.transferredCount },
              ),
            );
            setTransferModalOpen(false);
            loadPageData(); // Reload to update progress
            if (onProgressUpdate) onProgressUpdate();
          } else {
            setError(response.error || "Transfer failed");
          }
          setTransferring(false);
        }
      },
      () => {
        if (componentMounted.current) {
          setError(
            intl.formatMessage({
              id: "notebook.archive.transferError",
              defaultMessage: "Failed to transfer samples",
            }),
          );
          setTransferring(false);
        }
      },
    );
  };

  const handleFinalize = async () => {
    if (!confirmFinalize) {
      setError(
        intl.formatMessage({
          id: "notebook.archive.confirmRequired",
          defaultMessage:
            "Please confirm that you want to finalize this notebook",
        }),
      );
      return;
    }

    setFinalizing(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/archive/finalize`,
      {},
      (response) => {
        if (componentMounted.current) {
          if (response.success) {
            setSuccess(
              intl.formatMessage({
                id: "notebook.archive.finalizeSuccess",
                defaultMessage: "Notebook has been finalized successfully",
              }),
            );
            setFinalizeModalOpen(false);
            if (onProgressUpdate) onProgressUpdate();
          } else {
            setError(response.error || "Finalization failed");
          }
          setFinalizing(false);
        }
      },
      () => {
        if (componentMounted.current) {
          setError(
            intl.formatMessage({
              id: "notebook.archive.finalizeError",
              defaultMessage: "Failed to finalize notebook",
            }),
          );
          setFinalizing(false);
        }
      },
    );
  };

  const canFinalize =
    traceabilityResult?.passed && archivingProgress?.readyForFinalization;

  // Render loading state
  if (loading) {
    return (
      <div className="page-loading">
        <Loading withOverlay={false} />
        <p>
          <FormattedMessage
            id="notebook.page.loading"
            defaultMessage="Loading page data..."
          />
        </p>
      </div>
    );
  }

  return (
    <div className="notebook-page archiving-page">
      <Grid>
        {/* Header */}
        <Column lg={16} md={8} sm={4}>
          <div className="page-header">
            <h3>
              <Archive size={24} />
              <FormattedMessage
                id="pathology.archive.title"
                defaultMessage="Disposal & Archiving"
              />
            </h3>
            <p className="page-description">
              <FormattedMessage
                id="pathology.archive.description"
                defaultMessage="Transfer samples to biorepository and finalize the notebook with complete traceability verification."
              />
            </p>
          </div>
        </Column>

        {/* Notifications */}
        {error && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "error",
                defaultMessage: "Error",
              })}
              subtitle={error}
              onCloseButtonClick={() => setError(null)}
            />
          </Column>
        )}

        {success && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="success"
              title={intl.formatMessage({
                id: "success",
                defaultMessage: "Success",
              })}
              subtitle={success}
              onCloseButtonClick={() => setSuccess(null)}
            />
          </Column>
        )}

        {/* Progress Summary */}
        <Column lg={8} md={4} sm={4}>
          <Tile className="archiving-progress-tile">
            <h4>
              <FormattedMessage
                id="pathology.archive.progress"
                defaultMessage="Archiving Progress"
              />
            </h4>
            {archivingProgress && (
              <>
                <ProgressBar
                  value={archivingProgress.percentComplete || 0}
                  max={100}
                  label={`${Math.round(archivingProgress.percentComplete || 0)}%`}
                  helperText={intl.formatMessage(
                    {
                      id: "pathology.archive.progressHelper",
                      defaultMessage: "{archived} of {total} samples archived",
                    },
                    {
                      archived: archivingProgress.archivedSamples || 0,
                      total: archivingProgress.totalSamples || 0,
                    },
                  )}
                />
                <div className="progress-details">
                  <div className="progress-item">
                    <Tag type="purple">
                      <FormattedMessage
                        id="pathology.archive.tissueSamples"
                        defaultMessage="Tissue Samples"
                      />
                    </Tag>
                    <span>
                      {archivingProgress.archivedTissue || 0} /{" "}
                      {archivingProgress.tissueSamples || 0}
                    </span>
                  </div>
                  <div className="progress-item">
                    <Tag type="cyan">
                      <FormattedMessage
                        id="pathology.archive.fluidSamples"
                        defaultMessage="Fluid Samples"
                      />
                    </Tag>
                    <span>
                      {archivingProgress.archivedFluid || 0} /{" "}
                      {archivingProgress.fluidSamples || 0}
                    </span>
                  </div>
                </div>
              </>
            )}
          </Tile>
        </Column>

        {/* Actions */}
        <Column lg={8} md={4} sm={4}>
          <Tile className="archiving-actions-tile">
            <h4>
              <FormattedMessage
                id="pathology.archive.actions"
                defaultMessage="Actions"
              />
            </h4>
            <div className="action-buttons">
              <Button
                kind="secondary"
                onClick={handleVerifyTraceability}
                disabled={verifyingTraceability}
                renderIcon={CheckmarkFilled}
              >
                <FormattedMessage
                  id="pathology.archive.verifyTraceability"
                  defaultMessage="Verify Traceability"
                />
              </Button>
              <Button
                kind="primary"
                onClick={() => setTransferModalOpen(true)}
                disabled={
                  archivableSamples.tissue.length === 0 &&
                  archivableSamples.fluid.length === 0
                }
                renderIcon={Archive}
              >
                <FormattedMessage
                  id="pathology.archive.transferToBiorepository"
                  defaultMessage="Transfer to Biorepository"
                />
              </Button>
              <Button
                kind="danger"
                onClick={() => setFinalizeModalOpen(true)}
                disabled={!canFinalize}
                renderIcon={DocumentExport}
              >
                <FormattedMessage
                  id="pathology.archive.finalize"
                  defaultMessage="Finalize Notebook"
                />
              </Button>
            </div>
            {!canFinalize && traceabilityResult && (
              <p className="action-helper-text">
                <FormattedMessage
                  id="pathology.archive.cannotFinalize"
                  defaultMessage="Resolve traceability issues before finalizing."
                />
              </p>
            )}
          </Tile>
        </Column>

        {/* Traceability Checklist */}
        <Column lg={16} md={8} sm={4}>
          <TraceabilityChecklist
            traceabilityResult={traceabilityResult}
            loading={verifyingTraceability}
          />
        </Column>

        {/* Sample List */}
        <Column lg={16} md={8} sm={4}>
          <Tile className="samples-tile">
            <h4>
              <FormattedMessage
                id="pathology.archive.samplesList"
                defaultMessage="Samples Pending Archive"
              />
            </h4>
            {samples.length > 0 ? (
              <StructuredListWrapper>
                <StructuredListHead>
                  <StructuredListRow head>
                    <StructuredListCell head>
                      <FormattedMessage
                        id="notebook.sample.id"
                        defaultMessage="Sample ID"
                      />
                    </StructuredListCell>
                    <StructuredListCell head>
                      <FormattedMessage
                        id="notebook.sample.type"
                        defaultMessage="Type"
                      />
                    </StructuredListCell>
                  </StructuredListRow>
                </StructuredListHead>
                <StructuredListBody>
                  {samples.slice(0, 20).map((sample, index) => (
                    <StructuredListRow key={index}>
                      <StructuredListCell>
                        {sample.sampleItemId}
                      </StructuredListCell>
                      <StructuredListCell>
                        <Tag
                          type={sample.type === "tissue" ? "purple" : "cyan"}
                        >
                          {sample.type === "tissue" ? "Tissue" : "Fluid"}
                        </Tag>
                      </StructuredListCell>
                    </StructuredListRow>
                  ))}
                </StructuredListBody>
              </StructuredListWrapper>
            ) : (
              <p className="no-samples-message">
                <FormattedMessage
                  id="pathology.archive.noSamplesPending"
                  defaultMessage="All samples have been archived."
                />
              </p>
            )}
            {samples.length > 20 && (
              <p className="samples-truncated">
                <FormattedMessage
                  id="pathology.archive.moreSamples"
                  defaultMessage="... and {count} more samples"
                  values={{ count: samples.length - 20 }}
                />
              </p>
            )}
          </Tile>
        </Column>
      </Grid>

      {/* Transfer Modal */}
      <Modal
        open={transferModalOpen}
        onRequestClose={() => setTransferModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.archive.transferModal.title",
          defaultMessage: "Transfer to Biorepository",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.archive.transfer",
          defaultMessage: "Transfer",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleTransferToBiorepository}
        primaryButtonDisabled={!selectedDevice || transferring}
      >
        {transferring && <Loading withOverlay />}
        <div className="transfer-form">
          <p>
            <FormattedMessage
              id="pathology.archive.transferModal.description"
              defaultMessage="Select the biorepository location for permanent sample storage."
            />
          </p>
          <Dropdown
            id="room-select"
            titleText={intl.formatMessage({
              id: "notebook.storage.room",
              defaultMessage: "Room",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectRoom",
              defaultMessage: "Select a room",
            })}
            items={rooms}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedRoom}
            onChange={handleRoomChange}
          />
          <Dropdown
            id="device-select"
            titleText={intl.formatMessage({
              id: "notebook.storage.device",
              defaultMessage: "Device / Unit",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectDevice",
              defaultMessage: "Select a device",
            })}
            items={devices}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedDevice}
            onChange={({ selectedItem }) => setSelectedDevice(selectedItem)}
            disabled={!selectedRoom}
          />
          <TextInput
            id="transfer-notes"
            labelText={intl.formatMessage({
              id: "pathology.archive.transferNotes",
              defaultMessage: "Transfer Notes",
            })}
            placeholder={intl.formatMessage({
              id: "pathology.archive.transferNotesPlaceholder",
              defaultMessage: "Optional notes about this transfer",
            })}
            value={transferNotes}
            onChange={(e) => setTransferNotes(e.target.value)}
          />
          <p className="transfer-count">
            <FormattedMessage
              id="pathology.archive.transferCount"
              defaultMessage="{count} samples will be transferred"
              values={{
                count:
                  selectedSampleIds.length > 0
                    ? selectedSampleIds.length
                    : archivableSamples.tissue.length +
                      archivableSamples.fluid.length,
              }}
            />
          </p>
        </div>
      </Modal>

      {/* Finalize Modal */}
      <Modal
        open={finalizeModalOpen}
        onRequestClose={() => {
          setFinalizeModalOpen(false);
          setConfirmFinalize(false);
        }}
        modalHeading={intl.formatMessage({
          id: "pathology.archive.finalizeModal.title",
          defaultMessage: "Finalize Notebook",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.archive.finalize",
          defaultMessage: "Finalize",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleFinalize}
        primaryButtonDisabled={!confirmFinalize || finalizing}
        danger
      >
        {finalizing && <Loading withOverlay />}
        <div className="finalize-form">
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "pathology.archive.finalizeWarning.title",
              defaultMessage: "This action is irreversible",
            })}
            subtitle={intl.formatMessage({
              id: "pathology.archive.finalizeWarning.subtitle",
              defaultMessage:
                "Once finalized, the notebook cannot be modified. All samples must be archived and traceability verified.",
            })}
            hideCloseButton
            lowContrast
          />
          <div className="finalize-summary">
            <h5>
              <FormattedMessage
                id="pathology.archive.finalizeSummary"
                defaultMessage="Finalization Summary"
              />
            </h5>
            <ul>
              <li>
                <FormattedMessage
                  id="pathology.archive.totalSamples"
                  defaultMessage="Total Samples: {count}"
                  values={{ count: archivingProgress?.totalSamples || 0 }}
                />
              </li>
              <li>
                <FormattedMessage
                  id="pathology.archive.archivedSamples"
                  defaultMessage="Archived Samples: {count}"
                  values={{ count: archivingProgress?.archivedSamples || 0 }}
                />
              </li>
              <li>
                <FormattedMessage
                  id="pathology.archive.traceabilityStatus"
                  defaultMessage="Traceability: {status}"
                  values={{
                    status: traceabilityResult?.passed
                      ? "Verified"
                      : "Not Verified",
                  }}
                />
              </li>
            </ul>
          </div>
          <Checkbox
            id="confirm-finalize"
            labelText={intl.formatMessage({
              id: "pathology.archive.confirmFinalize",
              defaultMessage:
                "I confirm that all samples have been properly archived and traceability has been verified. I understand this action cannot be undone.",
            })}
            checked={confirmFinalize}
            onChange={(_, { checked }) => setConfirmFinalize(checked)}
          />
        </div>
      </Modal>
    </div>
  );
}

export default PathologyDisposalArchivingPage;
