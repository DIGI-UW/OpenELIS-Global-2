/**
 * An alert rule names a measurement. On a multi-component test the test name
 * does not: a rule about the numeric Ct Value and one about the coded PCR
 * Result beside it are different rules, and the editor has to let the user say
 * which. A single-component test has nothing to disambiguate, so it keeps the
 * simpler form it has always had.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AlertRuleModal from "./AlertRuleModal";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const COMPONENTS = [
  { id: "c-pcr", value: "PCR Result" },
  { id: "c-ct", value: "Ct Value" },
];
const SAMPLE_TYPES = [
  { id: "30", value: "Respiratory Swab" },
  { id: "31", value: "Saliva" },
];

const serve = ({ components = COMPONENTS, sampleTypes = SAMPLE_TYPES }) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.endsWith("/components")) cb(components);
    else if (url.endsWith("/sample-types")) cb(sampleTypes);
    else cb([]);
  });
};

const renderModal = (rule = null) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AlertRuleModal
        open={true}
        onClose={() => {}}
        testId="300"
        rule={rule}
        onSaved={() => {}}
      />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  postToOpenElisServer.mockImplementation((url, payload, cb) => cb(201));
});

describe("AlertRuleModal scope", () => {
  it("offers the component and specimen a multi-component test needs", async () => {
    serve({});
    renderModal();

    expect(
      await screen.findByText(
        messages["label.testCatalog.alerts.field.component"],
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(messages["label.testCatalog.alerts.field.sampleType"]),
    ).toBeInTheDocument();
  });

  it("stays simple when there is nothing to disambiguate", async () => {
    serve({ components: [{ id: "c-only", value: "Result" }], sampleTypes: [] });
    renderModal();

    await screen.findByLabelText(
      messages["label.testCatalog.alerts.field.name"],
    );
    expect(
      screen.queryByText(messages["label.testCatalog.alerts.field.component"]),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText(messages["label.testCatalog.alerts.field.sampleType"]),
    ).not.toBeInTheDocument();
  });

  it("sends the chosen component and specimen with the rule", async () => {
    serve({});
    renderModal();

    fireEvent.change(
      await screen.findByLabelText(
        messages["label.testCatalog.alerts.field.name"],
      ),
      { target: { value: "High Ct" } },
    );
    fireEvent.click(screen.getByText("Any component"));
    fireEvent.click(await screen.findByText("Ct Value"));
    fireEvent.click(screen.getByText("Any sample type"));
    fireEvent.click(await screen.findByText("Respiratory Swab"));
    fireEvent.click(screen.getByText(messages["button.save"]));

    const body = JSON.parse(postToOpenElisServer.mock.calls[0][1]);
    expect(body.componentId).toBe("c-ct");
    expect(body.sampleTypeId).toBe("30");
  });

  it("sends nothing for a rule left watching the whole test", async () => {
    serve({});
    renderModal();

    fireEvent.change(
      await screen.findByLabelText(
        messages["label.testCatalog.alerts.field.name"],
      ),
      { target: { value: "Any abnormal" } },
    );
    fireEvent.click(screen.getByText(messages["button.save"]));

    const body = JSON.parse(postToOpenElisServer.mock.calls[0][1]);
    expect(body.componentId).toBeNull();
    expect(body.sampleTypeId).toBeNull();
  });

  it("shows an existing rule's scope when editing it", async () => {
    serve({});
    renderModal({
      id: "r1",
      name: "High Ct",
      triggerType: "ALL",
      componentId: "c-ct",
      sampleTypeId: "31",
    });

    expect(await screen.findByText("Ct Value")).toBeInTheDocument();
    expect(screen.getByText("Saliva")).toBeInTheDocument();
  });
});
