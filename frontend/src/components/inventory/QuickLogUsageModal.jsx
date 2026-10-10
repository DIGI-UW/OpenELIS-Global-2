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

/** Logs a consumption, never an adjustment: the run-out projection reads only usage. */
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
    // inventory_usage has CHECK >= 1 but the service lets fractions through to a 500.
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
      // onSave unmounts this modal, so no state updates after it.
      onSave();
    } catch (err) {
      // Prefer errorCode: the server's message is English whatever the locale.
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
