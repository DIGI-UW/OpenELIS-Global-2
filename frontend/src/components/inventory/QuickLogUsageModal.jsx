import React, { useState } from "react";
import {
  Modal,
  ComboBox,
  NumberInput,
  InlineNotification,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryManagementAPI } from "./InventoryService";

/**
 * Log that stock was used, without hunting for the item's row first.
 *
 * <p>This replaces the old RecordUsageModal, which is deleted in the same
 * change. That component was framed around a single lot — it took a lot, showed
 * that lot's number and balance, and capped the quantity at it — but submitted
 * only the lot's <em>item</em> id, so the server picked lots by earliest expiry
 * and could decrement a different lot than the one on screen. Its accession
 * lookup also pointed at a path no controller serves, so it never populated.
 * This surface is honest about the grain the write actually has: pick an item,
 * say how much, and let earliest-expiry-first choose the lots.
 *
 * <p>What it writes matters: a consumption, never an adjustment. Consumption
 * records usage, and usage is the only thing the run-out projection reads. An
 * adjustment moves the same number on the shelf and leaves the projection
 * describing a consumption rate that never happened.
 */
const QuickLogUsageModal = ({
  open,
  items,
  initialItemId = null,
  onClose,
  onSave,
}) => {
  const intl = useIntl();
  const [itemId, setItemId] = useState(initialItemId);
  const [quantity, setQuantity] = useState(1);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const options = items.map((row) => ({
    id: row.itemId,
    text: row.code ? `${row.name} (${row.code})` : row.name,
    row,
  }));
  const selected = options.find((o) => o.id === itemId) || null;

  const submit = async () => {
    if (itemId == null) {
      setError(intl.formatMessage({ id: "inventory.logUsage.error.noItem" }));
      return;
    }
    // inventory_usage carries a CHECK of >= 1, and the service only guards
    // positivity — so a fractional quantity passes the client and the service
    // and then dies on the constraint as an opaque 500, with the lot already
    // decremented in the same transaction. Refuse it here instead.
    if (!Number.isInteger(quantity) || quantity < 1) {
      setError(
        intl.formatMessage({ id: "inventory.logUsage.error.wholeUnits" }),
      );
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await InventoryManagementAPI.consume({
        itemId: String(itemId),
        quantity,
      });
      // onSave unmounts this modal, so nothing may touch state after it — a
      // `finally` that clears the saving flag runs against a dead component.
      onSave();
    } catch (err) {
      // Insufficient stock comes back as a readable sentence naming what is
      // available, so show it rather than a generic failure. The server sends
      // errorCode and params alongside it; prefer those, because the message
      // itself is English regardless of the user's locale. Only this branch
      // keeps the modal alive, so only this branch resets the flag.
      setError(
        err.errorCode
          ? intl.formatMessage({ id: err.errorCode }, err.params)
          : err.message,
      );
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={submit}
      modalHeading={intl.formatMessage({ id: "inventory.logUsage.title" })}
      primaryButtonText={intl.formatMessage({ id: "usage.record.button" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving}
      size="sm"
    >
      <Stack gap={5}>
        <p className="board-subline">
          <FormattedMessage id="inventory.logUsage.help" />
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

        <ComboBox
          id="quick-log-usage-item"
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
          id="quick-log-usage-quantity"
          label={
            selected ? (
              <FormattedMessage
                id="inventory.logUsage.quantityWithUnits"
                values={{ units: selected.row.units }}
              />
            ) : (
              <FormattedMessage id="usage.quantityUsed" />
            )
          }
          min={1}
          step={1}
          value={quantity}
          onChange={(event, { value }) => setQuantity(Number(value))}
        />

        {selected && (
          <p className="board-subline">
            <FormattedMessage
              id="inventory.logUsage.onHand"
              values={{
                quantity: intl.formatNumber(selected.row.onHand),
                units: selected.row.units,
              }}
            />
          </p>
        )}
      </Stack>
    </Modal>
  );
};

export default QuickLogUsageModal;
