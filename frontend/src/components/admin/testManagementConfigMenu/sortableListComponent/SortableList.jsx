import React, { useEffect, useState } from "react";
import { Draggable } from "@carbon/icons-react";
import { Button, Checkbox } from "@carbon/react";
import { FormattedMessage } from "react-intl";

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

export const SortableResultSelectionOptionList = ({
  test,
  onSort,
  onRemove,
}) => {
  const [tests, setTests] = useState(test);

  const handleDragStart = (e, index) => {
    e.dataTransfer.setData("dragIndex", index);
  };

  const handleDragOver = (e) => e.preventDefault();

  const handleDrop = (e, dropIndex) => {
    const dragIndex = e.dataTransfer.getData("dragIndex");
    if (dragIndex === dropIndex) return;

    const newItems = [...tests.items];
    const [draggedItem] = newItems.splice(dragIndex, 1);
    newItems.splice(dropIndex, 0, draggedItem);
    const updatedItems = newItems.map((item, index) => ({
      ...item,
      order: index,
    }));

    setTests({
      ...tests,
      items: updatedItems,
    });

    onSort({
      ...tests,
      items: updatedItems,
    });
  };

  useEffect(() => {
    setTests(test);
    onSort(test);
  }, [test]);

  return (
    <div
      style={{
        width: "300px",
        border: "1px solid #ccc",
        padding: "10px",
        margin: "10px",
      }}
    >
      {tests &&
        typeof tests === "object" &&
        tests !== null &&
        "description" in tests && (
          <h3
            key={tests?.id}
            style={{
              background: "#cce0d0",
              padding: "5px",
              textAlign: "center",
            }}
          >
            {tests?.description}
          </h3>
        )}
      {tests &&
        tests?.items &&
        tests?.items?.length > 0 &&
        tests?.items?.map((item, index) => (
          <div
            key={item.id}
            draggable
            onDragStart={(e) => handleDragStart(e, index)}
            onDragOver={handleDragOver}
            onDrop={(e) => handleDrop(e, index)}
            style={{
              padding: "10px",
              margin: "5px 0",
              background: item.qualifiable ? "#cfc" : "#eee",
              display: "flex",
              alignItems: "center",
              cursor: "grab",
              border: "1px solid #bbb",
            }}
          >
            <Draggable
              aria-label="result-select-values-list-draggable"
              size={24}
              key={item.id}
            />
            {item.value}
          </div>
        ))}
      <div style={{ margin: "10px", textAlign: "center" }}>
        {test.items.map((item, index) => {
          return (
            <div key={item.id ?? index}>
              {Object.prototype.hasOwnProperty.call(item, "qualifiable") && (
                <Checkbox
                  id={`qualifiable-checkbox-${test.id}-${index}`}
                  labelText={"Qualifiable"}
                  checked={item.qualifiable}
                  onChange={() => {
                    const updatedItems = test.items.map((itm, idx) =>
                      idx === index
                        ? { ...itm, qualifiable: !itm.qualifiable }
                        : itm,
                    );

                    const updatedTest = { ...test, items: updatedItems };
                    onSort(updatedTest);
                  }}
                />
              )}
              {Object.prototype.hasOwnProperty.call(item, "normal") && (
                <Checkbox
                  id={`normal-checkbox-${test.id}-${index}`}
                  labelText={"Normal"}
                  onChange={() => {
                    const updatedItems = test.items.map((itm, idx) =>
                      idx === index ? { ...itm, normal: !itm.normal } : itm,
                    );
                    const updatedTest = { ...test, items: updatedItems };
                    onSort(updatedTest);
                    alert(
                      <>
                        <FormattedMessage id="configuration.selectList.confirmChange" />
                      </>,
                    );
                  }}
                />
              )}
            </div>
          );
        })}
        <br />
        <Button
          type="button"
          kind="tertiary"
          onClick={() => {
            onRemove(tests?.id);
          }}
        >
          <FormattedMessage id="label.button.remove" />
        </Button>
      </div>
    </div>
  );
};

export const CustomCommonSortableOrderList = ({
  test,
  onSort,
  disableSorting,
}) => {
  const [tests, setTests] = useState(test);

  const handleDragStart = (e, index) => {
    e.dataTransfer.setData("dragIndex", index);
  };

  const handleDragOver = (e) => e.preventDefault();

  const handleDrop = (e, dropIndex) => {
    const dragIndex = e.dataTransfer.getData("dragIndex");
    if (dragIndex === dropIndex) return;

    const newItems = [...tests];
    const [draggedItem] = newItems.splice(dragIndex, 1);
    newItems.splice(dropIndex, 0, draggedItem);
    const updatedItems = newItems.map((item, index) => ({
      ...item,
      sortOrder: index,
    }));

    setTests(updatedItems);
    onSort(updatedItems);
  };

  useEffect(() => {
    const alreadySorted = test?.every(
      (item, index) => item.sortOrder === index,
    );

    if (!alreadySorted) {
      const initialized = test.map((item, index) => ({
        ...item,
        sortOrder: index,
      }));
      setTests(initialized);
      onSort(initialized);
    } else {
      setTests(test);
    }
  }, [test]);

  return (
    <div
      style={{
        width: "300px",
        border: "1px solid #ccc",
        padding: "10px",
        margin: "10px",
      }}
    >
      {tests.map((test, index) => (
        <div
          key={test.id}
          draggable={!disableSorting}
          onDragStart={
            disableSorting ? undefined : (e) => handleDragStart(e, index)
          }
          onDragOver={disableSorting ? undefined : handleDragOver}
          onDrop={disableSorting ? undefined : (e) => handleDrop(e, index)}
          style={{
            padding: "10px",
            margin: "5px 0",
            background: "#eee",
            display: "flex",
            alignItems: "center",
            cursor: "grab",
            border: "1px solid #bbb",
          }}
        >
          <Draggable aria-label="sample-type-list-draggable" size={24} />
          {test.value ? test.value : test.name}
        </div>
      ))}
    </div>
  );
};
