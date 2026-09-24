const test = require("node:test");
const assert = require("node:assert/strict");
const { isDocsPath, requiresE2E, resolvePullRequest, MAX_LISTED_FILES } = require("./e2e-scope.cjs");

const files = (...names) => names.map((filename) => ({ filename }));

test("documentation paths are recognised", () => {
  for (const path of [
    "README.md",
    "frontend/src/components/patient/README.md",
    "docs/install.rst",
    "specs/OGC-1054/spec.md",
    ".github/ISSUE_TEMPLATE/bug_report.yml",
  ]) {
    assert.equal(isDocsPath(path), true, path);
  }
});

test("anything else is not documentation", () => {
  for (const path of [
    "pom.xml",
    "src/main/java/Foo.java",
    "frontend/src/App.js",
    "tools/openelis-analyzer-bridge",
    ".github/workflows/e2e-playwright.yml",
    ".github/PULL_REQUEST_TEMPLATE.md.bak",
    "mkdocs.yml",
    "docsite/index.html",
    "",
    undefined,
  ]) {
    assert.equal(isDocsPath(path), false, String(path));
  }
});

test("a documentation-only pull request skips E2E", () => {
  assert.equal(requiresE2E(files("README.md", "docs/a.md", ".github/ISSUE_TEMPLATE/x.yml")), false);
});

test("one non-documentation file is enough to run E2E", () => {
  assert.equal(requiresE2E(files("README.md", "src/main/java/Foo.java")), true);
});

test("a rename out of code still runs E2E", () => {
  assert.equal(requiresE2E([{ filename: "docs/Foo.md", previous_filename: "src/main/java/Foo.java" }]), true);
});

test("a rename between documentation paths skips E2E", () => {
  assert.equal(requiresE2E([{ filename: "docs/new.md", previous_filename: "docs/old.md" }]), false);
});

test("an empty or unusable file list runs E2E", () => {
  assert.equal(requiresE2E([]), true);
  assert.equal(requiresE2E(undefined), true);
});

test("a list at the API's truncation limit runs E2E", () => {
  const many = Array.from({ length: MAX_LISTED_FILES }, (_, i) => ({ filename: `docs/${i}.md` }));
  assert.equal(requiresE2E(many), true);
});

// A fake API: `pulls` is every pull request the repo has; list filters by head ref.
function fakeGithub(pulls) {
  return {
    rest: {
      pulls: {
        get: async ({ pull_number }) => {
          const pull = pulls.find((p) => p.number === pull_number);
          if (!pull) {
            throw Object.assign(new Error("Not Found"), { status: 404 });
          }
          return { data: pull };
        },
        list: async ({ head, state }) => ({
          data: pulls.filter((p) => p.state === state && `${p.headOwner}:${p.head.ref}` === head),
        }),
      },
    },
  };
}

const pr = (number, sha, extra = {}) => ({
  number,
  state: "open",
  headOwner: "fork",
  head: { ref: "b", sha },
  ...extra,
});
const at = { owner: "o", repo: "r", headOwner: "fork", headBranch: "b", headSha: "abc" };

test("the claimed pull request is accepted when the API confirms it is open at this head", async () => {
  const pull = await resolvePullRequest({ github: fakeGithub([pr(7, "abc")]), ...at, claimedNumber: "7" });
  assert.equal(pull.number, 7);
});

test("a missing or malformed claimed number runs E2E", async () => {
  // PR #1000 exists, so "1e3" is rejected by the parse, not by a failed lookup.
  const github = fakeGithub([pr(7, "abc"), pr(1000, "abc", { headOwner: "x" })]);
  for (const claimedNumber of [undefined, "", "0", "-3", "7abc", "1e3", "7.5", "0x7", 7]) {
    assert.equal(await resolvePullRequest({ github, ...at, claimedNumber }), null, String(claimedNumber));
  }
});

test("a forged number pointing at another pull request runs E2E", async () => {
  const github = fakeGithub([pr(7, "abc"), pr(8, "other-sha", { head: { ref: "docs", sha: "other-sha" } })]);
  assert.equal(await resolvePullRequest({ github, ...at, claimedNumber: "8" }), null);
});

test("a number for a pull request that does not exist runs E2E", async () => {
  assert.equal(await resolvePullRequest({ github: fakeGithub([pr(7, "abc")]), ...at, claimedNumber: "99" }), null);
});

test("a closed pull request runs E2E", async () => {
  const github = fakeGithub([pr(7, "abc", { state: "closed" })]);
  assert.equal(await resolvePullRequest({ github, ...at, claimedNumber: "7" }), null);
});

test("a stale head runs E2E", async () => {
  assert.equal(await resolvePullRequest({ github: fakeGithub([pr(7, "newer")]), ...at, claimedNumber: "7" }), null);
});

test("a second open pull request at the same head runs E2E, because the status sha is shared", async () => {
  const github = fakeGithub([pr(7, "abc", { base: { ref: "develop" } }), pr(8, "abc", { base: { ref: "feat/782-x" } })]);
  assert.equal(await resolvePullRequest({ github, ...at, claimedNumber: "7" }), null);
});
