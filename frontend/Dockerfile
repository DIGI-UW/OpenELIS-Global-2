# frontend/Dockerfile
#
# Single multi-stage source of truth for dev, CI, and prod frontend images.
# Docker's 2026 canonical pattern (https://docs.docker.com/build/building/multi-stage/):
# one file, named stages, callers pick via `--target=` or compose `build.target:`.
#
# Stages:
#   base    — node + deps + source (shared foundation, deduped by layer cache)
#   dev     — Vite dev server with HMR (used by harness local-dev mode)
#   build   — runs `npm run build` to produce dist/ (intermediate, not shipped)
#   runtime — nginx serving the built SPA (used by CI, prod, harness CI-parity)
#
# Callers:
#   - build.docker-compose.yml            → no `target:` → defaults to last stage = runtime
#   - .github/workflows/frontend.yml      → same default → runtime
#   - .github/workflows/publish-dev-*.yml → same default → runtime (multi-platform)
#   - .github/workflows/e2e-playwright.yml → bake reads compose → runtime
#   - projects/analyzer-harness/docker-compose.dev.yml → `build.target: dev`
#
# `runtime` MUST stay last (build-push-action defaults to the final stage).

# ---- base: shared foundation ----
FROM node:20.20-alpine AS base
RUN apk update && apk upgrade && apk add --no-cache bash
WORKDIR /app
ENV PATH=/app/node_modules/.bin:$PATH
COPY package.json package-lock.json ./
# legacy-peer-deps: React 17 + @carbon/react v1.15 genuinely requires it.
# Durable fix lands in #3452 (pnpm migration, epic #3449 M1.f).
RUN npm ci --legacy-peer-deps
COPY . ./

# ---- dev: Vite dev server (HMR via volume mounts from harness compose) ----
FROM base AS dev
EXPOSE 80
CMD ["npm", "start"]

# ---- build: produce dist/ (intermediate stage, never shipped directly) ----
FROM base AS build
RUN npm run build

# ---- runtime: nginx serving static SPA on HTTP :80 (edge-termination pattern) ----
FROM nginx:1.27-alpine AS runtime
USER root
COPY --from=build /app/dist /usr/share/nginx/html
COPY --from=build /app/nginx/nginx.conf /etc/nginx/conf.d/default.conf
RUN apk update && apk upgrade && apk add --no-cache bash curl
ADD nginx/healthcheck.sh /healthcheck.sh
RUN chown nginx:nginx /healthcheck.sh && chmod 770 /healthcheck.sh
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
