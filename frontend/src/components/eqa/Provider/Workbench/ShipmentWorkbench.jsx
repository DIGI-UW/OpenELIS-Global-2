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
import { hintStyle } from "../../eqaCommon";
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
  // Which write is in flight: an organizationId for that row's save, "ship" for
  // the batch. One flag would freeze every row while a single one saved.
  const [busy, setBusy] = useState(null);

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
    setBusy(row.organizationId);
    saveShipmentDetails(
      cycleId,
      { organizationId: row.organizationId, ...draft },
      ({ ok, body }) => {
        setBusy(null);
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
    setBusy("ship");
    markShipped(cycleId, selected, ({ ok, body }) => {
      setBusy(null);
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

  const handleLabel = (row) =>
    generateLabelPDF(
      {
        boxId: row.boxCode,
        destinationFacility: row.organizationName,
        temperature: row.temperatureRequirement,
        sampleCount: (prep?.panels || []).reduce(
          (sum, panel) => sum + panel.sampleCount,
          0,
        ),
      },
      intl.formatMessage,
    );

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
              // undefined = the read failed; null marks it so a partial manifest is
              // refused rather than shipped as if it were the whole box.
              resolve(
                samples === undefined
                  ? null
                  : samples.map((sample) => ({
                      accessionNumber: sample.blindCode || sample.sampleCode,
                      typeOfSample: panel.panelName,
                      referralTests: sample.analyteName || "",
                    })),
              ),
            ),
          ),
      ),
    ).then((perPanel) => {
      const samples = perPanel.filter(Boolean).flat();
      // Any panel that could not be read, or nothing to list at all: a pack list a
      // courier signs for must never quietly under-report the box.
      if (perPanel.some((rows) => rows === null) || samples.length === 0) {
        onNotice({
          kind: "error",
          text: t(
            "eqa.shipment.packList.unavailable",
            "The panel samples could not be read, so no pack list was produced. Reload and try again.",
          ),
        });
        return;
      }
      generateManifestPDF(
        {
          boxId: row.boxCode,
          destinationFacility: row.organizationName,
          state: row.boxState,
          temperature: row.temperatureRequirement,
          samples: samples,
          notes: t(
            "eqa.shipment.packList.notes",
            "EQA panel material — do not open before the testing window.",
          ),
        },
        intl.formatMessage,
      );
    });
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
              disabled={selected.length === 0 || busy !== null}
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
                <TableHeader>{t("column.actions", "Actions")}</TableHeader>
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
                        labelText={t(
                          "eqa.shipment.selectRow",
                          "Select {name} for dispatch",
                          { name: row.organizationName },
                        )}
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
                        labelText={t(
                          "eqa.shipment.courierFor",
                          "Courier for {name}",
                          { name: row.organizationName },
                        )}
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
                        labelText={t(
                          "eqa.shipment.trackingFor",
                          "Tracking number for {name}",
                          { name: row.organizationName },
                        )}
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
                          labelText={t(
                            "eqa.shipment.expectedFor",
                            "Expected delivery for {name}",
                            { name: row.organizationName },
                          )}
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
                          disabled={busy === row.organizationId}
                          onClick={() => handleSave(row)}
                        >
                          {t("button.save", "Save")}
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
