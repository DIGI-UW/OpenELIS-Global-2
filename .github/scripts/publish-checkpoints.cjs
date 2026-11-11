const REQUIRED_CHECKS = ["01 Checkpoint - Backend", "02 Checkpoint - Frontend"];

/** Server errors, rate limits and network failures; a 4xx answer is final. */
function isTransient(error) {
  const status = Number(error && error.status);
  return !status || status >= 500 || status === 429;
}

module.exports = async function waitForPublishCheckpoints({
  github,
  core,
  owner,
  repo,
  sha,
  buildRunId,
  buildRunAttempt,
  timeoutMs = 120 * 60 * 1000,
  pollMs = 30 * 1000,
  now = Date.now,
  sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
}) {
  if (
    !/^[1-9][0-9]*$/.test(String(buildRunId)) ||
    !/^[1-9][0-9]*$/.test(String(buildRunAttempt))
  ) {
    throw new Error("Publication requires the source build run and attempt");
  }
  const E2E = `03 Checkpoint - E2E / build-${buildRunId}-${buildRunAttempt}`;
  // A pull request whose head is this commit (develop -> main) runs the same
  // checkpoints on the same sha; only the candidate's own push or release runs
  // decide whether it is published.
  async function readCandidate() {
    const runs = await github.rest.actions.listWorkflowRunsForRepo({
      owner,
      repo,
      head_sha: sha,
      per_page: 100,
    });
    const candidateSuites = new Set(
      runs.data.workflow_runs
        .filter((run) => !String(run.event).startsWith("pull_request"))
        .map((run) => run.check_suite_id),
    );
    const [statuses, ...checks] = await Promise.all([
      github.rest.repos.listCommitStatusesForRef({
        owner,
        repo,
        ref: sha,
        per_page: 100,
      }),
      ...REQUIRED_CHECKS.map((name) =>
        github.rest.checks.listForRef({
          owner,
          repo,
          ref: sha,
          check_name: name,
          filter: "latest",
          per_page: 100,
        }),
      ),
    ]);
    return { candidateSuites, statuses, checks };
  }

  const started = now();
  while (now() - started < timeoutMs) {
    let candidateSuites, statuses, checks;
    try {
      ({ candidateSuites, statuses, checks } = await readCandidate());
    } catch (error) {
      // A slow or failing GitHub API answer is not a verdict on the candidate;
      // the next poll asks again (2026-10-20: one timeout ended a deploy's wait).
      if (!isTransient(error)) {
        throw error;
      }
      core.info(
        `GitHub API did not answer (${error.status || error.message}); retrying.`,
      );
      await sleep(pollMs);
      continue;
    }
    const e2e = statuses.data.find((entry) => entry.context === E2E);
    const required = REQUIRED_CHECKS.map((name, index) => ({
      name,
      check: checks[index].data.check_runs
        .filter(
          (entry) =>
            entry.name === name &&
            entry.head_sha === sha &&
            candidateSuites.has(entry.check_suite?.id),
        )
        .sort((left, right) => right.id - left.id)[0],
    }));

    if (e2e && ["failure", "error"].includes(e2e.state)) {
      throw new Error(`${E2E} is ${e2e.state} for ${sha}`);
    }
    for (const { name, check } of required) {
      if (check?.status === "completed" && check.conclusion !== "success") {
        throw new Error(`${name} is ${check.conclusion} for ${sha}`);
      }
    }
    if (
      e2e?.state === "success" &&
      required.every(
        ({ check }) =>
          check?.status === "completed" && check.conclusion === "success",
      )
    ) {
      core.info(
        `Backend, frontend and E2E passed for ${sha}, build ${buildRunId}, attempt ${buildRunAttempt}.`,
      );
      return;
    }
    core.info(
      `Waiting for ${sha}: ${required.map(({ name, check }) => `${name}=${check?.conclusion || check?.status || "missing"}`).join(", ")}, E2E=${e2e?.state || "missing"}.`,
    );
    await sleep(pollMs);
  }
  throw new Error(
    `Timed out waiting for backend, frontend and E2E checkpoints for ${sha}`,
  );
};
