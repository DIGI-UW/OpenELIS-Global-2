import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import DisposalSection from "./DisposalSection";
import messages from "../../../../languages/en.json";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <DisposalSection sampleTypeId="7" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((_url, cb) =>
    cb({
      success: true,
      data: { id: "7", disposalInstructions: "Autoclave first" },
    }),
  );
});

describe("DisposalSection", () => {
  it("loads the stored disposal instructions into the textarea", async () => {
    renderSection();

    expect(document.getElementById("st-disposal-instructions")).toHaveValue(
      "Autoclave first",
    );
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/sample-types/7",
      expect.any(Function),
    );
  });

  it("saves only the disposal field — never isActive — so section saves can't reactivate a type", async () => {
    putToOpenElisServer.mockImplementation((_url, _body, cb) => cb(200));
    renderSection();

    fireEvent.change(document.getElementById("st-disposal-instructions"), {
      target: { value: "Incinerate via biohazard waste" },
    });
    fireEvent.click(document.getElementById("st-disposal-save"));

    const [url, body] = putToOpenElisServer.mock.calls[0];
    expect(url).toBe("/rest/sample-types/7");
    expect(JSON.parse(body)).toEqual({
      disposalInstructions: "Incinerate via biohazard waste",
    });

    expect(
      await screen.findByText("Disposal instructions saved."),
    ).toBeInTheDocument();
  });

  it("surfaces an error when the save fails", async () => {
    putToOpenElisServer.mockImplementation((_url, _body, cb) => cb(500));
    renderSection();

    fireEvent.click(document.getElementById("st-disposal-save"));

    expect(
      await screen.findByText("Could not save disposal instructions."),
    ).toBeInTheDocument();
  });
});
