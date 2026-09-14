import { afterEach, describe, expect, it, vi } from "vitest";
import en from "../en.json";
import { resolveMessagesForLocale } from "../index";

/**
 * Catalogs are their own chunks now, so fetching one is a network operation
 * that can fail the way any chunk fetch can. Both callers of
 * resolveMessagesForLocale apply its result directly, so a rejection there
 * would abandon a language switch with nothing rendered about it.
 */
vi.mock("../fr.json", () => {
  throw new Error("Failed to fetch dynamically imported module");
});

describe("a catalog chunk that will not load", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("leaves the caller on English rather than rejecting", async () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => undefined);

    const { code, messages } = await resolveMessagesForLocale("fr");

    expect(code).toBe("fr");
    expect(messages).toStrictEqual(en);
    expect(consoleError).toHaveBeenCalled();
  });
});
