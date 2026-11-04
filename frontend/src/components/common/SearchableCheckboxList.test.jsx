import React, { useState } from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import messages from "../../languages/en.json";
import SearchableCheckboxList from "./SearchableCheckboxList";

const ITEMS = [
  { id: "1", name: "Hématocrite" },
  { id: "2", name: "Glucose" },
  { id: "3", name: "Hemoglobin" },
  { id: "4", name: "Créatinine" },
];

const Controlled = ({ onToggle = vi.fn(), initial = [] }) => {
  const [selected, setSelected] = useState(initial);
  return (
    <IntlProvider locale="en" messages={messages}>
      <SearchableCheckboxList
        id="probe"
        idPrefix="test_"
        searchLabel="Search tests"
        items={ITEMS}
        selectedIds={selected}
        onToggle={(item, checked) => {
          setSelected((prev) =>
            checked ? [...prev, item.id] : prev.filter((id) => id !== item.id),
          );
          onToggle(item, checked);
        }}
      />
    </IntlProvider>
  );
};

const listed = () =>
  screen.queryAllByRole("checkbox").map((box) => box.labels[0].textContent);

describe("SearchableCheckboxList", () => {
  it("lists every item until something is typed", () => {
    render(<Controlled />);

    expect(listed()).toEqual([
      "Hématocrite",
      "Glucose",
      "Hemoglobin",
      "Créatinine",
    ]);
    expect(screen.getByLabelText("Glucose")).toHaveAttribute("id", "test_2");
  });

  it("narrows the list to the items containing what is typed, ignoring case and accents", async () => {
    render(<Controlled />);

    await userEvent.type(
      screen.getByRole("searchbox", { name: "Search tests" }),
      "HEM",
    );
    expect(listed()).toEqual(["Hématocrite", "Hemoglobin"]);

    await userEvent.clear(
      screen.getByRole("searchbox", { name: "Search tests" }),
    );
    await userEvent.type(
      screen.getByRole("searchbox", { name: "Search tests" }),
      "creat",
    );
    expect(listed()).toEqual(["Créatinine"]);
  });

  it("says nothing matches when no item contains the typed text", async () => {
    render(<Controlled />);

    await userEvent.type(
      screen.getByRole("searchbox", { name: "Search tests" }),
      "zzz",
    );

    expect(listed()).toEqual([]);
    expect(screen.getByText('Nothing matches "zzz".')).toBeInTheDocument();
  });

  it("ticks an item with the keyboard and shows it as a tag that stays while the search hides it", async () => {
    const onToggle = vi.fn();
    render(<Controlled onToggle={onToggle} />);

    await userEvent.tab();
    await userEvent.keyboard("gluc");
    await userEvent.tab();
    expect(screen.getByRole("button", { name: /clear/i })).toHaveFocus();
    await userEvent.tab();
    expect(screen.getByLabelText("Glucose")).toHaveFocus();
    await userEvent.keyboard(" ");
    expect(onToggle).toHaveBeenCalledWith(ITEMS[1], true);
    expect(screen.getByLabelText("Glucose")).toBeChecked();

    await userEvent.clear(
      screen.getByRole("searchbox", { name: "Search tests" }),
    );
    await userEvent.type(
      screen.getByRole("searchbox", { name: "Search tests" }),
      "hemo",
    );
    const chosen = screen.getByTestId("probe-selected");
    expect(within(chosen).getByText("Glucose")).toBeInTheDocument();
    expect(listed()).toEqual(["Hemoglobin"]);
  });

  it("unticks an item when its tag is dismissed", async () => {
    const onToggle = vi.fn();
    render(<Controlled onToggle={onToggle} initial={["4"]} />);

    const chosen = screen.getByTestId("probe-selected");
    await userEvent.click(within(chosen).getByRole("button"));

    expect(onToggle).toHaveBeenCalledWith(ITEMS[3], false);
    expect(screen.getByLabelText("Créatinine")).not.toBeChecked();
    expect(screen.queryByTestId("probe-selected")).not.toBeInTheDocument();
  });
});
