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

$CATALINA_HOME/bin/catalina.sh run
