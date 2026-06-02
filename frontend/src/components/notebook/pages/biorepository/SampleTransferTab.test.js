import { buildAcceptErrorMessage } from "./SampleTransferTab";

describe("buildAcceptErrorMessage", () => {
  it("returns deduplicated server errors joined with separators", () => {
    const message = buildAcceptErrorMessage([
      "Transfer item is not pending: 12",
      "Transfer item is not pending: 12",
      "Sample item not found: ABC-100",
    ]);

    expect(message).toBe(
      "Transfer item is not pending: 12 | Sample item not found: ABC-100",
    );
  });

  it("returns null when there are no valid errors", () => {
    expect(buildAcceptErrorMessage([])).toBeNull();
    expect(buildAcceptErrorMessage([null, "", undefined])).toBeNull();
  });
});
