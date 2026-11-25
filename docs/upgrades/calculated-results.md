# Calculated results use the builder's operation set

Calculated-result rules now use a bounded numeric and Boolean evaluator. The
server no longer executes JavaScript, and the editor no longer executes rule
text in the browser. There is no script fallback or database migration.

## Supported rules

The supported operations are decimal constants, numeric test results, patient
age in years, parentheses, `+`, `-`, `*`, `/`, `==`, `!=`, `>=`, `<=`, `&&`,
`||`, and the existing normal-range operations. Logical operators require
conditions; arithmetic requires numbers. Range operations must immediately
follow a test result. Rules are limited to 256 operations and an expression-tree
depth of 32. The editor no longer offers patient weight, which this calculation
path did not implement.

The existing result rounding, component/specimen selection, missing-operand and
missing-birth-date behavior remain in place. Existing range boundary comparisons
and their expression precedence are preserved; this change does not redefine
clinical ranges.

## Before adopting this change

Review any rules authored outside the standard builder. JavaScript calls,
assignments, arbitrary property access, and other operations outside the list
above are not supported. Replace these rules with supported operations before
using them. Do not add a script fallback to preserve an unsupported rule.

The backend rejects invalid rule saves and activation with HTTP 400. Invalid
stored rules and evaluations that produce a non-finite number are skipped and
logged with the calculation identifier. They do not overwrite an existing
result. Operators can still deactivate an invalid rule. Existing encrypted data,
installation keys, and stored result values are not migrated by this change.
