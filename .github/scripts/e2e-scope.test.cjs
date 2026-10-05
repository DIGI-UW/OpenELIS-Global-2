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

const { stackDeferral, isStackTop, skipReason, stackMembers, containsCommit, MAX_COMPARED_FILES } = require("./e2e-scope.cjs");

const stacked = (position, size, extra = {}) => ({
  number: 4552,
  head: { sha: "top" },
  stack: { number: 4533, position, size, base: { ref: "develop", sha: "base" } },
  ...extra,
});

test("a pull request that is not in a stack runs E2E itself", () => {
  assert.equal(stackDeferral({ number: 7 }), null);
  assert.equal(stackDeferral({ number: 7, stack: null }), null);
  assert.equal(stackDeferral(undefined), null);
  assert.equal(isStackTop({ number: 7 }), false);
});

test("the top of a stack runs E2E itself", () => {
  assert.equal(stackDeferral(stacked(20, 20)), null);
  assert.equal(stackDeferral(stacked(1, 1)), null);
  assert.equal(isStackTop(stacked(20, 20)), true);
  assert.equal(isStackTop(stacked(3, 20)), false);
});

test("a pull request below the top defers to the top of its stack", () => {
  assert.deepEqual(stackDeferral(stacked(3, 20)), { number: 4533, position: 3, size: 20 });
  assert.deepEqual(stackDeferral(stacked(1, 2)), { number: 4533, position: 1, size: 2 });
});

test("unusable stack metadata runs E2E", () => {
  for (const stack of [
    { number: 1, position: 21, size: 20 },
    { number: 1, position: 0, size: 20 },
    { number: 1, position: 3, size: 0 },
    { number: 1, position: "3", size: 20 },
    { number: 1, position: 3 },
    { number: 1, size: 20 },
    { number: 1, position: 2.5, size: 20 },
    { position: 3, size: 20 },
    { number: "4533", position: 3, size: 20 },
    { number: 0, position: 3, size: 20 },
  ]) {
    assert.equal(stackDeferral({ number: 7, stack }), null, JSON.stringify(stack));
  }
});

// A fake API for skipReason: the pull request's own files, and the stack's
// files from the base branch to the head.
function fakeFilesGithub({ own, stack, compareStatus = "ahead" }) {
  return {
    paginate: async () => files(...own),
    rest: {
      pulls: { listFiles: "listFiles" },
      repos: {
        compareCommitsWithBasehead: async ({ basehead }) => ({
          data: { status: compareStatus, files: stack === undefined ? undefined : files(...stack), basehead },
        }),
      },
    },
  };
}
const skipAt = { owner: "o", repo: "r" };

test("a pull request outside a stack is judged on its own files", async () => {
  const docs = await skipReason({ github: fakeFilesGithub({ own: ["docs/a.md"] }), ...skipAt, pull: { number: 7 } });
  assert.equal(docs.kind, "docs");
  assert.equal(docs.description, "Documentation-only change; E2E suites skipped");
  assert.equal(await skipReason({ github: fakeFilesGithub({ own: ["src/main/java/Foo.java"] }), ...skipAt, pull: { number: 7 } }), null);
});

test("a pull request below the top waits for the top whatever it changes", async () => {
  const reason = await skipReason({ github: fakeFilesGithub({ own: ["docs/a.md"] }), ...skipAt, pull: stacked(3, 20) });
  assert.equal(reason.kind, "stack");
  assert.equal(reason.description, "Stacked PR 3 of 20 (stack #4533); waiting for the top of the stack to run E2E");
});

test("the top of a stack is judged on the whole stack's files, not its own layer", async () => {
  const codeBelow = fakeFilesGithub({ own: ["docs/top.md"], stack: ["docs/top.md", "src/main/java/Foo.java"] });
  assert.equal(await skipReason({ github: codeBelow, ...skipAt, pull: stacked(20, 20) }), null);
  const allDocs = fakeFilesGithub({ own: ["src/main/java/Foo.java"], stack: ["docs/a.md", "docs/b.md"] });
  const reason = await skipReason({ github: allDocs, ...skipAt, pull: stacked(20, 20) });
  assert.equal(reason.kind, "docs");
  assert.equal(reason.files, 2);
});

test("a stack comparison GitHub may have cut off runs E2E", async () => {
  const many = Array.from({ length: MAX_COMPARED_FILES }, (_, i) => `docs/${i}.md`);
  assert.equal(await skipReason({ github: fakeFilesGithub({ own: [], stack: many }), ...skipAt, pull: stacked(20, 20) }), null);
  assert.equal(await skipReason({ github: fakeFilesGithub({ own: [], stack: undefined }), ...skipAt, pull: stacked(20, 20) }), null);
});

test("the members of a stack are the open pull requests in it, bottom first", async () => {
  const pulls = [stacked(20, 20), { number: 9, head: { sha: "x" } }, stacked(3, 20, { number: 4535 }), stacked(1, 20, { number: 4531 }),
    { number: 8, head: { sha: "y" }, stack: { number: 99, position: 1, size: 2 } }];
  const github = { paginate: async () => pulls, rest: { pulls: { list: "list" } } };
  assert.deepEqual((await stackMembers({ github, ...skipAt, number: 4533 })).map((p) => p.number), [4531, 4535, 4552]);
});

test("a lower head counts as covered only when the top's head contains it", async () => {
  for (const [status, covered] of [["ahead", true], ["identical", true], ["behind", false], ["diverged", false]]) {
    const github = fakeFilesGithub({ own: [], compareStatus: status });
    assert.equal(await containsCommit({ github, ...skipAt, sha: "lower", headSha: "top" }), covered, status);
  }
});
