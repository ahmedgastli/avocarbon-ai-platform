# Stage 1: Build the Angular application
FROM node:22-alpine AS build
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy the rest of the application source code
COPY . .

# Replace hardcoded localhost:8080 with relative path for Nginx reverse proxy
RUN find src -type f -name "*.ts" -exec sed -i 's|http://localhost:8080||g' {} +

# Build the application
RUN npm run build

# Stage 2: Serve the application using Nginx
FROM nginx:alpine

# Remove default Nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy the built Angular app from the build stage
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Copy the custom Nginx configuration for SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
