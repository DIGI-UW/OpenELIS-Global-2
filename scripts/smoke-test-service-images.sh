#!/usr/bin/env bash
# Run after building db/Dockerfile and fhir/Dockerfile with the tags below.
set -euo pipefail

DOCKER=${DOCKER:-docker}
DB_IMAGE=${DB_IMAGE:-openelis-base-refresh-db:local}
FHIR_IMAGE=${FHIR_IMAGE:-openelis-base-refresh-fhir:local}
export DOCKER_DEFAULT_PLATFORM=${DOCKER_DEFAULT_PLATFORM:-linux/amd64}
evidence=$(mktemp -d "${TMPDIR:-/tmp}/oe-service-images.XXXXXXXX")
name=$(basename "$evidence" | tr . -)

cleanup() {
  result=$?
  trap - EXIT
  "$DOCKER" logs "$name-db" > "$evidence/database.log" 2>&1 || true
  "$DOCKER" logs "$name-fhir" > "$evidence/fhir.log" 2>&1 || true
  "$DOCKER" rm -f -v "$name-fhir" "$name-db" >/dev/null 2>&1 || true
  "$DOCKER" volume rm "$name-certs" >/dev/null 2>&1 || true
  "$DOCKER" network rm "$name" >/dev/null 2>&1 || true
  printf 'Smoke-test logs: %s\n' "$evidence"
  exit "$result"
}
trap cleanup EXIT

"$DOCKER" network create "$name" >/dev/null
"$DOCKER" volume create "$name-certs" >/dev/null
"$DOCKER" run --rm --user root -v "$name-certs:/certs" "$FHIR_IMAGE" sh -ec '
  keytool -genkeypair -alias smoke -keyalg RSA -validity 2 -dname CN=localhost \
    -ext SAN=dns:localhost -ext EKU=serverAuth,clientAuth -storetype PKCS12 \
    -keystore /certs/smoke.p12 -storepass smoke-password -noprompt
  keytool -exportcert -rfc -alias smoke -keystore /certs/smoke.p12 \
    -storepass smoke-password -file /certs/smoke.crt
  keytool -importcert -alias smoke -file /certs/smoke.crt -storetype PKCS12 \
    -keystore /certs/trust.p12 -storepass smoke-password -noprompt
  chgrp 8443 /certs/*
  chmod 640 /certs/*
'

"$DOCKER" run -d --name "$name-db" --network "$name" --network-alias db \
  --memory 512m --cpus 1 \
  -e POSTGRES_PASSWORD=smoke-password -e POSTGRES_DB=clinlims \
  -e POSTGRES_INITDB_ARGS=--auth-host=md5 -e DB_PASSWORD=smoke-password \
  -e DB_SUPERUSER_PASSWORD=smoke-password "$DB_IMAGE" >/dev/null

db_ready() {
  "$DOCKER" exec -e PGPASSWORD=smoke-password "$name-db" psql \
    -h 127.0.0.1 -U clinlims -d clinlims -Atc \
    "SELECT count(*) FROM clinlims.site_information" >/dev/null 2>&1
}
wait_for() {
  deadline=$((SECONDS + 300))
  until "$@"; do
    if (( SECONDS >= deadline )); then
      printf 'Timed out waiting for %s\n' "$*" >&2
      return 1
    fi
    sleep 2
  done
}
wait_for db_ready
"$DOCKER" exec "$name-db" postgres --version

"$DOCKER" run -d --name "$name-fhir" --network "$name" --memory 2g --cpus 2 \
  -v "$name-certs:/certs:ro" \
  -e JAVA_OPTS='-Xms256m -Xmx1024m' \
  -e CATALINA_OPTS='-Dhapi.ssl.keystorepath=/certs/smoke.p12 -Dhapi.ssl.keystorepassword=smoke-password -Dhapi.ssl.truststorepath=/certs/trust.p12 -Dhapi.ssl.truststorepassword=smoke-password' \
  -e 'FHIR_DATASOURCE_URL=jdbc:postgresql://db:5432/clinlims?currentSchema=clinlims' \
  -e FHIR_DATASOURCE_USERNAME=clinlims -e FHIR_DATASOURCE_PASSWORD=smoke-password \
  -e FHIR_SERVER_ADRESS=https://localhost:8443/fhir/ "$FHIR_IMAGE" >/dev/null

fhir() {
  "$DOCKER" exec "$name-fhir" curl --fail --silent --show-error --max-time 15 \
    --cacert /certs/smoke.crt --cert-type P12 \
    --cert /certs/smoke.p12:smoke-password "$@"
}
fhir_ready() {
  fhir 'https://localhost:8443/fhir/metadata?_format=json' \
    > "$evidence/metadata.json" 2> "$evidence/readiness.log"
}
wait_for fhir_ready
jq -e '.resourceType == "CapabilityStatement" and .fhirVersion == "4.0.1"' \
  "$evidence/metadata.json" >/dev/null
test "$("$DOCKER" exec "$name-fhir" id -u)" = 8443
if "$DOCKER" exec "$name-fhir" curl --fail --silent --show-error --max-time 15 \
  --cacert /certs/smoke.crt https://localhost:8443/fhir/metadata \
  > "$evidence/no-client-cert.log" 2>&1; then
  printf 'FHIR unexpectedly accepted a request without a client certificate.\n' >&2
  exit 1
fi

fhir -X PUT -H 'Content-Type: application/fhir+json' \
  --data '{"resourceType":"Patient","id":"service-image-smoke","active":true}' \
  https://localhost:8443/fhir/Patient/service-image-smoke \
  > "$evidence/created.json"
jq -e '.resourceType == "Patient" and .id == "service-image-smoke"' \
  "$evidence/created.json" >/dev/null

"$DOCKER" stop "$name-fhir" "$name-db" >/dev/null
"$DOCKER" start "$name-db" >/dev/null
wait_for db_ready
"$DOCKER" start "$name-fhir" >/dev/null
wait_for fhir_ready
fhir https://localhost:8443/fhir/Patient/service-image-smoke \
  > "$evidence/persisted.json"
jq -e '.resourceType == "Patient" and .id == "service-image-smoke" and .active == true' \
  "$evidence/persisted.json" >/dev/null
printf 'PASS: database initialization, non-root FHIR, mutual TLS, R4 metadata, write/read across restart.\n'
