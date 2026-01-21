If any guidance conflicts with the core SpecKit command, follow this section.

**OpenELIS Constitution Structure**:

The OpenELIS constitution at `.specify/memory/constitution.md` already contains
9 established principles (v1.8.x). When updating:

1. **Preserve Existing Structure**: Do not reorganize or renumber existing
   principles (I through IX) without explicit user approval.

2. **Sync Impact Report**: The constitution already uses HTML comment Sync
   Impact Reports at the top. Preserve and update this format.

3. **Templates Requiring Updates**: The constitution tracks which templates need
   updates. Ensure this section remains accurate after changes.

4. **Version Bumping**: Follow existing semantic versioning. Current version is
   in the 1.8.x range. Most changes are PATCH (clarifications) or MINOR (new
   guidance within principles).

5. **Path References**: Use `.specify/memory/constitution.md` as the path (not
   `/memory/constitution.md`).

6. **Propagation Targets**: When constitution changes, also check:
   - `.specify/templates/plan-template.md`
   - `.specify/templates/spec-template.md`
   - `.specify/templates/tasks-template.md`
   - `AGENTS.md` (contains constitution summary)
   - `CLAUDE.md` (contains constitution references)
