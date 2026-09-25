import React, { useState, useContext, useEffect, useCallback } from "react";
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
  Dropdown,
  Loading,
  Select,
  SelectItem,
} from "@carbon/react";
import { Scan, Checkmark, CloudDownload, View } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServerV2,
  postToOpenElisServerFullResponse,
  putToOpenElisServer,
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
  const [submitting, setSubmitting] = useState(false);
  const [importing, setImporting] = useState(false);
  const [sampleScanInput, setSampleScanInput] = useState("");
  const [validationErrors, setValidationErrors] = useState({});
  const [expectedSpecimens, setExpectedSpecimens] = useState([]);
  const [incomingBoxes, setIncomingBoxes] = useState([]);

  const fetchIncomingBoxes = useCallback(() => {
    getFromOpenElisServerV2("/rest/shipping-box/incoming").then((data) => {
      setIncomingBoxes(Array.isArray(data) ? data : []);
    });
  }, []);

  useEffect(() => {
    fetchIncomingBoxes();
  }, [fetchIncomingBoxes]);

  const handleOpenIncomingBox = (incomingBox) => {
    setBoxId(incomingBox.boxId);
    handleScanBox(incomingBox.boxId);
  };

  // Build the reception-status map keyed by boxSampleItemId (required by the PUT
  // endpoint); default to RECEIVED_GOOD unless the backend already set a status.
  const buildStatuses = (list) => {
    const statuses = {};
    list.forEach((sample) => {
      const sampleKey =
        sample.boxSampleItemId || sample.sampleItemId || sample.id;
      const hasBeenReceived =
        sample.receptionStatus && sample.receptionStatus !== "PENDING";
      statuses[sampleKey] = {
        status: hasBeenReceived ? sample.receptionStatus : "RECEIVED_GOOD",
        notes: sample.receptionNotes || "",
      };
    });
    return statuses;
  };

  const handleImportFromFhir = () => {
    setImporting(true);
    postToOpenElisServerFullResponse(
      "/rest/shipping-box/import-from-fhir",
      JSON.stringify({}),
      async (response) => {
        try {
          if (response.ok) {
            const result = await response.json();
            const count = result.imported || 0;
            addNotification({
              kind: count > 0 ? "success" : "info",
              title: intl.formatMessage({ id: "notification.success" }),
              message: intl.formatMessage(
                { id: "shipment.reception.importResult" },
                { count },
              ),
            });
            fetchIncomingBoxes();
          } else {
            addNotification({
              kind: "error",
              title: intl.formatMessage({ id: "notification.error" }),
              message: intl.formatMessage({
                id: "shipment.reception.importError",
              }),
            });
          }
        } catch (error) {
          console.error("Error importing from FHIR:", error);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message: intl.formatMessage({
              id: "shipment.reception.importError",
            }),
          });
        } finally {
          setImporting(false);
        }
      },
    );
  };

  const handleScanBox = async (scanId) => {
    const idToScan = (scanId ?? boxId).trim();
    if (!idToScan) {
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
      const boxResponse = await getFromOpenElisServerV2(
        `/rest/shipping-box/by-box-id/${encodeURIComponent(idToScan)}`,
      );

      if (!boxResponse) {
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "notification.error" }),
          message: intl.formatMessage({ id: "shipment.error.boxNotFound" }),
        });
        setLoading(false);
        return;
      }

      setBox(boxResponse);

      // Fetch sample items in this box using new SampleItem-based API
      const samplesResponse = await getFromOpenElisServerV2(
        `/rest/box-sample/items/by-box/${boxResponse.id}`,
      );

      if (samplesResponse && Array.isArray(samplesResponse)) {
        setSamples(samplesResponse);
        setSampleStatuses(buildStatuses(samplesResponse));
      }

      // Resolve declared specimens to their orders and surface ones awaiting acceptance.
      // Reconciliation may auto-link specimens, creating new box_sample_item rows,
      // so re-fetch samples afterwards to pick up any newly linked items.
      await reconcileExpectedSpecimens(boxResponse.id);

      const refreshedSamples = await getFromOpenElisServerV2(
        `/rest/box-sample/items/by-box/${boxResponse.id}`,
      );
      if (refreshedSamples && Array.isArray(refreshedSamples)) {
        setSamples(refreshedSamples);
        setSampleStatuses(buildStatuses(refreshedSamples));
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

  const reconcileExpectedSpecimens = (shippingBoxDbId) =>
    new Promise((resolve) => {
      postToOpenElisServerFullResponse(
        `/rest/box-sample/items/reconcile-shipment/${shippingBoxDbId}`,
        JSON.stringify({}),
        async (response) => {
          try {
            const result = response.ok ? await response.json() : [];
            // LINKED ones show in the sample verification table; list only the rest here.
            setExpectedSpecimens(
              (Array.isArray(result) ? result : []).filter(
                (s) => s.status !== "LINKED",
              ),
            );
          } catch (error) {
            console.error("Error reconciling shipment specimens:", error);
            setExpectedSpecimens([]);
          } finally {
            resolve();
          }
        },
      );
    });

  const handleAcceptSpecimen = (specimen) => {
    // Open the pre-filled sample-entry form; re-scanning after save links the sample to the box.
    window.location.href = `/SamplePatientEntry?ID=${encodeURIComponent(
      specimen.externalOrderNumber,
    )}`;
  };

  const handleSampleStatusChange = (sampleKey, status) => {
    setSampleStatuses((prev) => ({
      ...prev,
      [sampleKey]: {
        ...prev[sampleKey],
        status,
      },
    }));
  };

  const handleSampleNotesChange = (sampleKey, notes) => {
    setSampleStatuses((prev) => ({
      ...prev,
      [sampleKey]: {
        ...prev[sampleKey],
        notes,
      },
    }));
  };

  const handleApplyStatusToAll = (status) => {
    const updated = {};
    samples.forEach((sample) => {
      const sampleKey =
        sample.boxSampleItemId || sample.sampleItemId || sample.id;
      updated[sampleKey] = {
        ...sampleStatuses[sampleKey],
        status,
      };
    });
    setSampleStatuses(updated);
  };

  const handleScanSample = () => {
    const input = sampleScanInput.trim();
    if (!input) return;

    const matchedSample = samples.find(
      (s) =>
        s.accessionNumber === input ||
        s.accessionNumber?.replace(/\.\d+$/, "") === input,
    );

    if (matchedSample) {
      const sampleKey =
        matchedSample.boxSampleItemId ||
        matchedSample.sampleItemId ||
        matchedSample.id;
      setSampleStatuses((prev) => ({
        ...prev,
        [sampleKey]: {
          ...prev[sampleKey],
          status: "RECEIVED_GOOD",
        },
      }));
      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage(
          { id: "shipment.reception.sampleScanned" },
          { accessionNumber: matchedSample.accessionNumber },
        ),
      });
    } else {
      addNotification({
        kind: "warning",
        title: intl.formatMessage({ id: "notification.warning" }),
        message: intl.formatMessage(
          { id: "shipment.reception.sampleNotFound" },
          { accessionNumber: input },
        ),
      });
    }
    setSampleScanInput("");
  };

  const hasValidationErrors = () => {
    const errors = {};
    Object.entries(sampleStatuses).forEach(([key, { status, notes }]) => {
      if (status !== "RECEIVED_GOOD" && (!notes || !notes.trim())) {
        errors[key] = true;
      }
    });
    setValidationErrors(errors);
    return Object.keys(errors).length > 0;
  };

  const handleConfirmReception = async () => {
    setSubmitting(true);
    try {
      // Update each sample item's reception status
      let allUpdatesSucceeded = true;
      for (const sample of samples) {
        const sampleKey =
          sample.boxSampleItemId || sample.sampleItemId || sample.id;
        const statusData = sampleStatuses[sampleKey];

        if (!statusData) {
          console.error(
            "No status data found for key:",
            sampleKey,
            "sample:",
            sample,
          );
          continue;
        }

        let receptionEndpoint =
          "/rest/box-sample/items/" +
          sampleKey +
          "/reception-status?status=" +
          statusData.status;
        if (statusData.notes) {
          receptionEndpoint += "&notes=" + encodeURIComponent(statusData.notes);
        }

        const status = await new Promise((resolve) => {
          putToOpenElisServer(receptionEndpoint, null, (status) => {
            resolve(status);
          });
        });

        if (status < 200 || status >= 300) {
          console.error(
            "Failed to update reception status for",
            sampleKey,
            "status:",
            status,
          );
          allUpdatesSucceeded = false;
        }
      }

      if (!allUpdatesSucceeded) {
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "notification.error" }),
          message: intl.formatMessage({
            id: "shipment.error.partialReception",
          }),
        });
        return;
      }

      // Update box state to RECEIVED (only if not already RECEIVED or RECONCILED)
      if (
        box.state !== "RECEIVED" &&
        box.state !== "RECONCILED" &&
        box.state !== "PARTIALLY_RECEIVED"
      ) {
        const stateStatus = await new Promise((resolve) => {
          putToOpenElisServer(
            `/rest/shipping-box/${box.id}/state?newState=RECEIVED`,
            null,
            (status) => {
              resolve(status);
            },
          );
        });

        if (stateStatus < 200 || stateStatus >= 300) {
          console.error("Failed to update box state:", stateStatus);
        }
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
      setExpectedSpecimens([]);
      setShowConfirmModal(false);
    } catch (error) {
      console.error("Error confirming reception:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.confirmReception" }),
      });
    } finally {
      setSubmitting(false);
    }
  };

  const renderBoxStateTag = (state) => {
    const cfg = {
      DRAFT: { type: "gray", id: "shipment.state.draft" },
      READY_TO_SEND: { type: "blue", id: "shipment.state.readyToSend" },
      SENT: { type: "purple", id: "shipment.state.sent" },
      IN_TRANSIT: { type: "cyan", id: "shipment.state.inTransit" },
      RECEIVED: { type: "green", id: "shipment.state.received" },
      RECONCILED: { type: "teal", id: "shipment.state.reconciled" },
    };
    const c = cfg[state] || cfg.DRAFT;
    return <Tag type={c.type}>{intl.formatMessage({ id: c.id })}</Tag>;
  };

  const renderStatusTag = (status) => {
    const statusConfig = {
      RECEIVED_GOOD: {
        type: "green",
        label: intl.formatMessage({ id: "shipment.reception.received" }),
      },
      RECEIVED_DAMAGED: {
        type: "red",
        label: intl.formatMessage({ id: "shipment.reception.damaged" }),
      },
      RECEIVED_LEAKED: {
        type: "magenta",
        label: intl.formatMessage({ id: "shipment.reception.leaked" }),
      },
      MISSING: {
        type: "gray",
        label: intl.formatMessage({ id: "shipment.reception.missing" }),
      },
      REJECTED: {
        type: "purple",
        label: intl.formatMessage({ id: "shipment.reception.rejected" }),
      },
    };
    const cfg = statusConfig[status] || statusConfig.RECEIVED_GOOD;
    return <Tag type={cfg.type}>{cfg.label}</Tag>;
  };

  const headers = [
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
      key: "status",
      header: intl.formatMessage({ id: "shipment.reception.status" }),
    },
    {
      key: "notes",
      header: intl.formatMessage({ id: "shipment.reception.notes" }),
    },
  ];

  const renderRows = () => {
    return samples.map((sample) => {
      const sampleKey =
        sample.boxSampleItemId || sample.sampleItemId || sample.id;
      const currentStatus =
        sampleStatuses[sampleKey]?.status || "RECEIVED_GOOD";

      return {
        id: sampleKey?.toString() || "-",
        accessionNumber: sample.accessionNumber || "-",
        typeOfSample: sample.typeOfSample || "-",
        referralTests: sample.referralTests
          ? sample.referralTests.map((t) => t.testName).join(", ")
          : "-",
        status: (
          <Select
            id={`status-${sampleKey}`}
            size="sm"
            labelText=""
            hideLabel
            value={currentStatus}
            onChange={(e) =>
              handleSampleStatusChange(sampleKey, e.target.value)
            }
          >
            <SelectItem
              value="RECEIVED_GOOD"
              text={intl.formatMessage({
                id: "shipment.reception.received",
              })}
            />
            <SelectItem
              value="RECEIVED_DAMAGED"
              text={intl.formatMessage({
                id: "shipment.reception.damaged",
              })}
            />
            <SelectItem
              value="RECEIVED_LEAKED"
              text={intl.formatMessage({
                id: "shipment.reception.leaked",
              })}
            />
            <SelectItem
              value="MISSING"
              text={intl.formatMessage({
                id: "shipment.reception.missing",
              })}
            />
            <SelectItem
              value="REJECTED"
              text={intl.formatMessage({
                id: "shipment.reception.rejected",
              })}
            />
          </Select>
        ),
        notes: (
          <TextInput
            size="sm"
            id={`notes-${sampleKey}`}
            labelText=""
            hideLabel
            placeholder={intl.formatMessage({
              id:
                currentStatus !== "RECEIVED_GOOD"
                  ? "shipment.reception.notesRequiredPlaceholder"
                  : "shipment.reception.notesPlaceholder",
            })}
            value={sampleStatuses[sampleKey]?.notes || ""}
            onChange={(e) => {
              handleSampleNotesChange(sampleKey, e.target.value);
              if (e.target.value.trim()) {
                setValidationErrors((prev) => {
                  const next = { ...prev };
                  delete next[sampleKey];
                  return next;
                });
              }
            }}
            invalid={!!validationErrors[sampleKey]}
            invalidText={intl.formatMessage({
              id: "shipment.reception.notesRequired",
            })}
          />
        ),
      };
    });
  };

  // Count statuses for the confirmation summary
  const getStatusSummary = () => {
    const summary = {};
    Object.values(sampleStatuses).forEach(({ status }) => {
      summary[status] = (summary[status] || 0) + 1;
    });
    return summary;
  };

  const handleReset = () => {
    setBox(null);
    setSamples([]);
    setSampleStatuses({});
    setBoxId("");
    setGeneralNotes("");
    setExpectedSpecimens([]);
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
            onKeyDown={(e) => e.key === "Enter" && handleScanBox()}
            disabled={loading || box !== null}
          />
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Button
            renderIcon={Scan}
            onClick={() => handleScanBox()}
            disabled={loading || box !== null}
            className="scan-button"
          >
            <FormattedMessage id="shipment.reception.scanButton" />
          </Button>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Button
            kind="tertiary"
            renderIcon={CloudDownload}
            onClick={handleImportFromFhir}
            disabled={importing}
            className="scan-button"
          >
            {importing ? (
              <FormattedMessage id="label.loading" />
            ) : (
              <FormattedMessage id="shipment.reception.importFromFhir" />
            )}
          </Button>
        </Column>
      </Grid>

      {!box && incomingBoxes.length > 0 && (
        <Grid fullWidth className="incoming-boxes-section">
          <Column lg={16} md={8} sm={4}>
            <h3 className="section-title">
              <FormattedMessage id="shipment.reception.incomingBoxes" />
            </h3>
            <TableContainer>
              <Table size="sm">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage id="shipment.label.boxId" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="shipment.reception.originFacility" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="shipment.reception.shipped" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="shipment.label.state" />
                    </TableHeader>
                    <TableHeader />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {incomingBoxes.map((incomingBox) => (
                    <TableRow key={incomingBox.id}>
                      <TableCell>{incomingBox.boxId}</TableCell>
                      <TableCell>
                        {incomingBox.originFacilityName || "-"}
                      </TableCell>
                      <TableCell>
                        {incomingBox.actualSampleCount || 0}
                      </TableCell>
                      <TableCell>
                        {renderBoxStateTag(incomingBox.state)}
                      </TableCell>
                      <TableCell>
                        <Button
                          size="sm"
                          kind="tertiary"
                          renderIcon={View}
                          onClick={() => handleOpenIncomingBox(incomingBox)}
                        >
                          <FormattedMessage id="shipment.reception.openBox" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Column>
        </Grid>
      )}

      {!box && incomingBoxes.length === 0 && !loading && (
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <p className="empty-state-message">
              <FormattedMessage id="shipment.reception.noIncomingBoxes" />
            </p>
          </Column>
        </Grid>
      )}

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
                {renderBoxStateTag(box.state)}
              </h3>
              <div className="box-info-details">
                <div className="info-row">
                  <span className="info-label">
                    {box.inbound ? (
                      <FormattedMessage id="shipment.reception.originFacility" />
                    ) : (
                      <FormattedMessage id="shipment.box.destination" />
                    )}
                    :
                  </span>
                  <span className="info-value">
                    {box.inbound
                      ? box.originFacilityName || "-"
                      : box.destinationFacilityName}
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
                  <span className="info-value">
                    {intl.formatMessage(
                      { id: "shipment.reception.sampleCountSummary" },
                      {
                        accepted: samples.length,
                        expected: samples.length + expectedSpecimens.length,
                      },
                    )}
                  </span>
                </div>
              </div>
            </div>
          </Column>

          {expectedSpecimens.length > 0 && (
            <Column lg={16} md={8} sm={4}>
              <div className="verification-header">
                <h3 className="section-title">
                  <FormattedMessage id="shipment.reception.pendingAcceptance" />
                </h3>
              </div>
              <TableContainer>
                <Table size="sm">
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        <FormattedMessage id="shipment.reception.specimen" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="shipment.reception.referralOrder" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="shipment.reception.status" />
                      </TableHeader>
                      <TableHeader />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {expectedSpecimens.map((specimen) => (
                      <TableRow key={specimen.specimenUuid}>
                        <TableCell>
                          {specimen.typeDisplay || specimen.specimenUuid}
                        </TableCell>
                        <TableCell>{specimen.externalOrderNumber}</TableCell>
                        <TableCell>
                          {specimen.status === "UNRESOLVED" ? (
                            <Tag type="red">
                              <FormattedMessage id="shipment.reception.unresolved" />
                            </Tag>
                          ) : (
                            <Tag type="blue">
                              <FormattedMessage id="shipment.reception.awaitingAcceptance" />
                            </Tag>
                          )}
                        </TableCell>
                        <TableCell>
                          {specimen.status === "PENDING" && (
                            <Button
                              size="sm"
                              kind="tertiary"
                              renderIcon={Checkmark}
                              onClick={() => handleAcceptSpecimen(specimen)}
                            >
                              <FormattedMessage id="shipment.reception.acceptSample" />
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Column>
          )}

          <Column lg={16} md={8} sm={4}>
            <div className="verification-header">
              <h3 className="section-title">
                <FormattedMessage id="shipment.reception.sampleVerification" />
              </h3>
              <div className="apply-all-actions">
                <Dropdown
                  id="applyStatusToAll"
                  size="sm"
                  label={intl.formatMessage({
                    id: "shipment.reception.applyToAll",
                  })}
                  titleText=""
                  items={[
                    {
                      id: "RECEIVED_GOOD",
                      text: intl.formatMessage({
                        id: "shipment.reception.received",
                      }),
                    },
                    {
                      id: "RECEIVED_DAMAGED",
                      text: intl.formatMessage({
                        id: "shipment.reception.damaged",
                      }),
                    },
                    {
                      id: "RECEIVED_LEAKED",
                      text: intl.formatMessage({
                        id: "shipment.reception.leaked",
                      }),
                    },
                    {
                      id: "MISSING",
                      text: intl.formatMessage({
                        id: "shipment.reception.missing",
                      }),
                    },
                    {
                      id: "REJECTED",
                      text: intl.formatMessage({
                        id: "shipment.reception.rejected",
                      }),
                    },
                  ]}
                  itemToString={(item) => (item ? item.text : "")}
                  onChange={({ selectedItem }) => {
                    if (selectedItem) {
                      handleApplyStatusToAll(selectedItem.id);
                    }
                  }}
                />
              </div>
            </div>

            <div className="sample-scan-section">
              <TextInput
                id="sampleScanInput"
                size="sm"
                labelText={intl.formatMessage({
                  id: "shipment.reception.scanSample",
                })}
                placeholder={intl.formatMessage({
                  id: "shipment.reception.scanSamplePlaceholder",
                })}
                value={sampleScanInput}
                onChange={(e) => setSampleScanInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleScanSample()}
              />
              <Button
                size="sm"
                kind="secondary"
                renderIcon={Scan}
                onClick={handleScanSample}
                className="sample-scan-button"
              >
                <FormattedMessage id="shipment.reception.scanSampleButton" />
              </Button>
            </div>

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
              <Button kind="secondary" onClick={handleReset}>
                <FormattedMessage id="label.cancel" />
              </Button>
              <Button
                kind="primary"
                onClick={() => {
                  if (hasValidationErrors()) {
                    addNotification({
                      kind: "error",
                      title: intl.formatMessage({ id: "notification.error" }),
                      message: intl.formatMessage({
                        id: "shipment.reception.notesRequired",
                      }),
                    });
                    return;
                  }
                  setShowConfirmModal(true);
                }}
                disabled={submitting}
              >
                <FormattedMessage id="shipment.reception.confirmReception" />
              </Button>
            </div>
          </Column>
        </Grid>
      )}

      {/* Confirmation Modal with Summary */}
      <Modal
        open={showConfirmModal}
        modalHeading={intl.formatMessage({
          id: "shipment.reception.confirmReception",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.confirm" })}
        secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
        onRequestSubmit={handleConfirmReception}
        onRequestClose={() => setShowConfirmModal(false)}
        primaryButtonDisabled={submitting}
      >
        <p>
          <FormattedMessage
            id="shipment.reception.confirmMessage"
            values={{ count: samples.length }}
          />
        </p>
        <div style={{ marginTop: "1rem" }}>
          {Object.entries(getStatusSummary()).map(([status, count]) => (
            <div
              key={status}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                marginBottom: "0.25rem",
              }}
            >
              {renderStatusTag(status)}
              <span>
                {count} <FormattedMessage id="shipment.label.samples" />
              </span>
            </div>
          ))}
        </div>
      </Modal>
    </div>
  );
};

export default ReceptionWorkflow;
