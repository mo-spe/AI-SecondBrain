ARG NGINX_IMAGE=nginx:stable-alpine
ARG WEB_BUILD_STAGE=build
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM scratch AS prebuilt
COPY dist /app/dist

FROM ${WEB_BUILD_STAGE} AS web-assets

FROM ${NGINX_IMAGE}
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=web-assets /app/dist /usr/share/nginx/html
EXPOSE 80

