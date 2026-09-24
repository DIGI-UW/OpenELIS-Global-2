import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import TestSelectForm from "../TestSelectForm";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
  };
});

/**
 * The workplan's test dropdown offers the tests the signed-in user may work
 * on. The server narrows that list to their Results lab units; asking for the
 * whole catalogue let a user choose a test they hold no lab unit for and land
 * on an empty workplan.
 */
describe("Workplan test selection", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/test-list")) {
        callback([
          { id: "10", value: "Haemoglobin" },
          { id: "11", value: "Platelet Count" },
        ]);
      } else {
        callback([]);
      }
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("offers the user's own tests, read from the lab-unit scoped list", async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <TestSelectForm value={vi.fn()} title="Test" />
      </IntlProvider>,
    );

    expect(
      await screen.findByRole("option", { name: "Haemoglobin" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: "Platelet Count" }),
    ).toBeInTheDocument();

    const requested = getFromOpenElisServer.mock.calls.map(([url]) => url);
    expect(requested).toContain("/rest/test-list");
    expect(requested).not.toContain("/rest/displayList/ALL_TESTS");
  });
});
