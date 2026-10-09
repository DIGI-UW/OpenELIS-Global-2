import { describe, expect, it } from "vitest";
import messages from "./en.json";

/**
 * Transifex reads an en.json value that opens with an ICU plural as a
 * pluralized entry and refuses the whole upload when anything after that
 * plural is another argument ("Invalid format of pluralized entry"), which
 * fails the push of every source change (microbiology.sets.count, two plurals
 * joined by a comma). Such a message is split: one key per plural, joined by a
 * plain message.
 */
const topLevelArguments = (value) => {
  const found = [];
  let depth = 0;
  let start = -1;
  for (let i = 0; i < value.length; i += 1) {
    if (value[i] === "{") {
      if (depth === 0) start = i;
      depth += 1;
    } else if (value[i] === "}") {
      depth -= 1;
      if (depth === 0) found.push(value.slice(start, i + 1));
    }
  }
  return found;
};

const opensWithPlural = (value) => /^\{\s*\w+\s*,\s*plural\s*,/.test(value);

describe("en.json values Transifex accepts", () => {
  it("has no message that opens with a plural and then carries another argument", () => {
    const rejected = Object.entries(messages)
      .filter(([, value]) => typeof value === "string")
      .filter(
        ([, value]) =>
          opensWithPlural(value) && topLevelArguments(value).length > 1,
      )
      .map(([key]) => key);

    expect(rejected).toEqual([]);
  });

  it("recognises the shape that failed the push", () => {
    const value =
      "{sets, plural, one {# set} other {# sets}}, {bottles, plural, one {# bottle} other {# bottles}}";

    expect(opensWithPlural(value) && topLevelArguments(value).length > 1).toBe(
      true,
    );
  });
});
