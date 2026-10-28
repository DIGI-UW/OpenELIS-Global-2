#!/bin/sh
# Gives this install its own TLS key and keeps the key and trust stores built from it,
# and its own key for database secrets (install/encryption.key).
#
# Run by the compose `certs` service on every start. Only the certificate baked into
# the public certgen image, which every install shared, is replaced: the first start
# generates the install's key and certificate under /etc/openelis-global/install and
# rebuilds the stores and the proxy certificate that still hold the image's. A keystore
# that holds a certificate of the site's own is kept, and its key becomes the install's.
# A store that does not open with KEYSTORE_PW or TRUSTSTORE_PW stops startup unchanged. The
# proxy's certificate files are written only when they are missing or still hold the
# image's certificate, so a Let's Encrypt or locally issued one is kept.
set -eu

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

if [ -s "$STORES/keystore" ] && ! keystore_opens; then
  echo "ERROR: KEYSTORE_PW cannot open the existing keystore; restore its configured password before restarting" >&2
  exit 1
fi
if [ -s "$STORES/truststore" ] && ! truststore_opens; then
  echo "ERROR: TRUSTSTORE_PW cannot open the existing truststore; restore its configured password before restarting" >&2
  exit 1
fi

mkdir -p "$INSTALL"
chmod 700 "$INSTALL"

# The install's key for database secrets, read by the webapp's entrypoint.
if [ ! -s "$INSTALL/encryption.key" ]; then
  echo "Generating this install's encryption key for database secrets"
  (umask 077 && openssl rand -hex 32 >"$INSTALL/encryption.key.tmp")
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

for store in keystore client_facing_keystore truststore; do
  if [ -f "$STORES/$store" ]; then chmod a+r "$STORES/$store"; fi
done

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

echo "TLS stores hold this install's certificate (SHA-256 $current)"
