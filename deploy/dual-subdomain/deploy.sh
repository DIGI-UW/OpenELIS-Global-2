#!/usr/bin/env bash
#
# deploy.sh — reproducible lifecycle for the dual-subdomain OpenELIS demo:
#   amr.openelis-global.org        — Microbiology MVP (OGC-782)
#   analyzers.openelis-global.org  — Analyzer Types & Mapping + harness (OGC-1054)
# One host, two isolated stacks behind one umbrella reverse proxy, per-domain LE.
#
# Everything here is idempotent and scripted — no hand-run SSH steps. Config is
# in .env (copy from .env.example). Reuses: the harness compose_args_local chain,
# scripts/generate-certs.sh + certbot-renew.sh, and the deploy-vector-demo.sh
# detached-runner-with-polling pattern (from-source builds take 20-40 min).
#
# USAGE
#   ./deploy.sh status              # AWS + instance + both HTTPS + deployed commits (read-only)
#   ./deploy.sh connect [cmd…]      # SSH shell (or run a remote command)
#   ./deploy.sh configure           # install Docker/git, make the 3 checkouts, install renew cron (idempotent)
#   ./deploy.sh deploy [--yes]      # build + bring up router + both stacks on self-signed (detached + polled)
#   ./deploy.sh certs               # issue LE certs for both domains (run AFTER DNS resolves to the host)
#   ./deploy.sh up-to-certs --yes   # configure -> deploy -> (certs, if DNS already resolves)
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

C_I=$'\033[1;36m'; C_W=$'\033[1;33m'; C_E=$'\033[1;31m'; C_0=$'\033[0m'
log()  { printf '%s>> %s%s\n' "$C_I" "$*" "$C_0"; }
warn() { printf '%s!! %s%s\n' "$C_W" "$*" "$C_0" >&2; }
die()  { printf '%s!! %s%s\n' "$C_E" "$*" "$C_0" >&2; exit 1; }

require_aws() { aws sts get-caller-identity --region "$REGION" >/dev/null 2>&1 || die "no AWS session — run 'aws login' first"; }
my_ip() { curl -fsS --max-time 10 https://checkip.amazonaws.com | tr -d '[:space:]'; }

# Idempotently allow SSH from the current public IP (the runner's IP may differ
# from the fixed admin rule). Same approach as deploy-vector-demo.sh.
allow_ssh_ingress() {
  local ip; ip="$(my_ip)"
  aws ec2 describe-security-groups --region "$REGION" --group-ids "$SG_ID" \
    --query "SecurityGroups[0].IpPermissions[?FromPort==\`22\`].IpRanges[].CidrIp" --output text 2>/dev/null \
    | tr '\t' '\n' | grep -qx "$ip/32" && { log "SSH ingress for $ip/32 present"; return; }
  log "authorizing SSH from $ip/32"
  aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG_ID" \
    --ip-permissions "IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=$ip/32,Description=deploy.sh}]" \
    >/dev/null 2>&1 || warn "ingress authorize failed (may already exist)"
}

SSH_OPTS=(-i "$SSH_KEY_EXPANDED" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20 -o ServerAliveInterval=30)
remote() { ssh "${SSH_OPTS[@]}" "$OS_USER@$EIP" "$@"; }

# ---- the on-box deploy runner (heredoc; local vars interpolate, \$(...) runs remote) ----
_write_runner() {
  remote "cat > '$REMOTE_RUNNER'" <<RUNNER
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
AMR_DOMAIN="$AMR_DOMAIN" ANALYZERS_DOMAIN="$ANALYZERS_DOMAIN" \
  docker compose -p oe-edge -f docker-compose.router.yml up -d --build

echo "[deploy] amr stack build+up"
cd "$AMR_DIR"
docker compose -p amr -f build.docker-compose.yml \
  -f "$EDGE_DIR/deploy/dual-subdomain/amr/docker-compose.override.yml" \
  up -d --build certs db.openelis.org oe.openelis.org fhir.openelis.org frontend.openelis.org

# Build the analyzers webapp FROM SOURCE (like amr), using the harness's CI chain
# (compose_args_ci: build.docker-compose.yml + base + ci.analyzer-harness.yml).
# NOT the dev chain (docker-compose.dev.yml), which host-mounts a pre-built
# target/OpenELIS-Global.war and fails ("mount dir onto file") when it's absent.
# Run from the repo root so the ci-harness `./tools/...` build contexts resolve.
echo "[deploy] analyzers stack build+up (build-from-source CI harness chain)"
cd "$ANALYZERS_DIR"
mkdir -p projects/analyzer-harness/volume/analyzer-imports
docker compose -p analyzers \
  -f build.docker-compose.yml \
  -f projects/analyzer-harness/docker-compose.base.yml \
  -f .github/ci/ci.analyzer-harness.yml \
  -f "$EDGE_DIR/deploy/dual-subdomain/analyzers/docker-compose.override.yml" \
  up -d --build certs db.openelis.org oe.openelis.org fhir.openelis.org frontend.openelis.org \
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
  remote "chmod +x '$REMOTE_RUNNER'"
}

_poll() {
  local deadline=$(( $(date +%s) + DEPLOY_TIMEOUT )) out
  while [ "$(date +%s)" -lt "$deadline" ]; do
    sleep 45
    allow_ssh_ingress >/dev/null 2>&1 || true
    out="$(remote "tail -3 '$REMOTE_LOG' 2>/dev/null; echo ---; grep -q '$DONE_MARK' '$REMOTE_LOG' && echo DONE_OK; pgrep -f oe-dual-deploy.run.sh >/dev/null && echo RUNNING || echo STOPPED" 2>/dev/null || echo SSHFAIL)"
    # SSHFAIL must NOT be silently treated as "still building" — it means we lost
    # visibility (e.g. egress-IP churn), not that the build is progressing.
    if [ "$out" = SSHFAIL ]; then warn "SSH unreachable this round (egress IP churn?) — can't read build state; retrying"; continue; fi
    printf '%s\n' "$out" | grep -vE '^(DONE_OK|RUNNING|STOPPED|---)$' | sed 's/^/   /'
    printf '%s' "$out" | grep -q DONE_OK && { log "runner finished"; return 0; }
    printf '%s' "$out" | grep -q STOPPED && { warn "runner stopped without success marker — inspect: ./deploy.sh connect 'tail -60 $REMOTE_LOG'"; return 1; }
    log "still building… (polling 45s, up to ${DEPLOY_TIMEOUT}s)"
  done
  warn "poll deadline reached; VM may still be building — ./deploy.sh connect 'tail -60 $REMOTE_LOG'"; return 1
}

cmd_configure() {
  require_aws; allow_ssh_ingress
  log "installing Docker + git on the host (idempotent)"
  remote 'set -e
    if ! command -v docker >/dev/null; then curl -fsSL https://get.docker.com | sudo sh; sudo usermod -aG docker '"$OS_USER"'; fi
    command -v git >/dev/null || { sudo apt-get update -qq && sudo apt-get install -y -qq git; }
    command -v envsubst >/dev/null || { sudo apt-get update -qq && sudo apt-get install -y -qq gettext-base; }  # bootstrap.sh renders templates
    docker --version; docker compose version | head -1'
  log "installing certbot renewal cron"
  remote "sudo tee /etc/cron.d/oe-edge-certbot-renew >/dev/null <<CRON
# twice-daily LE renewal for the dual-subdomain demo (installed by deploy.sh)
17 3,15 * * * $OS_USER LETSENCRYPT_DIR=$LE_DIR CERTBOT_WEBROOT=$WEBROOT_DIR ROUTER_CONTAINER_NAME=oe-edge-router bash $EDGE_DIR/deploy/dual-subdomain/scripts/certbot-renew.sh >> /home/$OS_USER/certbot-renew.log 2>&1
CRON"
  log "configure complete — next: ./deploy.sh deploy --yes"
}

cmd_deploy() {
  [ "${1:-}" = "--yes" ] || die "deploy rebuilds both stacks (long). Re-run: ./deploy.sh deploy --yes"
  require_aws; allow_ssh_ingress
  log "writing detached runner + launching (nohup) — amr=$AMR_BRANCH analyzers=$ANALYZERS_BRANCH"
  _write_runner
  remote "cd ~ && nohup bash '$REMOTE_RUNNER' > '$REMOTE_LOG' 2>&1 & echo '   launched pid '\$!"
  log "polling until both stacks are up (safe to Ctrl-C; VM keeps building — resume with connect+tail)"
  _poll || die "deploy did not complete cleanly"
  log "STACKS UP (self-signed). Once DNS resolves to $EIP, run: ./deploy.sh certs"
}

cmd_certs() {
  require_aws; allow_ssh_ingress
  for d in "$AMR_DOMAIN" "$ANALYZERS_DOMAIN"; do
    local got; got="$(dig +short "$d" | tail -1)"
    [ "$got" = "$EIP" ] || warn "DNS: $d -> ${got:-<none>} (expected $EIP) — ACME will fail until this resolves"
  done
  log "issuing certs for both domains on the host"
  remote "AMR_DOMAIN=$AMR_DOMAIN ANALYZERS_DOMAIN=$ANALYZERS_DOMAIN LETSENCRYPT_EMAIL=$LETSENCRYPT_EMAIL LETSENCRYPT_STAGING=${LETSENCRYPT_STAGING:-false} LETSENCRYPT_DIR=$LE_DIR CERTBOT_WEBROOT=$WEBROOT_DIR bash $EDGE_DIR/deploy/dual-subdomain/scripts/generate-certs.sh"
  cmd_status
}

cmd_status() {
  require_aws
  log "instance"; aws ec2 describe-instances --region "$REGION" --instance-ids "$INSTANCE_ID" \
    --query "Reservations[0].Instances[0].[State.Name,PublicIpAddress,InstanceType]" --output text | sed 's/^/   /'
  for d in "$AMR_DOMAIN" "$ANALYZERS_DOMAIN"; do
    printf '   https://%s/ -> HTTP %s\n' "$d" "$(curl -sk -o /dev/null -w '%{http_code}' --max-time 15 "https://$d/" 2>/dev/null || echo 000)"
  done
  allow_ssh_ingress >/dev/null 2>&1 || true
  remote "echo '   containers:'; docker ps --format '     {{.Names}}: {{.Status}}' 2>/dev/null | grep -E 'amr-|analyzers-|oe-edge' || true" || warn "remote status failed"
}

cmd_up_to_certs() { cmd_configure; cmd_deploy "${1:-}"; cmd_certs; }

main() {
  local sub="${1:-help}"; shift || true
  case "$sub" in
    status) cmd_status ;;
    connect) if [ "$#" -gt 0 ]; then allow_ssh_ingress; remote "$@"; else allow_ssh_ingress; ssh -t "${SSH_OPTS[@]}" "$OS_USER@$EIP"; fi ;;
    configure) cmd_configure ;;
    deploy) cmd_deploy "$@" ;;
    certs) cmd_certs ;;
    up-to-certs) cmd_up_to_certs "$@" ;;
    help|-h|--help) sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//' ;;
    *) die "unknown subcommand '$sub' (status|connect|configure|deploy|certs|up-to-certs|help)" ;;
  esac
}
main "$@"
