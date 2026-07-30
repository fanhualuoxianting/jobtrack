FROM node:22.15.0-bookworm-slim AS build

WORKDIR /workspace
COPY jobtrack-web/package.json jobtrack-web/package-lock.json ./
RUN npm ci
COPY jobtrack-web/ ./
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run type-check && npm run build

FROM nginx:1.27.5-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
