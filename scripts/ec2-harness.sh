#!/usr/bin/env bash
# ec2-harness.sh — Local CLI for managing EC2 analyzer harness deployments.
#
# Requires: aws CLI, gh CLI, ssh, rsync
#
# Usage:
#   ec2-harness.sh provision  [--name NAME] [--instance-type TYPE] [--region REGION]
#   ec2-harness.sh configure-github --target NAME [--environment ENV_NAME]
#   ec2-harness.sh deploy     --target NAME [--pr NUMBER | --branch BRANCH] [--full-reset]
#   ec2-harness.sh status     --target NAME
#   ec2-harness.sh teardown   --target NAME [--force]
#   ec2-harness.sh list
#
# Config stored in: ~/.config/openelis-harness/targets/<name>/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG_DIR="${HOME}/.config/openelis-harness/targets"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# --- Helpers ---

die() { echo -e "${RED}ERROR: $*${NC}" >&2; exit 1; }
info() { echo -e "${BLUE}$*${NC}"; }
ok() { echo -e "${GREEN}✓ $*${NC}"; }
warn() { echo -e "${YELLOW}⚠ $*${NC}"; }

require_cmd() {
  command -v "$1" &>/dev/null || die "$1 is required but not installed"
}

target_dir() {
  echo "${CONFIG_DIR}/$1"
}

load_target() {
  local name="$1"
  local dir
  dir="$(target_dir "$name")"
  [ -f "$dir/instance.json" ] || die "Target '$name' not found. Run: $0 list"
  INSTANCE_ID="$(jq -r '.instance_id' "$dir/instance.json")"
  REGION="$(jq -r '.region' "$dir/instance.json")"
  PUBLIC_IP="$(jq -r '.public_ip' "$dir/instance.json")"
  SG_ID="$(jq -r '.security_group_id' "$dir/instance.json")"
  SSH_KEY="$dir/ssh_key"
  DEPLOY_USER="deploy"
}

ssh_cmd() {
  ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
    -i "$SSH_KEY" "${DEPLOY_USER}@${PUBLIC_IP}" "$@"
}

ssh_root() {
  ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
    -i "$SSH_KEY" "ubuntu@${PUBLIC_IP}" "$@"
}

# --- Subcommands ---

cmd_provision() {
  local name="harness-$(date +%Y%m%d)"
  local instance_type="t3.xlarge"
  local region="${AWS_DEFAULT_REGION:-us-west-2}"
  local ami=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --name)           name="$2"; shift 2 ;;
      --instance-type)  instance_type="$2"; shift 2 ;;
      --region)         region="$2"; shift 2 ;;
      --ami)            ami="$2"; shift 2 ;;
      *)                die "Unknown option: $1" ;;
    esac
  done

  require_cmd aws
  require_cmd jq

  local dir
  dir="$(target_dir "$name")"
  [ -d "$dir" ] && die "Target '$name' already exists at $dir"
  mkdir -p "$dir"

  info "Provisioning EC2 instance: $name"
  echo "  Region:        $region"
  echo "  Instance type: $instance_type"

  # Find Ubuntu 22.04 LTS AMI if not specified
  if [ -z "$ami" ]; then
    info "Finding latest Ubuntu 22.04 AMI..."
    ami=$(aws ec2 describe-images \
      --region "$region" \
      --owners 099720109477 \
      --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
                "Name=state,Values=available" \
      --query 'sort_by(Images, &CreationDate)[-1].ImageId' \
      --output text)
    echo "  AMI: $ami"
  fi

  # Generate SSH key pair
  info "Generating SSH key pair..."
  ssh-keygen -t ed25519 -C "harness-deploy-$name" -f "$dir/ssh_key" -N "" -q
  ok "SSH key: $dir/ssh_key"

  # Create security group
  info "Creating security group..."
  local sg_name="harness-${name}-sg"
  SG_ID=$(aws ec2 create-security-group \
    --region "$region" \
    --group-name "$sg_name" \
    --description "Analyzer harness deploy - $name" \
    --query 'GroupId' --output text)
  ok "Security group: $SG_ID ($sg_name)"

  # Allow SSH from current IP
  local my_ip
  my_ip=$(curl -s https://checkip.amazonaws.com)
  aws ec2 authorize-security-group-ingress --region "$region" \
    --group-id "$SG_ID" --protocol tcp --port 22 --cidr "${my_ip}/32" >/dev/null
  ok "SSH allowed from $my_ip"

  # Allow HTTP/HTTPS from anywhere
  aws ec2 authorize-security-group-ingress --region "$region" \
    --group-id "$SG_ID" --protocol tcp --port 80 --cidr "0.0.0.0/0" >/dev/null
  aws ec2 authorize-security-group-ingress --region "$region" \
    --group-id "$SG_ID" --protocol tcp --port 443 --cidr "0.0.0.0/0" >/dev/null
  ok "HTTP/HTTPS open"

  # Allow harness ports (bridge, simulator)
  aws ec2 authorize-security-group-ingress --region "$region" \
    --group-id "$SG_ID" --protocol tcp --port 8085 --cidr "0.0.0.0/0" >/dev/null
  aws ec2 authorize-security-group-ingress --region "$region" \
    --group-id "$SG_ID" --protocol tcp --port 8442 --cidr "0.0.0.0/0" >/dev/null
  ok "Harness ports (8085, 8442) open"

  # Import key pair to AWS
  local key_name="harness-${name}-key"
  aws ec2 import-key-pair --region "$region" \
    --key-name "$key_name" \
    --public-key-material "fileb://$dir/ssh_key.pub" >/dev/null
  ok "Key pair imported: $key_name"

  # Launch instance
  info "Launching EC2 instance..."
  INSTANCE_ID=$(aws ec2 run-instances \
    --region "$region" \
    --image-id "$ami" \
    --instance-type "$instance_type" \
    --key-name "$key_name" \
    --security-group-ids "$SG_ID" \
    --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":60,"VolumeType":"gp3"}}]' \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=analyzer-harness-${name}}]" \
    --query 'Instances[0].InstanceId' --output text)
  ok "Instance launched: $INSTANCE_ID"

  # Wait for running
  info "Waiting for instance to be running..."
  aws ec2 wait instance-running --region "$region" --instance-ids "$INSTANCE_ID"
  ok "Instance running"

  # Get public IP
  PUBLIC_IP=$(aws ec2 describe-instances \
    --region "$region" \
    --instance-ids "$INSTANCE_ID" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)
  ok "Public IP: $PUBLIC_IP"

  # Save config
  cat > "$dir/instance.json" << EOF
{
  "name": "$name",
  "instance_id": "$INSTANCE_ID",
  "region": "$region",
  "public_ip": "$PUBLIC_IP",
  "security_group_id": "$SG_ID",
  "key_name": "$key_name",
  "instance_type": "$instance_type",
  "created": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF

  # Wait for SSH
  info "Waiting for SSH access..."
  local retries=0
  while [ $retries -lt 30 ]; do
    if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 \
         -i "$dir/ssh_key" "ubuntu@${PUBLIC_IP}" "echo ok" 2>/dev/null; then
      break
    fi
    retries=$((retries + 1))
    sleep 5
  done
  [ $retries -ge 30 ] && die "SSH timeout after 150s"
  ok "SSH accessible"

  # Run EC2 setup script
  info "Running EC2 setup (Docker, deploy user)..."
  local pubkey
  pubkey=$(cat "$dir/ssh_key.pub")
  scp -o StrictHostKeyChecking=no -i "$dir/ssh_key" \
    "$REPO_ROOT/projects/analyzer-harness/ec2-setup.sh" \
    "ubuntu@${PUBLIC_IP}:/tmp/ec2-setup.sh"
  ssh -o StrictHostKeyChecking=no -i "$dir/ssh_key" "ubuntu@${PUBLIC_IP}" \
    "sudo bash /tmp/ec2-setup.sh '$pubkey'"

  # Allow SSH from GitHub Actions IP ranges (broad — they change)
  info "Adding GitHub Actions SSH access to security group..."
  # GitHub Actions uses many IP ranges. For simplicity, we allow the deploy
  # user's key-only auth to be sufficient security. Add your org's VPN CIDR
  # here for tighter control.
  # Uncomment to allow from anywhere (key-only auth):
  aws ec2 authorize-security-group-ingress --region "$region" \
    --group-id "$SG_ID" --protocol tcp --port 22 --cidr "0.0.0.0/0" >/dev/null 2>&1 || true
  warn "SSH open to all IPs (secured by key-only auth). Restrict to your VPN for production."

  # Verify deploy user SSH
  info "Verifying deploy user SSH..."
  if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
       -i "$dir/ssh_key" "deploy@${PUBLIC_IP}" "docker compose version" 2>/dev/null; then
    ok "Deploy user verified"
  else
    die "Cannot SSH as deploy user"
  fi

  echo ""
  echo -e "${GREEN}========================================${NC}"
  echo -e "${GREEN}  Provisioning Complete${NC}"
  echo -e "${GREEN}========================================${NC}"
  echo ""
  echo -e "  Name:        $name"
  echo -e "  Instance:    $INSTANCE_ID"
  echo -e "  IP:          $PUBLIC_IP"
  echo -e "  SSH:         ssh -i $dir/ssh_key deploy@$PUBLIC_IP"
  echo -e "  Config:      $dir/"
  echo ""
  echo -e "  Next: $0 configure-github --target $name"
  echo ""
}

cmd_configure_github() {
  local target=""
  local environment="analyzer-harness-ec2"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --target)       target="$2"; shift 2 ;;
      --environment)  environment="$2"; shift 2 ;;
      *)              die "Unknown option: $1" ;;
    esac
  done

  [ -z "$target" ] && die "Usage: $0 configure-github --target NAME"

  require_cmd gh
  load_target "$target"

  info "Configuring GitHub environment: $environment"

  # Create environment (gh doesn't have a direct create command — setting a
  # secret on it auto-creates the environment)
  local private_key
  private_key=$(cat "$SSH_KEY")

  gh secret set EC2_SSH_PRIVATE_KEY \
    --env "$environment" \
    --body "$private_key"
  ok "Set EC2_SSH_PRIVATE_KEY"

  gh secret set EC2_HOST \
    --env "$environment" \
    --body "$PUBLIC_IP"
  ok "Set EC2_HOST ($PUBLIC_IP)"

  gh secret set EC2_USER \
    --env "$environment" \
    --body "$DEPLOY_USER"
  ok "Set EC2_USER ($DEPLOY_USER)"

  echo ""
  echo -e "${GREEN}GitHub environment '$environment' configured.${NC}"
  echo -e "  Add the ${YELLOW}deploy-test${NC} label to a PR to trigger deployment."
  echo ""
}

cmd_deploy() {
  local target=""
  local pr=""
  local branch=""
  local full_reset=false

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --target)      target="$2"; shift 2 ;;
      --pr)          pr="$2"; shift 2 ;;
      --branch)      branch="$2"; shift 2 ;;
      --full-reset)  full_reset=true; shift ;;
      *)             die "Unknown option: $1" ;;
    esac
  done

  [ -z "$target" ] && die "Usage: $0 deploy --target NAME [--pr NUMBER | --branch BRANCH]"

  load_target "$target"

  # Determine image tags
  local tag=""
  if [ -n "$pr" ]; then
    info "Resolving PR #$pr head SHA..."
    local sha
    sha=$(gh pr view "$pr" --json headRefOid --jq '.headRefOid')
    tag="sha-${sha}"
    info "PR #$pr → $tag"
  elif [ -n "$branch" ]; then
    tag="$branch"
  else
    tag="develop"
  fi

  local owner_lower
  owner_lower=$(gh repo view --json owner --jq '.owner.login' | tr '[:upper:]' '[:lower:]')

  # Generate .env
  local env_file="/tmp/harness-deploy-$$.env"
  if [[ "$tag" == sha-* ]]; then
    cat > "$env_file" << EOF
OE_IMAGE=ghcr.io/${owner_lower}/openelis-global-2/e2e-cache/openelisglobal-webapp.build:${tag}
DB_IMAGE=ghcr.io/${owner_lower}/openelis-global-2/e2e-cache/openelisglobal-databse.build:${tag}
FHIR_IMAGE=ghcr.io/${owner_lower}/openelis-global-2/e2e-cache/external-fhir-api.build:${tag}
FRONTEND_IMAGE=ghcr.io/${owner_lower}/openelis-global-2/e2e-cache/frontend:${tag}
PROXY_IMAGE=ghcr.io/${owner_lower}/openelis-global-2/e2e-cache/openelisglobal-proxy.build:${tag}
CERTS_IMAGE=ghcr.io/${owner_lower}/openelis-global-2/e2e-cache/certgen:${tag}
BRIDGE_IMAGE=itechuw/openelis-analyzer-bridge:develop
SIMULATOR_IMAGE=itechuw/astm-mock-server:main
TEST_USER=admin
TEST_PASS=adminADMIN!
EOF
  else
    cat > "$env_file" << EOF
OE_IMAGE=itechuw/openelis-global-2-dev:${tag}
FRONTEND_IMAGE=itechuw/openelis-global-2-frontend-dev:${tag}
BRIDGE_IMAGE=itechuw/openelis-analyzer-bridge:develop
SIMULATOR_IMAGE=itechuw/astm-mock-server:main
TEST_USER=admin
TEST_PASS=adminADMIN!
EOF
  fi

  local deploy_dir="/opt/analyzer-harness"

  info "Deploying to $PUBLIC_IP (tag: $tag)..."

  # Rsync deploy bundle
  info "Syncing deploy bundle..."
  ssh_cmd "mkdir -p ${deploy_dir}"

  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" --delete \
    "$REPO_ROOT/projects/analyzer-harness/docker-compose.dev.yml" \
    "$REPO_ROOT/projects/analyzer-harness/docker-compose.analyzer-test.yml" \
    "$REPO_ROOT/projects/analyzer-harness/docker-compose.deploy.yml" \
    "$REPO_ROOT/projects/analyzer-harness/docker-compose.letsencrypt.yml" \
    "$REPO_ROOT/projects/analyzer-harness/deploy-ec2.sh" \
    "$REPO_ROOT/projects/analyzer-harness/seed-analyzers.sh" \
    "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/"

  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" \
    "$env_file" "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/.env"

  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" --delete \
    "$REPO_ROOT/projects/analyzer-harness/volume/" \
    "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/volume/"

  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" --delete \
    "$REPO_ROOT/projects/analyzer-profiles/" \
    "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/projects/analyzer-profiles/"

  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" --delete \
    "$REPO_ROOT/projects/analyzer-harness/e2e-fixtures/" \
    "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/e2e-fixtures/"

  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" --delete \
    "$REPO_ROOT/volume/openelis-analyzer-bridge/" \
    "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/bridge-config/"

  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" \
    "$REPO_ROOT/nginx-proxy/docker-entrypoint.sh" \
    "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/nginx-proxy/docker-entrypoint.sh"

  ssh_cmd "mkdir -p ${deploy_dir}/certbot ${deploy_dir}/letsencrypt"
  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" \
    "$REPO_ROOT/volume/nginx/certbot/" \
    "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/certbot/" 2>/dev/null || true
  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" \
    "$REPO_ROOT/volume/letsencrypt/" \
    "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/letsencrypt/" 2>/dev/null || true

  rsync -az -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" --delete \
    "$REPO_ROOT/src/test/resources/" \
    "${DEPLOY_USER}@${PUBLIC_IP}:${deploy_dir}/test-fixtures/"

  ok "Deploy bundle synced"

  # GHCR login for PR images
  if [[ "$tag" == sha-* ]]; then
    info "Logging into GHCR on EC2..."
    local ghcr_token
    ghcr_token=$(gh auth token)
    ssh_cmd "echo '$ghcr_token' | docker login ghcr.io -u '$(gh api user --jq .login)' --password-stdin" 2>/dev/null
    ok "GHCR authenticated on EC2"
  fi

  # Run deploy
  local flags=""
  [ "$full_reset" = true ] && flags="--full-reset"
  info "Running deploy-ec2.sh..."
  ssh_cmd "cd ${deploy_dir} && bash deploy-ec2.sh ${flags}"

  rm -f "$env_file"

  echo ""
  ok "Deploy complete: https://${PUBLIC_IP}/"
}

cmd_status() {
  local target=""
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --target) target="$2"; shift 2 ;;
      *)        die "Unknown option: $1" ;;
    esac
  done
  [ -z "$target" ] && die "Usage: $0 status --target NAME"

  load_target "$target"

  info "Target: $target ($PUBLIC_IP)"
  echo ""

  # Instance state
  local state
  state=$(aws ec2 describe-instances \
    --region "$REGION" \
    --instance-ids "$INSTANCE_ID" \
    --query 'Reservations[0].Instances[0].State.Name' --output text 2>/dev/null || echo "unknown")
  echo "  EC2 state: $state"

  if [ "$state" != "running" ]; then
    warn "Instance is not running"
    return
  fi

  # Docker containers
  echo ""
  echo "  Containers:"
  ssh_cmd "cd /opt/analyzer-harness && docker compose \
    -f docker-compose.dev.yml \
    -f docker-compose.analyzer-test.yml \
    -f docker-compose.deploy.yml \
    ps --format 'table {{.Name}}\t{{.Status}}\t{{.Ports}}'" 2>/dev/null || warn "Cannot list containers"

  # Health checks
  echo ""
  echo "  Health:"
  if curl -sk "https://${PUBLIC_IP}/" 2>/dev/null | grep -q "OpenELIS\|Login"; then
    ok "OE webapp: https://${PUBLIC_IP}/"
  else
    echo -e "  ${RED}✗ OE webapp not responding${NC}"
  fi

  if curl -s "http://${PUBLIC_IP}:8085/health" 2>/dev/null | grep -q "ok\|healthy"; then
    ok "Simulator: http://${PUBLIC_IP}:8085/health"
  else
    echo -e "  ${RED}✗ Simulator not responding${NC}"
  fi

  if curl -sk "https://${PUBLIC_IP}:8442/actuator/health" 2>/dev/null | grep -q "UP"; then
    ok "Bridge: https://${PUBLIC_IP}:8442/actuator/health"
  else
    echo -e "  ${RED}✗ Bridge not responding${NC}"
  fi
}

cmd_teardown() {
  local target=""
  local force=false

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --target) target="$2"; shift 2 ;;
      --force)  force=true; shift ;;
      *)        die "Unknown option: $1" ;;
    esac
  done
  [ -z "$target" ] && die "Usage: $0 teardown --target NAME [--force]"

  load_target "$target"
  local dir
  dir="$(target_dir "$target")"

  if [ "$force" != true ]; then
    echo -e "${YELLOW}This will TERMINATE instance $INSTANCE_ID ($PUBLIC_IP) and remove all data.${NC}"
    read -r -p "Continue? [y/N] " confirm
    [ "$confirm" = "y" ] || [ "$confirm" = "Y" ] || die "Aborted"
  fi

  info "Tearing down target: $target"

  # Terminate instance
  info "Terminating EC2 instance $INSTANCE_ID..."
  aws ec2 terminate-instances --region "$REGION" --instance-ids "$INSTANCE_ID" >/dev/null 2>&1 || true
  ok "Instance termination initiated"

  # Delete security group (may fail if instance hasn't terminated yet)
  info "Deleting security group $SG_ID..."
  sleep 5
  aws ec2 delete-security-group --region "$REGION" --group-id "$SG_ID" 2>/dev/null || \
    warn "Could not delete SG yet (instance still terminating). Try: aws ec2 delete-security-group --region $REGION --group-id $SG_ID"

  # Delete key pair
  local key_name
  key_name=$(jq -r '.key_name' "$dir/instance.json")
  aws ec2 delete-key-pair --region "$REGION" --key-name "$key_name" 2>/dev/null || true
  ok "Key pair deleted"

  # Remove GitHub environment (optional — may not exist)
  info "Removing GitHub environment..."
  gh api --method DELETE "repos/{owner}/{repo}/environments/analyzer-harness-ec2" 2>/dev/null || \
    warn "GitHub environment not found or already removed"

  # Remove local config
  rm -rf "$dir"
  ok "Local config removed: $dir"

  echo ""
  ok "Teardown complete for target: $target"
}

cmd_list() {
  if [ ! -d "$CONFIG_DIR" ] || [ -z "$(ls -A "$CONFIG_DIR" 2>/dev/null)" ]; then
    echo "No targets configured."
    echo "  Create one: $0 provision --name my-test"
    return
  fi

  echo "Configured targets:"
  echo ""
  for dir in "$CONFIG_DIR"/*/; do
    [ -f "$dir/instance.json" ] || continue
    local name ip instance state
    name=$(jq -r '.name' "$dir/instance.json")
    ip=$(jq -r '.public_ip' "$dir/instance.json")
    instance=$(jq -r '.instance_id' "$dir/instance.json")
    region=$(jq -r '.region' "$dir/instance.json")
    state=$(aws ec2 describe-instances \
      --region "$region" \
      --instance-ids "$instance" \
      --query 'Reservations[0].Instances[0].State.Name' --output text 2>/dev/null || echo "unknown")
    printf "  %-20s  %-15s  %-20s  %s\n" "$name" "$ip" "$instance" "$state"
  done
  echo ""
}

# --- Main ---

show_help() {
  cat << 'EOF'
ec2-harness.sh — Local CLI for managing EC2 analyzer harness deployments.

Usage:
  ec2-harness.sh <command> [options]

Commands:
  provision          Provision a new EC2 instance
  configure-github   Configure GitHub environment + secrets for a target
  deploy             Deploy a PR or branch to a target
  status             Check health of a deployed target
  teardown           Terminate an EC2 instance and clean up
  list               List all configured targets

Run ec2-harness.sh <command> --help for command-specific options.
EOF
}

case "${1:-}" in
  provision)        shift; cmd_provision "$@" ;;
  configure-github) shift; cmd_configure_github "$@" ;;
  deploy)           shift; cmd_deploy "$@" ;;
  status)           shift; cmd_status "$@" ;;
  teardown)         shift; cmd_teardown "$@" ;;
  list)             shift; cmd_list "$@" ;;
  --help|-h|help)   show_help ;;
  "")               show_help; exit 1 ;;
  *)                die "Unknown command: $1. Run $0 --help" ;;
esac
