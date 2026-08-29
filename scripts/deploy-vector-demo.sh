#!/usr/bin/env bash
#
# deploy-vector-demo.sh — lifecycle helper for the V-04 Vector Surveillance demo
# server.
#
# WHY THIS EXISTS
#   The demo EC2 was provisioned ad-hoc via AWS CLI and deployed by hand; the
#   procedure lived only in a chat transcript + a scratchpad SSH key that gets
#   wiped between sessions. This script is the durable, version-controlled source
#   of truth so the reconnect/deploy/seed steps never have to be reconstructed
#   again. Companion note: reference_vector_demo_ec2 in the Claude memory dir.
#
# THE SERVER (AWS, region us-west-2)
#   Instance   $INSTANCE_ID (t3.large, Ubuntu 22.04, 30GB gp3, ami-0e1601cee784a69a2)
#   Elastic IP $EIP         -> $HOST
#   Sec group  $SG_ID       (vector-demo-sg; ingress 22=your-IP, 80/443=public)
#   SSH user   ubuntu       (key access via EC2 Instance Connect — see below)
#
#   EIP and HOST have no built-in defaults: the deployment address is environment
#   data, not source, so it is not committed here. Export them (or source a local
#   untracked env file) before running any command that talks to the server:
#     export EIP=<elastic-ip> HOST=<fqdn>
#   INSTANCE_ID and SG_ID are likewise overridable via env.
#
# WHAT RUNS ON THE VM (built on the box, not host-mounted)
#   ~/OpenELIS-Global-2            (branch 372-vector-surveillance-reporting)
#   ~/openelis-indonesia-distro    (branch main; compose + healthcheck + config-perms
#             + proxy AND the per-city catalog. Deploy pins instance id = VECTOR_INSTANCE.)
#   ~/openelis-madagascar-test-harness  (orchestrator: scripts/restart-stack.sh)
#   Deploy  = restart-stack.sh --clean --rebuild   (--clean wipes volumes = full reset;
#             --rebuild rebuilds the WAR + frontend from OE_REPO at the checked-out branch)
#   Catalog = config-import from the distro's per-city CSVs for VECTOR_INSTANCE (sample
#             types, tests, sections, dictionaries, species, trap types) — created at
#             boot, NOT seeded — the same path a real Indonesia deployment runs. Result
#             significance is matched to tests by name; base-fallback domains (e.g.
#             test-results) resolve from the distro top-level or the OE classpath. The
#             rebuild deletes stale *-checksums.properties so config-import re-imports.
#   Seed    = client-side Playwright (frontend/playwright vector-surveillance-seed),
#             transactional data ONLY (sites, backdated collections, identifications,
#             results) via the REST API against BASE_URL — NO SQL, NO psql.
#
# ACCESS MODEL
#   No long-lived .pem is kept. Each run generates an ephemeral SSH keypair,
#   authorizes your current public IP on port 22 in the SG, and pushes the public
#   key to the instance with EC2 Instance Connect (valid ~60s), then SSHes with
#   the private key. Requires a live AWS session (`aws login`) — the one step this
#   script cannot perform for you (interactive SSO).
#
# USAGE
#   ./scripts/deploy-vector-demo.sh status          # auth + instance + HTTPS + deployed commit (read-only)
#   ./scripts/deploy-vector-demo.sh connect [cmd…]  # open a shell (or run a remote command)
#   ./scripts/deploy-vector-demo.sh deploy --yes    # DESTRUCTIVE: full reset -> rebuild latest (config-import catalog) -> re-inject host LE cert -> API seed -> verify (detached rebuild + polled)
#   ./scripts/deploy-vector-demo.sh logs            # tail the detached deploy log + status (resume a poll)
#   ./scripts/deploy-vector-demo.sh seed            # (re)seed transactional data only via the REST API (client-side Playwright)
#   ./scripts/deploy-vector-demo.sh provision       # break-glass: recreate the instance if it is gone
#
# Every value below is overridable via env (e.g. OE_BRANCH=some-branch ./deploy-vector-demo.sh deploy --yes).
set -euo pipefail

REGION="${REGION:-us-west-2}"
INSTANCE_ID="${INSTANCE_ID:-i-05e265d7fe2bc9630}"
EIP="${EIP:-}"
HOST="${HOST:-}"
SG_ID="${SG_ID:-sg-049d715dea927aa77}"
OS_USER="${OS_USER:-ubuntu}"
OE_BRANCH="${OE_BRANCH:-372-vector-surveillance-reporting}"
# Canonical distro. The demo runs the real per-city config (VECTOR_INSTANCE) so it
# represents an actual Indonesia deployment, not a synthetic core-only config.
DISTRO_BRANCH="${DISTRO_BRANCH:-main}"
# Config instance id the demo loads (a real city, so config-import uses the per-city
# catalog jakarta/bogor ship — the path production runs).
VECTOR_INSTANCE="${VECTOR_INSTANCE:-jakarta}"
AMI="${AMI:-ami-0e1601cee784a69a2}"       # Ubuntu 22.04 (us-west-2); provision only

# The reset+rebuild is long (~10-20 min) and destructive, so it runs DETACHED on
# the VM (nohup) writing to REMOTE_LOG; deploy/logs then poll. This survives a
# dropped SSH/tool timeout without leaving a half-wiped stack.
REMOTE_RUNNER="/home/${OS_USER}/vector-demo-deploy.run.sh"
REMOTE_LOG="/home/${OS_USER}/vector-demo-deploy.log"
DONE_MARK="VDEMO_DEPLOY_DONE_OK"
DEPLOY_TIMEOUT="${DEPLOY_TIMEOUT:-2400}"  # seconds to poll before giving up (40 min)

C_INFO=$'\033[1;36m'; C_WARN=$'\033[1;33m'; C_ERR=$'\033[1;31m'; C_OFF=$'\033[0m'
log()  { printf '%s>> %s%s\n' "$C_INFO" "$*" "$C_OFF"; }
warn() { printf '%s!! %s%s\n' "$C_WARN" "$*" "$C_OFF" >&2; }
die()  { printf '%s!! %s%s\n' "$C_ERR" "$*" "$C_OFF" >&2; exit 1; }

# Ephemeral SSH key, created per run under a private temp dir, removed on exit.
KEYDIR=""; KEY=""
_mktmpkey() {
  [ -n "$KEYDIR" ] && rm -rf "$KEYDIR"
  KEYDIR="$(mktemp -d)"; KEY="$KEYDIR/id_ephemeral"
  # shellcheck disable=SC2064
  trap "rm -rf '$KEYDIR'" EXIT
  ssh-keygen -t ed25519 -f "$KEY" -N "" -q -C "vector-demo-$(id -un)"
}

# EIP/HOST are deployment addresses, not source; they carry no committed default.
require_target() {
  [ -n "$EIP" ]  || die "EIP is not set. Export the demo server's elastic IP, e.g. 'export EIP=<elastic-ip>'."
  [ -n "$HOST" ] || die "HOST is not set. Export the demo server's FQDN, e.g. 'export HOST=<fqdn>'."
}

require_aws_auth() {
  if ! aws sts get-caller-identity --region "$REGION" >/dev/null 2>&1; then
    die "AWS session is not active. Run 'aws login' (interactive SSO) first, then re-run this script."
  fi
}

my_public_ip() {
  curl -fsS --max-time 10 https://checkip.amazonaws.com 2>/dev/null | tr -d '[:space:]' \
    || die "could not determine your public IP (needed for the SSH ingress rule)"
}

# Idempotently allow SSH (22) from the current public IP in the SG.
allow_ssh_ingress() {
  local ip; ip="$(my_public_ip)"
  if aws ec2 describe-security-groups --region "$REGION" --group-ids "$SG_ID" \
        --query "SecurityGroups[0].IpPermissions[?FromPort==\`22\`].IpRanges[].CidrIp" \
        --output text 2>/dev/null | tr '\t' '\n' | grep -qx "$ip/32"; then
    log "SSH ingress for $ip/32 already present"
  else
    log "authorizing SSH (22) from $ip/32 in $SG_ID"
    aws ec2 authorize-security-group-ingress --region "$REGION" --group-id "$SG_ID" \
      --ip-permissions "IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=$ip/32,Description=vector-demo-helper}]" \
      >/dev/null 2>&1 || warn "ingress authorize failed (may already exist under another rule)"
  fi
}

# Push the ephemeral public key via EC2 Instance Connect (valid ~60s).
push_key() {
  log "pushing ephemeral SSH key via EC2 Instance Connect -> $INSTANCE_ID"
  local ok
  ok="$(aws ec2-instance-connect send-ssh-public-key --region "$REGION" \
        --instance-id "$INSTANCE_ID" --instance-os-user "$OS_USER" \
        --ssh-public-key "file://$KEY.pub" --query Success --output text 2>&1)" \
    || die "send-ssh-public-key failed: $ok"
  [ "$ok" = "True" ] || die "send-ssh-public-key did not report success: $ok"
}

SSH_OPTS=(-i "" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20 \
          -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR)
remote() { SSH_OPTS[1]="$KEY"; ssh "${SSH_OPTS[@]}" "$OS_USER@$EIP" "$@"; }

# Establish a fresh, usable SSH channel (auth + ingress + key + key material).
connect_setup() {
  require_aws_auth
  _mktmpkey
  allow_ssh_ingress
  push_key
}

cmd_status() {
  require_aws_auth
  log "AWS identity"; aws sts get-caller-identity --region "$REGION" --output json | sed 's/^/   /'
  log "instance state"
  aws ec2 describe-instances --region "$REGION" --instance-ids "$INSTANCE_ID" \
    --query "Reservations[0].Instances[0].[State.Name,PublicIpAddress,InstanceType]" --output text | sed 's/^/   /'
  log "HTTPS health"
  printf '   https://%s/ -> HTTP %s\n' "$HOST" \
    "$(curl -sk -o /dev/null -w '%{http_code}' --max-time 20 "https://$HOST/" 2>/dev/null || echo 000)"
  _mktmpkey; allow_ssh_ingress; push_key
  log "deployed commit on the VM"
  remote 'echo "   OE      : $(git -C ~/OpenELIS-Global-2 branch --show-current 2>/dev/null)@$(git -C ~/OpenELIS-Global-2 rev-parse --short HEAD 2>/dev/null)"; echo "   distro  : $(git -C ~/openelis-indonesia-distro branch --show-current 2>/dev/null)@$(git -C ~/openelis-indonesia-distro rev-parse --short HEAD 2>/dev/null)"; echo "   containers:"; docker ps --format "     {{.Names}}: {{.Status}}" 2>/dev/null | head' \
    || warn "remote status read failed"
}

cmd_connect() {
  connect_setup
  if [ "$#" -gt 0 ]; then remote "$@"; else
    log "opening interactive shell on $HOST (Instance Connect key valid ~60s to establish)"
    SSH_OPTS[1]="$KEY"; ssh -t "${SSH_OPTS[@]}" "$OS_USER@$EIP"
  fi
}

# Seed transactional data (sites, backdated collections, identifications,
# results) via the REST API — client-side, NO SSH, NO SQL. Runs the guarded
# vector-surveillance-seed Playwright spec on THIS machine against the remote
# BASE_URL. The catalog it references is created on the VM by config-import.
cmd_seed() {
  local fe; fe="$(cd "$(dirname "$0")/../frontend" && pwd)"
  [ -d "$fe/node_modules" ] || die "frontend deps missing — run 'npm ci' in $fe first"
  : "${TEST_USER:=admin}"
  : "${TEST_PASS:=adminADMIN!}"   # standard OE demo admin password (override via env)
  : "${VECTOR_SEED_WEEKS:=5}"
  log "seeding https://$HOST via the REST API (client-side Playwright, NO SQL) — ${VECTOR_SEED_WEEKS} weeks"
  ( cd "$fe" && BASE_URL="https://$HOST" TEST_USER="$TEST_USER" TEST_PASS="$TEST_PASS" \
      VECTOR_SEED=1 VECTOR_SEED_WEEKS="$VECTOR_SEED_WEEKS" \
      npm run pw:test:core-demo -- vector-surveillance-seed --workers=1 ) \
    || die "seed run failed — inspect the Playwright output above"
  log "seeded — check https://$HOST/VectorSurveillanceReport"
}

# Write the detached deploy runner to the VM. Local vars ($OE_BRANCH etc.)
# interpolate here; \$(...) runs on the VM. On success it prints DONE_MARK.
_write_runner() {
  remote "cat > '$REMOTE_RUNNER'" <<RUNNER
#!/usr/bin/env bash
set -euo pipefail
echo "[deploy] start \$(date -u)"
# checkout -B <branch> FETCH_HEAD is refspec-agnostic: these are single-branch
# clones, so a plain 'git checkout <other-branch>' can't resolve origin/<branch>.
cd ~/OpenELIS-Global-2 && git fetch --depth 1 origin '$OE_BRANCH' && git checkout -f -B '$OE_BRANCH' FETCH_HEAD && git submodule update --init --depth 1 dataexport tools/openelis-analyzer-bridge tools/analyzer-mock-server
# Container-written log files under configs/ are root-owned; chown so the (ubuntu)
# git checkout can update tracked files when switching branches instead of aborting.
sudo chown -R "$OS_USER":"$OS_USER" ~/openelis-indonesia-distro/configs 2>/dev/null || true
cd ~/openelis-indonesia-distro && git fetch --depth 1 origin '$DISTRO_BRANCH' && git checkout -f -B '$DISTRO_BRANCH' FETCH_HEAD
echo "[deploy] OE -> \$(git -C ~/OpenELIS-Global-2 rev-parse --short HEAD); distro -> \$(git -C ~/openelis-indonesia-distro rev-parse --short HEAD)"
# Pin the config instance id to a real city so config-import loads the per-city
# catalog that production runs (not core classpath). Must run after 'git reset
# --hard' restores the distro .env's default value.
sed -i "s/^\(ORG_OPENELISGLOBAL_CONFIGURATION_INSTANCE_ID\)=.*/\1=$VECTOR_INSTANCE/" ~/openelis-indonesia-distro/.env 2>/dev/null || true
export ORG_OPENELISGLOBAL_CONFIGURATION_INSTANCE_ID=$VECTOR_INSTANCE
# Delete config-import checksum files so a from-scratch rebuild re-imports the
# (new/changed) vector catalog CSVs instead of skipping them.
sudo find ~/openelis-indonesia-distro/configs/configuration -name '*checksum*.properties' -delete 2>/dev/null || true
sudo find ~/OpenELIS-Global-2/volume/configuration -name '*checksum*.properties' -delete 2>/dev/null || true
cd ~/openelis-madagascar-test-harness
DISTRO_REPO=~/openelis-indonesia-distro OE_REPO=~/OpenELIS-Global-2 BRIDGE_REPO=~/OpenELIS-Global-2/tools/openelis-analyzer-bridge sudo -E ./scripts/restart-stack.sh --clean --rebuild
# Known post-rebuild gotchas on this VM: autoheal churns the docker network in a
# restart loop, and the proxy is sometimes left in Created. Best-effort fixes.
sudo docker stop autoheal-oe 2>/dev/null || true
sudo docker start openelisglobal-proxy 2>/dev/null || true
# Gate DONE on the webapp actually being healthy (its container has a healthcheck)
# so the client-side API seed that runs next doesn't hit a half-up stack.
echo "[deploy] waiting for webapp health (up to 15 min)"
for i in \$(seq 1 90); do
  st=\$(sudo docker inspect -f '{{.State.Health.Status}}' openelisglobal-webapp 2>/dev/null || echo none)
  [ "\$st" = healthy ] && { echo "[deploy] webapp healthy after ~\$((i*10))s"; break; }
  sleep 10
done
# Re-inject the host Let's Encrypt cert into the proxy's cert/key volumes, which
# --clean wipes each deploy. This is a local copy from the certbot-managed host
# cert (/etc/letsencrypt) — it never contacts Let's Encrypt, so no rate-limit risk.
# nginx.conf reads apache-selfsigned.crt/.key from these volumes; certbot.timer
# keeps the host cert renewed.
LE_DIR=/etc/letsencrypt/live/$HOST
if sudo test -f "\$LE_DIR/fullchain.pem"; then
  CERTS_VOL=\$(sudo docker inspect openelisglobal-proxy --format '{{range .Mounts}}{{if eq .Destination "/etc/nginx/certs"}}{{.Name}}{{end}}{{end}}')
  KEYS_VOL=\$(sudo docker inspect openelisglobal-proxy --format '{{range .Mounts}}{{if eq .Destination "/etc/nginx/keys"}}{{.Name}}{{end}}{{end}}')
  sudo docker run --rm -v /etc/letsencrypt:/le:ro -v "\$CERTS_VOL":/certs -v "\$KEYS_VOL":/keys alpine sh -c \
    "cp -L /le/live/$HOST/fullchain.pem /certs/apache-selfsigned.crt && cp -L /le/live/$HOST/privkey.pem /keys/apache-selfsigned.key"
  sudo docker exec openelisglobal-proxy nginx -s reload 2>/dev/null || sudo docker restart openelisglobal-proxy 2>/dev/null || true
  echo "[deploy] re-injected host LE cert ($HOST) into \$CERTS_VOL / \$KEYS_VOL + reloaded nginx"
else
  echo "[deploy] WARN: no host LE cert at \$LE_DIR — HTTPS stays self-signed until certbot issues one on the host"
fi
echo "$DONE_MARK \$(date -u)"
RUNNER
  remote "chmod +x '$REMOTE_RUNNER'"
}

# Poll the remote log until the runner finishes (re-establishing SSH each round,
# since Instance Connect keys expire in ~60s). 0 = success, 1 = failed/timeout.
_poll_deploy() {
  local deadline=$(( $(date +%s) + DEPLOY_TIMEOUT )) out
  while [ "$(date +%s)" -lt "$deadline" ]; do
    sleep 45
    _mktmpkey; allow_ssh_ingress >/dev/null 2>&1 || true; push_key >/dev/null 2>&1 || { warn "instance-connect push failed; retrying"; continue; }
    out="$(remote "tail -4 '$REMOTE_LOG' 2>/dev/null; echo ---; grep -q '$DONE_MARK' '$REMOTE_LOG' 2>/dev/null && echo VDEMO_OK; pgrep -f vector-demo-deploy.run.sh >/dev/null && echo VDEMO_RUNNING || echo VDEMO_STOPPED" 2>/dev/null || echo VDEMO_SSHFAIL)"
    printf '%s\n' "$out" | grep -v '^VDEMO_' | sed 's/^/   /'
    if printf '%s' "$out" | grep -q VDEMO_OK; then
      local code; code="$(curl -sk -o /dev/null -w '%{http_code}' --max-time 20 "https://$HOST/" 2>/dev/null || echo 000)"
      log "runner finished; https://$HOST/ -> HTTP $code"; return 0
    fi
    if printf '%s' "$out" | grep -q VDEMO_STOPPED; then
      warn "runner stopped WITHOUT success marker — deploy failed. Inspect: ./scripts/deploy-vector-demo.sh logs"; return 1
    fi
    log "still building… (polling every 45s, up to ${DEPLOY_TIMEOUT}s)"
  done
  warn "poll deadline reached; runner may still be going. Check: ./scripts/deploy-vector-demo.sh logs"; return 1
}

cmd_deploy() {
  [ "${1:-}" = "--yes" ] || die "deploy is DESTRUCTIVE (wipes the VM DB via --clean). Re-run with: deploy --yes"
  connect_setup
  log "writing detached deploy runner to the VM"
  _write_runner
  log "launching reset+rebuild detached (nohup) — target OE branch $OE_BRANCH"
  remote "cd ~ && nohup bash '$REMOTE_RUNNER' > '$REMOTE_LOG' 2>&1 & echo \"   launched pid \$!\""
  log "polling until the rebuild is complete (safe to Ctrl-C; the VM keeps building — resume with 'logs')"
  _poll_deploy || die "rebuild did not complete cleanly; NOT seeding. Inspect: ./scripts/deploy-vector-demo.sh logs"
  log "rebuild complete + webapp healthy; seeding transactional data via the REST API (client-side)"
  cmd_seed
  log "DEPLOY COMPLETE -> https://$HOST/VectorSurveillanceReport"
}

cmd_logs() {
  connect_setup
  log "remote deploy log ($REMOTE_LOG):"
  remote "tail -40 '$REMOTE_LOG' 2>/dev/null || echo '(no deploy log yet)'; echo ---; grep -q '$DONE_MARK' '$REMOTE_LOG' 2>/dev/null && echo 'STATUS: DONE_OK' || (pgrep -f vector-demo-deploy.run.sh >/dev/null && echo 'STATUS: RUNNING' || echo 'STATUS: STOPPED (no success marker)')"
}

cmd_provision() {
  require_aws_auth
  local state
  state="$(aws ec2 describe-instances --region "$REGION" --instance-ids "$INSTANCE_ID" \
            --query 'Reservations[0].Instances[0].State.Name' --output text 2>/dev/null || echo absent)"
  if [ "$state" = running ] || [ "$state" = stopped ] || [ "$state" = pending ]; then
    die "instance $INSTANCE_ID already exists (state=$state). Use 'deploy', not 'provision'."
  fi
  warn "recreating the demo instance from scratch (break-glass). Recovered recipe:"
  cat <<REF
  # 1) key pair + security group (22=your IP, 80/443=public)
  aws ec2 create-key-pair --region $REGION --key-name vector-demo-key --query KeyMaterial --output text > ~/.ssh/vector-demo-key.pem && chmod 400 ~/.ssh/vector-demo-key.pem
  SG=\$(aws ec2 create-security-group --region $REGION --group-name vector-demo-sg --description "vector demo" --query GroupId --output text)
  MYIP=\$(curl -fsS https://checkip.amazonaws.com | tr -d '[:space:]')
  aws ec2 authorize-security-group-ingress --region $REGION --group-id \$SG \\
    --ip-permissions \\
      "IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=\${MYIP}/32,Description=ssh-me}]" \\
      "IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges=[{CidrIp=0.0.0.0/0,Description=http-le}]" \\
      "IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges=[{CidrIp=0.0.0.0/0,Description=https}]"
  # 2) elastic IP + instance (Ubuntu 22.04, t3.large, 30GB gp3)
  EIP_ALLOC=\$(aws ec2 allocate-address --region $REGION --domain vpc --query AllocationId --output text)
  IID=\$(aws ec2 run-instances --region $REGION --image-id $AMI --instance-type t3.large \\
    --key-name vector-demo-key --security-group-ids \$SG \\
    --block-device-mappings 'DeviceName=/dev/sda1,Ebs={VolumeSize=30,VolumeType=gp3}' \\
    --tag-specifications 'ResourceType=instance,Tags=[{Key=Project,Value=vector-demo},{Key=Name,Value=vector-demo}]' \\
    --query 'Instances[0].InstanceId' --output text)
  aws ec2 wait instance-running --region $REGION --instance-ids \$IID
  aws ec2 associate-address --region $REGION --instance-id \$IID --allocation-id \$EIP_ALLOC
  # 3) point DNS ${HOST:-<fqdn>} at the new EIP, install Docker + compose, certbot for TLS, then:
  #    OE_BRANCH=$OE_BRANCH DISTRO_BRANCH=$DISTRO_BRANCH ./scripts/deploy-vector-demo.sh deploy --yes
REF
}

main() {
  local sub="${1:-help}"; shift || true
  case "$sub" in
    status)    require_target; cmd_status ;;
    connect)   require_target; cmd_connect "$@" ;;
    deploy)    require_target; cmd_deploy "$@" ;;
    logs)      require_target; cmd_logs ;;
    seed)      require_target; cmd_seed ;;
    provision) cmd_provision ;;
    help|-h|--help)
      sed -n '2,57p' "$0" | sed 's/^# \{0,1\}//' ;;
    *) die "unknown subcommand '$sub' (try: status | connect | deploy --yes | logs | seed | provision | help)" ;;
  esac
}
main "$@"
