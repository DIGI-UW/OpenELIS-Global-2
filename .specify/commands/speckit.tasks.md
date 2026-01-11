---
description:
  Generate an actionable, dependency-ordered tasks.md for the feature based on
  available design artifacts.
scripts:
  sh: .specify/scripts/bash/check-prerequisites.sh --json
  ps: .specify/scripts/powershell/check-prerequisites.ps1 -Json
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Outline

1. **Setup**: Run `.specify/scripts/bash/check-prerequisites.sh --json` from
   repo root and parse FEATURE_DIR and AVAILABLE_DOCS list. All paths must be
   absolute. For single quotes in args like "I'm Groot", use escape syntax: e.g
   'I'\''m Groot' (or double-quote if possible: "I'm Groot").

2. **Load design documents**: Read from FEATURE_DIR:

   - **Required**: plan.md (tech stack, libraries, structure), spec.md (user
     stories with priorities)
   - **Optional**: data-model.md (entities), contracts/ (API endpoints),
     research.md (decisions), quickstart.md (test scenarios)
   - Note: Not all projects have all documents. Generate tasks based on what's
     available.

3. **Execute task generation workflow**:

   - Load plan.md and extract:
     - Tech stack, libraries, project structure
     - **Milestone Plan table** (REQUIRED per Constitution Principle IX)
     - Milestone dependencies and parallel markers `[P]`
   - Load spec.md and extract user stories with their priorities (P1, P2, P3,
     etc.)
   - If data-model.md exists: Extract entities and map to milestones
   - If contracts/ exists: Map endpoints to milestones
   - If research.md exists: Extract decisions for setup tasks
   - **Map user stories to milestones** based on Milestone Plan scope
   - Generate tasks organized by **MILESTONE** (see Task Generation Rules below)
   - Generate Mermaid dependency graph showing milestone/PR completion order
   - Mark parallel milestones with `[P]` in section headers
   - Include branch creation as FIRST task of each milestone
   - Include PR creation as LAST task of each milestone
   - Validate task completeness (each milestone has all needed tasks,
     independently reviewable)

4. **Generate tasks.md**: Use `.specify/templates/tasks-template.md` as
   structure, fill with:

   - Correct feature name from plan.md
   - **Milestone Dependency Graph** (Mermaid diagram showing PR order)
   - **One section per Milestone** (from Milestone Plan in plan.md):
     - Branch setup task (FIRST - create milestone branch)
     - Tests for that milestone (TDD - before implementation)
     - Implementation tasks
     - PR creation task (LAST - create milestone PR)
   - **User Story to Milestone Mapping**: Each milestone header shows which user
     stories (P1, P2, etc.) it covers
   - Parallel milestones marked with `[P]` in section header
   - Clear file paths for each task
   - All tasks must follow the strict checklist format (see Task Generation
     Rules below)
   - Dependencies section showing milestone/PR completion order
   - Parallel execution examples per milestone
   - Implementation strategy section (MVP first, incremental delivery)

5. **Report**: Output path to generated tasks.md and summary:
   - Total task count
   - Task count per user story
   - Parallel opportunities identified
   - Independent test criteria for each story
   - Suggested MVP scope (typically just User Story 1)
   - Format validation: Confirm ALL tasks follow the checklist format (checkbox,
     ID, labels, file paths)

Context for task generation: $ARGUMENTS

The tasks.md should be immediately executable - each task must be specific
enough that an LLM can complete it without additional context.

## Task Generation Rules

**CRITICAL**: Tasks MUST be organized by **MILESTONE** (per Constitution
Principle IX). Each milestone maps to one or more **USER STORIES** from spec.md.

**SDD Alignment** (per [GitHub SpecKit](https://github.com/github/spec-kit)):

- Milestones are the **implementation units** (1 Milestone = 1 PR)
- User Stories are the **specification units** (from spec.md)
- Milestones should cover 1-3 user stories to keep PRs reviewable

**Tests are MANDATORY**: Per Constitution Principle V, test tasks MUST be
included and appear BEFORE implementation tasks (TDD workflow).

### Checklist Format (REQUIRED)

Every task MUST strictly follow this format:

```text
- [ ] [TaskID] [P?] [Story?] Description with file path
```

**Format Components**:

1. **Checkbox**: ALWAYS start with `- [ ]` (markdown checkbox)
2. **Task ID**: Sequential number (T001, T002, T003...) in execution order
3. **[P] marker**: Include ONLY if task is parallelizable (different files, no
   dependencies on incomplete tasks)
4. **[Story] label**: REQUIRED for user story phase tasks only
   - Format: [US1], [US2], [US3], etc. (maps to user stories from spec.md)
   - Setup phase: NO story label
   - Foundational phase: NO story label
   - User Story phases: MUST have story label
   - Polish phase: NO story label
5. **Description**: Clear action with exact file path

**Examples**:

- ✅ CORRECT: `- [ ] T001 Create project structure per implementation plan`
- ✅ CORRECT:
  `- [ ] T005 [P] Implement authentication middleware in src/middleware/auth.py`
- ✅ CORRECT: `- [ ] T012 [P] [US1] Create User model in src/models/user.py`
- ✅ CORRECT:
  `- [ ] T014 [US1] Implement UserService in src/services/user_service.py`
- ❌ WRONG: `- [ ] Create User model` (missing ID and Story label)
- ❌ WRONG: `T001 [US1] Create model` (missing checkbox)
- ❌ WRONG: `- [ ] [US1] Create User model` (missing Task ID)
- ❌ WRONG: `- [ ] T001 [US1] Create model` (missing file path)

### Task Organization

1. **From Milestones (plan.md)** - PRIMARY ORGANIZATION:

   - Each milestone from Milestone Plan table gets its own section
   - Map user stories to milestones based on scope column
   - Each milestone section header MUST include:
     - Branch name: `feat/{issue}/m{N}-{name}`
     - Type: Sequential or Parallel `[P]`
     - User Stories covered: P1, P2, etc. (from spec.md)
     - Verification criteria: What tests must pass

2. **User Story to Milestone Mapping**:

   - Read Milestone Plan "Scope" column to determine which user stories each
     milestone covers
   - A milestone typically covers 1-3 user stories (keeps PRs reviewable)
   - Backend milestones often cover backend portions of multiple stories
   - Frontend milestones often cover frontend portions of multiple stories
   - Integration milestones tie everything together

3. **From Contracts/Data Model**:

   - Map each contract/endpoint → to the milestone that implements it
   - Map each entity → to the milestone that creates it
   - Entities used by multiple milestones: Create in earliest milestone

4. **Milestone Size Validation** (PR Scope):
   - Target: 15-25 tasks per milestone
   - Maximum: 30 tasks per milestone
   - If >30 tasks: Split into sub-milestones (m1a-backend-entities,
     m1b-backend-services)

### Milestone Structure

- **Each Milestone Section** includes:

  1. Branch setup task (FIRST): `git checkout -b feat/{issue}/m{N}-{name}`
  2. Test tasks (TDD): Write failing tests
  3. Implementation tasks: Models → Services → Controllers → UI
  4. PR creation task (LAST): Create PR to target branch

- **Milestone Dependencies**:
  - Sequential: M1 → M3 (M3 depends on M1)
  - Parallel `[P]`: M1 and M2 can run simultaneously
  - Final: Polish milestone depends on all prior milestones
