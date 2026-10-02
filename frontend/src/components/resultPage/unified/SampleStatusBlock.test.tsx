import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  postToOpenElisServerJsonResponse: vi.fn(),
}));
// eslint-disable-next-line import/first
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";
const postMock = postToOpenElisServerJsonResponse as ReturnType<typeof vi.fn>;

import SampleStatusBlock from "./SampleStatusBlock";

/**
 * OGC-1026 (R7, D13) — sample status block: partial use decrements remaining
 * volume via record-usage; exhaustion (remaining 0) reveals the disposal
 * hand-off; disposed samples are read-only.
 */
const wrap = (node: React.ReactElement) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {node}
    </IntlProvider>,
  );

const confirmMarkUsedUp = () =>
  fireEvent.click(
    within(screen.getByTestId("mark-used-up-confirm")).getByRole("button", {
      name: /Mark used up/,
    }),
  );

describe("SampleStatusBlock (R7 / D13)", () => {
  beforeEach(() => postMock.mockReset());

  it("available sample offers Record amount used + Mark used up, no disposal", () => {
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 3.5, unitOfMeasure: "mL" }}
        editable
        onChanged={() => {}}
      />,
    );
    expect(screen.getByTestId("record-usage-open")).toBeInTheDocument();
    expect(screen.getByTestId("mark-used-up")).toBeInTheDocument();
    expect(screen.queryByTestId("start-disposal")).not.toBeInTheDocument();
  });

  it("recording an amount posts to record-usage with the amount", () => {
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 3.5, unitOfMeasure: "mL" }}
        editable
        onChanged={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("record-usage-open"));
    fireEvent.change(screen.getByLabelText("Amount used this test"), {
      target: { value: "1.0" },
    });
    fireEvent.click(screen.getByTestId("record-usage-apply"));
    expect(postMock).toHaveBeenCalledWith(
      "/rest/storage/sample-items/record-usage",
      JSON.stringify({ sampleItemId: "17", amountUsed: "1.0" }),
      expect.any(Function),
    );
  });

  it("an amount above the remaining volume is refused with the remaining amount", () => {
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 1, unitOfMeasure: "mL" }}
        editable
        onChanged={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("record-usage-open"));
    fireEvent.change(screen.getByLabelText("Amount used this test"), {
      target: { value: "5" },
    });
    expect(
      screen.getByText(
        "Requested quantity (5) exceeds remaining quantity (1 mL)",
      ),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("record-usage-apply"));
    expect(postMock).not.toHaveBeenCalled();
  });

  it("Mark used up posts markUsedUp and refreshes on success", () => {
    const onChanged = vi.fn();
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({ exhausted: true });
        }
      },
    );
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 3.5 }}
        editable
        onChanged={onChanged}
      />,
    );
    fireEvent.click(screen.getByTestId("mark-used-up"));
    expect(postMock).not.toHaveBeenCalled();
    confirmMarkUsedUp();
    expect(postMock).toHaveBeenCalledWith(
      "/rest/storage/sample-items/record-usage",
      JSON.stringify({ sampleItemId: "17", markUsedUp: true }),
      expect.any(Function),
    );
    expect(onChanged).toHaveBeenCalled();
  });

  it("cancelling the Mark used up confirmation sends nothing", () => {
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 3.5 }}
        editable
        onChanged={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("mark-used-up"));
    fireEvent.click(
      within(screen.getByTestId("mark-used-up-confirm")).getByRole("button", {
        name: "Cancel",
      }),
    );
    expect(postMock).not.toHaveBeenCalled();
  });

  it("exhausted sample offers Start disposal, which posts the disposal form", () => {
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 0, unitOfMeasure: "mL" }}
        editable
        onChanged={() => {}}
      />,
    );
    expect(screen.queryByTestId("mark-used-up")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("start-disposal"));
    fireEvent.change(screen.getByLabelText("Disposal reason"), {
      target: { value: "testing_complete" },
    });
    fireEvent.change(screen.getByLabelText("Disposal method"), {
      target: { value: "autoclave" },
    });
    fireEvent.click(screen.getByTestId("confirm-disposal"));
    expect(postMock).toHaveBeenCalledWith(
      "/rest/storage/sample-items/dispose",
      JSON.stringify({
        sampleItemId: "17",
        reason: "testing_complete",
        method: "autoclave",
        notes: "",
      }),
      expect.any(Function),
    );
  });

  it("Confirm disposal with no method marks the method as required and sends nothing", () => {
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 0, unitOfMeasure: "mL" }}
        editable
        onChanged={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("start-disposal"));
    fireEvent.change(screen.getByLabelText("Disposal reason"), {
      target: { value: "testing_complete" },
    });
    fireEvent.click(screen.getByTestId("confirm-disposal"));
    expect(postMock).not.toHaveBeenCalled();
    expect(screen.getByLabelText("Disposal method")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(screen.getByLabelText("Disposal reason")).not.toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(screen.getByText("This field is required")).toBeInTheDocument();
  });

  it("disposed sample shows the status with no actions", () => {
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 0, disposed: true }}
        editable
        onChanged={() => {}}
      />,
    );
    expect(screen.getByText("disposed")).toBeInTheDocument();
    expect(screen.queryByTestId("record-usage-open")).not.toBeInTheDocument();
    expect(screen.queryByTestId("mark-used-up")).not.toBeInTheDocument();
    expect(screen.queryByTestId("start-disposal")).not.toBeInTheDocument();
  });

  it("read-only rows (validated elsewhere) render no mutation buttons", () => {
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 3.5 }}
        editable={false}
        onChanged={() => {}}
      />,
    );
    expect(screen.queryByTestId("record-usage-open")).not.toBeInTheDocument();
    expect(screen.queryByTestId("mark-used-up")).not.toBeInTheDocument();
  });

  it("a 4xx response surfaces the server message instead of refreshing", () => {
    const onChanged = vi.fn();
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({ status: 400, message: "SampleItem is already disposed" });
        }
      },
    );
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 3.5 }}
        editable
        onChanged={onChanged}
      />,
    );
    fireEvent.click(screen.getByTestId("mark-used-up"));
    confirmMarkUsedUp();
    expect(
      screen.getByText("SampleItem is already disposed"),
    ).toBeInTheDocument();
    expect(onChanged).not.toHaveBeenCalled();
  });

  /**
   * OGC-1408 — a request that got no answer reaches the callback as status 0,
   * which a bare `status >= 400` test read as success: the block refreshed as
   * if the sample had been marked used up.
   */
  it("a request that got no answer reports the failure and never refreshes", () => {
    const onChanged = vi.fn();
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({
            error: "Failed to fetch",
            message: "Failed to fetch",
            status: 0,
          });
        }
      },
    );
    wrap(
      <SampleStatusBlock
        sampleItemId="17"
        snapshot={{ quantity: 5, remainingQuantity: 3.5 }}
        editable
        onChanged={onChanged}
      />,
    );
    fireEvent.click(screen.getByTestId("mark-used-up"));
    confirmMarkUsedUp();
    expect(
      screen.getByText(messages["label.results.sampleStatus.updateFailed"]),
    ).toBeInTheDocument();
    expect(screen.queryByText("Failed to fetch")).not.toBeInTheDocument();
    expect(onChanged).not.toHaveBeenCalled();
  });
});
