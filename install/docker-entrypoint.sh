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

# Configure context path from environment variable if provided
# Default is /lab
CONTEXT_PATH="${SERVER_SERVLET_CONTEXT_PATH:-/lab}"
CONTEXT_PATH=$(echo "$CONTEXT_PATH" | sed 's|^/*|/|' | sed 's|/*$||')

echo "Configuring OpenELIS context path to: ${CONTEXT_PATH}"

# Update server.xml to enable HTTP connector and set context path
if [ -f /usr/local/tomcat/conf/server.xml ]; then
    # Enable HTTP connector on port 8080 (uncomment it)
    sed -i 's|<!-- <Connector port="8080" protocol="HTTP/1.1"|          <Connector port="8080" protocol="HTTP/1.1"|g' /usr/local/tomcat/conf/server.xml
    sed -i 's|               redirectPort="8443" /> -->|               redirectPort="8443" />|g' /usr/local/tomcat/conf/server.xml
    
    # Add RemoteIpValve to trust proxy headers (so HTTPS redirects work correctly behind proxy)
    # This tells Tomcat to trust X-Forwarded-Proto header from the gateway
    if ! grep -q "RemoteIpValve" /usr/local/tomcat/conf/server.xml; then
        # Create RemoteIpValve XML fragment in a temp file to avoid sed escaping issues
        cat > /tmp/remoteip_valve.xml << 'REMOTEIP_VALVE'
               <!-- RemoteIpValve to trust proxy headers -->
               <Valve className="org.apache.catalina.valves.RemoteIpValve"
                      remoteIpHeader="x-forwarded-for"
                      protocolHeader="x-forwarded-proto"
                      portHeader="x-forwarded-port"
                      proxiesHeader="x-forwarded-by"
                      internalProxies="172\.\d{1,3}\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}"
                      trustedProxies="172\.\d{1,3}\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}"/>
REMOTEIP_VALVE
        # Insert after Engine tag
        sed -i '/<Engine name="Catalina" defaultHost="localhost">/r /tmp/remoteip_valve.xml' /usr/local/tomcat/conf/server.xml
        rm -f /tmp/remoteip_valve.xml
        echo "Added RemoteIpValve to trust proxy headers"
    else
        # Fix existing RemoteIpValve regex pattern if it's broken
        sed -i 's|internalProxies="172\.d{1,3}\.d{1,3}\.d{1,3}|internalProxies="172\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|g' /usr/local/tomcat/conf/server.xml
        sed -i 's|trustedProxies="172\.d{1,3}\.d{1,3}\.d{1,3}|trustedProxies="172\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|g' /usr/local/tomcat/conf/server.xml
        sed -i 's|10\.d{1,3}\.d{1,3}\.d{1,3}|10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|g' /usr/local/tomcat/conf/server.xml
        sed -i 's|192\.168\.d{1,3}\.d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|g' /usr/local/tomcat/conf/server.xml
        echo "Fixed RemoteIpValve regex pattern"
    fi
    
    # Update context path - handle both old format (/api/OpenELIS-Global/) and new format
    sed -i "s|path=\"/api/OpenELIS-Global/\"|path=\"${CONTEXT_PATH}\"|g" /usr/local/tomcat/conf/server.xml
    sed -i "s|path=\"/api/OpenELIS-Global\"|path=\"${CONTEXT_PATH}\"|g" /usr/local/tomcat/conf/server.xml
    sed -i "s|path=\"/lab\"|path=\"${CONTEXT_PATH}\"|g" /usr/local/tomcat/conf/server.xml
    
    # Remove ROOT context if it exists (we don't need /api anymore)
    sed -i '/<Context docBase="ROOT" path="\/api"\/>/d' /usr/local/tomcat/conf/server.xml
    
    echo "Updated server.xml: enabled HTTP connector and set context path to ${CONTEXT_PATH}"
else
    echo "WARNING: server.xml not found at /usr/local/tomcat/conf/server.xml"
fi

$CATALINA_HOME/bin/catalina.sh run
