If any guidance conflicts with the core SpecKit command, follow this section.

- Use `.specify/memory/constitution.md` Principle IX to enforce milestone
  boundaries (one milestone per PR).
- Before implementation, validate scope against the current branch:
  - If on a milestone branch, only execute tasks for that milestone.
  - If on a feature branch and a Milestone Plan exists, prompt to create a
    milestone branch first.
  - Warn if scope exceeds 30 tasks or 20 files, and do not auto-proceed without
    user confirmation.
- Do not auto-advance to the next milestone. Prompt the user to create a PR
  after completing a milestone.
