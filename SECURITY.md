# Security Policy

OpenELIS Global is laboratory information system software used in public health
settings. Protecting patient data and laboratory operations is a core project
priority. This document explains how to report security vulnerabilities in
OpenELIS Global 2 and what to expect from the maintainers.

**Steward:** [DIGI](https://openelis-global.org/about/) (Digital Initiatives Group at the
University of Washington).

**Primary repository:** [DIGI-UW/OpenELIS-Global-2](https://github.com/DIGI-UW/OpenELIS-Global-2)

For general community conduct, see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). For
functional bugs and feature requests, use
[GitHub Issues](https://github.com/DIGI-UW/OpenELIS-Global-2/issues) — **not**
this process.

---

## Supported versions

Security fixes are provided for supported lines below. Version numbers follow the
project release scheme (for example `3.2.1.9`). See
[Releases](https://github.com/DIGI-UW/OpenELIS-Global-2/releases) for tags and
installer assets.

| Version / line | Supported | Notes |
| -------------- | --------- | ----- |
| Latest [GitHub Release](https://github.com/DIGI-UW/OpenELIS-Global-2/releases) (current `3.2.x` patch) | Yes | Production deployments should run a current release tag or installer build. |
| `main` | Yes | Production release branch; receives security backports from `develop`. |
| `develop` | Yes | Integration branch; fixes land here first, then are backported to `main` for release. |
| Older `3.2.x` patches superseded by a newer release | Best effort | Upgrade to the latest `3.2.x` release when possible. |
| `2.x` and earlier major versions | No | End of life; no security support unless explicitly listed in a release notice. |

If you are unsure whether your deployment is supported, include your **release
tag**, **installer version**, or **`git describe` output** when you report.

---

## Scope

### In scope

Vulnerabilities in software maintained for OpenELIS Global 2, including when
reasonably exercised in supported deployments:

- This repository (`OpenELIS-Global-2`): application server (Java/Spring),
  React UI, REST APIs, authentication/authorization, session handling, and
  bundled configuration shipped here.
- **Analyzer bridge** when used with OpenELIS Global:
  [DIGI-UW/openelis-analyzer-bridge](https://github.com/DIGI-UW/openelis-analyzer-bridge)
  (report bridge-specific issues to that repository, or note the bridge in your
  report if the issue spans both codebases).
- **Official deployment artifacts** published from this project (for example
  release installers and documented Docker workflows linked from
  [openelis-docker](https://github.com/DIGI-UW/openelis-docker)), when the flaw
  is in OpenELIS-owned images, defaults, or documented configuration — not
  one-off site customization.

### Out of scope

Please use normal support channels (issues, forum, contact form) for:

- General product bugs without demonstrated security impact.
- Misconfigurations on a specific lab server (weak passwords, missing TLS,
  exposed admin ports, outdated OS) unless the **default** documented install is
  unsafe.
- Social engineering, physical access, or denial-of-service without a fixable
  defect in OpenELIS code.
- Third-party services (hosting provider, email, identity provider) except where
  OpenELIS integrates with them in a demonstrably unsafe way.
- Vulnerabilities in dependencies already fixed in a **supported** release; we
  still appreciate reports if the fix is not yet released — coordinate via the
  reporting process below.

---

## How to report a vulnerability

**Do not** report security vulnerabilities through public GitHub Issues,
Discussions, or Pull Requests. Public reports can put deployments and patient
data at risk before a fix is available.

### Preferred: GitHub private vulnerability reporting

1. Open
   **[Report a vulnerability](https://github.com/DIGI-UW/OpenELIS-Global-2/security/advisories/new)**
   for this repository (Security → Advisories → **Report a vulnerability**).
2. Provide as much detail as you can (see [What to include](#what-to-include)).

Maintainers use [GitHub Security Advisories](https://github.com/DIGI-UW/OpenELIS-Global-2/security/advisories)
for coordinated disclosure when appropriate.

### Alternative: OpenELIS contact form

If you cannot use GitHub private reporting (for example, your account cannot
access Security Advisories), use the official contact form and clearly mark the
submission as a **security vulnerability**:

**[https://openelis-global.org/getting-started/contact/](https://openelis-global.org/getting-started/contact/)**

- **Subject:** `Security vulnerability — OpenELIS Global 2`
- Do **not** paste exploit payloads or patient data into the form; describe
  impact and offer to share details through a safer channel if needed.

For community questions that are not security-sensitive, see
[Get involved](https://openelis-global.org/community/get-involved/).

---

## What to include

Help us triage quickly:

1. **Affected component** — OpenELIS app, analyzer bridge, FHIR path, plugin, etc.
2. **Version** — release tag (for example `v3.2.1.9`), installer version, or commit SHA on `develop` / `main`.
3. **Environment** — Docker vs installer, optional modules, bridge enabled or not.
4. **Description** — what is wrong and why it is security-relevant.
5. **Reproduction** — minimal steps or proof of concept.
6. **Impact** — confidentiality, integrity, availability; whether authenticated access is required.
7. **Suggested fix** (optional).

### Protected health information (PHI)

OpenELIS processes laboratory and patient data. **Never** include real patient
records, accession numbers from production, or live database dumps in a
report. Use synthetic data, local test fixtures, or redacted screenshots only.

---

## What to expect

| Stage | Target |
| ----- | ------ |
| Acknowledgement | Within **3 business days** |
| Initial triage / severity | Within **10 business days** when possible |
| Fix and advisory | Depends on severity; we aim for **coordinated disclosure** |

We follow coordinated disclosure: we work with you on a reasonable timeline
before publishing details, typically up to **90 days** from acknowledgement,
unless a shorter or longer window is agreed (for example, active exploitation or
complex fixes).

When a fix is ready, we may:

- Publish a **GitHub Security Advisory** (and credit you if you wish).
- Ship a patch release on `main` and tag a new
  [Release](https://github.com/DIGI-UW/OpenELIS-Global-2/releases).
- Document mitigation steps for administrators in the advisory or release notes.

---

## Good-faith research

We appreciate good-faith security research conducted against **test environments**
without real patient data. Please follow this policy, use local or non-production
deployments where possible, and avoid impacting live laboratory operations.

---

## Security-related documentation

- **Product security overview:**
  [Security and privacy (openelis-global.org)](https://openelis-global.org/about/security-is-critical-to-our-mission/)
- **Administrator security:** [OpenELIS documentation](https://docs.openelis-global.org/)
- **Engineering principles (RBAC, audit, transport):** Constitution Principle VIII in
  [.specify/memory/constitution.md](.specify/memory/constitution.md)
- **Contributing (non-security):** [CONTRIBUTING.md](CONTRIBUTING.md)

---

## Related repositories

| Repository | Report here when |
| ---------- | ---------------- |
| [DIGI-UW/OpenELIS-Global-2](https://github.com/DIGI-UW/OpenELIS-Global-2) | Core LIMS application (this policy). |
| [DIGI-UW/openelis-analyzer-bridge](https://github.com/DIGI-UW/openelis-analyzer-bridge) | Analyzer bridge service and its security boundary. |
| [DIGI-UW/openelis-docker](https://github.com/DIGI-UW/openelis-docker) | Published compose/images **only** if the vulnerability is in those artifacts; otherwise report via OpenELIS Global 2 if the root cause is application code. |

If a flaw spans multiple repositories, report once via OpenELIS Global 2 private
reporting and mention all affected components.
