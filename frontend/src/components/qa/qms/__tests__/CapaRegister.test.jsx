import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import { getFromOpenElisServer } from "../../../utils/Utils";
import CapaRegister from "../CapaRegister";

vi.mock("../../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
}));

const ACTIONS = [
  {
    id: 1,
    nceNumber: "NCE-FEB",
    correctiveAction: "Recalibrate",
    personResponsible: "Ann",
    dueDate: "2026-02-10",
  },
  {
    id: 2,
    nceNumber: "NCE-AUG",
    correctiveAction: "Retrain",
    personResponsible: "Ben",
    dueDate: "2026-08-10",
  },
];

const renderRegister = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <CapaRegister />
        <button type="button">Elsewhere</button>
      </IntlProvider>
    </MemoryRouter>,
  );

const typeAndLeave = (input, text) => {
  act(() => input.focus());
  fireEvent.click(input);
  fireEvent.input(input, { target: { value: text } });
  const elsewhere = screen.getByRole("button", { name: "Elsewhere" });
  elsewhere.dispatchEvent(new Event("touchstart", { bubbles: true }));
  elsewhere.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
  elsewhere.focus();
};

describe("CAPA register due-date range", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(ACTIONS),
    );
  });

  it("keeps the start of the range while the end is still to be entered", async () => {
    renderRegister();
    const from = document.getElementById("capa-due-from");
    typeAndLeave(from, "2026-01-01");

    await waitFor(() => expect(from).toHaveValue("2026-01-01"));
    expect(screen.getByText("NCE-AUG")).toBeInTheDocument();
  });

  it("filters by due date once both ends are entered", async () => {
    renderRegister();

    typeAndLeave(document.getElementById("capa-due-from"), "2026-01-01");
    typeAndLeave(document.getElementById("capa-due-to"), "2026-03-31");

    await waitFor(() =>
      expect(screen.queryByText("NCE-AUG")).not.toBeInTheDocument(),
    );
    expect(screen.getByText("NCE-FEB")).toBeInTheDocument();
  });
});
