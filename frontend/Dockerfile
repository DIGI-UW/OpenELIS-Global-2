# frontend/Dockerfile
#
# Production / CI / harness frontend image. Multi-stage:
#   - `build` stage starts FROM the shared base (openelis-frontend-base:local)
#     and runs `npm run build` to produce dist/.
#   - Final stage is nginx:alpine serving dist/ on HTTP :80.
#
# TLS terminates at the outer reverse proxy (real cert in prod, self-signed
# in the analyzer harness, cluster ingress in prod deploys). This container
# speaks plain HTTP internally — standard edge-termination pattern.
#
# Prereq: build Dockerfile.base first and tag as openelis-frontend-base:local.
# The harness's build.sh does this automatically; CI's pipeline runs the same
# pre-build step.

# --- Build stage: reuse the shared base, produce dist/ ---
FROM openelis-frontend-base:local AS build
RUN npm run build

# --- Runtime stage: nginx serving the built SPA ---
FROM nginx:1.27-alpine
USER root

# Static SPA artifact + the app's nginx site config
COPY --from=build /app/dist /usr/share/nginx/html
COPY --from=build /app/nginx/nginx.conf /etc/nginx/conf.d/default.conf

# Runtime tooling for healthcheck.sh
RUN apk update && apk upgrade && apk add --no-cache bash curl

# Healthcheck script (invoked by compose or orchestrator)
ADD nginx/healthcheck.sh /healthcheck.sh
RUN chown nginx:nginx /healthcheck.sh && chmod 770 /healthcheck.sh

EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
