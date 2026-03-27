import {
  Add,
  Checkmark,
  Document,
  Download,
  TrashCan,
} from "@carbon/icons-react";
import {
  Button,
  Column,
  DataTable,
  Grid,
  Loading,
  Modal,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useParams } from "react-router-dom";
import config from "../../config.json";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import {
  getFromOpenElisServerV2,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import "./BoxDetails.css";
import SampleAssignmentModal from "./SampleAssignmentModal";
import ShipmentNavigation from "./ShipmentNavigation";
import { generateLabelPDF, generateManifestPDF } from "./utils/pdfGenerator";

const BoxDetails = () => {
  const intl = useIntl();
  const { boxId } = useParams();
  const { addNotification } = useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [box, setBox] = useState(null);
  const [samples, setSamples] = useState([]);
  const [showAddSampleModal, setShowAddSampleModal] = useState(false);
  const [showReadyModal, setShowReadyModal] = useState(false);
  const [showSendModal, setShowSendModal] = useState(false);
  const [showRemoveModal, setShowRemoveModal] = useState(false);
  const [sampleToRemove, setSampleToRemove] = useState(null);

  useEffect(() => {
    if (boxId) {
      fetchBoxDetails();
      fetchBoxSamples();
    } else {
      setLoading(false);
    }
  }, [boxId]);

  const fetchBoxDetails = async () => {
    try {
      const response = await getFromOpenElisServerV2(
        `/rest/shipping-box/${boxId}`,
      );
      if (response) {
        setBox(response);
      }
    } catch (error) {
      console.error("Error fetching box details:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "error.fetch.boxDetails" }),
      });
    } finally {
      setLoading(false);
    }
  };

  const fetchBoxSamples = async () => {
    try {
      // Use new SampleItem-based endpoint
      const response = await getFromOpenElisServerV2(
        `/rest/box-sample/items/by-box/${boxId}`,
      );
      if (response) {
        setSamples(response);
      }
    } catch (error) {
      console.error("Error fetching box samples:", error);
    }
  };

  const handleAddSample = (sampleItemId) => {
    postToOpenElisServerJsonResponse(
      "/rest/box-sample/items",
      JSON.stringify({
        shippingBoxId: boxId,
        sampleItemId: sampleItemId,
      }),
      (response) => {
        if (response.error) {
          console.error("Error adding sample:", response);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message:
              response.message ||
              intl.formatMessage({ id: "shipment.error.addSample" }),
          });
        } else {
          addNotification({
            kind: "success",
            title: intl.formatMessage({ id: "notification.success" }),
            message: intl.formatMessage({
              id: "shipment.notification.sampleAdded",
            }),
          });
          fetchBoxSamples();
          setShowAddSampleModal(false);
        }
      },
    );
  };

  const handleRemoveSampleClick = (sample) => {
    setSampleToRemove(sample);
    setShowRemoveModal(true);
  };

  const handleRemoveSample = () => {
    if (!sampleToRemove) return;

    postToOpenElisServerJsonResponse(
      `/rest/box-sample/items/${sampleToRemove.sampleItemId || sampleToRemove.id}/remove`,
      JSON.stringify({}),
      (response) => {
        if (response.error) {
          console.error("Error removing sample:", response);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message:
              response.message ||
              intl.formatMessage({ id: "shipment.error.removeSample" }),
          });
        } else {
          addNotification({
            kind: "success",
            title: intl.formatMessage({ id: "notification.success" }),
            message: intl.formatMessage({
              id: "shipment.notification.sampleRemoved",
            }),
          });
          fetchBoxSamples();
          setShowRemoveModal(false);
          setSampleToRemove(null);
        }
      },
    );
  };

  const handleMarkReadyToSend = async () => {
    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/shipping-box/${boxId}/state?newState=READY_TO_SEND`,
        {
          credentials: "include",
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
        },
      );

      if (!response.ok) {
        throw new Error(intl.formatMessage({ id: "shipment.error.markReady" }));
      }

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({
          id: "shipment.notification.boxReadyToSend",
        }),
      });

      fetchBoxDetails();
      setShowReadyModal(false);
    } catch (error) {
      console.error("Error marking box as ready:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.markReady" }),
      });
    }
  };

  const handleSendBox = async () => {
    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/shipping-box/${boxId}/state?newState=SENT`,
        {
          credentials: "include",
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
        },
      );

      if (!response.ok) {
        throw new Error(intl.formatMessage({ id: "shipment.error.sendBox" }));
      }

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({
          id: "shipment.notification.boxSent",
        }),
      });

      // Auto-generate packing list on send (spec User Story 4)
      try {
        const manifestResponse = await getFromOpenElisServerV2(
          `/rest/shipping-box/${boxId}/manifest-data`,
        );
        if (manifestResponse) {
          await generateManifestPDF(manifestResponse, intl.formatMessage);
        }
      } catch {
        // Non-blocking — manifest can be regenerated later
      }

      fetchBoxDetails();
      setShowSendModal(false);
    } catch (error) {
      console.error("Error sending box:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.sendBox" }),
      });
    }
  };

  const handleDownloadLabel = async () => {
    try {
      const response = await getFromOpenElisServerV2(
        `/rest/shipping-box/${boxId}`,
      );
      if (response) {
        // Compute sample type counts: { "Serum": 3, "Plasma": 2, ... }
        const typeCounts = {};
        samples.forEach((s) => {
          const type =
            s.typeOfSample && s.typeOfSample !== "-"
              ? s.typeOfSample
              : "Unknown";
          typeCounts[type] = (typeCounts[type] || 0) + 1;
        });

        generateLabelPDF(
          {
            boxId: response.boxId,
            destinationFacility: response.destinationFacilityName,
            temperature: response.temperatureRequirement,
            sampleCount: samples.length,
            sampleTypeCounts: typeCounts,
            createdDate: response.createdDate,
          },
          intl.formatMessage,
        );
      }
    } catch (error) {
      console.error("Error generating label:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.generateLabel" }),
      });
    }
  };

  const handleDownloadManifest = async ({ isResend = false } = {}) => {
    try {
      const response = await getFromOpenElisServerV2(
        `/rest/shipping-box/${boxId}/manifest-data`,
      );
      if (response) {
        // Enforce time-based rules (Rule 3): regeneration ≤24h, recall ≤7d
        if (isResend && response.canRegenerate === false) {
          addNotification({
            kind: "warning",
            title: intl.formatMessage({ id: "notification.warning" }),
            message: intl.formatMessage({
              id: "shipment.manifest.regenerationExpired",
            }),
          });
          return;
        }
        if (isResend && response.canRecall === false) {
          addNotification({
            kind: "warning",
            title: intl.formatMessage({ id: "notification.warning" }),
            message: intl.formatMessage({
              id: "shipment.manifest.recallExpired",
            }),
          });
          return;
        }
        await generateManifestPDF(response, intl.formatMessage);
      }
    } catch (error) {
      console.error("Error generating manifest:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({
          id: "shipment.error.generateManifest",
        }),
      });
    }
  };

  const renderStateTag = (state) => {
    const stateConfig = {
      DRAFT: {
        type: "gray",
        label: intl.formatMessage({ id: "shipment.state.draft" }),
      },
      READY_TO_SEND: {
        type: "blue",
        label: intl.formatMessage({ id: "shipment.state.readyToSend" }),
      },
      SENT: {
        type: "purple",
        label: intl.formatMessage({ id: "shipment.state.sent" }),
      },
      RECEIVED: {
        type: "green",
        label: intl.formatMessage({ id: "shipment.state.received" }),
      },
      RECONCILED: {
        type: "teal",
        label: intl.formatMessage({ id: "shipment.state.reconciled" }),
      },
    };

    const cfg = stateConfig[state] || stateConfig.DRAFT;
    return <Tag type={cfg.type}>{cfg.label}</Tag>;
  };

  const renderReceptionTag = (status) => {
    const cfg = {
      PENDING: { type: "gray", id: "shipment.reception.pending" },
      RECEIVED_GOOD: { type: "green", id: "shipment.reception.received" },
      RECEIVED_DAMAGED: { type: "red", id: "shipment.reception.damaged" },
      RECEIVED_LEAKED: { type: "magenta", id: "shipment.reception.leaked" },
      MISSING: { type: "gray", id: "shipment.reception.missing" },
      REJECTED: { type: "purple", id: "shipment.reception.rejected" },
    };
    const c = cfg[status] || cfg.PENDING;
    return <Tag type={c.type}>{intl.formatMessage({ id: c.id })}</Tag>;
  };

  const handleReconcile = async () => {
    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/shipping-box/${boxId}/state?newState=RECONCILED`,
        {
          credentials: "include",
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
        },
      );

      if (!response.ok) {
        throw new Error("Failed to reconcile");
      }

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({
          id: "shipment.notification.boxReconciled",
        }),
      });
      fetchBoxDetails();
    } catch (error) {
      console.error("Error reconciling box:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.reconcile" }),
      });
    }
  };

  const sampleHeaders = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "sample.label.accessionNumber" }),
    },
    {
      key: "typeOfSample",
      header: intl.formatMessage({ id: "sample.label.typeOfSample" }),
    },
    {
      key: "referralTests",
      header: intl.formatMessage({ id: "shipment.label.tests" }),
    },
    {
      key: "collectionDate",
      header: intl.formatMessage({ id: "sample.label.collectionDate" }),
    },
    {
      key: "receptionStatus",
      header: intl.formatMessage({ id: "shipment.reception.status" }),
    },
    {
      key: "receptionNotes",
      header: intl.formatMessage({ id: "shipment.reception.notes" }),
    },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  const renderSampleRows = () => {
    return samples.map((sample) => ({
      id: sample.sampleItemId || sample.id?.toString() || "-",
      accessionNumber: sample.accessionNumber,
      typeOfSample: sample.typeOfSample || "-",
      referralTests: sample.referralTests
        ? sample.referralTests.map((t) => t.testName).join(", ")
        : "-",
      collectionDate: sample.collectionDate
        ? new Date(sample.collectionDate).toLocaleDateString()
        : "-",
      receptionStatus: sample.receptionStatus
        ? renderReceptionTag(sample.receptionStatus)
        : renderReceptionTag("PENDING"),
      receptionNotes: sample.receptionNotes || "-",
      actions: (
        <Button
          kind="ghost"
          size="sm"
          renderIcon={TrashCan}
          onClick={() => handleRemoveSampleClick(sample)}
          disabled={box?.state !== "DRAFT"}
          iconDescription={intl.formatMessage({ id: "label.remove" })}
        >
          <FormattedMessage id="label.remove" />
        </Button>
      ),
    }));
  };

  if (loading) {
    return (
      <div className="loading-container">
        <Loading />
      </div>
    );
  }

  if (!box) {
    return (
      <div className="error-container">
        <p>
          <FormattedMessage id="shipment.error.boxNotFound" />
        </p>
      </div>
    );
  }

  return (
    <div className="box-details">
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "shipment.breadcrumb", link: "/SampleShipment" },
        ]}
      />
      <ShipmentNavigation />

      <Grid fullWidth className="box-details-content">
        <Column lg={16} md={8} sm={4}>
          <div className="box-header">
            <div>
              <h2 className="box-title">
                <FormattedMessage
                  id="shipment.box.title"
                  values={{ boxId: box.boxId }}
                />
              </h2>
              <div className="box-meta">
                {renderStateTag(box.state)}
                <span className="box-sample-count">
                  {samples.length}{" "}
                  <FormattedMessage id="shipment.label.samples" />
                </span>
              </div>
            </div>
            <div className="box-actions">
              {box.state === "DRAFT" && (
                <>
                  <Button
                    kind="secondary"
                    renderIcon={Add}
                    onClick={() => setShowAddSampleModal(true)}
                  >
                    <FormattedMessage id="shipment.box.addSample" />
                  </Button>
                  <Button
                    kind="primary"
                    renderIcon={Checkmark}
                    onClick={() => setShowReadyModal(true)}
                    disabled={samples.length === 0}
                  >
                    <FormattedMessage id="shipment.box.readyToSend" />
                  </Button>
                </>
              )}
              {box.state === "READY_TO_SEND" && (
                <>
                  <Button
                    kind="primary"
                    renderIcon={Checkmark}
                    onClick={() => setShowSendModal(true)}
                  >
                    <FormattedMessage id="shipment.box.sendBox" />
                  </Button>
                  <Button
                    kind="tertiary"
                    renderIcon={Download}
                    onClick={handleDownloadLabel}
                  >
                    <FormattedMessage id="shipment.box.downloadLabel" />
                  </Button>
                  <Button
                    kind="tertiary"
                    renderIcon={Document}
                    onClick={handleDownloadManifest}
                  >
                    <FormattedMessage id="shipment.box.downloadManifest" />
                  </Button>
                </>
              )}
              {(box.state === "SENT" || box.state === "IN_TRANSIT") && (
                <>
                  <Button
                    kind="tertiary"
                    renderIcon={Download}
                    onClick={handleDownloadLabel}
                  >
                    <FormattedMessage id="shipment.box.downloadLabel" />
                  </Button>
                  <Button
                    kind="tertiary"
                    renderIcon={Document}
                    onClick={() => handleDownloadManifest({ isResend: true })}
                  >
                    <FormattedMessage id="shipment.box.resendManifest" />
                  </Button>
                </>
              )}
              {box.state === "RECEIVED" && (
                <>
                  <Button
                    kind="primary"
                    renderIcon={Checkmark}
                    onClick={handleReconcile}
                  >
                    <FormattedMessage id="shipment.box.reconcile" />
                  </Button>
                  <Button
                    kind="tertiary"
                    renderIcon={Download}
                    onClick={handleDownloadLabel}
                  >
                    <FormattedMessage id="shipment.box.downloadLabel" />
                  </Button>
                  <Button
                    kind="tertiary"
                    renderIcon={Document}
                    onClick={handleDownloadManifest}
                  >
                    <FormattedMessage id="shipment.box.downloadManifest" />
                  </Button>
                </>
              )}
              {box.state === "RECONCILED" && (
                <>
                  <Button
                    kind="tertiary"
                    renderIcon={Download}
                    onClick={handleDownloadLabel}
                  >
                    <FormattedMessage id="shipment.box.downloadLabel" />
                  </Button>
                  <Button
                    kind="tertiary"
                    renderIcon={Document}
                    onClick={handleDownloadManifest}
                  >
                    <FormattedMessage id="shipment.box.downloadManifest" />
                  </Button>
                </>
              )}
            </div>
          </div>

          <div className="box-info">
            <div className="info-item">
              <span className="info-label">
                <FormattedMessage id="shipment.box.destination" />:
              </span>
              <span className="info-value">{box.destinationFacilityName}</span>
            </div>
            <div className="info-item">
              <span className="info-label">
                <FormattedMessage id="shipment.box.temperature" />:
              </span>
              <span className="info-value">{box.temperatureRequirement}</span>
            </div>
            <div className="info-item">
              <span className="info-label">
                <FormattedMessage id="shipment.box.createdBy" />
              </span>
              <span className="info-value">{box.createdByName || "-"}</span>
            </div>
            <div className="info-item">
              <span className="info-label">
                <FormattedMessage id="shipment.box.created" />:
              </span>
              <span className="info-value">
                {box.createdDate
                  ? new Date(box.createdDate).toLocaleString()
                  : "-"}
              </span>
            </div>
            {box.notes && (
              <div className="info-item">
                <span className="info-label">
                  <FormattedMessage id="shipment.box.notes" />:
                </span>
                <span className="info-value">{box.notes}</span>
              </div>
            )}
          </div>

          <div className="samples-section">
            <h3 className="section-title">
              <FormattedMessage id="shipment.label.samples" />
            </h3>

            {samples.length === 0 ? (
              <div className="empty-state">
                <p>
                  <FormattedMessage id="shipment.box.noSamples" />
                </p>
                {box.state === "DRAFT" && (
                  <Button
                    kind="tertiary"
                    onClick={() => setShowAddSampleModal(true)}
                  >
                    <FormattedMessage id="shipment.box.addSample" />
                  </Button>
                )}
              </div>
            ) : (
              <DataTable rows={renderSampleRows()} headers={sampleHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
                            <TableHeader
                              {...getHeaderProps({ header })}
                              key={header.key}
                            >
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.map((row) => (
                          <TableRow {...getRowProps({ row })} key={row.id}>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
            )}
          </div>
        </Column>
      </Grid>

      {/* Sample Assignment Modal */}
      {showAddSampleModal && (
        <SampleAssignmentModal
          open={showAddSampleModal}
          onClose={() => setShowAddSampleModal(false)}
          onAddSample={handleAddSample}
          destinationFacilityId={box.destinationFacilityId}
        />
      )}

      {/* Ready to Send Confirmation Modal */}
      <Modal
        open={showReadyModal}
        modalHeading={intl.formatMessage({ id: "shipment.box.readyToSend" })}
        primaryButtonText={intl.formatMessage({ id: "label.confirm" })}
        secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
        onRequestSubmit={handleMarkReadyToSend}
        onRequestClose={() => setShowReadyModal(false)}
      >
        <p>
          <FormattedMessage id="shipment.box.readyConfirmation" />
        </p>
        <p>
          <FormattedMessage
            id="shipment.box.readySampleCount"
            values={{ count: samples.length }}
          />
        </p>
      </Modal>

      {/* Send Box Confirmation Modal */}
      <Modal
        open={showSendModal}
        modalHeading={intl.formatMessage({ id: "shipment.box.sendBox" })}
        primaryButtonText={intl.formatMessage({ id: "label.confirm" })}
        secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
        onRequestSubmit={handleSendBox}
        onRequestClose={() => setShowSendModal(false)}
      >
        <p>
          <FormattedMessage id="shipment.box.sendConfirmation" />
        </p>
        <p>
          <FormattedMessage
            id="shipment.box.sendSampleCount"
            values={{ count: samples.length }}
          />
        </p>
      </Modal>

      {/* Remove Sample Confirmation Modal */}
      <Modal
        open={showRemoveModal}
        modalHeading={intl.formatMessage({ id: "shipment.box.removeSample" })}
        primaryButtonText={intl.formatMessage({ id: "label.remove" })}
        secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
        onRequestSubmit={handleRemoveSample}
        onRequestClose={() => {
          setShowRemoveModal(false);
          setSampleToRemove(null);
        }}
        danger
      >
        <p>
          <FormattedMessage id="shipment.box.removeSampleConfirmation" />
        </p>
        {sampleToRemove && (
          <p>
            <strong>
              <FormattedMessage id="sample.label.accessionNumber" />:
            </strong>{" "}
            {sampleToRemove.accessionNumber}
          </p>
        )}
      </Modal>
    </div>
  );
};

export default BoxDetails;
