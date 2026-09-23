import React, { useState } from "react";
import {
  Modal,
  FileUploaderDropContainer,
  InlineNotification,
  Tag,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Link,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryImportAPI } from "./InventoryService";

const OUTCOME_TAG = {
  CREATE: "green",
  UPDATE: "blue",
  UNCHANGED: "gray",
  SKIP: "red",
};

/**
 * Defining a catalogue from a file.
 *
 * <p>The file is never parsed here. The browser reads its text and posts it;
 * the server answers with what the file would do, and the same call with the
 * same text is what commits it. A preview parsed in the browser and a commit
 * parsed on the server would be two readings of one file, which is how the
 * importer this replaces could show a preview its commit disagreed with.
 */
const ImportItemsModal = ({ open, onClose, onImported }) => {
  const intl = useIntl();
  const [fileName, setFileName] = useState(null);
  const [csv, setCsv] = useState(null);
  const [plan, setPlan] = useState(null);
  const [applied, setApplied] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const reset = () => {
    setFileName(null);
    setCsv(null);
    setPlan(null);
    setApplied(false);
    setError(null);
  };

  const close = () => {
    reset();
    onClose();
  };

  // A new file invalidates the plan: applying one file against another's
  // preview is exactly what the confirm step exists to prevent.
  const handleFile = async (files) => {
    const file = files?.[0];
    if (!file) return;
    reset();
    setFileName(file.name);
    try {
      setCsv(await file.text());
    } catch {
      setError(intl.formatMessage({ id: "inventory.import.error.unreadable" }));
    }
  };

  const run = async (action, isApply) => {
    if (!csv) return;
    setBusy(true);
    setError(null);
    try {
      const result = await action(csv);
      setPlan(result);
      if (isApply) {
        setApplied(true);
        onImported();
      }
    } catch (err) {
      setError(
        err?.message ||
          intl.formatMessage({ id: "inventory.import.error.failed" }),
      );
    } finally {
      setBusy(false);
    }
  };

  const skipped = (plan?.rows || []).filter((row) => row.outcome === "SKIP");
  const hasErrors = skipped.length > 0;
  const nothingToDo =
    plan && plan.created === 0 && plan.updated === 0 && !applied;

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({ id: "inventory.import.title" })}
      primaryButtonText={intl.formatMessage({
        id: applied
          ? "button.close"
          : plan
            ? "inventory.import.apply"
            : "inventory.import.preview",
      })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      // A skipped row blocks itself, not the file. The server evaluates and
      // commits row by row, so the eleven good rows go in and the bad one is
      // listed with its reason; disabling Apply threw the good rows away too
      // and left the user editing a file the server was willing to take.
      primaryButtonDisabled={busy || !csv || (plan && !applied && nothingToDo)}
      onRequestClose={close}
      onSecondarySubmit={close}
      onRequestSubmit={() => {
        if (applied) return close();
        return plan
          ? run(InventoryImportAPI.apply, true)
          : run(InventoryImportAPI.preview, false);
      }}
      size="lg"
    >
      <Stack gap={5}>
        <p>
          <FormattedMessage id="inventory.import.help" />
        </p>
        <p>
          <Link href={InventoryImportAPI.templateUrl()}>
            <FormattedMessage id="inventory.import.template" />
          </Link>
        </p>

        {!applied && (
          <FileUploaderDropContainer
            accept={[".csv", "text/csv"]}
            labelText={intl.formatMessage({ id: "inventory.import.dropzone" })}
            onAddFiles={(event, { addedFiles }) => handleFile(addedFiles)}
          />
        )}
        {fileName && <p>{fileName}</p>}

        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            role="status"
            title={intl.formatMessage({ id: "notification.error" })}
            subtitle={error}
            onCloseButtonClick={() => setError(null)}
          />
        )}

        {plan && (
          <div>
            <h5>
              <FormattedMessage
                id={
                  applied
                    ? "inventory.import.result.title"
                    : "inventory.import.plan.title"
                }
              />
            </h5>
            <div style={{ margin: "0.5rem 0" }}>
              <Tag type="green">
                <FormattedMessage
                  id="inventory.import.count.created"
                  values={{ count: plan.created }}
                />
              </Tag>
              <Tag type="blue">
                <FormattedMessage
                  id="inventory.import.count.updated"
                  values={{ count: plan.updated }}
                />
              </Tag>
              <Tag type="gray">
                <FormattedMessage
                  id="inventory.import.count.unchanged"
                  values={{ count: plan.unchanged }}
                />
              </Tag>
              <Tag type="red">
                <FormattedMessage
                  id="inventory.import.count.skipped"
                  values={{ count: plan.skipped }}
                />
              </Tag>
            </div>

            {hasErrors && !applied && (
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                role="status"
                title={intl.formatMessage({
                  id: "inventory.import.error.rowsTitle",
                })}
                subtitle={intl.formatMessage({
                  id: "inventory.import.error.rowsMessage",
                })}
              />
            )}
            {nothingToDo && !hasErrors && (
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                role="status"
                title={intl.formatMessage({
                  id: "inventory.import.nothingToDo",
                })}
                subtitle={intl.formatMessage({
                  id: "inventory.import.nothingToDo.help",
                })}
              />
            )}

            <div style={{ overflowX: "auto", maxHeight: "18rem" }}>
              <Table size="sm">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage id="inventory.import.column.line" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="catalog.item.name" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="inventory.import.column.outcome" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="inventory.import.column.reason" />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {plan.rows.map((row, index) => (
                    <TableRow key={`${row.lineNumber}-${index}`}>
                      <TableCell>{row.lineNumber}</TableCell>
                      <TableCell>{row.name}</TableCell>
                      <TableCell>
                        <Tag
                          type={OUTCOME_TAG[row.outcome] || "gray"}
                          size="sm"
                        >
                          <FormattedMessage
                            id={`inventory.import.outcome.${row.outcome.toLowerCase()}`}
                          />
                        </Tag>
                      </TableCell>
                      <TableCell>{row.reason}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </div>
        )}
      </Stack>
    </Modal>
  );
};

export default ImportItemsModal;
