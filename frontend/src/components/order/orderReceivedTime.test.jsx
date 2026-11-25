import React from "react";
import { render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { describe, expect, it, vi } from "vitest";
import { OrderProvider, useOrderContext } from "./OrderContext";
import { getFromOpenElisServer } from "../utils/Utils";

vi.mock("react-router-dom", () => ({
  useLocation: () => ({ pathname: "/order/clinical/enter", search: "" }),
}));

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

describe("the order's receipt time", () => {
  it("is the laboratory's time from the server, not the browser clock", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/SamplePatientEntry") {
        cb({
          currentDate: "27/09/2026",
          sampleOrderItems: { receivedTime: "03:17" },
        });
      }
    });
    let latest;
    const Probe = () => {
      latest = useOrderContext().orderData;
      return null;
    };

    render(
      <OrderProvider workflowType="clinical">
        <Probe />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(latest.sampleOrderItems.receivedTime).toBe("03:17"),
    );
    expect(latest.sampleOrderItems.receivedDateForDisplay).toBe("27/09/2026");
  });
});
