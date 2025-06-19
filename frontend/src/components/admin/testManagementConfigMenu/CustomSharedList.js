import { useState } from "react";

// type CustomSharedListProps<T> = {
//   leftTitle: string;
//   rightTitle: string;
//   leftList: T[];
//   rightList: T[];
//   renderItem: (item: T) => string;
//   onChange: (left: T[], right: T[]) => void;
// };

export function CustomSharedList({
  leftTitle,
  rightTitle,
  leftList,
  rightList,
  renderItem,
  onChange,
  // }: CustomSharedListProps<T>
}) {
  const [selectedLeft, setSelectedLeft] = useState(new Set());
  const [selectedRight, setSelectedRight] = useState(new Set());

  const toggleSelection = (item, side) => {
    const id = item.id;
    const setFn = side === "left" ? setSelectedLeft : setSelectedRight;
    const current = side === "left" ? selectedLeft : selectedRight;
    const newSet = new Set(current);
    if (newSet.has(item)) newSet.delete(item);
    else newSet.add(item);
    setFn(newSet);
  };

  const moveRight = () => {
    const newLeft = leftList?.filter((item) => !selectedLeft.has(item));
    const newRight = [
      ...rightList,
      ...Array.from(selectedLeft).filter((i) => !rightList?.includes(i)),
    ];
    setSelectedLeft(new Set());
    onChange(newLeft, newRight);
  };

  const moveLeft = () => {
    const newRight = rightList?.filter((item) => !selectedRight.has(item));
    const newLeft = [
      ...leftList,
      ...Array.from(selectedRight).filter((i) => !leftList?.includes(i)),
    ];
    setSelectedRight(new Set());
    onChange(newLeft, newRight);
  };

  return (
    <div style={{ display: "flex", gap: "1rem", alignItems: "center" }}>
      {/* Left list */}
      <div>
        <div>
          <strong>{leftTitle}</strong>
        </div>
        <br />
        <ul
          style={{
            border: "1px solid #ccc",
            minHeight: "200px",
            width: "200px",
            listStyle: "none",
            padding: 0,
          }}
        >
          {leftList?.map((item) => (
            <li
              key={item.id}
              onClick={() => toggleSelection(item, "left")}
              style={{
                padding: "5px",
                cursor: "pointer",
                backgroundColor: selectedLeft.has(item) ? "#cce5ff" : "white",
              }}
            >
              {renderItem(item.value)}
            </li>
          ))}
        </ul>
      </div>

      {/* Buttons */}
      <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
        <button onClick={moveRight}>&gt;</button>
        <button onClick={moveLeft}>&lt;</button>
      </div>

      {/* Right list */}
      <div>
        <div>
          <strong>{rightTitle}</strong>
        </div>
        <br />
        <ul
          style={{
            border: "1px solid #ccc",
            minHeight: "200px",
            width: "200px",
            listStyle: "none",
            padding: 0,
          }}
        >
          {rightList?.map((item) => (
            <li
              key={item.id}
              onClick={() => toggleSelection(item, "right")}
              style={{
                padding: "5px",
                cursor: "pointer",
                backgroundColor: selectedRight.has(item) ? "#cce5ff" : "white",
              }}
            >
              {renderItem(item.value)}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
