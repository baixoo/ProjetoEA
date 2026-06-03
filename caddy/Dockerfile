FROM node:24.14.1-alpine AS build
WORKDIR /app
COPY frontend/NoTUB_Frontend/ .
RUN npm install
RUN npm run build -- -m pwa

FROM caddy:2
COPY caddy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist/pwa /srv/frontend
EXPOSE 80 443
