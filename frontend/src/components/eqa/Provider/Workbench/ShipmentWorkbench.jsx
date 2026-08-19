import React, { useState } from "react";
import {
  Button,
  Checkbox,
  DatePicker,
  DatePickerInput,
  InlineNotification,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { resolveApiErrorMessage, toLocalIsoDate } from "../../../utils/Utils";
import {
  generateLabelPDF,
  generateManifestPDF,
} from "../../../shipment/utils/pdfGenerator";
import {
  fetchPanelSamples,
  markShipped,
  saveShipmentDetails,
} from "./workbenchApi";

const BOX_STATE_TAG = {
  DRAFT: "gray",
  READY_TO_SEND: "blue",
  SENT: "teal",
  IN_TRANSIT: "teal",
  RECEIVED: "green",
  RECONCILED: "green",
  CANCELLED: "red",
  LOST_IN_TRANSIT: "red",
};

const hintStyle = { fontSize: "0.75rem", color: "#525252" };

/**
 * Shipment workbench (FR-V2.5-13): one row per participant, courier details,
 * bulk dispatch, and the two documents that travel with the box.
 *
 * Both PDFs are generated in the browser through the shipment module's own
 * jsPDF helpers — its backend PDF services are deprecated for removal, so
 * reusing them would have been reuse of the wrong thing. The pack list lists
 * panel sample codes and analyte names; target values are never on it, and the
 * API nulls them anyway unless the caller may unblind.
 */
const ShipmentWorkbench = ({ cycleId, prep, rows, onChanged, onNotice }) => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [drafts, setDrafts] = useState({});
  const [selected, setSelected] = useState([]);
  const [busy, setBusy] = useState(false);

  const draftOf = (row) => ({
    courier: row.courier || "",
    trackingNumber: row.trackingNumber || "",
    estimatedDeliveryDate: (row.estimatedDeliveryDate || "").slice(0, 10),
    ...(drafts[row.organizationId] || {}),
  });

  const setDraft = (organizationId, patch) =>
    setDrafts((prev) => ({
      ...prev,
      [organizationId]: { ...(prev[organizationId] || {}), ...patch },
    }));

  const dispatched = (row) => row.boxState === "SENT" || !!row.shippedDate;

  const handleSave = (row) => {
    const draft = draftOf(row);
    setBusy(true);
    saveShipmentDetails(
      cycleId,
      { organizationId: row.organizationId, ...draft },
      ({ ok, body }) => {
        setBusy(false);
        if (ok) {
          setDrafts((prev) => ({ ...prev, [row.organizationId]: undefined }));
          onChanged();
          onNotice({
            kind: "success",
            text: t("eqa.shipment.saved", "Shipment details saved."),
          });
          return;
        }
        onNotice({
          kind: "error",
          text: resolveApiErrorMessage(intl, body, "eqa.shipment.saveFailed"),
        });
      },
    );
  };

  const handleShip = () => {
    setBusy(true);
    markShipped(cycleId, selected, ({ ok, body }) => {
      setBusy(false);
      if (ok) {
        setSelected([]);
        onChanged();
        onNotice({
          kind: "success",
          text: t(
            "eqa.shipment.shipped",
            "{n} participant shipments dispatched.",
            { n: (body || []).length },
          ),
        });
        return;
      }
      onNotice({
        kind: "error",
        text: resolveApiErrorMessage(intl, body, "eqa.shipment.shipFailed"),
      });
    });
  };

  const labelData = (row) => ({
    boxId: row.boxCode,
    destinationFacility: row.organizationName,
    temperature: row.temperatureRequirement,
    sampleCount: totalSamples(),
    sampleTypeCounts: {},
  });

  const totalSamples = () =>
    (prep?.panels || []).reduce((sum, panel) => sum + panel.sampleCount, 0);

  const handleLabel = (row) =>
    generateLabelPDF(labelData(row), intl.formatMessage);

  /** Pack list = the panel samples this participant's box carries. */
  const handlePackList = (row) => {
    const panels = prep?.panels || [];
    if (panels.length === 0) {
      onNotice({
        kind: "error",
        text: t(
          "eqa.shipment.packList.noPanel",
          "This cycle has no panel, so there is nothing to pack.",
        ),
      });
      return;
    }
    Promise.all(
      panels.map(
        (panel) =>
          new Promise((resolve) =>
            fetchPanelSamples(panel.panelId, (samples) =>
              resolve(
                samples.map((sample) => ({
                  accessionNumber: sample.blindCode || sample.sampleCode,
                  typeOfSample: panel.panelName,
                  referralTests: sample.analyteName || "",
                  collectionDate: null,
                })),
              ),
            ),
          ),
      ),
    ).then((perPanel) =>
      generateManifestPDF(
        {
          boxId: row.boxCode,
          serviceLocation: "",
          destinationFacility: row.organizationName,
          state: row.boxState,
          temperature: row.temperatureRequirement,
          createdDate: null,
          createdBy: "",
          samples: perPanel.flat(),
          notes: t(
            "eqa.shipment.packList.notes",
            "EQA panel material — do not open before the testing window.",
          ),
        },
        intl.formatMessage,
      ),
    );
  };

  const selectable = rows.filter((row) => !dispatched(row) && row.boxId);
  const allSelected =
    selectable.length > 0 && selected.length === selectable.length;

  return (
    <>
      {rows.length === 0 && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={t("eqa.shipment.noParticipants.title", "No participants")}
          subtitle={t(
            "eqa.shipment.noParticipants.body",
            "Enrol participant laboratories in this scheme before shipping panels.",
          )}
        />
      )}
      {rows.length > 0 && (
        <>
          <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
            <Checkbox
              id="select-all-shipments"
              labelText={t("eqa.shipment.selectAll", "Select all pending")}
              checked={allSelected}
              disabled={selectable.length === 0}
              onChange={(_e, { checked }) =>
                setSelected(
                  checked ? selectable.map((row) => row.organizationId) : [],
                )
              }
            />
            <Button
              size="sm"
              disabled={selected.length === 0 || busy}
              onClick={handleShip}
            >
              {t("eqa.shipment.markShipped", "Mark {n} shipped", {
                n: selected.length,
              })}
            </Button>
            <span style={hintStyle}>
              {t(
                "eqa.shipment.firstShipHint",
                "The first dispatch moves the cycle to shipped.",
              )}
            </span>
          </div>
          <Table size="sm" style={{ marginTop: "0.75rem" }}>
            <TableHead>
              <TableRow>
                <TableHeader />
                <TableHeader>
                  {t("eqa.shipment.participant", "Participant")}
                </TableHeader>
                <TableHeader>{t("eqa.shipment.box", "Box")}</TableHeader>
                <TableHeader>
                  {t("eqa.shipment.courier", "Courier")}
                </TableHeader>
                <TableHeader>
                  {t("eqa.shipment.tracking", "Tracking number")}
                </TableHeader>
                <TableHeader>
                  {t("eqa.shipment.expected", "Expected delivery")}
                </TableHeader>
                <TableHeader>
                  {t("eqa.shipment.actions", "Actions")}
                </TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => {
                const draft = draftOf(row);
                const isDispatched = dispatched(row);
                return (
                  <TableRow key={row.organizationId}>
                    <TableCell>
                      <Checkbox
                        id={`select-${row.organizationId}`}
                        labelText=""
                        hideLabel
                        checked={selected.includes(row.organizationId)}
                        disabled={isDispatched || !row.boxId}
                        onChange={(_e, { checked }) =>
                          setSelected((prev) =>
                            checked
                              ? [...prev, row.organizationId]
                              : prev.filter((id) => id !== row.organizationId),
                          )
                        }
                      />
                    </TableCell>
                    <TableCell>{row.organizationName}</TableCell>
                    <TableCell>
                      {row.boxCode || "—"}
                      <br />
                      {row.boxState && (
                        <Tag
                          type={BOX_STATE_TAG[row.boxState] || "gray"}
                          size="sm"
                        >
                          {row.boxState.replace(/_/g, " ")}
                        </Tag>
                      )}
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id={`courier-${row.organizationId}`}
                        labelText=""
                        hideLabel
                        size="sm"
                        value={draft.courier}
                        disabled={isDispatched}
                        onChange={(e) =>
                          setDraft(row.organizationId, {
                            courier: e.target.value,
                          })
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id={`tracking-${row.organizationId}`}
                        labelText=""
                        hideLabel
                        size="sm"
                        value={draft.trackingNumber}
                        disabled={isDispatched}
                        onChange={(e) =>
                          setDraft(row.organizationId, {
                            trackingNumber: e.target.value,
                          })
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <DatePicker
                        datePickerType="single"
                        dateFormat="d/m/Y"
                        value={draft.estimatedDeliveryDate}
                        onChange={(dates) =>
                          setDraft(row.organizationId, {
                            estimatedDeliveryDate: dates[0]
                              ? toLocalIsoDate(dates[0])
                              : "",
                          })
                        }
                      >
                        <DatePickerInput
                          id={`expected-${row.organizationId}`}
                          labelText=""
                          hideLabel
                          size="sm"
                          placeholder="dd/mm/yyyy"
                          disabled={isDispatched}
                        />
                      </DatePicker>
                    </TableCell>
                    <TableCell>
                      {!isDispatched && (
                        <Button
                          kind="ghost"
                          size="sm"
                          disabled={busy}
                          onClick={() => handleSave(row)}
                        >
                          {t("eqa.shipment.save", "Save")}
                        </Button>
                      )}
                      {row.boxId && (
                        <>
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() => handlePackList(row)}
                          >
                            {t("eqa.shipment.packList", "Pack list")}
                          </Button>
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() => handleLabel(row)}
                          >
                            {t("eqa.shipment.label", "Label")}
                          </Button>
                        </>
                      )}
                      {isDispatched && (
                        <div style={hintStyle}>
                          {t("eqa.shipment.shippedOn", "Shipped {date}", {
                            date: (row.shippedDate || "").slice(0, 10),
                          })}
                        </div>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </>
      )}
    </>
  );
};

export default ShipmentWorkbench;
