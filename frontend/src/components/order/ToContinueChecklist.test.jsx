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

  // OGC-1240: an existing patient's National ID is not on the page until its
  // details are opened and unlocked, so the link reaches the control that does.
  it.each([
    ["the field when it is open and enabled", ["nationalId"], "National ID"],
    [
      "the Edit toggle when the field is locked",
      ["nationalId-locked", "patient-edit-toggle"],
      "Edit",
    ],
    [
      "Edit details when the patient's details are closed",
      ["patient-edit-details"],
      "Edit details",
    ],
  ])("focuses %s", async (_, present, focusedName) => {
    Element.prototype.scrollIntoView = () => {};
    renderChecklist(
      [
        {
          id: "n",
          label: "Enter the patient's National ID",
          targetId: [
            "nationalId",
            "patient-edit-toggle",
            "patient-edit-details",
          ],
        },
      ],
      <div>
        {present.includes("nationalId") && (
          <input id="nationalId" aria-label="National ID" />
        )}
        {present.includes("nationalId-locked") && (
          <fieldset disabled>
            <input id="nationalId" aria-label="National ID" />
          </fieldset>
        )}
        {present.includes("patient-edit-toggle") && (
          <button id="patient-edit-toggle">Edit</button>
        )}
        {present.includes("patient-edit-details") && (
          <button id="patient-edit-details">Edit details</button>
        )}
      </div>,
    );

    await userEvent
      .setup()
      .click(
        screen.getByRole("link", { name: "Enter the patient's National ID" }),
      );

    expect(
      screen.getByRole(focusedName === "National ID" ? "textbox" : "button", {
        name: focusedName,
      }),
    ).toHaveFocus();
  });
});
