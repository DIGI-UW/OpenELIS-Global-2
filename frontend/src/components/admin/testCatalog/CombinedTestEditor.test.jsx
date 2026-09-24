/**
 * CombinedTestEditor — OGC-1118 (ranges-only bulk apply to related tests).
 *
 * The shared range set is seeded from the first test; saving writes it to every
 * selected test. Every bound the range dialog offers (normal, critical, valid)
 * must reach the server, and a set that differs across the tests must say so
 * before anything is overwritten.
 */
import { vi } from "vitest";

vi.mock("../../layout/Layout", async () => {
  const React = await import("react");
  return {
    NotificationContext: React.createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
      notificationVisible: false,
    }),
  };
});

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  putToOpenElisServerJsonResponse: vi.fn(),
}));

import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import CombinedTestEditor from "./CombinedTestEditor";
import { getFromOpenElisServer, putToOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

const seedRange = {
  id: "49",
  componentId: "comp-31",
  gender: null,
  minAge: 0,
  maxAge: null,
  lowNormal: 1,
  highNormal: 5,
  lowCritical: 0.5,
  highCritical: 8,
  lowValid: 0,
  highValid: 10,
};

const renderGroup = (ids = "31,32") =>
  render(
    <MemoryRouter
      initialEntries={[
        `/MasterListsPage/TestCatalogEditor/group/${ids}/ranges`,
      ]}
    >
      <IntlProvider locale="en" messages={messages}>
        <Route path="/MasterListsPage/TestCatalogEditor/group/:ids/:section?">
          <CombinedTestEditor />
        </Route>
      </IntlProvider>
    </MemoryRouter>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("/group/summary")) {
      cb([
        {
          testId: "31",
          name: "HIV rapid test HIV(Serum)",
          code: "HIV-S",
          sampleType: "Serum",
          loinc: null,
          active: true,
        },
        {
          testId: "32",
          name: "HIV rapid test HIV(Plasma)",
          code: "HIV-P",
          sampleType: "Plasma",
          loinc: null,
          active: true,
        },
      ]);
    } else if (url.endsWith("/tests/31/ranges")) {
      cb({ testId: "31", ranges: [seedRange], sampleTypes: [] });
    } else if (url.endsWith("/tests/32/ranges")) {
      // The sibling holds nothing yet, so the set differs across the tests.
      cb({ testId: "32", ranges: [], sampleTypes: [] });
    } else if (url.endsWith("/sample-results")) {
      cb({
        testId: "31",
        components: [{ id: "comp-31", code: "PRIMARY", label: "Result" }],
      });
    } else {
      cb(null);
    }
  });
  putToOpenElisServer.mockImplementation((url, payload, cb) => cb(200));
});

describe("CombinedTestEditor ranges", () => {
  it("warns that the ranges differ and shows every bound of the seed set", async () => {
    renderGroup();

    expect(await screen.findByTestId("ranges-differ-warning")).toBeVisible();
    expect(screen.getByTestId("group-range-critical-0")).toHaveTextContent(
      "0.5 / 8",
    );
    expect(screen.getByTestId("group-range-valid-0")).toHaveTextContent(
      "0 / 10",
    );
  });

  it("sends the valid bounds along with normal and critical when applying to all", async () => {
    renderGroup();
    await screen.findByTestId("ranges-differ-warning");

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["button.testCatalog.setAllTo"],
      }),
    );

    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalledTimes(1));
    const [url, payload] = putToOpenElisServer.mock.calls[0];
    expect(url).toBe("/rest/test-catalog/group/ranges");
    const body = JSON.parse(payload);
    expect(body.testIds).toEqual(["31", "32"]);
    expect(body.ranges).toHaveLength(1);
    expect(body.ranges[0]).toMatchObject({
      componentId: "comp-31",
      lowNormal: 1,
      highNormal: 5,
      lowCritical: 0.5,
      highCritical: 8,
      lowValid: 0,
      highValid: 10,
    });
    expect(body.ranges[0].id).toBeUndefined();
  });
});

describe("CombinedTestEditor range specimen scope (OGC-1238)", () => {
  const serum = { id: "st-serum", name: "Serum" };
  const plasma = { id: "st-plasma", name: "Plasma" };

  beforeEach(() => {
    const base = getFromOpenElisServer.getMockImplementation();
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.endsWith("/tests/31/ranges")) {
        cb({
          testId: "31",
          ranges: [{ ...seedRange, sampleTypeId: "st-serum" }],
          sampleTypes: [serum],
        });
      } else if (url.endsWith("/tests/32/ranges")) {
        // Identical bounds, but shared instead of scoped to Serum.
        cb({
          testId: "32",
          ranges: [{ ...seedRange, id: "50" }],
          sampleTypes: [plasma],
        });
      } else {
        base(url, cb);
      }
    });
  });

  it("warns when the ranges differ only in specimen scope, and shows the scope", async () => {
    renderGroup();

    expect(await screen.findByTestId("ranges-differ-warning")).toBeVisible();
    expect(screen.getByTestId("group-range-sample-type-0")).toHaveTextContent(
      "Serum",
    );
  });

  it("keeps each range's specimen scope in the group save", async () => {
    renderGroup();
    await screen.findByTestId("ranges-differ-warning");

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["button.testCatalog.setAllTo"],
      }),
    );

    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalledTimes(1));
    const body = JSON.parse(putToOpenElisServer.mock.calls[0][1]);
    expect(body.ranges[0].sampleTypeId).toBe("st-serum");
  });

  it("offers every selected test's sample type in the range dialog", async () => {
    renderGroup();
    await screen.findByTestId("ranges-differ-warning");

    fireEvent.click(
      screen.getAllByRole("button", {
        name: messages["label.button.edit"],
      })[0],
    );

    const picker = await screen.findByLabelText(
      messages["label.testCatalog.override.col.sampleType"],
    );
    expect(picker.value).toBe("st-serum");
    const options = Array.from(picker.querySelectorAll("option")).map(
      (o) => o.value,
    );
    expect(options).toEqual(["", "st-serum", "st-plasma"]);
  });
});

describe("CombinedTestEditor compares ranges across tests' own components (OGC-1238)", () => {
  const sibling = (componentId, overrides = {}) => ({
    ...seedRange,
    componentId,
    componentCode: "PRIMARY",
    ...overrides,
  });

  const wire = (range31, range32) => {
    const base = getFromOpenElisServer.getMockImplementation();
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.endsWith("/tests/31/ranges")) {
        cb({ testId: "31", ranges: [range31], sampleTypes: [] });
      } else if (url.endsWith("/tests/32/ranges")) {
        cb({ testId: "32", ranges: [range32], sampleTypes: [] });
      } else {
        base(url, cb);
      }
    });
  };

  it("shows no warning when siblings hold the same range on their own components", async () => {
    wire(sibling("comp-31"), sibling("comp-32", { id: "50" }));
    renderGroup();
    await screen.findByTestId("group-range-critical-0");

    expect(screen.queryByTestId("ranges-differ-warning")).toBeNull();
  });

  it("still warns when the siblings' bounds differ", async () => {
    wire(sibling("comp-31"), sibling("comp-32", { id: "50", highNormal: 6 }));
    renderGroup();

    expect(await screen.findByTestId("ranges-differ-warning")).toBeVisible();
  });
});
