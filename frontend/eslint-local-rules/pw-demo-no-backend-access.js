/**
 * ESLint rule: pw-demo-no-backend-access
 *
 * Demo specs validate *visible UI state*, not logs or backend calls.
 * They must not:
 *
 *   1. Listen to page 'console' or 'pageerror' events.
 *   2. Use `waitForResponse(...)`.
 *   3. Call `page.request.get/put/delete(...)`.
 *
 * Activate via ESLint flat config with a `files` scoped to demo paths
 * (CORE_DEMO_TESTS / HARNESS_DEMO_TESTS — see playwright.config.ts).
 *
 * See .specify/guides/playwright-best-practices.md.
 */

function getStringLiteralValue(node) {
  if (!node) return null;
  if (node.type === "Literal" && typeof node.value === "string") {
    return node.value;
  }
  if (
    node.type === "TemplateLiteral" &&
    node.quasis.length === 1 &&
    node.expressions.length === 0
  ) {
    return node.quasis[0].value.cooked;
  }
  return null;
}

export default {
  meta: {
    type: "problem",
    docs: {
      description:
        "Demo specs must be UI-only: no console/pageerror listeners, " +
        "no waitForResponse, no direct backend requests.",
    },
    schema: [],
    messages: {
      consoleListener:
        "Demo specs must not listen to '{{ event }}' events. Demos " +
        "validate visible UI state, not console chatter.",
      waitForResponse:
        "Demo specs must not use `waitForResponse`. Synchronize via " +
        "UI assertions (`toBeVisible`, `toHaveText`, `toHaveURL`).",
      backendRequest:
        "Demo specs must not call `page.request.{{ method }}()`. " +
        "Demos record the user's visible journey, not backend calls.",
    },
  },
  create(context) {
    return {
      CallExpression(node) {
        const callee = node.callee;
        if (!callee || callee.type !== "MemberExpression") return;
        if (!callee.property || callee.property.type !== "Identifier") return;
        const methodName = callee.property.name;

        if (methodName === "on") {
          const event = getStringLiteralValue(node.arguments[0]);
          if (event === "console" || event === "pageerror") {
            context.report({
              node,
              messageId: "consoleListener",
              data: { event },
            });
            return;
          }
        }

        if (methodName === "waitForResponse") {
          context.report({ node, messageId: "waitForResponse" });
          return;
        }

        if (["get", "put", "delete"].includes(methodName)) {
          const obj = callee.object;
          if (
            obj &&
            obj.type === "MemberExpression" &&
            obj.property &&
            obj.property.type === "Identifier" &&
            obj.property.name === "request"
          ) {
            context.report({
              node,
              messageId: "backendRequest",
              data: { method: methodName },
            });
          }
        }
      },
    };
  },
};
