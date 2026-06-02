import {
  isDeviceSelectionRequired,
  validateRoundCapacity,
} from "./biorepositoryQcScopeHelpers";

describe("biorepositoryQcScopeHelpers", () => {
  test("isDeviceSelectionRequired when multiple devices and not admin", () => {
    expect(isDeviceSelectionRequired(2, false)).toBe(true);
    expect(isDeviceSelectionRequired(2, true)).toBe(false);
    expect(isDeviceSelectionRequired(1, false)).toBe(false);
  });

  test("validateRoundCapacity blocks insufficient boxes and samples", () => {
    expect(validateRoundCapacity(10, 3, 5, 8).ok).toBe(false);
    expect(validateRoundCapacity(10, 3, 5, 8).reason).toBe("boxes");

    expect(validateRoundCapacity(2, 3, 10, 5).ok).toBe(false);
    expect(validateRoundCapacity(2, 3, 10, 5).reason).toBe("samples");

    expect(validateRoundCapacity(2, 3, 10, 20).ok).toBe(true);
  });
});
