import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import { ConfigurationContext } from "../../../layout/Layout";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";

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

  test("printDisabled keeps every card's Print Labels button disabled", () => {
    const onPrintLabels = vi.fn();
    renderSection({ onPrintLabels, printDisabled: true });
    const button = screen.getByRole("button", {
      name: messages["collect.sample.printLabels"],
    });
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(onPrintLabels).not.toHaveBeenCalled();
  });
});

describe("SamplesCollectionSection receipt and handling (OGC-1424, FR-B23, FR-C9a)", () => {
  const twoSamples = [
    { ...sample, index: 0, sampleItemId: "501", arrivalCondition: "" },
    { ...sample, index: 1, sampleItemId: "502", arrivalCondition: "" },
  ];
  const renderWithUser = (props = {}) =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <UserSessionDetailsContext.Provider
          value={{
            userSessionDetails: {
              userId: "7",
              firstName: "Mary",
              lastName: "Kila",
            },
          }}
        >
          <ConfigurationContext.Provider
            value={{ configurationProperties: {} }}
          >
            <SamplesCollectionSection
              samples={twoSamples}
              setSamples={vi.fn()}
              sampleTypes={[{ id: "12", value: "Serum" }]}
              unitOfMeasures={[]}
              updateSampleCollectionDetails={vi.fn()}
              isReadOnly={false}
              admissionDate=""
              {...props}
            />
          </ConfigurationContext.Provider>
        </UserSessionDetailsContext.Provider>
      </IntlProvider>,
    );

  test("Received by reads as the signed-in user with Change", () => {
    renderWithUser();

    expect(screen.getByTestId("received-by-text")).toHaveTextContent(
      "Received by Mary Kila (you)",
    );
    expect(screen.getByTestId("received-by-change")).toHaveTextContent(
      "Change",
    );
  });

  test("a receiver recorded by someone else reads without (you)", () => {
    renderWithUser({
      samples: [
        { ...twoSamples[0], receivedById: "9", receivedByName: "John Tau" },
        twoSamples[1],
      ],
    });

    expect(screen.getByTestId("received-by-text")).toHaveTextContent(
      "Received by John Tau",
    );
    expect(screen.getByTestId("received-by-text")).not.toHaveTextContent(
      "(you)",
    );
  });

  test("Same for all samples copies the arrival to every primary sample", () => {
    const updateSampleCollectionDetails = vi.fn();
    renderWithUser({
      samples: [
        { ...twoSamples[0], arrivalCondition: "REFRIGERATED" },
        twoSamples[1],
      ],
      updateSampleCollectionDetails,
    });

    fireEvent.click(screen.getByTestId("handling-same-for-all-0"));

    expect(updateSampleCollectionDetails).toHaveBeenCalledWith(0, {
      arrivalCondition: "REFRIGERATED",
      arrivalTemperature: "",
    });
    expect(updateSampleCollectionDetails).toHaveBeenCalledWith(1, {
      arrivalCondition: "REFRIGERATED",
      arrivalTemperature: "",
    });
  });

  test("environmental samples keep their own fields and no receiver line", () => {
    renderWithUser({ workflowType: "environmental" });

    expect(screen.queryByTestId("received-by-line")).toBeNull();
    expect(screen.queryByTestId("handling-group-0")).toBeNull();
  });
});

describe("SamplesCollectionSection help text (OGC-1443)", () => {
  test("points at the controls the page has, never at the removed Print More Sample Labels", () => {
    const missing = vi.spyOn(console, "error");
    renderSection();

    expect(
      screen.getByText(messages["collect.printMoreLabels.helper"]),
    ).toBeVisible();
    expect(
      screen.queryByText(/Print More Sample Labels/),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Add Sample" })).toBeVisible();
    expect(
      missing.mock.calls.some((call) =>
        String(call[0]).includes("MISSING_TRANSLATION"),
      ),
    ).toBe(false);
    missing.mockRestore();
  });
});
