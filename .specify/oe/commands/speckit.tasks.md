If any guidance conflicts with the core SpecKit command, follow this section.

**OVERRIDE - Task Organization**: Ignore the core rule "Tasks MUST be organized
by user story". In OpenELIS, tasks are organized by **Milestone** per
Constitution Principle IX. Use Milestone Plan rows from plan.md, NOT user story
phases.

**OVERRIDE - Test Requirement**: Ignore the core rule "Tests are OPTIONAL". In
OpenELIS, tests are **MANDATORY** per Constitution Principle V (TDD). Tests must
appear before implementation tasks in each milestone.

Additional requirements:

- Use `.specify/guides/testing-roadmap.md` for test requirements and patterns.
- Each milestone must include a branch creation task first and a PR creation
  task last.
- Mark parallel milestones with `[P]` and include a milestone dependency graph.
- Reference `.specify/memory/constitution.md` Principle IX for milestone
  structure.
