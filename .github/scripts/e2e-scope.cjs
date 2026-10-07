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
//   all: the others post a pending checkpoint, and the top's run posts its
//   result on each of them whose head it contains. Stack metadata that is
//   missing or malformed means "run E2E".
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

// GitHub lists at most this many files for a comparison, and cuts the list off
// silently past it. A list this long cannot be judged, so E2E runs.
const MAX_COMPARED_FILES = 300;

function positiveInteger(value) {
  return Number.isInteger(value) && value >= 1;
}

// The stack a pull request defers to, or null when it runs E2E itself: not in
// a stack, or the top of its stack. The REST pull request object and the
// pull_request event payload both carry `stack` with `number`, `position`
// (1 is the bottom) and `size`. Metadata that is not three positive integers
// with the position below the size runs E2E.
function stackDeferral(pull) {
  const stack = pull && pull.stack;
  if (!stack) {
    return null;
  }
  const { number, position, size } = stack;
  if (![number, position, size].every(positiveInteger) || position >= size) {
    return null;
  }
  return { number, position, size };
}

// The top pull request of a stack holds every commit of the stack, so its
// branch, not its own focused diff, is what the stack changes.
function isStackTop(pull) {
  const stack = pull && pull.stack;
  return Boolean(stack) && positiveInteger(stack.position) && positiveInteger(stack.size) && stack.position === stack.size;
}

// The files a stack changes as a whole: the comparison from the stack's base
// branch to the top's head. Returns null when GitHub may have cut the list off.
async function listStackFiles({ github, owner, repo, pull }) {
  const { data } = await github.rest.repos.compareCommitsWithBasehead({
    owner,
    repo,
    basehead: `${pull.stack.base.ref}...${pull.head.sha}`,
  });
  const files = data.files || [];
  return files.length >= MAX_COMPARED_FILES ? null : files;
}

// Why a pull request skips E2E, as { kind, description }, or null when it runs
// E2E. The description is the checkpoint status text, so both workflows and
// the gate say the same thing. The stack rule comes first: a pull request below
// the top waits for the top's result whatever it changes, and the top is
// judged on the whole stack's files, since its own diff is one layer.
async function skipReason({ github, owner, repo, pull }) {
  const deferral = stackDeferral(pull);
  if (deferral) {
    return {
      kind: "stack",
      ...deferral,
      description: `Stacked PR ${deferral.position} of ${deferral.size} (stack #${deferral.number}); waiting for the top of the stack to run E2E`,
    };
  }
  const files = isStackTop(pull)
    ? await listStackFiles({ github, owner, repo, pull })
    : await listPullRequestFiles({ github, owner, repo, pullNumber: pull.number });
  if (files !== null && !requiresE2E(files)) {
    return { kind: "docs", files: files.length, description: "Documentation-only change; E2E suites skipped" };
  }
  return null;
}

// The open pull requests of stack `number`, bottom first. GitHub has no lookup
// by stack, so this reads the open pull requests and keeps the stack's.
async function stackMembers({ github, owner, repo, number }) {
  const pulls = await github.paginate(github.rest.pulls.list, { owner, repo, state: "open", per_page: 100 });
  return pulls
    .filter((pull) => pull.stack && pull.stack.number === number)
    .sort((a, b) => a.stack.position - b.stack.position);
}

// Whether `sha` is in `headSha`'s history. The top's E2E result covers a lower
// pull request only if the top's head contains the lower head; one pushed
// after the stack was built is not covered until the stack is rebuilt.
async function containsCommit({ github, owner, repo, sha, headSha }) {
  const { data } = await github.rest.repos.compareCommitsWithBasehead({ owner, repo, basehead: `${sha}...${headSha}` });
  return data.status === "ahead" || data.status === "identical";
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
  isStackTop,
  listStackFiles,
  skipReason,
  stackMembers,
  containsCommit,
  listPullRequestFiles,
  openPullsAtHead,
  resolvePullRequest,
  MAX_LISTED_FILES,
  MAX_COMPARED_FILES,
};
