/**
 * PanelTestsSection — the domain-guarded picker (OGC-224) and what it tells
 * the operator when the server refuses a member (OGC-1232).
 *
 * - candidates are fetched for the panel's own domain only, and never before
 *   the panel is known;
 * - a candidates response that arrives after the panel's domain has changed
 *   is dropped, so an Environmental panel is never offered the Clinical list;
 * - a 422 that names the refused tests is shown by name, a bodyless one falls
 *   back to the generic message.
 */

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import PanelTestsSection from "./PanelTestsSection";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import messages from "../../../../languages/en.json";

const ENV_PANEL = { id: "14", name: "Water Panel", domain: "ENVIRONMENTAL" };

const notification = {
  addNotification: vi.fn(),
  setNotificationVisible: vi.fn(),
};

const wrap = (panel) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={notification}>
        <PanelTestsSection
          panel={panel}
          autoActivate={false}
          onSaved={vi.fn()}
        />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const candidateCalls = () =>
  getFromOpenElisServer.mock.calls.filter(([url]) =>
    url.startsWith("/rest/test-catalog/tests?"),
  );

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("/test-order")) {
      cb({ tests: [] });
    } else if (url.startsWith("/rest/sample-types")) {
      cb([]);
    }
    // candidate fetches are answered by each test, in the order it chooses
  });
});

describe("PanelTestsSection picker (domain guard)", () => {
  it("fetches candidates for the panel's own domain, and none before the panel is known", () => {
    const { rerender } = render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={notification}>
          <PanelTestsSection
            panel={null}
            autoActivate={false}
            onSaved={vi.fn()}
          />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    expect(candidateCalls()).toHaveLength(0);

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={notification}>
          <PanelTestsSection
            panel={ENV_PANEL}
            autoActivate={false}
            onSaved={vi.fn()}
          />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    expect(candidateCalls()).toHaveLength(1);
    expect(candidateCalls()[0][0]).toContain("domain=ENVIRONMENTAL");
    expect(candidateCalls()[0][0]).not.toContain("domain=CLINICAL");
  });

  it("drops a candidates response that arrives after the panel's domain changed", async () => {
    const { rerender } = wrap({ ...ENV_PANEL, domain: "CLINICAL" });
    const clinicalCallback = candidateCalls()[0][1];

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={notification}>
          <PanelTestsSection
            panel={ENV_PANEL}
            autoActivate={false}
            onSaved={vi.fn()}
          />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    const environmentalCallback = candidateCalls()[1][1];

    // the empty Environmental answer lands first, the big Clinical one last
    environmentalCallback({ rows: [] });
    clinicalCallback({
      rows: [{ testId: "6", name: "Albumin(Urines)", code: "Albumin-Urines" }],
    });

    fireEvent.click(screen.getByRole("button", { name: /open/i }));
    await waitFor(() =>
      expect(screen.getByRole("listbox")).toBeInTheDocument(),
    );
    expect(screen.queryByText(/Albumin\(Urines\)/)).toBeNull();
  });
});

describe("PanelTestsSection save refusals (OGC-1232)", () => {
  const refuse = (body) =>
    putToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({
        ok: false,
        status: 422,
        json: () =>
          body === undefined
            ? Promise.reject(new Error())
            : Promise.resolve(body),
      }),
    );

  it("names the tests the domain guard refused", async () => {
    wrap(ENV_PANEL);
    refuse({
      domainConflict: {
        domain: "ENVIRONMENTAL",
        tests: [{ testId: "6", name: "Albumin(Urines)", domain: "CLINICAL" }],
      },
    });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    await waitFor(() =>
      expect(notification.addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message:
            "Not saved: Albumin(Urines) (Clinical) is not in this panel's domain (Environmental). Only Environmental-domain tests can be added.",
        }),
      ),
    );
  });

  it("keeps the generic message for a bodyless refusal", async () => {
    wrap(ENV_PANEL);
    refuse(undefined);
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    await waitFor(() =>
      expect(notification.addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: messages["error.panel.save"],
        }),
      ),
    );
  });
});

describe("PanelTestsSection when the member list fails to load (OGC-1456)", () => {
  const failMembers = () =>
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/test-order")) {
        cb(undefined);
      } else if (url.startsWith("/rest/sample-types")) {
        cb([]);
      }
    });

  it("says so and keeps Save disabled, so nothing replaces the members", () => {
    failMembers();
    wrap(ENV_PANEL);

    expect(screen.getByTestId("panel-tests-load-error")).toBeVisible();
    expect(screen.getByRole("button", { name: /^save$/i })).toBeDisabled();
    expect(screen.queryByText(messages["empty.panel.tests"])).toBeNull();
    expect(screen.queryByTestId("panel-tests-count")).toBeNull();
  });

  it("loads the members on Retry and enables Save", async () => {
    failMembers();
    wrap(ENV_PANEL);

    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/test-order")) {
        cb({
          tests: [{ testId: "6", testName: "pH", code: "PH", position: 1 }],
        });
      } else if (url.startsWith("/rest/sample-types")) {
        cb([]);
      }
    });
    fireEvent.click(
      screen.getByRole("button", { name: messages["common.retry"] }),
    );

    await waitFor(() =>
      expect(screen.queryByTestId("panel-tests-load-error")).toBeNull(),
    );
    expect(screen.getByText("pH")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^save$/i })).toBeEnabled();
  });
});

describe("PanelTestsSection membership refusals (OGC-1456)", () => {
  const refuse = (body) =>
    putToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({ ok: false, status: 422, json: () => Promise.resolve(body) }),
    );

  it("explains that an active panel keeps at least one test", async () => {
    wrap(ENV_PANEL);
    refuse({ refusal: "tests.activeNeedsTest" });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    await waitFor(() =>
      expect(notification.addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: messages["error.panel.tests.activeNeedsTest"],
        }),
      ),
    );
  });

  it("shows the stored members again after that refusal", async () => {
    wrap(ENV_PANEL);
    const memberLoads = () =>
      getFromOpenElisServer.mock.calls.filter(([url]) =>
        url.includes("/test-order"),
      ).length;
    const before = memberLoads();
    refuse({ refusal: "tests.activeNeedsTest" });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    await waitFor(() => expect(memberLoads()).toBe(before + 1));
  });

  it("names the inactive tests it refused", async () => {
    wrap(ENV_PANEL);
    refuse({
      refusal: "tests.inactive",
      refusedTests: [{ testId: "6", name: "pH(Water)" }],
    });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    await waitFor(() =>
      expect(notification.addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: expect.stringContaining("pH(Water)"),
        }),
      ),
    );
  });
});

describe("PanelTestsSection two editors (OGC-1463)", () => {
  it("sends the loaded version and shows a stale save with Refresh", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/test-order")) {
        cb({ tests: [], version: "v1" });
      } else if (url.startsWith("/rest/sample-types")) {
        cb([]);
      }
    });
    putToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb({
        ok: false,
        status: 409,
        json: () => Promise.resolve({ conflict: "stale", version: "v2" }),
      }),
    );
    wrap(ENV_PANEL);
    const memberLoads = () =>
      getFromOpenElisServer.mock.calls.filter(([url]) =>
        url.includes("/test-order"),
      ).length;

    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));

    expect(
      await screen.findByTestId("panel-tests-stale-save"),
    ).toBeInTheDocument();
    expect(
      JSON.parse(putToOpenElisServerFullResponse.mock.calls[0][1]).version,
    ).toBe("v1");
    expect(screen.getByRole("button", { name: /^save$/i })).toBeDisabled();
    const before = memberLoads();
    fireEvent.click(
      screen.getByRole("button", { name: messages["label.results.refresh"] }),
    );
    await waitFor(() => expect(memberLoads()).toBe(before + 1));
    expect(notification.addNotification).not.toHaveBeenCalled();
  });
});
