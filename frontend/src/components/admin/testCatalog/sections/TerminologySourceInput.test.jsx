import React, { useState } from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import TerminologySourceInput from "./TerminologySourceInput";
import messages from "../../../../languages/en.json";

let latest;

const Field = () => {
  const [source, setSource] = useState("");
  latest = source;
  return (
    <>
      <TerminologySourceInput
        id="source"
        titleText="Terminology source"
        value={source}
        onChange={setSource}
      />
      <input aria-label="Code" />
    </>
  );
};

const renderField = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <Field />
    </IntlProvider>,
  );

const sourceField = () =>
  screen.getByRole("combobox", { name: "Terminology source" });

describe("TerminologySourceInput", () => {
  it("keeps a typed source when the user moves on", async () => {
    renderField();

    await userEvent.type(sourceField(), "CPHL");
    await userEvent.click(screen.getByLabelText("Code"));

    expect(latest).toBe("CPHL");
    expect(sourceField()).toHaveValue("CPHL");
  });

  it("narrows the suggestions to what is typed", async () => {
    renderField();

    await userEvent.type(sourceField(), "sno");

    expect(
      screen.getAllByRole("option").map((option) => option.textContent),
    ).toEqual(["SNOMED"]);
  });

  it("takes a suggestion the user picks", async () => {
    renderField();

    await userEvent.click(sourceField());
    await userEvent.click(screen.getByRole("option", { name: "CIEL" }));

    expect(latest).toBe("CIEL");
  });
});
