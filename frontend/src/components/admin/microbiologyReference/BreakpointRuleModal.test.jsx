import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import { vi } from "vitest";
import BreakpointRuleModal from "./BreakpointRuleModal";

test("an any-organism rule can be saved", () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <BreakpointRuleModal
        value={{
          id: "rule-1",
          organismId: "",
          organismGroup: "",
          antibioticId: "agent-1",
          method: "MIC",
          breakpointType: "MIC",
          susceptibleValue: 1,
        }}
        organisms={[{ id: "organism-1", label: "Escherichia coli" }]}
        organismGroups={[]}
        antibiotics={[{ id: "agent-1", label: "Ciprofloxacin" }]}
        specimenTypes={[]}
        onClose={() => {}}
        onSave={() => {}}
      />
    </IntlProvider>,
  );

  expect(screen.getByRole("button", { name: "Save" })).toBeEnabled();
  expect(
    screen.getByRole("option", { name: "Any organism" }),
  ).toBeInTheDocument();
});

test("a zone rule needs its disk content before it can be saved", () => {
  const onSave = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <BreakpointRuleModal
        value={{
          organismId: "organism-1",
          antibioticId: "agent-1",
          method: "ZONE",
          breakpointType: "ZONE",
          susceptibleValue: 20,
          resistantValue: 17,
        }}
        organisms={[{ id: "organism-1", label: "Escherichia coli" }]}
        organismGroups={[]}
        antibiotics={[{ id: "agent-1", label: "Ciprofloxacin" }]}
        specimenTypes={[]}
        onClose={() => {}}
        onSave={onSave}
      />
    </IntlProvider>,
  );

  const save = screen.getByRole("button", { name: "Save" });
  expect(save).toBeDisabled();
  fireEvent.change(screen.getByLabelText(/Disk content/), {
    target: { value: "30ug" },
  });
  expect(save).toBeEnabled();
  fireEvent.click(save);
  expect(onSave).toHaveBeenCalledWith(
    expect.objectContaining({ diskContent: "30ug" }),
  );
});
