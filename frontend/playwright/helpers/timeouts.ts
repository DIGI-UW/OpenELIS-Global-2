/** Conditional checks, dropdown options, negative assertions (element absent) */
export const QUICK_TIMEOUT = 2_000;

/** Element attachment, visibility, enabled checks */
export const SHORT_TIMEOUT = 5_000;

/** Post-action UI state assertions (state changes after clicks/saves) */
export const UI_TIMEOUT = 10_000;

/** Post-action UI state assertions (state changes after clicks/saves) */
export const UI_TIMEOUT_PLUS = 15_000;

/** Page navigation, cross-page verification, form submissions */
export const LONG_TIMEOUT = 30_000;

/** Full page load, auth flows, initial bootstrap */
export const NAV_TIMEOUT = 45_000;

/** Full page load, auth flows, initial bootstrap */
export const NAV_TIMEOUT_EXTENDED = 60_000;

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

/** Standard pause for dropdown interactions and short waits */
export const SHORT_PAUSE_PLUS = 600;

/** Moderate pause for form field interactions */
export const MODERATE_PAUSE = 800;

/** Moderate pause for form field interactions */
export const MODERATE_PAUSE_PLUS = 900;

/** One second pause for transitions requiring full second waits */
export const LONG_PAUSE = 1_000;

/** Extended pause for complex UI operations */
export const LONG_PAUSE_PLUS = 1_500;

/** Two second pause for extended transitions */
export const EXTENDED_PAUSE = 2_000;

/** Extended pause for demo scenarios and title cards */
export const DEMO_PAUSE = 3_000;

/** Extended-plus pause for longer demo operations */
export const EXTENDED_PAUSE_PLUS = 5_000;

// ── Polling Intervals ───────────────────────────────────────────────────

/** Health check polling intervals */
export const HEALTH_CHECK_INTERVALS = [1_000, 2_000, 5_000];

/** Results UI polling intervals */
export const RESULTS_UI_POLL_INTERVALS = [2_000];

// ── Video Pause Durations ─────────────────────────────────────────────

/** Short video pause for quick UI transitions */
export const SHORT_VIDEO_PAUSE = 1_000;

/** Short-plus video pause for quick highlights (600ms) */
export const SHORT_VIDEO_PAUSE_PLUS = 600;

/** Medium video pause for standard UI operations (800ms) */
export const MEDIUM_VIDEO_PAUSE = 800;

/** Extra-long video pause between title cards (1200ms) */
export const EXTRA_VIDEO_PAUSE = 1_200;

/** Standard video pause for moderate UI operations */
export const LONG_VIDEO_PAUSE = 1_500;

/** Long video pause for extended UI operations */
export const EXTENDED_VIDEO_PAUSE = 2_000;

// ── File import Durations ─────────────────────────────────────────────
/** Default value for file import poll interval */
export const DEFAULT_FILE_IMPORT_POLL_MS = 60_000;

/** Default value for file import drop buffer */
export const DEFAULT_FILE_IMPORT_DROP_BUFFER_MS = 45_000;

// ── API Durations ─────────────────────────────────────────────

/** API timeout */
export const API_READY_TIMEOUT_MS = 15_000;

/** API retry delay */
export const API_RETRY_DELAY_MS = 500;
