import { isStorageAssignmentSuccess } from "./storageAssignmentResponse";

describe("isStorageAssignmentSuccess", () => {
  describe("explicit success flag (authoritative)", () => {
    it("returns true when body.success === true regardless of other fields", () => {
      expect(isStorageAssignmentSuccess({ success: true })).toBe(true);
      expect(
        isStorageAssignmentSuccess({
          success: true,
          error: "ignored when success=true",
        }),
      ).toBe(true);
      expect(
        isStorageAssignmentSuccess({
          success: true,
          message: "ignored when success=true",
        }),
      ).toBe(true);
    });

    it("returns false when body.success === false regardless of other fields", () => {
      expect(isStorageAssignmentSuccess({ success: false })).toBe(false);
      expect(
        isStorageAssignmentSuccess({
          success: false,
          assignmentId: "still-false",
        }),
      ).toBe(false);
      expect(
        isStorageAssignmentSuccess({
          success: false,
          newHierarchicalPath: "still-false",
        }),
      ).toBe(false);
    });

    it("does not treat truthy non-boolean success as explicit positive", () => {
      // Only literal true short-circuits. "true" / 1 fall back to heuristic to
      // avoid accidental positives from string/numeric backend drift.
      expect(isStorageAssignmentSuccess({ success: "true" })).toBe(false);
      expect(isStorageAssignmentSuccess({ success: 1 })).toBe(false);
    });
  });

  describe("legacy heuristic fallback (no explicit success field)", () => {
    it("returns true for an assign response with assignmentId and no error/message", () => {
      expect(
        isStorageAssignmentSuccess({
          assignmentId: "A-1",
          hierarchicalPath: "Lab > Room > Freezer",
        }),
      ).toBe(true);
    });

    it("returns true for a move response with movementId and newHierarchicalPath", () => {
      expect(
        isStorageAssignmentSuccess({
          movementId: "M-1",
          newHierarchicalPath: "Lab > Room > Freezer B",
        }),
      ).toBe(true);
    });

    it("returns false when body has an error field", () => {
      expect(
        isStorageAssignmentSuccess({
          error: "capacity exceeded",
          assignmentId: "A-1",
        }),
      ).toBe(false);
    });

    it("returns false when body has a message field", () => {
      expect(
        isStorageAssignmentSuccess({
          message: "Sample is already assigned",
        }),
      ).toBe(false);
    });

    it("returns false when body has no recognizable success field", () => {
      expect(isStorageAssignmentSuccess({})).toBe(false);
      expect(isStorageAssignmentSuccess({ unrelated: "x" })).toBe(false);
    });
  });

  describe("defensive inputs", () => {
    it("returns false for null/undefined", () => {
      expect(isStorageAssignmentSuccess(null)).toBe(false);
      expect(isStorageAssignmentSuccess(undefined)).toBe(false);
    });
  });
});
