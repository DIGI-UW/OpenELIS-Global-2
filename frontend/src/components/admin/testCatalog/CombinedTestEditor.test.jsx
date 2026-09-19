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
