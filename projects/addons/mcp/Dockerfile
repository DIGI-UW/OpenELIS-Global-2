# The MCP server add-on service. Build from this directory:
#   podman build -t openelis-addon-mcp:1.0.0 .
FROM docker.io/library/node:22-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY tsconfig.json tsconfig.build.json ./
COPY src src
RUN npm run build \
 && npm ci --omit=dev --no-audit --no-fund

FROM docker.io/library/node:22-alpine
WORKDIR /opt/mcp
COPY --from=build /src/package.json ./
COPY --from=build /src/node_modules node_modules
COPY --from=build /src/dist dist
ENV NODE_ENV=production PORT=8080 MCP_DATA_DIR=/data
# the agent tokens (hashes only) and the call record go to /data; mount a volume there
RUN mkdir -p /data && chown 65534:65534 /data
VOLUME /data
USER 65534:65534
EXPOSE 8080
ENTRYPOINT ["node", "--max-old-space-size=64", "dist/main.js"]
