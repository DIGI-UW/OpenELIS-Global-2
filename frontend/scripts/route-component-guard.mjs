// Fails the build on local/no-inline-route-component.
//
// The blocking lint step runs `eslint playwright/` only; the step that reaches
// src/ ends in `|| exit 0` under continue-on-error, so an error the rule reports
// there is printed and discarded. This runs the project's own ESLint config over
// src/ and exits non-zero on that one rule id, leaving the rest of src/'s lint
// debt where the advisory step already tracks it.
//
// Scope follows eslint.config.js, which registers the rule for JS/JSX only: a
// route declared in a .ts/.tsx file is not covered.

import { ESLint } from "eslint";

const RULE = "local/no-inline-route-component";

const results = await new ESLint().lintFiles(["src/"]);
const hits = results.flatMap((file) =>
  file.messages
    .filter((message) => message.ruleId === RULE)
    .map(
      (message) =>
        `${file.filePath}:${message.line}:${message.column}  ${message.message}`,
    ),
);

if (hits.length) {
  console.error(hits.join("\n"));
  process.exit(1);
}
console.log(`${RULE}: clean across src/`);
