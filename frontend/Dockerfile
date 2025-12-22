# Build stage
FROM node:25-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install ALL dependencies (devDependencies needed for Angular build)
# Using npm install instead of npm ci because this is part of a monorepo
# and the lock file is managed at root level
RUN npm install --legacy-peer-deps

# Copy source code
COPY . .

# Build argument for API URL (Angular uses environment files, not env vars at runtime)
ARG API_URL=http://localhost:8080/api

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets from build stage
COPY --from=build /app/dist/texas-holdem-frontend /usr/share/nginx/html

# Add non-root user for security
RUN addgroup -g 1001 -S nodejs && adduser -S angular -u 1001

# Set permissions
RUN chown -R angular:nodejs /usr/share/nginx/html && \
    chown -R angular:nodejs /var/cache/nginx && \
    chown -R angular:nodejs /var/log/nginx && \
    chown -R angular:nodejs /etc/nginx/conf.d

# Switch to non-root user
USER angular

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

# Expose port
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
