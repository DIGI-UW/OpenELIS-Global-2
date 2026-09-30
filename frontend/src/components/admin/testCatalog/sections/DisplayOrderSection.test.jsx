/**
 * DisplayOrderSection — OGC-949 M12 / OGC-983..985.
 *
 * Covers: picking a sample type loads its tests in display order; the keyboard
 * arrow control (FR-009) reorders + auto-saves with the renumbered payload
 * captured; the empty + error states.
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
  putToOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import DisplayOrderSection from "./DisplayOrderSection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <DisplayOrderSection testId="42" />
    </IntlProvider>,
  );

const sampleTypes = [
  { id: "1", name: "Serum" },
  { id: "2", name: "Plasma" },
];

const orderFor1 = {
  sampleTypeId: "1",
  tests: [
    { testId: "7", testName: "Glucose", displayOrder: 1 },
    { testId: "9", testName: "BUN", displayOrder: 2 },
  ],
};

// Default wiring: sample-types list, then the test-order for the selected type.
const wireServer = (order = orderFor1, basicInfo = { testId: "42" }) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/test-catalog/sample-types") {
      cb(sampleTypes);
    } else if (url.includes("/test-order")) {
      cb(order);
    } else if (url.endsWith("/basic-info")) {
      cb(basicInfo);
    }
  });
};

beforeEach(() => {
  vi.clearAllMocks();
  putToOpenElisServer.mockImplementation((url, payload, cb) => cb(200));
});

describe("DisplayOrderSection", () => {
  it("loads the first sample type's tests in display order", async () => {
    wireServer();
    renderSection();
    expect(await screen.findByText("Glucose")).toBeInTheDocument();
    expect(screen.getByText("BUN")).toBeInTheDocument();
  });

  it("reorders via the down arrow and auto-saves the renumbered payload", async () => {
    wireServer();
    renderSection();
    await screen.findByText("Glucose");
    // Move Glucose (row 0) down → order becomes BUN(1), Glucose(2).
    fireEvent.click(screen.getByTestId("move-down-7"));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    const [url, payload] = putToOpenElisServer.mock.calls[0];
    expect(url).toBe("/rest/test-catalog/sample-types/1/test-order");
    const items = JSON.parse(payload).items;
    expect(items).toEqual([
      { testId: "9", displayOrder: 1 },
      { testId: "7", displayOrder: 2 },
    ]);
  });

  it("shows the empty state when a sample type has no tests", async () => {
    wireServer({ sampleTypeId: "1", tests: [] });
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.displayOrder.empty"]),
    ).toBeInTheDocument();
  });

  it("shows an error state when the order fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/test-catalog/sample-types") {
        cb(sampleTypes);
      } else {
        cb(undefined);
      }
    });
    renderSection();
    expect(
      await screen.findByText(
        messages["label.testCatalog.displayOrder.loadError"],
      ),
    ).toBeInTheDocument();
  });
});

describe("DisplayOrderSection opened from a test (OGC-1238)", () => {
  const orderFor2 = {
    sampleTypeId: "2",
    tests: [
      { testId: "5", testName: "Albumin", displayOrder: 1 },
      { testId: "42", testName: "Ferritin", displayOrder: 2 },
    ],
  };

  const wireBySampleType = (basicInfo) =>
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/test-catalog/sample-types") {
        cb(sampleTypes);
      } else if (url.endsWith("/sample-types/1/test-order")) {
        cb(orderFor1);
      } else if (url.endsWith("/sample-types/2/test-order")) {
        cb(orderFor2);
      } else if (url.endsWith("/tests/42/basic-info")) {
        cb(basicInfo);
      }
    });

  it("opens on the edited test's own sample type and marks its row", async () => {
    wireBySampleType({ testId: "42", sampleTypeId: "2", sampleTypeIds: ["2"] });
    renderSection();

    expect(await screen.findByText("Ferritin")).toBeInTheDocument();
    expect(
      screen.getByLabelText(
        messages["label.testCatalog.displayOrder.pickSampleType"],
      ).value,
    ).toBe("2");
    expect(screen.getByTestId("display-order-current-test")).toHaveTextContent(
      messages["label.testCatalog.displayOrder.currentTest"],
    );
    expect(screen.getByTestId("order-row-42")).toContainElement(
      screen.getByTestId("display-order-current-test"),
    );
  });

  it("uses a linked sample type when the test has no primary one", async () => {
    wireBySampleType({
      testId: "42",
      sampleTypeId: null,
      sampleTypeIds: ["2"],
    });
    renderSection();

    expect(await screen.findByText("Ferritin")).toBeInTheDocument();
  });

  it("falls back to the first sample type when the test's is not listed", async () => {
    wireBySampleType({ testId: "42", sampleTypeId: "99", sampleTypeIds: [] });
    renderSection();

    expect(await screen.findByText("Glucose")).toBeInTheDocument();
    expect(screen.queryByTestId("display-order-current-test")).toBeNull();
  });
});
