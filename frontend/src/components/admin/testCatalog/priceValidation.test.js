import { priceErrorId } from "./priceValidation";

describe("priceErrorId", () => {
  test.each(["", "   ", null, undefined])(
    "blank (%p) is fine: it leaves the item unpriced",
    (value) => {
      expect(priceErrorId(value)).toBeNull();
    },
  );

  test.each(["0", "500", "1234.5", "1234.50", " 99.99 ", "10.", "10.500"])(
    "a plain amount (%p) is accepted",
    (value) => {
      expect(priceErrorId(value)).toBeNull();
    },
  );

  test.each(["ten", "1e5", "+5", "-10", "1,000", "₹500", "5 0"])(
    "%p is not a plain amount",
    (value) => {
      expect(priceErrorId(value)).toBe("error.price.format");
    },
  );

  test("more than two decimals is refused", () => {
    expect(priceErrorId("10.555")).toBe("error.price.decimals");
  });

  test("the maximum is accepted and one paisa more is refused", () => {
    expect(priceErrorId("1000000")).toBeNull();
    expect(priceErrorId("1000000.01")).toBe("error.price.max");
    expect(priceErrorId("99999999999999999999")).toBe("error.price.max");
  });
});
