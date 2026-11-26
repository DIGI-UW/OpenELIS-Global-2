import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    postToOpenElisServerJsonResponse: vi.fn(),
  },
}));

vi.mock("../../../utils/Utils", () => utilsMock);

import BatchTheseBar from "./BatchTheseBar";

const pick = (testId: string, label: string) => ({ testId, label });

const renderBar = (
  picks: Record<string, { testId: string; label: string }>,
) => {
  const onClear = vi.fn();
  const onOpenRun = vi.fn();
  const onRemove = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <BatchTheseBar
        picks={picks}
        onRemove={onRemove}
        onClear={onClear}
        onOpenRun={onOpenRun}
      />
    </IntlProvider>,
  );
  return { onClear, onOpenRun, onRemove };
};

describe("OGC-1200 FR-B4 Batch these", () => {
  beforeEach(() => {
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
  });

  it("shows nothing until a row is picked", () => {
    renderBar({});
    expect(screen.queryByTestId("batch-these-bar")).not.toBeInTheDocument();
  });

  it("asks for a second row of the test before batching", () => {
    renderBar({ "61": pick("5", "LAB-1 Amylase") });
    expect(screen.getByTestId("batch-these-count")).toHaveTextContent(
      "Rows selected for a batch: 1",
    );
    expect(screen.getByTestId("batch-these")).toBeDisabled();
    expect(screen.getByTestId("batch-these-hint")).toHaveTextContent(
      "Select two or more pending rows of one test to batch them.",
    );
  });

  it("does not batch rows of different tests together", () => {
    renderBar({
      "61": pick("5", "LAB-1 Amylase"),
      "62": pick("9", "LAB-2 GPT/ALAT"),
    });
    expect(screen.getByTestId("batch-these")).toBeDisabled();
    expect(screen.getByTestId("batch-these-hint")).toHaveTextContent(
      "Rows of different tests cannot be batched together.",
    );
  });

  it("lists each pick and removes one on its own", () => {
    const { onRemove } = renderBar({
      "61": pick("5", "LAB-1 Amylase"),
      "62": pick("9", "LAB-2 GPT/ALAT"),
    });

    expect(screen.getByTestId("batch-these-picks")).toHaveTextContent(
      "LAB-1 Amylase",
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "Remove LAB-2 GPT/ALAT from the batch",
      }),
    );
    expect(onRemove).toHaveBeenCalledWith("62");
  });

  it("batches rows of one test into a run and opens it", () => {
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: (r: unknown) => void) =>
        callback({ id: 30, status: "ACTIVE", runId: 205 }),
    );
    const { onClear, onOpenRun } = renderBar({
      "61": pick("5", "LAB-1 Amylase"),
      "62": pick("5", "LAB-2 Amylase"),
    });

    fireEvent.click(screen.getByTestId("batch-these"));

    const [url, body] =
      utilsMock.postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/batch-workplans/batches/from-worklist");
    expect(JSON.parse(body)).toEqual({ analysisIds: ["61", "62"] });
    expect(onClear).toHaveBeenCalled();
    expect(onOpenRun).toHaveBeenCalledWith(205);
  });

  it("drops a refusal once the picks it was about change", () => {
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: (r: unknown) => void) =>
        callback({ status: 400, message: "Already in an open batch: LAB-2" }),
    );
    const props = {
      onRemove: vi.fn(),
      onClear: vi.fn(),
      onOpenRun: vi.fn(),
    };
    const { rerender } = render(
      <IntlProvider locale="en" messages={messages}>
        <BatchTheseBar
          picks={{
            "61": pick("5", "LAB-1 Amylase"),
            "62": pick("5", "LAB-2 Amylase"),
          }}
          {...props}
        />
      </IntlProvider>,
    );
    fireEvent.click(screen.getByTestId("batch-these"));
    expect(screen.getByTestId("batch-these-error")).toBeInTheDocument();

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <BatchTheseBar
          picks={{
            "61": pick("5", "LAB-1 Amylase"),
            "63": pick("5", "LAB-3 Amylase"),
          }}
          {...props}
        />
      </IntlProvider>,
    );
    expect(screen.queryByTestId("batch-these-error")).not.toBeInTheDocument();
  });

  it("says why the rows could not be batched and opens nothing", () => {
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: (r: unknown) => void) =>
        callback({
          status: 400,
          message: "Already in an open batch: DEV0126000000000400 Amylase",
        }),
    );
    const { onOpenRun } = renderBar({
      "61": pick("5", "LAB-1 Amylase"),
      "62": pick("5", "LAB-2 Amylase"),
    });

    fireEvent.click(screen.getByTestId("batch-these"));

    expect(screen.getByTestId("batch-these-error")).toHaveTextContent(
      "Already in an open batch: DEV0126000000000400 Amylase",
    );
    expect(onOpenRun).not.toHaveBeenCalled();
  });
});
