#!/usr/bin/env bash
#
# deploy-vector-demo.sh — lifecycle helper for the V-04 Vector Surveillance demo
# server at https://vector-demo.openelis-global.org/
#
# WHY THIS EXISTS
#   The demo EC2 was provisioned ad-hoc via AWS CLI and deployed by hand; the
#   procedure lived only in a chat transcript + a scratchpad SSH key that gets
#   wiped between sessions. This script is the durable, version-controlled source
#   of truth so the reconnect/deploy/seed steps never have to be reconstructed
#   again. Companion note: reference_vector_demo_ec2 in the Claude memory dir.
#
# THE SERVER (AWS, region us-west-2)
#   Instance   i-05e265d7fe2bc9630  (t3.large, Ubuntu 22.04, 30GB gp3, ami-0e1601cee784a69a2)
#   Elastic IP 35.163.107.94        -> vector-demo.openelis-global.org
#   Sec group  sg-049d715dea927aa77 (vector-demo-sg; ingress 22=your-IP, 80/443=public)
#   SSH user   ubuntu               (key access via EC2 Instance Connect — see below)
#
# WHAT RUNS ON THE VM (built on the box, not host-mounted)
#   ~/OpenELIS-Global-2            (branch 372-vector-surveillance-reporting)
#   ~/openelis-indonesia-distro    (branch feat/vector-result-significance; the demo distro + seed)
#   ~/openelis-madagascar-test-harness  (orchestrator: scripts/restart-stack.sh)
#   Deploy  = restart-stack.sh --clean --rebuild   (--clean wipes volumes = full reset;
#             --rebuild rebuilds the WAR + frontend from OE_REPO at the checked-out branch)
#   Seed    = openelis-indonesia-distro/scripts/seed-vector-demo.sh --clean
#             (psql via docker exec openelisglobal-database; Indonesian dataset, id>=970000)
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
#   ./scripts/deploy-vector-demo.sh deploy --yes    # DESTRUCTIVE: full reset -> rebuild latest -> reseed -> verify (detached + polled)
#   ./scripts/deploy-vector-demo.sh logs            # tail the detached deploy log + status (resume a poll)
#   ./scripts/deploy-vector-demo.sh seed            # reseed only (seed-vector-demo.sh --clean)
#   ./scripts/deploy-vector-demo.sh provision       # break-glass: recreate the instance if it is gone
#
# Every value below is overridable via env (e.g. OE_BRANCH=some-branch ./deploy-vector-demo.sh deploy --yes).
set -euo pipefail

REGION="${REGION:-us-west-2}"
INSTANCE_ID="${INSTANCE_ID:-i-05e265d7fe2bc9630}"
EIP="${EIP:-35.163.107.94}"
HOST="${HOST:-vector-demo.openelis-global.org}"
SG_ID="${SG_ID:-sg-049d715dea927aa77}"
OS_USER="${OS_USER:-ubuntu}"
OE_BRANCH="${OE_BRANCH:-372-vector-surveillance-reporting}"
DISTRO_BRANCH="${DISTRO_BRANCH:-feat/vector-result-significance}"
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

cmd_seed() {
  connect_setup
  log "reseeding (seed-vector-demo.sh --clean) — Indonesian vector demo dataset"
  remote 'sudo DB_CONTAINER=openelisglobal-database bash ~/openelis-indonesia-distro/scripts/seed-vector-demo.sh --clean 2>&1 | tail -20'
  log "done — check https://$HOST/VectorSurveillanceReport"
}

# Write the detached deploy runner to the VM. Local vars ($OE_BRANCH etc.)
# interpolate here; \$(...) runs on the VM. On success it prints DONE_MARK.
_write_runner() {
  remote "cat > '$REMOTE_RUNNER'" <<RUNNER
#!/usr/bin/env bash
set -euo pipefail
echo "[deploy] start \$(date -u)"
cd ~/OpenELIS-Global-2 && git fetch --depth 1 origin '$OE_BRANCH' && git checkout -f '$OE_BRANCH' && git reset --hard 'origin/$OE_BRANCH' && git submodule update --init --depth 1 dataexport tools/openelis-analyzer-bridge
cd ~/openelis-indonesia-distro && git fetch --depth 1 origin '$DISTRO_BRANCH' && git checkout -f '$DISTRO_BRANCH' && git reset --hard 'origin/$DISTRO_BRANCH'
echo "[deploy] OE -> \$(git -C ~/OpenELIS-Global-2 rev-parse --short HEAD); distro -> \$(git -C ~/openelis-indonesia-distro rev-parse --short HEAD)"
cd ~/openelis-madagascar-test-harness
DISTRO_REPO=~/openelis-indonesia-distro OE_REPO=~/OpenELIS-Global-2 BRIDGE_REPO=~/OpenELIS-Global-2/tools/openelis-analyzer-bridge sudo -E ./scripts/restart-stack.sh --clean --rebuild
echo "[deploy] stack rebuilt; seeding"
sudo DB_CONTAINER=openelisglobal-database bash ~/openelis-indonesia-distro/scripts/seed-vector-demo.sh --clean
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
  log "launching reset+rebuild+reseed detached (nohup) — target OE branch $OE_BRANCH"
  remote "cd ~ && nohup bash '$REMOTE_RUNNER' > '$REMOTE_LOG' 2>&1 & echo \"   launched pid \$!\""
  log "polling until complete (safe to Ctrl-C; the VM keeps building — resume with 'logs')"
  _poll_deploy && log "DEPLOY COMPLETE -> https://$HOST/VectorSurveillanceReport"
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
  # 3) point DNS $HOST at the new EIP, install Docker + compose, certbot for TLS, then:
  #    OE_BRANCH=$OE_BRANCH DISTRO_BRANCH=$DISTRO_BRANCH ./scripts/deploy-vector-demo.sh deploy --yes
REF
}

main() {
  local sub="${1:-help}"; shift || true
  case "$sub" in
    status)    cmd_status ;;
    connect)   cmd_connect "$@" ;;
    deploy)    cmd_deploy "$@" ;;
    logs)      cmd_logs ;;
    seed)      cmd_seed ;;
    provision) cmd_provision ;;
    help|-h|--help)
      sed -n '2,60p' "$0" | sed 's/^# \{0,1\}//' ;;
    *) die "unknown subcommand '$sub' (try: status | connect | deploy --yes | logs | seed | provision | help)" ;;
  esac
}
main "$@"
