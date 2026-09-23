import React, { useState } from "react";
import {
  Modal,
  ComboBox,
  NumberInput,
  TextInput,
  DatePicker,
  DatePickerInput,
  InlineNotification,
  FormLabel,
  Button,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  InventoryManagementAPI,
  InventoryLotStorageAPI,
} from "./InventoryService";
import { toIsoDate } from "./dates";
import LocationPickerModal from "../storage/LocationPicker/LocationPickerModal";
import {
  selectionToHierarchicalPath,
  getDeepestLocationSelection,
  positionToCoordinate,
} from "../storage/LocationPicker/locationSelectionMapper";

/**
 * More of something that already exists.
 *
 * <p>The everyday case is a box arriving, not a delivery being entered. It asks
 * for a quantity, and for a lot number and expiry only when the item is one
 * that is tracked lot by lot — a box of gloves is not, and being made to invent
 * a lot number for one is how a lot table fills with noise.
 *
 * <p>Scanning is how an item is identified without searching: a product
 * barcode resolves against the items already on the board. A code that matches
 * nothing is not an error worth stopping for — it means this is something new,
 * and the offer is to define it with the code already filled in.
 *
 * <p>Two things this deliberately does not send. It never sends a lot id: the
 * receive endpoint routes a body carrying one into an overwrite of that lot
 * rather than a new receipt, while still recording the full quantity as
 * arriving. And it never sends a receipt date, because the server stamps its
 * own clock over whatever arrives and a field that is silently discarded is
 * worse than no field.
 */
const QuickReceiveModal = ({
  open,
  items,
  initialItemId = null,
  onClose,
  onSave,
  onDefineNew,
}) => {
  const intl = useIntl();
  const [itemId, setItemId] = useState(initialItemId);
  const [quantity, setQuantity] = useState(1);
  const [lotNumber, setLotNumber] = useState("");
  const [expirationDate, setExpirationDate] = useState("");
  const [scan, setScan] = useState("");
  const [scanMiss, setScanMiss] = useState(null);
  const [assignment, setAssignment] = useState(null);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const options = items.map((row) => ({
    id: row.itemId,
    text: row.code ? `${row.name} (${row.code})` : row.name,
    row,
  }));
  const selected = options.find((option) => option.id === itemId) || null;
  const tracksLots = selected?.row.trackLots === true;

  const locationSummary = assignment
    ? selectionToHierarchicalPath(assignment.selection)
    : null;

  const resolveScan = (code) => {
    const normalized = code.trim();
    if (!normalized) return;
    const match = options.find(
      (option) => option.row.upc && option.row.upc.trim() === normalized,
    );
    if (match) {
      setItemId(match.id);
      setScanMiss(null);
    } else {
      // Not a failure: an unrecognised product barcode is usually something the
      // lab has not defined yet, and the useful answer is to offer to define it.
      setScanMiss(normalized);
    }
    setScan("");
  };

  const submit = async () => {
    if (itemId == null) {
      setError(intl.formatMessage({ id: "inventory.receive.error.noItem" }));
      return;
    }
    if (!(quantity > 0)) {
      setError(intl.formatMessage({ id: "inventory.receive.error.quantity" }));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const received = await InventoryManagementAPI.receive({
        inventoryItem: { id: itemId },
        // Blank is meaningful: the server mints a lot number from the item code
        // and the date, which is the right answer for stock that has none.
        lotNumber: tracksLots && lotNumber.trim() ? lotNumber.trim() : null,
        initialQuantity: quantity,
        currentQuantity: quantity,
        expirationDate:
          tracksLots && expirationDate
            ? new Date(expirationDate).toISOString()
            : null,
      });

      if (assignment && received?.id != null) {
        try {
          const deepest = getDeepestLocationSelection(assignment.selection, {
            requireAssignable: true,
          });
          await InventoryLotStorageAPI.assignLocation({
            inventoryLotId: String(received.id),
            locationId: deepest ? String(deepest.value.id) : null,
            locationType: deepest ? deepest.type : null,
            positionCoordinate: positionToCoordinate(assignment.position, {
              emptyValue: null,
            }),
            notes: assignment.notes || "",
          });
        } catch (assignErr) {
          // The stock is in. Saying so and naming what failed is better than an
          // error that reads as though nothing was received.
          setError(
            intl.formatMessage(
              { id: "inventory.receive.error.locationAfterReceive" },
              { reason: assignErr.message || "" },
            ),
          );
          setSaving(false);
          return;
        }
      }
      // onSave unmounts this modal, so nothing may touch state after it. The
      // quantity goes with it because the caller has something to explain: a
      // received lot starts at PENDING QC, and on-hand counts only stock that
      // has passed, so the row will not move until it does. Receiving and
      // seeing no change otherwise reads as a failed save.
      onSave({ quantity, units: selected.row.units });
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  return (
    <>
      <Modal
        open={open}
        onRequestClose={onClose}
        onRequestSubmit={submit}
        modalHeading={intl.formatMessage({ id: "inventory.receive.title" })}
        primaryButtonText={intl.formatMessage({
          id: "inventory.receive.submit",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={saving}
        size="sm"
      >
        <Stack gap={5}>
          <p className="board-subline">
            <FormattedMessage id="inventory.receive.help" />
          </p>

          {error && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "notification.error" })}
              subtitle={error}
            />
          )}

          <TextInput
            id="quick-receive-scan"
            labelText={<FormattedMessage id="inventory.receive.scan" />}
            helperText={intl.formatMessage({
              id: "inventory.receive.scan.help",
            })}
            value={scan}
            onChange={(event) => setScan(event.target.value)}
            onKeyDown={(event) => {
              if (event.key !== "Enter") return;
              // A scanner sends its code and then Enter. Without this the
              // keystroke submits the dialog with nothing filled in.
              event.preventDefault();
              event.stopPropagation();
              resolveScan(scan);
            }}
          />

          {scanMiss && (
            <div className="quick-receive-miss">
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: "inventory.receive.scan.unknown",
                })}
                subtitle={scanMiss}
              />
              {onDefineNew && (
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => onDefineNew(scanMiss)}
                >
                  <FormattedMessage id="inventory.item.new" />
                </Button>
              )}
            </div>
          )}

          <ComboBox
            id="quick-receive-item"
            titleText={<FormattedMessage id="catalog.item.name" />}
            placeholder={intl.formatMessage({
              id: "inventory.search.placeholder",
            })}
            items={options}
            selectedItem={selected}
            itemToString={(option) => (option ? option.text : "")}
            onChange={({ selectedItem }) =>
              setItemId(selectedItem ? selectedItem.id : null)
            }
          />

          <NumberInput
            id="quick-receive-quantity"
            label={
              selected ? (
                <FormattedMessage
                  id="inventory.receive.quantityWithUnits"
                  values={{ units: selected.row.units }}
                />
              ) : (
                <FormattedMessage id="lot.currentQuantity" />
              )
            }
            min={1}
            step={1}
            value={quantity}
            onChange={(event, { value }) => setQuantity(Number(value))}
          />

          {tracksLots && (
            <>
              <TextInput
                id="quick-receive-lot"
                labelText={<FormattedMessage id="lot.number" />}
                helperText={intl.formatMessage({
                  id: "inventory.receive.lotNumber.help",
                })}
                value={lotNumber}
                onChange={(event) => setLotNumber(event.target.value)}
              />

              {/* Y-m-d, like every other date field in the module. Three
                  formats across four screens meant typing one screen's into
                  another produced a different date without saying so. And the
                  picked day is kept as a calendar date rather than an instant:
                  toISOString() on local midnight moves the day, which is how a
                  scanned label and a typed one came to disagree by one. */}
              <DatePicker
                datePickerType="single"
                dateFormat="Y-m-d"
                value={expirationDate}
                onChange={(dates) =>
                  setExpirationDate(dates[0] ? toIsoDate(dates[0]) : "")
                }
              >
                <DatePickerInput
                  id="quick-receive-expiry"
                  labelText={<FormattedMessage id="lot.expirationDate" />}
                  placeholder="yyyy-mm-dd"
                />
              </DatePicker>
            </>
          )}

          <div>
            <div className="quick-receive-location">
              <FormLabel>
                <FormattedMessage id="lot.selectLocation" />
              </FormLabel>
              <Button
                kind="ghost"
                size="sm"
                onClick={() => setPickerOpen(true)}
              >
                <FormattedMessage
                  id={
                    locationSummary
                      ? "storage.location.move"
                      : "storage.location.assign"
                  }
                />
              </Button>
            </div>
            <div className="board-subline">
              {locationSummary || (
                <FormattedMessage id="inventory.receive.location.optional" />
              )}
            </div>
          </div>
        </Stack>
      </Modal>

      <LocationPickerModal
        isOpen={pickerOpen}
        occupantType="INVENTORY_LOT"
        occupant={{
          label: lotNumber,
          type: selected?.row.name || "",
          status: "ACTIVE",
        }}
        currentLocation={null}
        onConfirm={(confirmed) => {
          setPickerOpen(false);
          // The assign endpoint rejects a blank location with a 400, and here
          // that rejection would land after the stock was already received.
          if (
            !getDeepestLocationSelection(confirmed.selection, {
              requireAssignable: true,
            })
          ) {
            setError(
              intl.formatMessage({
                id: "storage.manageLocation.error.selectTarget",
              }),
            );
            return;
          }
          setAssignment(confirmed);
        }}
        onCancel={() => setPickerOpen(false)}
      />
    </>
  );
};

export default QuickReceiveModal;
