import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import PatientImageSelector from "./PatientImageSelector";

/**
 * The patient form disables its fields with a fieldset in view mode. A dialog
 * rendered inside that fieldset inherits the disabled state, which took the
 * dialog's own Close and Cancel buttons with it — so the dialogs render through a
 * portal instead. View mode still governs editing: clicking the photo opens the
 * read-only viewer rather than the picker.
 */

const PHOTO = "data:image/jpeg;base64,AAAA";

const renderInDisabledForm = (disabled: boolean) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <fieldset disabled={disabled}>
        <PatientImageSelector
          value={PHOTO}
          onChange={() => {}}
          disabled={disabled}
        />
      </fieldset>
    </IntlProvider>,
  );

describe("PatientImageSelector", () => {
  it("renders its dialogs outside the disabled fieldset", () => {
    const { container } = renderInDisabledForm(true);

    const fieldset = container.querySelector("fieldset");
    expect(fieldset).toBeDisabled();
    // The dialogs must not live inside the fieldset, or its disabled state would
    // reach their controls.
    expect(fieldset?.querySelector(".cds--modal")).toBeNull();
    expect(document.body.querySelector(".cds--modal")).not.toBeNull();
  });

  it("opens the read-only viewer when the form is in view mode", async () => {
    renderInDisabledForm(true);

    const display = document.querySelector(".image-display") as HTMLElement;
    fireEvent.click(display);

    const heading = await screen.findByRole("heading", {
      name: messages["patient.photo.view"] as string,
    });
    expect(heading.closest(".cds--modal")).toHaveClass("is-visible");
    expect(
      screen
        .getByRole("heading", {
          name: messages["patient.photo.modal.heading"] as string,
        })
        .closest(".cds--modal"),
    ).not.toHaveClass("is-visible");
  });

  it("labels the photo for viewing, not retaking, in view mode", () => {
    renderInDisabledForm(true);

    const tile = document.querySelector(".image-display") as HTMLElement;
    expect(tile.querySelector(".overlay-text")?.textContent?.trim()).toBe(
      messages["patient.photo.view"],
    );
    expect(tile.textContent).not.toContain(messages["patient.photo.retake"]);
    // The whole tile opens the viewer, and a button inside the form's disabled
    // fieldset would only render as a dead control.
    expect(tile.querySelector(".patient-photo-view-btn")).toBeNull();
  });

  it("opens the picker, not the viewer, in edit mode", async () => {
    renderInDisabledForm(false);

    fireEvent.click(document.querySelector(".image-display") as HTMLElement);

    expect(
      screen
        .getByRole("heading", {
          name: messages["patient.photo.modal.heading"] as string,
        })
        .closest(".cds--modal"),
    ).toHaveClass("is-visible");
    expect(
      screen
        .getByRole("heading", {
          name: messages["patient.photo.view"] as string,
        })
        .closest(".cds--modal"),
    ).not.toHaveClass("is-visible");
    expect(document.querySelector(".overlay-text")?.textContent?.trim()).toBe(
      messages["patient.photo.retake"],
    );
  });

  it("offers nothing to click when there is no photo to view", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <PatientImageSelector value={null} onChange={() => {}} disabled />
      </IntlProvider>,
    );

    expect(
      screen.queryByText(messages["patient.photo.add"] as string),
    ).toBeNull();
    fireEvent.click(document.querySelector(".image-display") as HTMLElement);
    const openDialogs = Array.from(
      document.body.querySelectorAll(".cds--modal.is-visible"),
    );
    expect(openDialogs).toHaveLength(0);
  });

  it("leaves every dialog control enabled in view mode", () => {
    renderInDisabledForm(true);

    const dialogs = Array.from(document.body.querySelectorAll(".cds--modal"));
    expect(dialogs.length).toBeGreaterThan(0);
    for (const dialog of dialogs) {
      expect(dialog.querySelectorAll("button:disabled")).toHaveLength(0);
    }
  });
});
