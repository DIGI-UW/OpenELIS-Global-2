import { createIntl } from "react-intl";
import { extendedEnd, positivityDuration } from "../MicrobiologyTime";
import messages from "../../../languages/en.json";
const intl = createIntl({ locale: "en", messages });
it("extends the existing incubation end using elapsed units across midnight", () => {
  expect(extendedEnd("2026-10-11T22:45:16.123Z", 12, "HOURS")).toBe(
    "2026-10-12T10:45:16.123Z",
  );
  expect(extendedEnd("2026-10-11T22:45:16.123Z", 2, "DAYS")).toBe(
    "2026-10-13T22:45:16.123Z",
  );
  expect(extendedEnd("2026-10-11T22:45:16.123Z", 0, "HOURS")).toBeNull();
});
it("shows positivity in elapsed days, hours and minutes without rounding upward", () => {
  expect(positivityDuration(intl, 49.5)).toBe("2d 1h 30m");
  expect(positivityDuration(intl, 0)).toBe("0d 0h 0m");
  expect(positivityDuration(intl, null)).toBe(messages["not.available"]);
});
