import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFormData: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));
// eslint-disable-next-line import/first
import {
  getFromOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;
const postFormMock = postToOpenElisServerFormData as ReturnType<typeof vi.fn>;
const postJsonMock = postToOpenElisServerJsonResponse as ReturnType<
  typeof vi.fn
>;

import {
  AttachmentsSection,
  OrderInfoSection,
  ProgrammeSection,
  StorageSection,
  attachmentVisibleOnRow,
  storageAssignmentRequest,
} from "./orderContextSections";

/**
 * OGC-811 gallery parity — order-fed reference sections: content comes from
 * the shipped /rest/order/search + attachments endpoints; sections with no
 * content are not mounted (FR-C5).
 */
const wrap = (node: React.ReactElement) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {node}
    </IntlProvider>,
  );

describe("orderContextSections (FR-C3/C5)", () => {
  beforeEach(() => {
    getMock.mockReset();
    postFormMock.mockReset();
    postJsonMock.mockReset();
  });

  it("Order info summarizes clinician · priority · site when closed", () => {
    wrap(
      <OrderInfoSection
        open={false}
        onToggle={() => {}}
        order={{
          loaded: true,
          sampleOrderItems: {
            providerFirstName: "M.",
            providerLastName: "Chen",
            priority: "STAT",
            referringSiteName: "Endocrinology",
          },
        }}
      />,
    );
    expect(
      screen.getByText("M. Chen · STAT · Endocrinology"),
    ).toBeInTheDocument();
  });

  it("Programme section is not mounted without a program or EQA flag (FR-C5)", () => {
    const { container } = wrap(
      <ProgrammeSection
        open={false}
        onToggle={() => {}}
        order={{ loaded: true, sampleOrderItems: {} }}
      />,
    );
    expect(container.querySelector(".unifiedRefSection")).toBeNull();
  });

  it("Programme section renders the program name and EQA tag", () => {
    wrap(
      <ProgrammeSection
        open={true}
        onToggle={() => {}}
        order={{
          loaded: true,
          sampleOrderItems: { program: "PMI Vector Sentinel" },
        }}
        eqaSample
        eqaPriority="STANDARD"
      />,
    );
    expect(screen.getByText("PMI Vector Sentinel")).toBeInTheDocument();
    expect(screen.getByText("EQA · STANDARD")).toBeInTheDocument();
  });

  it("Programme section renders the captured programme data (programView parity)", () => {
    wrap(
      <ProgrammeSection
        open={true}
        onToggle={() => {}}
        order={{
          loaded: true,
          sampleOrderItems: {
            program: "Histopathology",
            additionalQuestions: {
              item: [
                {
                  text: "Specimen site",
                  answer: [{ valueString: "Left breast" }],
                },
                {
                  text: "Grossing performed",
                  answer: [{ valueBoolean: true }],
                },
              ],
            },
          },
        }}
      />,
    );
    expect(screen.getByTestId("program-captured-data")).toBeInTheDocument();
    expect(screen.getByText("Specimen site")).toBeInTheDocument();
    expect(screen.getByText("Left breast")).toBeInTheDocument();
    expect(screen.getByText("Grossing performed")).toBeInTheDocument();
    expect(screen.getByText("True")).toBeInTheDocument();
  });

  it("Programme section omits the captured-data block when no responses exist", () => {
    wrap(
      <ProgrammeSection
        open={true}
        onToggle={() => {}}
        order={{
          loaded: true,
          sampleOrderItems: { program: "Histopathology" },
        }}
      />,
    );
    expect(screen.getByText("Histopathology")).toBeInTheDocument();
    expect(screen.queryByTestId("program-captured-data")).toBeNull();
  });

  it("Storage section offers Assign Location when nothing is assigned", async () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof url === "string" && url.includes("/rest/storage/")) {
        cb({});
      }
    });
    wrap(
      <StorageSection
        open={true}
        onToggle={() => {}}
        sampleItemId="41"
        accessionNumber="DEV1"
        sampleType="Serum"
      />,
    );
    expect(
      await screen.findByTestId("storage-location-button"),
    ).toHaveTextContent("Assign Location");
  });

  it("Storage section offers Move to Location once a location exists", async () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof url === "string" && url.includes("/rest/storage/")) {
        cb({ hierarchicalPath: "Main Lab > Freezer 2 > Shelf 1" });
      }
    });
    wrap(
      <StorageSection
        open={true}
        onToggle={() => {}}
        sampleItemId="41"
        accessionNumber="DEV1"
        sampleType="Serum"
      />,
    );
    expect(
      await screen.findByTestId("storage-location-button"),
    ).toHaveTextContent("Move to Location");
  });

  it("the storage button opens the shipped LocationPickerModal inline (no navigation)", async () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof url === "string" && url.includes("/rest/storage/")) {
        cb({});
      }
    });
    wrap(
      <StorageSection
        open={true}
        onToggle={() => {}}
        sampleItemId="41"
        accessionNumber="DEV1"
        sampleType="Serum"
      />,
    );
    fireEvent.click(await screen.findByTestId("storage-location-button"));
    expect(
      await screen.findByText("Assign Storage Location"),
    ).toBeInTheDocument();
  });

  it("confirm translates to the same assign/move REST calls the old page makes", () => {
    const selection = {
      room: { id: 3, name: "Main Lab" },
      device: { id: 7, name: "Freezer 2" },
      shelf: { id: 12, name: "Shelf 1" },
    };
    const assign = storageAssignmentRequest("41", false, {
      selection,
      position: { mode: "text", value: "A1" },
      notes: "keep frozen",
    });
    expect(assign).toEqual({
      url: "/rest/storage/sample-items/assign",
      body: {
        sampleItemId: "41",
        locationId: "12",
        locationType: "shelf",
        positionCoordinate: "A1",
        notes: "keep frozen",
      },
    });

    const move = storageAssignmentRequest("41", true, {
      selection,
      position: null,
      reason: "",
      notes: "",
    });
    expect(move?.url).toBe("/rest/storage/sample-items/move");
    expect(move?.body.reason).toBe("Reassignment from result entry workflow");

    // nothing assignable selected → no request
    expect(
      storageAssignmentRequest("41", false, { selection: {}, position: null }),
    ).toBeNull();
  });

  it("Attachments section lists files from the order-attachments endpoint", async () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof url !== "string") {
        return;
      }
      if (url.includes("/attachments")) {
        cb([
          {
            id: 7,
            fileName: "insurance-auth.pdf",
            fileSizeBytes: 91136,
            uploadedAt: "06/10/2026 06:45",
          },
        ]);
      }
    });
    wrap(
      <AttachmentsSection
        open={true}
        onToggle={() => {}}
        accessionNumber="DEV1"
      />,
    );
    expect(await screen.findByText("insurance-auth.pdf")).toBeInTheDocument();
    expect(screen.getByText("Download")).toBeInTheDocument();
  });

  it("Attachments section stays mounted with an empty state when the order has no files", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof cb === "function") {
        cb([]);
      }
    });
    wrap(
      <AttachmentsSection
        open={true}
        onToggle={() => {}}
        accessionNumber="DEV1"
      />,
    );
    expect(
      screen.getByText("No attachments on this order yet."),
    ).toBeInTheDocument();
  });

  it("the Add attachment button shows on read-only rows too (old-page behavior)", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof cb === "function") {
        cb([]);
      }
    });
    wrap(
      <AttachmentsSection
        open={true}
        onToggle={() => {}}
        accessionNumber="DEV1"
        analysisId="25"
        componentId="comp-A"
      />,
    );
    expect(screen.getAllByText("Add attachment").length).toBeGreaterThan(0);
    expect(document.querySelector('input[type="file"]')).not.toBeNull();
  });

  it("collapsed empty Attachments section summarizes as none", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof cb === "function") {
        cb([]);
      }
    });
    wrap(
      <AttachmentsSection
        open={false}
        onToggle={() => {}}
        accessionNumber="DEV1"
      />,
    );
    expect(screen.getByText("none")).toBeInTheDocument();
  });

  it("editable rows can add an attachment — multipart POST then list refresh", () => {
    let listCalls = 0;
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof url !== "string" || typeof cb !== "function") {
        return;
      }
      if (url.includes("/attachments")) {
        listCalls += 1;
        cb(
          listCalls > 1
            ? [{ id: 9, fileName: "lab-report.pdf", fileSizeBytes: 2048 }]
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
    wrap(
      <AttachmentsSection
        open={true}
        onToggle={() => {}}
        accessionNumber="DEV1"
      />,
    );
    const input = document.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;
    expect(input).not.toBeNull();
    const file = new File(["%PDF-1.4"], "lab-report.pdf", {
      type: "application/pdf",
    });
    fireEvent.change(input, { target: { files: [file] } });
    expect(postFormMock).toHaveBeenCalledWith(
      "/rest/order/DEV1/attachments",
      expect.any(FormData),
      expect.any(Function),
    );
    const sentForm = postFormMock.mock.calls[0][1] as FormData;
    expect((sentForm.get("files") as File).name).toBe("lab-report.pdf");
    expect(screen.getByText("lab-report.pdf")).toBeInTheDocument();
  });

  it("oversized files are rejected client-side without posting", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof cb === "function") {
        cb([]);
      }
    });
    wrap(
      <AttachmentsSection
        open={true}
        onToggle={() => {}}
        accessionNumber="DEV1"
      />,
    );
    const input = document.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;
    const big = new File(["x"], "big.pdf", { type: "application/pdf" });
    Object.defineProperty(big, "size", { value: 11 * 1024 * 1024 });
    fireEvent.change(input, { target: { files: [big] } });
    expect(postFormMock).not.toHaveBeenCalled();
    expect(
      screen.getByText("File exceeds the 10 MB limit."),
    ).toBeInTheDocument();
  });

  it("attachment visibility follows the persisted scope (component isolation)", () => {
    const orderLevel = { id: 1, analysisId: "", testResultComponentId: "" };
    const analysisWide = { id: 2, analysisId: "28", testResultComponentId: "" };
    const componentA = {
      id: 3,
      analysisId: "28",
      testResultComponentId: "comp-A",
    };
    // order-level: everywhere
    expect(attachmentVisibleOnRow(orderLevel, "28", "comp-A")).toBe(true);
    expect(attachmentVisibleOnRow(orderLevel, "99", undefined)).toBe(true);
    // analysis-wide: only that analysis's rows
    expect(attachmentVisibleOnRow(analysisWide, "28", "comp-A")).toBe(true);
    expect(attachmentVisibleOnRow(analysisWide, "28", "comp-B")).toBe(true);
    expect(attachmentVisibleOnRow(analysisWide, "99", "comp-A")).toBe(false);
    // component-scoped: only the matching component row
    expect(attachmentVisibleOnRow(componentA, "28", "comp-A")).toBe(true);
    expect(attachmentVisibleOnRow(componentA, "28", "comp-B")).toBe(false);
    expect(attachmentVisibleOnRow(componentA, "99", "comp-A")).toBe(false);
  });

  it("component rows only show their own attachments; order-level shows everywhere", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof url !== "string" || typeof cb !== "function") {
        return;
      }
      if (url.includes("/attachments")) {
        cb([
          { id: 1, fileName: "order.pdf", analysisId: "" },
          {
            id: 2,
            fileName: "hgb.png",
            analysisId: "28",
            testResultComponentId: "comp-A",
          },
          {
            id: 3,
            fileName: "wbc.png",
            analysisId: "28",
            testResultComponentId: "comp-B",
          },
        ]);
      }
    });
    wrap(
      <AttachmentsSection
        open={true}
        onToggle={() => {}}
        accessionNumber="DEV1"
        analysisId="28"
        componentId="comp-A"
      />,
    );
    expect(screen.getByText("order.pdf")).toBeInTheDocument();
    expect(screen.getByText("hgb.png")).toBeInTheDocument();
    expect(screen.queryByText("wbc.png")).not.toBeInTheDocument();
    expect(screen.getByText("Order Entry")).toBeInTheDocument();
    expect(screen.getByText("Results")).toBeInTheDocument();
  });

  it("uploads carry the row's analysis + component scope", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof cb === "function") {
        cb([]);
      }
    });
    postFormMock.mockImplementation(
      (_url: string, _form: FormData, cb: (status: number) => void) => {
        if (typeof cb === "function") {
          cb(200);
        }
      },
    );
    wrap(
      <AttachmentsSection
        open={true}
        onToggle={() => {}}
        accessionNumber="DEV1"
        analysisId="28"
        componentId="comp-A"
      />,
    );
    const input = document.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;
    const file = new File(["%PDF-1.4"], "hgb.pdf", {
      type: "application/pdf",
    });
    fireEvent.change(input, { target: { files: [file] } });
    expect(postFormMock).toHaveBeenCalledWith(
      "/rest/order/DEV1/attachments?analysisId=28&testResultComponentId=comp-A",
      expect.any(FormData),
      expect.any(Function),
    );
  });
});
