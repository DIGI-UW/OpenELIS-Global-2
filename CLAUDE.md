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

**Setup:** Run `python scripts/install-speckit-commands.py` to install slash
commands.

**Full documentation:** See [AGENTS.md](AGENTS.md) § "GitHub SpecKit
Integration" for:

- Available commands (`/speckit.specify`, `/speckit.plan`, etc.)
- Standard workflow
- Command installation options

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

### Cypress E2E Test Execution (CRITICAL for Claude Code Environment)

**IMPORTANT:** In Claude Code CLI environment, `ELECTRON_RUN_AS_NODE=1` is set,
which breaks Cypress. All `npm run cy:*` scripts include
`unset ELECTRON_RUN_AS_NODE` to work around this. **ALWAYS use the npm scripts,
NOT direct `npx cypress` commands.**

**Available Scripts (use these, not direct cypress commands):**

```bash
# Run specific test file
npm run cy:spec "cypress/e2e/home.cy.js"

# Run all admin tests
npm run cy:admin

# Run all analyzer tests
npm run cy:analyzer

# Run full suite (development)
npm run cy:run

# Run full suite with fail-fast (stops on first failure)
npm run cy:failfast

# Run specific test with fail-fast
npm run cy:failfast:spec "cypress/e2e/AdminE2E/organizationManagement.cy.js"

# Open Cypress UI (interactive mode)
npm run cy:open
```

**Three-Phase Workflow (Constitution V.5):**

1. **During Development:** Run individual tests (`npm run cy:spec "..."`)
2. **Before Pushing (MANDATORY):** Run full suite (`npm run cy:failfast`)
3. **In CI/CD:** Automatic via GitHub Actions

**Anti-Pattern:** Running only individual tests, pushing, and waiting for CI.
This wastes 60+ minutes of CI time.

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

**Last Updated:** 2026-01-27 **Constitution Version:** 1.9.0
