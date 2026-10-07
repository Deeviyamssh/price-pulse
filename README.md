# PricePulse

PricePulse is a product price monitoring platform that tracks product prices over time and sends alerts when prices drop below specified thresholds.

## Tech Stack

### Backend
- **Java 21** with Spring Boot 3.3.4
- **Spring Security** with JWT authentication
- **Spring Data JPA** with PostgreSQL
- **Flyway** for database migrations
- **Spring Boot Actuator** for health checks
- **Testcontainers** for integration testing
- **jqwik** for property-based testing

### Frontend
- **React 18** with TypeScript
- **Vite** for build tooling
- **React Router** for routing
- **TanStack Query** for data fetching
- **Recharts** for data visualization
- **Axios** for HTTP requests
- **Vitest** for testing

### Infrastructure
- **Docker** and Docker Compose for containerization
- **PostgreSQL 16** for database
- **Mailhog** for email testing (development only)
- **Nginx** for serving the frontend

## Prerequisites

- Docker and Docker Compose
- Node.js 22+ (for local frontend development)
- Java 21+ (for local backend development)
- Maven 3.9+ (for local backend development)

## Quick Start with Docker

The easiest way to run PricePulse is using Docker Compose:

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd price-pulse
   ```

2. **Create environment file**
   ```bash
   cp .env.example .env
   ```

3. **Edit .env with your values**
   ```bash
   # Minimum required configuration
   POSTGRES_PASSWORD=your-secure-password
   JWT_SECRET=your-jwt-secret-at-least-32-characters-long
   SCHEDULER_INTERVAL_MS=1800000
   ```

4. **Start the application**
   ```bash
   docker-compose up --build
   ```

5. **Access the application**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - Mailhog (email testing): http://localhost:8025
   - Health check: http://localhost:8080/actuator/health

## Local Development

### Backend Development

1. **Navigate to backend directory**
   ```bash
   cd backend
   ```

2. **Set up PostgreSQL** (ensure you have PostgreSQL running locally)
   ```bash
   # Create database
   createdb pricepulse
   ```

3. **Configure environment**
   ```bash
   # Create application-local.yml in src/main/resources/
   # Or set environment variables
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pricepulse
   export SPRING_DATASOURCE_USERNAME=your_postgres_user
   export SPRING_DATASOURCE_PASSWORD=your_postgres_password
   export JWT_SECRET=your-jwt-secret
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

5. **Run tests**
   ```bash
   mvn test
   ```

### Frontend Development

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start development server**
   ```bash
   npm run dev
   ```

4. **Run tests**
   ```bash
   npm run test
   ```

5. **Build for production**
   ```bash
   npm run build
   ```

## Environment Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `POSTGRES_PASSWORD` | PostgreSQL database password | `secure-password-123` |
| `JWT_SECRET` | Secret key for JWT token signing (min 32 chars) | `your-super-secret-jwt-key-at-least-32-chars` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SCHEDULER_INTERVAL_MS` | Price check interval in milliseconds | `1800000` (30 minutes) |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://postgres:5432/pricepulse` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | `pricepulse` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | `POSTGRES_PASSWORD` value |
| `SPRING_MAIL_HOST` | SMTP server host | `mailhog` (dev) |
| `SPRING_MAIL_PORT` | SMTP server port | `1025` (dev) |
| `SPRING_MAIL_USERNAME` | SMTP username | Empty string |
| `SPRING_MAIL_PASSWORD` | SMTP password | Empty string |

**Note**: Email notifications are enabled by default. For production, configure a real SMTP service (e.g., SendGrid) to send price alert emails. See [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md) for details.

## Docker Configuration

### Development (docker-compose.yml)

The default Docker Compose configuration includes:
- **PostgreSQL**: Database with persistent volume
- **Mailhog**: Email testing service
- **Backend**: Spring Boot application
- **Frontend**: Nginx serving React build

### Production (docker-compose.prod.yml)

For production deployment, use the production variant:
```bash
docker-compose -f docker-compose.prod.yml up --build
```

The production configuration:
- Excludes Mailhog (use real SMTP)
- Configures secure cookies
- Optimized for production performance

## Project Structure

```
price-pulse/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/pricepulse/
│   │   │   │   ├── alert/          # Price alert management
│   │   │   │   ├── auth/           # Authentication & authorization
│   │   │   │   ├── common/         # Shared utilities
│   │   │   │   ├── notification/   # Email notifications
│   │   │   │   ├── pricing/        # Price checking logic
│   │   │   │   ├── product/        # Product management
│   │   │   │   └── scheduler/      # Scheduled price checks
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-docker.yml
│   │   │       ├── application-local.yml
│   │   │       └── db/migration/   # Flyway migrations
│   │   └── test/                   # Unit & integration tests
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/                    # API client
│   │   ├── auth/                   # Authentication context
│   │   ├── components/             # React components
│   │   ├── pages/                  # Page components
│   │   ├── styles/                 # Global styles
│   │   └── utils/                  # Utility functions
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── vite.config.ts
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env.example
└── README.md
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/logout` - Logout user

### Products
- `GET /api/products` - List user's products
- `POST /api/products` - Add new product
- `GET /api/products/{id}` - Get product details
- `DELETE /api/products/{id}` - Delete product

### Price Records
- `GET /api/products/{id}/price-records` - Get price history

### Alerts
- `POST /api/products/{id}/alerts` - Set price alert
- `DELETE /api/products/{id}/alerts` - Remove alert

## Database Migrations

Database migrations are managed by Flyway. New migrations should be added to `backend/src/main/resources/db/migration/` with the naming convention `V{version}__{description}.sql`.

## Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm run test
```

## Deployment

### Docker Registry

Build and push images to a container registry:

```bash
# Build backend image
docker build -t your-registry/price-pulse-backend:latest ./backend

# Build frontend image
docker build -t your-registry/price-pulse-frontend:latest ./frontend

# Push images
docker push your-registry/price-pulse-backend:latest
docker push your-registry/price-pulse-frontend:latest
```

### Production Deployment

1. **Set up production environment variables**
   ```bash
   # Copy and edit production env file
   cp .env.example .env.prod
   # Update with production values
   ```

2. **Deploy with production compose**
   ```bash
   docker-compose -f docker-compose.prod.yml -f docker-compose.override.yml up -d
   ```

3. **Configure reverse proxy** (optional)
   - Use Nginx, Traefik, or similar for SSL termination
   - Configure domain names and HTTPS

## Security Considerations

- Always use strong, unique passwords for PostgreSQL
- Use a strong, randomly generated JWT_SECRET in production
- Enable HTTPS in production
- Configure proper CORS settings for your domain
- Regularly update dependencies for security patches
- Never commit `.env` files to version control

## Troubleshooting

### Database Connection Issues
- Ensure PostgreSQL is running: `docker-compose ps postgres`
- Check logs: `docker-compose logs postgres`
- Verify environment variables in `.env`

### Build Failures
- Clear Docker cache: `docker system prune -a`
- Rebuild without cache: `docker-compose build --no-cache`

### Frontend Not Loading
- Check backend health: `curl http://localhost:8080/actuator/health`
- Verify nginx configuration in `frontend/nginx.conf`
- Check browser console for errors


