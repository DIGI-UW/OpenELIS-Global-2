import React, { useState, useEffect, useCallback } from "react";
import {
  Modal,
  TextInput,
  Button,
  Checkbox,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryTagAPI } from "./InventoryService";

/**
 * Light governance over free-form tags: see what has accumulated, add one ahead
 * of use, and retire the duplicates.
 *
 * <p>Deactivating is not deleting. A retired tag stops being suggested and
 * drops out of the filter, but every item carrying it keeps it and goes on
 * showing it — which is what lets a lab settle on "glove" over "gloves" without
 * rewriting what it recorded last year.
 */
const ManageTagsModal = ({ open, onClose, onSave }) => {
  const intl = useIntl();
  const [directory, setDirectory] = useState([]);
  const [showDeactivated, setShowDeactivated] = useState(false);
  const [newTag, setNewTag] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  // Set only once something actually changed, so closing without touching
  // anything does not make the board refetch.
  const [dirty, setDirty] = useState(false);

  const load = useCallback(async () => {
    try {
      setDirectory(await InventoryTagAPI.getDirectory());
      setError(null);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    if (open) load();
  }, [open, load]);

  const run = async (action) => {
    setBusy(true);
    setError(null);
    try {
      await action();
      setDirty(true);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const addTag = () => {
    const trimmed = newTag.trim();
    if (!trimmed) return;
    run(async () => {
      await InventoryTagAPI.create(trimmed);
      setNewTag("");
    });
  };

  const visible = directory.filter((tag) => showDeactivated || tag.active);

  return (
    <Modal
      open={open}
      onRequestClose={() => {
        if (dirty) onSave();
        setDirty(false);
        onClose();
      }}
      modalHeading={intl.formatMessage({ id: "inventory.tags.manage" })}
      passiveModal
      size="md"
    >
      <p className="board-suggestions-help">
        <FormattedMessage id="inventory.tags.help" />
      </p>

      {error && <div className="board-error">{error}</div>}

      <div className="manage-tags-add">
        <TextInput
          id="new-tag"
          labelText={<FormattedMessage id="inventory.tags.new" />}
          value={newTag}
          onChange={(event) => setNewTag(event.target.value)}
          onKeyDown={(event) => {
            if (event.key !== "Enter") return;
            // The dialog must not treat the key that adds a tag as a submit.
            event.preventDefault();
            event.stopPropagation();
            addTag();
          }}
        />
        <Button
          kind="tertiary"
          size="md"
          onClick={addTag}
          disabled={busy || !newTag.trim()}
        >
          <FormattedMessage id="inventory.tags.add" />
        </Button>
      </div>

      <Checkbox
        id="manage-tags-show-deactivated"
        labelText={intl.formatMessage({ id: "inventory.tags.showDeactivated" })}
        checked={showDeactivated}
        onChange={(_, { checked }) => setShowDeactivated(checked)}
      />

      {visible.length === 0 ? (
        <p className="board-empty">
          <FormattedMessage id="inventory.tags.none" />
        </p>
      ) : (
        /* A plain table, not Carbon's DataTable. Nothing here sorts, selects or
           searches, and DataTable's row ids have to be mapped back to the tag
           they came from — a lookup that returns undefined the moment the list
           it is looking in has already been filtered, which is every render
           after a tag is retired. */
        <Table size="sm">
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="inventory.tags.tag" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="inventory.tags.usage" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="inventory.tags.status" />
              </TableHeader>
              <TableHeader />
            </TableRow>
          </TableHead>
          <TableBody>
            {visible.map((tag) => (
              <TableRow key={tag.name}>
                <TableCell>{tag.name}</TableCell>
                <TableCell>{tag.itemCount}</TableCell>
                <TableCell>
                  <Tag type={tag.active ? "green" : "gray"} size="sm">
                    {intl.formatMessage({
                      id: tag.active
                        ? "inventory.tags.active"
                        : "inventory.tags.deactivated",
                    })}
                  </Tag>
                </TableCell>
                <TableCell>
                  <Button
                    kind="ghost"
                    size="sm"
                    disabled={busy}
                    onClick={() =>
                      run(() =>
                        tag.active
                          ? InventoryTagAPI.deactivate(tag.name)
                          : InventoryTagAPI.activate(tag.name),
                      )
                    }
                  >
                    <FormattedMessage
                      id={
                        tag.active
                          ? "inventory.tags.deactivate"
                          : "inventory.tags.reactivate"
                      }
                    />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Modal>
  );
};

export default ManageTagsModal;
