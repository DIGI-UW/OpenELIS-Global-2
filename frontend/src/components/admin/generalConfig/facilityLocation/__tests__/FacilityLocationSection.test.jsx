import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../../utils/Utils";
import FacilityLocationSection from "../FacilityLocationSection";

vi.mock("../../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const manual = {
  mode: "MANUAL",
  autolocation: false,
  latitude: -1.2921,
  longitude: 36.8219,
  source: "properties",
  registryConfigured: true,
  registryUrl: "http://gofr:4000/fhir/DEFAULT",
  lastRegistrationSuccess: true,
  lastRegistrationMessage:
    "Lab A in http://registry/fhir at 0.3476,32.5825 (properties)",
};

const auto = {
  ...manual,
  mode: "AUTO",
  autolocation: true,
  latitude: 0.3476,
  longitude: 32.5825,
  source: "ip",
};

const renderWith = (status) => {
  getFromOpenElisServer.mockImplementation((url, callback) => callback(status));
  return render(
    <IntlProvider locale="en" messages={messages}>
      <FacilityLocationSection />
    </IntlProvider>,
  );
};

describe("FacilityLocationSection", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    postToOpenElisServerJsonResponse.mockReset();
  });

  afterEach(() => {
    delete global.navigator.geolocation;
  });

  it("shows the manual coordinates and offers no device location", async () => {
    renderWith(manual);

    expect(await screen.findByText("Manual")).toBeInTheDocument();
    expect(
      screen.getByTestId("facility-location-coordinates"),
    ).toHaveTextContent("-1.2921, 36.8219");
    expect(
      screen.getByText("The latitude and longitude properties"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("http://gofr:4000/fhir/DEFAULT"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("facility-location-registration"),
    ).toHaveTextContent("Registered");
    expect(
      screen.queryByText("Use this device's location"),
    ).not.toBeInTheDocument();
  });

  it("sends this device's position in automatic mode and shows the result", async () => {
    global.navigator.geolocation = {
      getCurrentPosition: (ok) =>
        ok({ coords: { latitude: 0.0512, longitude: 32.4635 } }),
    };
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({
        ...auto,
        latitude: 0.0512,
        longitude: 32.4635,
        source: "browser",
      }),
    );
    renderWith(auto);

    expect(
      await screen.findByText("This server's public address (approximate)"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByText("Use this device's location"));

    await waitFor(() =>
      expect(screen.getByText("An administrator's device")).toBeInTheDocument(),
    );
    expect(
      JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]),
    ).toEqual({
      latitude: 0.0512,
      longitude: 32.4635,
    });
    expect(
      screen.getByTestId("facility-location-coordinates"),
    ).toHaveTextContent("0.0512, 32.4635");
  });

  it("says why when the browser refuses to share its location", async () => {
    global.navigator.geolocation = {
      getCurrentPosition: (ok, err) => err({ code: 1 }),
    };
    renderWith(auto);

    fireEvent.click(await screen.findByText("Use this device's location"));

    expect(
      await screen.findByText(/was not allowed to share its location/),
    ).toBeInTheDocument();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  it("shows the server's reason when it refuses the position", async () => {
    global.navigator.geolocation = {
      getCurrentPosition: (ok) => ok({ coords: { latitude: 1, longitude: 2 } }),
    };
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({
        status: 409,
        message: "The facility location is in manual mode",
      }),
    );
    renderWith(auto);

    fireEvent.click(await screen.findByText("Use this device's location"));

    expect(
      await screen.findByText("The facility location is in manual mode"),
    ).toBeInTheDocument();
  });

  it("notes that the properties are ignored in automatic mode", async () => {
    renderWith({ ...auto, manualCoordinatesIgnored: true });

    expect(
      await screen.findByText(
        "Automatic mode is on, so the latitude and longitude properties are ignored.",
      ),
    ).toBeInTheDocument();
  });
});
