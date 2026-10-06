# Build stage: Node from tech-stack.md; installs exactly the lock file.
FROM node:24.13.0-alpine AS build
WORKDIR /build
COPY package.json package-lock.json .npmrc ./
RUN npm ci --no-fund --no-audit
COPY index.html tsconfig.json vite.config.ts ./
COPY src/ src/
RUN npm run build

# Runtime stage: nginx as the unprivileged nginx user on port 8080 (SB-11).
FROM nginx:1.30.5-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY write-config.sh /usr/local/bin/write-config.sh
COPY --from=build /build/dist/ /usr/share/nginx/html/
RUN sed -i 's/\r$//' /usr/local/bin/write-config.sh && chmod 0755 /usr/local/bin/write-config.sh \
  && mkdir -p /tmp/confreg && chown nginx:nginx /tmp/confreg
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
ENTRYPOINT ["/usr/local/bin/write-config.sh"]
CMD ["nginx", "-g", "daemon off;"]
