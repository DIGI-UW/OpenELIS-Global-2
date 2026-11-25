import { classifyNumericResult } from "../numericResultFlag";
import { normalizeScientificNotation } from "../scientificNotation";
import { isResultFlag, ResultFlag } from "./flags";

/**
 * The fields a worklist row carries for judging a numeric value: the
 * patient-resolved bounds the server copied from the result limit it selected
 * for this analysis, component and patient, and the flag it computed for the
 * stored value.
 */
export interface FlaggableRow {
  resultType?: string;
  resultValue?: string;
  resultFlag?: unknown;
  resultLimitId?: string | null;
  lowerNormalRange?: number | string | null;
  upperNormalRange?: number | string | null;
  lowerAbnormalRange?: number | string | null;
  upperAbnormalRange?: number | string | null;
  lowerCritical?: number | string | null;
  higherCritical?: number | string | null;
  significantDigits?: number | null;
}

/**
 * A plain decimal as the test reports it: rounded half up to its decimal
 * places, as the server judges flags (OGC-1544). Anything else, or unknown
 * places, is left as it is.
 */
function asReported(value: string, places?: number | null): string {
  if (places === undefined || places === null || places < 0) {
    return value;
  }
  if (!/^-?\d*\.?\d+$/.test(value)) {
    return value;
  }
  const negative = value.startsWith("-");
  const magnitude = negative ? value.slice(1) : value;
  const rounded = Number(
    Math.round(Number(`${magnitude}e${places}`)) + `e-${places}`,
  );
  return String(negative ? -rounded : rounded);
}

/**
 * The flag a row should show for the value it holds right now.
 *
 * A numeric row is judged as it is typed, rounded to the places the test
 * reports (a value behind < or > is not rounded), against the bounds the server
 * resolved for it, with the same rule the legacy Results screen and the server
 * apply (INVALID beats CRITICAL beats ABNORMAL beats NORMAL). Every other row,
 * and a numeric row the server found no result limit for, keeps the flag the
 * server computed. A blank or unparseable value carries no flag, as on the
 * server.
 */
export function resultFlagFor(row: FlaggableRow): ResultFlag | undefined {
  const serverFlag = isResultFlag(row.resultFlag) ? row.resultFlag : undefined;
  if (row.resultType !== "N" || !row.resultLimitId) {
    return serverFlag;
  }
  const typed = String(row.resultValue ?? "").trim();
  if (!typed) {
    return undefined;
  }
  const plain = normalizeScientificNotation(typed.replace(/[<>]/g, ""));
  const numeric = /^[<>]/.test(typed)
    ? plain
    : asReported(plain, row.significantDigits);
  const { flag } = classifyNumericResult(numeric, row);
  return isResultFlag(flag) ? flag : undefined;
}
