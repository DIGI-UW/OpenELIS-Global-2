FROM node:22-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY server.mjs ./
ENV NODE_ENV=production
EXPOSE 8585
USER node
CMD ["node", "server.mjs"]
