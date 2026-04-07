/** Minimal timeout for pause use purposes */
export const MINIMAL_TIMEOUT = 1_000;

/** Conditional checks, dropdown options, negative assertions (element absent) */
export const QUICK_TIMEOUT = 2_000;

/** Element attachment, visibility, enabled checks */
export const SHORT_TIMEOUT = 5_000;

/** Post-action UI state assertions (state changes after clicks/saves) */
export const UI_TIMEOUT = 10_000;

/** Page navigation, cross-page verification, form submissions */
export const LONG_TIMEOUT = 30_000;

/** Full page load, auth flows, initial bootstrap */
export const NAV_TIMEOUT = 45_000;

/** Result processing, analyzer communication */
export const RESULTS_TIMEOUT = 90_000;

/** Extended test execution, complex workflows */
export const EXTENDED_TIMEOUT = 120_000;

/** Long-running demo scenarios, full test suites */
export const DEMO_TIMEOUT = 180_000;

/** Maximum timeout for comprehensive E2E flows */
export const MAX_TIMEOUT = 240_000;

// ── Pause Durations ─────────────────────────────────────────────────────

/** Minimal pause for quick UI transitions */
export const MINIMAL_PAUSE = 200;

/** Standard pause for dropdown interactions and short waits */
export const SHORT_PAUSE = 500;

/** Moderate pause for form field interactions */
export const MODERATE_PAUSE = 800;

/** Extended pause for complex UI operations */
export const LONG_PAUSE = 1_500;

/** Extended pause for demo scenarios and title cards */
export const DEMO_PAUSE = 3_000;

// ── Polling Intervals ───────────────────────────────────────────────────

/** Health check polling intervals */
export const HEALTH_CHECK_INTERVALS = [1_000, 2_000, 5_000];

// ── Video Pause Durations ─────────────────────────────────────────────

/** Short video pause for quick UI transitions */
export const VIDEO_PAUSE_SHORT = 1_000;

/** Standard video pause for moderate UI operations */
export const VIDEO_PAUSE_LONG = 1_500;

/** Long video pause for extended UI operations */
export const VIDEO_PAUSE_EXTENDED = 2_000;
