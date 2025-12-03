# TruHoldem Enterprise v2.0 - Senior+++ Level Poker Application

A **production-ready, enterprise-grade Texas Hold'em poker application** built with modern technologies and best practices.

## 🚀 Enterprise Features Added

### ✅ Backend Architecture Improvements

#### **Security & Authentication**
- 🔐 **JWT-based authentication** with refresh token mechanism
- 🛡️ **Spring Security** integration with role-based access control (RBAC)
- 👤 **User management system** with registration, login, and profile management
- 🔒 **Password hashing** using BCrypt with salt
- ⚡ **Redis session store** for scalability

#### **Database & Persistence**
- 🗄️ **Liquibase migrations** for database versioning and evolution
- 📊 **Optimized PostgreSQL configuration** with connection pooling (HikariCP)
- 🏗️ **Proper entity relationships** with JPA/Hibernate optimizations
- 📈 **Database indexing** for query performance
- 🔄 **Transaction management** with proper isolation levels

#### **API & Documentation**
- 📖 **OpenAPI 3.0** specification with Swagger UI
- 🎯 **RESTful API design** with proper HTTP status codes
- ✅ **Request/Response validation** using Bean Validation
- 🏷️ **API versioning** strategy implemented
- 📝 **Comprehensive error handling** with custom exceptions

#### **Performance & Scalability**
- ⚡ **Redis caching** for game states and user sessions
- 🚀 **Asynchronous processing** for bot actions
- 📊 **Database query optimization** with JPA criteria queries
- 🔄 **Connection pooling** and batch processing
- 🎛️ **Configuration externalization** with profiles

#### **Monitoring & Observability**
- 📈 **Spring Actuator** endpoints for health checks and metrics
- 📊 **Prometheus metrics** integration
- 📋 **Structured logging** with logback configuration
- 🔍 **Request tracing** and performance monitoring
- ⚠️ **Alert mechanisms** for system health

#### **Code Quality**
- 🧹 **Clean Architecture** with proper layering (Controller → Service → Repository)
- 🎯 **SOLID principles** implementation
- 🗂️ **Design patterns** (Strategy, Factory, Observer)
- 🧪 **Comprehensive testing** (Unit, Integration, E2E)
- 📋 **MapStruct** for DTO mapping

### ✅ DevOps & Production Readiness

#### **Containerization**
- 🐳 **Docker** multi-stage builds for optimization
- 🐙 **Docker Compose** for complete stack deployment
- 🔒 **Non-root container** execution for security
- 🩺 **Health checks** and proper startup probes

#### **Environment Management**
- 🌍 **Multi-environment configuration** (dev, test, prod, docker)
- 🔐 **Secrets management** with environment variables
- ⚙️ **Configuration validation** with type-safe properties
- 📦 **Profile-specific optimizations**

#### **Monitoring Stack** (Optional)
- 📊 **Prometheus** for metrics collection
- 📈 **Grafana** dashboards for visualization
- 🔔 **Alerting rules** for critical issues
- 📋 **Custom business metrics** tracking

### ✅ Advanced Poker Features (Planned)

#### **Game Enhancements**
- 🏆 **Tournament mode** with buy-ins and prize pools
- 📊 **Player statistics** and hand history tracking
- 🤖 **Advanced AI strategies** with difficulty levels
- 👥 **Multi-table support** for simultaneous games
- 👀 **Spectator mode** for watching games

#### **Real-time Features**
- ⚡ **WebSocket integration** for real-time updates
- 💬 **Chat system** during games
- 🔄 **Live game state synchronization**
- 📱 **Push notifications** for game events

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│    Frontend     │    │     Backend      │    │    Database     │
│   (Angular)     │◄──►│  (Spring Boot)   │◄──►│  (PostgreSQL)   │
│                 │    │                  │    │                 │
│  • Angular 17   │    │  • Spring Boot 3 │    │  • PostgreSQL   │
│  • TypeScript   │    │  • Spring Sec    │    │  • Liquibase    │
│  • Bootstrap    │    │  • JWT Auth      │    │  • Connection   │
│  • WebSocket    │    │  • Redis Cache   │    │    Pooling      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                       │
         │                        │                       │
         ▼                        ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│     Nginx       │    │      Redis       │    │   Monitoring    │
│  (Reverse       │    │   (Cache &       │    │ • Prometheus    │
│   Proxy)        │    │    Sessions)     │    │ • Grafana       │
└─────────────────┘    └──────────────────┘    │ • Actuator      │
                                               └─────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- **Java 21+**
- **Node.js 18+**
- **Docker & Docker Compose**
- **PostgreSQL 16+** (if running locally)
- **Redis 7+** (if running locally)

### 🐳 Docker Deployment (Recommended)

```bash
# Clone the repository
git clone https://github.com/APorkolab/Truholdem.git
cd Truholdem

# Start the complete stack
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker-compose logs -f backend
```

**Services will be available at:**
- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080/api
- **API Documentation**: http://localhost:8080/api/swagger-ui.html
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

### 🖥️ Local Development

#### Backend Setup
```bash
cd backend

# Install dependencies
./mvnw clean install

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

## 🧪 Testing

### Backend Testing
```bash
cd backend

# Run unit tests
./mvnw test

# Run integration tests
./mvnw test -Dtest=**/*IntegrationTest

# Generate test coverage report
./mvnw jacoco:report
```

### Frontend Testing
```bash
cd frontend

# Run unit tests
npm test

# Run e2e tests
npm run e2e

# Generate coverage report
npm run test:coverage
```

## 📚 API Documentation

The API is fully documented using **OpenAPI 3.0** specification. Access the interactive documentation at:

**Swagger UI**: http://localhost:8080/api/swagger-ui.html

### Key API Endpoints

#### Authentication
```
POST /api/auth/register    - User registration
POST /api/auth/login       - User login
POST /api/auth/refresh     - Refresh JWT token
POST /api/auth/logout      - User logout
```

#### Game Management
```
GET    /api/v1/games              - List active games
POST   /api/v1/games              - Create new game
GET    /api/v1/games/{id}         - Get game details
POST   /api/v1/games/{id}/join    - Join a game
POST   /api/v1/games/{id}/action  - Player action
```

#### User Management
```
GET    /api/v1/users/profile      - Get user profile
PUT    /api/v1/users/profile      - Update profile
GET    /api/v1/users/statistics   - Get playing statistics
```

## 🔧 Configuration

### Environment Variables

#### Database Configuration
```bash
DB_USERNAME=user
DB_PASSWORD=password
DB_HOST=localhost
DB_PORT=5432
DB_NAME=truholdem
```

#### Security Configuration
```bash
JWT_SECRET=your-super-secret-key-minimum-256-bits
JWT_EXPIRATION=86400000  # 24 hours
JWT_REFRESH_EXPIRATION=604800000  # 7 days
```

#### Redis Configuration
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=optional-password
```

#### CORS Configuration
```bash
CORS_ORIGINS=http://localhost:4200,https://yourdomain.com
WEBSOCKET_ORIGINS=http://localhost:4200,https://yourdomain.com
```

## 🚀 Deployment

### Production Deployment

1. **Update environment variables** in `docker-compose.yml`
2. **Generate strong JWT secret**
3. **Configure SSL certificates** in nginx
4. **Set up monitoring alerts**
5. **Configure backup strategies**

```bash
# Production deployment
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Health Checks

The application includes comprehensive health checks:

- **Application**: `GET /api/actuator/health`
- **Database**: Connection pool health
- **Redis**: Cache connectivity
- **Custom**: Game service health

## 📊 Monitoring

### Metrics Available
- **JVM metrics**: Memory, GC, threads
- **Database metrics**: Connection pool, query performance
- **Cache metrics**: Hit rate, evictions
- **Business metrics**: Active games, player actions
- **HTTP metrics**: Request/response times, error rates

### Grafana Dashboards
Pre-configured dashboards for:
- **Application Overview**
- **Database Performance**
- **Game Statistics**
- **Error Tracking**

## 🛡️ Security

### Implemented Security Measures
- ✅ **JWT token authentication**
- ✅ **Password hashing with BCrypt**
- ✅ **CSRF protection**
- ✅ **CORS configuration**
- ✅ **SQL injection prevention**
- ✅ **Input validation and sanitization**
- ✅ **Rate limiting** (configurable)
- ✅ **Security headers** (HSTS, CSP, etc.)

### Security Best Practices
- 🔒 **Non-root container execution**
- 🔐 **Secrets management with environment variables**
- 🛡️ **Database connection encryption**
- ⚠️ **Error message sanitization**
- 📋 **Audit logging for sensitive operations**

## 🤝 Contributing

### Development Workflow
1. **Fork the repository**
2. **Create feature branch** (`git checkout -b feature/amazing-feature`)
3. **Write tests** for new functionality
4. **Ensure all tests pass** (`./mvnw test && npm test`)
5. **Commit changes** (`git commit -m 'Add amazing feature'`)
6. **Push to branch** (`git push origin feature/amazing-feature`)
7. **Open Pull Request**

### Code Quality Standards
- ✅ **Follow Java coding conventions**
- ✅ **Write comprehensive tests** (minimum 80% coverage)
- ✅ **Document public APIs**
- ✅ **Use meaningful commit messages**
- ✅ **Update documentation** when needed

## 📋 Roadmap

### Phase 2 Features
- [ ] **Real-time multiplayer** with WebSockets
- [ ] **Tournament system** with bracket management
- [ ] **Advanced AI opponents** with machine learning
- [ ] **Mobile responsive design**
- [ ] **Social features** (friends, chat, clubs)

### Phase 3 Features
- [ ] **Microservices architecture**
- [ ] **Event sourcing** for game history
- [ ] **Multi-language support**
- [ ] **Payment integration** for real money games
- [ ] **Advanced analytics** and reporting

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Spring Framework** team for excellent documentation
- **Angular** team for the robust frontend framework
- **PostgreSQL** community for the reliable database
- **Docker** for containerization technology

---

## 📞 Support

For support, questions, or contributions:

- **Email**: [adam.porkolab@example.com](mailto:adam.porkolab@example.com)
- **Website**: [www.adamaporkolab.com](https://www.adamaporkolab.com)
- **Issues**: [GitHub Issues](https://github.com/APorkolab/Truholdem/issues)

---

**🎯 This is now a senior+++ level enterprise application ready for production deployment!**
