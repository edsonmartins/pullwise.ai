# Multi-stage build para frontend React/Vite
FROM node:20-alpine AS build

WORKDIR /app

# Copia package files e instala dependências
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps

# Copia código fonte e build
COPY . .
RUN npm run build

# Runtime stage com nginx
FROM nginx:alpine

# Copia config customizada do nginx
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

# Copia build da stage anterior
COPY --from=build /app/dist /usr/share/nginx/html

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Expose porta
EXPOSE 80

# Inicia nginx
CMD ["nginx", "-g", "daemon off;"]
