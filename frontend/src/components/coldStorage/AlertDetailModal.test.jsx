import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import AlertDetailModal from "./AlertDetailModal";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { deleteAlert } from "./api";
import { Roles } from "../utils/Utils";

vi.mock("./api", () => ({
  fetchAlertDetails: vi.fn(() =>
    Promise.resolve({
      id: 7,
      alertType: "FREEZER_TEMPERATURE",
      alertEntityType: "Freezer",
      alertEntityId: 100,
      severity: "CRITICAL",
      status: "OPEN",
      message: "Temperature threshold violated",
    }),
  ),
  acknowledgeAlert: vi.fn(),
  resolveAlert: vi.fn(),
  deleteAlert: vi.fn(),
  createCorrectiveAction: vi.fn(),
}));

/**
 * The action-type labels must come from the translation catalogue, not from a
 * hardcoded English list, so a non-English deployment does not get an English
 * dropdown inside an otherwise translated modal.
 */
describe("AlertDetailModal corrective-action types", () => {
  it("renders the action-type options from the translation catalogue", async () => {
    render(
      <IntlProvider
        locale="en"
        messages={{
          ...messages,
          "coldStorage.correctiveAction.type.calibration": "Étalonnage",
        }}
      >
        <AlertDetailModal alertId={7} open onClose={vi.fn()} />
      </IntlProvider>,
    );

    fireEvent.click(await screen.findByText("Log corrective action"));
    fireEvent.click(screen.getByRole("combobox"));

    expect(await screen.findByText("Étalonnage")).toBeInTheDocument();
  });
});

/**
 * DELETE /rest/alerts/{id} is a hard row delete with no audit trail, so the
 * button must not reach it on one click.
 */
describe("AlertDetailModal delete confirmation", () => {
  const renderAsAdmin = () =>
    render(
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { roles: [Roles.GLOBAL_ADMIN] } }}
      >
        <IntlProvider locale="en" messages={messages}>
          <AlertDetailModal alertId={7} open onClose={vi.fn()} />
        </IntlProvider>
      </UserSessionDetailsContext.Provider>,
    );

  beforeEach(() => {
    deleteAlert.mockClear();
    deleteAlert.mockResolvedValue({});
  });

  it("asks before deleting instead of deleting on the first click", async () => {
    renderAsAdmin();

    fireEvent.click(await screen.findByText("Delete Alert"));

    expect(deleteAlert).not.toHaveBeenCalled();
    expect(
      screen.getByText("Delete this alert permanently?"),
    ).toBeInTheDocument();
  });

  it("deletes once the confirmation is accepted", async () => {
    renderAsAdmin();

    fireEvent.click(await screen.findByText("Delete Alert"));
    fireEvent.click(screen.getByText("Delete permanently"));

    expect(deleteAlert).toHaveBeenCalledWith(7);
  });

  it("keeps the alert when the confirmation is cancelled", async () => {
    renderAsAdmin();

    fireEvent.click(await screen.findByText("Delete Alert"));
    fireEvent.click(screen.getByText("Cancel"));

    expect(deleteAlert).not.toHaveBeenCalled();
    expect(screen.getByText("Delete Alert")).toBeInTheDocument();
  });
});
