import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  forgetRequester,
  readRememberedRequester,
  rememberRequester,
} from "./rememberedRequester";

describe("remembered site and requester", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("carries only the site and requester fields that are set", () => {
    rememberRequester({
      referringSiteId: "7",
      referringSiteName: "Kisumu Clinic",
      providerPersonId: "31",
      providerEmail: "",
      labNo: "LAB-1",
      patientPK: "99",
    });

    expect(readRememberedRequester()).toEqual({
      referringSiteId: "7",
      referringSiteName: "Kisumu Clinic",
      providerPersonId: "31",
    });
  });

  it("remembers nothing when no site or requester was chosen", () => {
    rememberRequester({ labNo: "LAB-1" });
    expect(readRememberedRequester()).toBeNull();
  });

  it("forgets on request", () => {
    rememberRequester({ referringSiteId: "7" });
    forgetRequester();
    expect(readRememberedRequester()).toBeNull();
  });

  it("survives a browser that refuses storage", () => {
    const getItem = vi
      .spyOn(Storage.prototype, "getItem")
      .mockImplementation(() => {
        throw new Error("denied");
      });
    const setItem = vi
      .spyOn(Storage.prototype, "setItem")
      .mockImplementation(() => {
        throw new Error("denied");
      });

    expect(() => rememberRequester({ referringSiteId: "7" })).not.toThrow();
    expect(readRememberedRequester()).toBeNull();

    getItem.mockRestore();
    setItem.mockRestore();
  });
});
