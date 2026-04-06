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

**Setup:** Run `python3 scripts/install-agent-skills.py` to install slash
commands and packaged skills.

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

**Exception — CI shared-build root project:** The E2E `shared-build` step in
both `e2e-playwright.yml` and `e2e-fork-pr.yml` intentionally omits
`-Dmaven.test.skip=true` on the root project build because the `test-jar`
artifact must be produced for plugin compilation (GenericASTM, GenericFile,
GenericHL7 depend on it). The `dataexport` and `plugins` sub-builds still use
both flags.

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
- [ ] React Intl (NO hardcoded strings, new keys in `en.json` ONLY — Transifex
      is source of truth for non-English translations)
- [ ] Test-Driven Development (TDD workflow)
- [ ] Liquibase for schema changes
- [ ] @Transactional in services ONLY (NOT controllers)
- [ ] Services compile all data within transaction (prevent
      LazyInitializationException)
- [ ] Test Quality Invariants V.6 (Inversion Test, no assert-on-mock-return,
      auth ordering tests)

### TDD Workflow (MANDATORY for SpecKit)

When using `/speckit.implement`, follow **Red-Green-Refactor** cycle:

1. **Red:** Write failing test first
2. **Green:** Write minimal code to make test pass
3. **Refactor:** Improve code quality while keeping tests green

### Post-Compaction Context Recovery (MANDATORY)

**After any context compaction or session resume**, run these commands FIRST —
before reading files, editing code, or starting analysis:

```bash
# 1. Discover all active worktrees and their branches
git worktree list

# 2. Check status of each relevant worktree
git status  # (in each worktree path)

# 3. List open PRs and their branches
gh pr list --author @me
```

**Why:** Compaction drops operational state (active worktrees, open PRs, CI
status). These commands reconstruct the full dev context in seconds. Without
this, work targets the wrong branch/directory.

### Cypress E2E — DEPRECATED

> **Do not create new Cypress tests.** See [AGENTS.md](AGENTS.md) "E2E Tests
> (Cypress) — DEPRECATED" for existing test maintenance scripts and execution
> constraints.

### Playwright E2E — RECOMMENDED

> See [AGENTS.md](AGENTS.md) "E2E Tests (Playwright)" for the full execution
> contract, scripts, and project descriptions. Key invariant: always use
> `npm run pw:test` scripts, never raw `npx playwright test`.

### Playwright Anti-Patterns (CRITICAL)

**DO NOT** introduce these patterns — they cause flaky tests:

1. **`response.ok()` as pass/fail** — Use `waitForResponse` for sync only, then
   assert on visible UI state (`toBeVisible`, `toHaveURL`, `toHaveText`)
2. **`{ force: true }` on Carbon inputs** — Click the `<label>` instead; Carbon
   hides `<input>` elements with `visually-hidden`
3. **`.catch(() => false)` on `isVisible()`** — `isVisible()` already returns
   boolean; the catch hides real errors
4. **`isVisible({ timeout: N })`** — The timeout parameter is deprecated and
   ignored; use `expect(el).toBeVisible({ timeout: N })` for waiting

**Full guide:** `.specify/guides/playwright-best-practices.md` **Quality
report:** `.specify/guides/playwright-e2e-quality-report.md`

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

## Active Technologies

- Java 21 LTS (OpenJDK/Temurin) + React 17 (JavaScript) (005-eqa-module)
- PostgreSQL 14+ via JPA/Hibernate, Liquibase 4.8.0 for migrations
  (005-eqa-module)

**Last Updated:** 2026-04-05 **Constitution Version:** 1.9.1

## Recent Changes

- 005-eqa-module: Added Java 21 LTS (OpenJDK/Temurin) + React 17 (JavaScript)
