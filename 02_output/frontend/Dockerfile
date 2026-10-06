# Build stage: pinned Node image.
FROM node:24.13.0-alpine AS build
WORKDIR /build
COPY package.json package-lock.json .npmrc ./
RUN npm ci --no-fund --no-audit
COPY index.html tsconfig.json vite.config.ts ./
COPY src src
RUN npx vite build

# Runtime stage: nginx as the non-root user "nginx" on port 8080 (SB-11).
FROM nginx:1.30.5-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /build/dist /usr/share/nginx/html
RUN mkdir -p /tmp/nginx && chown -R nginx:nginx /tmp/nginx /var/cache/nginx
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=6 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
