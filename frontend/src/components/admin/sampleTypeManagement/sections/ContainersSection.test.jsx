/**
 * Sample type Containers section: OGC-1611 FR-G7 (default container type) and
 * FR-G6b (the tests a sample type is primary and secondary for).
 */

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerJsonResponse: vi.fn(),
}));

import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ContainersSection from "./ContainersSection";
import {
  getFromOpenElisServer,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const PLASMA_VIEW = {
  sampleTypeId: "3",
  defaultContainerTypeId: null,
  containerChoices: [
    { id: 1, name: "K2EDTA tube 4 mL", active: true },
    { id: 8, name: "Plasma separator tube (PST)", active: true },
  ],
  primaryForTests: [{ id: "61", value: "Glucose(Plasma)" }],
  secondaryForTests: [
    {
      testId: "70",
      testName: "Creatinine(Serum)",
      containerName: "Plasma separator tube (PST)",
      usedAsSampleTypeName: "Serum",
    },
  ],
};

const renderSection = () => {
  getFromOpenElisServer.mockImplementation((url, callback) =>
    callback(PLASMA_VIEW),
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <ContainersSection sampleTypeId="3" />
    </IntlProvider>,
  );
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("Sample type ContainersSection", () => {
  it("renders once the server answers after the first render, as a real fetch does", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      setTimeout(() => callback(PLASMA_VIEW), 0),
    );
    render(
      <IntlProvider locale="en" messages={messages}>
        <ContainersSection sampleTypeId="3" />
      </IntlProvider>,
    );

    expect(
      await screen.findByTestId("sample-type-containers"),
    ).toBeInTheDocument();
  });

  it("names the tests the sample type is primary and secondary for", () => {
    renderSection();

    expect(screen.getByTestId("sample-type-primary-tests")).toHaveTextContent(
      "Primary for 1 testGlucose(Plasma)",
    );
    expect(screen.getByTestId("sample-type-secondary-tests")).toHaveTextContent(
      "Secondary for 1 testCreatinine(Serum), via Plasma separator tube (PST), used as Serum",
    );
  });

  it("saves the chosen default container type and clears it with None", () => {
    renderSection();
    putToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ ...PLASMA_VIEW, defaultContainerTypeId: 8 }),
    );
    const save = screen.getByTestId("sample-type-containers-save");
    expect(save).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Default container type"), {
      target: { value: "8" },
    });
    fireEvent.click(save);

    expect(putToOpenElisServerJsonResponse.mock.calls[0][0]).toBe(
      "/rest/sample-type-containers/3",
    );
    expect(
      JSON.parse(putToOpenElisServerJsonResponse.mock.calls[0][1]),
    ).toEqual({ defaultContainerTypeId: 8 });
    expect(screen.getByLabelText("Default container type")).toHaveValue("8");
    expect(
      screen.getByText("The default container type is saved."),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Default container type"), {
      target: { value: "" },
    });
    fireEvent.click(save);
    expect(
      JSON.parse(putToOpenElisServerJsonResponse.mock.calls[1][1]),
    ).toEqual({ defaultContainerTypeId: null });
  });

  it("says why a default container is refused", () => {
    renderSection();
    putToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({
        status: 400,
        field: "defaultContainerTypeId",
        code: "defaultContainerSampleType",
      }),
    );

    fireEvent.change(screen.getByLabelText("Default container type"), {
      target: { value: "1" },
    });
    fireEvent.click(screen.getByTestId("sample-type-containers-save"));

    expect(
      screen.getByText("This container type does not yield this sample type."),
    ).toBeInTheDocument();
  });
});
