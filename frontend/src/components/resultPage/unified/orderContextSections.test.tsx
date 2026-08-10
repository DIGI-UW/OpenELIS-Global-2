import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));
// eslint-disable-next-line import/first
import { getFromOpenElisServer } from "../../utils/Utils";
const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;

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
  beforeEach(() => getMock.mockReset());

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

  it("Attachments section is not mounted when the order has no files (FR-C5)", () => {
    getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
      if (typeof cb === "function") {
        cb([]);
      }
    });
    const { container } = wrap(
      <AttachmentsSection
        open={false}
        onToggle={() => {}}
        accessionNumber="DEV1"
      />,
    );
    expect(container.querySelector(".unifiedRefSection")).toBeNull();
  });
});
