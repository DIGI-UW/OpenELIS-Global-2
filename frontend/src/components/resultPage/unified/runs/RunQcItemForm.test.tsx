import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  },
}));

vi.mock("../../../utils/Utils", () => utilsMock);
vi.mock("../../../common/useSiteDateFormat", () => ({
  default: () => ({ pattern: "MM/dd/yyyy" }),
}));

import RunQcBlock, { RunQc } from "./RunQcBlock";

const QC: RunQc = {
  overall: "PASS_WITH_WARNING",
  tests: [
    {
      testId: "11",
      testName: "Glucose",
      verdict: "PASS_WITH_WARNING",
      controls: [],
      items: [
        {
          id: 1,
          kind: "DUPLICATE",
          parentAccession: "DEV0126000000000500",
          observed: "100",
          evaluation: "FAIL",
          detail: "RPD = 26.1% (threshold 10%)",
          operator: "Ana Lab",
        },
        {
          id: 2,
          kind: "BLANK",
          observed: "0.2",
          evaluation: "BELOW_LOQ",
        },
      ],
    },
  ],
};

const renderBlock = (onRecorded = vi.fn()) => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <RunQcBlock runId={7} qc={QC} onRecorded={onRecorded} active />
    </IntlProvider>,
  );
  return onRecorded;
};

describe("OGC-1200 lab blanks and duplicates on a run", () => {
  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
  });

  it("lists each blank and duplicate under its test with what it came to", () => {
    renderBlock();

    expect(screen.getByTestId("run-qc-overall")).toHaveTextContent(
      "Pass with warning",
    );
    const duplicate = screen.getByTestId("run-qc-item-1");
    expect(duplicate).toHaveTextContent("Fail");
    expect(duplicate).toHaveTextContent(
      "Duplicate of DEV0126000000000500 · 100 · RPD = 26.1% (threshold 10%) · Ana Lab",
    );
    expect(screen.getByTestId("run-qc-item-2")).toHaveTextContent(
      "Below LOQ Blank · 0.2",
    );
  });

  it("records a duplicate of one of the run's samples and shows the run's new QC", () => {
    utilsMock.getFromOpenElisServer.mockImplementation(
      (url: string, callback: (body: unknown) => void) =>
        url === "/rest/result-runs/7/qc-items/samples?testId=11"
          ? callback([{ id: "61", value: "DEV0126000000000500" }])
          : callback([]),
    );
    const answer = { ...QC, overall: "HELD" };
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: (body: unknown) => void) =>
        callback(answer),
    );
    const onRecorded = renderBlock();

    fireEvent.click(screen.getByTestId("run-qc-record-item"));
    fireEvent.click(screen.getByLabelText("Duplicate"));
    fireEvent.click(screen.getByTestId("run-qc-item-save"));
    expect(screen.getByText(messages["run.qc.item.incomplete"])).toBeVisible();
    expect(utilsMock.postToOpenElisServerJsonResponse).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText("Sample it repeats"), {
      target: { value: "61" },
    });
    fireEvent.change(screen.getByLabelText("Value read"), {
      target: { value: "130" },
    });
    fireEvent.click(screen.getByTestId("run-qc-item-save"));

    const [url, body] =
      utilsMock.postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/result-runs/7/qc-items");
    expect(JSON.parse(body)).toEqual({
      testId: "11",
      kind: "DUPLICATE",
      value: "130",
      outcome: null,
      parentAnalysisId: "61",
    });
    expect(onRecorded).toHaveBeenCalledWith(answer);
  });
});
