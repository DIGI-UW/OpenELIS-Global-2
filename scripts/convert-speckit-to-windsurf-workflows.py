#!/usr/bin/env python3
"""
Convert SpecKit commands to Windsurf workflows.
"""

import re
from pathlib import Path

def convert_command_to_workflow(command_file: Path, output_dir: Path):
    """Convert a command markdown file to a Windsurf workflow."""
    content = command_file.read_text(encoding='utf-8')
    
    # Extract frontmatter
    frontmatter_match = re.match(r'^---\n(.*?)\n---\n', content, re.DOTALL)
    if frontmatter_match:
        frontmatter = frontmatter_match.group(1)
        body = content[frontmatter_match.end():]
    else:
        frontmatter = ""
        body = content
    
    # Extract description from frontmatter
    description = ""
    desc_match = re.search(r'description:\s*(.*?)(?:\n|$)', frontmatter)
    if desc_match:
        description = desc_match.group(1).strip()
    
    # Extract the main content after frontmatter
    # Remove command-specific syntax
    body = re.sub(r'```text\n\$ARGUMENTS\n```', '', body)
    body = re.sub(r'You \*\*MUST\*\* consider the user input before proceeding.*?\n\n', '', body)
    
    # Create workflow content
    workflow_name = command_file.stem
    workflow_content = f"""---
description: {description or f"Workflow for {workflow_name}"}
---

# {workflow_name}

{body.strip()}

---
*SpecKit workflow for OpenELIS Global 2*
"""
    
    # Write workflow file
    output_file = output_dir / f"{workflow_name}.md"
    output_file.write_text(workflow_content, encoding='utf-8')
    print(f"Created workflow: {output_file}")

def main():
    repo_root = Path(__file__).resolve().parent.parent
    source_dir = repo_root / ".specify" / "core" / "commands"
    oe_dir = repo_root / ".specify" / "oe" / "commands"
    workflows_dir = repo_root / ".windsurf" / "workflows"
    
    workflows_dir.mkdir(parents=True, exist_ok=True)
    
    # Convert core SpecKit commands
    if source_dir.exists():
        for cmd_file in source_dir.glob("speckit.*.md"):
            convert_command_to_workflow(cmd_file, workflows_dir)
    
    # Convert OE commands
    if oe_dir.exists():
        for cmd_file in oe_dir.glob("*.md"):
            convert_command_to_workflow(cmd_file, workflows_dir)
    
    print("\nConversion complete!")
    print(f"Workflows saved to: {workflows_dir}")
    print("\nThese will be available as /workflow-name in Windsurf Cascade")

if __name__ == "__main__":
    main()
