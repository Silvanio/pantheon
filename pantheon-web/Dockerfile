FROM node:22-alpine AS build
WORKDIR /workspace
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_PANTHEON_SERVICE_URL
ENV VITE_PANTHEON_SERVICE_URL=${VITE_PANTHEON_SERVICE_URL}
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8585
