FROM node:24.13.0-alpine AS build
WORKDIR /src
COPY package.json package-lock.json .npmrc ./
RUN npm ci --no-audit --no-fund
COPY index.html tsconfig.json vite.config.ts ./
COPY src ./src
RUN npx tsc --noEmit && npx vite build

FROM nginx:1.30.5-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html
# Run as the unprivileged nginx user (SB-11): it needs to write the cache, the rendered
# config and the pid file only.
RUN sed -i '/^user /d; s#^pid .*#pid /tmp/nginx.pid;#' /etc/nginx/nginx.conf \
    && chown -R nginx:nginx /var/cache/nginx /etc/nginx/conf.d
USER nginx
EXPOSE 8080
