// ESLint flat config (ESLint 9+). Replaces legacy .eslintrc.*.
//
// Native ESM — loaded directly by Node under package.json `"type": "module"`.
// Inline plugin objects (no `eslint-plugin-local-rules` shim).
//
// Load order: base globals → JS/JSX → TS/TSX → tests (Jest) → Playwright →
// demo subset → helper exemptions. Later blocks refine earlier ones via
// flat-config's `files` matching.

import js from "@eslint/js";
import globals from "globals";
import reactPlugin from "eslint-plugin-react";
import reactHooksPlugin from "eslint-plugin-react-hooks";
import prettierPlugin from "eslint-plugin-prettier";
import prettierConfig from "eslint-config-prettier";
import jestPlugin from "eslint-plugin-jest";
import playwrightPlugin from "eslint-plugin-playwright";
import tseslint from "typescript-eslint";

import pwCountComparisonMatcher from "./eslint-local-rules/pw-count-comparison-matcher.js";
import pwDemoNoBackendAccess from "./eslint-local-rules/pw-demo-no-backend-access.js";
import noUseEffectTimerLeaks from "./eslint-local-rules/no-useeffect-timer-leaks.js";

export default [
  // ─── Global ignores ─────────────────────────────────────────────────
  // Migrated from the former .eslintignore. Flat config uses the
  // top-level `ignores` key instead of a separate file.
  {
    ignores: [
      "node_modules/",
      "dist/",
      "build/",
      "coverage/",
      "playwright-report/",
      "test-results/",
      "cypress/videos/",
      "cypress/screenshots/",
      ".vite/",
      "public/",
      "scripts/",
      "**/*.min.js",
    ],
  },

  // ─── Local plugin registration (global) ─────────────────────────────
  // Flat config forbids redefining a plugin key across blocks, so the
  // `local` namespace is registered once here and later blocks enable
  // its rules per-file-pattern.
  {
    plugins: {
      local: {
        rules: {
          "pw-count-comparison-matcher": pwCountComparisonMatcher,
          "pw-demo-no-backend-access": pwDemoNoBackendAccess,
          "no-useeffect-timer-leaks": noUseEffectTimerLeaks,
        },
      },
    },
  },

  // ─── Base JS/JSX ────────────────────────────────────────────────────
  js.configs.recommended,
  {
    files: ["**/*.{js,jsx,mjs,cjs}"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    plugins: {
      react: reactPlugin,
      "react-hooks": reactHooksPlugin,
      prettier: prettierPlugin,
    },
    settings: {
      react: { version: "detect" },
    },
    rules: {
      ...reactPlugin.configs.recommended.rules,
      ...reactHooksPlugin.configs.recommended.rules,
      ...prettierConfig.rules,
      "react/prop-types": "off",
      "react/react-in-jsx-scope": "off",
      "react/no-unescaped-entities": "warn",
      "react-hooks/exhaustive-deps": "off",
      "no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
      "prettier/prettier": "warn",
      // React hook timer-leak guard (prevents the ErrorDashboard.jsx
      // class of flake where a setTimeout scheduled inside useEffect
      // fires after unmount and crashes Vitest with `window is not
      // defined`).
      "local/no-useeffect-timer-leaks": "error",
    },
  },

  // ─── TypeScript ─────────────────────────────────────────────────────
  ...tseslint.configs.recommended.map((cfg) => ({
    ...cfg,
    files: ["**/*.{ts,tsx}"],
  })),
  {
    files: ["**/*.{ts,tsx}"],
    plugins: {
      react: reactPlugin,
      "react-hooks": reactHooksPlugin,
    },
    settings: {
      react: { version: "detect" },
    },
    rules: {
      ...reactPlugin.configs.recommended.rules,
      "react/prop-types": "warn",
      "react/react-in-jsx-scope": "off",
      "react/display-name": "off",
      "no-case-declarations": "off",
      "@typescript-eslint/no-empty-function": "warn",
      "@typescript-eslint/no-empty-interface": "warn",
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
      // Disable the base rule; the TS-aware version above supersedes it.
      "no-unused-vars": "off",
      // Same hook-leak guard for TS/TSX components.
      "local/no-useeffect-timer-leaks": "error",
    },
  },

  // ─── Jest unit tests ────────────────────────────────────────────────
  {
    files: ["**/*.test.{js,jsx,ts,tsx}"],
    plugins: { jest: jestPlugin },
    languageOptions: {
      globals: { ...globals.jest },
    },
    rules: {
      "jest/expect-expect": "error",
      "jest/no-disabled-tests": "warn",
      "jest/no-focused-tests": "error",
      "jest/valid-expect": "error",
    },
  },

  // ─── Playwright E2E specs + helpers ─────────────────────────────────
  // `local` plugin is registered in the global block above; rules are
  // referenced here by name (`local/pw-count-comparison-matcher`).
  {
    files: ["playwright/**/*.{ts,js}"],
    plugins: {
      playwright: playwrightPlugin,
    },
    rules: {
      // Upstream plugin rules (eslint-plugin-playwright 2.x).
      "playwright/expect-expect": [
        "error",
        {
          // Page-object methods that assert internally count as
          // assertions. Identifier match is literal (not glob) — grep
          // for `\.expect[A-Z]\w+\(` to find new ones.
          assertFunctionNames: [
            // Page-object `expect*` methods (sidenav, list, form fixtures).
            "expectLoaded",
            "expectOpen",
            "expectSuccessNotification",
            "expectExpanded",
            "expectCollapsed",
            "expectNoActiveGreyBackground",
            // Demo-flow helpers that assert visible UI state internally.
            "verifyResults",
            "verifyImportedResults",
            "acceptAndVerifyResults",
            "testConnection",
            "testAnalyzerConnection",
          ],
        },
      ],
      "playwright/missing-playwright-await": "error",
      "playwright/no-wait-for-timeout": "error",
      "playwright/no-force-option": "error",
      "playwright/prefer-web-first-assertions": "error",
      "playwright/prefer-to-have-count": "error",

      // Local rule — covers the count() + comparison-matcher gap
      // upstream's prefer-to-have-count does not match (it only handles
      // equality matchers: toBe, toEqual, toStrictEqual, toHaveLength).
      "local/pw-count-comparison-matcher": "error",
    },
  },

  // ─── Demo specs: UI-only ────────────────────────────────────────────
  // Plugin registration inherited from the playwright block above.
  {
    files: ["playwright/tests/demo/**/*.spec.ts"],
    rules: {
      "local/pw-demo-no-backend-access": "error",
    },
  },

  // ─── Helper exemption: pacing helpers use waitForTimeout by design ──
  {
    files: [
      "playwright/helpers/title-card.ts",
      "playwright/helpers/video-pause.ts",
    ],
    rules: {
      "playwright/no-wait-for-timeout": "off",
    },
  },
];
