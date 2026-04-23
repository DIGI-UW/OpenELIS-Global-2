/**
 * ESLint rule: no-raw-react-lazy
 *
 * Requires `lazyWithRetry(() => import(...))` in place of raw
 * `React.lazy(() => import(...))` for route-level code splitting.
 *
 * Raw `React.lazy` does not retry on factory failure. A single
 * transient chunk-fetch error (e.g., Chrome's
 * `ERR_NETWORK_CHANGED` when the browser's network state flickers
 * mid-request) permanently breaks the route until full page reload,
 * falling through to the `RouteErrorBoundary` fallback. This
 * surfaced as a recurring develop-CI flake on the AnalyzerForm
 * chunk fetch; the `lazyWithRetry` helper (App.jsx) wraps the
 * dynamic-import factory in a bounded retry loop.
 *
 * If the replacement helper is genuinely unavailable at the call
 * site (e.g., inside the definition of `lazyWithRetry` itself),
 * suppress with a targeted disable comment:
 *
 *   // eslint-disable-next-line local/no-raw-react-lazy --
 *   // helper implementation: wraps React.lazy with retry semantics.
 *
 * Only detects the `React.lazy(...)` form. If the codebase ever
 * uses `import { lazy } from "react"` + bare `lazy(...)` calls,
 * extend this rule to also resolve the identifier's import source.
 */

export default {
  meta: {
    type: "problem",
    docs: {
      description:
        "Require lazyWithRetry in place of raw React.lazy for " +
        "retry-on-transient-failure on dynamic chunk imports.",
    },
    schema: [],
    messages: {
      useLazyWithRetry:
        "Use `lazyWithRetry(() => import(...))` instead of " +
        "`React.lazy(() => import(...))`. Raw React.lazy doesn't " +
        "retry on failure — a single transient chunk-fetch blip " +
        "(e.g., ERR_NETWORK_CHANGED) permanently breaks the route " +
        "and triggers the RouteErrorBoundary fallback. See the " +
        "`lazyWithRetry` helper in App.jsx.",
    },
  },
  create(context) {
    return {
      CallExpression(node) {
        const callee = node.callee;
        if (callee.type !== "MemberExpression") return;
        if (
          callee.object.type === "Identifier" &&
          callee.object.name === "React" &&
          callee.property &&
          callee.property.type === "Identifier" &&
          callee.property.name === "lazy"
        ) {
          context.report({ node, messageId: "useLazyWithRetry" });
        }
      },
    };
  },
};
