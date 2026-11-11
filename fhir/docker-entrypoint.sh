#!/usr/bin/env bash
# The previous FHIR image was Bitnami Tomcat, and existing deployments (the installer template,
# older compose files) mount their server.xml at /opt/bitnami/tomcat/conf/server.xml. This image
# runs the official Tomcat, whose configuration lives in /usr/local/tomcat/conf, so a file mounted
# at the old path is applied there before Tomcat starts. Deployments that pass the hapi.ssl.*
# properties instead (CI, openelis-docker) use the baked server.xml unchanged.
set -euo pipefail

LEGACY_SERVER_XML=/opt/bitnami/tomcat/conf/server.xml
if [ -f "$LEGACY_SERVER_XML" ]; then
  echo "[fhir-entrypoint] applying server.xml mounted at the legacy Bitnami path"
  cp "$LEGACY_SERVER_XML" "$CATALINA_HOME/conf/server.xml"
fi

exec catalina.sh run "$@"
