#!/usr/bin/env bash
#
# deploy-to-testing.sh — deploy the CURRENT working tree's OpenELIS build to a
# testing EC2 VM that runs the host-mounted dev stack (e.g.
# https://testing.openelis-global.org/).
#
# Shared by two callers so there is ONE source of truth:
#   * Local  : `scripts/deploy-to-testing.sh`            ("deploy local to testing")
#   * CI      : .github/workflows/deploy-testing.yml      ("deploy a passing PR commit")
#     — the workflow checks out the target commit, then runs THIS script.
#
# Mechanism (matches the VM, probed 2026-06): the analyzer-harness compose
# host-mounts the build artifacts into the dev-image containers —
#     target/OpenELIS-Global.war      -> webapp Tomcat   (image is just the base)
#     frontend/src, frontend/public   -> frontend dev container
# so a deploy = build the WAR, copy WAR + frontend source to the VM, and restart
# the two app containers. The TLS proxy, Postgres, and analyzer-harness infra
# stay up; on webapp restart Liquibase applies any new changesets (e.g. the
# label_preset tables). No image push and no remote branch checkout — the VM's
# compose/config is left exactly as configured.
#
# Requirements (caller env): JDK 21 + Maven on PATH (JAVA_HOME set to 21), an SSH
# identity that can reach $DEPLOY_HOST as $REMOTE_USER, rsync, and a Docker
# daemon NOT required locally (the WAR is built with Maven, not Docker).
#
# Overridable via env:
#   DEPLOY_HOST     ssh host/alias            (default: testing.openelis-global.org)
#   REMOTE_USER     ssh user                  (default: ubuntu)
#   REMOTE_DIR      repo path on the VM        (default: /home/ubuntu/OpenELIS-Global-2)
#   HARNESS_SUBDIR  compose dir under REMOTE_DIR (default: projects/analyzer-harness)
#   COMPOSE_FILES   compose -f chain           (default: the dev/base/analyzer-test overlay)
#   WEBAPP_SVC      webapp compose service     (default: oe.openelis.org)
#   FRONTEND_SVC    frontend compose service   (default: frontend.openelis.org)
#   SKIP_BUILD=1    reuse an existing target/OpenELIS-Global.war (skip mvn)
#
set -euo pipefail

DEPLOY_HOST="${DEPLOY_HOST:-testing.openelis-global.org}"
REMOTE_USER="${REMOTE_USER:-ubuntu}"
REMOTE_DIR="${REMOTE_DIR:-/home/ubuntu/OpenELIS-Global-2}"
HARNESS_SUBDIR="${HARNESS_SUBDIR:-projects/analyzer-harness}"
COMPOSE_FILES="${COMPOSE_FILES:--f docker-compose.dev.yml -f docker-compose.base.yml -f docker-compose.analyzer-test.yml}"
WEBAPP_SVC="${WEBAPP_SVC:-oe.openelis.org}"
FRONTEND_SVC="${FRONTEND_SVC:-frontend.openelis.org}"
SSH_OPTS="${SSH_OPTS:--o StrictHostKeyChecking=accept-new -o ConnectTimeout=30}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

TARGET="${REMOTE_USER}@${DEPLOY_HOST}"
REF_DESC="$(git describe --always --dirty --tags 2>/dev/null || git rev-parse --short HEAD)"
BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"

log() { printf '\033[1;36m>> %s\033[0m\n' "$*"; }

log "Deploying ${BRANCH} (${REF_DESC}) -> ${TARGET}:${REMOTE_DIR}"

# 1. Build the WAR (skip tests — the mounted WAR is the deliverable).
if [[ "${SKIP_BUILD:-0}" == "1" && -f target/OpenELIS-Global.war ]]; then
    log "SKIP_BUILD=1 — reusing existing target/OpenELIS-Global.war"
else
    log "Building WAR (mvn clean package -DskipTests -Dmaven.test.skip=true)…"
    mvn -q clean package -DskipTests -Dmaven.test.skip=true
fi
[[ -f target/OpenELIS-Global.war ]] || { echo "ERROR: target/OpenELIS-Global.war not found after build" >&2; exit 1; }

# 1b. Capture a restore point on the VM (cheap insurance before overwriting a
#     live box). Backend rollback = restore the .bak WAR; frontend rollback =
#     git checkout the mounted dirs (they are tracked in the VM's repo).
log "Capturing restore point on the VM…"
ssh $SSH_OPTS "$TARGET" "set -e; cd '${REMOTE_DIR}'; \
    pb=\$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?'); ps=\$(git rev-parse --short HEAD 2>/dev/null || echo '?'); \
    if [ -f target/OpenELIS-Global.war ]; then cp -f target/OpenELIS-Global.war target/OpenELIS-Global.war.bak; bak=yes; else bak=none; fi; \
    echo \"restore-point: branch=\$pb sha=\$ps war.bak=\$bak\""

# 2. Push artifacts. WAR -> target/, frontend source -> frontend/{src,public}.
#    --delete scoped to src/ and public/ keeps the VM's frontend matching this ref.
log "Copying WAR ($(du -h target/OpenELIS-Global.war | cut -f1)) -> VM…"
scp $SSH_OPTS target/OpenELIS-Global.war "${TARGET}:${REMOTE_DIR}/target/OpenELIS-Global.war"

log "Syncing frontend/src + frontend/public -> VM…"
rsync -az --delete -e "ssh $SSH_OPTS" frontend/src/    "${TARGET}:${REMOTE_DIR}/frontend/src/"
rsync -az --delete -e "ssh $SSH_OPTS" frontend/public/ "${TARGET}:${REMOTE_DIR}/frontend/public/"

# 3. Recreate the two app containers; leave proxy/DB/harness untouched.
log "Recreating ${WEBAPP_SVC} + ${FRONTEND_SVC} on the VM…"
# shellcheck disable=SC2029  # intentional client-side expansion of the var chain
ssh $SSH_OPTS "$TARGET" "set -e; cd '${REMOTE_DIR}/${HARNESS_SUBDIR}'; \
    docker compose ${COMPOSE_FILES} up -d --force-recreate '${WEBAPP_SVC}' '${FRONTEND_SVC}'"

# 4. Verify the REAL app, not just the container. (container-up != app-up:
#    Tomcat 500s if Liquibase fails a checksum or the WAR fails to explode.)
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-180}"
log "Waiting up to ${HEALTH_TIMEOUT}s for https://${DEPLOY_HOST}/ to answer…"
deadline=$(( $(date +%s) + HEALTH_TIMEOUT )); code=000
while [ "$(date +%s)" -lt "$deadline" ]; do
    code=$(curl -sk -o /dev/null -w '%{http_code}' "https://${DEPLOY_HOST}/" 2>/dev/null || echo 000)
    case "$code" in 200|301|302) break;; esac
    sleep 10
done
log "HTTPS https://${DEPLOY_HOST}/ -> HTTP ${code}"

log "Scanning webapp logs for Liquibase / fatal errors…"
ssh $SSH_OPTS "$TARGET" "cd '${REMOTE_DIR}/${HARNESS_SUBDIR}'; docker compose ${COMPOSE_FILES} logs --tail=250 '${WEBAPP_SVC}' 2>&1 | grep -iE 'liquibase|checksum|ValidationFailed|SEVERE|ERROR|Exception' | tail -25 || true"

if ! printf '%s' "$code" | grep -qE '^(200|301|302)$'; then
    printf '\033[1;31m!! app did not return a healthy HTTP code (got %s)\033[0m\n' "$code" >&2
    cat >&2 <<ROLLBACK
ROLLBACK (run on the VM):
  cd ${REMOTE_DIR} \\
    && cp -f target/OpenELIS-Global.war.bak target/OpenELIS-Global.war \\
    && git checkout -- frontend/src frontend/public \\
    && cd ${HARNESS_SUBDIR} \\
    && docker compose ${COMPOSE_FILES} up -d --force-recreate ${WEBAPP_SVC} ${FRONTEND_SVC}
ROLLBACK
    exit 1
fi

log "Deployed ${BRANCH} (${REF_DESC}) -> https://${DEPLOY_HOST}/ (HTTP ${code})"
log "Rollback: on the VM restore target/OpenELIS-Global.war.bak + 'git checkout -- frontend/src frontend/public', then recreate the two services."
