import React from "react";
import { render, fireEvent, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import AttachSamplePanel, { buildFiltersFromReferenceItem } from "./AttachSamplePanel";
import { getFromOpenElisServer } from "../../../utils/Utils";

jest.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
}));

jest.mock("./biorepositorySamplePathHelpers", () => ({
  formatBrf02SamplePath: () => "ROOM / DEVICE / BOX",
}));

const renderPanel = (props) =>
  render(
    <IntlProvider locale="en">
      <AttachSamplePanel {...props} />
    </IntlProvider>,
  );

describe("AttachSamplePanel", () => {
  test("buildFiltersFromReferenceItem maps requested fields", () => {
    expect(
      buildFiltersFromReferenceItem({
        requestedSampleType: "PLASMA",
        requestedOriginLab: "LAB-A",
        requestedProjectId: "P-1",
        requestedAccessionNumber: "ACC-1",
        requestedBarcode: "BC-1",
      }),
    ).toMatchObject({
      sampleType: "PLASMA",
      originLab: "LAB-A",
      projectId: "P-1",
      accessionNumber: "ACC-1",
      barcode: "BC-1",
    });
  });

  test("typing updates filters and search calls API with query", () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));

    renderPanel({
      referenceItem: { id: 1 },
      onAttachSuccess: jest.fn(),
      onCancel: jest.fn(),
    });

    fireEvent.change(screen.getByLabelText("Accession / Lab Number"), {
      target: { value: "ACC-99" },
    });
    fireEvent.change(screen.getByLabelText("Batch No. / Barcode"), {
      target: { value: "BC-99" },
    });

    fireEvent.click(screen.getByText("Search Inventory"));

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringMatching(
        /\/rest\/biorepository\/sample\/search\?status=STORED&limit=50&accessionNumber=ACC-99&barcode=BC-99/,
      ),
      expect.any(Function),
    );

    expect(screen.getByText("No stored samples matched your search.")).toBeTruthy();
  });

  test("renders storage path column for results", () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([
        {
          id: 101,
          accessionNumber: "ACC-1",
          barcode: "BC-1",
          originLab: "LAB-A",
          sampleType: { description: "PLASMA" },
          remainingQuantity: 1,
          unitOfMeasure: "mL",
        },
      ]),
    );

    renderPanel({
      referenceItem: { id: 1, requestedAccessionNumber: "ACC-1" },
      onAttachSuccess: jest.fn(),
      onCancel: jest.fn(),
    });

    // Auto-search runs on mount due to prefill.
    expect(screen.getByText("ROOM / DEVICE / BOX")).toBeTruthy();
  });
});

