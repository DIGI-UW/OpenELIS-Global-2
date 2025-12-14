import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Button,
  Modal,
  Form,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Tag,
  Loading,
  InlineNotification,
  Tile,
} from "@carbon/react";
import { Add, View, Checkmark, WarningAlt, ArrowUp } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";

const EnvironmentalExcursions = () => {
  const intl = useIntl();
  const [excursions, setExcursions] = useState([]);
  const [activeExcursions, setActiveExcursions] = useState([]);
  const [unacknowledged, setUnacknowledged] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isRecordModalOpen, setIsRecordModalOpen] = useState(false);
  const [isAcknowledgeModalOpen, setIsAcknowledgeModalOpen] = useState(false);
  const [isResolveModalOpen, setIsResolveModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [selectedExcursion, setSelectedExcursion] = useState(null);
  const [formData, setFormData] = useState({
    deviceId: "",
    alertType: "TEMPERATURE_HIGH",
    recordedValue: "",
    thresholdValue: "",
    deviceLocation: "",
  });
  const [acknowledgeNotes, setAcknowledgeNotes] = useState("");
  const [resolutionNotes, setResolutionNotes] = useState("");

  const fetchExcursions = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/pharmaceutical/excursions", (response) => {
      if (response) {
        setExcursions(response);
      }
      setLoading(false);
    });
  }, []);

  const fetchActiveExcursions = useCallback(() => {
    getFromOpenElisServer(
      "/rest/pharmaceutical/excursions/active",
      (response) => {
        if (response) {
          setActiveExcursions(response);
        }
      },
    );
  }, []);

  const fetchUnacknowledged = useCallback(() => {
    getFromOpenElisServer(
      "/rest/pharmaceutical/excursions/unacknowledged",
      (response) => {
        if (response) {
          setUnacknowledged(response);
        }
      },
    );
  }, []);

  useEffect(() => {
    fetchExcursions();
    fetchActiveExcursions();
    fetchUnacknowledged();
  }, [fetchExcursions, fetchActiveExcursions, fetchUnacknowledged]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleRecordExcursion = () => {
    postToOpenElisServer(
      "/rest/pharmaceutical/excursions/record",
      JSON.stringify({
        ...formData,
        deviceId: parseInt(formData.deviceId),
        recordedValue: parseFloat(formData.recordedValue),
        thresholdValue: parseFloat(formData.thresholdValue),
      }),
      (response) => {
        if (response) {
          setIsRecordModalOpen(false);
          setFormData({
            deviceId: "",
            alertType: "TEMPERATURE_HIGH",
            recordedValue: "",
            thresholdValue: "",
            deviceLocation: "",
          });
          fetchExcursions();
          fetchActiveExcursions();
          fetchUnacknowledged();
        } else {
          setError("Failed to record excursion");
        }
      },
    );
  };

  const handleAcknowledge = () => {
    if (!selectedExcursion) return;

    postToOpenElisServer(
      `/rest/pharmaceutical/excursions/${selectedExcursion.id}/acknowledge?notes=${encodeURIComponent(
        acknowledgeNotes,
      )}`,
      null,
      (response) => {
        if (response) {
          setIsAcknowledgeModalOpen(false);
          setSelectedExcursion(null);
          setAcknowledgeNotes("");
          fetchExcursions();
          fetchActiveExcursions();
          fetchUnacknowledged();
        } else {
          setError("Failed to acknowledge excursion");
        }
      },
    );
  };

  const handleResolve = () => {
    if (!selectedExcursion) return;

    postToOpenElisServer(
      `/rest/pharmaceutical/excursions/${selectedExcursion.id}/resolve?resolutionNotes=${encodeURIComponent(
        resolutionNotes,
      )}`,
      null,
      (response) => {
        if (response) {
          setIsResolveModalOpen(false);
          setSelectedExcursion(null);
          setResolutionNotes("");
          fetchExcursions();
          fetchActiveExcursions();
        } else {
          setError("Failed to resolve excursion");
        }
      },
    );
  };

  const handleEscalate = (excursionId, reason) => {
    postToOpenElisServer(
      `/rest/pharmaceutical/excursions/${excursionId}/escalate?escalationReason=${encodeURIComponent(
        reason,
      )}`,
      null,
      (response) => {
        if (response) {
          fetchExcursions();
          fetchActiveExcursions();
        } else {
          setError("Failed to escalate excursion");
        }
      },
    );
  };

  const openAcknowledgeModal = (excursion) => {
    setSelectedExcursion(excursion);
    setIsAcknowledgeModalOpen(true);
  };

  const openResolveModal = (excursion) => {
    setSelectedExcursion(excursion);
    setIsResolveModalOpen(true);
  };

  const openViewModal = (excursion) => {
    setSelectedExcursion(excursion);
    setIsViewModalOpen(true);
  };

  const getStatusTag = (status) => {
    const statusColors = {
      ACTIVE: "red",
      ACKNOWLEDGED: "orange",
      RESOLVED: "green",
      ESCALATED: "purple",
    };
    return <Tag type={statusColors[status] || "gray"}>{status}</Tag>;
  };

  const getAlertTypeTag = (alertType) => {
    const alertColors = {
      TEMPERATURE_HIGH: "red",
      TEMPERATURE_LOW: "blue",
      HUMIDITY_HIGH: "cyan",
      HUMIDITY_LOW: "orange",
      POWER_FAILURE: "purple",
      DOOR_OPEN: "magenta",
    };
    return <Tag type={alertColors[alertType] || "gray"}>{alertType}</Tag>;
  };

  const headers = [
    {
      key: "id",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.id" }),
    },
    {
      key: "deviceId",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.device" }),
    },
    {
      key: "alertType",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.alertType" }),
    },
    {
      key: "recordedValue",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.recorded" }),
    },
    {
      key: "thresholdValue",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.threshold" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.actions" }),
    },
  ];

  const rows = excursions.map((excursion) => ({
    id: String(excursion.id),
    deviceId: excursion.deviceId,
    alertType: excursion.alertType,
    recordedValue: excursion.recordedValue,
    thresholdValue: excursion.thresholdValue,
    status: excursion.status,
    actions: excursion,
  }));

  if (loading) {
    return <Loading />;
  }

  return (
    <Grid>
      <Column lg={16} md={8} sm={4}>
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={error}
            onCloseButtonClick={() => setError(null)}
            className="pharmaceutical-alert"
          />
        )}

        {unacknowledged.length > 0 && (
          <Tile className="pharmaceutical-alert pharmaceutical-alert--critical">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <WarningAlt size={24} />
              <strong>
                <FormattedMessage id="pharmaceutical.excursion.unacknowledged.alert" />
              </strong>
            </div>
            <p>
              {unacknowledged.length}{" "}
              <FormattedMessage id="pharmaceutical.excursion.unacknowledged.count" />
            </p>
            <div style={{ marginTop: "0.5rem" }}>
              {unacknowledged.slice(0, 3).map((exc) => (
                <div
                  key={exc.id}
                  className="excursion-card excursion-card--active"
                  style={{ padding: "0.5rem", marginBottom: "0.5rem" }}
                >
                  <strong>Device {exc.deviceId}</strong> - {exc.alertType}
                  <br />
                  <small>
                    {exc.recordedValue} (Threshold: {exc.thresholdValue})
                  </small>
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={() => openAcknowledgeModal(exc)}
                    style={{ marginLeft: "1rem" }}
                  >
                    <FormattedMessage id="pharmaceutical.excursion.acknowledge" />
                  </Button>
                </div>
              ))}
            </div>
          </Tile>
        )}

        {activeExcursions.length > 0 && unacknowledged.length === 0 && (
          <Tile className="pharmaceutical-alert pharmaceutical-alert--warning">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <WarningAlt size={20} />
              <strong>
                <FormattedMessage id="pharmaceutical.excursion.active.alert" />
              </strong>
            </div>
            <p>
              {activeExcursions.length}{" "}
              <FormattedMessage id="pharmaceutical.excursion.active.count" />
            </p>
          </Tile>
        )}

        <DataTable rows={rows} headers={headers}>
          {({
            rows,
            headers,
            getTableProps,
            getHeaderProps,
            getRowProps,
            onInputChange,
          }) => (
            <>
              <TableToolbar>
                <TableToolbarContent>
                  <TableToolbarSearch onChange={onInputChange} />
                  <Button
                    renderIcon={Add}
                    onClick={() => setIsRecordModalOpen(true)}
                  >
                    <FormattedMessage id="pharmaceutical.excursion.record" />
                  </Button>
                </TableToolbarContent>
              </TableToolbar>
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader
                        key={header.key}
                        {...getHeaderProps({ header })}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status" ? (
                            getStatusTag(cell.value)
                          ) : cell.info.header === "alertType" ? (
                            getAlertTypeTag(cell.value)
                          ) : cell.info.header === "actions" ? (
                            <div className="pharmaceutical-actions">
                              <Button
                                kind="ghost"
                                size="sm"
                                renderIcon={View}
                                iconDescription="View"
                                hasIconOnly
                                onClick={() => openViewModal(cell.value)}
                              />
                              {cell.value.status === "ACTIVE" && (
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  renderIcon={Checkmark}
                                  iconDescription="Acknowledge"
                                  hasIconOnly
                                  onClick={() =>
                                    openAcknowledgeModal(cell.value)
                                  }
                                />
                              )}
                              {(cell.value.status === "ACTIVE" ||
                                cell.value.status === "ACKNOWLEDGED") && (
                                <>
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    onClick={() => openResolveModal(cell.value)}
                                  >
                                    <FormattedMessage id="pharmaceutical.excursion.resolve" />
                                  </Button>
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={ArrowUp}
                                    iconDescription="Escalate"
                                    hasIconOnly
                                    onClick={() =>
                                      handleEscalate(
                                        cell.value.id,
                                        "Requires immediate attention",
                                      )
                                    }
                                  />
                                </>
                              )}
                            </div>
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </>
          )}
        </DataTable>

        {/* Record Excursion Modal */}
        <Modal
          open={isRecordModalOpen}
          onRequestClose={() => setIsRecordModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.excursion.record.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.save" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleRecordExcursion}
        >
          <Form className="pharmaceutical-form">
            <TextInput
              id="deviceId"
              name="deviceId"
              labelText={intl.formatMessage({
                id: "pharmaceutical.excursion.device",
              })}
              value={formData.deviceId}
              onChange={handleInputChange}
              required
            />
            <Select
              id="alertType"
              name="alertType"
              labelText={intl.formatMessage({
                id: "pharmaceutical.excursion.alertType",
              })}
              value={formData.alertType}
              onChange={handleInputChange}
            >
              <SelectItem value="TEMPERATURE_HIGH" text="Temperature High" />
              <SelectItem value="TEMPERATURE_LOW" text="Temperature Low" />
              <SelectItem value="HUMIDITY_HIGH" text="Humidity High" />
              <SelectItem value="HUMIDITY_LOW" text="Humidity Low" />
              <SelectItem value="POWER_FAILURE" text="Power Failure" />
              <SelectItem value="DOOR_OPEN" text="Door Open" />
            </Select>
            <TextInput
              id="recordedValue"
              name="recordedValue"
              labelText={intl.formatMessage({
                id: "pharmaceutical.excursion.recorded",
              })}
              value={formData.recordedValue}
              onChange={handleInputChange}
              required
            />
            <TextInput
              id="thresholdValue"
              name="thresholdValue"
              labelText={intl.formatMessage({
                id: "pharmaceutical.excursion.threshold",
              })}
              value={formData.thresholdValue}
              onChange={handleInputChange}
              required
            />
            <TextInput
              id="deviceLocation"
              name="deviceLocation"
              labelText={intl.formatMessage({
                id: "pharmaceutical.excursion.location",
              })}
              value={formData.deviceLocation}
              onChange={handleInputChange}
            />
          </Form>
        </Modal>

        {/* Acknowledge Modal */}
        <Modal
          open={isAcknowledgeModalOpen}
          onRequestClose={() => setIsAcknowledgeModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.excursion.acknowledge.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.confirm" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleAcknowledge}
        >
          <Form className="pharmaceutical-form">
            {selectedExcursion && (
              <div style={{ marginBottom: "1rem" }}>
                <p>
                  <strong>
                    <FormattedMessage id="pharmaceutical.excursion.device" />:
                  </strong>{" "}
                  {selectedExcursion.deviceId}
                </p>
                <p>
                  <strong>
                    <FormattedMessage id="pharmaceutical.excursion.alertType" />
                    :
                  </strong>{" "}
                  {getAlertTypeTag(selectedExcursion.alertType)}
                </p>
                <p>
                  <strong>
                    <FormattedMessage id="pharmaceutical.excursion.recorded" />:
                  </strong>{" "}
                  {selectedExcursion.recordedValue}
                </p>
              </div>
            )}
            <TextArea
              id="acknowledgeNotes"
              labelText={intl.formatMessage({
                id: "pharmaceutical.excursion.acknowledge.notes",
              })}
              value={acknowledgeNotes}
              onChange={(e) => setAcknowledgeNotes(e.target.value)}
              rows={4}
            />
          </Form>
        </Modal>

        {/* Resolve Modal */}
        <Modal
          open={isResolveModalOpen}
          onRequestClose={() => setIsResolveModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.excursion.resolve.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.confirm" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleResolve}
        >
          <Form className="pharmaceutical-form">
            <TextArea
              id="resolutionNotes"
              labelText={intl.formatMessage({
                id: "pharmaceutical.excursion.resolve.notes",
              })}
              value={resolutionNotes}
              onChange={(e) => setResolutionNotes(e.target.value)}
              rows={4}
              required
            />
          </Form>
        </Modal>

        {/* View Excursion Modal */}
        <Modal
          open={isViewModalOpen}
          onRequestClose={() => setIsViewModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.excursion.details.title",
          })}
          passiveModal
        >
          {selectedExcursion && (
            <div>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.excursion.id" />:
                </strong>{" "}
                {selectedExcursion.id}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.excursion.device" />:
                </strong>{" "}
                {selectedExcursion.deviceId}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.excursion.location" />:
                </strong>{" "}
                {selectedExcursion.deviceLocation}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.excursion.alertType" />:
                </strong>{" "}
                {getAlertTypeTag(selectedExcursion.alertType)}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.excursion.recorded" />:
                </strong>{" "}
                {selectedExcursion.recordedValue}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.excursion.threshold" />:
                </strong>{" "}
                {selectedExcursion.thresholdValue}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.excursion.status" />:
                </strong>{" "}
                {getStatusTag(selectedExcursion.status)}
              </p>
              {selectedExcursion.notes && (
                <div style={{ marginTop: "1rem" }}>
                  <h5>
                    <FormattedMessage id="pharmaceutical.excursion.notes" />
                  </h5>
                  <p>{selectedExcursion.notes}</p>
                </div>
              )}
            </div>
          )}
        </Modal>
      </Column>
    </Grid>
  );
};

export default EnvironmentalExcursions;
