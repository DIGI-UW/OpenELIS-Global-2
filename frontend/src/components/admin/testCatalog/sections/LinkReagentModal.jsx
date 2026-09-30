import React, { useEffect, useState } from "react";
import {
  Modal,
  FilterableMultiSelect,
  Tag,
  Stack,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";

/**
 * OGC-949 M15 / OGC-992 (epic OGC-762) — Link Reagent modal.
 *
 * Multi-select of reagent inventory (item_type=REAGENT) minus the reagents
 * already linked to this test. "Link Selected" creates one test_reagent_link
 * per selected reagent with default usage_type=PRIMARY (quantity left null,
 * filled in later via the per-row inline edit, OGC-993).
 */
const LinkReagentModal = ({
  open,
  onClose,
  testId,
  linkedReagentIds = [],
  onLinked,
}) => {
  const intl = useIntl();

  const [available, setAvailable] = useState([]);
  const [selected, setSelected] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open || !testId) {
      return;
    }
    setSelected([]);
    setError(null);
    const linked = new Set(linkedReagentIds.map((id) => String(id)));
    getFromOpenElisServer("/rest/inventory/items/type/REAGENT", (res) => {
      const items = Array.isArray(res) ? res : [];
      setAvailable(
        items
          .filter((i) => !linked.has(String(i.id)))
          .map((i) => ({ id: i.id, name: i.name })),
      );
    });
  }, [open, testId, linkedReagentIds]);

  const removeSelected = (id) => {
    setSelected((prev) => prev.filter((s) => s.id !== id));
  };

  const linkOne = (reagentId) =>
    new Promise((resolve) => {
      postToOpenElisServer(
        `/rest/test-catalog/${testId}/reagents`,
        JSON.stringify({ reagentId, usageType: "PRIMARY" }),
        (status) => resolve(status),
      );
    });

  const handleSubmit = async () => {
    if (selected.length === 0) {
      onClose();
      return;
    }
    setSaving(true);
    setError(null);
    const statuses = await Promise.all(selected.map((s) => linkOne(s.id)));
    setSaving(false);
    if (statuses.every((s) => s >= 200 && s < 300)) {
      setSelected([]);
      onLinked();
      onClose();
    } else {
      setError(
        intl.formatMessage({ id: "label.testCatalog.reagents.linkError" }),
      );
    }
  };

  const noneAvailable = available.length === 0;

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({
        id: "label.testCatalog.reagents.modal.title",
      })}
      primaryButtonText={intl.formatMessage({
        id: "label.testCatalog.reagents.modal.linkSelected",
      })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving || noneAvailable || selected.length === 0}
      size="md"
    >
      <Stack gap={5}>
        {noneAvailable ? (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "label.testCatalog.reagents.modal.allLinked",
            })}
          />
        ) : (
          <>
            <FilterableMultiSelect
              id="reagent-multiselect"
              titleText={intl.formatMessage({
                id: "label.testCatalog.reagents.modal.select.label",
              })}
              placeholder={intl.formatMessage({
                id: "label.testCatalog.reagents.modal.select.placeholder",
              })}
              items={available}
              itemToString={(item) => (item ? item.name : "")}
              selectedItems={selected}
              onChange={({ selectedItems }) => setSelected(selectedItems || [])}
            />
            {selected.length > 0 && (
              <div>
                {selected.map((s) => (
                  <Tag
                    key={s.id}
                    type="blue"
                    filter
                    onClose={() => removeSelected(s.id)}
                    title={intl.formatMessage({ id: "label.button.remove" })}
                  >
                    {s.name}
                  </Tag>
                ))}
              </div>
            )}
          </>
        )}
        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            onCloseButtonClick={() => setError(null)}
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={error}
          />
        )}
      </Stack>
    </Modal>
  );
};

export default LinkReagentModal;
