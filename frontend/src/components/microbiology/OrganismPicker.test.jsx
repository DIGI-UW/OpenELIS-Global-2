import React, { useState } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";
import OrganismPicker from "./OrganismPicker";

const ORGANISM = { id: "ab", label: "Acinetobacter baumannii", code: "aba" };

vi.mock("./OrganismSearch", async (original) => ({
  ...(await original()),
  searchOrganisms: () => Promise.resolve([ORGANISM]),
  getOrganism: () => Promise.resolve(ORGANISM),
}));

const Picker = ({ initial, onChange }) => {
  const [value, setValue] = useState(initial);
  return (
    <IntlProvider locale="en" messages={messages}>
      <OrganismPicker
        id="organism"
        titleText="Organism"
        value={value}
        onChange={(id) => {
          onChange(id);
          setValue(id);
        }}
      />
      <button>Next field</button>
    </IntlProvider>
  );
};

test("erasing the name of a stored organism clears the choice", async () => {
  const onChange = vi.fn();
  render(<Picker initial="ab" onChange={onChange} />);
  const input = await screen.findByDisplayValue(
    "Acinetobacter baumannii (aba)",
  );

  act(() => input.focus());
  fireEvent.change(input, { target: { value: "" } });

  expect(onChange).toHaveBeenCalledWith("");
  expect(input).toHaveValue("");
  act(() => input.blur());
  expect(input).toHaveValue("");
});

test("erasing the name of an organism just chosen clears the choice", async () => {
  const onChange = vi.fn();
  render(<Picker initial="" onChange={onChange} />);
  const input = screen.getByRole("combobox", { name: "Organism" });
  act(() => input.focus());
  fireEvent.click(input);
  fireEvent.click(
    await screen.findByRole("option", { name: /Acinetobacter baumannii/ }),
  );
  expect(onChange).toHaveBeenLastCalledWith("ab");
  expect(input).toHaveValue("Acinetobacter baumannii (aba)");

  fireEvent.change(input, { target: { value: "" } });

  expect(onChange).toHaveBeenLastCalledWith("");
  expect(input).toHaveValue("");
  act(() => input.blur());
  expect(input).toHaveValue("");
});

test("text that matches nothing leaves the stored organism in place", async () => {
  const onChange = vi.fn();
  render(<Picker initial="ab" onChange={onChange} />);
  const input = await screen.findByDisplayValue(
    "Acinetobacter baumannii (aba)",
  );

  act(() => input.focus());
  fireEvent.change(input, { target: { value: "zzz" } });
  act(() => input.blur());

  expect(onChange).not.toHaveBeenCalled();
  expect(input).toHaveValue("Acinetobacter baumannii (aba)");
});

test("leaving the field on a highlighted row chooses it", async () => {
  const onChange = vi.fn();
  render(<Picker initial="" onChange={onChange} />);
  const input = screen.getByRole("combobox", { name: "Organism" });
  act(() => input.focus());
  fireEvent.change(input, { target: { value: "aci" } });
  await screen.findByRole("option", { name: /Acinetobacter baumannii/ });

  fireEvent.keyDown(input, { key: "ArrowDown" });
  act(() => screen.getByRole("button", { name: "Next field" }).focus());

  expect(input).toHaveValue("Acinetobacter baumannii (aba)");
  expect(onChange).toHaveBeenCalledTimes(1);
  expect(onChange).toHaveBeenCalledWith("ab");
});

test("a clicked row is reported once", async () => {
  const onChange = vi.fn();
  render(<Picker initial="" onChange={onChange} />);
  const input = screen.getByRole("combobox", { name: "Organism" });
  act(() => input.focus());
  fireEvent.click(input);

  fireEvent.click(
    await screen.findByRole("option", { name: /Acinetobacter baumannii/ }),
  );

  expect(onChange).toHaveBeenCalledTimes(1);
  expect(onChange).toHaveBeenCalledWith("ab");
});
