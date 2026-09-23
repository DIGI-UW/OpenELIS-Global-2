// Decides whether a pull request needs the E2E suites at all.
//
// The rule is a skip-list, not an include-list: E2E is skipped only when every
// changed path is documentation. A path this list misses makes E2E run when it
// did not need to, which is safe; an include-list that missed a path would
// give a code change a green checkpoint without running anything.
//
// Both sides use this file. `03 - E2E` calls it to skip the Shared Build, and
// `E2E / Tests` calls it again from the default branch to decide the
// `03 Checkpoint - E2E` status. The second call is the one that counts: a pull
// request can change its own copy of `03 - E2E`, but not the default branch's
// copy of this script.

const DOCS_PREFIXES = ["docs/", "specs/", ".github/ISSUE_TEMPLATE/"];

// Pull requests listing more files than this are treated as needing E2E,
// because the files API stops returning entries past 3000.
const MAX_LISTED_FILES = 3000;

function isDocsPath(path) {
  if (typeof path !== "string" || path === "") {
    return false;
  }
  return path.endsWith(".md") || DOCS_PREFIXES.some((prefix) => path.startsWith(prefix));
}

// A rename counts on both sides: moving code into docs/ still removes code.
function requiresE2E(files) {
  if (!Array.isArray(files) || files.length === 0 || files.length >= MAX_LISTED_FILES) {
    return true;
  }
  return files.some(
    (file) => !isDocsPath(file.filename) || (file.previous_filename !== undefined && !isDocsPath(file.previous_filename)),
  );
}

async function listPullRequestFiles({ github, owner, repo, pullNumber }) {
  return github.paginate(github.rest.pulls.listFiles, {
    owner,
    repo,
    pull_number: pullNumber,
    per_page: 100,
  });
}

// Finds the open pull request whose head is exactly headSha. workflow_run
// payloads leave pull_requests empty for forks, so this searches by head ref.
async function findPullRequest({ github, owner, repo, headOwner, headBranch, headSha }) {
  if (!headOwner || !headBranch || !headSha) {
    return null;
  }
  const { data } = await github.rest.pulls.list({
    owner,
    repo,
    state: "open",
    head: `${headOwner}:${headBranch}`,
    per_page: 100,
  });
  return data.find((pull) => pull.head && pull.head.sha === headSha) || null;
}

module.exports = { isDocsPath, requiresE2E, listPullRequestFiles, findPullRequest, MAX_LISTED_FILES };
