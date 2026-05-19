# Use Node 24.14.1 -- use a fixed version so changes in future node don't break my app
FROM node:24.14.1-alpine AS build

WORKDIR /app

COPY NoTUB_Frontend/ .
RUN npm install

RUN npm run build

FROM nginx:stable
COPY --from=build /app/dist/spa /usr/share/nginx/html
COPY default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]