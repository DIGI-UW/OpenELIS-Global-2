// Data seam for Analyst Competency (OGC-611, FR-V2.3-06). One rollup endpoint
// serves the tiles, the table and each analyst's history: the bands and the
// evidence behind them must be computed once, or a row could disagree with the
// events it expands to show.
import { getFromOpenElisServer } from "../../utils/Utils";

const EMPTY = { kpis: {}, analysts: [] };

export const fetchAnalystCompetency = (callback) =>
  getFromOpenElisServer("/rest/eqa/analyst-competency", (data) =>
    callback(data ? { ...EMPTY, ...data } : EMPTY),
  );
