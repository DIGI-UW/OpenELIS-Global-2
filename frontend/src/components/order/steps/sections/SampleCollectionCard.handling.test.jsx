import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import { ConfigurationContext } from "../../../layout/Layout";

vi.mock("../../../addOrder/GpsCoordinatesCapture", () => ({
  default: () => <div data-testid="gps-capture" />,
}));

const { requirementsMock, locationMock, dictionaryMock } = vi.hoisted(() => ({
  requirementsMock: vi.fn(),
  locationMock: vi.fn(),
  dictionaryMock: vi.fn(),
}));

vi.mock("../../api/orderEntryCleanupApi", () => ({
  getHandlingRequirements: (...args) => requirementsMock(...args),
  getSampleStorageLocation: (...args) => locationMock(...args),
}));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: (url, callback) => callback(dictionaryMock(url)),
}));

import SampleCollectionCard from "./SampleCollectionCard";

const SAMPLE = {
  sampleItemId: "10200",
  sampleTypeId: "5",
  sampleTypeName: "Serum",
  collectionDate: "2026-10-06",
  collectionTime: "09:00",
  receivedDate: "2026-10-06",
  receivedTime: "09:30",
  tests: [{ id: "11", name: "Haemoglobin" }],
  panels: [],
};

const renderCard = ({
  sample = SAMPLE,
  workflowType = "clinical",
  onUpdate = vi.fn(),
  onSameForAll,
} = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
      >
        <SampleCollectionCard
          sample={sample}
          sampleIndex={0}
          sampleTypes={[]}
          unitOfMeasures={[]}
          serverReceivedDate="2026-10-06"
          serverReceivedTime="09:30"
          onUpdate={onUpdate}
          onRemove={vi.fn()}
          onPrintLabels={vi.fn()}
          isReadOnly={false}
          canRemove={false}
          workflowType={workflowType}
          labNumber="DEV01260000000001200"
          onSameForAll={onSameForAll}
        />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

describe("SampleCollectionCard handling (OGC-1424, FR-C9, FR-C9a)", () => {
  beforeEach(() => {
    requirementsMock.mockReset();
    requirementsMock.mockResolvedValue([
      { testId: "11", storageCondition: "REFRIGERATED", holdingMinutes: 240 },
    ]);
    locationMock.mockReset();
    locationMock.mockResolvedValue({
      hierarchicalPath: "Main Lab > Fridge 2",
      temperatureSetting: "4.0",
    });
    dictionaryMock.mockReset();
    dictionaryMock.mockImplementation((url) =>
      url.includes("specimen-origins")
        ? [{ id: "1", dictEntry: "REFERRED", localizedName: "Referred in" }]
        : [],
    );
  });

  it("drops specimen origin, collection conditions, free-text temperature and lab performed sampling from the clinical row", () => {
    renderCard();

    expect(screen.queryByLabelText("Specimen Origin")).toBeNull();
    expect(screen.queryByLabelText("Collection Conditions")).toBeNull();
    expect(screen.queryByLabelText("Sample Temperature")).toBeNull();
    expect(screen.queryByLabelText("Lab performed sampling")).toBeNull();
    expect(screen.getByLabelText("Collection Method")).toBeInTheDocument();
  });

  it("keeps them on environmental and vector samples, with no handling group", () => {
    renderCard({ workflowType: "environmental" });

    expect(screen.getByLabelText("Sample Temperature")).toBeInTheDocument();
    expect(screen.getByLabelText("Lab performed sampling")).toBeInTheDocument();
    expect(screen.queryByTestId("handling-group-0")).toBeNull();
    expect(requirementsMock).not.toHaveBeenCalled();
  });

  it("shows the catalog requirement, the arrival fields and where the sample is stored", async () => {
    renderCard();

    await waitFor(() =>
      expect(screen.getByTestId("handling-required-0")).toHaveTextContent(
        "Required: Refrigerated (2–8°C) · process within 4 h",
      ),
    );
    expect(requirementsMock).toHaveBeenCalledWith(["11"]);
    expect(screen.getByLabelText("Arrived as")).toHaveValue("");
    expect(
      screen.getByLabelText("Measured temperature (°C)"),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByTestId("handling-stored-at-0")).toHaveTextContent(
        "Stored at: Main Lab > Fridge 2 (4.0 °C)",
      ),
    );
    expect(screen.queryByTestId("handling-mismatch-0")).toBeNull();
  });

  it("says No requirement when the catalog sets none", async () => {
    requirementsMock.mockResolvedValue([
      { testId: "11", storageCondition: null, holdingMinutes: null },
    ]);
    renderCard({ sample: { ...SAMPLE, sampleItemId: "" } });

    await waitFor(() =>
      expect(screen.getByTestId("handling-required-0")).toHaveTextContent(
        "Required: No requirement",
      ),
    );
    expect(screen.getByTestId("handling-stored-at-0")).toHaveTextContent(
      "Stored at: Not stored yet",
    );
    expect(locationMock).not.toHaveBeenCalled();
  });

  it("flags a handling mismatch with what differs and offers a prefilled non-conformity report, without blocking", async () => {
    renderCard({
      sample: { ...SAMPLE, arrivalCondition: "ROOM_TEMPERATURE" },
    });

    const flag = await screen.findByTestId("handling-mismatch-0");
    expect(within(flag).getByText("Handling mismatch")).toBeInTheDocument();
    expect(
      within(flag).getByLabelText(
        "Needs Refrigerated (2–8°C); arrived as Room temperature",
      ),
    ).toBeInTheDocument();
    const report = screen.getByTestId("handling-report-nce-0");
    expect(report).toHaveAttribute(
      "href",
      "/ReportNonConformingEvent?labNumber=DEV01260000000001200&description=" +
        encodeURIComponent(
          "Needs Refrigerated (2–8°C); arrived as Room temperature",
        ),
    );
    expect(screen.getByRole("button", { name: /Print Labels/ })).toBeEnabled();
  });

  it("records the arrival condition and measured temperature on the sample", async () => {
    const onUpdate = vi.fn();
    renderCard({ onUpdate });

    fireEvent.change(screen.getByLabelText("Arrived as"), {
      target: { value: "ON_ICE" },
    });
    fireEvent.change(screen.getByLabelText("Measured temperature (°C)"), {
      target: { value: "3.5" },
    });

    expect(onUpdate).toHaveBeenCalledWith(0, { arrivalCondition: "ON_ICE" });
    expect(onUpdate).toHaveBeenCalledWith(0, { arrivalTemperature: "3.5" });
  });

  it("Same for all samples hands this sample's arrival to the section", () => {
    const onSameForAll = vi.fn();
    renderCard({
      sample: {
        ...SAMPLE,
        arrivalCondition: "REFRIGERATED",
        arrivalTemperature: "5",
      },
      onSameForAll,
    });

    fireEvent.click(screen.getByText("Same for all samples"));

    expect(onSameForAll).toHaveBeenCalledWith({
      arrivalCondition: "REFRIGERATED",
      arrivalTemperature: "5",
    });
  });

  it("shows values saved before this version read-only", async () => {
    renderCard({
      sample: {
        ...SAMPLE,
        specimenOrigin: "REFERRED",
        collectionConditions: "Fasting",
        sampleTemperature: "4 C",
      },
    });

    const legacy = await screen.findByTestId("legacy-values-0");
    expect(
      within(legacy).getByText("Recorded before this version"),
    ).toBeInTheDocument();
    expect(legacy).toHaveTextContent("Specimen Origin: Referred in");
    expect(legacy).toHaveTextContent("Collection Conditions: Fasting");
    expect(legacy).toHaveTextContent("Sample Temperature: 4 C");
    expect(within(legacy).queryByRole("textbox")).toBeNull();
  });

  it("flags a measured temperature that cannot be stored", () => {
    renderCard({ sample: { ...SAMPLE, arrivalTemperature: "999" } });

    expect(screen.getByLabelText("Measured temperature (°C)")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(
      screen.getByText("Enter a temperature from -100 to 60 °C."),
    ).toBeInTheDocument();
  });
});
