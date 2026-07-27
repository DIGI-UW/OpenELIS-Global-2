/**
 * Shared qi_config threshold semantics (OGC-710) for the QI Dashboard tiles
 * and the per-indicator detail pages — one place so the surfaces can't drift,
 * mirroring the useQiEnabled consolidation.
 */

/**
 * Tone of a metric value against a resolved config: "green" at/inside target,
 * "red" at/past action, "amber" between, "gray" when unjudgeable (no value,
 * disabled, or thresholds missing — fail-open like useQiEnabled).
 */
export function rateTone(value, config) {
  if (
    value == null ||
    !config?.enabled ||
    config.target == null ||
    config.action == null
  ) {
    return "gray";
  }
  if (config.direction === "HIGHER_BETTER") {
    if (value >= config.target) {
      return "green";
    }
    return value <= config.action ? "red" : "amber";
  }
  if (value <= config.target) {
    return "green";
  }
  return value >= config.action ? "red" : "amber";
}

/** Display unit for an indicator's thresholds: TAT is hours, rates are %. */
export function unitFor(indicatorKey) {
  if (indicatorKey === "TAT") {
    return "h";
  }
  return indicatorKey === "NCE" ? "" : "%";
}

/**
 * Carbon-charts threshold lines (green target / red action) for a detail
 * page's trend chart, or undefined when the config carries no usable
 * thresholds. Labels come in pre-formatted so this stays intl-free.
 */
export function chartThresholds(config, targetLabel, actionLabel) {
  if (!config?.enabled || config.target == null || config.action == null) {
    return undefined;
  }
  return [
    { value: config.target, label: targetLabel, fillColor: "#198038" },
    { value: config.action, label: actionLabel, fillColor: "#da1e28" },
  ];
}

/**
 * Threshold display parts for a tile, e.g. {target: "≤ 0.5%", action: "≥ 2%"}
 * (LOWER_BETTER). Null when the config carries no usable thresholds.
 */
export function thresholdParts(config, indicatorKey) {
  if (!config?.enabled || config.target == null || config.action == null) {
    return null;
  }
  const unit = unitFor(indicatorKey);
  const [targetOp, actionOp] =
    config.direction === "HIGHER_BETTER" ? ["≥", "≤"] : ["≤", "≥"];
  return {
    target: `${targetOp} ${config.target}${unit}`,
    action: `${actionOp} ${config.action}${unit}`,
  };
}
