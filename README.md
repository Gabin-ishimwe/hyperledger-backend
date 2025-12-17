# OpenLedger Java

A production-grade **Spring Boot 3.4+** backend for **OpenLedger** - a blockchain-based fintech platform for Rwanda's banking sector.

## 🎯 Core Objective

Build a production-grade backend that connects multiple financial institutions including Bank of Kigali, MTN MOMO, and Equity Bank through Hyperledger Fabric technology.

- **Philosophy:** "Pure Blockchain" – business data lives on Hyperledger Fabric
- **Dynamic Identity:** Identities (Certs/Keys) are fetched from an external API on-demand to connect to institution-specific peers
- **Mapping:** Programmatic transformation using `jsonata4java`

## 📂 Project Structure

This is a multi-module Maven project with the following structure:

```
openledger-java/
├── openledger-common/           # Shared blockchain & crypto utilities
├── openledger-integration/      # Data pull & transformation services
├── openledger-api/             # REST API for queries & transactions
└── pom.xml                     # Parent POM
```

### Module Descriptions

#### `openledger-common`
- **`CryptoClient`**: Fetches crypto materials from external API using Spring RestClient
- **`GatewayRegistry`**: Manages Fabric Gateway instances with dynamic identity fetching
- **`FabricIdentityHelper`**: Converts PEM strings to Fabric Identity/Signer objects
- **`JsonataTransformer`**: Programmatic data transformation utility

#### `openledger-integration`
- **`BankConnector`**: Calls external Bank APIs using RestClient
- **`BankMapper`**: Maps Bank JSON to Ledger POJOs using JSONata
- **`SyncScheduler`**: Periodically triggers pull/map/ledger-write flow

#### `openledger-api`
- **`BankController`**: Exposes REST endpoints (e.g., `/api/v1/bk/transactions`)
- **`BankService`**: Obtains specific Contract from GatewayRegistry to query ledger
- **Global Exception Handling**: Comprehensive error handling for ledger and mapping errors

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker (for Hyperledger Fabric)
- Access to crypto materials API

### Environment Variables

```bash
# Crypto Service
CRYPTO_SERVICE_URL=http://localhost:8081

# Bank of Kigali Configuration
BK_ORG_ID=BankOfKigaliMSP
BK_USER_ID=admin
BK_PEER_ID=peer0.bk.openledger.com
BK_PEER_ENDPOINT=grpcs://peer0.bk.openledger.com:7051
BK_CHANNEL=openledger-channel
BK_CHAINCODE=openledger
BK_API_URL=https://api.bk.rw
BK_API_KEY=your-api-key

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local
```

### Build & Run

```bash
# Clone the repository
git clone <repository-url>
cd openledger-java

# Build all modules
mvn clean compile

# Run the API
cd openledger-api
mvn spring-boot:run

# Or build and run with Docker
mvn clean package
java -jar openledger-api/target/openledger-api-1.0.0-SNAPSHOT.jar
```

### API Documentation

Once running, access the API documentation at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Docs: http://localhost:8080/api-docs

## 🏗 Architecture

### Dynamic Identity Management

```java
// Crypto materials are fetched dynamically
CryptoMaterials materials = cryptoClient.fetchCrypto(org, user, peer);

// Gateway is created with fetched identity
Gateway gateway = Gateway.newInstance()
    .identity(identityHelper.createIdentity(materials.certificate(), materials.mspId()))
    .signer(identityHelper.createSigner(materials.privateKey()))
    .connection(peerEndpoint)
    .connect();
```

### Data Transformation

```java
// JSONata transformations are applied programmatically
String bankJson = bankConnector.fetchTransactions(fromDate, toDate, limit);
String ledgerJson = bankMapper.mapTransactions(bankJson);

// Write to blockchain
contract.submitTransaction("writeData", org, "transactions", ledgerJson, timestamp);
```

## 📋 API Endpoints

### Bank of Kigali (`/api/v1/bk`)

- `GET /transactions` - Get transactions with date filtering
- `GET /transactions/{id}` - Get specific transaction
- `GET /balances` - Get account balances
- `GET /customers/{id}` - Get customer information
- `POST /transactions` - Create new transaction
- `GET /accounts/{id}/transactions` - Get account transaction history
- `GET /status` - Check network status

## 🔧 Configuration

### Institution Mapping

Configure institution-to-peer mappings in `application.yml`:

```yaml
institutions:
  bk:
    org: BankOfKigaliMSP
    peer:
      endpoint: grpcs://peer0.bk.openledger.com:7051
    channel: openledger-channel
    chaincode: openledger
```

### Synchronization Schedule

```yaml
sync:
  bk:
    transactions:
      interval: 300000  # 5 minutes
    balances:
      interval: 900000  # 15 minutes
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific module tests
cd openledger-common && mvn test
cd openledger-integration && mvn test
cd openledger-api && mvn test
```

## 📊 Monitoring

The application includes Spring Boot Actuator endpoints:

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info: `/actuator/info`

## 🚀 Deployment

### Docker

```bash
# Build Docker image
mvn clean package
docker build -t openledger-api:latest openledger-api/

# Run with environment variables
docker run -p 8080:8080 \
  -e CRYPTO_SERVICE_URL=http://crypto-service:8081 \
  -e BK_PEER_ENDPOINT=grpcs://peer0.bk.openledger.com:7051 \
  openledger-api:latest
```

### Kubernetes

Deploy to Kubernetes using the provided manifests (coming soon).

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the GitHub repository
- Contact the development team
- Check the documentation at `/swagger-ui.html`
