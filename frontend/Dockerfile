FROM node:22-alpine AS build
WORKDIR /app

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ .
COPY moviesApi.yaml oscarApi.yaml ./
RUN npm run build \
 && cp -r node_modules/swagger-ui-dist dist/docs \
 && cp swagger-index.html dist/docs/index.html \
 && rm -rf dist/docs/es dist/docs/*.map \
 && cp moviesApi.yaml oscarApi.yaml dist/docs/

FROM nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 443
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --no-check-certificate --spider https://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
