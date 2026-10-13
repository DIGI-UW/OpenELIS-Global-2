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
import { toLocalIsoDate } from "../utils/Utils";
import LocationPickerModal from "../storage/LocationPicker/LocationPickerModal";
import {
  selectionToHierarchicalPath,
  getDeepestLocationSelection,
  positionToCoordinate,
} from "../storage/LocationPicker/locationSelectionMapper";

/** Never sends a lot id: the receive endpoint would overwrite that lot. */
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
        // Blank goes as null so the server generates the lot number.
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
      // onSave unmounts this modal, so no state updates after it.
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
              // A scanner ends its code with Enter, which must not submit the dialog.
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

              <DatePicker
                datePickerType="single"
                dateFormat="Y-m-d"
                value={expirationDate}
                onChange={(dates) =>
                  setExpirationDate(dates[0] ? toLocalIsoDate(dates[0]) : "")
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
