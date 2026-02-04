# Navbar Extraction Plan

**Date**: 2025-01-XX  
**Jira Issue**:
[OGC-232](https://uwdigi.atlassian.net/jira/software/c/projects/OGC/boards/51?assignee=5e765a025e755d0cd425c863&selectedIssue=OGC-232)  
**Current Branch**: `nav-branch-pre-merge`  
**Target Branch**: `feat/009-ogc-232-carbon-sidenav` (to be created from
`origin/develop`)

## Executive Summary

This branch (`nav-branch-pre-merge`) contains **two major feature sets**:

1. **Navbar/Sidenav refactor** (spec 009-carbon-sidenav, Jira OGC-232) - **TO
   EXTRACT**
2. **Analyzer feature work** - **TO KEEP ON CURRENT BRANCH**

We need to extract only the navbar-related changes into a clean branch based on
`origin/develop`, leaving the analyzer work on the current branch.

## Current State Analysis

### Branch Statistics

- **Total files changed**: ~297 files
- **Total insertions**: ~52,614 lines
- **Total deletions**: ~2,388 lines
- **Commits**: ~20 commits (mostly navbar fixes and analyzer work mixed)

### Navbar-Related Files (TO EXTRACT)

#### Core Components

- `frontend/src/components/layout/Header.js` (M - modified)
- `frontend/src/components/layout/Layout.js` (M - modified)
- `frontend/src/components/layout/AnalyzerLayout.js` (A - added, uses navbar)
- `frontend/src/components/layout/AnalyzerLayout.css` (A - added)
- `frontend/src/components/layout/index.js` (A - added)

#### Custom Hooks

- `frontend/src/components/layout/useSideNavPreference.js` (A - added)
- `frontend/src/components/layout/useMenuAutoExpand.js` (A - added)

#### Tests

- `frontend/src/components/layout/Header.test.js` (A - added)
- `frontend/src/components/layout/Layout.test.js` (A - added)
- `frontend/src/components/layout/Layout.integration.test.js` (A - added)
- `frontend/src/components/layout/useSideNavPreference.test.js` (A - added)
- `frontend/src/components/layout/useMenuAutoExpand.test.js` (A - added)

#### E2E Tests

- `frontend/cypress/e2e/sidenavEnhanced.cy.js` (A - added)
- `frontend/cypress/e2e/sidenavNavigation.cy.js` (A - added)

#### Search Component (Modified)

- `frontend/src/components/layout/search/searchBar.js` (M - modified,
  navbar-related)

#### Archive/Reference Files

- `frontend/src/components/layout/_archive/TwoModeLayout.js` (A - added)
- `frontend/src/components/layout/_archive/TwoModeLayout.css` (A - added)
- `frontend/src/components/layout/_archive/TwoModeLayout.test.js` (A - added)

#### Specification Files

- `specs/009-carbon-sidenav/` (entire directory)
  - `spec.md`
  - `plan.md`
  - `tasks.md`
  - `quickstart.md`
  - `data-model.md`
  - `research.md`
  - `architecture-analysis.md`
  - `checklists/requirements.md`
  - `contracts/layout-props.md`

#### Supporting Files (May Need Review)

- `frontend/src/components/Style.css` (M - modified, may contain navbar styles)
- `frontend/src/App.js` (M - modified, may use Layout)
- `frontend/src/App.test.js` (M - modified, may test Layout)
- `frontend/src/components/Login.js` (M - modified, may be affected by Layout)

### Analyzer-Related Files (TO KEEP ON CURRENT BRANCH)

These should **NOT** be extracted:

- All `frontend/src/components/analyzers/` files
- All `frontend/cypress/e2e/analyzer*.cy.js` files
- All analyzer-related backend files
- `analyzer-setup.docker-compose.yml`
- `docker-compose.astm-test.yml`
- Analyzer-related test data files

### Mixed/Uncertain Files (NEED REVIEW)

These files may contain both navbar and analyzer changes:

- `frontend/src/App.js` - May have both navbar routing and analyzer routes
- `frontend/src/App.test.js` - May test both
- `frontend/src/components/Login.js` - May have Layout-related changes
- `frontend/src/components/Style.css` - May have both navbar and analyzer styles
- `.specify/guides/testing-roadmap.md` - May have both
- `.specify/memory/constitution.md` - May have both
- `.specify/scripts/bash/common.sh` - May have both

## Extraction Strategy

### Option 1: Interactive Cherry-Pick (Recommended)

**Pros**: Precise control, can review each commit  
**Cons**: Time-consuming, requires manual conflict resolution

**Steps**:

1. Create new branch from `origin/develop`
2. Identify navbar-only commits (review commit messages)
3. Cherry-pick navbar commits one by one
4. Manually add any files that weren't in commits but are navbar-related
5. Resolve conflicts as they arise

### Option 2: File-Based Extraction (Faster)

**Pros**: Faster, extracts all navbar files at once  
**Cons**: May include unrelated changes in mixed files

**Steps**:

1. Create new branch from `origin/develop`
2. Use `git checkout` to bring navbar files from current branch
3. Review and clean up mixed files (App.js, Style.css, etc.)
4. Commit navbar changes

### Option 3: Hybrid Approach (Recommended for This Case)

**Pros**: Balance of speed and precision  
**Cons**: Requires careful review

**Steps**:

1. Create new branch from `origin/develop`
2. Use `git checkout` for clearly navbar-only files
3. Manually review and extract navbar portions from mixed files
4. Test to ensure navbar works independently

## Detailed Extraction Plan (Hybrid Approach)

### Phase 1: Setup

```bash
# 1. Ensure we're on current branch and it's clean
git checkout nav-branch-pre-merge
git status  # Should be clean (or stash changes)

# 2. Create new branch from origin/develop (following Constitution Principle IX naming)
git fetch origin develop
git checkout -b feat/009-ogc-232-carbon-sidenav origin/develop
```

### Phase 2: Extract Core Navbar Files

These are clearly navbar-only and can be copied directly:

```bash
# Core components
git checkout nav-branch-pre-merge -- frontend/src/components/layout/Header.js
git checkout nav-branch-pre-merge -- frontend/src/components/layout/Layout.js
git checkout nav-branch-pre-merge -- frontend/src/components/layout/AnalyzerLayout.js
git checkout nav-branch-pre-merge -- frontend/src/components/layout/AnalyzerLayout.css
git checkout nav-branch-pre-merge -- frontend/src/components/layout/index.js

# Custom hooks
git checkout nav-branch-pre-merge -- frontend/src/components/layout/useSideNavPreference.js
git checkout nav-branch-pre-merge -- frontend/src/components/layout/useMenuAutoExpand.js

# Tests
git checkout nav-branch-pre-merge -- frontend/src/components/layout/Header.test.js
git checkout nav-branch-pre-merge -- frontend/src/components/layout/Layout.test.js
git checkout nav-branch-pre-merge -- frontend/src/components/layout/Layout.integration.test.js
git checkout nav-branch-pre-merge -- frontend/src/components/layout/useSideNavPreference.test.js
git checkout nav-branch-pre-merge -- frontend/src/components/layout/useMenuAutoExpand.test.js

# E2E tests
git checkout nav-branch-pre-merge -- frontend/cypress/e2e/sidenavEnhanced.cy.js
git checkout nav-branch-pre-merge -- frontend/cypress/e2e/sidenavNavigation.cy.js

# Search component
git checkout nav-branch-pre-merge -- frontend/src/components/layout/search/searchBar.js

# Archive files
git checkout nav-branch-pre-merge -- frontend/src/components/layout/_archive/

# Specification
git checkout nav-branch-pre-merge -- specs/009-carbon-sidenav/
```

### Phase 3: Review Mixed Files

For these files, we need to manually review and extract only navbar-related
changes:

1. **`frontend/src/App.js`**

   - Check if it has Layout/Header imports
   - Check if it has navbar routing changes
   - Extract only navbar-related changes

2. **`frontend/src/App.test.js`**

   - Check if it tests Layout/Header
   - Extract only navbar-related tests

3. **`frontend/src/components/Login.js`**

   - Check if it uses Layout
   - Extract only Layout-related changes

4. **`frontend/src/components/Style.css`**

   - Search for navbar/sidenav-related CSS
   - Extract only navbar styles (may need to diff against develop)

5. **`.specify/` files**
   - Review if changes are navbar-specific or general
   - Extract only navbar-related documentation

### Phase 4: Verify and Test

```bash
# 1. Check what files are staged
git status

# 2. Review changes
git diff --cached

# 3. Build and test
mvn clean install -DskipTests -Dmaven.test.skip=true
cd frontend && npm install && npm test -- --testPathPattern="layout|sidenav" && cd ..

# 4. Run navbar E2E tests
cd frontend && npm run cy:run -- --spec "cypress/e2e/sidenav*.cy.js" && cd ..
```

### Phase 5: Commit

```bash
# Commit navbar extraction
git add .
git commit -m "feat(navbar): Extract navbar refactor from nav-branch-pre-merge

- Implement tri-state sidenav (show/lock/close)
- Add useSideNavPreference hook for state management
- Add useMenuAutoExpand hook for auto-expansion
- Update Header and Layout components
- Add comprehensive tests (unit, integration, E2E)
- Add specification documentation (spec 009)

Related to OGC-232
Extracted from nav-branch-pre-merge, based on origin/develop"
```

## Verification Checklist

Before considering extraction complete:

- [ ] New branch created from `origin/develop`
- [ ] All navbar-only files extracted
- [ ] Mixed files reviewed and only navbar changes included
- [ ] No analyzer files included
- [ ] Build passes: `mvn clean install -DskipTests -Dmaven.test.skip=true`
- [ ] Frontend tests pass: `npm test -- --testPathPattern="layout|sidenav"`
- [ ] Navbar E2E tests pass:
      `npm run cy:run -- --spec "cypress/e2e/sidenav*.cy.js"`
- [ ] No broken imports or references
- [ ] Specification files included
- [ ] Commit message follows convention

## Post-Extraction Tasks

After successful extraction:

1. **Update current branch** (`nav-branch-pre-merge`):

   - Remove navbar files (they're now in new branch)
   - Keep analyzer work
   - Rebase or merge from `origin/develop` if needed

2. **Clean up new branch** (`feat/009-ogc-232-carbon-sidenav`):

   - Review and finalize navbar implementation
   - Run full test suite
   - Format code: `mvn spotless:apply && cd frontend && npm run format`
   - Create PR targeting `develop` with reference to OGC-232

3. **Documentation**:
   - Update spec if needed
   - Ensure quickstart.md is accurate
   - Update any cross-references

## Risk Assessment

### Low Risk

- Core navbar files (Header.js, Layout.js, hooks) - clearly separated
- Test files - clearly separated
- Specification files - clearly separated

### Medium Risk

- Mixed files (App.js, Style.css) - need careful review
- AnalyzerLayout.js - uses navbar but is analyzer-specific

### High Risk

- None identified - files are generally well-separated

## Questions to Resolve

1. **AnalyzerLayout.js**: Should this be included in navbar branch or analyzer
   branch?

   - **Recommendation**: Include in navbar branch since it demonstrates navbar
     usage, but note it's analyzer-specific

2. **Style.css changes**: How to extract only navbar-related CSS?

   - **Recommendation**: Use
     `git diff origin/develop...nav-branch-pre-merge -- frontend/src/components/Style.css`
     and manually extract navbar styles

3. **App.js routing**: If App.js has both navbar and analyzer routing, how to
   split?
   - **Recommendation**: Extract navbar routing to new branch, leave analyzer
     routing on current branch

## Next Steps

1. Review this plan with team
2. Execute Phase 1 (Setup)
3. Execute Phase 2 (Extract Core Files)
4. Review Phase 3 (Mixed Files) - may need team input
5. Execute Phase 4 (Verify and Test)
6. Execute Phase 5 (Commit)
7. Create PR for review with OGC-232 reference

## Jira Issue Reference

- **Issue**:
  [OGC-232](https://uwdigi.atlassian.net/jira/software/c/projects/OGC/boards/51?assignee=5e765a025e755d0cd425c863&selectedIssue=OGC-232)
- **Spec**: `009-carbon-sidenav`
- **Branch Convention**: Following Constitution Principle IX, using format
  `feat/{NNN}[-{jira}]-{name}`
