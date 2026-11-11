/**
 * ESLint rule: pw-count-comparison-matcher
 *
 * Disallows non-retrying comparison assertions on `locator.count()` — the
 * specific gap that `eslint-plugin-playwright`'s `prefer-to-have-count`
 * does not cover (that rule only matches equality matchers: toBe, toEqual,
 * toStrictEqual, toHaveLength).
 *
 * Flagged patterns — both inline and capture-then-assert shapes:
 *
 *   expect(await rows.count()).toBeGreaterThan(0);
 *   expect(await rows.count()).toBeGreaterThanOrEqual(1);
 *   expect(await rows.count()).toBeLessThan(10);
 *   expect(await rows.count()).toBeLessThanOrEqual(5);
 *
 *   const n = await rows.count();
 *   expect(n).toBeGreaterThan(0);
 *
 * Correct replacements:
 *
 *   await expect(rows).toHaveCount(3);
 *
 *   await expect(rows.first()).toBeVisible();
 *   const n = await rows.count();
 *   expect(n).toBeGreaterThan(0);
 *
 * See .specify/guides/playwright-best-practices.md §"Use Auto-Retrying
 * Assertions" and Constitution V.6 (Test Quality Invariants).
 */

const COMPARISON_MATCHERS = new Set([
  "toBeGreaterThan",
  "toBeGreaterThanOrEqual",
  "toBeLessThan",
  "toBeLessThanOrEqual",
]);

function dereference(context, node) {
  if (!node || node.type !== "Identifier") return node;
  const scope = context.sourceCode.getScope(node);
  let variable = scope.variables.find((v) => v.name === node.name);
  let parentScope = scope.upper;
  while (!variable && parentScope) {
    variable = parentScope.variables.find((v) => v.name === node.name);
    parentScope = parentScope.upper;
  }
  if (!variable || variable.defs.length === 0) return node;
  const def = variable.defs[0];
  if (def.node.type === "VariableDeclarator" && def.node.init) {
    return def.node.init;
  }
  return node;
}

function isAwaitedCountCall(node) {
  if (!node || node.type !== "AwaitExpression") return false;
  const call = node.argument;
  if (!call || call.type !== "CallExpression") return false;
  const callee = call.callee;
  if (!callee || callee.type !== "MemberExpression") return false;
  const prop = callee.property;
  if (!prop) return false;
  if (prop.type === "Identifier" && prop.name === "count") return true;
  if (prop.type === "Literal" && prop.value === "count") return true;
  return false;
}

export default {
  meta: {
    type: "problem",
    docs: {
      description:
        "Disallow non-retrying comparison assertions on Playwright " +
        "locator.count() — use web-first assertions (toHaveCount) or " +
        "gate with toBeVisible(.first()) before reading count.",
    },
    schema: [],
    messages: {
      countComparisonMatcher:
        "Non-retrying comparison on `locator.count()`. `count()` is a " +
        "one-shot DOM snapshot; paired with {{ matcher }} on a raw " +
        "number, this flakes when rows haven't hydrated. Use " +
        "`await expect(locator).toHaveCount(N)` for exact counts, or " +
        "`await expect(locator.first()).toBeVisible()` as a precondition " +
        "before reading count() for range checks.",
    },
  },
  create(context) {
    return {
      CallExpression(node) {
        const callee = node.callee;
        if (!callee || callee.type !== "MemberExpression") return;
        if (!callee.property || callee.property.type !== "Identifier") return;
        const matcher = callee.property.name;
        if (!COMPARISON_MATCHERS.has(matcher)) return;

        const expectCall = callee.object;
        if (!expectCall || expectCall.type !== "CallExpression") return;
        if (
          !expectCall.callee ||
          expectCall.callee.type !== "Identifier" ||
          expectCall.callee.name !== "expect"
        ) {
          return;
        }

        const arg = expectCall.arguments && expectCall.arguments[0];
        if (!arg) return;

        const resolved = dereference(context, arg);
        if (isAwaitedCountCall(resolved)) {
          context.report({
            node,
            messageId: "countComparisonMatcher",
            data: { matcher },
          });
        }
      },
    };
  },
};
