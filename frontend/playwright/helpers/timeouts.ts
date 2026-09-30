/**
 * PW_TIMEOUT_SCALE multiplies every timeout tier. Default 1 — CI and local
 * stack runs are unaffected. Set it (e.g. 10) for runs against remote
 * dev-mode deployments like testing.openelis-global.org, where the vite dev
 * server delivers the full unbundled module graph over the WAN and a fresh
 * browser context's first page load takes minutes (module scripts block
 * DOMContentLoaded until the whole import graph arrives).
 */
export const TIMEOUT_SCALE = Math.max(
  1,
  Number(process.env.PW_TIMEOUT_SCALE) || 1,
);

/** Conditional checks, dropdown options, negative assertions (element absent) */
export const QUICK_TIMEOUT = 2_000 * TIMEOUT_SCALE;

/** Element attachment, visibility, enabled checks */
export const SHORT_TIMEOUT = 5_000 * TIMEOUT_SCALE;

/** Post-action UI state assertions (state changes after clicks/saves) */
export const UI_TIMEOUT = 10_000 * TIMEOUT_SCALE;

/** Page navigation, cross-page verification, form submissions */
export const LONG_TIMEOUT = 30_000 * TIMEOUT_SCALE;

/** Full page load, auth flows, initial bootstrap */
export const NAV_TIMEOUT = 45_000 * TIMEOUT_SCALE;
