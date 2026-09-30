import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";

const location = vi.hoisted(() => ({ pathname: "/order/clinical/qa" }));
vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push: vi.fn() }),
  useLocation: () => location,
}));
vi.mock("./OrderContext", () => ({
  useOrderContext: () => ({
    isSubmitting: false,
    isReadOnly: false,
    isEditMode: false,
    saveOrder: vi.fn(),
    labNumber: "DEV01260000000000100",
  }),
}));

import SaveNavigationButtons from "./SaveNavigationButtons";

const renderButtons = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <SaveNavigationButtons currentStep={3} {...props} />
    </IntlProvider>,
  );

describe("SaveNavigationButtons on the last step", () => {
  it("Submit runs the step's submit handler, not its plain save", async () => {
    const onSave = vi.fn();
    const onSaveAndNext = vi.fn();
    renderButtons({ onSave, onSaveAndNext });

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Submit" }));

    expect(onSaveAndNext).toHaveBeenCalledTimes(1);
    expect(onSave).not.toHaveBeenCalled();
  });

  it("Save still runs the plain save", async () => {
    const onSave = vi.fn();
    const onSaveAndNext = vi.fn();
    renderButtons({ onSave, onSaveAndNext });

    await userEvent.setup().click(screen.getByRole("button", { name: "Save" }));

    expect(onSave).toHaveBeenCalledTimes(1);
    expect(onSaveAndNext).not.toHaveBeenCalled();
  });

  it("Submit falls back to the plain save when the step has no submit handler", async () => {
    const onSave = vi.fn();
    renderButtons({ onSave });

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Submit" }));

    expect(onSave).toHaveBeenCalledTimes(1);
  });
});
