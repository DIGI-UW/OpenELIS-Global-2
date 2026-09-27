import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFormData: vi.fn(),
}));
// eslint-disable-next-line import/first
import {
  getFromOpenElisServer,
  postToOpenElisServerFormData,
} from "../../utils/Utils";
const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;
const postFormMock = postToOpenElisServerFormData as ReturnType<typeof vi.fn>;

import CompactFileInput from "./FileInput";

/**
 * OGC-811 — the old Results page's per-row attachment control now rides the
 * order-attachment API (same store as Add Order and the unified page),
 * scoped to the row's analysis + result component.
 */
const wrap = (node: React.ReactElement) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {node}
    </IntlProvider>,
  );

const ROW = {
  accessionNumber: "DEV1",
  analysisId: "28",
  testResultComponentId: "comp-A",
};

describe("CompactFileInput (order-attachment backed)", () => {
  beforeEach(() => {
    getMock.mockReset();
    postFormMock.mockReset();
  });

  it("lists only this row's attachments (component isolation)", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof url !== "string" || typeof cb !== "function") {
        return;
      }
      if (url.includes("/attachments")) {
        cb([
          { id: 1, fileName: "order.pdf", analysisId: "" },
          {
            id: 2,
            fileName: "mine.png",
            analysisId: "28",
            testResultComponentId: "comp-A",
          },
          {
            id: 3,
            fileName: "sibling.png",
            analysisId: "28",
            testResultComponentId: "comp-B",
          },
        ]);
      }
    });
    wrap(<CompactFileInput data={ROW} />);
    expect(getMock).toHaveBeenCalledWith(
      "/rest/order/DEV1/attachments",
      expect.any(Function),
    );
    expect(screen.getByText("order.pdf")).toBeInTheDocument();
    expect(screen.getByText("mine.png")).toBeInTheDocument();
    expect(screen.queryByText("sibling.png")).not.toBeInTheDocument();
  });

  it("uploads through the shared API with the row's scope, then refreshes", () => {
    let listCalls = 0;
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof url !== "string" || typeof cb !== "function") {
        return;
      }
      if (url.includes("/attachments")) {
        listCalls += 1;
        cb(
          listCalls > 1
            ? [
                {
                  id: 9,
                  fileName: "new.png",
                  analysisId: "28",
                  testResultComponentId: "comp-A",
                },
              ]
            : [],
        );
      }
    });
    postFormMock.mockImplementation(
      (_url: string, _form: FormData, cb: (status: number) => void) => {
        if (typeof cb === "function") {
          cb(200);
        }
      },
    );
    wrap(<CompactFileInput data={ROW} />);
    const input = document.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;
    const file = new File(["x"], "new.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [file] } });
    expect(postFormMock).toHaveBeenCalledWith(
      "/rest/order/DEV1/attachments?analysisId=28&testResultComponentId=comp-A",
      expect.any(FormData),
      expect.any(Function),
    );
    expect(screen.getByText("new.png")).toBeInTheDocument();
  });

  it("failed uploads surface an error instead of silently dropping", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof cb === "function") {
        cb([]);
      }
    });
    postFormMock.mockImplementation(
      (_url: string, _form: FormData, cb: (status: number) => void) => {
        if (typeof cb === "function") {
          cb(400);
        }
      },
    );
    wrap(<CompactFileInput data={ROW} />);
    const input = document.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;
    fireEvent.change(input, {
      target: { files: [new File(["x"], "bad.png", { type: "image/png" })] },
    });
    expect(
      screen.getByText("Upload failed — PDF, JPG, PNG, or TIFF up to 10 MB."),
    ).toBeInTheDocument();
  });
});
