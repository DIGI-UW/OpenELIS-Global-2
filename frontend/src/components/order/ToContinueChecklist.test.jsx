import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import ToContinueChecklist from "./ToContinueChecklist";

const renderChecklist = (items, extra = null) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {extra}
      <ToContinueChecklist nextStep="Prepare Samples" items={items} />
    </IntlProvider>,
  );

// OGC-1266 FR-A9: the checklist names what the step still needs and each
// item takes the user to its field.
describe("ToContinueChecklist", () => {
  it("renders nothing once the step is complete", () => {
    const { container } = renderChecklist([]);
    expect(container).toBeEmptyDOMElement();
  });

  it("names the next step and lists the missing items", () => {
    renderChecklist([
      { id: "a", label: "Add a lab number", targetId: "labNumber" },
      { id: "b", label: "Select or create the patient", targetId: "x" },
    ]);

    expect(
      screen.getByRole("region", { name: "To continue to Prepare Samples" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Add a lab number")).toBeInTheDocument();
    expect(
      screen.getByText("Select or create the patient"),
    ).toBeInTheDocument();
  });

  it("focuses the item's field when it is clicked", async () => {
    Element.prototype.scrollIntoView = () => {};
    renderChecklist(
      [{ id: "a", label: "Add a lab number", targetId: "labNumber" }],
      <input id="labNumber" aria-label="Lab Number" />,
    );

    await userEvent
      .setup()
      .click(screen.getByRole("link", { name: "Add a lab number" }));

    expect(screen.getByRole("textbox", { name: "Lab Number" })).toHaveFocus();
  });
});
