# Releases

This file is the source of truth for which OpenELIS Global versions exist, which
are supported, and how the branches relate to them.

## Supported release lines

| Line    | Status              | Latest tag | Notes                                                                                                                                |
| ------- | ------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| 3.4     | In development      |            | `develop` is at 3.4.0-SNAPSHOT.                                                                                                      |
| 3.3     | Supported (current) | 3.3.0      | `main` holds the current release. The first release under Semantic Versioning.                                                       |
| 3.2.3.x | Supported           | 3.2.3.0    | Baseline release.                                                                                                                    |
| 3.2.2.x | Supported (legacy)  | 3.2.2.0    | For sites that cannot yet take the 3.2.3 analyzer framework. Support ends when those sites move up; the date will be announced here. |
| ≤ 3.2.1 | Unsupported         | 3.2.1.11   | Upgrade to the current line.                                                                                                         |

## Branches

| Branch            | What it holds                                                                                                                                                         |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `develop`         | Integration branch and default branch. Development pull requests target it. Its version is the next release with a `-SNAPSHOT` suffix. Never deploy it to production. |
| `main`            | The latest release. It changes only through a reviewed release pull request from a release branch. Each current-line release is tagged on `main`.                     |
| `release/<X.Y>.x` | One branch per release line, cut from `develop` before a release. It receives only fixes that are already merged to `develop`.                                        |
| `3.2.2.x`         | The legacy 3.2.2 line, named before the `release/` convention.                                                                                                        |

## Branch and tag protection

Repository rulesets enforce the branch roles above.

| Ruleset                           | Applies to                              | Rules                                                                                                                                                                        | Who can bypass       |
| --------------------------------- | --------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------- |
| main: stable release branch       | `main`                                  | No deletion, no force push; changes only by pull request, merged with a merge commit; one approval from someone other than the last pusher; the three checkpoint checks pass | Nobody               |
| main: only release managers merge | `main`                                  | Only the release managers can merge into it                                                                                                                                  | The release managers |
| release tags are immutable        | Tags matching `[0-9]*` and `archive/**` | No deletion, no moving                                                                                                                                                       | Nobody               |

Anyone can open or approve a release pull request. Only a release manager can
merge it.

In an emergency, a repository admin can set a ruleset's enforcement to
`disabled` and restore it afterwards. The change is recorded in the organization
audit log.

## Cutting a release

1. Cut `release/<X.Y>.x` from `develop`. In a pull request to `develop`, move
   `develop` to the next minor version with a `-SNAPSHOT` suffix.
2. On the release branch, set `revision` in `pom.xml` to the release version.
   Tag release candidates (`X.Y.0-rc.1`, ...) on the release branch for testing.
3. Open a pull request from `release/<X.Y>.x` into `main`. After approval and
   green checks, a release manager merges it with **Create a merge commit**.
4. Tag the release (`X.Y.0`) on the merge commit on `main` and publish the
   GitHub Release from that tag. Publishing the release builds, tests and pushes
   the versioned images.

Release branches are never merged back into `develop`. Every fix is on `develop`
first, so nothing on a release branch is missing from `develop` except the
version bump.

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
3. For the current line, bump the patch version on its release branch and
   release it into `main` as in steps 3 and 4 above.
4. For an older supported line, bump and tag the patch version on its
   maintenance branch. `main` does not change, because it always holds the
   newest release. Modern lines use `release/<X.Y>.x`; the four-part 3.2.3 line
   uses `release/3.2.3.x`, and the existing legacy 3.2.2 branch remains
   `3.2.2.x`. Establish the maintenance branch before preparing a patch if it
   does not exist.

The existing image publisher verifies that a release tag still identifies the
exact tested commit and matches its application version. Current stable releases
must be contained in `main`; older releases must belong to a line marked
Supported in the table above and be contained in its maintenance branch. Release
candidates remain on their release branches. These source checks supplement the
required backend, frontend and end-to-end checkpoints; they do not replace
branch review or authorize merging.

## Consuming a release

Distributions and deployments pin a release tag by digest, for example
`itechuw/openelis-global-2:3.2.3.0@sha256:...`. The `:develop` images are
rebuilt on every merge and are for the testing server only.

## Distribution delivery

After all three checkpoints pass, the existing image publisher promotes the
tested images and attaches `deployment-manifest.json` to the OE2 release. It
records the release tag, tested commit and immutable digests for every published
service, including certificate initialization, Bridge and mock. Repeated
publication accepts identical assets/images and refuses replacement with
different contents.

The lightweight `openelis-docker` release workflow consumes that manifest and
pins its Compose file to the same images. It does not select another Bridge
version or rebuild images. Distribution tags are immutable. A missing manifest
means the release is not yet ready for that distribution workflow; never fall
back to `develop` or reconstruct an image set from independent version tags.

The same distro workflow bundles those exact images and configuration for
offline installation, and verifies the archive in an empty Docker daemon without
network access. It does not rebuild application images or select a different
Bridge. Operators load the archive and use Docker Compose with `--pull never`.

This preserves the supported maintenance-branch exception above. Historical
packages keep their tagged instructions. The OE2 publisher is the only shared
testing-server deployment owner.
