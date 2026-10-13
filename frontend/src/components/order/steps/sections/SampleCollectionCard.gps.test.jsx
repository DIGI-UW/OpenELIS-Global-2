import React, { useEffect } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import { ConfigurationContext } from "../../../layout/Layout";

/**
 * OGC-1443: the GPS control reports its values whenever it renders them,
 * including on mount. Prepare Samples opened right after Save and next was
 * flagged "Unsaved changes" because the card passed that report on as an edit.
 * The stand-in reports the way the real control does.
 */
vi.mock("../../../addOrder/GpsCoordinatesCapture", () => ({
  default: ({ sampleXml, onChange }) => {
    useEffect(() => {
      onChange({ ...sampleXml });
    }, []); // eslint-disable-line react-hooks/exhaustive-deps
    return (
      <button
        type="button"
        onClick={() =>
          onChange({
            ...sampleXml,
            gpsLatitude: "-18.8792",
            gpsLongitude: "47.5079",
            gpsCaptureMethod: "MANUAL",
          })
        }
      >
        set location
      </button>
    );
  },
}));

import SampleCollectionCard from "./SampleCollectionCard";

const renderCard = (onUpdate) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
      >
        <SampleCollectionCard
          sample={{
            sampleItemId: "10200",
            sampleTypeId: "5",
            sampleTypeName: "Urine",
            collectionDate: "2026-10-09",
            collectionTime: "08:30",
            receivedDate: "2026-10-09",
            receivedTime: "08:45",
            tests: [{ id: "930", name: "Albumin" }],
            panels: [],
          }}
          sampleIndex={0}
          sampleTypes={[]}
          unitOfMeasures={[]}
          serverReceivedDate="2026-10-09"
          serverReceivedTime="09:00"
          onUpdate={onUpdate}
          onRemove={vi.fn()}
          onPrintLabels={vi.fn()}
          isReadOnly={false}
          canRemove={false}
          workflowType="environmental"
        />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

describe("SampleCollectionCard GPS (OGC-1443)", () => {
  it("does not report the GPS values it was opened with as an edit", () => {
    const onUpdate = vi.fn();
    renderCard(onUpdate);

    expect(onUpdate).not.toHaveBeenCalled();
  });

  it("reports a location the user sets", () => {
    const onUpdate = vi.fn();
    renderCard(onUpdate);

    fireEvent.click(screen.getByRole("button", { name: "set location" }));

    expect(onUpdate).toHaveBeenCalledWith(
      0,
      expect.objectContaining({
        gpsLatitude: "-18.8792",
        gpsLongitude: "47.5079",
      }),
    );
  });
});
