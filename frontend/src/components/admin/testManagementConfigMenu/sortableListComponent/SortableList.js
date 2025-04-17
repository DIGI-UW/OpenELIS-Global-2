import React, { useEffect, useState } from "react";
import { Draggable } from "@carbon/icons-react";

export const SortableTestList = ({ sampleType, tests, onSort }) => {
  const [items, setItems] = useState(tests);

  const handleDragStart = (e, index) => {
    e.dataTransfer.setData("dragIndex", index);
  };

  const handleDragOver = (e) => e.preventDefault();

  const handleDrop = (e, dropIndex) => {
    const dragIndex = e.dataTransfer.getData("dragIndex");
    if (dragIndex === dropIndex) return;

    const newItems = [...items];
    const [draggedItem] = newItems.splice(dragIndex, 1);
    newItems.splice(dropIndex, 0, draggedItem);
    const updatedItems = newItems.map((item, index) => ({
      ...item,
      sortOrder: index,
    }));
    setItems(updatedItems);

    const sortedSendout = updatedItems.map((item, index) => ({
      id: Number(item.id),
      activated: item.activated ?? false,
      sortOrder: index,
    }));

    onSort(sortedSendout);
  };

  useEffect(() => {
    setItems(tests);
  }, [tests]);

  return (
    <div style={{ width: "300px", border: "1px solid #ccc", padding: "10px" }}>
      <h3
        style={{ background: "#cce0d0", padding: "5px", textAlign: "center" }}
      >
        {sampleType}
      </h3>
      {items.map((test, index) => (
        <div
          key={test.id}
          draggable
          onDragStart={(e) => handleDragStart(e, index)}
          onDragOver={handleDragOver}
          onDrop={(e) => handleDrop(e, index)}
          style={{
            padding: "10px",
            margin: "5px 0",
            background: test.activated ? "#cfc" : "#eee",
            display: "flex",
            alignItems: "center",
            cursor: "grab",
            border: "1px solid #bbb",
          }}
        >
          <Draggable aria-label="test-list-draggable" size={24} />
          {test.value}
        </div>
      ))}
    </div>
  );
};

export const SortableSampleTypeList = ({ tests, onSort }) => {
  const [items, setItems] = useState(tests);

  const handleDragStart = (e, index) => {
    e.dataTransfer.setData("dragIndex", index);
  };

  const handleDragOver = (e) => e.preventDefault();

  const handleDrop = (e, dropIndex) => {
    const dragIndex = e.dataTransfer.getData("dragIndex");
    if (dragIndex === dropIndex) return;

    const newItems = [...items];
    const [draggedItem] = newItems.splice(dragIndex, 1);
    newItems.splice(dropIndex, 0, draggedItem);
    const updatedItems = newItems.map((item, index) => ({
      ...item,
      sortOrder: index,
    }));

    setItems(updatedItems);

    const sortedSendout = updatedItems.map((item, index) => {
      return {
        id: Number(item.id),
        activated: item.activated ?? false,
        sortOrder: index,
      };
    });

    onSort(sortedSendout);
  };

  useEffect(() => {
    setItems(tests);
  }, [tests]);

  return (
    <div style={{ width: "300px", border: "1px solid #ccc", padding: "10px" }}>
      <h3
        style={{ background: "#cce0d0", padding: "5px", textAlign: "center" }}
      >
        Sample Types
      </h3>
      {items.map((test, index) => (
        <div
          key={test.id}
          draggable
          onDragStart={(e) => handleDragStart(e, index)}
          onDragOver={handleDragOver}
          onDrop={(e) => handleDrop(e, index)}
          style={{
            padding: "10px",
            margin: "5px 0",
            background: test.activated ? "#cfc" : "#eee",
            display: "flex",
            alignItems: "center",
            cursor: "grab",
            border: "1px solid #bbb",
          }}
        >
          <Draggable aria-label="sample-type-list-draggable" size={24} />
          {test.value}
        </div>
      ))}
    </div>
  );
};
