import React, { useEffect, useRef } from "react";
import { render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { OrderProvider, useOrderContext } from "./OrderContext";
import {
  postToOpenElisServerFullResponse,
  getFromOpenElisServer,
} from "../utils/Utils";

vi.mock("react-router-dom", () => ({
  useLocation: () => ({ pathname: "/order/clinical/collect", search: "" }),
}));

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

const SaveOnce = ({ samples: requested }) => {
  const context = useOrderContext();
  const started = useRef(false);
  useEffect(() => {
    context.setSamples(requested);
  }, []);
  useEffect(() => {
    if (started.current || !context.samples.some((s) => s.sampleTypeId)) {
      return;
    }
    started.current = true;
    context.saveOrder().catch(() => {});
  }, [context.samples]);
  return null;
};

const sampleTags = (sampleXML) =>
  [...sampleXML.matchAll(/<sample [^>]*\/>/g)].map((match) => match[0]);

describe("a sample's container type travels with the save (OGC-1611 P4)", () => {
  beforeEach(() => {
    postToOpenElisServerFullResponse.mockReset();
    getFromOpenElisServer.mockReset();
    postToOpenElisServerFullResponse.mockImplementation((url, payload, cb) =>
      cb(undefined),
    );
  });

  it("sends the chosen container, an emptied one, and nothing for a sample that never had the field", async () => {
    render(
      <OrderProvider workflowType="clinical">
        <SaveOnce
          samples={[
            { sampleTypeId: "3", containerTypeId: "12" },
            { sampleTypeId: "4", containerTypeId: "" },
            { sampleTypeId: "5" },
          ]}
        />
      </OrderProvider>,
    );

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const { sampleXML } = JSON.parse(
      postToOpenElisServerFullResponse.mock.calls[0][1],
    );
    const [chosen, emptied, absent] = sampleTags(sampleXML);
    expect(chosen).toContain("containerTypeId='12'");
    expect(emptied).toContain("containerTypeId=''");
    expect(absent).not.toContain("containerTypeId");
  });
});
