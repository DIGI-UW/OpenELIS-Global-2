/**
 * While the site configuration has not loaded, Modify Order must settle. A
 * fresh default object on every render re-ran the validation effect forever,
 * which kept the page too busy to leave it (the OGC-1192 environmental
 * redirect then never completed).
 */
import React from "react";
import { act, render } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  },
}));

vi.mock("../utils/Utils", async (importOriginal) => ({
  ...utilsMock,
  resolveApiErrorMessage: (await importOriginal()).resolveApiErrorMessage,
}));

vi.mock("../layout/Layout", () => ({
  ConfigurationContext: React.createContext({}),
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
}));

vi.mock("../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

vi.mock("../addOrder/AddOrder", () => ({ default: () => <div /> }));
vi.mock("./EditSample", () => ({ default: () => <div /> }));
vi.mock("./EditOrderEntryAdditionalQuestions", () => ({
  default: () => <div />,
}));
vi.mock("../addOrder/OrderSuccessMessage", () => ({ default: () => <div /> }));
vi.mock("../common/PatientHeader", () => ({ default: () => <div /> }));
vi.mock("../common/PageBreadCrumb", () => ({ default: () => <div /> }));
vi.mock("../addOrder/Index", () => ({
  sampleObject: { sampleTypeId: "", tests: [], sampleXML: {} },
}));

import ModifyOrder from "./ModifyOrder";

const settle = () =>
  act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 150));
  });

test("Modify Order stops validating once nothing changes, before the configuration loads", async () => {
  window.scrollTo = vi.fn();
  utilsMock.getFromOpenElisServer.mockImplementation(() => {});
  const debug = vi.spyOn(console, "debug").mockImplementation(() => {});
  const validations = () =>
    debug.mock.calls.filter(([label]) =>
      /^(Valid Data|Validation Errors):$/.test(label),
    ).length;

  render(
    <IntlProvider locale="en" messages={messages}>
      <ModifyOrder />
    </IntlProvider>,
  );
  await settle();
  const afterFirstWait = validations();
  await settle();

  expect(afterFirstWait).toBeLessThan(5);
  expect(validations()).toBe(afterFirstWait);
  debug.mockRestore();
});
