#!/usr/bin/env bash
# After mvn package (with test compilation), exercise the production entrypoint and upload service.
set -euo pipefail
cd "$(dirname "$0")/../.."
image="${OE_PERMISSION_TEST_IMAGE:-itechuw/openelis-global-2-dev:develop}"
docker run --rm --user 0 --entrypoint sh \
  -v "$PWD/install/docker-entrypoint.sh:/entrypoint.sh:ro" \
  -v "$PWD/target/classes:/classes:ro" \
  -v "$PWD/target/test-classes:/test-classes:ro" \
  -v "$PWD/target/OpenELIS-Global/WEB-INF/lib:/libraries:ro" \
  -e SSL_TRUSTSTORE_PATH=/tmp/truststore \
  "$image" -ec '
    export OE_TRUSTSTORE_PROBE_PASSWORD=$(od -An -N24 -tx1 /dev/urandom | tr -d " \n")
    export EXPECTED_ENCRYPTION_KEY=$(od -An -N32 -tx1 /dev/urandom | tr -d " \n")
    unset OE_ENCRYPTION_PASSWORD
    mkdir -p /etc/openelis-global/install
    printf "%s" "$EXPECTED_ENCRYPTION_KEY" > /etc/openelis-global/install/encryption.key
    chmod 600 /etc/openelis-global/install/encryption.key
    keytool -genkeypair -alias partner -keyalg RSA -dname CN=permission-test \
      -keystore /tmp/identity -storepass:env OE_TRUSTSTORE_PROBE_PASSWORD -keypass:env OE_TRUSTSTORE_PROBE_PASSWORD >/dev/null 2>&1
    keytool -exportcert -rfc -alias partner -keystore /tmp/identity \
      -storepass:env OE_TRUSTSTORE_PROBE_PASSWORD -file /tmp/partner.pem >/dev/null 2>&1
    keytool -importcert -noprompt -alias initial -file /tmp/partner.pem \
      -keystore /tmp/truststore -storepass:env OE_TRUSTSTORE_PROBE_PASSWORD >/dev/null 2>&1
    chmod 644 /tmp/truststore
    chmod 600 /tmp/identity
    java_bin=$(command -v java)
    if su tomcat_admin -c "$java_bin -cp /classes:/test-classes:/libraries/\* org.openelisglobal.security.certs.TruststoreWriteProbe" > /tmp/before.log 2>&1; then
      echo "Expected upload to fail before ownership repair" >&2; exit 1
    fi
    grep -q "Permission denied" /tmp/before.log || { cat /tmp/before.log; exit 1; }
    mkdir -p /tmp/probe-tomcat/bin
    printf "%s\n" "#!/bin/sh" "test \"\$OE_ENCRYPTION_PASSWORD\" = \"\$EXPECTED_ENCRYPTION_KEY\" || exit 1" "exec $java_bin -cp /classes:/test-classes:/libraries/\* org.openelisglobal.security.certs.TruststoreWriteProbe" > /tmp/probe-tomcat/bin/catalina.sh
    chmod 755 /tmp/probe-tomcat/bin/catalina.sh
    CATALINA_HOME=/tmp/probe-tomcat sh /entrypoint.sh
    test "$(stat -c %a /tmp/identity)" = 600
    test "$(stat -c %u /tmp/identity)" = 0
    echo "PASS: installation key loaded; upload repaired; private store permissions unchanged"
  '
