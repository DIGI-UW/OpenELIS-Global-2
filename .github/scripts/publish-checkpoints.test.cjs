const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const wait = require("./publish-checkpoints.cjs");
const sha = "a".repeat(40);
const buildContext = "03 Checkpoint - E2E / build-123-2";
const backend = (conclusion, id = 1, head_sha = sha) => ({
  name: "01 Checkpoint - Backend",
  id,
  head_sha,
  status: "completed",
  conclusion,
});

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

test("both checkpoints for the candidate allow publication", () =>
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
test("a missing source build identity fails closed", async () => {
  await assert.rejects(wait({ sha }), /requires the source build/);
});
test("release publication produces the backend checkpoint required by the gate", () => {
  const workflow = fs.readFileSync(".github/workflows/backend.yml", "utf8");
  assert.match(workflow, /\n  release:\n    types: \[published\]\n/);
});
