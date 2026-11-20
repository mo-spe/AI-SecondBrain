ARG WEB_BUILD_STAGE=web-build
FROM node:22-alpine AS web-build
WORKDIR /app/web
COPY web/package*.json ./
RUN npm ci
COPY web/ ./
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM scratch AS web-prebuilt
COPY web/dist /app/web/dist

FROM ${WEB_BUILD_STAGE} AS web-assets

FROM node:22-alpine
WORKDIR /app/server
COPY server/package*.json ./
RUN npm ci --omit=dev
COPY server/src ./src
COPY server/.env.example ./.env.example
COPY --from=web-assets /app/web/dist ./public
ENV SERVE_WEB=true
ENV NODE_ENV=production
USER node
EXPOSE 4000
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 CMD node -e "fetch('http://127.0.0.1:4000/api/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["npm", "start"]
