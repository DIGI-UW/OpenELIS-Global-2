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

  const [cascades, setCascades] = useState(() => parseValue(value));

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

    const updated = { ...cascades, [nextKey]: [] };
    setCascades(updated);
    emitChange(updated);
  };

  const removeCascade = (key) => {
    const updated = { ...cascades };
    delete updated[key];
    setCascades(updated);
    emitChange(updated);
  };

  const updateCascade = (key, selectedItems) => {
    const updated = {
      ...cascades,
      [key]: selectedItems.map((i) => i.id),
    };
    setCascades(updated);
    emitChange(updated);
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
              <div
                key={key}
                style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}
              >
                <Column lg={16} sm={4} md={8}>
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
                </Column>

                <Button
                  kind="ghost"
                  size="sm"
                  hasIconOnly
                  renderIcon={TrashCan}
                  iconDescription="Remove"
                  onClick={() => removeCascade(key)}
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
