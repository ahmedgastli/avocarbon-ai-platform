# Stage 1: Build the Angular application
FROM node:22-alpine AS build
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci --ignore-scripts

# Copy the rest of the application source code
COPY . .

# Replace hardcoded localhost:8080 with relative path for Nginx reverse proxy
RUN find src -type f -name "*.ts" -exec sed -i 's|http://localhost:8080||g' {} +

# Build the application
RUN npm run build

# Stage 2: Serve the application using Nginx (unprivileged, non-root)
FROM nginxinc/nginx-unprivileged:alpine

# Copy the built Angular app from the build stage directly into html dir.
# Using COPY --chown avoids needing root-level rm -rf on nginx-unprivileged.
COPY --from=build --chown=nginx:nginx /app/dist/frontend/browser /usr/share/nginx/html

# Copy the custom Nginx configuration for SPA routing
COPY --chown=nginx:nginx nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 8080 (nginx-unprivileged listens on 8080, not 80)
EXPOSE 8080

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
