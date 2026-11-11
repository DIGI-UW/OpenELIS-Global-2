import React, { useEffect, useRef, useState } from "react";
import { ComboBox } from "@carbon/react";
import { searchAreas } from "./locationsApi";

/**
 * FR-B4a / FR-C3: a typeahead over geographic areas. Each option shows the
 * area, its level and its path, so a district is told apart from a village of
 * the same name. The chosen area is returned as {id, name, levelName, path}.
 */
const areaToItem = (area, byId) => {
  const path = [];
  let current = area;
  const seen = new Set();
  while (current && current.parentId && !seen.has(current.parentId)) {
    seen.add(current.parentId);
    const parent = byId[current.parentId];
    if (!parent) break;
    path.unshift(parent.name);
    current = parent;
  }
  return {
    id: area.id,
    name: area.name,
    levelName: area.levelName,
    path: [...path, area.name],
    text: area.name,
    sub: `${area.levelName || ""}${path.length ? " · " + path.join(" / ") : ""}`,
  };
};

const LocationComboBox = ({
  id,
  titleText,
  placeholder,
  helperText,
  invalid,
  invalidText,
  selected,
  onChange,
  disabled,
}) => {
  const [items, setItems] = useState(selected ? [selected] : []);
  const timer = useRef(null);

  useEffect(() => {
    if (selected && !items.some((item) => item.id === selected.id)) {
      setItems((current) => [selected, ...current]);
    }
  }, [selected]);

  const search = (text) => {
    if (timer.current) {
      clearTimeout(timer.current);
    }
    if (!text || text.trim().length < 2) {
      return;
    }
    timer.current = setTimeout(() => {
      searchAreas(text, "active")
        .then((areas) => {
          const byId = {};
          (areas || []).forEach((area) => {
            byId[area.id] = area;
          });
          const options = (areas || [])
            .filter((area) => area.matched)
            .map((area) => areaToItem(area, byId));
          setItems(
            selected && !options.some((item) => item.id === selected.id)
              ? [selected, ...options]
              : options,
          );
        })
        .catch(() => setItems(selected ? [selected] : []));
    }, 300);
  };

  return (
    <ComboBox
      id={id}
      titleText={titleText}
      placeholder={placeholder}
      helperText={helperText}
      invalid={invalid}
      invalidText={invalidText}
      disabled={disabled}
      items={items}
      itemToString={(item) => (item ? item.text || item.name : "")}
      itemToElement={(item) =>
        item ? (
          <span>
            {item.text || item.name}
            {item.sub ? (
              <span className="cds--label"> · {item.sub}</span>
            ) : null}
          </span>
        ) : null
      }
      selectedItem={
        selected
          ? items.find((item) => item.id === selected.id) || selected
          : null
      }
      onInputChange={search}
      onChange={({ selectedItem }) => onChange(selectedItem || null)}
    />
  );
};

export default LocationComboBox;
