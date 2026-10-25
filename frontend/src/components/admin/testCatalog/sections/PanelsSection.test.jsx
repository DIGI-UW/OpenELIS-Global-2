/**
 * PanelsSection — OGC-949 M9 / OGC-980..982.
 *
 * Covers: loading memberships, adding a panel via the typeahead + saving the
 * captured payload, editing this test's position, removing a membership, and
 * the error state.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../layout/Layout", async () => {
  const React = await import("react");
  return {
    NotificationContext: React.createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
    }),
  };
});

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import PanelsSection from "./PanelsSection";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const allPanels = [
  { id: "1", name: "Lipid Panel" },
  { id: "2", name: "Metabolic Panel" },
];

const wire = (memberships) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/test-catalog/panels") {
      cb(allPanels);
    } else if (url.includes("/tests/")) {
      cb({ testId: "42", memberships });
    }
  });
};

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <PanelsSection testId="42" testDomain="CLINICAL" />
    </IntlProvider>,
  );

const okPut = (membershipsOf = (payload) => payload.memberships) =>
  putToOpenElisServerFullResponse.mockImplementation((url, payload, cb) => {
    const parsed = JSON.parse(payload);
    cb({
      status: 200,
      json: () =>
        Promise.resolve({
          testId: "42",
          memberships: membershipsOf(parsed).map((m, i) => ({
            panelId: m.panelId,
            panelName: `Panel ${m.panelId}`,
            position: m.position == null ? i + 1 : m.position,
          })),
        }),
    });
  });

beforeEach(() => {
  vi.clearAllMocks();
  okPut();
});

describe("PanelsSection", () => {
  it("loads and renders existing memberships", async () => {
    wire([{ panelId: "1", panelName: "Lipid Panel", position: 3 }]);
    renderSection();
    expect(await screen.findByText("Lipid Panel")).toBeInTheDocument();
    const row = screen.getByTestId("panel-membership-1");
    expect(within(row).getByRole("spinbutton")).toHaveValue(3);
  });

  it("adds a panel via the typeahead and saves the captured payload", async () => {
    wire([]);
    renderSection();
    await screen.findByText(messages["label.testCatalog.panels.empty"]);
    const combo = screen.getByPlaceholderText(
      messages["label.testCatalog.panels.addToPanel"],
    );
    fireEvent.click(combo);
    fireEvent.click(screen.getByText("Metabolic Panel"));
    // Membership row appears.
    expect(await screen.findByTestId("panel-membership-2")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(
      JSON.parse(putToOpenElisServerFullResponse.mock.calls[0][1]).memberships,
    ).toEqual([{ panelId: "2", position: null }]);
  });

  it("edits this test's position and saves it", async () => {
    wire([{ panelId: "1", panelName: "Lipid Panel", position: 3 }]);
    renderSection();
    await screen.findByText("Lipid Panel");
    const row = screen.getByTestId("panel-membership-1");
    fireEvent.change(within(row).getByRole("spinbutton"), {
      target: { value: "5" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(
      JSON.parse(putToOpenElisServerFullResponse.mock.calls[0][1]).memberships,
    ).toEqual([{ panelId: "1", position: 5 }]);
  });

  it("removes a membership so it drops out of the payload", async () => {
    wire([{ panelId: "1", panelName: "Lipid Panel", position: 3 }]);
    renderSection();
    await screen.findByText("Lipid Panel");
    // FR-79 — the trash button opens a confirm modal; click its danger primary
    // button to confirm the removal.
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["label.testCatalog.panels.remove"],
      }),
    );
    const confirmButton = document.querySelector(
      ".cds--modal-footer .cds--btn--danger",
    );
    fireEvent.click(confirmButton);
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(
      JSON.parse(putToOpenElisServerFullResponse.mock.calls[0][1]).memberships,
    ).toEqual([]);
  });

  it("free-text create assigns AND persists in one action", async () => {
    wire([]);
    postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({
        status: 201,
        json: () => Promise.resolve({ id: "9", name: "Chem Panel" }),
      }),
    );
    renderSection();
    await screen.findByText(messages["label.testCatalog.panels.empty"]);
    fireEvent.change(document.querySelector("#new-panel-name"), {
      target: { value: "Chem Panel" },
    });
    fireEvent.click(screen.getByTestId("create-panel-button"));
    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/test-catalog/panels",
        // OGC-1140: the inline create inherits the originating test's domain;
        // it starts inactive until this test is its member (OGC-1456)
        JSON.stringify({
          name: "Chem Panel",
          domain: "CLINICAL",
          active: false,
        }),
        expect.any(Function),
      ),
    );
    // the assignment is persisted immediately — no separate Save required
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(
      JSON.parse(putToOpenElisServerFullResponse.mock.calls[0][1]).memberships,
    ).toEqual([{ panelId: "9", position: null }]);
    // and the rendered row comes from the SERVER response (refresh)
    expect(await screen.findByTestId("panel-membership-9")).toBeInTheDocument();
    // with its first member in place, the new panel is switched on
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/test-catalog/panels/9/basic-info",
        JSON.stringify({ active: true }),
        expect.any(Function),
      ),
    );
  });

  it("an inline create is busy until the new panel is switched on, and names a failed switch", async () => {
    wire([]);
    postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({
        status: 201,
        json: () => Promise.resolve({ id: "9", name: "Chem Panel" }),
      }),
    );
    let activate;
    putToOpenElisServerFullResponse.mockImplementation((url, payload, cb) => {
      if (url.endsWith("/basic-info")) {
        activate = cb;
        return;
      }
      cb({
        status: 200,
        json: () =>
          Promise.resolve({
            testId: "42",
            memberships: [
              { panelId: "9", panelName: "Chem Panel", position: 1 },
            ],
          }),
      });
    });
    const addNotification = vi.fn();
    const { NotificationContext } = await import("../../../layout/Layout");
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{ addNotification, setNotificationVisible: () => {} }}
        >
          <PanelsSection testId="42" testDomain="CLINICAL" />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    await screen.findByText(messages["label.testCatalog.panels.empty"]);
    fireEvent.change(document.querySelector("#new-panel-name"), {
      target: { value: "Chem Panel" },
    });
    fireEvent.click(screen.getByTestId("create-panel-button"));
    await waitFor(() => expect(activate).toBeDefined());
    fireEvent.change(document.querySelector("#new-panel-name"), {
      target: { value: "Second" },
    });
    expect(screen.getByTestId("create-panel-button")).toBeDisabled();

    activate({ status: 500 });

    await waitFor(() =>
      expect(screen.getByTestId("create-panel-button")).toBeEnabled(),
    );
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message:
          'Panel "Chem Panel" was created and this test added, but it could not be switched on. Activate it in the Panel Editor.',
      }),
    );
    // An apostrophe before {name} is ICU quoting and printed "{name}" literally.
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "success",
        message:
          'Panel "Chem Panel" created and this test added. Configure it further in the Panel Editor.',
      }),
    );
  });

  it("a new panel name over 20 characters is capped and its refusal named (OGC-1456)", async () => {
    wire([]);
    postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({
        status: 422,
        json: () => Promise.resolve({ refusal: "name.tooLong" }),
      }),
    );
    const addNotification = vi.fn();
    const { NotificationContext } = await import("../../../layout/Layout");
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{ addNotification, setNotificationVisible: () => {} }}
        >
          <PanelsSection testId="42" testDomain="CLINICAL" />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    await screen.findByText(messages["label.testCatalog.panels.empty"]);
    expect(document.querySelector("#new-panel-name")).toHaveAttribute(
      "maxLength",
      "20",
    );
    fireEvent.change(document.querySelector("#new-panel-name"), {
      target: { value: "Chem Panel" },
    });
    fireEvent.click(screen.getByTestId("create-panel-button"));

    await waitFor(() =>
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: messages["error.panel.nameTooLong"],
        }),
      ),
    );
  });

  it("free-text name matching an existing panel assigns it without POSTing", async () => {
    wire([]);
    renderSection();
    await screen.findByText(messages["label.testCatalog.panels.empty"]);
    fireEvent.change(document.querySelector("#new-panel-name"), {
      target: { value: "lipid panel" },
    });
    fireEvent.click(screen.getByTestId("create-panel-button"));
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
    expect(
      JSON.parse(putToOpenElisServerFullResponse.mock.calls[0][1]).memberships,
    ).toEqual([{ panelId: "1", position: null }]);
  });

  it("save renders the server's response, not the optimistic list", async () => {
    wire([{ panelId: "1", panelName: "Lipid Panel", position: 3 }]);
    // server normalizes the null position to 7
    putToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({
        status: 200,
        json: () =>
          Promise.resolve({
            testId: "42",
            memberships: [
              { panelId: "1", panelName: "Lipid Panel", position: 7 },
            ],
          }),
      }),
    );
    renderSection();
    await screen.findByText("Lipid Panel");
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => {
      const row = screen.getByTestId("panel-membership-1");
      expect(within(row).getByRole("spinbutton")).toHaveValue(7);
    });
  });

  it("a failed save keeps the staged edits and does not fake success", async () => {
    wire([]);
    putToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({ status: 422, json: () => Promise.resolve({}) }),
    );
    renderSection();
    await screen.findByText(messages["label.testCatalog.panels.empty"]);
    const combo = screen.getByPlaceholderText(
      messages["label.testCatalog.panels.addToPanel"],
    );
    fireEvent.click(combo);
    fireEvent.click(screen.getByText("Metabolic Panel"));
    expect(await screen.findByTestId("panel-membership-2")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    // staged row still present for correction — not cleared, not "saved"
    expect(screen.getByTestId("panel-membership-2")).toBeInTheDocument();
  });

  it("names the active panel a removal would leave empty (OGC-1456)", async () => {
    wire([{ panelId: "1", panelName: "Lipid Panel", position: 1 }]);
    putToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({
        status: 422,
        json: () =>
          Promise.resolve({
            refusal: "panel.activeNeedsTest",
            refusedPanels: [{ panelId: "1", panelName: "Lipid Panel" }],
          }),
      }),
    );
    const addNotification = vi.fn();
    const { NotificationContext } = await import("../../../layout/Layout");
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{ addNotification, setNotificationVisible: () => {} }}
        >
          <PanelsSection testId="42" testDomain="CLINICAL" />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    await screen.findByText("Lipid Panel");
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: expect.stringContaining("Lipid Panel"),
        }),
      ),
    );
    expect(addNotification.mock.calls[0][0].message).not.toBe(
      messages["server.error.msg"],
    );
  });

  it("shows the stored membership again after that refusal (OGC-1456)", async () => {
    wire([{ panelId: "1", panelName: "Lipid Panel", position: 1 }]);
    putToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({
        status: 422,
        json: () =>
          Promise.resolve({
            refusal: "panel.activeNeedsTest",
            refusedPanels: [{ panelId: "1", panelName: "Lipid Panel" }],
          }),
      }),
    );
    renderSection();
    await screen.findByText("Lipid Panel");
    fireEvent.click(
      within(screen.getByTestId("panel-membership-1")).getByRole("button", {
        name: messages["label.testCatalog.panels.remove"],
      }),
    );
    fireEvent.click(
      document.querySelector(".cds--modal-footer .cds--btn--danger"),
    );
    expect(screen.queryByTestId("panel-membership-1")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    expect(await screen.findByTestId("panel-membership-1")).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/test-catalog/panels") {
        cb(allPanels);
      } else {
        cb(undefined);
      }
    });
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.panels.loadError"]),
    ).toBeInTheDocument();
  });
});
