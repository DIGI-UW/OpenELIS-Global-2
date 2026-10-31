const test = require("node:test");
const assert = require("node:assert/strict");
const publish = require("./publish-release-manifest.cjs");
const { createHash } = require("node:crypto");
function fixture() {
  const calls = [];
  const state = { assets: [], commit: "a".repeat(40), draft: false };
  const manifest = {
    appSha: state.commit,
    appBranch: "3.4.0",
    images: Object.fromEntries(
      [
        ["oe.openelis.org", ""],
        ["db.openelis.org", "-database"],
        ["fhir.openelis.org", "-fhir"],
        ["frontend.openelis.org", "-frontend"],
        ["proxy", "-proxy"],
        ["certs", "-certs"],
        ["openelis-analyzer-bridge", "-analyzer-bridge"],
        ["astm-simulator", "-analyzer-mock"],
      ].map(([name, suffix]) => [
        name,
        `itechuw/openelis-global-2${suffix}@sha256:${"b".repeat(64)}`,
      ]),
    ),
  };
  const api = {
    getCommit: async () => ({ data: { sha: state.commit } }),
    getReleaseByTag: async () => ({
      data: {
        id: 1,
        tag_name: manifest.appBranch,
        draft: state.draft,
        prerelease: false,
      },
    }),
    listReleaseAssets: () => {},
    uploadReleaseAsset: async (data) => {
      calls.push(data);
    },
  };
  return {
    manifest,
    state,
    calls,
    run: () =>
      publish({
        github: { rest: { repos: api }, paginate: async () => state.assets },
        owner: "DIGI-UW",
        repo: "OpenELIS-Global-2",
        manifest,
      }),
  };
}
test("publishes complete digest set and retries without replacing an asset", async () => {
  const f = fixture();
  assert.equal(await f.run(), "published");
  const upload = f.calls[0];
  const data = JSON.parse(upload.data);
  assert.equal(data.schemaVersion, 1);
  assert.equal(data.images.certs, f.manifest.images.certs);
  f.state.assets = [
    {
      name: upload.name,
      digest:
        "sha256:" + createHash("sha256").update(upload.data).digest("hex"),
    },
  ];
  assert.equal(await f.run(), "unchanged");
  assert.equal(f.calls.length, 1);
});
for (const [name, change] of [
  ["development", (f) => (f.manifest.appBranch = "develop")],
  ["missing initializer", (f) => delete f.manifest.images.certs],
  [
    "moving tag",
    (f) =>
      (f.manifest.images.proxy = "itechuw/openelis-global-2-proxy:develop"),
  ],
  [
    "wrong repository",
    (f) => (f.manifest.images.certs = f.manifest.images.proxy),
  ],
  [
    "different namespace",
    (f) =>
      (f.manifest.images.certs = f.manifest.images.certs.replace(
        "itechuw/",
        "other/",
      )),
  ],
  ["moved release", (f) => (f.state.commit = "c".repeat(40))],
  ["draft", (f) => (f.state.draft = true)],
  [
    "changed published asset",
    (f) =>
      (f.state.assets = [
        {
          name: "deployment-manifest.json",
          digest: "sha256:" + "d".repeat(64),
        },
      ]),
  ],
])
  test(`rejects ${name} without publishing`, async () => {
    const f = fixture();
    change(f);
    await assert.rejects(f.run());
    assert.equal(f.calls.length, 0);
  });
