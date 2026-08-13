/**
 * ESLint rule: pw-demo-no-backend-access
 *
 * Demo specs validate *visible UI state*, not logs or backend calls.
 * They must not:
 *
 *   1. Listen to page 'console' or 'pageerror' events.
 *   2. Synchronize through responses or backend polling.
 *   3. Call request APIs or browser `fetch`.
 *   4. Stub network traffic.
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

function getStaticPropertyName(node) {
  if (!node || node.type !== "MemberExpression") return null;
  if (!node.computed && node.property.type === "Identifier") {
    return node.property.name;
  }
  return getStringLiteralValue(node.property);
}

function unwrapExpression(node) {
  if (!node) return node;
  if (node.type === "AwaitExpression" || node.type === "ChainExpression") {
    return unwrapExpression(node.argument || node.expression);
  }
  return node;
}

function isPlaywrightNetworkOwner(node) {
  const expression = unwrapExpression(node);
  if (!expression) return false;
  if (expression.type === "Identifier") {
    return ["browserContext", "context", "page"].includes(expression.name);
  }
  return (
    expression.type === "CallExpression" &&
    getStaticPropertyName(expression.callee) === "context"
  );
}

export default {
  meta: {
    type: "problem",
    docs: {
      description:
        "Demo specs must be UI-only: no console/pageerror listeners, " +
        "no backend requests or polling, and no network stubs.",
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
        "Demo specs must not call a Playwright request API (`{{ method }}`). " +
        "Demos record the user's visible journey, not backend calls.",
      backendFetch:
        "Demo specs must not call browser `fetch`. Drive the workflow " +
        "through visible controls and assert visible outcomes.",
      backendPoll:
        "Demo specs must not use `expect.poll` to inspect backend state. " +
        "Synchronize through visible UI assertions.",
      networkStub:
        "Demo specs must not stub or intercept network traffic. They must " +
        "exercise the deployed user workflow.",
    },
  },
  create(context) {
    const requestAliases = new Set(["request"]);
    const requestMethods = new Set([
      "delete",
      "fetch",
      "get",
      "head",
      "newContext",
      "patch",
      "post",
      "put",
    ]);

    function isRequestClient(node) {
      const expression = unwrapExpression(node);
      if (!expression) return false;
      if (expression.type === "Identifier") {
        return requestAliases.has(expression.name);
      }
      if (expression.type === "MemberExpression") {
        return getStaticPropertyName(expression) === "request";
      }
      if (expression.type === "CallExpression") {
        const method = getStaticPropertyName(expression.callee);
        return (
          method === "newContext" &&
          expression.callee.type === "MemberExpression" &&
          isRequestClient(expression.callee.object)
        );
      }
      return false;
    }

    function recordRequestAlias(target, source) {
      if (target?.type === "Identifier" && isRequestClient(source)) {
        requestAliases.add(target.name);
      }
    }

    return {
      Property(node) {
        if (
          node.parent?.type === "ObjectPattern" &&
          getStringLiteralValue(node.key) === null &&
          node.key?.type === "Identifier" &&
          node.key.name === "request" &&
          node.value?.type === "Identifier"
        ) {
          requestAliases.add(node.value.name);
        }
      },
      VariableDeclarator(node) {
        recordRequestAlias(node.id, node.init);
      },
      AssignmentExpression(node) {
        recordRequestAlias(node.left, node.right);
      },
      CallExpression(node) {
        const callee = node.callee;
        if (!callee) return;

        if (callee.type === "Identifier" && callee.name === "fetch") {
          context.report({ node, messageId: "backendFetch" });
          return;
        }

        if (callee.type !== "MemberExpression") return;
        const methodName = getStaticPropertyName(callee);
        if (!methodName) return;

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

        if (
          methodName === "fetch" &&
          callee.object.type === "Identifier" &&
          ["globalThis", "self", "window"].includes(callee.object.name)
        ) {
          context.report({ node, messageId: "backendFetch" });
          return;
        }

        if (
          methodName === "poll" &&
          callee.object.type === "Identifier" &&
          callee.object.name === "expect"
        ) {
          context.report({ node, messageId: "backendPoll" });
          return;
        }

        if (
          ["route", "routeFromHAR"].includes(methodName) &&
          isPlaywrightNetworkOwner(callee.object)
        ) {
          context.report({ node, messageId: "networkStub" });
          return;
        }

        if (requestMethods.has(methodName) && isRequestClient(callee.object)) {
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
