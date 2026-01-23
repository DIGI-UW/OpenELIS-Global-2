import { Column, MultiSelect } from "@carbon/react";
import { useMemo } from "react";

export default function ResultMultiSelect({
  id,
  name,
  dictionaryValues = [],
  value = "{}",
  onChange,
}) {
  const selectedIds = useMemo(() => {
    try {
      const parsed = JSON.parse(value || "{}");
      return Object.values(parsed)
        .flatMap((v) => v.split(","))
        .filter(Boolean);
    } catch {
      return [];
    }
  }, [value]);

  const selectedItems = useMemo(
    () => dictionaryValues.filter((d) => selectedIds.includes(String(d.id))),
    [dictionaryValues, selectedIds],
  );

  const handleChange = ({ selectedItems }) => {
    const ids = selectedItems.map((i) => String(i.id));

    const json = ids.length > 0 ? JSON.stringify({ 0: ids.join(",") }) : "{}";

    onChange({
      target: {
        id,
        name,
        value: json,
      },
    });
  };

  return (
    <>
      <Column lg={16} sm={4} md={8}>
        <MultiSelect
          style={{ width: "300px" }}
          id={id}
          items={dictionaryValues.map((d) => ({
            id: d.id,
            label: d.value,
          }))}
          itemToString={(item) => item?.label || ""}
          selectedItems={selectedItems.map((d) => ({
            id: d.id,
            label: d.value,
          }))}
          onChange={handleChange}
          selectionFeedback="top-after-reopen"
          label=""
        />
      </Column>
    </>
  );
}
