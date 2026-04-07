---
name: speckit
description:
  GitHub SpecKit integration for specification-driven development. Use for
  creating feature specifications, implementation plans, task breakdowns, and
  maintaining constitution compliance throughout the development lifecycle.
---

# SpecKit Skill

Use this skill when:

- creating a new feature specification from user requirements
- generating technical implementation plans
- breaking down features into manageable tasks
- clarifying underspecified requirements
- analyzing code or specifications for compliance
- creating quality checklists

## Primary Entrypoints

- `/speckit.specify` - Create a comprehensive feature specification
- `/speckit.plan` - Generate technical implementation plan
- `/speckit.tasks` - Create task breakdown with dependencies
- `/speckit.clarify` - Identify and resolve undersified areas
- `/speckit.implement` - Generate implementation code
- `/speckit.analyze` - Analyze code or specifications
- `/speckit.checklist` - Create quality checklists
- `/speckit.taskstoissues` - Convert tasks to GitHub issues
- `/speckit.constitution` - View OpenELIS constitution principles

## Lifecycle

For specification-driven development:

1. `/speckit.specify` - Create the specification
2. `/speckit.clarify` - Resolve any ambiguities (if needed)
3. `/speckit.plan` - Create implementation plan
4. `/speckit.tasks` - Break down into tasks
5. `/speckit.implement` - Execute implementation
6. `/speckit.checklist` - Verify quality

## Core Non-Negotiables

- All specifications must follow OpenELIS constitution principles
- Features over 3 days effort must be broken into validation milestones
- Use Carbon Design System for all UI components
- Follow 5-layer architecture pattern (Valueholder → DAO → Service → Controller
  → Form)
- Write tests before implementation (TDD)
- All user-facing strings must use React Intl
- Database changes must use Liquibase migrations
- FHIR entities must include fhir_uuid field

## Configuration

The skill automatically detects:

- Current git branch for feature naming
- Project structure for appropriate templates
- Existing specifications for updates

## Integration

Works seamlessly with:

- OpenELIS Global 2 project structure
- GitHub PR workflow
- AGENTS.md context system
- Constitution compliance checking
