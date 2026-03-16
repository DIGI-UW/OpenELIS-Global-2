# Packaged AI Skills

This directory contains reusable, packaged AI skills that are independent from
SpecKit command source layout.

## Layout

Each skill lives under `.ai/skills/<skill-name>/` and should include:

- `SKILL.md` - canonical behavior and trigger guidance
- `commands/*.md` - slash-command entrypoints compiled into agent command dirs
- `reference/*.md` - deeper guidance used by command entrypoints
- `templates/*` - starter templates consumed by commands
- `scripts/*` - deterministic helper utilities

## Installation

Use the generic installer:

```bash
python3 scripts/install-agent-skills.py -y all
```

Legacy compatibility remains available:

```bash
python3 scripts/install-speckit-commands.py -y all
```

The legacy installer delegates to the generic installer with `--legacy-only`.
