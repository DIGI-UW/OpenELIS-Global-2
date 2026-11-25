/**
 * ProgramSelect — OGC-1222.
 *
 * The legacy order-entry program picker reads the same /rest/user-programs
 * endpoint that a deleted Program row used to break. It must survive a failed
 * fetch rather than crashing the page, and it must pick its default program by
 * code so a site that renames or translates "Routine Testing" keeps one.
 */

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../utils/Utils", () => ({ getFromOpenElisServer }));
vi.mock("../common/Questionnaire", () => ({ default: () => null }));

import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { ProgramSelect } from "./OrderEntryAdditionalQuestions";
import messages from "../../languages/en.json";

const wrap = (node) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {node}
    </IntlProvider>,
  );

const serve = (payload) =>
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url === "/rest/user-programs") {
      callback(payload);
    } else {
      callback({});
    }
  });

beforeEach(() => {
  getFromOpenElisServer.mockReset();
});

describe("ProgramSelect (OGC-1222)", () => {
  it("defaults to the routine program by code, not by its display name", async () => {
    // A site that renamed or translated the program keeps its default.
    serve([
      { id: "1", value: "Tests de routine", code: "ROUTINE" },
      { id: "8", value: "Microbiology", code: "MICROBIOLOGY" },
    ]);
    const programChange = vi.fn();

    wrap(
      <ProgramSelect
        programChange={programChange}
        orderFormValues={{ sampleOrderItems: {} }}
      />,
    );

    await waitFor(() =>
      expect(programChange).toHaveBeenCalledWith(
        expect.objectContaining({ target: { value: "1" } }),
      ),
    );
  });

  it("renders without crashing when the program fetch fails", async () => {
    serve(undefined);

    wrap(
      <ProgramSelect
        programChange={vi.fn()}
        orderFormValues={{ sampleOrderItems: {} }}
      />,
    );

    // No select is offered, and nothing throws: the page stays up.
    await waitFor(() =>
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        "/rest/user-programs",
        expect.any(Function),
      ),
    );
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
  });

  it("ignores a non-list payload such as a server error body", async () => {
    serve({ status: 500, error: "Internal Server Error" });

    wrap(
      <ProgramSelect
        programChange={vi.fn()}
        orderFormValues={{ sampleOrderItems: {} }}
      />,
    );

    await waitFor(() => expect(getFromOpenElisServer).toHaveBeenCalled());
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
  });
});
