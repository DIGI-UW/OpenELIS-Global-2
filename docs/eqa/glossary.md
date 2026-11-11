# EQA vocabulary

One word per thing. Three names for one object shipped side by side before this
was written down, and a user could not tell whether creating the long-lived
arrangement, joining it, and configuring result submission were three things or
one.

Use these words in page titles, column headers, helper text, error messages and
endpoint documentation. Table, route and endpoint names are **not** covered:
renaming those is a migration, and the vocabulary problem is a copy problem.

## The objects

| Word to use                      | Table                        | What it is                                                                                                          |
| -------------------------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| **Scheme**                       | `eqa_program`                | The long-lived arrangement a provider runs: HIV viral load PT, and so on. Not "program" or "programme" in new copy. |
| **Scheme enrollment**            | `eqa_program_enrollment`     | A laboratory's membership of a scheme, recorded by the provider.                                                    |
| **Cycle participant**            | `eqa_cycle_participant`      | A laboratory selected into one particular cycle of a scheme.                                                        |
| **Lab submission configuration** | `eqa_lab_program_enrollment` | This laboratory's own record of what it submits and how. Local to the participant, not the provider's view of them. |
| **Cycle**                        | `eqa_cycle`                  | One round of a scheme.                                                                                              |

**Two tables surface as "enrollment" and they are different things.**
`eqa_program_enrollment` is the provider's roster; `eqa_lab_program_enrollment`
is the participant's own configuration. Always say which.

## Panel means two unrelated things

| Word to use            | What it is                                                                                                                                      | Where it appears                                        |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
| **Test panel**         | The standard bundle from the test catalogue, served by `/rest/displayList/PANELS`                                                               | My Schemes, the enrollment form                         |
| **EQA material panel** | `eqa_panel` — physical material with a source type, lot, vendor certificate, aliquots produced, reserved and shipped, and a storage temperature | Provider prep, in-house blinding, shipments, pack lists |

A participant meets both: they choose **test panels** when they enrol, and they
receive an **EQA material panel** when a cycle ships. Never label a control
"Panel" on a screen where both senses are reachable.

## Scope of the change that introduced this

Applied to the participant and administration lanes, whose titles disagreed with
the provider lane. The provider lane already used "scheme" and was left alone.
Deeper strings — cycle statuses, panel source and storage vocabularies, wizard
step labels — were **not** rewritten: they are unambiguous in context, and every
changed English string sends the other languages back to Transifex, so churn has
a real cost.
