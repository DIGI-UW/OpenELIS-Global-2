import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { BrowserRouter } from "react-router-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import SampleBatchEntrySetup from "./SampleBatchEntrySetup";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../utils/Utils";

vi.mock("../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
}));

const renderSetup = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider
          value={{
            configurationProperties: {
              currentDateAsText: "01/01/2026",
              currentTimeAsText: "10:00",
            },
          }}
        >
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <SampleBatchEntrySetup />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>
    </BrowserRouter>,
  );

const box = (name) => screen.getByLabelText(name);
const next = () => screen.getByTestId("next-button-BatchOrderEntry");

async function chooseEidWithMethod(user) {
  await user.selectOptions(screen.getByLabelText(/Form:/), "EID");
  await user.selectOptions(screen.getByLabelText("Methods"), "Pre-Printed");
}

async function postedForm(user) {
  await user.click(next());
  const [, body] = postToOpenElisServerFullResponse.mock.calls[0];
  return JSON.parse(body);
}

describe("SampleBatchEntrySetup EID form", () => {
  beforeEach(() => {
    window.scrollTo = vi.fn();
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/SamplePatientEntry") {
        callback({ sampleOrderItems: { referringSiteList: [] } });
      } else {
        callback([]);
      }
    });
    postToOpenElisServerFullResponse.mockReset();
  });

  test("ticking a specimen does not select the DNA PCR test", async () => {
    const user = userEvent.setup();
    renderSetup();
    await chooseEidWithMethod(user);

    await user.click(box("Dry Tube"));
    await user.click(box("Dry Blood Spot"));

    expect(box("DNA PCR")).not.toBeChecked();
    expect(next()).toBeDisabled();
  });

  test("each box sets only its own flag and DNA PCR alone selects the test", async () => {
    const user = userEvent.setup();
    renderSetup();
    await chooseEidWithMethod(user);

    await user.click(box("Dry Tube"));
    await user.click(box("DNA PCR"));
    const form = await postedForm(user);

    expect(form._ProjectDataEID).toMatchObject({
      dryTubeTaken: true,
      dnaPCR: true,
    });
    expect(form._ProjectDataEID.dbsTaken).toBeFalsy();
    expect(form.tests.map((t) => t.value)).toEqual(["DNA PCR"]);
  });

  test("unticking a specimen keeps the DNA PCR test selected", async () => {
    const user = userEvent.setup();
    renderSetup();
    await chooseEidWithMethod(user);

    await user.click(box("Dry Tube"));
    await user.click(box("DNA PCR"));
    await user.click(box("Dry Tube"));

    expect(next()).toBeEnabled();
    const form = await postedForm(user);
    expect(form._ProjectDataEID).toMatchObject({
      dryTubeTaken: false,
      dnaPCR: true,
    });
    expect(form.tests.map((t) => t.value)).toEqual(["DNA PCR"]);
  });

  test("unticking DNA PCR clears the test", async () => {
    const user = userEvent.setup();
    renderSetup();
    await chooseEidWithMethod(user);

    await user.click(box("DNA PCR"));
    await user.click(box("Dry Blood Spot"));
    await user.click(box("DNA PCR"));

    expect(next()).toBeDisabled();
  });
});

describe("SampleBatchEntrySetup viral load form", () => {
  beforeEach(() => {
    window.scrollTo = vi.fn();
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/SamplePatientEntry") {
        callback({ sampleOrderItems: { referringSiteList: [] } });
      } else {
        callback([]);
      }
    });
    postToOpenElisServerFullResponse.mockReset();
  });

  test("ticking a specimen does not select the viral load test", async () => {
    const user = userEvent.setup();
    renderSetup();
    await user.selectOptions(screen.getByLabelText(/Form:/), "viralLoad");
    await user.selectOptions(screen.getByLabelText("Methods"), "Pre-Printed");

    await user.click(box("Dry Tube"));

    expect(next()).toBeDisabled();
  });
});
