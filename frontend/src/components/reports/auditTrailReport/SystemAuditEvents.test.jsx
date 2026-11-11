import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../utils/Utils", () => ({ getFromOpenElisServer }));
vi.mock("../../patient/SearchPatientForm", () => ({ default: () => null }));

import SystemAuditEvents from "./SystemAuditEvents";

const optionsOf = (box) =>
  within(document.getElementById(box.getAttribute("aria-controls")))
    .queryAllByRole("option")
    .map((option) => option.textContent);

const lastEventsQuery = () => {
  const urls = getFromOpenElisServer.mock.calls
    .map(([url]) => url)
    .filter((url) => url.startsWith("/rest/systemAuditEvents?"));
  return new URLSearchParams(urls[urls.length - 1].split("?")[1]);
};

/**
 * The system audit trail's Entity Type and User filters are searched by
 * typing (OGC-1515); an empty choice still means all.
 */
describe("System audit trail filters", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/systemAuditEvents/entityTypes") {
        callback([{ name: "PATIENT" }, { name: "SAMPLE" }, { name: "TEST" }]);
      } else if (url === "/rest/users") {
        callback([
          { id: "1", value: "admin" },
          { id: "7", value: "José Muñoz" },
          { id: "8", value: "Jane Doe" },
        ]);
      } else if (url.startsWith("/rest/systemAuditEvents?")) {
        callback({ events: [], totalCount: 0 });
      } else {
        callback([]);
      }
    });
  });

  const renderEvents = () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <SystemAuditEvents />
      </IntlProvider>,
    );

  it("searches the users by typing, ignoring accents, and queries the chosen one", async () => {
    renderEvents();
    const user = screen.getByRole("combobox", { name: "User" });

    expect(user).toHaveAttribute("placeholder", "All");
    await userEvent.type(user, "munoz");
    expect(optionsOf(user)).toEqual(["José Muñoz"]);
    await userEvent.click(screen.getByRole("option", { name: "José Muñoz" }));
    await userEvent.click(screen.getByRole("button", { name: /^Search/ }));

    expect(lastEventsQuery().get("userId")).toBe("7");
  });

  it("searches the entity types by typing; Patient asks for a patient and clearing means all types", async () => {
    renderEvents();
    const entity = screen.getByRole("combobox", { name: "Entity Type" });

    await userEvent.type(entity, "pat");
    expect(optionsOf(entity)).toEqual(["PATIENT"]);
    await userEvent.click(screen.getByRole("option", { name: "PATIENT" }));
    expect(screen.getByText(/Selected Patient/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: /clear/i }));
    await userEvent.click(screen.getByRole("button", { name: /^Search/ }));

    expect(screen.queryByText(/Selected Patient/)).not.toBeInTheDocument();
    expect(lastEventsQuery().has("entityType")).toBe(false);
  });
});
