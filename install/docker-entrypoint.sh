#!/bin/sh

ENV_SECRETS_DIR="/run/secrets"

file_env_secret() {
    secret_name="$1"
    secret_file="${ENV_SECRETS_DIR}/${secret_name}"
    if [ -f "${secret_file}" ]; then
        secret_val=$(cat "${secret_file}")
#        export ${secret_name}="${secret_val}"
        export CATALINA_OPTS="${CATALINA_OPTS} -D${secret_name}=${secret_val}"
    else
        echo "Secret file does not exist! ${secret_file}"
    fi
}

#must stay the same as filename in docker-compose.yml
file_env_secret "datasource.password"

#############################################
# Permission Fix Section
#############################################

echo "Fixing OpenELIS runtime volume permissions..."

OE_LOGS="/var/lib/openelis-global/logs"
TOMCAT_LOGS="/usr/local/tomcat/logs"
LUCENE="/var/lib/lucene_index"
BRANDING="/var/lib/openelis-global/branding"

# Create dirs if missing (safe even if mounted)
mkdir -p \
  "$OE_LOGS" \
  "$TOMCAT_LOGS" \
  "$LUCENE" \
  "$BRANDING"

# Fix ownership → UID 8443 (tomcat_admin)
chown -R 8443:tomcat "$OE_LOGS" || true
chown -R 8443:tomcat "$TOMCAT_LOGS" || true
chown -R 8443:tomcat "$LUCENE" || true
chown -R 8443:tomcat "$BRANDING" || true

# Fix permissions
chmod -R 770 "$OE_LOGS" || true
chmod -R 770 "$TOMCAT_LOGS" || true
chmod -R 770 "$LUCENE" || true
chmod -R 770 "$BRANDING" || true

echo "Volume permissions ready."


#############################################
# Drop privileges & start Tomcat
#############################################

echo "Starting Tomcat as tomcat_admin..."

exec su tomcat_admin -c "$CATALINA_HOME/bin/catalina.sh run"
