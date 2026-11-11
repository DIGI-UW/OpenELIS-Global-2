import { renderHook } from "@testing-library/react-hooks";
import useUnsavedChangesGuard from "./useUnsavedChangesGuard";
import { vi } from "vitest";

// These cover the browser half of the warning only. Moving between screens
// of the application fires no beforeunload at all, so nothing here can say
// anything about it; that half is the router Prompt, and it is tested where
// it is rendered, in CaseActionBar.
describe("useUnsavedChangesGuard", () => {
  it("registers a beforeunload listener while dirty, and that listener defaults-prevents the event", () => {
    const addSpy = vi.spyOn(window, "addEventListener");

    renderHook(() => useUnsavedChangesGuard(true));

    expect(addSpy).toHaveBeenCalledWith("beforeunload", expect.any(Function));

    const event = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);

    addSpy.mockRestore();
  });

  it("registers no beforeunload listener while the form is clean", () => {
    const addSpy = vi.spyOn(window, "addEventListener");

    renderHook(() => useUnsavedChangesGuard(false));

    expect(addSpy).not.toHaveBeenCalledWith(
      "beforeunload",
      expect.any(Function),
    );

    addSpy.mockRestore();
  });

  it("removes the listener when the component unmounts", () => {
    const addSpy = vi.spyOn(window, "addEventListener");
    const removeSpy = vi.spyOn(window, "removeEventListener");

    const { unmount } = renderHook(() => useUnsavedChangesGuard(true));
    const [, handler] = addSpy.mock.calls.find(
      ([eventName]) => eventName === "beforeunload",
    );

    unmount();

    expect(removeSpy).toHaveBeenCalledWith("beforeunload", handler);

    addSpy.mockRestore();
    removeSpy.mockRestore();
  });

  it("removes the listener when dirty goes back to false, so the warning does not outlive the save", () => {
    const addSpy = vi.spyOn(window, "addEventListener");
    const removeSpy = vi.spyOn(window, "removeEventListener");

    const { rerender } = renderHook(
      ({ dirty }) => useUnsavedChangesGuard(dirty),
      { initialProps: { dirty: true } },
    );
    const [, handler] = addSpy.mock.calls.find(
      ([eventName]) => eventName === "beforeunload",
    );

    rerender({ dirty: false });

    expect(removeSpy).toHaveBeenCalledWith("beforeunload", handler);

    const event = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(event);
    expect(event.defaultPrevented).toBe(false);

    addSpy.mockRestore();
    removeSpy.mockRestore();
  });
});
