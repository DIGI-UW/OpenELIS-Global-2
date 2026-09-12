import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

const { orderContextValue, programSectionProps, configurationValue } =
  vi.hoisted(() => ({
    configurationValue: { configurationProperties: {} },
    orderContextValue: {
      orderData: {
        patientProperties: { lastName: "Ada" },
        sampleOrderItems: {
          environmentalFields: { workflowType: "clinical" },
        },
      },
      setOrderData: vi.fn(),
      samples: [
        {
          sampleTypeId: "blood",
          tests: [
            {
              id: "culture-test",
              cultureWorkflowType: "BACTERIOLOGY",
            },
          ],
        },
      ],
      setSamples: vi.fn(),
      labNumber: "LAB-1",
      isSubmitting: false,
      saveStatus: "saved",
      error: null,
      fieldErrors: {},
      saveOrderEntry: vi.fn(),
      markStepComplete: vi.fn(),
      isReadOnly: false,
      isEditMode: false,
      resetOrder: vi.fn(),
    },
    programSectionProps: vi.fn(),
  }));

const currentLocation = { pathname: "/order/clinical/enter", search: "" };
vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push: vi.fn(), replace: vi.fn() }),
  useLocation: () => currentLocation,
}));

vi.mock("../OrderContext", () => ({
  useOrderContext: () => orderContextValue,
  SaveStatus: {
    SAVED: "saved",
    SAVING: "saving",
    ERROR: "error",
    UNSAVED: "unsaved",
  },
}));

vi.mock("../../layout/contexts", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext(configurationValue),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { error: "error", success: "success" },
}));

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../OrderWorkflowLayout", () => ({
  default: ({ children, extraButtons }) => (
    <div>
      {children}
      {extraButtons}
    </div>
  ),
}));

vi.mock("./sections/PatientSearchSection", () => ({
  default: () => null,
}));

vi.mock("./sections/ProgramSection", () => ({
  default: (props) => {
    programSectionProps(props);
    return <div data-testid="program-section" />;
  },
}));

vi.mock("./sections/ClinicalInfoSection", () => ({
  default: () => null,
}));

const requesterSectionProps = vi.fn();
vi.mock("./sections/RequesterSection", () => ({
  default: (props) => {
    requesterSectionProps(props);
    return null;
  },
}));

vi.mock("./sections/SampleTestSection", () => ({
  default: () => null,
}));

import ClinicalOrderEnter from "./ClinicalOrderEnter";

describe("ClinicalOrderEnter", () => {
  beforeEach(() => {
    programSectionProps.mockClear();
  });

  it("does not offer a second draft save while one is in flight", () => {
    orderContextValue.isSubmitting = true;
    const { unmount } = render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

    expect(screen.getByRole("button", { name: "Save Draft" })).toBeDisabled();
    unmount();

    orderContextValue.isSubmitting = false;
    render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

    expect(screen.getByRole("button", { name: "Save Draft" })).toBeEnabled();
  });

  it("marks the lab number and lists the rest when the server blocks the save", () => {
    orderContextValue.saveStatus = "error";
    orderContextValue.error = "sampleOrderItems.labNo: must not be blank";
    orderContextValue.fieldErrors = {
      "sampleOrderItems.labNo": "must not be blank",
      "sampleOrderItems.receivedDateForDisplay": "invalid date",
    };
    render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

    expect(screen.getByRole("textbox", { name: /Lab Number/ })).toBeInvalid();
    expect(screen.getByText("must not be blank")).toBeInTheDocument();
    expect(screen.getByText("The order was not saved")).toBeInTheDocument();
    expect(
      screen.getByText("sampleOrderItems.receivedDateForDisplay: invalid date"),
    ).toBeInTheDocument();

    orderContextValue.saveStatus = "saved";
    orderContextValue.error = null;
    orderContextValue.fieldErrors = {};
  });

  it("shares selected samples with the Program section", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

    expect(screen.getByTestId("program-section")).toBeInTheDocument();
    expect(programSectionProps).toHaveBeenCalledWith(
      expect.objectContaining({ samples: orderContextValue.samples }),
    );
  });
});

describe("ClinicalOrderEnter required-field configuration", () => {
  const renderEnter = () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

  beforeEach(() => {
    requesterSectionProps.mockClear();
    configurationValue.configurationProperties = {};
    orderContextValue.isSubmitting = false;
    orderContextValue.saveStatus = "saved";
    orderContextValue.error = null;
    orderContextValue.fieldErrors = {};
    orderContextValue.orderData = {
      patientProperties: { lastName: "Ada" },
      sampleOrderItems: { environmentalFields: { workflowType: "clinical" } },
    };
  });

  // OGC-1201 K: the provider never appeared in the save gate, and
  // ClinicalOrderEnter never read the configuration at all.
  //
  // The site setting only ever marked the field — that is all it does in the
  // legacy screen it was written for, and no server validation reads it — so
  // it drives the asterisk, not the gate. Turning it into a gate blocks every
  // order on the profiles that set it, which is not what it has ever meant.
  it("does not block the save on the site marker setting", () => {
    configurationValue.configurationProperties = {
      SampleEntryReferralSiteNameRequired: "true",
    };
    renderEnter();

    expect(screen.getByRole("button", { name: "Save Draft" })).toBeEnabled();
  });

  it("blocks the save when the deployment requires a requester", () => {
    configurationValue.configurationProperties = { REQUESTER_REQUIRED: "true" };
    renderEnter();

    expect(screen.getByRole("button", { name: "Save Draft" })).toBeDisabled();
  });

  it("leaves the save open when the deployment requires neither", () => {
    renderEnter();

    expect(screen.getByRole("button", { name: "Save Draft" })).toBeEnabled();
  });

  it("marks the required fields so the user can see them", () => {
    configurationValue.configurationProperties = {
      SampleEntryReferralSiteNameRequired: "true",
      REQUESTER_REQUIRED: "true",
    };
    renderEnter();

    expect(requesterSectionProps).toHaveBeenCalledWith(
      expect.objectContaining({ siteRequired: true, providerRequired: true }),
    );
  });

  // PatientRequired is TRUE in DefaultFormFields and every shipped profile,
  // so an absent value must not read as "patient optional".
  it("requires a patient unless the deployment turns it off", () => {
    orderContextValue.orderData = {
      patientProperties: {},
      sampleOrderItems: { environmentalFields: { workflowType: "clinical" } },
    };
    renderEnter();
    expect(screen.getByRole("button", { name: "Save Draft" })).toBeDisabled();
  });
});

describe("ClinicalOrderEnter EQA pre-set", () => {
  beforeEach(() => {
    configurationValue.configurationProperties = {};
    orderContextValue.setOrderData = vi.fn();
    orderContextValue.orderData = {
      patientProperties: {},
      sampleOrderItems: { environmentalFields: { workflowType: "clinical" } },
    };
  });

  afterEach(() => {
    currentLocation.search = "";
  });

  // OGC-1201 W: the EQA worklist used to push at the legacy screen with
  // ?isEQA=true. EQA is a control on the shared form now, and a caller can
  // pre-set it — which is what keeps the override a recorded decision rather
  // than something only a human click can produce.
  it("arrives with EQA and no-patient already declared", () => {
    currentLocation.search = "?eqa=true";
    render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

    const applied = orderContextValue.setOrderData.mock.calls
      .map(([value]) => value)
      .filter((value) => typeof value === "function")
      .map((value) => value(orderContextValue.orderData))
      .find((next) => next.sampleOrderItems?.isEQASample);

    expect(applied).toBeDefined();
    expect(applied.sampleOrderItems).toEqual(
      expect.objectContaining({
        isEQASample: true,
        noPatientOverride: true,
        noPatientReasonCode: "EQA",
      }),
    );
  });

  it("leaves an ordinary new order alone", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

    const applied = orderContextValue.setOrderData.mock.calls
      .map(([value]) => value)
      .filter((value) => typeof value === "function")
      .map((value) => value(orderContextValue.orderData))
      .find((next) => next.sampleOrderItems?.isEQASample);

    expect(applied).toBeUndefined();
  });
});
