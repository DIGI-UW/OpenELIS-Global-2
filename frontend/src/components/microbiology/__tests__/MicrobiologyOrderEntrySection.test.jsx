import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import MicrobiologyOrderEntrySection from "../MicrobiologyOrderEntrySection";
import messages from "../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../utils/Utils", () => ({ getFromOpenElisServer }));

const renderSection = (samples, orderFormValues, setOrderFormValues) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MicrobiologyOrderEntrySection
        samples={samples}
        orderFormValues={orderFormValues}
        setOrderFormValues={setOrderFormValues}
      />
    </IntlProvider>,
  );

const baseForm = {
  microbiologyOrderDetail: {
    patientOrigin: "",
    numberOfSets: "",
    clinicalHistory: "",
    antibioticExposure: false,
    criticalNotificationPreference: null,
    cultureMethodId: "",
  },
};

describe("MicrobiologyOrderEntrySection", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/microbiology/reference/patient-origins")) {
        callback({
          defaultCode: url.includes("organizationId=27") ? "INPATIENT" : null,
          options: [
            { code: "INPATIENT", label: "Inpatient", whonetCode: "INP" },
            { code: "OUTPATIENT", label: "Outpatient", whonetCode: "OUT" },
            { code: "INTENSIVE_CARE", label: "ICU", whonetCode: "ICU" },
            { code: "EMERGENCY", label: "Emergency", whonetCode: "EME" },
            {
              code: "LONG_TERM_CARE",
              label: "Long-term Care",
              whonetCode: "LTC",
            },
            { code: "UNKNOWN", label: "Unknown", whonetCode: "UNK" },
          ],
        });
      }
    });
  });

  it("stays hidden when no selected test starts a culture workflow", () => {
    renderSection(
      [{ tests: [{ id: "1", name: "Complete blood count" }] }],
      baseForm,
      vi.fn(),
    );

    expect(
      screen.queryByRole("heading", { name: "Microbiology order details" }),
    ).not.toBeInTheDocument();
  });

  it("renders the authoritative controls and captures binary choices", async () => {
    const user = userEvent.setup();
    const setOrderFormValues = vi.fn();
    renderSection(
      [
        {
          tests: [
            {
              id: "2",
              name: "Blood culture",
              cultureWorkflowType: "BACTERIOLOGY",
              methods: [
                {
                  methodId: "7",
                  methodName: "Blood Culture Standard",
                  methodCode: "BCSTD",
                  isDefault: true,
                },
                {
                  methodId: "8",
                  methodName: "Blood Culture Alternate",
                  methodCode: "BCALT",
                  isDefault: false,
                },
              ],
            },
          ],
          sampleTypeName: "Blood",
        },
      ],
      baseForm,
      setOrderFormValues,
    );

    expect(
      screen.getByRole("heading", { name: "Microbiology Program Details" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Bacteriology")).toBeInTheDocument();
    expect(
      screen.getByRole("combobox", { name: "Culture Method" }),
    ).toHaveValue("Blood Culture Standard");
    expect(
      screen.getByRole("spinbutton", { name: "Number of sets" }),
    ).toHaveValue(2);
    expect(
      screen.getByRole("spinbutton", { name: "Number of sets" }),
    ).toHaveAttribute("max", "10");
    await screen.findByRole("option", { name: "Emergency" });
    await user.selectOptions(
      screen.getByLabelText("Patient origin"),
      "EMERGENCY",
    );
    expect(
      screen.getByLabelText(
        "Patient has recent antibiotic exposure (within 2 weeks)",
      ),
    ).not.toBeChecked();
    expect(
      screen.getByLabelText(
        "Notify clinician immediately for a positive culture",
      ),
    ).toBeChecked();

    await user.click(
      screen.getByLabelText(
        "Patient has recent antibiotic exposure (within 2 weeks)",
      ),
    );

    const update = setOrderFormValues.mock.calls.at(-1)[0];
    expect(update(baseForm)).toEqual(
      expect.objectContaining({
        microbiologyOrderDetail: expect.objectContaining({
          antibioticExposure: true,
        }),
      }),
    );
  });

  it("loads deployment origins and applies a configured requesting-unit default", async () => {
    let latestForm;
    const ControlledSection = () => {
      const [form, setForm] = React.useState({
        ...baseForm,
        sampleOrderItems: { referringSiteDepartmentId: "27" },
      });
      latestForm = form;
      return (
        <MicrobiologyOrderEntrySection
          samples={[
            {
              sampleTypeName: "Blood",
              tests: [
                {
                  id: "2",
                  cultureWorkflowType: "BACTERIOLOGY",
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
          orderFormValues={form}
          setOrderFormValues={setForm}
        />
      );
    };

    render(
      <IntlProvider locale="en" messages={messages}>
        <ControlledSection />
      </IntlProvider>,
    );

    expect(
      await screen.findByRole("option", { name: "Long-term Care" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Unknown" })).toBeInTheDocument();
    expect(screen.getByLabelText("Patient origin")).toHaveValue("INPATIENT");
    expect(latestForm.microbiologyOrderDetail.patientOrigin).toBe("INPATIENT");
  });
});
