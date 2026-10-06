# Frontend image (spec 7, 10.1): Vite build, served by unprivileged nginx (SB-11).
FROM node:24.13.0-alpine AS build
WORKDIR /build
COPY package.json package-lock.json .npmrc ./
RUN npm ci --no-fund --no-audit
COPY index.html tsconfig.json vite.config.ts ./
COPY src ./src
RUN npm run build

FROM nginx:1.30.5-alpine
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/40-runtime-config.sh /docker-entrypoint.d/40-runtime-config.sh
COPY --from=build /build/dist /usr/share/nginx/html
RUN chmod 0755 /docker-entrypoint.d/40-runtime-config.sh \
  && touch /usr/share/nginx/html/config.js \
  && chown nginx:nginx /usr/share/nginx/html/config.js

USER nginx
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=6 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1
