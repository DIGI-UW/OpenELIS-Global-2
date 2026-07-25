/**
 * ESLint rule: pw-demo-no-backend-access
 *
 * Demo specs validate *visible UI state*, not logs or backend calls.
 * They must not:
 *
 *   1. Listen to page 'console' or 'pageerror' events.
 *   2. Use `waitForResponse(...)`.
 *   3. Use Playwright request contexts or browser `fetch(...)`.
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
        "Demo specs must not call a request context's `{{ method }}()` method. " +
        "Demos record the user's visible journey, not backend calls.",
      backendFetch:
        "Demo specs must not call browser `fetch()`. Demos record the " +
        "user's visible journey, not backend calls.",
    },
  },
  create(context) {
    const requestAliases = new Set(["request"]);
    const requestMethods = new Set([
      "delete",
      "fetch",
      "get",
      "head",
      "patch",
      "post",
      "put",
      "newContext",
    ]);

    const propertyName = (member) =>
      member?.property?.type === "Identifier"
        ? member.property.name
        : getStringLiteralValue(member?.property);

    const isRequestObject = (node) => {
      if (!node) return false;
      if (node.type === "Identifier") {
        return requestAliases.has(node.name);
      }
      return (
        node.type === "MemberExpression" && propertyName(node) === "request"
      );
    };

    const collectRequestAliases = (pattern) => {
      if (!pattern || pattern.type !== "ObjectPattern") return;
      for (const property of pattern.properties) {
        if (
          property.type === "Property" &&
          ((property.key.type === "Identifier" &&
            property.key.name === "request") ||
            getStringLiteralValue(property.key) === "request") &&
          property.value.type === "Identifier"
        ) {
          requestAliases.add(property.value.name);
        }
      }
    };

    return {
      VariableDeclarator(node) {
        collectRequestAliases(node.id);
        if (node.id.type === "Identifier" && isRequestObject(node.init)) {
          requestAliases.add(node.id.name);
        }
      },
      "FunctionDeclaration, FunctionExpression, ArrowFunctionExpression"(node) {
        node.params.forEach(collectRequestAliases);
      },
      CallExpression(node) {
        const callee = node.callee;
        if (callee?.type === "Identifier" && callee.name === "fetch") {
          context.report({ node, messageId: "backendFetch" });
          return;
        }
        if (!callee || callee.type !== "MemberExpression") return;
        const methodName = propertyName(callee);
        if (!methodName) return;

        if (
          methodName === "fetch" &&
          callee.object.type === "Identifier" &&
          ["window", "globalThis"].includes(callee.object.name)
        ) {
          context.report({ node, messageId: "backendFetch" });
          return;
        }

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

        if (requestMethods.has(methodName) && isRequestObject(callee.object)) {
          context.report({
            node,
            messageId: "backendRequest",
            data: { method: methodName },
          });
        }
      },
    };
  },
};
