/**
 * ESLint rule: no-inline-route-component
 *
 * Bans a per-render function as a react-router `component` prop; use `render`.
 *
 * react-router renders the `component` prop with `React.createElement`, so a
 * function built during a render is a new component TYPE on every render of
 * whatever built the route element. React then unmounts the old subtree and
 * mounts a fresh one, losing every piece of the page's state. `render` calls
 * the function instead, so the element type stays whatever the function returns
 * and React reconciles.
 *
 * This cost the WHONET export page its filters: App.jsx builds its route table
 * inside a parent `render` callback, so every `history.replace` the page made
 * to put its own filters in the query string rebuilt the route table, remounted
 * the page, and reset the filters it had just written. It surfaced as a block
 * of microbiology E2E failures, not as anything visible in a unit test.
 *
 * Two shapes are reported: the function literal written in the attribute, and
 * an identifier bound to a function literal declared inside a function, which
 * is the same fresh identity per render one `const` away. A module-scope
 * binding is the pattern to move TO and is never reported.
 *
 * What it still does not see: an identity produced by a call rather than a
 * literal (`React.memo(...)`, `useMemo`) or arriving as a parameter, neither of
 * which says whether the value is stable across renders; and routes written in
 * .ts/.tsx, which eslint.config.js does not register this rule for.
 */
export default {
  meta: {
    type: "problem",
    docs: {
      description:
        "Require `render` in place of a per-render function `component` prop " +
        "on a route, which remounts the page on every parent render.",
    },
    schema: [],
    messages: {
      useRender:
        "Use `render={() => ...}` instead of `component={() => ...}` on a " +
        "route. react-router calls React.createElement on `component`, so an " +
        "inline arrow is a new component type every render and React remounts " +
        "the page, discarding its state.",
      perRenderBinding:
        "`{{name}}` is declared inside the function that renders this route, " +
        "so it is a new component type every render and React remounts the " +
        "page, discarding its state. Move the declaration to module scope, or " +
        "pass it through `render={() => <{{name}} />}`.",
    },
  },
  create(context) {
    const sourceCode = context.sourceCode ?? context.getSourceCode();

    // Where the binding lives decides whether its value survives a render:
    // module and global bindings are evaluated once, anything inside a
    // function is rebuilt on every call of that function.
    const rebuiltEachCall = (variable) =>
      !["module", "global"].includes(variable.scope.variableScope.type);

    const holdsAFunctionLiteral = (variable) =>
      variable.defs.some(
        (def) =>
          def.type === "FunctionName" ||
          (def.type === "Variable" &&
            ["ArrowFunctionExpression", "FunctionExpression"].includes(
              def.node.init?.type,
            )),
      );

    // Resolve through the reference the scope analyser already recorded, so
    // shadowing and hoisting are its problem rather than a name lookup's.
    function resolve(identifier) {
      for (
        let scope = sourceCode.getScope(identifier);
        scope;
        scope = scope.upper
      ) {
        const reference = scope.references.find(
          (candidate) => candidate.identifier === identifier,
        );
        if (reference) return reference.resolved;
      }
      return null;
    }

    return {
      JSXAttribute(node) {
        if (node.name?.type !== "JSXIdentifier") return;
        if (node.name.name !== "component") return;
        const value = node.value;
        if (value?.type !== "JSXExpressionContainer") return;
        const expression = value.expression;
        if (
          expression?.type === "ArrowFunctionExpression" ||
          expression?.type === "FunctionExpression"
        ) {
          context.report({ node, messageId: "useRender" });
          return;
        }
        if (expression?.type !== "Identifier") return;
        const variable = resolve(expression);
        if (!variable) return;
        if (!rebuiltEachCall(variable)) return;
        if (!holdsAFunctionLiteral(variable)) return;
        context.report({
          node,
          messageId: "perRenderBinding",
          data: { name: expression.name },
        });
      },
    };
  },
};
