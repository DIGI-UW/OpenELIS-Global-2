import { act, renderHook } from "@testing-library/react-hooks";
import { describe, expect, it } from "vitest";
import useLocaleMessages from "../useLocaleMessages";

// The race case runs first, while no other locale than English is in memory:
// a bundle already loaded would resolve at once and there would be no race.
describe("useLocaleMessages", () => {
  it("applies only the newest switch when an earlier download lands later", async () => {
    const { result } = renderHook(() => useLocaleMessages("en"));

    let slow;
    let fast;
    await act(async () => {
      slow = result.current.switchLocale("de");
      fast = await result.current.switchLocale("en");
    });

    expect(fast.code).toBe("en");
    expect(await slow, "the overtaken switch applies nothing").toBeNull();
    expect(result.current.code).toBe("en");
    expect(result.current.messages["login.title"]).toBe("Login");
  });

  it("starts with English at once and marks a stored locale ready once its bundle is here", async () => {
    const { result, waitFor } = renderHook(() => useLocaleMessages("fr"));

    expect(result.current.code).toBe("fr");
    expect(result.current.ready).toBe(false);
    expect(result.current.messages["login.title"]).toBe("Login");

    await waitFor(() => expect(result.current.ready).toBe(true));
    expect(result.current.messages["login.title"]).toBe("Se connecter");
  });

  it("switches to a loaded locale and reports the canonical code", async () => {
    const { result } = renderHook(() => useLocaleMessages("en"));

    let applied;
    await act(async () => {
      applied = await result.current.switchLocale("fr_FR");
    });

    expect(applied.code).toBe("fr-FR");
    expect(result.current.code).toBe("fr-FR");
    expect(result.current.messages["login.title"]).toBe("Se connecter");
  });
});
