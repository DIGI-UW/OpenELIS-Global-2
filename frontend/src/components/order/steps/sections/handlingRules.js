/**
 * The Handling group of a sample (FRS clinical order entry v4, FR-C9a, D-217):
 * what the test catalog requires, the condition the sample arrived in, where
 * it is stored, and whether any of those miss the requirement. Display and
 * flag only; a mismatch never blocks a save.
 */

export const ARRIVAL_CONDITIONS = [
  "ROOM_TEMPERATURE",
  "REFRIGERATED",
  "FROZEN",
  "ON_ICE",
  "DRY_ICE",
];

const ARRIVAL_LABEL_KEYS = {
  ROOM_TEMPERATURE: "roomTemp",
  REFRIGERATED: "refrigerated",
  FROZEN: "frozen",
  ON_ICE: "onIce",
  DRY_ICE: "dryIce",
};

export const arrivalLabelId = (condition) =>
  ARRIVAL_LABEL_KEYS[condition]
    ? `sample.condition.${ARRIVAL_LABEL_KEYS[condition]}`
    : null;

export const storageConditionLabelId = (condition) =>
  `label.testCatalog.storage.condition.${condition}`;

/** Degrees C a catalog storage condition allows; null bounds are open. */
export const CONDITION_RANGES = {
  AMBIENT: [15, 30],
  CONTROLLED_ROOM_TEMPERATURE: [20, 25],
  COOL_ROOM: [8, 15],
  COLD_ROOM: [2, 8],
  REFRIGERATED: [2, 8],
  FROZEN: [null, -15],
  DEEP_FROZEN: [null, -60],
  ULTRA_LOW_FREEZER: [null, -70],
  WARM_INCUBATOR: [35, 37],
};

/** The arrival conditions that meet each catalog storage condition. */
export const ACCEPTED_ARRIVALS = {
  AMBIENT: ["ROOM_TEMPERATURE"],
  CONTROLLED_ROOM_TEMPERATURE: ["ROOM_TEMPERATURE"],
  COOL_ROOM: ["REFRIGERATED", "ON_ICE"],
  COLD_ROOM: ["REFRIGERATED", "ON_ICE"],
  REFRIGERATED: ["REFRIGERATED", "ON_ICE"],
  FROZEN: ["FROZEN", "DRY_ICE"],
  DEEP_FROZEN: ["DRY_ICE"],
  ULTRA_LOW_FREEZER: ["DRY_ICE"],
  WARM_INCUBATOR: ["ROOM_TEMPERATURE"],
};

const toNumber = (value) => {
  if (value === null || value === undefined || String(value).trim() === "") {
    return null;
  }
  const number = Number(String(value).replace(",", "."));
  return Number.isFinite(number) ? number : null;
};

const outside = (condition, degrees) => {
  const range = CONDITION_RANGES[condition];
  if (!range || degrees === null) {
    return false;
  }
  const [low, high] = range;
  return (low !== null && degrees < low) || (high !== null && degrees > high);
};

export const MIN_ARRIVAL_TEMPERATURE = -100;
export const MAX_ARRIVAL_TEMPERATURE = 60;

/**
 * Whether a measured arrival temperature can be stored: blank, or a number
 * (decimal comma accepted) from -100 to 60 °C, the same bounds the server
 * keeps.
 */
export function isPlausibleTemperature(value) {
  if (value === null || value === undefined || String(value).trim() === "") {
    return true;
  }
  const degrees = toNumber(value);
  return (
    degrees !== null &&
    degrees >= MIN_ARRIVAL_TEMPERATURE &&
    degrees <= MAX_ARRIVAL_TEMPERATURE
  );
}

/** The distinct catalog storage conditions of a sample's tests. */
export function requiredConditions(requirements = []) {
  return [
    ...new Set(
      requirements.map((r) => r && r.storageCondition).filter(Boolean),
    ),
  ];
}

/** Free-text conditions the catalog records instead of, or beside, a code. */
export function customConditions(requirements = []) {
  return [
    ...new Set(
      requirements.map((r) => r && r.storageConditionCustom).filter(Boolean),
    ),
  ];
}

/** The shortest holding time of the sample's tests, in minutes, or null. */
export function shortestHoldingMinutes(requirements = []) {
  const limits = requirements
    .map((r) => Number(r && r.holdingMinutes))
    .filter((minutes) => Number.isFinite(minutes) && minutes > 0);
  return limits.length > 0 ? Math.min(...limits) : null;
}

/**
 * Every way the sample misses its requirement: arrived in a condition that
 * does not meet it, measured outside its range on arrival, or stored in a
 * device set outside its range. Empty when there is no requirement or nothing
 * recorded to compare.
 */
export function handlingMismatches({
  requirements = [],
  arrivalCondition = "",
  arrivalTemperature = "",
  storageTemperature = "",
}) {
  const measured = toNumber(arrivalTemperature);
  const stored = toNumber(storageTemperature);
  const mismatches = [];
  requiredConditions(requirements).forEach((required) => {
    const accepted = ACCEPTED_ARRIVALS[required];
    if (arrivalCondition && accepted && !accepted.includes(arrivalCondition)) {
      mismatches.push({ kind: "arrival", required, actual: arrivalCondition });
    }
    if (outside(required, measured)) {
      mismatches.push({ kind: "measured", required, actual: measured });
    }
    if (outside(required, stored)) {
      mismatches.push({ kind: "stored", required, actual: stored });
    }
  });
  return mismatches;
}

/** One sentence per mismatch, for the tag's tooltip and the NCE report. */
export function describeMismatch(intl, mismatch) {
  const required = intl.formatMessage({
    id: storageConditionLabelId(mismatch.required),
  });
  let actual;
  if (mismatch.kind === "arrival") {
    actual = intl.formatMessage(
      { id: "sample.handling.actual.arrived" },
      {
        condition: intl.formatMessage({ id: arrivalLabelId(mismatch.actual) }),
      },
    );
  } else if (mismatch.kind === "measured") {
    actual = intl.formatMessage(
      { id: "sample.handling.actual.measured" },
      { degrees: mismatch.actual },
    );
  } else {
    actual = intl.formatMessage(
      { id: "sample.handling.actual.stored" },
      { degrees: mismatch.actual },
    );
  }
  return intl.formatMessage(
    { id: "sample.handling.mismatch.detail" },
    { required, actual },
  );
}
