# Project Branch Workflow - Quickstart

**Reference**: Constitution Principle IX.B

## When to Use

Use project branches when demos or deployments need in-progress features that
aren't yet merged to `develop`.

## Key Rules

1. **Dual PR**: Maintain PRs to both `develop` (review) and `project/{name}` (deployment)
2. **Never skip review**: Original PRs to `develop` must complete proper review
3. **Temporary**: Delete project branches after use

## Workflow

### 1. Create Project Branch

```bash
git checkout develop && git pull
git checkout -b project/{country-or-project}
git push -u origin project/{country-or-project}
```

### 2. Create Duplicate PRs

For each feature needed in the demo:

1. Keep original PR → `develop` (for review)
2. Create new PR from same feature branch → `project/{name}`
3. Title: `[Project] {Feature} (duplicate of #XXX)`

### 3. Merge to Project Branch

```bash
git checkout project/{name}
git merge feat/{feature-branch}
# Resolve conflicts if needed
git push
```

### 4. Test & Deploy

```bash
git checkout project/{name}
mvn clean install -DskipTests=false  # Backend tests
cd frontend && npm test               # Frontend tests
docker compose -f build.docker-compose.yml up -d --build
```

### 5. Cleanup

After demo completes:

```bash
git push origin --delete project/{name}
```

## Quick Reference

```bash
# Create project branch
git checkout develop && git checkout -b project/{name}

# List project branches
git branch -r | grep 'origin/project/'

# Delete project branch
git push origin --delete project/{name}
```

## Anti-Patterns

- ❌ Merging unreviewed code to `develop` for demos
- ❌ Deploying from `develop` with unreviewed features
- ❌ Forgetting to maintain original PRs to `develop`
- ❌ Keeping stale project branches (>30 days)
