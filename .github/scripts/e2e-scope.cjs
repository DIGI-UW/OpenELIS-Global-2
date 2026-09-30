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

// Open pull requests whose head is exactly headSha, found by head ref.
// workflow_run payloads leave pull_requests empty for forks, so the head ref is
// the only lookup that works for both.
async function openPullsAtHead({ github, owner, repo, headOwner, headBranch, headSha }) {
  if (!headOwner || !headBranch || !headSha) {
    return [];
  }
  const { data } = await github.rest.pulls.list({
    owner,
    repo,
    state: "open",
    head: `${headOwner}:${headBranch}`,
    per_page: 100,
  });
  return data.filter((pull) => pull.head && pull.head.sha === headSha);
}

// Identifies the pull request that triggered the build, or returns null, which
// means "run E2E".
//
// claimedNumber comes from the triggering run's artifact. That run executes the
// pull request's own workflow, so the number is untrusted: it is used only as a
// lookup key, and accepted only if the API confirms that pull request is open
// with exactly this head sha. A missing or forged number therefore runs E2E.
//
// The checkpoint is a commit status on the head sha, so two open pull requests
// with the same head share it. A skip judged from one pull request's files
// would pass the other's, so any second open pull request at the head also runs
// E2E, even when the claimed one is identified exactly.
async function resolvePullRequest({ github, owner, repo, claimedNumber, headOwner, headBranch, headSha }) {
  const text = typeof claimedNumber === "string" ? claimedNumber.trim() : "";
  if (!/^[1-9][0-9]{0,9}$/.test(text) || !headSha) {
    return null;
  }
  const number = Number(text);
  let pull;
  try {
    ({ data: pull } = await github.rest.pulls.get({ owner, repo, pull_number: number }));
  } catch (error) {
    return null;
  }
  if (!pull || pull.state !== "open" || !pull.head || pull.head.sha !== headSha) {
    return null;
  }
  const atHead = await openPullsAtHead({ github, owner, repo, headOwner, headBranch, headSha });
  if (atHead.length !== 1 || atHead[0].number !== pull.number) {
    return null;
  }
  return pull;
}

module.exports = { isDocsPath, requiresE2E, listPullRequestFiles, openPullsAtHead, resolvePullRequest, MAX_LISTED_FILES };
