import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  TextInput,
  NumberInput,
  ComboBox,
  Button,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  InlineNotification,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryBoardAPI, InventoryManagementAPI } from "./InventoryService";
import InventoryItemForm from "./InventoryItemForm";
import { parseGs1, gtinMatches } from "./gs1";
import "./InventoryItemsBoard.css";

/**
 * Entering a delivery.
 *
 * <p>Scan, type a quantity, add, scan the next. A GS1 label carries the product
 * code, the batch and the expiry, so everything except how much arrived is
 * already known by the time the scanner beeps — and how much arrived is the one
 * thing no barcode can tell you.
 *
 * <p>Manual entry is not a fallback bolted on the side: the same fields are
 * always present and always editable, because a label that will not scan is an
 * ordinary Tuesday and the line still has to be entered.
 *
 * <p>Nothing is written until the whole delivery is confirmed, and then it is
 * written in one transaction. A per-line save would leave a counter
 * reconciling a screen, a shelf and a ledger that agree with none of each other.
 */
const ReceiveDelivery = () => {
  const intl = useIntl();
  const scanRef = useRef(null);
  const nextLineKey = useRef(0);

  const [rows, setRows] = useState([]);
  const [lines, setLines] = useState([]);
  const [scan, setScan] = useState("");
  const [lastScan, setLastScan] = useState(null);
  const [itemId, setItemId] = useState(null);
  const [lotNumber, setLotNumber] = useState("");
  const [expirationDate, setExpirationDate] = useState("");
  const [quantity, setQuantity] = useState(1);
  // An unknown code is an offer to define something; defining it is a separate
  // decision, so the form does not open itself the moment a scan misses.
  const [unknownUpc, setUnknownUpc] = useState(null);
  const [definingUpc, setDefiningUpc] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [done, setDone] = useState(null);

  const load = useCallback(async () => {
    try {
      setRows(await InventoryBoardAPI.get());
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const options = rows.map((row) => ({
    id: row.itemId,
    text: row.code ? `${row.name} (${row.code})` : row.name,
    row,
  }));
  const selected = options.find((option) => option.id === itemId) || null;

  const today = new Date().toISOString().slice(0, 10);
  const expiredAlready = expirationDate && expirationDate < today;

  const clearLine = () => {
    setItemId(null);
    setLotNumber("");
    setExpirationDate("");
    setQuantity(1);
    setLastScan(null);
  };

  const focusScan = () => {
    if (scanRef.current) scanRef.current.focus();
  };

  const handleScan = (value) => {
    const parsed = parseGs1(value);
    setScan("");
    if (!parsed.raw) return;
    setLastScan(parsed);

    // What this label carries replaces what the last one filled in, including
    // when it carries nothing. Leaving the previous batch and expiry in place
    // let a box whose label prints only a product code inherit the batch of the
    // box before it, silently and on the line that gets committed. Values typed
    // by hand, with no scan behind them, are left alone.
    setLotNumber(parsed.fields.lotNumber || (lastScan ? "" : lotNumber));
    setExpirationDate(
      parsed.fields.expirationDate || (lastScan ? "" : expirationDate),
    );

    if (parsed.fields.gtin) {
      const match = options.find((option) =>
        gtinMatches(option.row.upc, parsed.fields.gtin),
      );
      if (match) {
        setItemId(match.id);
        setUnknownUpc(null);
      } else {
        // Not an error. The lab has met a product it has not defined, and the
        // lines already entered must survive defining it.
        setUnknownUpc(parsed.fields.gtin);
      }
    }
  };

  const addLine = () => {
    if (itemId == null) {
      setError(intl.formatMessage({ id: "inventory.receive.error.noItem" }));
      return;
    }
    if (!(quantity > 0)) {
      setError(intl.formatMessage({ id: "inventory.receive.error.quantity" }));
      return;
    }
    setError(null);
    setLines((previous) => [
      ...previous,
      {
        // A counter, not the line count: removing a line rewinds the length,
        // and the next line with the same item and lot would then mint a key a
        // surviving line already holds. React treats two such rows as one, and
        // removing either deletes both.
        key: `line-${nextLineKey.current++}`,
        itemId,
        itemName: selected.text,
        units: selected.row.units,
        lotNumber,
        expirationDate,
        quantity,
        rawScan: lastScan?.raw || null,
        unparsed: lastScan?.unparsed?.length
          ? lastScan.unparsed.join(" ")
          : null,
      },
    ]);
    clearLine();
    focusScan();
  };

  const removeLine = (key) =>
    setLines((previous) => previous.filter((line) => line.key !== key));

  const commit = async () => {
    if (lines.length === 0) return;
    setSaving(true);
    setError(null);
    try {
      await InventoryManagementAPI.receiveBatch(
        lines.map((line) => ({
          inventoryItem: { id: line.itemId },
          lotNumber: line.lotNumber ? line.lotNumber : null,
          initialQuantity: line.quantity,
          currentQuantity: line.quantity,
          expirationDate: line.expirationDate
            ? new Date(line.expirationDate).toISOString()
            : null,
        })),
      );
      setDone(lines.length);
      setLines([]);
      clearLine();
      load();
      focusScan();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="receive-delivery">
      <p className="board-purpose">
        <FormattedMessage id="inventory.delivery.purpose" />
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

      {done != null && (
        <InlineNotification
          kind="success"
          lowContrast
          onClose={() => setDone(null)}
          title={intl.formatMessage({ id: "notification.success" })}
          subtitle={intl.formatMessage(
            { id: "inventory.delivery.received" },
            { count: done },
          )}
        />
      )}

      <Stack gap={5}>
        <TextInput
          id="delivery-scan"
          ref={scanRef}
          labelText={<FormattedMessage id="inventory.delivery.scan" />}
          helperText={intl.formatMessage({
            id: "inventory.delivery.scan.help",
          })}
          value={scan}
          onChange={(event) => setScan(event.target.value)}
          onKeyDown={(event) => {
            if (event.key !== "Enter") return;
            // The scanner's own Enter. Without this it submits whatever form
            // it lands in before the code has been read.
            event.preventDefault();
            handleScan(scan);
          }}
        />

        {lastScan && !lastScan.recognised && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "inventory.delivery.scan.unread" })}
            subtitle={lastScan.raw}
          />
        )}

        {unknownUpc && (
          <div className="delivery-unknown">
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "inventory.receive.scan.unknown",
              })}
              subtitle={unknownUpc}
            />
            <Button
              kind="tertiary"
              size="sm"
              onClick={() => setDefiningUpc(unknownUpc)}
            >
              <FormattedMessage id="inventory.item.new" />
            </Button>
          </div>
        )}

        <div className="delivery-line">
          <ComboBox
            id="delivery-item"
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
          <TextInput
            id="delivery-lot"
            labelText={<FormattedMessage id="lot.number" />}
            value={lotNumber}
            onChange={(event) => setLotNumber(event.target.value)}
          />
          <TextInput
            id="delivery-expiry"
            type="date"
            labelText={<FormattedMessage id="lot.expirationDate" />}
            value={expirationDate}
            onChange={(event) => setExpirationDate(event.target.value)}
          />
          <NumberInput
            id="delivery-quantity"
            label={<FormattedMessage id="lot.currentQuantity" />}
            min={1}
            step={1}
            value={quantity}
            onChange={(event, { value }) => setQuantity(Number(value))}
          />
          <Button kind="tertiary" size="md" onClick={addLine}>
            <FormattedMessage id="inventory.delivery.add" />
          </Button>
        </div>

        {expiredAlready && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "inventory.delivery.expired" })}
            subtitle={intl.formatMessage({
              id: "inventory.delivery.expired.help",
            })}
          />
        )}

        {lines.length > 0 && (
          <>
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    <FormattedMessage id="catalog.item.name" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="lot.number" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="lot.expirationDate" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="lot.currentQuantity" />
                  </TableHeader>
                  <TableHeader />
                </TableRow>
              </TableHead>
              <TableBody>
                {lines.map((line) => (
                  <TableRow key={line.key}>
                    <TableCell>
                      {line.itemName}
                      {line.unparsed && (
                        <Tag size="sm" type="warm-gray" title={line.rawScan}>
                          <FormattedMessage id="inventory.delivery.partialScan" />
                        </Tag>
                      )}
                    </TableCell>
                    <TableCell>{line.lotNumber || "—"}</TableCell>
                    <TableCell>{line.expirationDate || "—"}</TableCell>
                    <TableCell>
                      {line.quantity} {line.units}
                    </TableCell>
                    <TableCell>
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() => removeLine(line.key)}
                      >
                        <FormattedMessage id="inventory.delivery.remove" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            <div className="delivery-actions">
              <Button kind="primary" disabled={saving} onClick={commit}>
                <FormattedMessage
                  id="inventory.delivery.confirm"
                  values={{ count: lines.length }}
                />
              </Button>
              <Button kind="ghost" onClick={() => setLines([])}>
                <FormattedMessage id="inventory.delivery.discard" />
              </Button>
            </div>
          </>
        )}
      </Stack>

      {definingUpc && (
        <InventoryItemForm
          open
          item={null}
          initialUpc={definingUpc}
          onClose={() => setDefiningUpc(null)}
          onSave={() => {
            // The lines already entered survive: only the catalogue changed.
            setDefiningUpc(null);
            setUnknownUpc(null);
            load();
          }}
        />
      )}
    </div>
  );
};

export default ReceiveDelivery;
