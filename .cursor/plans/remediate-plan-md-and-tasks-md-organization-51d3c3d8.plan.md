<!-- 51d3c3d8-94dd-4b37-b1d9-bf0190129f51 36341e9e-6a1d-4a66-9bf6-c62a689dffe9 -->

# Remediation Plan: plan.md and tasks.md Organization

## Objectives

1. **Fix critical misalignment**: plan.md should be an IMPLEMENTATION PLAN
   (technical approach, architecture, implementation phases), NOT a
   design/documentation workflow
2. Align phase numbering between plan.md and tasks.md (both should use
   implementation phase numbers)
3. Review git history to identify any lost content
4. Reorganize to clearly separate implemented vs to-be-implemented work
5. Update both documents for SampleItem-level tracking (per spec.md 2025-01-15)

## Spec Kit Structure (Correct Understanding)

**plan.md** = Implementation Plan

- Technical Context (tech stack, dependencies)
- Constitution Check
- Project Structure
- **Implementation Phases** (how to build it): Phase 1 (Setup), Phase 2
  (Foundational), Phase 3 (Position Hierarchy), etc.
- Each phase: Objective, Technical Approach, Architecture Decisions,
  Dependencies

**tasks.md** = Task Breakdown

- Breaks down plan.md implementation phases into actionable tasks
- Same phase structure as plan.md
- Each phase: Tests First → Implementation → Verification
- Organized by implementation phases matching plan.md

## Current State Analysis

### Phase Numbering Mismatch

**plan.md structure** (design/documentation phases):

- Phase 0: Outline & Research
- Phase 1: Design & Contracts
- Phase 2: Task Breakdown (Deferred)
- Phase 3.1: Tab-Specific Search
- Phase 4: Dashboard Metric Card
- Phase 5: Overflow Menu & Modal
- Phase 6: Location CRUD
- Phase 7: Expandable Rows
- Phase 10: Barcode Workflow

**tasks.md structure** (implementation phases):

- Phase 1: Setup & Database Schema
- Phase 2: Foundational
- Phase 3: Position Hierarchy Update
- Phase 4: Flexible Assignment Architecture
- Phase 5: User Story 1 (Assignment)
- Phase 6: User Story 2A (Search)
- Phase 7: User Story 2B (Movement)
- Phase 7.5: Modal Consolidation
- Phase 8: Location CRUD
- Phase 9: Expandable Rows
- Phase 9.5: Capacity Calculation
- Phase 10: Barcode Workflow
- Phase 11: Polish
- Phase 12: Compliance

**Key Finding**: plan.md uses "design phases" while tasks.md uses
"implementation phases". These serve different purposes but need clear mapping.

### Implementation Status

From git history and file structure:

- **Completed**: Phase 1, Phase 2, Phase 3, Phase 4, Phase 5 (partial), Phase
  7.5, Phase 8, Phase 9, Phase 9.5, Phase 10 (partial)
- **In Progress**: Phase 5 (dashboard features), Phase 10 (barcode workflow
  iterations)
- **Not Started**: Phase 6 (US2A Search), Phase 7 (US2B Movement - but 7.5
  done), Phase 11 (Polish), Phase 12 (Compliance)

### Missing Content Check

**Git History Review**:

- plan.md: 17 commits, original was simpler (Phase 0, 1, 2 only)
- tasks.md: 22 commits, original had Phase 1, 2, 3 (where Phase 3 = US1)
- **Finding**: Phases were added incrementally, not lost. Content evolved
  organically.

## Remediation Strategy

### Step 1: Create Phase Mapping Table

Add to both plan.md and tasks.md a "Phase Mapping" section that explains:

- plan.md phases = Design/Documentation phases (what to design)
- tasks.md phases = Implementation phases (what to build)
- Clear mapping between the two systems

**Location**: Add after "Summary" section in both files

### Step 2: Reorganize plan.md Structure

**New plan.md organization**:

1. **Design Phases** (keep existing Phase 0, 1, 2)

- Phase 0: Outline & Research
- Phase 1: Design & Contracts
- Phase 2: Task Breakdown (Deferred)

2. **Implementation Phase References** (new section)

- Reference tasks.md phases with brief descriptions
- Map design decisions to implementation phases
- Example: "Phase 5 (Overflow Menu) → tasks.md Phase 7.5 (Modal Consolidation)"

3. **Implementation Details** (consolidate existing Phase 3.1, 4, 5, 6, 7, 10)

- Rename to match tasks.md numbering
- Phase 3: Position Hierarchy (from tasks.md Phase 3)
- Phase 4: Flexible Assignment (from tasks.md Phase 4)
- Phase 5: User Story 1 (from tasks.md Phase 5)
- Phase 6: User Story 2A (from tasks.md Phase 6)
- Phase 7: User Story 2B (from tasks.md Phase 7)
- Phase 7.5: Modal Consolidation (from tasks.md Phase 7.5)
- Phase 8: Location CRUD (from tasks.md Phase 8)
- Phase 9: Expandable Rows (from tasks.md Phase 9)
- Phase 9.5: Capacity Calculation (from tasks.md Phase 9.5)
- Phase 10: Barcode Workflow (keep existing)
- Phase 11: Polish (from tasks.md Phase 11)
- Phase 12: Compliance (from tasks.md Phase 12)

### Step 3: Add Implementation Status Sections

**In tasks.md**: Add status markers to each phase header:

- `[COMPLETE]` - All tasks checked, verified working
- `[IN PROGRESS]` - Some tasks complete, some remaining
- `[NOT STARTED]` - No tasks complete

**In plan.md**: Add "Implementation Status" subsection to each phase:

- Link to tasks.md phase
- Brief status summary
- Key deliverables status

### Step 4: Fix Phase Cross-References

**In plan.md**:

- Update all "Phase X" references to match new numbering
- Fix "Phase 2" capacity calculation reference (line 59) → "Phase 9.5"
- Fix "Phase 10 below" references to actual Phase 10 location

**In tasks.md**:

- Verify all internal phase references are correct
- Update dependency diagrams to use consistent numbering

### Step 5: Add Missing Phases to plan.md

**Add new sections** (modeled after existing Phase 10 structure):

1. **Phase 3: Position Hierarchy Structure Update**

- Copy structure from tasks.md Phase 3
- Add design context and decisions
- Reference tasks.md for implementation details

2. **Phase 4: Flexible Assignment Architecture**

- Copy structure from tasks.md Phase 4
- Add design rationale
- Reference tasks.md for implementation details

3. **Phase 7.5: Modal Consolidation**

- Merge content from plan.md Phase 5 (Overflow Menu) with tasks.md Phase 7.5
- Document consolidation rationale
- Reference tasks.md for implementation details

4. **Phase 9.5: Capacity Calculation Logic**

- Add new section
- Document two-tier capacity system design
- Reference tasks.md Phase 9.5 for implementation

5. **Phase 11: Polish & Cross-Cutting Concerns**

- Add new section
- Document polish requirements
- Reference tasks.md Phase 11

6. **Phase 12: Constitution Compliance Verification**

- Add new section
- Document compliance checklist
- Reference tasks.md Phase 12

### Step 6: Update for SampleItem Integration

**In plan.md**:

- Update "Sample Entity Integration" section (line 2969-2985)
- Change title to "SampleItem Entity Integration"
- Update description: "SampleItem-to-location link via polymorphic location_id +
  location_type"
- Update data model references to SampleItem
- Update FHIR mapping: "SampleItem storage location maps to Specimen.container
  reference"

**In tasks.md**:

- Add SampleItem integration tasks to Phase 2 or Phase 5
- Task: Update SampleStorageAssignment entity to reference SampleItem (not
  Sample)
- Task: Update service methods to work with SampleItem IDs
- Task: Update frontend to display SampleItem ID/External ID with parent Sample
  context
- Task: Update search to match SampleItem ID/External ID or Sample accession
  number

### Step 7: Reorganize tasks.md for Clarity

**New tasks.md structure**:

1. **Implementation Status Overview** (new section at top)

- Summary table: Phase | Status | Tasks Complete | Tasks Remaining
- Quick reference for what's done vs what's left

2. **Completed Phases** (group at top)

- Phase 1: Setup [COMPLETE]
- Phase 2: Foundational [COMPLETE]
- Phase 3: Position Hierarchy [COMPLETE]
- Phase 4: Flexible Assignment [COMPLETE]

3. **In Progress Phases**

- Phase 5: User Story 1 [IN PROGRESS] - Dashboard features remaining
- Phase 10: Barcode Workflow [IN PROGRESS] - Some iterations complete

4. **Not Started Phases**

- Phase 6: User Story 2A [NOT STARTED]
- Phase 7: User Story 2B [NOT STARTED] (but 7.5 done)
- Phase 11: Polish [NOT STARTED]
- Phase 12: Compliance [NOT STARTED]

5. **Completed Sub-Phases** (within in-progress phases)

- Phase 7.5: Modal Consolidation [COMPLETE]
- Phase 8: Location CRUD [COMPLETE]
- Phase 9: Expandable Rows [COMPLETE]
- Phase 9.5: Capacity Calculation [COMPLETE]

### Step 8: Fix Phase Ordering

**In tasks.md**: Renumber Phase 7.5 → Phase 8, then:

- Current Phase 8 → Phase 9
- Current Phase 9 → Phase 10
- Current Phase 9.5 → Phase 10.5 (or keep as 9.5 if preferred)
- Current Phase 10 → Phase 11
- Current Phase 11 → Phase 12
- Current Phase 12 → Phase 13

**OR** (preferred): Keep Phase 7.5 as-is, but update all subsequent phase
numbers in dependency diagrams and cross-references.

## File Changes Summary

### plan.md Changes

1. Add "Phase Mapping" section after Summary
2. Reorganize implementation phases to match tasks.md numbering
3. Add missing phases: 3, 4, 7.5, 9.5, 11, 12
4. Fix all phase cross-references
5. Update "Sample Entity Integration" → "SampleItem Entity Integration"
6. Add "Implementation Status" subsections to each phase

### tasks.md Changes

1. Add "Implementation Status Overview" section at top
2. Reorganize phases into: Complete | In Progress | Not Started
3. Add status markers `[COMPLETE]`, `[IN PROGRESS]`, `[NOT STARTED]` to phase
   headers
4. Add SampleItem integration tasks to Phase 2 or Phase 5
5. Update all phase cross-references for consistency
6. Add "Phase Mapping" section explaining relationship to plan.md

## Validation Checklist

After remediation:

- [ ] All phase numbers consistent between plan.md and tasks.md
- [ ] All cross-references use correct phase numbers
- [ ] SampleItem integration documented in both files
- [ ] Implementation status clearly visible
- [ ] No content lost (verified against git history)
- [ ] Phase dependency diagrams updated
- [ ] Both documents reference each other appropriately

## Risk Mitigation

- **Risk**: Breaking existing cross-references
- **Mitigation**: Use grep to find all "Phase X" references before renumbering
- **Risk**: Losing implementation context
- **Mitigation**: Preserve all existing content, only reorganize
- **Risk**: Confusion about design vs implementation phases
- **Mitigation**: Clear "Phase Mapping" section in both documents
