import React, { useState } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { describe, expect, it, vi } from "vitest";
import SearchableSelect from "./SearchableSelect";

const OPTIONS = [
  { id: "1", value: "Hématologie" },
  { id: "2", value: "Biochimie" },
  { id: "3", value: "Sérologie VIH" },
  { id: "4", value: "Hemoglobin" },
];

const Controlled = ({ onChange, initial = "" }) => {
  const [value, setValue] = useState(initial);
  return (
    <SearchableSelect
      id="probe"
      labelText="Lab unit"
      placeholder="Type to search"
      options={OPTIONS}
      value={value}
      onChange={(id) => {
        setValue(id);
        onChange(id);
      }}
    />
  );
};

const optionNames = () =>
  screen.queryAllByRole("option").map((option) => option.textContent);

describe("SearchableSelect", () => {
  it("narrows the list to the options containing what is typed, ignoring case and accents", async () => {
    render(<Controlled onChange={vi.fn()} />);

    await userEvent.type(
      screen.getByRole("combobox", { name: "Lab unit" }),
      "HEM",
    );

    expect(optionNames()).toEqual(["Hématologie", "Hemoglobin"]);
  });

  it("matches the middle of a label", async () => {
    render(<Controlled onChange={vi.fn()} />);

    await userEvent.type(
      screen.getByRole("combobox", { name: "Lab unit" }),
      "vih",
    );

    expect(optionNames()).toEqual(["Sérologie VIH"]);
  });

  it("is usable with the keyboard alone and reports the chosen id", async () => {
    const onChange = vi.fn();
    render(<Controlled onChange={onChange} />);
    const box = screen.getByRole("combobox", { name: "Lab unit" });

    await userEvent.type(box, "bio");
    await userEvent.keyboard("{ArrowDown}{Enter}");

    expect(onChange).toHaveBeenLastCalledWith("2");
    expect(box).toHaveValue("Biochimie");
  });

  it("shows the selected option's label and offers every option when reopened", async () => {
    render(<Controlled onChange={vi.fn()} initial="3" />);
    const box = screen.getByRole("combobox", { name: "Lab unit" });

    expect(box).toHaveValue("Sérologie VIH");
    await userEvent.click(box);

    expect(optionNames()).toHaveLength(OPTIONS.length);
  });

  it("clearing the choice reports an empty id", async () => {
    const onChange = vi.fn();
    render(<Controlled onChange={onChange} initial="2" />);

    await userEvent.click(screen.getByRole("button", { name: /clear/i }));

    expect(onChange).toHaveBeenLastCalledWith("");
  });

  it("does not report a value the parent set, only a choice the user made", async () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <SearchableSelect
        id="probe"
        labelText="Lab unit"
        options={[]}
        value="2"
        onChange={onChange}
      />,
    );
    rerender(
      <SearchableSelect
        id="probe"
        labelText="Lab unit"
        options={OPTIONS}
        value="2"
        onChange={onChange}
      />,
    );
    rerender(
      <SearchableSelect
        id="probe"
        labelText="Lab unit"
        options={OPTIONS}
        value="3"
        onChange={onChange}
      />,
    );

    expect(screen.getByRole("combobox")).toHaveValue("Sérologie VIH");
    expect(onChange).not.toHaveBeenCalled();

    await userEvent.clear(screen.getByRole("combobox"));
    await userEvent.type(screen.getByRole("combobox"), "biochi");
    await userEvent.click(screen.getByRole("option", { name: "Biochimie" }));
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenLastCalledWith("2");
  });
});
