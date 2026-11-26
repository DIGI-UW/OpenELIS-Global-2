import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    putToOpenElisServerJsonResponse: vi.fn(),
  },
}));

vi.mock("../../../utils/Utils", () => utilsMock);

import RunLotPicker from "./RunLotPicker";

const soon = new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString();

const OPTIONS = {
  reagents: [
    {
      id: 4,
      name: "Amylase reagent",
      lots: [
        { id: 40, lotNumber: "LOT-SOON", expirationDate: soon },
        { id: 41, lotNumber: "LOT-LATER", expirationDate: "2030-06-30" },
      ],
    },
  ],
  currentLotId: null,
  lastUsedLotId: 41,
};

const renderPicker = (options: unknown = OPTIONS) => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (_url: string, callback: (r: unknown) => void) => callback(options),
  );
  const onSaved = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <RunLotPicker runId={7} onSaved={onSaved} />
    </IntlProvider>,
  );
  return onSaved;
};

const lotSelect = () =>
  screen.getByLabelText("Reagent lot for this run") as HTMLSelectElement;

describe("OGC-1200 FR-E3 a batch run's reagent lot", () => {
  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.putToOpenElisServerJsonResponse.mockReset();
  });

  it("offers the usable lots with nothing pre-selected and flags one expiring soon", () => {
    renderPicker();

    expect(utilsMock.getFromOpenElisServer.mock.calls[0][0]).toBe(
      "/rest/result-runs/7/lot-options",
    );
    expect(lotSelect().value).toBe("");
    expect(screen.getByTestId("run-lot-save")).toBeDisabled();
    expect(
      screen.getByRole("option", { name: /LOT-SOON.*\(expires soon\)/ }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: /LOT-LATER/ }).textContent,
    ).not.toContain("expires soon");
  });

  it("picks the last-used lot on request and saves it for the run", () => {
    utilsMock.putToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: (r: unknown) => void) =>
        callback({ id: 7, reagentLot: "LOT-LATER" }),
    );
    const onSaved = renderPicker();

    fireEvent.click(screen.getByTestId("run-lot-last-used"));
    expect(lotSelect().value).toBe("4:41");
    fireEvent.click(screen.getByTestId("run-lot-save"));

    const [url, body] = utilsMock.putToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/result-runs/7/reagent-lot");
    expect(JSON.parse(body)).toEqual({ reagentId: 4, reagentLotId: 41 });
    expect(onSaved).toHaveBeenCalledWith({ id: 7, reagentLot: "LOT-LATER" });
  });

  it("says why a lot was refused", () => {
    utilsMock.putToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: (r: unknown) => void) =>
        callback({ status: 400, message: "This lot is expired, used up" }),
    );
    const onSaved = renderPicker();

    fireEvent.change(lotSelect(), { target: { value: "4:40" } });
    fireEvent.click(screen.getByTestId("run-lot-save"));

    expect(screen.getByTestId("run-lot-error")).toHaveTextContent(
      "This lot is expired, used up",
    );
    expect(onSaved).not.toHaveBeenCalled();
  });

  it("says so when no reagent is linked to the run's tests", () => {
    renderPicker({ reagents: [], currentLotId: null, lastUsedLotId: null });
    expect(screen.getByTestId("run-lot-none")).toBeInTheDocument();
    expect(screen.queryByTestId("run-lot-picker")).not.toBeInTheDocument();
  });
});
