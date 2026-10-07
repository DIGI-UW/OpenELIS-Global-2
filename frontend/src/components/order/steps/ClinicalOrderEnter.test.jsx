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
      seedOrderData: vi.fn(),
      samples: [
        {
          sampleTypeId: "blood",
          tests: [
            {
              id: "test-1",
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

vi.mock("../../layout/Layout", () => ({
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

const layoutProps = vi.fn();
vi.mock("../OrderWorkflowLayout", () => ({
  default: (props) => {
    layoutProps(props);
    return (
      <div>
        {props.children}
        <button disabled={!props.canSave} onClick={props.onSave}>
          Save and exit
        </button>
        <button disabled={!props.canProceed} onClick={props.onSaveAndNext}>
          Save and next
        </button>
        <ul data-testid="to-continue">
          {(props.toContinue || []).map((item) => (
            <li key={item.id}>{item.label}</li>
          ))}
        </ul>
      </div>
    );
  },
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
import { getFromOpenElisServer } from "../../utils/Utils";

describe("ClinicalOrderEnter", () => {
  beforeEach(() => {
    programSectionProps.mockClear();
  });

  // OGC-1266 FR-A7: the entry step has two levels of required. Save and
  // exit needs the save level; Save and next needs the complete level too,
  // and lists what is still missing with a link to each field.
  it("opens Save and exit on the save level and lists the rest to continue", () => {
    configurationValue.configurationProperties = { REQUESTER_REQUIRED: "true" };
    layoutProps.mockClear();
    render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

    expect(screen.getByRole("button", { name: "Save and exit" })).toBeEnabled();
    expect(
      screen.getByRole("button", { name: "Save and next" }),
    ).toBeDisabled();
    expect(screen.getByTestId("to-continue")).toHaveTextContent(
      messages["order.continue.item.provider"],
    );
    expect(layoutProps).toHaveBeenLastCalledWith(
      expect.objectContaining({
        toContinue: [
          expect.objectContaining({
            id: "order.continue.item.provider",
            targetId: "providerName",
          }),
        ],
      }),
    );
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
  it("does not hold either save on the site marker setting", () => {
    configurationValue.configurationProperties = {
      SampleEntryReferralSiteNameRequired: "true",
    };
    renderEnter();

    expect(screen.getByRole("button", { name: "Save and exit" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Save and next" })).toBeEnabled();
    expect(screen.getByTestId("to-continue")).toBeEmptyDOMElement();
  });

  it("holds Save and next, not the save, when the deployment requires a requester", () => {
    configurationValue.configurationProperties = { REQUESTER_REQUIRED: "true" };
    renderEnter();

    expect(screen.getByRole("button", { name: "Save and exit" })).toBeEnabled();
    expect(
      screen.getByRole("button", { name: "Save and next" }),
    ).toBeDisabled();
  });

  it("leaves both saves open when the deployment requires neither", () => {
    renderEnter();

    expect(screen.getByRole("button", { name: "Save and exit" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Save and next" })).toBeEnabled();
    expect(screen.getByTestId("to-continue")).toBeEmptyDOMElement();
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
    expect(
      screen.getByRole("button", { name: "Save and exit" }),
    ).toBeDisabled();
    expect(screen.getByTestId("to-continue")).toHaveTextContent(
      messages["order.continue.item.patient"],
    );
  });
});

// Found on the live walk: an untouched new order said "Unsaved changes" and
// asked before leaving, because the defaults the form sets for itself (the
// workflow type, the generated lab number) were recorded as the user's edits.
describe("ClinicalOrderEnter defaults on a new order", () => {
  beforeEach(() => {
    configurationValue.configurationProperties = {};
    orderContextValue.setOrderData = vi.fn();
    orderContextValue.seedOrderData = vi.fn();
    orderContextValue.labNumber = null;
    orderContextValue.orderData = {
      patientProperties: {},
      sampleOrderItems: {},
    };
  });

  afterEach(() => {
    orderContextValue.labNumber = "LAB-1";
    getFromOpenElisServer.mockReset();
  });

  it("seeds the workflow type and the generated lab number without marking the order dirty", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("SampleEntryGenerateScanProvider")) {
        callback({ status: 200, body: "DEV01260000000000001" });
      }
    });
    render(
      <IntlProvider locale="en" messages={messages}>
        <ClinicalOrderEnter />
      </IntlProvider>,
    );

    expect(orderContextValue.seedOrderData).toHaveBeenCalled();
    expect(orderContextValue.setOrderData).not.toHaveBeenCalled();
    const seeded = orderContextValue.seedOrderData.mock.calls
      .map(([value]) => value)
      .filter((value) => typeof value === "function")
      .reduce((data, update) => update(data), orderContextValue.orderData);
    expect(seeded.sampleOrderItems.environmentalFields.workflowType).toBe(
      "clinical",
    );
  });
});

describe("ClinicalOrderEnter EQA pre-set", () => {
  beforeEach(() => {
    configurationValue.configurationProperties = {};
    orderContextValue.seedOrderData = vi.fn();
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

    const applied = orderContextValue.seedOrderData.mock.calls
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

    const applied = orderContextValue.seedOrderData.mock.calls
      .map(([value]) => value)
      .filter((value) => typeof value === "function")
      .map((value) => value(orderContextValue.orderData))
      .find((next) => next.sampleOrderItems?.isEQASample);

    expect(applied).toBeUndefined();
  });
});
