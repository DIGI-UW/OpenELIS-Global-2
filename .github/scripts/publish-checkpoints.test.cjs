const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const wait = require("./publish-checkpoints.cjs");
const sha = "a".repeat(40);
const buildContext = "03 Checkpoint - E2E / build-123-2";
const PUSH_SUITE = 10;
const PR_SUITE = 20;
const backend = (conclusion, id = 1, head_sha = sha, suite = PUSH_SUITE) => ({
  name: "01 Checkpoint - Backend",
  id,
  head_sha,
  status: "completed",
  conclusion,
  check_suite: { id: suite },
});
const workflowRuns = {
  async listWorkflowRunsForRepo(request) {
    assert.equal(request.head_sha, sha);
    return {
      data: {
        workflow_runs: [
          { event: "push", check_suite_id: PUSH_SUITE },
          { event: "pull_request", check_suite_id: PR_SUITE },
        ],
      },
    };
  },
};

async function run(checks, state = "success", context = buildContext) {
  let clock = 0;
  return wait({
    owner: "test",
    repo: "test",
    sha,
    buildRunId: 123,
    buildRunAttempt: 2,
    core: { info() {} },
    github: {
      rest: {
        actions: workflowRuns,
        repos: {
          async listCommitStatusesForRef(request) {
            assert.equal(request.ref, sha);
            return {
              data: state ? [{ context, state }] : [],
            };
          },
        },
        checks: {
          async listForRef(request) {
            assert.equal(request.ref, sha);
            if (request.check_name === "02 Checkpoint - Frontend") {
              return {
                data: {
                  check_runs: [
                    { ...backend("success"), name: request.check_name },
                  ],
                },
              };
            }
            return { data: { check_runs: checks } };
          },
        },
      },
    },
    timeoutMs: 2,
    pollMs: 1,
    now: () => clock,
    sleep: async () => {
      clock++;
    },
  });
}

test("all checkpoints for the candidate allow publication", () =>
  run([backend("success")]));
test("an upgrade/backend failure blocks publication even when E2E passes", async () => {
  await assert.rejects(run([backend("failure")]), /Backend.*failure/);
});
test("the latest backend result overrides an earlier successful run", async () => {
  await assert.rejects(
    run([backend("success"), backend("failure", 2)]),
    /Backend.*failure/,
  );
});
test("a checkpoint from another commit cannot authorize publication", async () => {
  await assert.rejects(
    run([backend("success", 1, "b".repeat(40))]),
    /Timed out/,
  );
});
test("missing, pending, and skipped backend checks cannot authorize publication", async () => {
  await assert.rejects(run([]), /Timed out/);
  await assert.rejects(
    run([{ ...backend(null), status: "in_progress" }]),
    /Timed out/,
  );
  await assert.rejects(run([backend("skipped")]), /Backend.*skipped/);
});
test("failed or missing E2E checks cannot authorize publication", async () => {
  await assert.rejects(run([backend("success")], "failure"), /E2E.*failure/);
  await assert.rejects(run([backend("success")], null), /Timed out/);
});
test("another build or attempt cannot authorize publishing rebuilt images", async () => {
  for (const context of [
    "03 Checkpoint - E2E",
    "03 Checkpoint - E2E / build-122-2",
    "03 Checkpoint - E2E / build-123-1",
  ]) {
    await assert.rejects(
      run([backend("success")], "success", context),
      /Timed out/,
    );
    await assert.rejects(
      run([backend("success")], "failure", context),
      /Timed out/,
    );
  }
});
test("a pull request run on the same commit cannot block or authorize publication", async () => {
  // PR #4772 (develop -> main) has develop as its head, so its pull_request
  // backend run shares the candidate sha; when the PR moved on, that run
  // failed and the gate refused the push's deployment (2026-10-20).
  await run([backend("success", 1), backend("failure", 2, sha, PR_SUITE)]);
  await assert.rejects(
    run([
      { ...backend(null, 1), status: "in_progress" },
      backend("failure", 2, sha, PR_SUITE),
    ]),
    /Timed out/,
  );
  await assert.rejects(
    run([backend("success", 2, sha, PR_SUITE)]),
    /Timed out/,
  );
});
test("a GitHub API timeout is retried, a refused request is not", async () => {
  // Run 39466371613 ended with "We couldn't respond to your request in time".
  const flaky = (status) => {
    let calls = 0;
    return {
      async listWorkflowRunsForRepo(request) {
        calls++;
        if (calls === 1) {
          throw Object.assign(new Error("We couldn't respond in time"), {
            status,
          });
        }
        return workflowRuns.listWorkflowRunsForRepo(request);
      },
    };
  };
  const attempt = (actions) => {
    let clock = 0;
    return wait({
      owner: "test",
      repo: "test",
      sha,
      buildRunId: 123,
      buildRunAttempt: 2,
      core: { info() {} },
      github: {
        rest: {
          actions,
          repos: {
            async listCommitStatusesForRef() {
              return { data: [{ context: buildContext, state: "success" }] };
            },
          },
          checks: {
            async listForRef(request) {
              return {
                data: {
                  check_runs: [
                    { ...backend("success"), name: request.check_name },
                  ],
                },
              };
            },
          },
        },
      },
      timeoutMs: 5,
      pollMs: 1,
      now: () => clock,
      sleep: async () => {
        clock++;
      },
    });
  };
  await attempt(flaky(504));
  await attempt(flaky(undefined));
  await assert.rejects(attempt(flaky(403)), /couldn't respond/);
});
test("a missing source build identity fails closed", async () => {
  await assert.rejects(wait({ sha }), /requires the source build/);
});
test("release publication produces the backend checkpoint required by the gate", () => {
  const workflow = fs.readFileSync(".github/workflows/backend.yml", "utf8");
  assert.match(workflow, /\n  release:\n    types: \[published\]\n/);
});
test("the default wait outlasts a backend suite that finishes 70 minutes after E2E", async () => {
  const minute = 60 * 1000;
  let clock = 0;
  await wait({
    owner: "test",
    repo: "test",
    sha,
    buildRunId: 123,
    buildRunAttempt: 2,
    core: { info() {} },
    github: {
      rest: {
        actions: workflowRuns,
        repos: {
          async listCommitStatusesForRef() {
            return { data: [{ context: buildContext, state: "success" }] };
          },
        },
        checks: {
          async listForRef(request) {
            if (request.check_name === "02 Checkpoint - Frontend") {
              return {
                data: {
                  check_runs: [
                    { ...backend("success"), name: request.check_name },
                  ],
                },
              };
            }
            return {
              data: {
                check_runs: clock >= 70 * minute ? [backend("success")] : [],
              },
            };
          },
        },
      },
    },
    now: () => clock,
    sleep: async (ms) => {
      clock += ms;
    },
  });
  assert.ok(clock >= 70 * minute);
});
