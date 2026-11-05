import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
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
