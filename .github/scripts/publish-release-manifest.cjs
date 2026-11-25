const { createHash } = require("node:crypto");

// Called only after the existing source and three-checkpoint publication gates.
module.exports = async function publishReleaseManifest({
  github,
  owner,
  repo,
  manifest,
}) {
  const tag = manifest.appBranch;
  if (
    !/^\d+\.\d+\.\d+(?:\.\d+)?(?:-rc\.\d+)?$/.test(tag || "") ||
    !/^[a-f0-9]{40}$/.test(manifest.appSha || "")
  ) {
    throw new Error(
      "A release manifest must identify its tested tag and commit",
    );
  }
  const services = {
    "oe.openelis.org": "",
    "db.openelis.org": "-database",
    "fhir.openelis.org": "-fhir",
    "frontend.openelis.org": "-frontend",
    proxy: "-proxy",
    certs: "-certs",
    "openelis-analyzer-bridge": "-analyzer-bridge",
    "astm-simulator": "-analyzer-mock",
  };
  if (
    Object.keys(manifest.images || {})
      .sort()
      .join() !== Object.keys(services).sort().join()
  ) {
    throw new Error(
      "Release manifest must contain the complete tested image set",
    );
  }
  let namespace;
  for (const [service, suffix] of Object.entries(services)) {
    const match = new RegExp(
      `^([a-z0-9][a-z0-9_-]*)/openelis-global-2${suffix}@sha256:[a-f0-9]{64}$`,
    ).exec(manifest.images[service]);
    if (!match || (namespace && namespace !== match[1]))
      throw new Error("Invalid published image digest");
    namespace = match[1];
  }
  const api = github.rest.repos;
  const { data: commit } = await api.getCommit({
    owner,
    repo,
    ref: `refs/tags/${tag}`,
  });
  if (commit.sha !== manifest.appSha)
    throw new Error("Release tag moved after testing");
  const { data: release } = await api.getReleaseByTag({ owner, repo, tag });
  if (
    release.draft ||
    release.tag_name !== tag ||
    release.prerelease !== tag.includes("-rc.")
  ) {
    throw new Error("Release metadata does not match the tested tag");
  }
  // Stable ordering makes retries byte-identical without overwriting assets.
  const data = Buffer.from(
    JSON.stringify(
      {
        schemaVersion: 1,
        appSha: manifest.appSha,
        appBranch: tag,
        images: Object.fromEntries(
          Object.keys(services)
            .sort()
            .map((key) => [key, manifest.images[key]]),
        ),
      },
      null,
      2,
    ) + "\n",
  );
  const name = "deployment-manifest.json";
  const digest = "sha256:" + createHash("sha256").update(data).digest("hex");
  const assets = await github.paginate(api.listReleaseAssets, {
    owner,
    repo,
    release_id: release.id,
    per_page: 100,
  });
  const existing = assets.find((asset) => asset.name === name);
  if (existing) {
    if (existing.digest !== digest)
      throw new Error(
        "Published deployment manifest differs; create a new release",
      );
    return "unchanged";
  }
  await api.uploadReleaseAsset({
    owner,
    repo,
    release_id: release.id,
    name,
    data,
    headers: {
      "content-type": "application/json",
      "content-length": data.length,
    },
  });
  return "published";
};
