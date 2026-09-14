FROM node:22-alpine AS builder
WORKDIR /app

# Install all dependencies including devDependencies for tsc
COPY package*.json ./
RUN npm ci --ignore-scripts

# Copy source files
COPY tsconfig.json ./
COPY src/ ./src/

# Compile TypeScript
RUN npm run build

# Runtime stage
FROM node:22-alpine
WORKDIR /app

# Install only production dependencies
COPY package*.json ./
RUN npm ci --omit=dev --ignore-scripts

# Copy compiled files from builder
COPY --from=builder /app/dist ./dist

# Run as non-root user
RUN addgroup -S mcpuser && adduser -S mcpuser -G mcpuser
RUN chown -R mcpuser:mcpuser /app
USER mcpuser

# Expose MCP port
EXPOSE 8081

# Start the server
CMD ["node", "dist/index.js"]
