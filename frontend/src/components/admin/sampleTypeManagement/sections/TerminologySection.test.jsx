import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import TerminologySection from "./TerminologySection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

const legacyWhonetMapping = {
  id: "legacy-whonet",
  source: "WHONET",
  code: "BLD",
  relationship: "SAME_AS",
};

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <TerminologySection sampleTypeId="sample-type-1" />
    </IntlProvider>,
  );

describe("Sample type terminology mappings", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback({
        sampleTypeId: "sample-type-1",
        mappings: [legacyWhonetMapping],
      }),
    );
    putToOpenElisServer.mockImplementation((_url, _payload, callback) =>
      callback(200),
    );
  });

  test("round-trips a retired WHONET source without offering an invalid edit", async () => {
    const user = userEvent.setup();
    renderSection();

    expect(await screen.findByText("WHONET")).toBeInTheDocument();
    expect(
      screen.queryByTestId("sampleType-edit-mapping-0"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Remove mapping" }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalledTimes(1));
    expect(JSON.parse(putToOpenElisServer.mock.calls[0][1])).toEqual({
      sampleTypeId: "sample-type-1",
      mappings: [legacyWhonetMapping],
    });
  });
});
