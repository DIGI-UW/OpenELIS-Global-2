// Data seam for Lab EQA Performance (OGC-611, FR-V2.3-07). One rollup endpoint
// serves both views: the page shows the same KPI row above either of them, and
// two calls would let the two halves disagree.
import { getFromOpenElisServer } from "../../utils/Utils";

const EMPTY = { kpis: {}, coverage: [], gaps: [], recentCycles: [] };

export const fetchLabPerformance = (callback) =>
  getFromOpenElisServer("/rest/eqa/lab-performance", (data) =>
    callback(data ? { ...EMPTY, ...data } : EMPTY),
  );

/**
 * The register's EQA filter (docs/eqa/nce-deep-links.md). Both EQA trigger
 * sources share the EQA_ prefix, so one query parameter covers auto-created
 * NCEs and supervisor escalations alike.
 */
export const NCE_REGISTER_EQA_LINK = "/NceDashboard?source=eqa";
