import React, { useEffect, useRef } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { OrderProvider, useOrderContext } from "./OrderContext";
import CancelTestReasonDialog from "./CancelTestReasonDialog";
import messages from "../../languages/en.json";
import {
  postToOpenElisServerFullResponse,
  getFromOpenElisServer,
} from "../utils/Utils";

vi.mock("react-router-dom", () => ({
  useLocation: () => ({ pathname: "/order/environmental/enter", search: "" }),
}));

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

const refusedForReason = {
  ok: false,
  status: 400,
  json: () =>
    Promise.resolve({
      fieldErrors: [
        {
          field: "cancelReason",
          defaultMessage: "A reason is required to cancel this test.",
        },
      ],
    }),
};

const saved = {
  ok: true,
  status: 200,
  clone: () => saved,
  json: () => Promise.resolve({}),
};

/** Saves the entry step once and shows what the save ended with. */
const SaveOnce = () => {
  const context = useOrderContext();
  const started = useRef(false);
  useEffect(() => {
    context.setSamples([{ sampleTypeId: "3" }]);
  }, []);
  useEffect(() => {
    if (started.current || !context.samples.some((s) => s.sampleTypeId)) {
      return;
    }
    started.current = true;
    context.saveOrderEntry().catch(() => {});
  }, [context.samples]);
  return (
    <div data-testid="outcome">
      {context.saveStatus}|{context.error || ""}|
      {Object.keys(context.fieldErrors || {}).join(",")}
    </div>
  );
};

const renderSave = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <OrderProvider workflowType="environmental">
        <CancelTestReasonDialog />
        <SaveOnce />
      </OrderProvider>
    </IntlProvider>,
  );

describe("removing a test that has results (OGC-1613)", () => {
  beforeEach(() => {
    postToOpenElisServerFullResponse.mockReset();
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/dictionary/categories/")) {
        callback([
          { code: "1", label: "Sample damaged" },
          { code: "2", label: "Other" },
        ]);
      }
    });
  });

  it("asks for the reason and sends the save again with it", async () => {
    postToOpenElisServerFullResponse
      .mockImplementationOnce((url, payload, callback) =>
        callback(refusedForReason),
      )
      .mockImplementationOnce((url, payload, callback) => callback(saved));
    renderSave();

    const reason = await screen.findByLabelText(/Reason/);
    expect(screen.getByText("Remove a test that has results")).toBeTruthy();
    fireEvent.change(reason, { target: { value: "Sample damaged" } });
    fireEvent.click(screen.getByRole("button", { name: /Remove test/ }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(2),
    );
    const [first, retry] = postToOpenElisServerFullResponse.mock.calls.map(
      (call) => JSON.parse(call[1]),
    );
    expect(first.cancelReason).toBeFalsy();
    expect(retry.cancelReason).toBe("Sample damaged");
  });

  it("keeping the test sends nothing more and says why without a field name", async () => {
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => callback(refusedForReason),
    );
    renderSave();

    fireEvent.click(
      await screen.findByRole("button", { name: "Keep the test" }),
    );

    await waitFor(() =>
      expect(screen.getByTestId("outcome").textContent).toContain(
        "order.cancel.reasonRequired",
      ),
    );
    expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("outcome").textContent).not.toContain(
      "cancelReason",
    );
  });
});
