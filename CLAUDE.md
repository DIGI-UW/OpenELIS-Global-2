# CLAUDE.md - Claude Code CLI Instructions

> **For Claude Code Users:** This file contains Claude-specific instructions.
> For comprehensive project context, **read [AGENTS.md](AGENTS.md) first.**

---

## Documentation Hierarchy

When working on this project, follow this documentation order:

1. **[constitution.md](.specify/memory/constitution.md)** - AUTHORITATIVE
   governance (v1.7.0, 8 core principles)
2. **[AGENTS.md](AGENTS.md)** - Comprehensive agent onboarding (works for ALL AI
   tools)
3. **[quickstart.md](specs/001-sample-storage/quickstart.md)** - Step-by-step
   feature development example
4. **[README.md](README.md)** - Human-facing project overview
5. **CLAUDE.md** - Claude-specific notes (this file)

**In case of conflict:** Constitution > AGENTS.md > Other docs

---

## GitHub SpecKit Integration

This project uses **GitHub SpecKit** for Specification-Driven Development (SDD).

### Available Slash Commands

- `/speckit.specify` - Create/update feature specification from description
- `/speckit.clarify` - Identify underspecified areas (max 5 clarification
  questions)
- `/speckit.plan` - Generate implementation plan with constitution check and
  research
- `/speckit.tasks` - Generate actionable, dependency-ordered tasks.md
- `/speckit.implement` - Execute implementation plan (process tasks.md)
- `/speckit.analyze` - Cross-artifact consistency analysis
- `/speckit.constitution` - Create/update project constitution
- `/speckit.checklist` - Generate custom quality validation checklist

### Standard Workflow

1. `/speckit.specify "Feature description"` → Creates
   `specs/{###-feature-name}/spec.md`
2. `/speckit.clarify` → Resolves ambiguities (max 3 rounds recommended)
3. `/speckit.plan` → Creates `plan.md` with architecture, research, constitution
   check
4. `/speckit.tasks` → Creates `tasks.md` with dependency-ordered task breakdown
5. `/speckit.implement` → Executes tasks using TDD workflow
6. `/speckit.analyze` → Validates consistency across spec/plan/tasks

---

## Critical Reminders (Claude-Specific)

### Test Skipping (CRITICAL)

**MUST use BOTH flags** when skipping tests:

```bash
# CORRECT (skips ALL tests including Surefire and Failsafe)
mvn clean install -DskipTests -Dmaven.test.skip=true

# WRONG (only skips Surefire, Failsafe integration tests still run)
mvn clean install -DskipTests
```

**Why both flags?**

- `-DskipTests`: Skips Surefire unit test execution
- `-Dmaven.test.skip=true`: Skips test compilation AND execution (including
  Failsafe)

### Pre-Commit Formatting (MANDATORY)

**MUST run BEFORE EVERY commit:**

```bash
# Backend formatting
mvn spotless:apply

# Frontend formatting
cd frontend && npm run format && cd ..
```

### Constitution Compliance (MANDATORY)

**ALWAYS check [constitution.md](.specify/memory/constitution.md) BEFORE
implementing features.**

Key principles to verify:

- [ ] Layered architecture (5-layer pattern:
      Valueholder→DAO→Service→Controller→Form)
- [ ] Carbon Design System (NO Bootstrap/Tailwind)
- [ ] FHIR R4 compliance (for external-facing entities)
- [ ] React Intl (NO hardcoded strings)
- [ ] Test-Driven Development (TDD workflow)
- [ ] Liquibase for schema changes
- [ ] @Transactional in services ONLY (NOT controllers)
- [ ] Services compile all data within transaction (prevent
      LazyInitializationException)

### TDD Workflow (MANDATORY for SpecKit)

When using `/speckit.implement`, follow **Red-Green-Refactor** cycle:

1. **Red:** Write failing test first
2. **Green:** Write minimal code to make test pass
3. **Refactor:** Improve code quality while keeping tests green

### Individual E2E Test Execution (Constitution V.5)

**Development:** Run tests INDIVIDUALLY (NOT full suite)

```bash
# CORRECT (individual test file during development)
npm run cy:run -- --spec "cypress/e2e/{feature}.cy.js"

# WRONG (full suite, only for CI/CD)
npm run cy:run
```

**Why?** Faster feedback (5 minutes vs 15+ minutes), easier debugging

---

## Quick Links

- **Constitution:**
  [.specify/memory/constitution.md](.specify/memory/constitution.md)
- **Agent Onboarding:** [AGENTS.md](AGENTS.md)
- **Project Overview:** [README.md](README.md)
- **Contributing:** [CONTRIBUTING.md](CONTRIBUTING.md)
- **PR Guidelines:** [PULL_REQUEST_TIPS.md](PULL_REQUEST_TIPS.md)
- **Example Feature:** [specs/001-sample-storage/](specs/001-sample-storage/)

---

**Last Updated:** 2025-11-09 **Constitution Version:** 1.7.0
