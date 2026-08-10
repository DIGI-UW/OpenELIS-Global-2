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

import {
  AttachmentsSection,
  OrderInfoSection,
  ProgrammeSection,
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
        editable
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
        editable
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

  it("the legacy per-result file (old Results page upload) is listed and viewable", () => {
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
        legacyResultFile={{
          fileName: "scan.png",
          fileType: "image/png",
          content: "AAAA",
        }}
      />,
    );
    expect(screen.getByTestId("legacy-result-file")).toBeInTheDocument();
    expect(screen.getByText("scan.png")).toBeInTheDocument();
    expect(screen.getByText("Result")).toBeInTheDocument();
  });
});
