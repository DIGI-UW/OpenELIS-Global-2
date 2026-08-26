import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import MicrobiologyOrderEntrySection from "../MicrobiologyOrderEntrySection";
import {
  formatAdmissionDateForApi,
  formatAdmissionDateForPicker,
} from "../MicrobiologyOrderDetailFields";
import messages from "../../../languages/en.json";
import { ConfigurationContext } from "../../layout/Layout";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../utils/Utils", () => ({ getFromOpenElisServer }));

const renderSection = (samples, orderFormValues, setOrderFormValues) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
      >
        <MicrobiologyOrderEntrySection
          samples={samples}
          orderFormValues={orderFormValues}
          setOrderFormValues={setOrderFormValues}
        />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

const baseForm = {
  microbiologyOrderDetail: {
    culturePurpose: "CLINICAL_DIAGNOSTIC",
    patientOrigin: "",
    numberOfSets: "",
    clinicalHistory: "",
    antibioticExposure: false,
    admissionDate: "",
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

  it("round-trips admission dates through configured display locales", () => {
    expect(formatAdmissionDateForPicker("2026-08-03", "en-US")).toBe(
      "08/03/2026",
    );
    expect(formatAdmissionDateForPicker("2026-08-03", "fr-FR")).toBe(
      "03/08/2026",
    );
    expect(formatAdmissionDateForApi("08/03/2026", "en-US")).toBe("2026-08-03");
    expect(formatAdmissionDateForApi("03/08/2026", "fr-FR")).toBe("2026-08-03");
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

  it("renders the derived protocol and five authoritative editable fields", async () => {
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
                  mediaDefaults: "BAP + CHOC",
                  incubationDefaults: "5 days at 35 C",
                  atmosphereDefaults: "aerobic + anaerobic",
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
    expect(screen.getByText("Blood Culture Standard")).toBeInTheDocument();
    expect(
      screen.getByText("BAP + CHOC - 5 days at 35 C - aerobic + anaerobic"),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("combobox", { name: "Culture Protocol" }),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Date of admission")).toBeEnabled();
    expect(
      screen.getByRole("radio", {
        name: "Clinical diagnosis or treatment",
      }),
    ).toBeChecked();
    expect(
      screen.getByRole("radio", { name: "Active screening or carriage" }),
    ).not.toBeChecked();
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
      screen.queryByLabelText(
        "Notify clinician immediately for a positive culture",
      ),
    ).not.toBeInTheDocument();

    await user.click(
      screen.getByLabelText(
        "Patient has recent antibiotic exposure (within 2 weeks)",
      ),
    );
    const exposureUpdate = setOrderFormValues.mock.calls.at(-1)[0];
    expect(exposureUpdate(baseForm)).toEqual(
      expect.objectContaining({
        microbiologyOrderDetail: expect.objectContaining({
          antibioticExposure: true,
        }),
      }),
    );
    await user.click(
      screen.getByRole("radio", { name: "Active screening or carriage" }),
    );

    const purposeUpdate = setOrderFormValues.mock.calls.at(-1)[0];
    expect(purposeUpdate(baseForm)).toEqual(
      expect.objectContaining({
        microbiologyOrderDetail: expect.objectContaining({
          culturePurpose: "ACTIVE_SCREENING",
        }),
      }),
    );
  });

  it("shows an unset protocol without adding a blocking input", () => {
    renderSection(
      [
        {
          tests: [
            {
              id: "2",
              name: "Culture without default method",
              cultureWorkflowType: "BACTERIOLOGY",
              methods: [],
            },
          ],
          sampleTypeName: "Urine",
        },
      ],
      baseForm,
      vi.fn(),
    );

    expect(
      screen.getByText("Not set - the bench will select a protocol"),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("combobox", { name: "Culture Protocol" }),
    ).not.toBeInTheDocument();
  });

  it("preserves the persisted protocol when the catalog default has changed", () => {
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
                  methodName: "Current Catalog Default",
                  isDefault: true,
                },
                {
                  methodId: "8",
                  methodName: "Protocol Ordered Earlier",
                  isDefault: false,
                },
              ],
            },
          ],
          sampleTypeName: "Blood",
        },
      ],
      {
        ...baseForm,
        microbiologyOrderDetail: {
          ...baseForm.microbiologyOrderDetail,
          cultureMethodId: "8",
        },
      },
      vi.fn(),
    );

    expect(screen.getByText("Protocol Ordered Earlier")).toBeInTheDocument();
    expect(
      screen.queryByText("Current Catalog Default"),
    ).not.toBeInTheDocument();
  });

  it("keeps admission date visible and disables it only for outpatients", async () => {
    const user = userEvent.setup();
    let latestForm;
    const ControlledSection = () => {
      const [form, setForm] = React.useState({
        ...baseForm,
        microbiologyOrderDetail: {
          ...baseForm.microbiologyOrderDetail,
          patientOrigin: "INPATIENT",
          admissionDate: "2026-08-03",
        },
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
        <ConfigurationContext.Provider
          value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
        >
          <ControlledSection />
        </ConfigurationContext.Provider>
      </IntlProvider>,
    );

    const admissionDate = screen.getByLabelText("Date of admission");
    expect(admissionDate).toBeEnabled();
    await screen.findByRole("option", { name: "Outpatient" });
    await user.selectOptions(
      screen.getByLabelText("Patient origin"),
      "OUTPATIENT",
    );
    expect(admissionDate).toBeDisabled();
    expect(latestForm.microbiologyOrderDetail.admissionDate).toBe("2026-08-03");
    expect(
      screen.getByText(
        "Outpatients are not admitted - recorded as community-origin.",
      ),
    ).toBeInTheDocument();
    await user.selectOptions(
      screen.getByLabelText("Patient origin"),
      "INPATIENT",
    );
    expect(admissionDate).toBeEnabled();
  });

  it("shows collection timing as read-only context when revisiting the order", () => {
    renderSection(
      [
        {
          sampleTypeName: "Blood",
          collectionDate: "2026-08-07",
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
      ],
      {
        ...baseForm,
        microbiologyOrderDetail: {
          ...baseForm.microbiologyOrderDetail,
          patientOrigin: "INPATIENT",
          admissionDate: "2026-08-03",
        },
      },
      vi.fn(),
    );

    expect(screen.getByText("Collection date: 08/07/2026")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Collected 4 days after admission - hospital-origin for surveillance.",
      ),
    ).toBeInTheDocument();
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
        <ConfigurationContext.Provider
          value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
        >
          <ControlledSection />
        </ConfigurationContext.Provider>
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
