import React, { useState, useContext } from "react";
import {
  Grid,
  Column,
  TextInput,
  Button,
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
  Loading,
} from "@carbon/react";
import { Scan, Checkmark, Close } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import PageBreadCrumb from "../common/PageBreadCrumb";
import ShipmentNavigation from "./ShipmentNavigation";
import "./ReceptionWorkflow.css";

const ReceptionWorkflow = () => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [boxId, setBoxId] = useState("");
  const [loading, setLoading] = useState(false);
  const [box, setBox] = useState(null);
  const [samples, setSamples] = useState([]);
  const [sampleStatuses, setSampleStatuses] = useState({});
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [generalNotes, setGeneralNotes] = useState("");

  const handleScanBox = async () => {
    if (!boxId.trim()) {
      addNotification({
        kind: "warning",
        title: intl.formatMessage({ id: "notification.warning" }),
        message: intl.formatMessage({ id: "shipment.reception.enterBoxId" }),
      });
      return;
    }

    setLoading(true);
    try {
      // Fetch box by boxId (not database ID)
      const boxResponse = await getFromOpenElisServer(
        `/rest/shipping-box/by-box-id/${boxId}`,
      );

      if (!boxResponse) {
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "notification.error" }),
          message: intl.formatMessage({ id: "shipment.error.boxNotFound" }),
        });
        return;
      }

      setBox(boxResponse);

      // Fetch samples in this box
      const samplesResponse = await getFromOpenElisServer(
        `/rest/box-sample/by-box/${boxResponse.id}`,
      );

      if (samplesResponse) {
        setSamples(samplesResponse);
        // Initialize all samples as received by default
        const initialStatuses = {};
        samplesResponse.forEach((sample) => {
          initialStatuses[sample.id] = {
            status: "RECEIVED",
            notes: "",
          };
        });
        setSampleStatuses(initialStatuses);
      }
    } catch (error) {
      console.error("Error fetching box:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.fetchBox" }),
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSampleStatusChange = (sampleId, status) => {
    setSampleStatuses((prev) => ({
      ...prev,
      [sampleId]: {
        ...prev[sampleId],
        status,
      },
    }));
  };

  const handleSampleNotesChange = (sampleId, notes) => {
    setSampleStatuses((prev) => ({
      ...prev,
      [sampleId]: {
        ...prev[sampleId],
        notes,
      },
    }));
  };

  const handleConfirmReception = async () => {
    try {
      // Update box state to RECEIVED
      await postToOpenElisServerJsonResponse(
        `/rest/shipping-box/${box.id}/state`,
        JSON.stringify({ state: "RECEIVED" }),
      );

      // Update each sample's reception status
      for (const sample of samples) {
        const status = sampleStatuses[sample.id];
        await postToOpenElisServerJsonResponse(
          `/rest/box-sample/${sample.id}/reception-status`,
          JSON.stringify({
            receptionStatus: status.status,
            receptionNotes: status.notes || generalNotes,
          }),
        );
      }

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({
          id: "shipment.notification.receptionComplete",
        }),
      });

      // Reset form
      setBox(null);
      setSamples([]);
      setSampleStatuses({});
      setBoxId("");
      setGeneralNotes("");
      setShowConfirmModal(false);
    } catch (error) {
      console.error("Error confirming reception:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.confirmReception" }),
      });
    }
  };

  const renderStatusTag = (status) => {
    const config = {
      RECEIVED: { type: "green", label: "Received" },
      DAMAGED: { type: "red", label: "Damaged" },
      MISSING: { type: "gray", label: "Missing" },
    };
    const statusConfig = config[status] || config.RECEIVED;
    return <Tag type={statusConfig.type}>{statusConfig.label}</Tag>;
  };

  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "sample.label.accessionNumber" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "shipment.reception.status" }),
    },
    {
      key: "notes",
      header: intl.formatMessage({ id: "shipment.reception.notes" }),
    },
  ];

  const renderRows = () => {
    return samples.map((sample) => ({
      id: sample.id.toString(),
      accessionNumber: sample.accessionNumber,
      status: (
        <div className="status-buttons">
          <Button
            size="sm"
            kind={
              sampleStatuses[sample.id]?.status === "RECEIVED"
                ? "primary"
                : "ghost"
            }
            renderIcon={Checkmark}
            onClick={() => handleSampleStatusChange(sample.id, "RECEIVED")}
          >
            <FormattedMessage id="shipment.reception.received" />
          </Button>
          <Button
            size="sm"
            kind={
              sampleStatuses[sample.id]?.status === "DAMAGED"
                ? "danger"
                : "ghost"
            }
            onClick={() => handleSampleStatusChange(sample.id, "DAMAGED")}
          >
            <FormattedMessage id="shipment.reception.damaged" />
          </Button>
          <Button
            size="sm"
            kind={
              sampleStatuses[sample.id]?.status === "MISSING"
                ? "ghost"
                : "ghost"
            }
            renderIcon={Close}
            onClick={() => handleSampleStatusChange(sample.id, "MISSING")}
          >
            <FormattedMessage id="shipment.reception.missing" />
          </Button>
        </div>
      ),
      notes: (
        <TextInput
          size="sm"
          placeholder={intl.formatMessage({
            id: "shipment.reception.notesPlaceholder",
          })}
          value={sampleStatuses[sample.id]?.notes || ""}
          onChange={(e) => handleSampleNotesChange(sample.id, e.target.value)}
        />
      ),
    }));
  };

  return (
    <div className="reception-workflow">
      <AlertDialog />
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "shipment.breadcrumb", link: "/SampleShipment" },
          {
            label: "shipment.reception.title",
            link: "/SampleShipment/receive",
          },
        ]}
      />
      <ShipmentNavigation />

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2 className="page-title">
            <FormattedMessage id="shipment.reception.title" />
          </h2>
          <p className="page-description">
            <FormattedMessage id="shipment.reception.description" />
          </p>
        </Column>
      </Grid>

      <Grid fullWidth className="scan-section">
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="boxId"
            labelText={intl.formatMessage({ id: "shipment.reception.scanBox" })}
            placeholder={intl.formatMessage({
              id: "shipment.reception.scanBoxPlaceholder",
            })}
            value={boxId}
            onChange={(e) => setBoxId(e.target.value)}
            onKeyPress={(e) => e.key === "Enter" && handleScanBox()}
            disabled={loading || box !== null}
          />
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Button
            renderIcon={Scan}
            onClick={handleScanBox}
            disabled={loading || box !== null}
            className="scan-button"
          >
            <FormattedMessage id="shipment.reception.scanButton" />
          </Button>
        </Column>
      </Grid>

      {loading && (
        <div className="loading-container">
          <Loading />
        </div>
      )}

      {box && !loading && (
        <Grid fullWidth className="reception-content">
          <Column lg={16} md={8} sm={4}>
            <div className="box-info-card">
              <h3 className="box-info-title">
                <FormattedMessage
                  id="shipment.box.title"
                  values={{ boxId: box.boxId }}
                />
                {renderStatusTag(box.state)}
              </h3>
              <div className="box-info-details">
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage id="shipment.box.destination" />:
                  </span>
                  <span className="info-value">
                    {box.destinationFacilityName}
                  </span>
                </div>
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage id="shipment.box.temperature" />:
                  </span>
                  <span className="info-value">
                    {box.temperatureRequirement}
                  </span>
                </div>
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage id="shipment.label.sampleCount" />:
                  </span>
                  <span className="info-value">{samples.length}</span>
                </div>
              </div>
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <h3 className="section-title">
              <FormattedMessage id="shipment.reception.sampleVerification" />
            </h3>

            <DataTable rows={renderRows()} headers={headers}>
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
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="generalNotes"
              labelText={intl.formatMessage({
                id: "shipment.reception.generalNotes",
              })}
              placeholder={intl.formatMessage({
                id: "shipment.reception.generalNotesPlaceholder",
              })}
              value={generalNotes}
              onChange={(e) => setGeneralNotes(e.target.value)}
              rows={3}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <div className="action-buttons">
              <Button
                kind="secondary"
                onClick={() => {
                  setBox(null);
                  setSamples([]);
                  setSampleStatuses({});
                  setBoxId("");
                  setGeneralNotes("");
                }}
              >
                <FormattedMessage id="label.cancel" />
              </Button>
              <Button kind="primary" onClick={() => setShowConfirmModal(true)}>
                <FormattedMessage id="shipment.reception.confirmReception" />
              </Button>
            </div>
          </Column>
        </Grid>
      )}

      {/* Confirmation Modal */}
      <Modal
        open={showConfirmModal}
        modalHeading={intl.formatMessage({
          id: "shipment.reception.confirmReception",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.confirm" })}
        secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
        onRequestSubmit={handleConfirmReception}
        onRequestClose={() => setShowConfirmModal(false)}
      >
        <p>
          <FormattedMessage
            id="shipment.reception.confirmMessage"
            values={{ count: samples.length }}
          />
        </p>
      </Modal>
    </div>
  );
};

export default ReceptionWorkflow;
