// Resolve publication authority from the released tag and repository history.
// RELEASES.md remains the source of truth for supported maintenance lines.
function version(tag) {
  const match = /^(\d+)\.(\d+)\.(\d+)(?:\.(\d+))?(?:-rc\.(\d+))?$/.exec(tag);
  if (!match) throw new Error(`Unsupported release tag: ${tag}`);
  const parts = match
    .slice(1, 5)
    .filter((value) => value !== undefined)
    .map(Number);
  return { parts, line: parts.slice(0, -1).join("."), candidate: !!match[5] };
}
function supported(policy, line) {
  return policy.split("\n").some((row) => {
    const cells = row.split("|").map((cell) => cell.trim());
    return (
      cells[1]?.replace(/\.x$/, "") === line &&
      /^Supported(?:\s|$)/.test(cells[2])
    );
  });
}
function revision(pom) {
  const source = pom.replace(/<!--[\s\S]*?-->/g, "");
  const value = /<revision>\s*([^<]+?)\s*<\/revision>/.exec(source)?.[1];
  if (value) return value;
  // Supported 3.2 branches predate the single revision property.
  const legacy = ["major", "minor", "state", "fix"].map(
    (name) =>
      new RegExp(`<${name}\\.version>\\s*(\\d+)\\s*</${name}\\.version>`).exec(
        source,
      )?.[1],
  );
  if (
    legacy.every((value) => value !== undefined) &&
    /<version>\s*\$\{major.version\}\.\$\{minor.version\}\.\$\{state.version\}\.\$\{fix.version\}\s*<\/version>/.test(
      source,
    )
  ) {
    return legacy.join(".");
  }
  throw new Error("Missing release revision in pom.xml");
}
module.exports = async function publicationSource({
  github,
  owner,
  repo,
  run,
  policy,
}) {
  if (run.event === "push" && run.head_branch === "develop") return "develop";
  if (run.event !== "release")
    throw new Error(
      "Only develop pushes and published releases may publish images",
    );
  const tag = run.head_branch;
  const target = version(tag);
  const api = github.rest.repos;
  const { data: release } = await api.getReleaseByTag({ owner, repo, tag });
  if (
    release.draft ||
    release.tag_name !== tag ||
    release.prerelease !== target.candidate
  ) {
    throw new Error(
      "Release metadata does not match the stable or candidate tag",
    );
  }
  const { data: commit } = await api.getCommit({
    owner,
    repo,
    ref: `refs/tags/${tag}`,
  });
  if (commit.sha !== run.head_sha)
    throw new Error("Release tag no longer points to the tested commit");
  async function pom(ref) {
    const { data } = await api.getContent({
      owner,
      repo,
      path: "pom.xml",
      ref,
    });
    if (data.encoding !== "base64" || !data.content)
      throw new Error("Cannot read release version");
    return revision(Buffer.from(data.content, "base64").toString("utf8"));
  }
  const declared = await pom(run.head_sha);
  if (
    declared !== tag &&
    !(target.candidate && declared === tag.replace(/-rc\.\d+$/, ""))
  ) {
    throw new Error("Tag does not match the tested application version");
  }
  const current = version(await pom("refs/heads/main"));
  let branch = "main";
  if (target.candidate) {
    branch = `release/${target.line}.x`;
  } else if (target.line !== current.line) {
    const older = target.parts.some((part, index) => {
      if (index >= target.parts.length - 1) return false;
      return (
        target.parts
          .slice(0, index)
          .every((value, i) => value === current.parts[i]) &&
        part < current.parts[index]
      );
    });
    if (!older || !supported(policy, target.line))
      throw new Error(
        "Release is not from the current or an older supported line",
      );
    branch = target.line === "3.2.2" ? "3.2.2.x" : `release/${target.line}.x`;
  }
  // base=candidate, head=branch: ahead/identical means the branch contains it.
  const { data: comparison } = await api.compareCommitsWithBasehead({
    owner,
    repo,
    basehead: `${run.head_sha}...refs/heads/${branch}`,
  });
  if (!["ahead", "identical"].includes(comparison.status)) {
    throw new Error(`Tested release commit is not contained in ${branch}`);
  }
  return tag;
};
