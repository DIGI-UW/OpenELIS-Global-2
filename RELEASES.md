# Releases

This file is the source of truth for which OpenELIS Global versions exist, which
are supported, and how the branches relate to them.

## Supported release lines

| Line    | Status              | Latest tag | Notes                                                                                                                                |
| ------- | ------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| 3.3     | Planned             |            | 3.3.0 is planned for 2026-11-27. It is the first release that uses Semantic Versioning.                                              |
| 3.2.3.x | Supported (current) | 3.2.3.0    | Baseline release. `main` points at it.                                                                                               |
| 3.2.2.x | Supported (legacy)  | 3.2.2.0    | For sites that cannot yet take the 3.2.3 analyzer framework. Support ends when those sites move up; the date will be announced here. |
| ≤ 3.2.1 | Unsupported         | 3.2.1.11   | Upgrade to the current line.                                                                                                         |

## Branches

| Branch            | What it holds                                                                                                                                                 |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `develop`         | Integration branch and default branch. All pull requests target it. Its version is the next release with a `-SNAPSHOT` suffix. Never deploy it to production. |
| `main`            | The latest release. It only ever moves forward to a newer release tag and never receives commits of its own.                                                  |
| `release/<X.Y>.x` | One branch per release line, cut from `develop` before a release. It receives only fixes that are already merged to `develop`.                                |
| `3.2.2.x`         | The legacy 3.2.2 line, named before the `release/` convention.                                                                                                |

## Branch and tag protection

Repository rulesets enforce the branch roles above.

| Ruleset                             | Applies to                              | Rules                                      | Who can bypass       |
| ----------------------------------- | --------------------------------------- | ------------------------------------------ | -------------------- |
| main: history is immutable          | `main`                                  | No deletion, no force push                 | Nobody               |
| main: only release managers move it | `main`                                  | Only bypass actors may create or update it | The release managers |
| release tags are immutable          | Tags matching `[0-9]*` and `archive/**` | No deletion, no moving                     | Nobody               |

`main` has no pull request or status check requirement. Every GitHub merge
method creates a new commit, so a pull request could never make `main` equal to
a release tag. The tag it moves to has already passed CI on `develop` or on its
release branch.

To move `main` to a new release, a release manager runs:

```bash
git push origin <tag>^{commit}:main
```

The push is refused unless the tag's commit descends from the current `main`. A
tag from an older line, such as `3.2.2.1`, can never move `main`.

When a GitHub Release is published, its target is the release tag or the release
branch, never `main`.

In an emergency, a repository admin can set a ruleset's enforcement to
`disabled` and restore it afterwards. The change is recorded in the organization
audit log.

## Version numbers

From 3.3.0, versions follow [Semantic Versioning](https://semver.org/):
`MAJOR.MINOR.PATCH`, tagged without a `v` prefix. Release candidates are tagged
`X.Y.Z-rc.N` on the release branch.

| Part  | Increases when                                                                                                                                                                                                 |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| MAJOR | An existing database cannot upgrade in one step, a REST or FHIR endpoint is removed or changes shape, or the distro contract changes (environment variables, volumes, image names, configuration file format). |
| MINOR | New features, new Liquibase changesets, or new configuration keys.                                                                                                                                             |
| PATCH | Fixes only. A database change in a patch is a new changeset that already exists on `develop` at the same path.                                                                                                 |

Releases up to and including the 3.2 lines keep their four-part numbers
(`3.2.3.1`, `3.2.2.1`, and so on).

The version is set in one place, the `revision` property in `pom.xml`. It is
changed when a release is prepared, not in feature pull requests.

Liquibase directory names such as `3.5.x.x` are a separate series. They do not
correspond to application versions and are never renamed, because a changeset's
file path is part of its identity.

## Fixing a released version

1. Merge the fix to `develop` as usual.
2. Cherry-pick it onto the release branch for each supported line that needs it,
   newest line first.
3. Tag the next patch version on that release branch.

## Consuming a release

Distributions and deployments pin a release tag by digest, for example
`itechuw/openelis-global-2:3.2.3.0@sha256:...`. The `:develop` images are
rebuilt on every merge and are for the testing server only.
