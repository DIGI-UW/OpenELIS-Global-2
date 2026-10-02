import React, { useContext } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../../languages/en.json";

/**
 * OGC-1420 (4): the page's messages are pinned in view wherever the page is
 * scrolled, a warning is not replaced by the success after it, an error stays
 * until dismissed, and a request with no answer says so in the user's words.
 */
vi.mock("../locationsApi", () => ({
  getLists: vi.fn(() => Promise.resolve({})),
}));
vi.mock("../PageIntro", () => ({ default: () => null }));
vi.mock("../../../common/PageBreadCrumb", () => ({ default: () => null }));
vi.mock("../AreasView", () => ({ default: () => null }));
vi.mock("../ImportExportView", () => ({ default: () => null }));
const holder = vi.hoisted(() => ({ context: null }));
vi.mock("../OrganizationsView", () => ({
  default: () => {
    const { notify } = useContext(holder.context);
    return (
      <>
        <button onClick={() => notify("Duplicate name here", "warning")}>
          warn
        </button>
        <button onClick={() => notify("Clinic added.")}>ok</button>
        <button onClick={() => notify("", "error")}>fail</button>
        <button onClick={() => notify("A refused", "error", null, "record-1")}>
          failA
        </button>
        <button onClick={() => notify("B refused", "error", null, "record-2")}>
          failB
        </button>
        <button onClick={() => notify("A saved.", "success", null, "record-1")}>
          savedA
        </button>
      </>
    );
  },
}));

import LocationsPage, {
  LocationsContext,
  legacyOrganizationEditTarget,
} from "../LocationsPage";

holder.context = LocationsContext;

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/MasterListsPage/locations"]}>
        <Route path="/MasterListsPage/locations">
          <LocationsPage />
        </Route>
      </MemoryRouter>
    </IntlProvider>,
  );

describe("LocationsPage messages (OGC-1420 4)", () => {
  afterEach(() => vi.useRealTimers());

  it("keeps a warning beside the success that follows it, pinned in view", () => {
    renderPage();
    fireEvent.click(screen.getByText("warn"));
    fireEvent.click(screen.getByText("ok"));

    const toasts = document.querySelectorAll(
      ".locationsToasts .locationsToast",
    );
    expect(toasts).toHaveLength(2);
    expect(toasts[0]).toHaveTextContent("Clinic added.");
    expect(toasts[1]).toHaveTextContent("Duplicate name here");
  });

  it("closes a success by itself but keeps an error until it is dismissed", () => {
    vi.useFakeTimers();
    renderPage();
    fireEvent.click(screen.getByText("ok"));
    fireEvent.click(screen.getByText("fail"));
    act(() => {
      vi.advanceTimersByTime(7000);
    });

    const toasts = document.querySelectorAll(".locationsToast");
    expect(toasts).toHaveLength(1);
    expect(toasts[0]).toHaveTextContent(
      "The request could not be completed, so nothing was saved.",
    );
  });

  it("clears a form's earlier errors when the same form saves, and only that form's", () => {
    renderPage();
    fireEvent.click(screen.getByText("failA"));
    fireEvent.click(screen.getByText("failB"));
    fireEvent.click(screen.getByText("savedA"));

    const texts = [...document.querySelectorAll(".locationsToast")].map(
      (toast) => toast.textContent,
    );
    expect(texts.join("|")).toContain("A saved.");
    expect(texts.join("|")).toContain("B refused");
    expect(texts.join("|")).not.toContain("A refused");
  });

  it("opens the record of an old link whether it says ID= or id=", () => {
    expect(
      legacyOrganizationEditTarget("/MasterListsPage", { search: "?ID=5" }),
    ).toBe("/MasterListsPage/locations?id=5");
    expect(
      legacyOrganizationEditTarget("/MasterListsPage", { search: "?id=5" }),
    ).toBe("/MasterListsPage/locations?id=5");
  });
});
