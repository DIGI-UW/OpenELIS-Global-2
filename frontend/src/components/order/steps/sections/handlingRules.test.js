import {
  ARRIVAL_CONDITIONS,
  arrivalLabelId,
  customConditions,
  describeMismatch,
  handlingMismatches,
  requiredConditions,
  shortestHoldingMinutes,
} from "./handlingRules";
import { createIntl } from "react-intl";
import messages from "../../../../languages/en.json";

const intl = createIntl({ locale: "en", messages });
const refrigerated = [
  { testId: "1", storageCondition: "REFRIGERATED", holdingMinutes: 240 },
  { testId: "2", storageCondition: "REFRIGERATED", holdingMinutes: 480 },
];

describe("handling rules (OGC-1424, FR-C9a)", () => {
  test("the requirement is the distinct catalog conditions and the shortest holding time", () => {
    expect(requiredConditions(refrigerated)).toEqual(["REFRIGERATED"]);
    expect(shortestHoldingMinutes(refrigerated)).toBe(240);
    expect(requiredConditions([{ storageCondition: null }])).toEqual([]);
    expect(shortestHoldingMinutes([{ holdingMinutes: null }])).toBeNull();
    expect(
      customConditions([{ storageConditionCustom: "Keep upright" }]),
    ).toEqual(["Keep upright"]);
  });

  test("a sample that arrived at room temperature misses a refrigerated requirement", () => {
    expect(
      handlingMismatches({
        requirements: refrigerated,
        arrivalCondition: "ROOM_TEMPERATURE",
      }),
    ).toEqual([
      {
        kind: "arrival",
        required: "REFRIGERATED",
        actual: "ROOM_TEMPERATURE",
      },
    ]);
  });

  test("on ice meets refrigerated, dry ice meets frozen", () => {
    expect(
      handlingMismatches({
        requirements: refrigerated,
        arrivalCondition: "ON_ICE",
      }),
    ).toEqual([]);
    expect(
      handlingMismatches({
        requirements: [{ storageCondition: "FROZEN" }],
        arrivalCondition: "DRY_ICE",
      }),
    ).toEqual([]);
  });

  test("a measured temperature or a storage device outside the range is a mismatch", () => {
    expect(
      handlingMismatches({
        requirements: refrigerated,
        arrivalCondition: "REFRIGERATED",
        arrivalTemperature: "11,5",
        storageTemperature: "-20",
      }),
    ).toEqual([
      { kind: "measured", required: "REFRIGERATED", actual: 11.5 },
      { kind: "stored", required: "REFRIGERATED", actual: -20 },
    ]);
    expect(
      handlingMismatches({
        requirements: refrigerated,
        arrivalTemperature: "4",
        storageTemperature: "5",
      }),
    ).toEqual([]);
  });

  test("nothing to compare is never a mismatch", () => {
    expect(handlingMismatches({ requirements: [] })).toEqual([]);
    expect(
      handlingMismatches({
        requirements: [],
        arrivalCondition: "ROOM_TEMPERATURE",
      }),
    ).toEqual([]);
    expect(handlingMismatches({ requirements: refrigerated })).toEqual([]);
  });

  test("every arrival condition has a text tag and a mismatch reads as a sentence", () => {
    ARRIVAL_CONDITIONS.forEach((condition) => {
      expect(messages[arrivalLabelId(condition)]).toBeTruthy();
    });
    expect(
      describeMismatch(intl, {
        kind: "arrival",
        required: "REFRIGERATED",
        actual: "ROOM_TEMPERATURE",
      }),
    ).toBe("Needs Refrigerated (2–8°C); arrived as Room temperature");
  });
});
