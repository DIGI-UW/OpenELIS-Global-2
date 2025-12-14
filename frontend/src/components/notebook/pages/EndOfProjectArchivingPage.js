import React, { useState, useEffect, useRef, useCallback } from "react";
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
import {
  Archive,
  CheckmarkFilled,
  DocumentExport,
  Reset,
  TrashCan,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";
import TraceabilityChecklist from "../workflow/TraceabilityChecklist";
import "../workflow/NotebookWorkflow.css";

/**
 * EndOfProjectArchivingPage - Page 9 of the immunology workflow.
 * Handles end-of-project archiving, biorepository transfer, and notebook finalization.
 *
 * US8: Once project concludes, transfer remaining Parent and Child Samples to
 * Biorepository Laboratory with permanent storage logged and complete traceability
 * links verified.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function EndOfProjectArchivingPage({
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
    parent: [],
    child: [],
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

  // Storage location selection (simplified for biorepository)
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
          setArchivableSamples(response || { parent: [], child: [] });
          // Build sample list for grid
          const allSamples = [
            ...(response?.parent || []).map((id) => ({
              sampleItemId: id,
              type: "parent",
            })),
            ...(response?.child || []).map((id) => ({
              sampleItemId: id,
              type: "child",
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
        : [...archivableSamples.parent, ...archivableSamples.child];

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
                id="notebook.archive.title"
                defaultMessage="End of Project Archiving"
              />
            </h3>
            <p className="page-description">
              <FormattedMessage
                id="notebook.archive.description"
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
                id="notebook.archive.progress"
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
                      id: "notebook.archive.progressHelper",
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
                    <Tag type="blue">
                      <FormattedMessage
                        id="notebook.archive.parentSamples"
                        defaultMessage="Parent Samples"
                      />
                    </Tag>
                    <span>
                      {archivingProgress.archivedParents || 0} /{" "}
                      {archivingProgress.parentSamples || 0}
                    </span>
                  </div>
                  <div className="progress-item">
                    <Tag type="teal">
                      <FormattedMessage
                        id="notebook.archive.childSamples"
                        defaultMessage="Child Samples"
                      />
                    </Tag>
                    <span>
                      {archivingProgress.archivedChildren || 0} /{" "}
                      {archivingProgress.childSamples || 0}
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
                id="notebook.archive.actions"
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
                  id="notebook.archive.verifyTraceability"
                  defaultMessage="Verify Traceability"
                />
              </Button>
              <Button
                kind="primary"
                onClick={() => setTransferModalOpen(true)}
                disabled={
                  archivableSamples.parent.length === 0 &&
                  archivableSamples.child.length === 0
                }
                renderIcon={Archive}
              >
                <FormattedMessage
                  id="notebook.archive.transferToBiorepository"
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
                  id="notebook.archive.finalize"
                  defaultMessage="Finalize Notebook"
                />
              </Button>
            </div>
            {!canFinalize && traceabilityResult && (
              <p className="action-helper-text">
                <FormattedMessage
                  id="notebook.archive.cannotFinalize"
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
                id="notebook.archive.samplesList"
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
                        <Tag type={sample.type === "parent" ? "blue" : "teal"}>
                          {sample.type === "parent" ? "Parent" : "Child"}
                        </Tag>
                      </StructuredListCell>
                    </StructuredListRow>
                  ))}
                </StructuredListBody>
              </StructuredListWrapper>
            ) : (
              <p className="no-samples-message">
                <FormattedMessage
                  id="notebook.archive.noSamplesPending"
                  defaultMessage="All samples have been archived."
                />
              </p>
            )}
            {samples.length > 20 && (
              <p className="samples-truncated">
                <FormattedMessage
                  id="notebook.archive.moreSamples"
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
          id: "notebook.archive.transferModal.title",
          defaultMessage: "Transfer to Biorepository",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.archive.transfer",
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
              id="notebook.archive.transferModal.description"
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
              id: "notebook.archive.transferNotes",
              defaultMessage: "Transfer Notes",
            })}
            placeholder={intl.formatMessage({
              id: "notebook.archive.transferNotesPlaceholder",
              defaultMessage: "Optional notes about this transfer",
            })}
            value={transferNotes}
            onChange={(e) => setTransferNotes(e.target.value)}
          />
          <p className="transfer-count">
            <FormattedMessage
              id="notebook.archive.transferCount"
              defaultMessage="{count} samples will be transferred"
              values={{
                count:
                  selectedSampleIds.length > 0
                    ? selectedSampleIds.length
                    : archivableSamples.parent.length +
                      archivableSamples.child.length,
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
          id: "notebook.archive.finalizeModal.title",
          defaultMessage: "Finalize Notebook",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.archive.finalize",
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
              id: "notebook.archive.finalizeWarning.title",
              defaultMessage: "This action is irreversible",
            })}
            subtitle={intl.formatMessage({
              id: "notebook.archive.finalizeWarning.subtitle",
              defaultMessage:
                "Once finalized, the notebook cannot be modified. All samples must be archived and traceability verified.",
            })}
            hideCloseButton
            lowContrast
          />
          <div className="finalize-summary">
            <h5>
              <FormattedMessage
                id="notebook.archive.finalizeSummary"
                defaultMessage="Finalization Summary"
              />
            </h5>
            <ul>
              <li>
                <FormattedMessage
                  id="notebook.archive.totalSamples"
                  defaultMessage="Total Samples: {count}"
                  values={{ count: archivingProgress?.totalSamples || 0 }}
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.archive.archivedSamples"
                  defaultMessage="Archived Samples: {count}"
                  values={{ count: archivingProgress?.archivedSamples || 0 }}
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.archive.traceabilityStatus"
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
              id: "notebook.archive.confirmFinalize",
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

export default EndOfProjectArchivingPage;
