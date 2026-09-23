const test = require("node:test");
const assert = require("node:assert/strict");
const { isDocsPath, requiresE2E, findPullRequest, MAX_LISTED_FILES } = require("./e2e-scope.cjs");

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

test("findPullRequest matches the exact head sha, and only open pulls by head ref", async () => {
  let asked;
  const github = {
    rest: {
      pulls: {
        list: async (params) => {
          asked = params;
          return { data: [{ number: 1, head: { sha: "old" } }, { number: 2, head: { sha: "abc" } }] };
        },
      },
    },
  };
  const pull = await findPullRequest({ github, owner: "o", repo: "r", headOwner: "fork", headBranch: "b", headSha: "abc" });
  assert.equal(pull.number, 2);
  assert.equal(asked.head, "fork:b");
  assert.equal(asked.state, "open");
  assert.equal(await findPullRequest({ github, owner: "o", repo: "r", headOwner: "fork", headBranch: "b", headSha: "zzz" }), null);
  assert.equal(await findPullRequest({ github, owner: "o", repo: "r", headOwner: "", headBranch: "b", headSha: "abc" }), null);
});
