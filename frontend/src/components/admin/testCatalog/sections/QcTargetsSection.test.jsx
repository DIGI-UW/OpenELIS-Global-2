/**
 * QcTargetsSection — OGC-1148.
 *
 * The network seam (Utils) is mocked; behaviour is asserted through the
 * rendered rows and the captured PUT payload, the contract the backend upsert
 * consumes: a level default is added, a lot override links an existing lot,
 * and deactivation is a flag on the row, never a deletion.
 */
import { vi } from "vitest";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

vi.mock("../../../layout/Layout", async () => {
  const React = await import("react");
  return {
    NotificationContext: React.createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
    }),
  };
});

import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import QcTargetsSection from "./QcTargetsSection";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const LOT = {
  id: "lot-1",
  label: "Glucose control LOT-0001",
  lotNumber: "LOT-0001",
  productName: "Glucose control",
  controlLevel: "NORMAL",
  status: "ACTIVE",
};

const EMPTY = {
  testId: "7",
  quantitative: true,
  unit: "mmol/L",
  levels: ["LOW", "NORMAL", "HIGH"],
  targets: [],
  lots: [LOT],
  dictionaryOptions: [],
};

const WITH_NORMAL = {
  ...EMPTY,
  targets: [
    {
      id: "t-1",
      componentId: null,
      controlLevel: "NORMAL",
      qcControlLotId: null,
      expectedValue: 5.5,
      uncertainty: 0.4,
      active: true,
    },
  ],
};

const clone = (o) => JSON.parse(JSON.stringify(o));

const renderSection = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <QcTargetsSection testId="7" />
      </IntlProvider>
    </MemoryRouter>,
  );

const savedPayload = () =>
  JSON.parse(putToOpenElisServerFullResponse.mock.calls[0][1]);

const load = (payload) =>
  getFromOpenElisServer.mockImplementation((url, cb) => cb(clone(payload)));

beforeEach(() => {
  vi.clearAllMocks();
  load(EMPTY);
  putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
    cb({ status: 200 }),
  );
});

describe("QcTargetsSection", () => {
  it("renders one row per control level with the empty state and the unit", async () => {
    renderSection();
    expect(await screen.findByTestId("qc-target-row-LOW")).toBeVisible();
    expect(screen.getByTestId("qc-target-row-NORMAL")).toBeVisible();
    expect(screen.getByTestId("qc-target-row-HIGH")).toBeVisible();
    expect(screen.getAllByTestId(/qc-target-empty-/)).toHaveLength(3);
    expect(screen.getByTestId("qc-target-row-NORMAL")).toHaveTextContent(
      "mmol/L",
    );
    expect(screen.getByTestId("qc-targets-precedence")).toHaveTextContent(
      messages["admin.testCatalog.qcTargets.helper.precedence"],
    );
  });

  it("adds a level default and saves it as an active target with no lot", async () => {
    renderSection();
    fireEvent.click(await screen.findByTestId("qc-target-add-NORMAL"));

    fireEvent.change(screen.getByTestId("qc-target-NORMAL-expected"), {
      target: { value: "5.5" },
    });
    fireEvent.change(screen.getByTestId("qc-target-NORMAL-uncertainty"), {
      target: { value: "0.4" },
    });
    fireEvent.click(screen.getByTestId("qc-targets-save"));

    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalledTimes(1),
    );
    expect(putToOpenElisServerFullResponse.mock.calls[0][0]).toBe(
      "/rest/test-catalog/tests/7/qc-targets",
    );
    const payload = savedPayload();
    expect(payload.targets).toHaveLength(1);
    expect(payload.targets[0]).toMatchObject({
      id: null,
      controlLevel: "NORMAL",
      qcControlLotId: null,
      expectedValue: 5.5,
      uncertainty: 0.4,
      active: true,
    });
  });

  it("refuses to save a quantitative target without both numbers", async () => {
    renderSection();
    fireEvent.click(await screen.findByTestId("qc-target-add-LOW"));
    fireEvent.change(screen.getByTestId("qc-target-LOW-expected"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByTestId("qc-targets-save"));
    expect(putToOpenElisServerFullResponse).not.toHaveBeenCalled();
  });

  it("links an existing lot as an override and keeps the level default", async () => {
    load(WITH_NORMAL);
    renderSection();
    fireEvent.click(await screen.findByTestId("qc-target-edit-NORMAL"));
    fireEvent.click(screen.getByTestId("qc-override-start-NORMAL"));

    const lotBox = screen.getByRole("combobox", {
      name: messages["admin.testCatalog.qcTargets.lot.label"],
    });
    fireEvent.click(lotBox);
    fireEvent.click(await screen.findByText("Glucose control LOT-0001"));
    fireEvent.change(screen.getByTestId("qc-override-expected-NORMAL"), {
      target: { value: "5.9" },
    });
    fireEvent.change(screen.getByTestId("qc-override-uncertainty-NORMAL"), {
      target: { value: "0.3" },
    });
    fireEvent.click(screen.getByTestId("qc-override-add-NORMAL"));

    expect(screen.getByTestId("qc-target-overrides-NORMAL")).toHaveTextContent(
      "Glucose control LOT-0001",
    );

    fireEvent.click(screen.getByTestId("qc-targets-save"));
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalledTimes(1),
    );
    const payload = savedPayload();
    expect(payload.targets).toHaveLength(2);
    expect(payload.targets[0]).toMatchObject({
      id: "t-1",
      qcControlLotId: null,
    });
    expect(payload.targets[1]).toMatchObject({
      controlLevel: "NORMAL",
      qcControlLotId: "lot-1",
      expectedValue: 5.9,
      uncertainty: 0.3,
      active: true,
    });
  });

  it("deactivates a target as a flag and hides it until 'Show deactivated'", async () => {
    load(WITH_NORMAL);
    renderSection();
    fireEvent.click(await screen.findByTestId("qc-target-toggle-NORMAL"));

    // Hidden by default once deactivated; the empty state returns.
    expect(screen.getByTestId("qc-target-empty-NORMAL")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("switch"));
    expect(screen.getByTestId("qc-target-summary-NORMAL")).toHaveTextContent(
      "5.5",
    );
    expect(screen.getByTestId("qc-target-toggle-NORMAL")).toHaveTextContent(
      messages["admin.testCatalog.qcTargets.reactivate"],
    );

    fireEvent.click(screen.getByTestId("qc-targets-save"));
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalledTimes(1),
    );
    expect(savedPayload().targets[0]).toMatchObject({
      id: "t-1",
      active: false,
    });
  });

  it("offers the expected outcome for a qualitative test", async () => {
    load({
      ...EMPTY,
      quantitative: false,
      unit: "",
      dictionaryOptions: [
        { id: "500", name: "Positive" },
        { id: "501", name: "Negative" },
      ],
    });
    renderSection();
    fireEvent.click(await screen.findByTestId("qc-target-add-HIGH"));
    fireEvent.change(screen.getByTestId("qc-target-HIGH-outcome"), {
      target: { value: "500" },
    });
    fireEvent.click(screen.getByTestId("qc-targets-save"));
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalledTimes(1),
    );
    expect(savedPayload().targets[0]).toMatchObject({
      controlLevel: "HIGH",
      expectedDictResultId: "500",
      expectedValue: null,
      uncertainty: null,
    });
  });
});
