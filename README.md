# OpenLedger Backend

A blockchain-based financial platform for Rwanda's banking sector, built with Spring Boot and Hyperledger Fabric.

## Overview

OpenLedger is a unified backend service that connects multiple financial institutions (banks, mobile money providers) through a shared Hyperledger Fabric blockchain network. It provides:

- **Blockchain Integration**: Hyperledger Fabric Gateway SDK for ledger operations
- **Multi-Institution Support**: Modular architecture for Bank of Kigali, MTN, Equity Bank, and more
- **Data Transformation**: JSONata-based mapping between institution formats and ledger schema
- **REST API**: OpenAPI-documented endpoints for querying and submitting transactions
- **Scheduled Sync**: Automated data synchronization from institution APIs to blockchain

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 20 | Programming Language |
| Spring Boot | 3.4.0 | Application Framework |
| Hyperledger Fabric Gateway | 1.10.0 | Blockchain Integration |
| gRPC | 1.76.0 | Communication Protocol |
| JSONata4Java | 2.6.1 | Data Transformation |
| Springdoc OpenAPI | 2.3.0 | API Documentation |
| Maven | 3.8+ | Build Tool |

## Project Structure

```
openledger-java/
├── pom.xml                                 # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/openledger/
│   │   │   ├── OpenLedgerApplication.java  # Main entry point
│   │   │   │
│   │   │   ├── api/                        # API Layer
│   │   │   │   ├── controller/             # REST controllers
│   │   │   │   ├── exception/              # Global exception handling
│   │   │   │   └── security/               # JWT & API Key auth (TODO)
│   │   │   │
│   │   │   ├── institutions/               # Institution Modules
│   │   │   │   ├── common/                 # Base interfaces
│   │   │   │   ├── bk/                     # Bank of Kigali
│   │   │   │   │   ├── service/            # Business logic
│   │   │   │   │   ├── connector/          # External API client
│   │   │   │   │   └── mapper/             # Data transformation
│   │   │   │   ├── mtn/                    # MTN Mobile Money (TODO)
│   │   │   │   └── equity/                 # Equity Bank (TODO)
│   │   │   │
│   │   │   ├── blockchain/                 # Blockchain Layer
│   │   │   │   ├── client/                 # Fabric Gateway management
│   │   │   │   ├── service/                # Ledger operations
│   │   │   │   └── dto/                    # Blockchain data models
│   │   │   │
│   │   │   └── core/                       # Core Utilities
│   │   │       ├── config/                 # Spring configuration
│   │   │       ├── util/                   # Helper classes
│   │   │       └── factory/                # Institution routing
│   │   │
│   │   └── resources/
│   │       ├── application.yml             # Application configuration
│   │       └── fabric/                     # Fabric connection profiles
│   │
│   └── test/                               # Test classes
│       ├── java/com/openledger/
│       │   ├── unit/                       # Unit tests
│       │   └── integration/                # Integration tests
│       └── resources/
│           └── application-test.yml        # Test configuration
│
├── infra/                                  # Infrastructure
│   ├── docker/
│   │   ├── Dockerfile                      # Multi-stage build
│   │   └── docker-compose.yml              # Local development
│   └── caddy/
│       └── Caddyfile                       # API Gateway config
│
└── scripts/                                # Deployment utilities
```

## Getting Started

### Prerequisites

- Java 20 or higher
- Maven 3.8+
- Docker & Docker Compose (for local development)
- Access to a Hyperledger Fabric network
- Crypto service for identity management

### Configuration

Set the following environment variables or update `application.yml`:

```bash
# Crypto Service
export CRYPTO_SERVICE_URL=http://localhost:8080

# Fabric Network
export FABRIC_DEFAULT_CHANNEL=openledger-channel

# Bank of Kigali
export BK_ORG_ID=BankOfKigaliMSP
export BK_PEER_ENDPOINT=grpcs://peer0.bk.openledger.com:7051
export BK_API_URL=https://api.bk.rw
export BK_API_KEY=your-api-key
```

### Build

```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Package as JAR
mvn package -DskipTests

# Run the application
java -jar target/openledger-backend-1.0.0-SNAPSHOT.jar
```

### Docker

```bash
# Build and run with Docker Compose
cd infra/docker
docker-compose up --build

# Or build image directly
docker build -t openledger-backend -f infra/docker/Dockerfile .
```

## API Endpoints

Once running, access the API documentation at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

### Bank of Kigali Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/bk/transactions` | Get transactions |
| GET | `/api/v1/bk/transactions/{id}` | Get transaction by ID |
| POST | `/api/v1/bk/transactions` | Create transaction |
| GET | `/api/v1/bk/balances` | Get account balances |
| GET | `/api/v1/bk/customers/{id}` | Get customer info |
| GET | `/api/v1/bk/status` | Get network status |

### Health & Metrics

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Metrics |
| `/actuator/prometheus` | Prometheus metrics |

## Architecture

### Blockchain Integration

The application uses the Hyperledger Fabric Gateway SDK to interact with the blockchain:

1. **CryptoClient**: Fetches identity materials from external crypto service
2. **FabricIdentityHelper**: Converts PEM certificates to Fabric Identity objects
3. **FabricGatewayService**: Manages Gateway connections with caching and timeouts

### Data Flow

```
Institution API → Connector → Mapper → Ledger Service → Blockchain
                     ↓
              JSONata Transform
                     ↓
              Standard Schema
```

### Institution Modules

Each institution follows the same pattern:
- `connector/`: External API client
- `mapper/`: JSONata-based data transformation
- `service/`: Business logic and ledger operations
- `dto/`: Request/response objects
- `entity/`: Operational entities (sync jobs, etc.)

## Configuration Profiles

| Profile | Description |
|---------|-------------|
| `local` | Local development with debug logging |
| `docker` | Docker environment |
| `production` | Production with minimal logging |
| `test` | Test configuration with disabled schedulers |

## Adding a New Institution

1. Create package under `institutions/{institution-id}/`
2. Implement `InstitutionService` interface
3. Create connector, mapper, and service classes
4. Add configuration to `application.yml`
5. Register controller endpoints

Example structure:
```
institutions/mtn/
├── connector/MtnConnector.java
├── mapper/MtnMapper.java
├── service/MtnService.java
├── dto/
└── entity/
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=FabricGatewayServiceIntegrationTest

# Run with specific profile
mvn test -Dspring.profiles.active=test
```

**Note**: Integration tests require a running Fabric network and crypto service.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | Server port |
| `SPRING_PROFILES_ACTIVE` | local | Active profile |
| `CRYPTO_SERVICE_URL` | http://localhost:8080 | Crypto service URL |
| `FABRIC_DEFAULT_CHANNEL` | openledger-channel | Default channel |
| `BK_ORG_ID` | BankOfKigaliMSP | BK organization MSP ID |
| `BK_API_URL` | https://api.bk.rw | BK API endpoint |
| `BK_API_KEY` | - | BK API key |
| `SYNC_ENABLED` | true | Enable/disable sync scheduler |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-institution`)
3. Commit changes (`git commit -m 'Add new institution support'`)
4. Push to branch (`git push origin feature/new-institution`)
5. Open a Pull Request

## License

MIT License - see [LICENSE](LICENSE) for details.

## Support

For issues and feature requests, please open an issue on GitHub.
