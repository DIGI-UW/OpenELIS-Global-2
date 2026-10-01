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
}

/**
 * The flag a row should show for the value it holds right now.
 *
 * A numeric row is judged as it is typed, against the bounds the server
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
  const numeric = normalizeScientificNotation(typed.replace(/[<>]/g, ""));
  const { flag } = classifyNumericResult(numeric, row);
  return isResultFlag(flag) ? flag : undefined;
}
