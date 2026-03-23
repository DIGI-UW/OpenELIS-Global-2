import { Column, MultiSelect } from "@carbon/react";
import { useEffect, useMemo, useRef } from "react";

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

  const wrapperRef = useRef(null);

  useEffect(() => {
    const el = wrapperRef.current;
    if (!el) return;
    const menu = el.querySelector(".cds--list-box__menu");
    if (!menu) return;
    const rect = el.getBoundingClientRect();
    menu.style.position = "fixed";
    menu.style.top = rect.top - menu.offsetHeight + "px";
    menu.style.left = rect.left + "px";
    menu.style.width = rect.width + "px";
    menu.style.zIndex = "99999";
  });

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
        <div ref={wrapperRef}>
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
        </div>
      </Column>
    </>
  );
}
