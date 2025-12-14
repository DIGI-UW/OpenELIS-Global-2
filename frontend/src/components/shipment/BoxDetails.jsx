import React, { useState, useEffect, useContext } from "react";
import {
  Grid,
  Column,
  Button,
  Loading,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Modal,
  TextArea,
} from "@carbon/react";
import {
  Add,
  TrashCan,
  Checkmark,
  Download,
  Document,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { useParams } from "react-router-dom";
import {
  getFromOpenElisServerV2,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import config from "../../config.json";
import { NotificationContext } from "../layout/Layout";
import PageBreadCrumb from "../common/PageBreadCrumb";
import ShipmentNavigation from "./ShipmentNavigation";
import SampleAssignmentModal from "./SampleAssignmentModal";
import "./BoxDetails.css";

const BoxDetails = () => {
  const intl = useIntl();
  const { boxId } = useParams();
  const { addNotification } = useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [box, setBox] = useState(null);
  const [samples, setSamples] = useState([]);
  const [showAddSampleModal, setShowAddSampleModal] = useState(false);
  const [showReadyModal, setShowReadyModal] = useState(false);

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
      const response = await getFromOpenElisServerV2(
        `/rest/box-sample/by-box/${boxId}`,
      );
      if (response) {
        setSamples(response);
      }
    } catch (error) {
      console.error("Error fetching box samples:", error);
    }
  };

  const handleAddSample = async (sampleId) => {
    try {
      await postToOpenElisServerJsonResponse(
        "/rest/box-sample",
        JSON.stringify({
          shippingBoxId: boxId,
          sampleId: sampleId,
        }),
      );

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({
          id: "shipment.notification.sampleAdded",
        }),
      });

      fetchBoxSamples();
      setShowAddSampleModal(false);
    } catch (error) {
      console.error("Error adding sample:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message:
          error.message ||
          intl.formatMessage({ id: "shipment.error.addSample" }),
      });
    }
  };

  const handleRemoveSample = async (boxSampleId) => {
    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/box-sample/${boxSampleId}`,
        {
          credentials: "include",
          method: "DELETE",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
        },
      );

      if (!response.ok) {
        throw new Error("Failed to remove sample");
      }

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({
          id: "shipment.notification.sampleRemoved",
        }),
      });

      fetchBoxSamples();
    } catch (error) {
      console.error("Error removing sample:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.removeSample" }),
      });
    }
  };

  const handleMarkReadyToSend = async () => {
    try {
      await postToOpenElisServerJsonResponse(
        `/rest/shipping-box/${boxId}/state`,
        JSON.stringify({ state: "READY_TO_SEND" }),
      );

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

  const handleDownloadLabel = () => {
    window.open(`/rest/shipping-box/${boxId}/label/pdf`, "_blank");
  };

  const handleDownloadManifest = () => {
    window.open(`/rest/shipping-box/${boxId}/manifest/pdf`, "_blank");
  };

  const renderStateTag = (state) => {
    const stateConfig = {
      DRAFT: { type: "gray", label: "Draft" },
      READY_TO_SEND: { type: "blue", label: "Ready to Send" },
      SENT: { type: "purple", label: "Sent" },
      RECEIVED: { type: "green", label: "Received" },
      RECONCILED: { type: "teal", label: "Reconciled" },
    };

    const config = stateConfig[state] || stateConfig.DRAFT;
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  const sampleHeaders = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "sample.label.accessionNumber" }),
    },
    {
      key: "positionInBox",
      header: intl.formatMessage({ id: "shipment.label.position" }),
    },
    {
      key: "addedDate",
      header: intl.formatMessage({ id: "shipment.label.addedDate" }),
    },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  const renderSampleRows = () => {
    return samples.map((sample) => ({
      id: sample.id.toString(),
      accessionNumber: sample.accessionNumber,
      positionInBox: sample.positionInBox || "-",
      addedDate: sample.addedDate
        ? new Date(sample.addedDate).toLocaleDateString()
        : "-",
      actions: (
        <Button
          kind="ghost"
          size="sm"
          renderIcon={TrashCan}
          onClick={() => handleRemoveSample(sample.id)}
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
          { label: "shipment.box.title", link: `/SampleShipment/box/${boxId}` },
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
              {(box.state === "READY_TO_SEND" ||
                box.state === "SENT" ||
                box.state === "RECEIVED" ||
                box.state === "RECONCILED") && (
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
    </div>
  );
};

export default BoxDetails;
