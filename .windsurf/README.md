# Windsurf SpecKit Integration

This directory contains SpecKit integration for Windsurf with both workflows and
skills for maximum flexibility.

## Dual Integration Approach

### 1. Workflows (Official Method)

- **Location**: `.windsurf/workflows/`
- **Invocation**: Manual with `/command-name`
- **Purpose**: Direct command execution
- **Best for**: Precise control over which SpecKit command to run

### 2. Skills (Enhanced Method)

- **Location**: `.windsurf/skills/speckit/`
- **Invocation**: Automatic OR manual with `@speckit`
- **Purpose**: Intelligent workflow selection with supporting resources
- **Best for**: Letting Cascade automatically handle SpecKit processes

## Installation

```bash
# Convert all SpecKit commands to Windsurf workflows
python3 scripts/convert-speckit-to-windsurf-workflows.py

# This creates:
# - .windsurf/workflows/ (for manual / commands)
# - .windsurf/skills/speckit/ (for automatic invocation)
```

## Usage Examples

### Manual Workflow Commands

```
Type: /speckit.specify
→ Runs the specification workflow directly

Type: /speckit.plan
→ Runs the planning workflow directly
```

### Automatic Skill Invocation

```
"I need to create a spec for user authentication"
→ Cascade automatically invokes @speckit
→ Skill selects appropriate workflow and enhances it

"Help me plan this feature with proper task breakdown"
→ Skill runs /speckit.plan with templates and validation
```

### Manual Skill Invocation

```
Type: @speckit
→ Forces the skill to be used
→ Skill analyzes context and selects best workflow
```

## Available Commands

### Manual `/` Commands (Workflows)

- `/speckit.specify` - Create a feature specification
- `/speckit.plan` - Create an implementation plan
- `/speckit.implement` - Generate implementation code
- `/speckit.analyze` - Analyze code or issues
- `/speckit.checklist` - Create quality checklists
- `/speckit.clarify` - Handle specification clarifications
- `/speckit.tasks` - Generate task lists
- `/speckit.taskstoissues` - Convert tasks to GitHub issues
- `/speckit.constitution` - View OpenELIS constitution principles

### Custom OpenELIS Commands

- `/start-ticket` - Begin work on a new ticket
- `/finish-ticket` - Complete and submit a ticket
- `/review-pr` - Review a pull request
- `/address-pr-comments` - Address PR feedback
- `/audit-branch` - Audit branch for issues
- `/careful-rebase` - Safe rebase workflow
- `/daily-priorities` - Set daily work priorities
- `/fix-ci` - Fix CI build failures
- `/restart-dev-env` - Restart development environment
- `/restart-analyzer-harness` - Restart analyzer testing
- `/download-ci-logs` - Download CI build logs
- `/triage-prs` - Triage pull requests

## Skill Features

When using the `@speckit` skill, you get:

- **Automatic workflow selection** based on context
- **Template library** for specifications and plans
- **Constitution reference** for OpenELIS constraints
- **Quality checklists** for different domains
- **Enhanced validation** of outputs
- **Supporting files** and resources

## Spec-Driven Development Workflow

### Using Workflows (Manual Control)

1. `/speckit.specify` - Create feature specification
2. `/speckit.clarify` - Resolve ambiguities (if needed)
3. `/speckit.plan` - Create implementation plan
4. `/speckit.tasks` - Generate task breakdown
5. `/speckit.implement` - Execute implementation
6. `/speckit.checklist` - Verify quality

### Using Skills (Automatic Intelligence)

1. Describe what you want (e.g., "Create a spec for...")
2. Cascade invokes @speckit automatically
3. Skill selects and runs appropriate workflow
4. Enhances output with templates and validation
5. Provides additional context and resources

## MCP Integration (Alternative)

For external SpecKit CLI integration via MCP:

1. Install the MCP server:

   ```bash
   npm install -g @lsendel/spec-kit-mcp
   ```

2. Configure `.windsurf/mcp.json`:

   ```json
   {
     "servers": {
       "speckit": {
         "command": "npx",
         "args": ["-y", "@lsendel/spec-kit-mcp"],
         "autoStart": true
       }
     }
   }
   ```

3. Use natural language: "Use speckit_specify to create a spec..."

## Notes

- **Workflows** provide direct command access matching official SpecKit
- **Skills** provide intelligent automation with supporting resources
- Both approaches can be used together for maximum flexibility
- Skills enhance workflows with templates and validation
- The skill includes the OpenELIS constitution and templates
- Progressive disclosure keeps context usage efficient
