import { expect, test } from "../../../helpers/test-base";
import { measureBrowserOperation } from "../../../helpers/performance-evidence";

test.describe("Performance evidence statistics", () => {
  test("requires enough measurements for p95 to differ from the maximum", async () => {
    let operationCount = 0;

    await expect(
      measureBrowserOperation("undersampled-p95", 0, 19, 100, async () => {
        operationCount += 1;
        return 1;
      }),
    ).rejects.toThrow("p95 requires at least 20 measured iterations");
    expect(operationCount).toBe(0);
  });
});
