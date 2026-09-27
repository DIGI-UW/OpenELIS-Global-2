import { describe, expect, it } from "vitest";
import {
  DEFAULT_SERVER_PAGE_SIZE,
  serverPageSizeFrom,
  startingRecNoFor,
} from "./offsetPaging";

describe("offsetPaging", () => {
  it("reads the server's page size off a full page and keeps the default on the last one", () => {
    expect(serverPageSizeFrom("1", "20", "45")).toBe(20);
    expect(serverPageSizeFrom("21", "40", "45")).toBe(20);
    expect(serverPageSizeFrom("41", "45", "45")).toBe(DEFAULT_SERVER_PAGE_SIZE);
    expect(serverPageSizeFrom("1", "7", "7")).toBe(DEFAULT_SERVER_PAGE_SIZE);
    expect(serverPageSizeFrom("", "", "")).toBe(DEFAULT_SERVER_PAGE_SIZE);
    expect(serverPageSizeFrom("1", "25", "80")).toBe(25);
    expect(serverPageSizeFrom("41", "45", "45", 25)).toBe(25);
  });

  it("names the first record of the server page Carbon asks for", () => {
    expect(startingRecNoFor(1, 20)).toBe(1);
    expect(startingRecNoFor(2, 20)).toBe(21);
    expect(startingRecNoFor(5, 20)).toBe(81);
    expect(startingRecNoFor(0, 20)).toBe(1);
  });
});
