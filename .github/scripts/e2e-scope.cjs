// Decides whether a pull request needs the E2E suites at all.
//
// Two rules skip them, and both fail towards running E2E:
//
// - Documentation only. The rule is a skip-list, not an include-list: E2E is
//   skipped only when every changed path is documentation. A path this list
//   misses makes E2E run when it did not need to, which is safe; an
//   include-list that missed a path would give a code change a green
//   checkpoint without running anything.
// - Below the top of a stack. GitHub runs every pull request in a stack as if
//   it targeted the stack's base, so a 20-PR stack would run E2E 20 times. The
//   top pull request contains every commit of the stack, so its run covers them
//   all; the others defer to it. Stack metadata that is missing or malformed
//   means "run E2E".
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

// The stack a pull request defers to, or null when it runs E2E itself: not in
// a stack, or the top of its stack. The REST pull request object and the
// pull_request event payload both carry `stack` with `number`, `position`
// (1 is the bottom) and `size`. Anything that is not a positive integer below
// `size` runs E2E.
function stackDeferral(pull) {
  const stack = pull && pull.stack;
  if (!stack) {
    return null;
  }
  const { position, size } = stack;
  if (!Number.isInteger(position) || !Number.isInteger(size) || position < 1 || size < 1 || position >= size) {
    return null;
  }
  return { number: stack.number, position, size };
}

// Why a pull request skips E2E, as { kind, description }, or null when it runs
// E2E. The description is the checkpoint status text, so both workflows and
// the gate say the same thing.
async function skipReason({ github, owner, repo, pull }) {
  const files = await listPullRequestFiles({ github, owner, repo, pullNumber: pull.number });
  if (!requiresE2E(files)) {
    return { kind: "docs", files: files.length, description: "Documentation-only change; E2E suites skipped" };
  }
  const deferral = stackDeferral(pull);
  if (deferral) {
    return {
      kind: "stack",
      files: files.length,
      ...deferral,
      description: `Stacked PR ${deferral.position} of ${deferral.size} (stack #${deferral.number}); E2E runs on the top of the stack`,
    };
  }
  return null;
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

module.exports = {
  isDocsPath,
  requiresE2E,
  stackDeferral,
  skipReason,
  listPullRequestFiles,
  openPullsAtHead,
  resolvePullRequest,
  MAX_LISTED_FILES,
};
