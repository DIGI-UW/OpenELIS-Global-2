#!/usr/bin/env bash
# Install scheduling/tooling only. Data is reset when the timer fires or on an explicit manual invocation.
set -euo pipefail
site="${1:?testing site path required}"
deploy_user="${2:?deployment user required}"
source_dir="${3:?directory containing reset tooling required}"
[[ "$site" =~ ^/[A-Za-z0-9_./-]+$ && "$site" != / && "$site" != *..* ]] || { echo 'Invalid testing site path' >&2; exit 1; }
[[ "$deploy_user" =~ ^[A-Za-z_][A-Za-z0-9_-]*$ ]] || { echo 'Invalid deployment user' >&2; exit 1; }
test -f "$site/.openelis-ci/target.json"
test -f "$source_dir/openelis-testing-reset.service"
test -f "$source_dir/openelis-testing-reset.timer"
# Deployment already created this user-owned lock. Read-open avoids Linux's
# protected_regular restriction on root recreating/truncating files in /tmp.
exec 9</tmp/openelis-testing-deploy.lock
flock -n 9
install -d -m 755 /usr/local/lib/openelis-testing
for filename in reset-testing.py deploy-published-testing.py check-readiness.py; do
  install -m 644 "$source_dir/$filename" "/usr/local/lib/openelis-testing/$filename"
done
printf 'SITE_PATH=%s\n' "$site" > /etc/openelis-testing-reset.conf
chmod 644 /etc/openelis-testing-reset.conf
sed "s/@DEPLOY_USER@/$deploy_user/g" "$source_dir/openelis-testing-reset.service" > /etc/systemd/system/openelis-testing-reset.service
install -m 644 "$source_dir/openelis-testing-reset.timer" /etc/systemd/system/openelis-testing-reset.timer
systemctl daemon-reload
systemctl enable --now openelis-testing-reset.timer
systemctl list-timers openelis-testing-reset.timer --no-pager
