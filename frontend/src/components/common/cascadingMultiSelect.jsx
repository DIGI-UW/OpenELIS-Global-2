import { useState, useMemo } from "react";
import { Button, Column, MultiSelect } from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";

export default function CascadingMultiSelect({
  id,
  name,
  dictionaryValues = [],
  value = "{}",
  onChange,
}) {
  const items = useMemo(
    () =>
      dictionaryValues.map((d) => ({
        id: String(d.id),
        label: d.value,
      })),
    [dictionaryValues],
  );

  const parseValue = (val) => {
    try {
      const parsed = JSON.parse(val || "{}");
      const result = {};
      Object.keys(parsed).forEach((k) => {
        result[k] = parsed[k].split(",").filter(Boolean);
      });
      return result;
    } catch {
      return {};
    }
  };

  // Selections are derived from the value prop so saved results display and
  // edits round-trip through the parent form state. Empty cascades are
  // stripped from the emitted JSON, so newly added (still empty) rows live in
  // local state until they get a selection.
  const savedCascades = useMemo(() => parseValue(value), [value]);
  const [draftKeys, setDraftKeys] = useState([]);

  const cascades = useMemo(() => {
    const merged = {};
    draftKeys.forEach((k) => {
      merged[k] = [];
    });
    Object.keys(savedCascades).forEach((k) => {
      merged[k] = savedCascades[k];
    });
    return merged;
  }, [savedCascades, draftKeys]);

  const cascadeKeys = Object.keys(cascades)
    .map(Number)
    .sort((a, b) => a - b);

  const emitChange = (updated) => {
    const json = {};
    Object.keys(updated).forEach((k) => {
      if (updated[k].length) json[k] = updated[k].join(",");
    });

    onChange({
      target: { id, name, value: JSON.stringify(json) },
    });
  };

  const addCascade = () => {
    const nextKey = cascadeKeys.length ? Math.max(...cascadeKeys) + 1 : 0;
    setDraftKeys([...draftKeys, nextKey]);
  };

  const removeCascade = (key) => {
    setDraftKeys(draftKeys.filter((k) => k !== key));

    const updated = { ...cascades };
    delete updated[key];
    emitChange(updated);
  };

  const updateCascade = (key, selectedItems) => {
    if (!draftKeys.includes(key)) {
      setDraftKeys([...draftKeys, key]);
    }

    emitChange({
      ...cascades,
      [key]: selectedItems.map((i) => i.id),
    });
  };

  return (
    <>
      <Column lg={16} sm={4} md={8}>
        <div>
          {cascadeKeys.map((key) => {
            const selectedItems = items.filter((i) =>
              cascades[key]?.includes(i.id),
            );

            return (
              <div key={key} style={{ marginBottom: "0.5rem" }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "flex-end",
                    marginBottom: "-0.5rem",
                  }}
                >
                  <Button
                    kind="ghost"
                    size="sm"
                    hasIconOnly
                    renderIcon={TrashCan}
                    iconDescription="Remove"
                    onClick={() => removeCascade(key)}
                  />
                </div>

                <MultiSelect
                  id={`${id}_${key}`}
                  items={items}
                  selectedItems={selectedItems}
                  itemToString={(item) => item?.label || ""}
                  onChange={({ selectedItems }) =>
                    updateCascade(key, selectedItems)
                  }
                  label=""
                  selectionFeedback="top-after-reopen"
                  style={{ minWidth: "250px" }}
                />
              </div>
            );
          })}

          <Button
            kind="ghost"
            size="sm"
            style={{ marginTop: "0.5rem" }}
            renderIcon={Add}
            onClick={addCascade}
          >
            Add
          </Button>
        </div>
      </Column>
    </>
  );
}
