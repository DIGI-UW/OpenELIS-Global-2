#!/usr/bin/env bash
#
# deploy.sh — reproducible lifecycle for the dual-subdomain OpenELIS demo:
#   amr.openelis-global.org        — Microbiology MVP (OGC-782)
#   analyzers.openelis-global.org  — Analyzer Types & Mapping + harness (OGC-1054)
# One host, two isolated stacks behind one umbrella reverse proxy, per-domain LE.
#
# Everything here is idempotent and scripted — no hand-run steps. Config is in
# .env (copy from .env.example). Reuses: the harness's own bootstrap.sh, the
# repo's certbot pattern (generate-certs.sh / certbot-renew.sh), the PR's own
# analyzer + microbiology seed fixtures, and the deploy-vector-demo.sh
# detached-runner-with-polling pattern (from-source builds take 20-40 min).
#
# TRANSPORT: every automated command runs over SSM (aws ssm send-command), not
# SSH. This was an SSH-based script originally; SSH proved unreliable here
# (client egress IP churns between calls, breaking the SG /32 rule mid-poll —
# it once silently masked a build stall as "still building"). SSM needs no SG
# rule, no key, no stable client IP — only a live `aws` session. `connect` is
# the one exception (a human wanting an interactive shell) and still uses SSH.
#
# USAGE
#   ./deploy.sh status              # AWS + instance + both HTTPS + container states (read-only)
#   ./deploy.sh connect [cmd…]      # SSH shell — interactive only, needs your IP in the SG (see below)
#   ./deploy.sh configure           # install Docker/git, install renew cron (idempotent)
#   ./deploy.sh deploy [--yes]      # build + bring up router + both stacks on self-signed (detached + polled)
#   ./deploy.sh certs               # issue LE certs for both domains (run AFTER DNS resolves to the host)
#   ./deploy.sh seed                # seed reviewable demo data: analyzers (9-device fleet) + a microbiology case
#   ./deploy.sh up-to-certs --yes   # configure -> deploy -> certs -> seed
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
[ -f "$HERE/.env" ] && . "$HERE/.env" || { echo "!! $HERE/.env missing — copy .env.example to .env and fill it in" >&2; exit 1; }

: "${REGION:?}" "${INSTANCE_ID:?}" "${EIP:?}" "${SG_ID:?}" "${OS_USER:?}" "${SSH_KEY:?}"
: "${AMR_DOMAIN:?}" "${ANALYZERS_DOMAIN:?}" "${AMR_BRANCH:?}" "${ANALYZERS_BRANCH:?}"
: "${EDGE_DIR:?}" "${AMR_DIR:?}" "${ANALYZERS_DIR:?}" "${LETSENCRYPT_EMAIL:?}"
SSH_KEY_EXPANDED="${SSH_KEY/#\~/$HOME}"
REPO_URL="${REPO_URL:-https://github.com/DIGI-UW/OpenELIS-Global-2.git}"
EDGE_BRANCH="${EDGE_BRANCH:-deploy/dual-subdomain-amr-analyzers}"
ROUTER_SUBDIR="deploy/dual-subdomain/router"
LE_DIR="$EDGE_DIR/$ROUTER_SUBDIR/letsencrypt"
WEBROOT_DIR="$EDGE_DIR/$ROUTER_SUBDIR/certbot"
DEPLOY_TIMEOUT="${DEPLOY_TIMEOUT:-3000}"
REMOTE_RUNNER="/home/$OS_USER/oe-dual-deploy.run.sh"
REMOTE_LOG="/home/$OS_USER/oe-dual-deploy.log"
DONE_MARK="OE_DUAL_DEPLOY_DONE_OK"
# astm-simulator's static analyzer-net IP (docker-compose.analyzer-test.yml IPAM,
# unchanged by the analyzers override — only the "default" network is remapped).
# The host can always reach this directly; it's never published as a host port.
MOCK_URL="${MOCK_URL:-http://172.21.1.100:8080}"

C_I=$'\033[1;36m'; C_W=$'\033[1;33m'; C_E=$'\033[1;31m'; C_0=$'\033[0m'
log()  { printf '%s>> %s%s\n' "$C_I" "$*" "$C_0"; }
warn() { printf '%s!! %s%s\n' "$C_W" "$*" "$C_0" >&2; }
die()  { printf '%s!! %s%s\n' "$C_E" "$*" "$C_0" >&2; exit 1; }

require_aws() { aws sts get-caller-identity --region "$REGION" >/dev/null 2>&1 || die "no AWS session — run 'aws login' first"; }
my_ip() { curl -fsS --max-time 10 https://checkip.amazonaws.com | tr -d '[:space:]'; }

# ---- SSM transport: base64-encode the script body (sidesteps all quoting),
# send it, poll for terminal status, print stdout, propagate failure. ----
SSM_POLL_TIMEOUT="${SSM_POLL_TIMEOUT:-600}"
ssm_run() {
  local script="$1" b64 cmdid status deadline
  b64="$(printf '%s' "$script" | base64 | tr -d '\n')"
  cmdid="$(aws ssm send-command --region "$REGION" --instance-ids "$INSTANCE_ID" \
    --document-name "AWS-RunShellScript" \
    --parameters "commands=[\"echo $b64 | base64 -d > /tmp/deploy-cmd.sh && bash /tmp/deploy-cmd.sh\"]" \
    --query "Command.CommandId" --output text 2>&1)" || { warn "ssm send-command failed: $cmdid"; return 1; }
  deadline=$(( $(date +%s) + SSM_POLL_TIMEOUT ))
  status=InProgress
  while [ "$(date +%s)" -lt "$deadline" ]; do
    status="$(aws ssm get-command-invocation --region "$REGION" --command-id "$cmdid" --instance-id "$INSTANCE_ID" --query Status --output text 2>/dev/null || echo InProgress)"
    case "$status" in Success|Failed|Cancelled|TimedOut) break ;; esac
    sleep 3
  done
  aws ssm get-command-invocation --region "$REGION" --command-id "$cmdid" --instance-id "$INSTANCE_ID" --query "StandardOutputContent" --output text 2>/dev/null
  if [ "$status" != Success ]; then
    warn "ssm command $status (id $cmdid)"
    aws ssm get-command-invocation --region "$REGION" --command-id "$cmdid" --instance-id "$INSTANCE_ID" --query "StandardErrorContent" --output text 2>&1 >&2 || true
    return 1
  fi
}
# Fire-and-forget: same as ssm_run but does not wait — used to launch the
# detached deploy runner, which we then watch via a separate polling loop.
ssm_fire() {
  local script="$1" b64
  b64="$(printf '%s' "$script" | base64 | tr -d '\n')"
  aws ssm send-command --region "$REGION" --instance-ids "$INSTANCE_ID" \
    --document-name "AWS-RunShellScript" \
    --parameters "commands=[\"echo $b64 | base64 -d > /tmp/deploy-cmd.sh && bash /tmp/deploy-cmd.sh\"]" \
    --query "Command.CommandId" --output text 2>&1
}

# ---- SSH: interactive `connect` only. Needs the caller's current IP allowed
# in the SG (idempotent, same approach deploy-vector-demo.sh uses). ----
allow_ssh_ingress() {
  local ip; ip="$(my_ip)"
  aws ec2 describe-security-groups --region "$REGION" --group-ids "$SG_ID" \
    --query "SecurityGroups[0].IpPermissions[?FromPort==\`22\`].IpRanges[].CidrIp" --output text 2>/dev/null \
    | tr '\t' '\n' | grep -qx "$ip/32" && return
  log "authorizing SSH from $ip/32"
  aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG_ID" \
    --ip-permissions "IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=$ip/32,Description=deploy.sh-connect}]" \
    >/dev/null 2>&1 || warn "ingress authorize failed (may already exist)"
}
SSH_OPTS=(-i "$SSH_KEY_EXPANDED" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20 -o ServerAliveInterval=30)

# ---- the on-box deploy runner (heredoc; local vars interpolate here, \$(...) runs remote) ----
_runner_script() {
  cat <<RUNNER
#!/usr/bin/env bash
set -euo pipefail
echo "[deploy] start \$(date -u)"
sync_checkout() { # dir branch
  local dir="\$1" br="\$2"
  if [ -d "\$dir/.git" ]; then
    sudo chown -R "$OS_USER":"$OS_USER" "\$dir" 2>/dev/null || true
    git -C "\$dir" fetch --depth 1 origin "\$br"
    git -C "\$dir" checkout -f -B "\$br" FETCH_HEAD
  else
    sudo mkdir -p "\$dir" && sudo chown "$OS_USER":"$OS_USER" "\$dir"
    git clone --depth 1 --single-branch --branch "\$br" "$REPO_URL" "\$dir"
  fi
  git -C "\$dir" submodule update --init --depth 1 dataexport tools/openelis-analyzer-bridge tools/analyzer-mock-server 2>/dev/null || true
  echo "[deploy] \$dir -> \$br @\$(git -C "\$dir" rev-parse --short HEAD)"
}
sync_checkout "$EDGE_DIR" "$EDGE_BRANCH"
sync_checkout "$AMR_DIR" "$AMR_BRANCH"
sync_checkout "$ANALYZERS_DIR" "$ANALYZERS_BRANCH"

docker network create oe-edge 2>/dev/null || true
mkdir -p "$LE_DIR" "$WEBROOT_DIR"

echo "[deploy] router up (self-signed until certs issued)"
cd "$EDGE_DIR/$ROUTER_SUBDIR"
AMR_DOMAIN="$AMR_DOMAIN" ANALYZERS_DOMAIN="$ANALYZERS_DOMAIN" \\
  docker compose -p oe-edge -f docker-compose.router.yml up -d --build

echo "[deploy] amr stack build+up"
cd "$AMR_DIR"
docker compose -p amr -f build.docker-compose.yml \\
  -f "$EDGE_DIR/deploy/dual-subdomain/amr/docker-compose.override.yml" \\
  up -d --build certs db.openelis.org oe.openelis.org fhir.openelis.org frontend.openelis.org

# Build the analyzers webapp FROM SOURCE (like amr), using the harness's CI chain
# (build.docker-compose.yml + base + ci.analyzer-harness.yml) — NOT the dev chain
# (docker-compose.dev.yml), which host-mounts a pre-built target/OpenELIS-Global.war
# and fails ("mount dir onto file") when it's absent. Run from the repo root so the
# ci-harness ./tools/... build contexts resolve.
echo "[deploy] analyzers stack build+up (build-from-source CI harness chain)"
cd "$ANALYZERS_DIR"
mkdir -p projects/analyzer-harness/volume/analyzer-imports
docker compose -p analyzers \\
  -f build.docker-compose.yml \\
  -f projects/analyzer-harness/docker-compose.base.yml \\
  -f .github/ci/ci.analyzer-harness.yml \\
  -f "$EDGE_DIR/deploy/dual-subdomain/analyzers/docker-compose.override.yml" \\
  up -d --build certs db.openelis.org oe.openelis.org fhir.openelis.org frontend.openelis.org \\
        astm-simulator openelis-analyzer-bridge

echo "[deploy] waiting for both webapps healthy (up to 20 min)"
for i in \$(seq 1 120); do
  a=\$(docker inspect -f '{{.State.Health.Status}}' amr-openelisglobal-webapp 2>/dev/null || echo none)
  n=\$(docker inspect -f '{{.State.Health.Status}}' analyzers-openelisglobal-webapp 2>/dev/null || echo none)
  echo "[deploy]   amr=\$a analyzers=\$n (\$((i*10))s)"
  [ "\$a" = healthy ] && [ "\$n" = healthy ] && break
  sleep 10
done
echo "[deploy] container states:"; docker ps --format '   {{.Names}}: {{.Status}}'
echo "$DONE_MARK \$(date -u)"
RUNNER
}

_poll() {
  local deadline=$(( $(date +%s) + DEPLOY_TIMEOUT )) out
  while [ "$(date +%s)" -lt "$deadline" ]; do
    sleep 45
    out="$(ssm_run "tail -3 '$REMOTE_LOG' 2>/dev/null; echo ---; grep -q '$DONE_MARK' '$REMOTE_LOG' && echo DONE_OK; pgrep -f oe-dual-deploy.run.sh >/dev/null && echo RUNNING || echo STOPPED" 2>/dev/null || echo SSMFAIL)"
    # SSMFAIL must NOT be silently treated as "still building" — it means we
    # lost visibility (e.g. a transient AWS API error), not that the build progressed.
    if [ "$out" = SSMFAIL ]; then warn "SSM unreachable this round — can't read build state; retrying"; continue; fi
    printf '%s\n' "$out" | grep -vE '^(DONE_OK|RUNNING|STOPPED|---)$' | sed 's/^/   /'
    printf '%s' "$out" | grep -q DONE_OK && { log "runner finished"; return 0; }
    printf '%s' "$out" | grep -q STOPPED && { warn "runner stopped without success marker — inspect: ./deploy.sh connect \"tail -60 $REMOTE_LOG\""; return 1; }
    log "still building… (polling 45s, up to ${DEPLOY_TIMEOUT}s)"
  done
  warn "poll deadline reached; VM may still be building — ./deploy.sh connect \"tail -60 $REMOTE_LOG\""; return 1
}

cmd_configure() {
  require_aws
  log "installing Docker + git on the host (idempotent)"
  ssm_run '
    if ! command -v docker >/dev/null; then curl -fsSL https://get.docker.com | sudo sh; sudo usermod -aG docker '"$OS_USER"'; fi
    command -v git >/dev/null || { sudo apt-get update -qq && sudo apt-get install -y -qq git; }
    command -v envsubst >/dev/null || { sudo apt-get update -qq && sudo apt-get install -y -qq gettext-base; }  # bootstrap.sh renders templates
    docker --version; docker compose version | head -1' || die "configure failed"
  log "installing certbot renewal cron"
  ssm_run "sudo tee /etc/cron.d/oe-edge-certbot-renew >/dev/null <<'CRON'
# twice-daily LE renewal for the dual-subdomain demo (installed by deploy.sh)
17 3,15 * * * $OS_USER LETSENCRYPT_DIR=$LE_DIR CERTBOT_WEBROOT=$WEBROOT_DIR ROUTER_CONTAINER_NAME=oe-edge-router bash $EDGE_DIR/deploy/dual-subdomain/scripts/certbot-renew.sh >> /home/$OS_USER/certbot-renew.log 2>&1
CRON" || die "cron install failed"
  log "configure complete — next: ./deploy.sh deploy --yes"
}

cmd_deploy() {
  [ "${1:-}" = "--yes" ] || die "deploy rebuilds both stacks (long). Re-run: ./deploy.sh deploy --yes"
  require_aws
  log "writing detached runner + launching (nohup) — amr=$AMR_BRANCH analyzers=$ANALYZERS_BRANCH"
  ssm_run "cat > '$REMOTE_RUNNER' <<'RUNNEREOF'
$(_runner_script)
RUNNEREOF
chmod +x '$REMOTE_RUNNER'" >/dev/null || die "failed to write runner"
  ssm_run "cd ~ && nohup bash '$REMOTE_RUNNER' > '$REMOTE_LOG' 2>&1 & echo launched pid \$!" || die "failed to launch runner"
  log "polling until both stacks are up (safe to Ctrl-C; box keeps building — resume by re-running deploy, or connect+tail)"
  _poll || die "deploy did not complete cleanly"
  log "STACKS UP (self-signed). Once DNS resolves to $EIP: ./deploy.sh certs, then ./deploy.sh seed"
}

cmd_certs() {
  require_aws
  for d in "$AMR_DOMAIN" "$ANALYZERS_DOMAIN"; do
    got="$(dig +short "$d" | tail -1)"
    [ "$got" = "$EIP" ] || warn "DNS: $d -> ${got:-<none>} (expected $EIP) — ACME will fail until this resolves"
  done
  log "issuing certs for both domains on the host"
  ssm_run "AMR_DOMAIN=$AMR_DOMAIN ANALYZERS_DOMAIN=$ANALYZERS_DOMAIN LETSENCRYPT_EMAIL=$LETSENCRYPT_EMAIL LETSENCRYPT_STAGING=${LETSENCRYPT_STAGING:-false} LETSENCRYPT_DIR=$LE_DIR CERTBOT_WEBROOT=$WEBROOT_DIR bash $EDGE_DIR/deploy/dual-subdomain/scripts/generate-certs.sh" \
    || die "cert issuance failed"
  cmd_status
}

# Seed reviewable demo data on BOTH instances. Idempotent-ish (each run adds
# fresh, uniquely-suffixed records rather than erroring on conflict):
#   analyzers — the harness's own seed-analyzers.sh (9-device Madagascar fleet:
#               ASTM + HL7/MLLP + FILE, mock networks wired to the bridge)
#   amr       — scripts/seed-microbiology.sh (a bacteriology + sibling TB case,
#               reusing the PR's own test-fixture SQL) so /MicrobiologyWorklist
#               and /MicrobiologyCaseView/:caseId have something to review.
# NOTE: neither /MicrobiologyWorklist nor /MicrobiologyCaseView has a sidenav
# entry on this branch — they're real, working, unlinked routes. Reviewers need
# the direct URL (printed below); this is a product gap upstream, not something
# this deploy script should patch around by hand-editing the frontend nav.
cmd_seed() {
  require_aws
  log "seeding analyzers.openelis-global.org (9-device fleet via the harness's own seed script)"
  ssm_run "cd $ANALYZERS_DIR/projects/analyzer-harness && BASE_URL=https://$ANALYZERS_DOMAIN MOCK_URL=$MOCK_URL DB_CONTAINER=analyzers-openelisglobal-database ./seed-analyzers.sh" \
    || warn "analyzer seed failed — see output above"
  log "seeding amr.openelis-global.org (microbiology demo case)"
  ssm_run "cat > /tmp/seed-microbiology.sh <<'SEEDEOF'
$(cat "$HERE/scripts/seed-microbiology.sh")
SEEDEOF
chmod +x /tmp/seed-microbiology.sh
DB_CONTAINER=amr-openelisglobal-database BASE_URL=https://$AMR_DOMAIN /tmp/seed-microbiology.sh" \
    || warn "microbiology seed failed — see output above"
  log "seed complete. Microbiology worklist has NO sidenav entry — visit directly:"
  log "  https://$AMR_DOMAIN/MicrobiologyWorklist"
}

cmd_status() {
  require_aws
  log "instance"; aws ec2 describe-instances --region "$REGION" --instance-ids "$INSTANCE_ID" \
    --query "Reservations[0].Instances[0].[State.Name,PublicIpAddress,InstanceType]" --output text | sed 's/^/   /'
  for d in "$AMR_DOMAIN" "$ANALYZERS_DOMAIN"; do
    printf '   https://%s/ -> HTTP %s\n' "$d" "$(curl -sk -o /dev/null -w '%{http_code}' --max-time 15 "https://$d/" 2>/dev/null || echo 000)"
  done
  echo "   containers:"
  ssm_run "docker ps --format '{{.Names}}: {{.Status}}' | grep -E 'amr-|analyzers-|oe-edge' || true" | sed 's/^/     /' \
    || warn "remote status failed"
}

cmd_up_to_certs() { cmd_configure; cmd_deploy "${1:-}"; cmd_certs; cmd_seed; }

main() {
  local sub="${1:-help}"; shift || true
  case "$sub" in
    status) cmd_status ;;
    connect)
      allow_ssh_ingress
      if [ "$#" -gt 0 ]; then ssh "${SSH_OPTS[@]}" "$OS_USER@$EIP" "$@"; else ssh -t "${SSH_OPTS[@]}" "$OS_USER@$EIP"; fi ;;
    configure) cmd_configure ;;
    deploy) cmd_deploy "$@" ;;
    certs) cmd_certs ;;
    seed) cmd_seed ;;
    up-to-certs) cmd_up_to_certs "$@" ;;
    help|-h|--help) sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//' ;;
    *) die "unknown subcommand '$sub' (status|connect|configure|deploy|certs|seed|up-to-certs|help)" ;;
  esac
}
main "$@"
