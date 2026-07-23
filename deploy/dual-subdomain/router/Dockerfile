# Minimal umbrella router: stock nginx + the two tools the entrypoint needs
# (envsubst for template rendering, openssl for the self-signed fallback).
FROM nginx:1.27-alpine
RUN apk add --no-cache openssl gettext bash
COPY docker-entrypoint.sh /docker-entrypoint-router.sh
RUN chmod +x /docker-entrypoint-router.sh
ENTRYPOINT ["/docker-entrypoint-router.sh"]
