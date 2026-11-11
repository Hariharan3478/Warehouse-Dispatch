# Warehouse Dispatch Management Application

A robust, production-grade Spring Boot backend service designed for automated warehouse dispatch management, route/carrier selection, freight calculation, and in-memory persistence.

---

## Tech Stack & Architecture

- **Java Version**: JDK 21
- **Framework**: Spring Boot 3.3.5
- **Database**: H2 In-Memory Database (with JPA/Hibernate)
- **Build & Dependency Management**: Apache Maven (`mvnw`)
- **Testing & Quality**: JUnit 5, MockMvc, JaCoCo (Code Coverage Analysis)

---

## Core Components

- **`DispatchController`**: Exposes REST endpoints for dispatch consolidation and order retrieval.
- **`DispatchService`**: Contains core business logic, carrier selection algorithms, and freight calculation engines.
- **`ReferenceDataLoader`**: Bootstraps reference data (such as carrier rates, zones, and FX rates) upon application startup.
- **`DispatchPersistenceWriter`**: Handles transactional writing and persistence of batches, loads, and orders into the H2 database.

---

## Getting Started & Running the Application

### Prerequisites
- Ensure you have **JDK 21** installed on your machine.

### 1. Clone & Build the Project
Open your terminal in the project root directory and run the clean build:
```bash
./mvnw clean install
```

### 2. Run the Application
Start the Spring Boot server locally:
```bash
./mvnw spring-boot:run
```
The application will launch on `http://localhost:8080`.

---

## Running Tests & Generating Coverage Reports

### Run All Automated Tests
To execute the unit, utility, and end-to-end integration test suite:
```bash
./mvnw test
```

### Generate JaCoCo Code Coverage Report
To generate the visual HTML code coverage report:
```bash
./mvnw test jacoco:report
```
Once complete, open the generated report in your browser:
- **Path**: `target/site/jacoco/index.html`

---

## API Documentation & Usage

### Base URL
`http://localhost:8080/api/v2/dispatch`

### 1. Consolidate a Dispatch Batch (POST)
- **Endpoint**: `POST /api/v2/dispatch/consolidations`
- **Required Headers**:
  - `Content-Type: application/json`
  - `X-Warehouse-Key: WH-CHN-01`
- **Sample Request Body**:
  ```json
  {
    "batchId": "DSP-20261106-0001",
    "dispatchDate": "2026-11-06",
    "requestedAt": "2026-11-06T10:00:00+05:30",
    "warehouse": { "code": "WH-CHN-01", "cutoffTime": "16:00" },
    "orders": [
      {
        "orderNo": "SO-101",
        "customer": { "code": "C1", "tier": "GOLD" },
        "deliveryAddress": { "line1": "Street", "city": "Bengaluru", "state": "KA", "pincode": "560001" },
        "serviceLevel": "STANDARD",
        "declaredValue": { "amount": 1000, "currency": "INR" },
        "packages": [
          {
            "packageId": "P1",
            "dimensions": { "length": 10, "width": 10, "height": 10, "unit": "CM" },
            "weight": { "value": 1, "unit": "KG" },
            "fragile": false,
            "hazmat": false
          }
        ]
      }
    ]
  }
  ```

### 2. Get Order Details (GET)
- **Endpoint**: `GET /api/v2/dispatch/orders/{orderNo}`
- **Example**: `GET http://localhost:8080/api/v2/dispatch/orders/SO-101`

---

## Database Access (H2 Console)

You can inspect the live H2 database while the application is running:
- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:dispatchdb`
- **User Name**: `sa`
- **Password**: *(leave blank)*
