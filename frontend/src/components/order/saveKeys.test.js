import { withClientKeys } from "./saveKeys";

describe("withClientKeys", () => {
  it("keys every unsaved sample row that has a sample type", () => {
    const { samples, changed } = withClientKeys([
      { sampleTypeId: "3" },
      { sampleTypeId: "" },
      { sampleTypeId: "4", sampleItemId: "90" },
    ]);

    expect(changed).toBe(true);
    expect(samples[0].clientKey).toMatch(/^[0-9a-f-]{36}$/);
    expect(samples[1].clientKey).toBeUndefined();
    expect(samples[2].clientKey).toBeUndefined();
  });

  it("keeps an existing key so a retry sends the same one", () => {
    const rows = [{ sampleTypeId: "3", clientKey: "k-1" }];

    const { samples, changed } = withClientKeys(rows);

    expect(changed).toBe(false);
    expect(samples).toBe(rows);
  });

  it("gives a copied row its own key instead of sharing one", () => {
    const { samples } = withClientKeys([
      { sampleTypeId: "3", clientKey: "k-1" },
      { sampleTypeId: "3", clientKey: "k-1" },
    ]);

    expect(samples[0].clientKey).toBe("k-1");
    expect(samples[1].clientKey).not.toBe("k-1");
  });
});
