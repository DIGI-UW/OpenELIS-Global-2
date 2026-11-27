/**
 * The chip row shared by the Findings pickers: what a chip reads, how its
 * remove button is named, and where focus goes once a chip is removed.
 */
import React, { useState } from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import SelectionChips from "./SelectionChips";

const he = { id: "41", value: "H&E" };
const pas = { id: "42", value: "PAS" };
const long = {
  id: "43",
  value: "Periodic acid-Schiff with diastase digestion, long form",
};

const name = (item) => item.value;

const Harness = ({ initial, readOnly = false }) => {
  const [items, setItems] = useState(initial);
  return (
    <>
      <input id="chips-fallback" aria-label="Fallback" />
      <SelectionChips
        items={items}
        label={name}
        onRemove={(_, position) =>
          setItems((prev) => prev.filter((__, index) => index !== position))
        }
        readOnly={readOnly}
        testId="chips"
        fallbackFocusId="chips-fallback"
      />
    </>
  );
};

const renderChips = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <Harness {...props} />
    </IntlProvider>,
  );

const removeButton = (label) =>
  within(screen.getByTestId("chips")).getByRole("button", {
    name: "Remove " + label,
  });

describe("SelectionChips", () => {
  it("removing a chip moves focus to the chip now at its place, else the last chip, else the named element", async () => {
    renderChips({ initial: [he, pas, long] });

    await userEvent.click(removeButton("PAS"));
    await waitFor(() => expect(removeButton(long.value)).toHaveFocus());

    await userEvent.click(removeButton(long.value));
    await waitFor(() => expect(removeButton("H&E")).toHaveFocus());

    await userEvent.click(removeButton("H&E"));
    await waitFor(() =>
      expect(screen.getByRole("textbox", { name: "Fallback" })).toHaveFocus(),
    );
  });

  it("read-only shows plain tags without remove buttons, each label titled with its full text", () => {
    renderChips({ initial: [he, long], readOnly: true });

    const row = screen.getByTestId("chips");
    expect(within(row).queryAllByRole("button")).toHaveLength(0);
    const labels = Array.from(row.querySelectorAll(".cds--tag__label"));
    expect(labels.map((label) => label.getAttribute("title"))).toEqual([
      "H&E",
      long.value,
    ]);
  });
});
