import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../../utils/Utils", () => ({ getFromOpenElisServer }));

vi.mock("../../../common/Questionnaire", () => ({
  default: () => <div data-testid="questionnaire" />,
}));

import ProgramSection from "./ProgramSection";

const orderData = {
  sampleOrderItems: {},
};

describe("ProgramSection program-specific fields", () => {
  const renderWithPrograms = (programs, questionnaire, programId) => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-programs") {
        callback(programs);
      } else if (url.endsWith("/questionnaire")) {
        callback(questionnaire);
      } else {
        callback({});
      }
    });
    return render(
      <IntlProvider locale="en" messages={messages}>
        <ProgramSection
          orderData={{
            ...orderData,
            sampleOrderItems: { ...orderData.sampleOrderItems, programId },
          }}
          setOrderData={vi.fn()}
          isReadOnly={false}
        />
      </IntlProvider>,
    );
  };

  // OGC-1201 Y: isVLProgram was a bare `includes("vl")` on the programme
  // name, so any programme whose name merely contains those letters took the
  // viral-load branch.
  it("does not take the viral load branch for a name that merely contains vl", async () => {
    renderWithPrograms(
      [{ id: "3", value: "Sylvatic Surveillance", code: "SYLV" }],
      null,
      "3",
    );

    expect(
      await screen.findByRole("combobox", { name: "Program" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByLabelText(/Pregnancy \/ Breastfeeding Status/),
    ).not.toBeInTheDocument();
  });

  it("takes the viral load branch on the programme code", async () => {
    renderWithPrograms([{ id: "4", value: "Routine", code: "VL" }], null, "4");

    expect(
      await screen.findByLabelText(/Pregnancy \/ Breastfeeding Status/),
    ).toBeInTheDocument();
  });

  // The VL panel used to render *instead of* the questionnaire, so a VL
  // programme's configured questionnaire could never be reached.
  it("renders a configured questionnaire even for a viral load programme", async () => {
    renderWithPrograms(
      [{ id: "5", value: "Viral Load", code: "VL" }],
      { item: [{ linkId: "q1", text: "Pregnant?", type: "boolean" }] },
      "5",
    );

    expect(await screen.findByTestId("questionnaire")).toBeInTheDocument();
    expect(
      screen.queryByLabelText(/Pregnancy \/ Breastfeeding Status/),
    ).not.toBeInTheDocument();
  });
});

describe("ProgramSection domain filter (OGC-781)", () => {
  const renderForDomain = (domain, programId) =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <ProgramSection
          orderData={{
            ...orderData,
            sampleOrderItems: programId ? { programId } : {},
          }}
          setOrderData={vi.fn()}
          isReadOnly={false}
          domain={domain}
        />
      </IntlProvider>,
    );

  beforeEach(() => {
    getFromOpenElisServer.mockReset();
  });

  it("asks the server for the order domain's active programs only", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/user-programs")) {
        callback([{ id: "7", value: "Water Quality", code: "WQ" }]);
      } else {
        callback({});
      }
    });

    renderForDomain("ENVIRONMENTAL");

    expect(
      await screen.findByRole("combobox", { name: "Program" }),
    ).toBeInTheDocument();
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/user-programs?domain=ENVIRONMENTAL",
      expect.any(Function),
    );
  });

  it("explains when no program of the order's domain is active", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(url.startsWith("/rest/user-programs") ? [] : {});
    });

    renderForDomain("VECTOR");

    expect(
      await screen.findByText(/No Vector programs are currently active/),
    ).toBeInTheDocument();
  });

  it("keeps showing the program an order already names when the picker no longer offers it", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/user-programs")) {
        callback([{ id: "1", value: "Routine Testing", code: "ROUTINE" }]);
      } else if (url === "/rest/program/9") {
        callback({
          program: { id: "9", programName: "Retired Survey", code: "RET" },
          domain: "CLINICAL",
          active: false,
        });
      } else {
        callback({});
      }
    });

    renderForDomain("CLINICAL", "9");

    expect(
      await screen.findByDisplayValue("Retired Survey"),
    ).toBeInTheDocument();
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/program/9",
      expect.any(Function),
    );
  });
});
