#!/bin/sh
# Creates keys only for an empty installation. Ordinary restarts validate and retain
# the complete existing identity. --upgrade-existing is a separate, explicit TLS
# adoption step; it preserves the supplied effective database-secret password and
# never converts encrypted database values.
set -eu
umask 077

fail() { echo "Install keys: $*" >&2; exit 1; }
mode=${1:-start}
case "$mode" in start|--upgrade-existing) ;; *) fail "Usage: $0 [--upgrade-existing]" ;; esac
[ "$#" -le 1 ] || fail "Too many arguments"

STORES=/etc/openelis-global
INSTALL="$STORES/install"
KEY="$INSTALL/server.key"
CERT="$INSTALL/server.crt"
PROXY_CERT=/etc/ssl/certs/apache-selfsigned.crt
PROXY_KEY=/etc/ssl/private/apache-selfsigned.key
# The self-signed certificate the public certgen image ships; it is rebuilt from time
# to time, so it is known by its name rather than its fingerprint.
IMAGE_SUBJECT="CN=localhost,OU=DIGI,O=I-TECH-UW,L=Seattle,ST=WA,C=US"
REPOSITORY_IMAGE_SUBJECT="CN=localhost,OU=DIGI,O=DIGI-UW,L=Seattle,ST=WA,C=US"
export KEYSTORE_PW="${KEYSTORE_PW:-kspass}"
export TRUSTSTORE_PW="${TRUSTSTORE_PW:-tspass}"
DAYS="${CERT_VALIDITY_DAYS:-3650}"

fingerprint() {
  openssl x509 -noout -fingerprint -sha256 2>/dev/null | cut -d= -f2
}

# Whether the PEM certificate on stdin is the certgen image's.
is_image_cert() {
  pem=$(cat)
  [ -n "$pem" ] || return 1
  subject=$(printf '%s\n' "$pem" | openssl x509 -noout -subject -nameopt RFC2253 2>/dev/null | sed 's/^subject= *//')
  issuer=$(printf '%s\n' "$pem" | openssl x509 -noout -issuer -nameopt RFC2253 2>/dev/null | sed 's/^issuer= *//')
  [ "$subject" = "$issuer" ] || return 1
  [ "$subject" = "$IMAGE_SUBJECT" ] || [ "$subject" = "$REPOSITORY_IMAGE_SUBJECT" ]
}

keystore_opens() {
  openssl pkcs12 -in "$STORES/keystore" -passin env:KEYSTORE_PW -noout >/dev/null 2>&1
}

keystore_cert() {
  openssl pkcs12 -in "$STORES/keystore" -passin env:KEYSTORE_PW -nokeys -clcerts 2>/dev/null
}

truststore_opens() {
  keytool -list -keystore "$STORES/truststore" -storetype pkcs12 -storepass:env TRUSTSTORE_PW >/dev/null 2>&1
}

truststore_cert() {
  keytool -exportcert -rfc -alias oeCert -keystore "$STORES/truststore" -storetype pkcs12 \
    -storepass:env TRUSTSTORE_PW 2>/dev/null
}

# Validate before changing any files, including operator-provided proxy keys.
[ "${#KEYSTORE_PW}" -ge 6 ] && [ "${#TRUSTSTORE_PW}" -ge 6 ] || fail "Store passwords must have at least six characters"
if [ -e "$PROXY_CERT" ] || [ -L "$PROXY_CERT" ] || [ -e "$PROXY_KEY" ] || [ -L "$PROXY_KEY" ]; then
  [ -s "$PROXY_CERT" ] && [ -s "$PROXY_KEY" ] || fail "Incomplete proxy identity; restore both certificate and key"
  proxy_public=$(openssl x509 -in "$PROXY_CERT" -pubkey -noout) || fail "Invalid proxy certificate"
  key_public=$(openssl pkey -in "$PROXY_KEY" -pubout 2>/dev/null) || fail "Invalid proxy key"
  [ "$proxy_public" = "$key_public" ] || fail "Proxy key does not match its certificate"
fi

fresh=false
if [ ! -d "$STORES" ] || [ -z "$(ls -A "$STORES")" ]; then
  fresh=true
fi
if [ "$fresh" = true ] && [ "$mode" = start ] && { [ -e "$PROXY_CERT" ] || [ -e "$PROXY_KEY" ]; }; then
  fail "Proxy identity exists without its stores; restore the complete backup or explicitly adopt the installation"
fi
if [ "$fresh" = false ] && [ "$mode" = start ]; then
  for file in keystore client_facing_keystore truststore install/server.key install/server.crt install/encryption.key; do
    [ -s "$STORES/$file" ] || fail "Incomplete security material ($file); restore the complete backup. Existing installations require explicit --upgrade-existing adoption"
  done
  [ ! -e "$INSTALL/.initializing" ] || fail "Initialization was interrupted; restore the complete backup"
  [ -s "$PROXY_CERT" ] && [ -s "$PROXY_KEY" ] || fail "Missing proxy identity; restore both certificate and key"
fi
if [ "$mode" = --upgrade-existing ] && [ ! -s "$INSTALL/encryption.key" ]; then
  [ -n "${OE_ENCRYPTION_PASSWORD:-}" ] || fail "Existing installations require their effective OE_ENCRYPTION_PASSWORD; no database encryption key will be guessed or generated"
fi

if [ -s "$STORES/keystore" ] && ! keystore_opens; then
  echo "ERROR: KEYSTORE_PW cannot open the existing keystore; restore its configured password before restarting" >&2
  exit 1
fi
if [ -s "$STORES/truststore" ] && ! truststore_opens; then
  echo "ERROR: TRUSTSTORE_PW cannot open the existing truststore; restore its configured password before restarting" >&2
  exit 1
fi

if [ -e "$STORES/client_facing_keystore" ]; then
  openssl pkcs12 -in "$STORES/client_facing_keystore" -passin env:KEYSTORE_PW -noout >/dev/null 2>&1 || fail "KEYSTORE_PW cannot open the existing client_facing_keystore"
fi
if [ "$fresh" = false ] && [ "$mode" = start ]; then
  keystore_cert | is_image_cert && fail "Shared image identity requires explicit --upgrade-existing adoption"
  openssl pkcs12 -in "$STORES/client_facing_keystore" -passin env:KEYSTORE_PW -nokeys -clcerts 2>/dev/null | is_image_cert && fail "Shared client-facing identity requires explicit --upgrade-existing adoption"
  install_public=$(openssl x509 -in "$CERT" -pubkey -noout) || fail "Invalid installation certificate"
  key_public=$(openssl pkey -in "$KEY" -pubout 2>/dev/null) || fail "Invalid installation private key"
  [ "$install_public" = "$key_public" ] || fail "Installation key does not match its certificate"
  [ "$(keystore_cert | fingerprint)" = "$(fingerprint <"$CERT")" ] || fail "Keystore changed; use explicit --upgrade-existing after checking the replacement"
  [ "$(truststore_cert | fingerprint)" = "$(fingerprint <"$CERT")" ] || fail "Truststore no longer trusts this installation; restore or explicitly adopt the replacement"
fi

mkdir -p "$INSTALL"
chown 0:0 "$STORES" "$INSTALL"
chmod 755 "$STORES"
chmod 700 "$INSTALL"
if [ "$fresh" = true ]; then touch "$INSTALL/.initializing"; fi

# The install's key for database secrets, read by the webapp's entrypoint.
if [ ! -s "$INSTALL/encryption.key" ]; then
  if [ "$mode" = --upgrade-existing ]; then
    printf '%s' "$OE_ENCRYPTION_PASSWORD" >"$INSTALL/encryption.key.tmp"
  else
    echo "Generating this install's encryption key for database secrets"
    openssl rand -hex 32 >"$INSTALL/encryption.key.tmp"
  fi
  mv "$INSTALL/encryption.key.tmp" "$INSTALL/encryption.key"
fi


# The private keystore is authoritative, including after an operator rotates it.
if [ -s "$STORES/keystore" ] && ! keystore_cert | is_image_cert; then
  if [ ! -s "$KEY" ] || [ ! -s "$CERT" ] || [ "$(keystore_cert | fingerprint)" != "$(fingerprint <"$CERT")" ]; then
    echo "Keeping the certificate this install's keystore already holds"
    (umask 077 && openssl pkcs12 -in "$STORES/keystore" -passin env:KEYSTORE_PW -nocerts -nodes \
      -out "$KEY.tmp" 2>/dev/null)
    keystore_cert | openssl x509 -out "$CERT.tmp"
    chmod 600 "$KEY.tmp"
    mv "$KEY.tmp" "$KEY"
    mv "$CERT.tmp" "$CERT"
  fi
elif [ ! -s "$KEY" ] || [ ! -s "$CERT" ] || is_image_cert <"$CERT"; then
  echo "Generating this install's TLS key and certificate"
  openssl req -x509 -nodes -newkey rsa:2048 -days "$DAYS" \
    -keyout "$KEY.tmp" -out "$CERT.tmp" \
    -subj "/O=OpenELIS Global/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,DNS:*.openelis.org,DNS:*.openelis-global.org,IP:127.0.0.1" 2>/dev/null
  chmod 600 "$KEY.tmp"
  mv "$KEY.tmp" "$KEY"
  mv "$CERT.tmp" "$CERT"
fi

current=$(fingerprint <"$CERT")

if [ ! -s "$STORES/keystore" ] || keystore_cert | is_image_cert; then
  echo "Building the keystore from this install's key"
  openssl pkcs12 -export -inkey "$KEY" -in "$CERT" -out "$STORES/keystore.tmp" -passout env:KEYSTORE_PW
  mv "$STORES/keystore.tmp" "$STORES/keystore"
  cp "$STORES/keystore" "$STORES/client_facing_keystore"
fi

if [ ! -s "$STORES/client_facing_keystore" ] || openssl pkcs12 -in "$STORES/client_facing_keystore" -passin env:KEYSTORE_PW -nokeys -clcerts 2>/dev/null | is_image_cert; then
  cp "$STORES/keystore" "$STORES/client_facing_keystore"
fi

if [ ! -s "$STORES/truststore" ] || [ "$(truststore_cert | fingerprint)" != "$current" ]; then
  echo "Trusting this install's certificate in the truststore"
  rm -f "$STORES/truststore.tmp"
  if [ -s "$STORES/truststore" ]; then
    cp "$STORES/truststore" "$STORES/truststore.tmp"
    keytool -delete -alias oeCert -keystore "$STORES/truststore.tmp" -storetype pkcs12 \
      -storepass:env TRUSTSTORE_PW >/dev/null 2>&1 || true
  fi
  keytool -importcert -noprompt -alias oeCert -file "$CERT" -keystore "$STORES/truststore.tmp" \
    -storetype pkcs12 -storepass:env TRUSTSTORE_PW >/dev/null
  mv "$STORES/truststore.tmp" "$STORES/truststore"
fi

# Both Tomcat consumers already belong to TLS group 8443. The Bridge needs only
# the public truststore; it must not gain access to the private stores or key.
for store in keystore client_facing_keystore; do
  chown 0:8443 "$STORES/$store"
  chmod 640 "$STORES/$store"
done
chmod 644 "$STORES/truststore"
chown 0:0 "$KEY" "$INSTALL/encryption.key"
chmod 600 "$KEY" "$INSTALL/encryption.key"

# Missing, or still the image's certificate; a symlink or any other certificate is kept.
if [ -d "$(dirname "$PROXY_CERT")" ] && [ -d "$(dirname "$PROXY_KEY")" ] && {
  { [ ! -e "$PROXY_CERT" ] && [ ! -L "$PROXY_CERT" ]; } ||
    { [ -f "$PROXY_CERT" ] && [ ! -L "$PROXY_CERT" ] && is_image_cert <"$PROXY_CERT"; }
}; then
  echo "Giving the proxy this install's certificate"
  cp "$CERT" "$PROXY_CERT"
  cp "$KEY" "$PROXY_KEY"
  chmod 644 "$PROXY_CERT"
  chmod 600 "$PROXY_KEY"
fi

rm -f "$INSTALL/.initializing"
echo "TLS stores hold this install's certificate (SHA-256 $current)"
