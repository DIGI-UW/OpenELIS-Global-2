/**
 * The Analyzer Bridge's container and its data volume, for the story where a
 * profile revision disappears from the Bridge while OpenELIS still uses it.
 *
 * The container is found by its Compose project when `COMPOSE_PROJECT_NAME` is
 * set (every isolated stack has one); `HARNESS_BRIDGE_CONTAINER` overrides, and
 * the contract name `openelis-analyzer-bridge` is the fallback.
 */
import { execFileSync } from "child_process";

const CATALOG = "/data/openelis-analyzer-bridge/profile-catalog";

function docker(args: string[]): string {
  return execFileSync("docker", args, { encoding: "utf8" }).trim();
}

export function bridgeContainer(): string {
  const named = process.env.HARNESS_BRIDGE_CONTAINER?.trim();
  if (named) {
    return named;
  }
  const project = process.env.COMPOSE_PROJECT_NAME?.trim();
  if (project) {
    const found = docker([
      "ps",
      "-q",
      "--filter",
      `label=com.docker.compose.project=${project}`,
      "--filter",
      "label=com.docker.compose.service=openelis-analyzer-bridge",
    ]);
    if (found) {
      return found.split("\n")[0];
    }
  }
  return "openelis-analyzer-bridge";
}

/** Delete one authored profile revision's file from the Bridge's data volume. */
export function deleteSiteProfileRevision(profileId: string, revision: number) {
  if (!/^site\.[0-9a-f-]+$/.test(profileId) || !Number.isInteger(revision)) {
    throw new Error(`Not a site profile revision: ${profileId} ${revision}`);
  }
  docker([
    "exec",
    bridgeContainer(),
    "rm",
    "-f",
    `${CATALOG}/${profileId}/${revision}.json`,
  ]);
}

export function restartBridge() {
  docker(["restart", bridgeContainer()]);
}
