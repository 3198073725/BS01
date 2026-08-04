# VidSprout production web container
# Builds both frontend SPAs and serves them with nginx (single entry point).

# ---------- web-client build ----------
FROM node:20-alpine AS web-client-build
WORKDIR /app
COPY web-client/package.json web-client/package-lock.json ./
RUN npm ci
COPY web-client/ ./
RUN npm run build

# ---------- admin-console build ----------
FROM node:20-alpine AS admin-console-build
WORKDIR /app
COPY admin-console/package.json admin-console/package-lock.json ./
RUN npm ci
COPY admin-console/ ./
RUN npm run build

# ---------- runtime ----------
FROM nginx:1.27-alpine
COPY deploy/nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY deploy/nginx/security-headers.conf /etc/nginx/security-headers.conf
COPY --from=web-client-build /app/dist /usr/share/nginx/html/web
COPY --from=admin-console-build /app/dist /usr/share/nginx/html/admin
EXPOSE 80
