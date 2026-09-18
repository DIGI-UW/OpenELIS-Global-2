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

const ManageTagsModal = ({ open, onClose, onSave }) => {
  const intl = useIntl();
  const [directory, setDirectory] = useState([]);
  const [showDeactivated, setShowDeactivated] = useState(false);
  const [newTag, setNewTag] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
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
        /* Not DataTable: its row-id lookup returns undefined once a retired tag
           drops out of the filtered list */
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
