/**
 * ESLint rule: no-useeffect-timer-leaks
 *
 * Flags `setTimeout` / `setInterval` calls inside a React `useEffect`
 * (or `useLayoutEffect`) body whose returned cleanup function does NOT
 * pair them with a matching `clearTimeout` / `clearInterval`.
 *
 * Root cause of the class — when a component unmounts, any timer
 * scheduled inside `useEffect` still fires unless the cleanup clears
 * it. This causes:
 *
 *   1. Memory leaks (timer holds the effect's captured scope).
 *   2. `Cannot perform a React state update on an unmounted component`
 *      warnings in dev.
 *   3. In Vitest, access to torn-down `window` / `document` globals →
 *      `ReferenceError` caught as "unhandled exception" → flaky CI.
 *
 * Surfaced by the ErrorDashboard.jsx:175 CI flake on 2026-04-20 (fixed
 * in #3461). Rule added here to prevent regression.
 *
 * Expected pattern (OK):
 *
 *   useEffect(() => {
 *     const t = setTimeout(fn, 100);
 *     return () => clearTimeout(t);
 *   }, []);
 *
 *   useEffect(() => {
 *     myRef.current = setInterval(fn, 1000);
 *     return () => clearInterval(myRef.current);
 *   }, []);
 *
 * Flagged patterns:
 *
 *   useEffect(() => {
 *     setTimeout(fn, 100);            // scheduled but not captured
 *     return () => { ... };           // cleanup missing clearTimeout
 *   }, []);
 *
 *   useEffect(() => {
 *     const t = setTimeout(fn, 100);  // captured but no cleanup at all
 *   }, []);
 *
 * The rule looks for `clearTimeout(` / `clearInterval(` anywhere in
 * the cleanup body — not precise handle matching. That's sufficient
 * for the anti-pattern this rule targets; precise handle matching
 * requires data-flow analysis out of scope here.
 *
 * See React docs §"useEffect cleanup"; project guide
 * .specify/guides/playwright-best-practices.md (same principle applies
 * to all effect scheduling — timers, listeners, subscriptions, fetch).
 */

const SCHEDULING_METHODS = ["setTimeout", "setInterval"];
const CLEANUP_METHODS_FOR = {
  setTimeout: "clearTimeout",
  setInterval: "clearInterval",
};
const EFFECT_HOOKS = new Set(["useEffect", "useLayoutEffect"]);

/**
 * Walks the AST below `node` looking for a CallExpression whose callee
 * identifier matches any name in `names` (either a bare identifier or
 * the property of a MemberExpression).
 */
function containsCallTo(node, names) {
  if (!node || typeof node !== "object") return false;
  if (node.type === "CallExpression") {
    const callee = node.callee;
    let calleeName = null;
    if (callee && callee.type === "Identifier") {
      calleeName = callee.name;
    } else if (
      callee &&
      callee.type === "MemberExpression" &&
      callee.property &&
      callee.property.type === "Identifier"
    ) {
      calleeName = callee.property.name;
    }
    if (calleeName && names.has(calleeName)) {
      return true;
    }
  }
  for (const key of Object.keys(node)) {
    if (key === "parent") continue;
    const child = node[key];
    if (Array.isArray(child)) {
      for (const c of child) {
        if (c && typeof c === "object" && containsCallTo(c, names)) return true;
      }
    } else if (child && typeof child === "object" && child.type) {
      if (containsCallTo(child, names)) return true;
    }
  }
  return false;
}

/**
 * Extract the cleanup-function body from a useEffect body, if any.
 * Looks for `return () => { ... }` or `return function() { ... }`.
 */
function findCleanupFunctionBody(effectBody) {
  if (!effectBody) return null;
  const statements =
    effectBody.type === "BlockStatement" ? effectBody.body : [effectBody];
  for (const stmt of statements) {
    if (stmt.type === "ReturnStatement" && stmt.argument) {
      const arg = stmt.argument;
      if (
        arg.type === "ArrowFunctionExpression" ||
        arg.type === "FunctionExpression"
      ) {
        return arg.body;
      }
    }
  }
  return null;
}

export default {
  meta: {
    type: "problem",
    docs: {
      description:
        "Disallow scheduling setTimeout/setInterval inside useEffect " +
        "without a matching clearTimeout/clearInterval in the effect's " +
        "cleanup function.",
    },
    schema: [],
    messages: {
      missingCleanup:
        "{{ schedulingMethod }} inside useEffect has no matching " +
        "{{ cleanupMethod }} in the effect's cleanup. Pending timers " +
        "fire after unmount and can crash tests (window undefined) or " +
        "warn about state updates on unmounted components. Capture the " +
        "timer handle and call {{ cleanupMethod }} in the returned " +
        "cleanup function.",
    },
  },
  create(context) {
    return {
      CallExpression(node) {
        const callee = node.callee;
        let hookName = null;
        if (callee.type === "Identifier") {
          hookName = callee.name;
        } else if (
          callee.type === "MemberExpression" &&
          callee.property &&
          callee.property.type === "Identifier"
        ) {
          hookName = callee.property.name;
        }
        if (!hookName || !EFFECT_HOOKS.has(hookName)) return;

        const effectArg = node.arguments[0];
        if (
          !effectArg ||
          (effectArg.type !== "ArrowFunctionExpression" &&
            effectArg.type !== "FunctionExpression")
        ) {
          return;
        }

        const effectBody = effectArg.body;
        if (!effectBody) return;

        const scheduled = [];
        for (const method of SCHEDULING_METHODS) {
          if (containsCallTo(effectBody, new Set([method]))) {
            scheduled.push(method);
          }
        }
        if (scheduled.length === 0) return;

        const cleanupBody = findCleanupFunctionBody(effectBody);

        for (const method of scheduled) {
          const cleanupMethod = CLEANUP_METHODS_FOR[method];
          if (
            !cleanupBody ||
            !containsCallTo(cleanupBody, new Set([cleanupMethod]))
          ) {
            context.report({
              node,
              messageId: "missingCleanup",
              data: {
                schedulingMethod: method,
                cleanupMethod,
              },
            });
          }
        }
      },
    };
  },
};
