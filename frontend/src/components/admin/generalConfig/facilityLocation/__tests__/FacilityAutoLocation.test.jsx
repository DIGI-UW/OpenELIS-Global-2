import React from "react";
import { render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import UserSessionDetailsContext from "../../../../../UserSessionDetailsContext";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../../utils/Utils";
import FacilityAutoLocation, { ASKED_KEY } from "../FacilityAutoLocation";

vi.mock("../../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const admin = { authenticated: true, roles: ["Global Administrator"] };
const getCurrentPosition = vi.fn((ok) =>
  ok({ coords: { latitude: 0.0512, longitude: 32.4635 } }),
);

const renderFor = (session, status) => {
  getFromOpenElisServer.mockImplementation((url, callback) => callback(status));
  return render(
    <UserSessionDetailsContext.Provider value={{ userSessionDetails: session }}>
      <FacilityAutoLocation />
    </UserSessionDetailsContext.Provider>,
  );
};

describe("FacilityAutoLocation", () => {
  beforeEach(() => {
    window.localStorage.removeItem(ASKED_KEY);
    getFromOpenElisServer.mockReset();
    postToOpenElisServerJsonResponse.mockReset();
    getCurrentPosition.mockClear();
    global.navigator.geolocation = { getCurrentPosition };
  });

  afterEach(() => {
    delete global.navigator.geolocation;
  });

  it("asks an administrator's browser once in automatic mode and sends the position", async () => {
    renderFor(admin, { autolocation: true, source: "ip" });

    await waitFor(() =>
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1),
    );
    expect(
      JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]),
    ).toEqual({
      latitude: 0.0512,
      longitude: 32.4635,
    });

    renderFor(admin, { autolocation: true, source: "ip" });
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(getCurrentPosition).toHaveBeenCalledTimes(1);
  });

  it("does not ask in manual mode", async () => {
    renderFor(admin, { autolocation: false, source: "properties" });
    await waitFor(() => expect(getFromOpenElisServer).toHaveBeenCalled());
    expect(getCurrentPosition).not.toHaveBeenCalled();
  });

  it("does not ask again once a browser position is stored", async () => {
    renderFor(admin, { autolocation: true, source: "browser" });
    await waitFor(() => expect(getFromOpenElisServer).toHaveBeenCalled());
    expect(getCurrentPosition).not.toHaveBeenCalled();
  });

  it("does nothing for a user who is not an administrator", async () => {
    renderFor(
      { authenticated: true, roles: ["Results"] },
      { autolocation: true, source: "ip" },
    );
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(getFromOpenElisServer).not.toHaveBeenCalled();
    expect(getCurrentPosition).not.toHaveBeenCalled();
  });
});
