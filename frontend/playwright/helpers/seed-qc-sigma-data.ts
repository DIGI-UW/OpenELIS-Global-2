/**
 * QC Westgard sigma-metric E2E seed helper (OGC-704, OGC-705).
 *
 * The sigma tile on ControlChartDetail reads
 * `GET /rest/qc/charts/{lot}/statistics`, which needs a control lot, a
 * computed `qc_statistics` row (mean/SD), and a per-test `TEa`. `seedQcLot`
 * writes all three; this file only fixes the sentinel ids the sigma spec
 * navigates by.
 */
import { QcLotSeed, seedQcLot } from "./seed-qc-lot";

export type SigmaSeed = QcLotSeed;

const IDENTITY = {
  analyzerId: 990901,
  analyzerName: "PW Sigma Analyzer",
  lotId: "pw-sigma-lot",
  statId: "pw-sigma-stat",
  // The lowest test row; the export seed borrows the next one up.
  testRowOffset: 0,
};

/**
 * @param opts.tea  total allowable error (percent)
 * @param opts.mean control mean (default 100)
 * @param opts.sd   control SD (default 2) → CV = sd/mean*100
 */
export function seedSigmaData(opts: {
  tea: number;
  mean?: number;
  sd?: number;
}): SigmaSeed {
  return seedQcLot(IDENTITY, {
    tea: opts.tea,
    mean: opts.mean ?? 100,
    sd: opts.sd ?? 2,
  });
}
