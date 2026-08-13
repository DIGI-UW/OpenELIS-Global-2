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
  microbiologyOrderDetail: {
    patientOrigin: "",
    admissionDate: "",
    numberOfSets: "",
    clinicalHistory: "",
    antibioticExposure: false,
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
    expect(screen.getByRole("combobox", { name: "Program" })).toBeDisabled();
    expect(
      screen.getByText(
        "Microbiology is derived from the selected culture test.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByDisplayValue("Blood Culture Standard"),
    ).toBeInTheDocument();
    expect(getFromOpenElisServer).not.toHaveBeenCalledWith(
      "/rest/program/8/questionnaire",
      expect.any(Function),
    );
  });

  it("restores the derived Program and protocol from reloaded test metadata", async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <ProgramSection
          orderData={{
            ...orderData,
            microbiologyOrderDetail: {
              ...orderData.microbiologyOrderDetail,
              cultureMethodId: "7",
            },
            sampleOrderItems: {
              programId: "8",
              programCode: "MICROBIOLOGY",
              microbiologyProgramId: "8",
            },
          }}
          setOrderData={vi.fn()}
          samples={cultureSamples}
          isReadOnly
        />
      </IntlProvider>,
    );

    expect(
      await screen.findByRole("combobox", { name: "Program" }),
    ).toHaveValue("Microbiology");
    expect(screen.getByRole("combobox", { name: "Program" })).toBeDisabled();
    expect(screen.getByDisplayValue("Blood Culture Standard")).toBeDisabled();
  });

  it("clears the previous Program questionnaire when culture derives Microbiology", async () => {
    const previousProgramOrderData = {
      ...orderData,
      sampleOrderItems: {
        programId: "1",
        questionnaire: { id: "routine-questionnaire" },
        additionalQuestions: {
          resourceType: "QuestionnaireResponse",
          questionnaire: "Questionnaire/routine-questionnaire",
        },
      },
    };
    const setOrderData = vi.fn();

    render(
      <IntlProvider locale="en" messages={messages}>
        <ProgramSection
          orderData={previousProgramOrderData}
          setOrderData={setOrderData}
          samples={cultureSamples}
          isReadOnly={false}
        />
      </IntlProvider>,
    );

    await screen.findByRole("heading", {
      name: "Microbiology Program Details",
    });
    const deriveMicrobiology = setOrderData.mock.calls
      .map(([value]) => value)
      .filter((value) => typeof value === "function")
      .find(
        (value) =>
          value(previousProgramOrderData).sampleOrderItems?.programId === "8",
      );

    expect(deriveMicrobiology).toBeDefined();
    expect(
      deriveMicrobiology(previousProgramOrderData).sampleOrderItems,
    ).toEqual(
      expect.objectContaining({
        programId: "8",
        microbiologyProgramId: "8",
        questionnaire: null,
        additionalQuestions: null,
      }),
    );
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
    expect(screen.getByText("Blood Culture Standard")).toBeInTheDocument();
  });

  it("reflects a cleared canonical Program value after culture test removal", async () => {
    const props = {
      setOrderData: vi.fn(),
      samples: [],
      isReadOnly: false,
    };
    const { rerender } = render(
      <IntlProvider locale="en" messages={messages}>
        <ProgramSection
          {...props}
          orderData={{
            ...orderData,
            sampleOrderItems: { programId: "8" },
          }}
        />
      </IntlProvider>,
    );

    expect(
      await screen.findByRole("heading", {
        name: "Microbiology Program Details",
      }),
    ).toBeInTheDocument();

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <ProgramSection
          {...props}
          orderData={{ ...orderData, sampleOrderItems: { programId: "" } }}
        />
      </IntlProvider>,
    );

    expect(
      await screen.findByRole("combobox", { name: "Program" }),
    ).toHaveValue("");
    expect(
      screen.queryByRole("heading", { name: "Microbiology Program Details" }),
    ).not.toBeInTheDocument();
  });

  it("confirms before discarding manually entered microbiology details on Program change", async () => {
    const user = userEvent.setup();
    let latestOrderData;
    const initialOrderData = {
      ...orderData,
      microbiologyOrderDetail: {
        ...orderData.microbiologyOrderDetail,
        clinicalHistory: "Persistent fever",
      },
      sampleOrderItems: { programId: "8" },
    };
    const ControlledProgramSection = () => {
      const [currentOrderData, setCurrentOrderData] =
        React.useState(initialOrderData);
      latestOrderData = currentOrderData;
      return (
        <ProgramSection
          orderData={currentOrderData}
          setOrderData={setCurrentOrderData}
          samples={[]}
          isReadOnly={false}
        />
      );
    };
    render(
      <IntlProvider locale="en" messages={messages}>
        <ControlledProgramSection />
      </IntlProvider>,
    );

    const program = await screen.findByRole("combobox", { name: "Program" });
    await user.click(program);
    await user.clear(program);
    await user.type(program, "Routine");
    await user.click(
      await screen.findByRole("option", { name: "Routine Testing" }),
    );

    expect(
      screen.getByRole("dialog", { name: "Discard Microbiology details?" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "You have entered Microbiology details. Changing the Program will discard them.",
      ),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Discard details$/ }));

    expect(
      screen.queryByRole("heading", { name: "Microbiology Program Details" }),
    ).not.toBeInTheDocument();
    expect(latestOrderData.sampleOrderItems.programId).toBe("1");
    expect(latestOrderData.microbiologyOrderDetail.clinicalHistory).toBe("");
  });
});
