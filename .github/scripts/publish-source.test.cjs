const test = require("node:test");
const assert = require("node:assert/strict");
const validate = require("./publish-source.cjs");
const sha = "a".repeat(40);
const policy =
  "| 3.2.2.x | Supported (legacy) | 3.2.2.0 |\n| 3.2.3.x | Supported | 3.2.3.0 |\n| 3.3 | Supported | 3.3.1 |";
function fixture(overrides = {}) {
  const state = {
    tag: "3.3.1",
    declared: "3.3.1",
    main: "3.3.0",
    status: "ahead",
    tagSha: sha,
    ...overrides,
  };
  const calls = [];
  const github = {
    rest: {
      repos: {
        async getReleaseByTag({ tag }) {
          assert.equal(tag, state.tag);
          return {
            data: {
              tag_name: tag,
              draft: false,
              prerelease: state.tag.includes("-rc."),
              ...state.release,
            },
          };
        },
        async getCommit({ ref }) {
          assert.equal(ref, `refs/tags/${state.tag}`);
          return { data: { sha: state.tagSha } };
        },
        async getContent({ ref }) {
          return {
            data: {
              encoding: "base64",
              content: Buffer.from(
                ref !== "refs/heads/main" && state.pom
                  ? state.pom
                  : `<project><properties><revision>${ref === "refs/heads/main" ? state.main : state.declared}</revision></properties></project>`,
              ).toString("base64"),
            },
          };
        },
        async compareCommitsWithBasehead({ basehead }) {
          calls.push(basehead);
          if (state.missingBranch) throw new Error("Not Found");
          return { data: { status: state.status } };
        },
      },
    },
  };
  return {
    calls,
    check: () =>
      validate({
        github,
        owner: "DIGI-UW",
        repo: "OpenELIS-Global-2",
        policy: state.policy ?? policy,
        run: { event: "release", head_branch: state.tag, head_sha: sha },
      }),
  };
}
test("current stable patch must be on main", async () => {
  const f = fixture();
  assert.equal(await f.check(), "3.3.1");
  assert.deepEqual(f.calls, [`${sha}...refs/heads/main`]);
});
test("release candidate remains allowed on its release branch", async () => {
  const f = fixture({ tag: "3.4.0-rc.1", declared: "3.4.0" });
  await f.check();
  assert.deepEqual(f.calls, [`${sha}...refs/heads/release/3.4.x`]);
});
test("older supported legacy patch uses its maintenance branch", async () => {
  const f = fixture({ tag: "3.2.2.1", declared: "3.2.2.1" });
  await f.check();
  assert.deepEqual(f.calls, [`${sha}...refs/heads/3.2.2.x`]);
});
test("four-part supported patch retains its distinct release line", async () => {
  const f = fixture({ tag: "3.2.3.1", declared: "3.2.3.1" });
  await f.check();
  assert.deepEqual(f.calls, [`${sha}...refs/heads/release/3.2.3.x`]);
});
test("supported branch absence does not fall back to develop or main", async () => {
  await assert.rejects(
    fixture({
      tag: "3.2.3.1",
      declared: "3.2.3.1",
      missingBranch: true,
    }).check(),
    /Not Found/,
  );
});
test("modern maintenance line uses the same supported policy table", async () => {
  const f = fixture({ tag: "3.3.2", declared: "3.3.2", main: "3.4.0" });
  await f.check();
  assert.deepEqual(f.calls, [`${sha}...refs/heads/release/3.3.x`]);
});
test("untested moved tag cannot authorize publishing", async () => {
  await assert.rejects(
    fixture({ tagSha: "b".repeat(40) }).check(),
    /tested commit/,
  );
});
test("a version label cannot disguise a different application version", async () => {
  await assert.rejects(
    fixture({ declared: "3.4.0-SNAPSHOT" }).check(),
    /tested application version/,
  );
});
for (const status of ["behind", "diverged", undefined])
  test(`unmerged release source is refused (${status})`, async () => {
    await assert.rejects(fixture({ status }).check(), /not contained/);
  });
for (const tag of ["3.2.1.12", "3.4.0", "3.4.0;echo injected"])
  test(`unauthorized stable version is refused (${tag})`, async () => {
    await assert.rejects(
      fixture({ tag, declared: tag }).check(),
      /older supported|Unsupported release tag/,
    );
  });
for (const release of [
  { draft: true },
  { prerelease: true },
  { tag_name: "3.3.0" },
])
  test(`release metadata mismatch is refused (${JSON.stringify(release)})`, async () => {
    await assert.rejects(fixture({ release }).check(), /metadata/);
  });
test("develop publication needs no release lookup", async () => {
  assert.equal(
    await validate({ run: { event: "push", head_branch: "develop" } }),
    "develop",
  );
});
for (const event of ["pull_request", "workflow_dispatch", "push"])
  test(`unrelated event or branch is refused (${event})`, async () => {
    await assert.rejects(
      validate({ run: { event, head_branch: "feature" } }),
      /Only develop/,
    );
  });

test("retiring a maintenance line removes its publication exception", async () => {
  await assert.rejects(
    fixture({
      tag: "3.2.2.1",
      declared: "3.2.2.1",
      policy: policy.replace("Supported (legacy)", "Unsupported"),
    }).check(),
    /older supported/,
  );
});

const legacyPom =
  "<project><version>${major.version}.${minor.version}.${state.version}.${fix.version}</version><properties><major.version>3</major.version><minor.version>2</minor.version><state.version>2</state.version><fix.version>1</fix.version></properties></project>";
test("legacy maintenance uses its actual four-property Maven version", async () => {
  const f = fixture({ tag: "3.2.2.1", pom: legacyPom });
  await f.check();
});
test("legacy version mismatch and missing property are refused", async () => {
  await assert.rejects(
    fixture({ tag: "3.2.2.2", pom: legacyPom }).check(),
    /tested application version/,
  );
  await assert.rejects(
    fixture({
      tag: "3.2.2.1",
      pom: legacyPom.replace("<fix.version>1</fix.version>", ""),
    }).check(),
    /Missing release revision/,
  );
});
