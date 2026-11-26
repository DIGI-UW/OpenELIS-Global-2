/**
 * The Reagents section saves an edit without a React error on the page. Kept in
 * its own file: React prints each warning once per module, so a warning another
 * test raised first would hide it here.
 */
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ReagentsSection from "./ReagentsSection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const LINK = {
  id: "link-1",
  reagentId: 7,
  reagentName: "Glucose Reagent",
  manufacturer: "Acme",
  usageType: "PRIMARY",
  quantityPerTest: 2.5,
  quantityUnit: "mL",
  currentStock: 120,
  lowStockThreshold: 50,
};

it("saves a quantity and a unit without a React error on the page", async () => {
  getFromOpenElisServer.mockImplementation((url, cb) => cb([LINK]));
  putToOpenElisServer.mockImplementation((url, body, cb) => cb(200));
  const errors = vi.spyOn(console, "error").mockImplementation(() => {});
  render(
    <IntlProvider locale="en" messages={messages}>
      <ReagentsSection testId="42" />
    </IntlProvider>,
  );
  const qty = await screen.findByDisplayValue("2.5");

  fireEvent.change(qty, { target: { value: "0.5" } });
  const unit = document.getElementById("unit-link-1");
  await userEvent.clear(unit);
  await userEvent.type(unit, "uL");
  await userEvent.click(screen.getByText("Glucose Reagent"));
  fireEvent.click(screen.getByTestId("reagents-save"));

  expect(
    await screen.findByText(messages["label.testCatalog.reagents.updated"]),
  ).toBeInTheDocument();
  expect(errors).not.toHaveBeenCalled();
  errors.mockRestore();
});
