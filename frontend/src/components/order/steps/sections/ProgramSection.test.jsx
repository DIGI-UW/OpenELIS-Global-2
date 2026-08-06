import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
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
  microbiologyOrderDetail: {
    patientOrigin: "",
    numberOfSets: "",
    clinicalHistory: "",
    antibioticExposure: false,
    criticalNotificationPreference: null,
    cultureMethodId: "",
  },
  sampleOrderItems: {},
};

const cultureSamples = [
  {
    sampleTypeId: "5",
    sampleTypeName: "Blood",
    tests: [
      {
        id: "42",
        name: "Blood culture",
        cultureWorkflowType: "BACTERIOLOGY",
        methods: [
          {
            methodId: "7",
            methodName: "Blood Culture Standard",
            methodCode: "BCSTD",
            isDefault: true,
          },
        ],
      },
    ],
  },
];

describe("ProgramSection microbiology derivation", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-programs") {
        callback([
          { id: "1", value: "Routine Testing", code: "ROUTINE" },
          { id: "8", value: "Microbiology", code: "MICROBIOLOGY" },
        ]);
      } else {
        callback({});
      }
    });
  });

  it("auto-selects the Microbiology Program by code for a culture test", async () => {
    const setOrderData = vi.fn();
    render(
      <IntlProvider locale="en" messages={messages}>
        <ProgramSection
          orderData={orderData}
          setOrderData={setOrderData}
          samples={cultureSamples}
          isReadOnly={false}
        />
      </IntlProvider>,
    );

    expect(
      await screen.findByRole("heading", {
        name: "Microbiology Program Details",
      }),
    ).toBeInTheDocument();
    expect(setOrderData).toHaveBeenCalledWith(expect.any(Function));
    const update = setOrderData.mock.calls
      .map(([value]) => value)
      .filter((value) => typeof value === "function")
      .find((value) => value(orderData).sampleOrderItems?.programId === "8");
    expect(update).toBeDefined();
    expect(update(orderData).sampleOrderItems.programId).toBe("8");
  });

  it("shows a named configuration error when the Program is unavailable", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/user-programs") {
        callback([{ id: "1", value: "Routine Testing", code: "ROUTINE" }]);
      } else {
        callback({});
      }
    });

    render(
      <IntlProvider locale="en" messages={messages}>
        <ProgramSection
          orderData={orderData}
          setOrderData={vi.fn()}
          samples={cultureSamples}
          isReadOnly={false}
        />
      </IntlProvider>,
    );

    expect(
      await screen.findByText(
        "Microbiology Program is not configured for this order workflow.",
      ),
    ).toBeInTheDocument();
  });

  it("shows the same details for a manually selected Microbiology Program with an untyped test", async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <ProgramSection
          orderData={{
            ...orderData,
            sampleOrderItems: { programId: "8" },
          }}
          setOrderData={vi.fn()}
          samples={[
            {
              sampleTypeId: "5",
              sampleTypeName: "Blood",
              tests: [
                {
                  id: "43",
                  name: "Untyped culture",
                  methods: [
                    {
                      methodId: "7",
                      methodName: "Blood Culture Standard",
                      isDefault: true,
                    },
                  ],
                },
              ],
            },
          ]}
          isReadOnly={false}
        />
      </IntlProvider>,
    );

    expect(
      await screen.findByRole("heading", {
        name: "Microbiology Program Details",
      }),
    ).toBeInTheDocument();
    expect(screen.getByText("Unassigned")).toBeInTheDocument();
    expect(
      screen.getByDisplayValue("Blood Culture Standard"),
    ).toBeInTheDocument();
  });
});
