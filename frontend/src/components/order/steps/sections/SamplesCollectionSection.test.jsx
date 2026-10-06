import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import { ConfigurationContext } from "../../../layout/Layout";

vi.mock("../../../addOrder/GpsCoordinatesCapture", () => ({
  default: () => null,
}));

import SamplesCollectionSection from "./SamplesCollectionSection";

const sample = {
  index: 0,
  sampleItemId: "501",
  sampleTypeId: "12",
  sampleTypeName: "Serum",
  tests: [],
  panels: [],
  collectionDate: "",
  collectionTime: "",
  receivedDate: "",
  receivedTime: "",
};

const renderSection = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
        <SamplesCollectionSection
          samples={[sample]}
          setSamples={vi.fn()}
          sampleTypes={[{ id: "12", value: "Serum" }]}
          unitOfMeasures={[]}
          updateSampleCollectionDetails={vi.fn()}
          isReadOnly={false}
          admissionDate=""
          {...props}
        />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

describe("SamplesCollectionSection print labels (OGC-1422)", () => {
  test("a sample card's Print Labels button reports that sample's position", () => {
    const onPrintLabels = vi.fn();
    renderSection({ onPrintLabels });

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["collect.sample.printLabels"],
      }),
    );

    expect(onPrintLabels).toHaveBeenCalledWith(0);
  });

  test("the button is harmless when no printer is registered", () => {
    renderSection();

    expect(() =>
      fireEvent.click(
        screen.getByRole("button", {
          name: messages["collect.sample.printLabels"],
        }),
      ),
    ).not.toThrow();
  });
});
