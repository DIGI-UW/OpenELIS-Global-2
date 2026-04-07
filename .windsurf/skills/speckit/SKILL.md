---
name: speckit
description:
  GitHub SpecKit integration for specification-driven development with automatic
  workflow selection and supporting resources
---

# SpecKit Skill

Advanced SpecKit integration that automatically selects the appropriate workflow
based on your needs. This skill provides intelligent specification-driven
development with access to templates, checklists, and the OpenELIS constitution.

## Automatic Invocation

Cascade automatically invokes this skill when you:

- Mention creating specifications, plans, or implementations
- Talk about feature development or requirements
- Need help with SpecKit processes
- Want to analyze code or specifications

Examples:

- "I need to spec out user authentication"
- "Help me plan this feature"
- "Create tasks for the dashboard update"
- "Analyze this PR for compliance"

## Manual Invocation

Force the skill to be used with `@speckit`

## Skill Capabilities

### 1. Specification Generation

- Creates comprehensive feature specifications
- Follows OpenELIS constitution principles
- Includes user stories and acceptance criteria
- Embeds project constraints automatically

### 2. Implementation Planning

- Generates technical implementation plans
- Identifies dependencies and milestones
- Estimates effort and resources
- Creates Mermaid dependency graphs

### 3. Task Management

- Breaks down features into manageable tasks
- Links to GitHub issues
- Provides effort estimates
- Tracks task dependencies

### 4. Quality Assurance

- Creates domain-specific checklists
- Validates constitution compliance
- Reviews code for anti-patterns
- Ensures testing requirements are met

## Supporting Resources

This skill includes:

- **Constitution reference** - OpenELIS principles and constraints
- **Template library** - Specification and plan templates
- **Checklist collection** - Domain-specific quality checklists
- **Command reference** - All available `/speckit.*` commands

## Integration with Workflows

When invoked, this skill can:

1. Automatically select and run the appropriate `/speckit.*` workflow
2. Provide context and resources to the workflow
3. Enhance the output with templates and checklists
4. Validate results against project constraints

## Available Commands

The skill can invoke these workflows:

- `/speckit.specify` - Create feature specification
- `/speckit.clarify` - Handle specification clarifications
- `/speckit.plan` - Create implementation plan
- `/speckit.tasks` - Generate task breakdown
- `/speckit.implement` - Generate implementation code
- `/speckit.analyze` - Analyze code or specifications
- `/speckit.checklist` - Create quality checklists
- `/speckit.taskstoissues` - Convert to GitHub issues
- `/speckit.constitution` - View constitution principles

## OpenELIS Constraints

Automatically enforced:

- Carbon Design System for UI
- 5-layer architecture pattern
- React Intl for internationalization
- FHIR R4 for healthcare entities
- Liquibase for database changes
- TDD approach with tests first

## Usage Patterns

### Automatic

```
User: "I need to create a spec for the new lab results feature"
→ Cascade invokes @speckit automatically
→ Skill runs /speckit.specify with enhanced context
```

### Manual

```
User: "@speckit help me plan the authentication overhaul"
→ Skill activates and runs /speckit.plan
```

### Enhanced Workflow

```
User: "/speckit.specify user profile management"
→ Workflow runs
→ Skill provides templates and validates output
```
