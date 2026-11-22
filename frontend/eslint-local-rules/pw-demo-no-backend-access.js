/**
 * ESLint rule: pw-demo-no-backend-access
 *
 * Demo specs validate visible UI state. They cannot observe, call, poll, or
 * replace backend traffic. See .specify/guides/playwright-best-practices.md.
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
  if (
    [
      "AwaitExpression",
      "ChainExpression",
      "TSAsExpression",
      "TSNonNullExpression",
      "TSTypeAssertion",
    ].includes(node.type)
  ) {
    return unwrapExpression(node.argument || node.expression);
  }
  return node;
}

function isFunction(node) {
  return [
    "ArrowFunctionExpression",
    "FunctionDeclaration",
    "FunctionExpression",
  ].includes(node?.type);
}

function isFunctionParameterPattern(pattern) {
  return isFunction(pattern?.parent) && pattern.parent.params.includes(pattern);
}

export default {
  meta: {
    type: "problem",
    docs: {
      description:
        "Demo specs must be UI-only: no network listeners, backend " +
        "requests or polling, and no network stubs.",
    },
    schema: [],
    messages: {
      consoleListener:
        "Demo specs must not listen to '{{ event }}' events. Demos " +
        "validate visible UI state, not console chatter.",
      networkListener:
        "Demo specs must not listen to '{{ event }}' network events. " +
        "Assert the resulting visible UI state.",
      waitForResponse:
        "Demo specs must not use `waitForResponse`. Synchronize via " +
        "UI assertions (`toBeVisible`, `toHaveText`, `toHaveURL`).",
      waitForRequest:
        "Demo specs must not use `waitForRequest`. Synchronize via " +
        "UI assertions (`toBeVisible`, `toHaveText`, `toHaveURL`).",
      backendRequest:
        "Demo specs must not call a Playwright request API (`{{ method }}`). " +
        "Demos record the user's visible journey, not backend calls.",
      backendRequestAccess:
        "Demo specs must not pass or retain a Playwright request client. " +
        "Drive the workflow through visible controls.",
      backendFetch:
        "Demo specs must not call browser `fetch`. Drive the workflow " +
        "through visible controls and assert visible outcomes.",
      backendPoll:
        "Demo specs must not use `expect.poll`. Synchronize through " +
        "Playwright's web-first visible UI assertions.",
      arbitraryWait:
        "Demo specs must not use `waitForTimeout`. Synchronize through " +
        "Playwright's web-first visible UI assertions.",
      forcedAction:
        "Demo specs must not use `{ force: true }`. Interact with the " +
        "visible, actionable control.",
      unresolvedLocalImport:
        "Demo specs must use a resolvable static local runtime import so " +
        "the UI-only dependency guard can inspect it.",
      networkStub:
        "Demo specs must not stub or intercept network traffic. They must " +
        "exercise the deployed user workflow.",
    },
  },
  create(context) {
    const sourceCode = context.sourceCode;
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
    const networkEvents = new Set([
      "request",
      "requestfailed",
      "requestfinished",
      "response",
    ]);
    const locatorActionsWithForce = new Set([
      "check",
      "click",
      "dblclick",
      "dragTo",
      "fill",
      "hover",
      "selectOption",
      "setChecked",
      "tap",
      "uncheck",
    ]);
    const locatorFactories = new Set([
      "frameLocator",
      "getByAltText",
      "getByLabel",
      "getByPlaceholder",
      "getByRole",
      "getByTestId",
      "getByText",
      "getByTitle",
      "locator",
    ]);
    const locatorRefinements = new Set([
      "and",
      "filter",
      "first",
      "last",
      "nth",
      "or",
    ]);

    function findVariable(identifier) {
      let scope = sourceCode.getScope(identifier);
      while (scope) {
        const variable = scope.set.get(identifier.name);
        if (variable) return variable;
        scope = scope.upper;
      }
      return null;
    }

    function fixtureKey(variable) {
      for (const identifier of variable?.identifiers || []) {
        const property = identifier.parent;
        const pattern = property?.parent;
        if (
          property?.type === "Property" &&
          pattern?.type === "ObjectPattern" &&
          isFunctionParameterPattern(pattern)
        ) {
          return getStringLiteralValue(property.key) || property.key?.name;
        }
      }
      return null;
    }

    function importedPlaywrightName(variable) {
      for (const definition of variable?.defs || []) {
        if (
          definition.type === "ImportBinding" &&
          definition.parent?.source?.value === "@playwright/test"
        ) {
          return (
            definition.node?.imported?.name || definition.node?.local?.name
          );
        }
      }
      return null;
    }

    function variableInitializers(variable) {
      const declarationInitializers = (variable?.defs || [])
        .filter(
          (definition) =>
            definition.type === "Variable" && definition.node?.init,
        )
        .map((definition) => definition.node.init);
      const assignmentInitializers = (variable?.references || [])
        .map((reference) => reference.writeExpr)
        .filter(Boolean);
      return [...declarationInitializers, ...assignmentInitializers];
    }

    function isPageClient(node, visited = new Set()) {
      const expression = unwrapExpression(node);
      if (!expression) return false;
      if (expression.type !== "Identifier") return false;

      const variable = findVariable(expression);
      if (!variable) return expression.name === "page";
      if (visited.has(variable)) return false;
      visited.add(variable);

      if (fixtureKey(variable) === "page") return true;
      if (
        expression.name === "page" &&
        variable.defs.some((definition) => definition.type === "Parameter")
      ) {
        return true;
      }
      return variableInitializers(variable).some((initializer) =>
        isPageClient(initializer, visited),
      );
    }

    function isBrowserContext(node, visited = new Set()) {
      const expression = unwrapExpression(node);
      if (!expression) return false;
      if (
        expression.type === "CallExpression" &&
        getStaticPropertyName(expression.callee) === "context" &&
        expression.callee.type === "MemberExpression" &&
        isPageClient(expression.callee.object)
      ) {
        return true;
      }
      if (expression.type !== "Identifier") return false;

      const variable = findVariable(expression);
      if (!variable) {
        return ["browserContext", "context"].includes(expression.name);
      }
      if (visited.has(variable)) return false;
      visited.add(variable);

      if (fixtureKey(variable) === "context") return true;
      if (
        ["browserContext", "context"].includes(expression.name) &&
        variable.defs.some((definition) => definition.type === "Parameter")
      ) {
        return true;
      }
      return variableInitializers(variable).some((initializer) =>
        isBrowserContext(initializer, visited),
      );
    }

    function isNetworkOwner(node) {
      return isPageClient(node) || isBrowserContext(node);
    }

    function isLocator(node, visited = new Set()) {
      const expression = unwrapExpression(node);
      if (!expression) return false;
      if (
        expression.type === "CallExpression" &&
        expression.callee.type === "MemberExpression"
      ) {
        const method = getStaticPropertyName(expression.callee);
        if (
          locatorFactories.has(method) &&
          (isPageClient(expression.callee.object) ||
            isLocator(expression.callee.object, visited))
        ) {
          return true;
        }
        if (
          locatorRefinements.has(method) &&
          isLocator(expression.callee.object, visited)
        ) {
          return true;
        }
      }
      if (expression.type !== "Identifier") return false;

      const variable = findVariable(expression);
      if (!variable || visited.has(variable)) return false;
      visited.add(variable);
      return variableInitializers(variable).some((initializer) =>
        isLocator(initializer, visited),
      );
    }

    function isDirectRequestAccess(node) {
      return (
        node?.type === "MemberExpression" &&
        getStaticPropertyName(node) === "request" &&
        isNetworkOwner(node.object)
      );
    }

    function isRequestClient(node, visited = new Set()) {
      const expression = unwrapExpression(node);
      if (!expression) return false;
      if (isDirectRequestAccess(expression)) return true;
      if (
        expression.type === "CallExpression" &&
        expression.callee.type === "MemberExpression" &&
        getStaticPropertyName(expression.callee) === "newContext" &&
        isRequestClient(expression.callee.object, visited)
      ) {
        return true;
      }
      if (expression.type !== "Identifier") return false;

      const variable = findVariable(expression);
      if (!variable) return expression.name === "request";
      if (visited.has(variable)) return false;
      visited.add(variable);

      if (fixtureKey(variable) === "request") return true;
      if (importedPlaywrightName(variable) === "request") return true;
      const destructuredFromPage = (variable.defs || []).some((definition) => {
        const declarator = definition.node;
        if (
          definition.type !== "Variable" ||
          declarator?.id?.type !== "ObjectPattern" ||
          !isPageClient(declarator.init)
        ) {
          return false;
        }
        return declarator.id.properties.some((property) => {
          if (property.type !== "Property") return false;
          const propertyName =
            getStringLiteralValue(property.key) || property.key?.name;
          const localName = property.value?.name;
          return propertyName === "request" && localName === expression.name;
        });
      });
      if (destructuredFromPage) return true;
      return variableInitializers(variable).some((initializer) =>
        isRequestClient(initializer, visited),
      );
    }

    function requestMethodAlias(identifier) {
      const variable = findVariable(identifier);
      for (const definition of variable?.defs || []) {
        const declarator = definition.node;
        if (
          definition.type !== "Variable" ||
          declarator?.id?.type !== "ObjectPattern" ||
          !isRequestClient(declarator.init)
        ) {
          continue;
        }
        for (const property of declarator.id.properties) {
          if (property.type !== "Property") continue;
          const localName = property.value?.name;
          const method =
            getStringLiteralValue(property.key) || property.key?.name;
          if (localName === identifier.name && requestMethods.has(method)) {
            return method;
          }
        }
      }
      return null;
    }

    function isBrowserFetch(node, visited = new Set()) {
      const expression = unwrapExpression(node);
      if (!expression) return false;
      if (
        expression.type === "MemberExpression" &&
        getStaticPropertyName(expression) === "fetch" &&
        expression.object.type === "Identifier" &&
        ["globalThis", "self", "window"].includes(expression.object.name)
      ) {
        return true;
      }
      if (expression.type !== "Identifier") return false;

      const variable = findVariable(expression);
      if (!variable) return expression.name === "fetch";
      if (visited.has(variable)) return false;
      visited.add(variable);
      return variableInitializers(variable).some((initializer) =>
        isBrowserFetch(initializer, visited),
      );
    }

    function isExpectPoll(node, visited = new Set()) {
      const expression = unwrapExpression(node);
      if (!expression) return false;
      if (
        expression.type === "MemberExpression" &&
        getStaticPropertyName(expression) === "poll" &&
        expression.object.type === "Identifier" &&
        expression.object.name === "expect"
      ) {
        return true;
      }
      if (expression.type !== "Identifier") return false;

      const variable = findVariable(expression);
      if (!variable || visited.has(variable)) return false;
      visited.add(variable);
      return variableInitializers(variable).some((initializer) =>
        isExpectPoll(initializer, visited),
      );
    }

    function usesForceOption(node) {
      return node.arguments.some(
        (argument) =>
          argument.type === "ObjectExpression" &&
          argument.properties.some(
            (property) =>
              property.type === "Property" &&
              (getStringLiteralValue(property.key) || property.key?.name) ===
                "force" &&
              property.value.type === "Literal" &&
              property.value.value === true,
          ),
      );
    }

    function isDefinitionIdentifier(node) {
      const parent = node.parent;
      if (!parent) return false;
      if (
        [
          "ImportDefaultSpecifier",
          "ImportNamespaceSpecifier",
          "ImportSpecifier",
        ].includes(parent.type)
      ) {
        return true;
      }
      if (parent.type === "VariableDeclarator" && parent.id === node)
        return true;
      if (parent.type === "AssignmentExpression" && parent.left === node) {
        return true;
      }
      return (
        parent.type === "Property" && parent.parent?.type === "ObjectPattern"
      );
    }

    return {
      Identifier(node) {
        if (
          isDefinitionIdentifier(node) ||
          (node.parent?.type === "MemberExpression" &&
            (node.parent.object === node ||
              (!node.parent.computed && node.parent.property === node)))
        ) {
          return;
        }
        if (isRequestClient(node)) {
          context.report({ node, messageId: "backendRequestAccess" });
        }
      },
      MemberExpression(node) {
        if (!isDirectRequestAccess(node)) return;
        const parent = node.parent;
        const isCalledRequestMethod =
          parent?.type === "MemberExpression" &&
          parent.object === node &&
          requestMethods.has(getStaticPropertyName(parent)) &&
          parent.parent?.type === "CallExpression" &&
          parent.parent.callee === parent;
        if (!isCalledRequestMethod) {
          context.report({ node, messageId: "backendRequestAccess" });
        }
      },
      CallExpression(node) {
        const callee = node.callee;
        if (!callee) return;

        if (callee.type === "Identifier") {
          if (isBrowserFetch(callee)) {
            context.report({ node, messageId: "backendFetch" });
            return;
          }
          if (isExpectPoll(callee)) {
            context.report({ node, messageId: "backendPoll" });
            return;
          }
          const method = requestMethodAlias(callee);
          if (method) {
            context.report({
              node,
              messageId: "backendRequest",
              data: { method },
            });
          }
          return;
        }

        if (callee.type !== "MemberExpression") return;
        const methodName = getStaticPropertyName(callee);
        if (!methodName) return;

        if (
          locatorActionsWithForce.has(methodName) &&
          (isPageClient(callee.object) || isLocator(callee.object)) &&
          usesForceOption(node)
        ) {
          context.report({ node, messageId: "forcedAction" });
          return;
        }

        if (
          ["on", "once"].includes(methodName) &&
          isNetworkOwner(callee.object)
        ) {
          const event = getStringLiteralValue(node.arguments[0]);
          if (event === "console" || event === "pageerror") {
            context.report({
              node,
              messageId: "consoleListener",
              data: { event },
            });
            return;
          }
          if (networkEvents.has(event)) {
            context.report({
              node,
              messageId: "networkListener",
              data: { event },
            });
            return;
          }
        }

        if (methodName === "waitForResponse" && isPageClient(callee.object)) {
          context.report({ node, messageId: "waitForResponse" });
          return;
        }
        if (methodName === "waitForRequest" && isPageClient(callee.object)) {
          context.report({ node, messageId: "waitForRequest" });
          return;
        }
        if (methodName === "waitForTimeout" && isPageClient(callee.object)) {
          context.report({ node, messageId: "arbitraryWait" });
          return;
        }

        if (isBrowserFetch(callee)) {
          context.report({ node, messageId: "backendFetch" });
          return;
        }

        if (isExpectPoll(callee)) {
          context.report({ node, messageId: "backendPoll" });
          return;
        }

        if (
          ["route", "routeFromHAR"].includes(methodName) &&
          isNetworkOwner(callee.object)
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
      ImportExpression(node) {
        if (getStringLiteralValue(node.source) !== null) return;
        context.report({ node, messageId: "unresolvedLocalImport" });
      },
    };
  },
};
