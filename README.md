# WireMock Integration Study Case
## Overview
This project serves as a comprehensive study case demonstrating the effective implementation of **WireMock** for mocking external service interactions within integration tests for Kotlin services. Developed with the aid of generative AI tools for test code generation, this repository aims to provide a practical reference implementation for future service development requiring robust service virtualization strategies.

## Application Architecture
The application is a client-side solution designed for interacting with external JSON-based Product APIs. Its modular design utilizes established libraries for HTTP communication and data serialization:

- `Product.kt`: Defines the immutable domain model for a single product entity.

- `ProductBatch.kt`: Represents a collection of `Product` entities for bulk operations.

- `HttpClient.kt`: A singleton OkHttp wrapper providing foundational synchronous HTTP GET capabilities with robust error handling.

- `JsonApiClient.kt`: A higher-level API client built on `HttpClient`, specializing in JSON payloads (fetching products, sending product batches, and orchestrating combined workflows).

## Testing Strategy with WireMock
The project employs a comprehensive testing strategy using **JUnit 5** for both unit and integration tests. WireMock is extensively utilized as a service virtualization tool to direct test-time HTTP requests to a local mock server, bypassing live external APIs.

### WireMock Integration Details
- **Setup**: The `@Rule` annotation with `WireMockRule` automatically manages the WireMock server's lifecycle for each test, utilizing `dynamicPort()` to prevent port conflicts. The `@Before` annotated `setup()` method ensures a fresh `JsonApiClient` instance for test isolation.

- **Operational Mechanism**:

  - **Stubbing:** `stubFor()` defines expected HTTP requests and the precise HTTP responses WireMock should return.

  - **Client Execution**: The application code makes HTTP calls directed to the WireMock server.

  - **Verification**: `verify()` asserts that WireMock received the expected HTTP requests, including detailed checks on URL, method, headers, and request body content using JSON matchers.

## WireMock Benefits & Limitations
- **Advantages**:

  - **Deterministic Testing**: Eliminates external service variability for repeatable and reliable tests.

  - **Isolation**: Decouples tests from external API availability, latency, and rate limits.

  - **Controlled Error Scenarios**: Facilitates testing of difficult-to-reproduce error conditions (e.g., specific HTTP errors, timeouts).

  - **Accelerated Feedback Loop**: Speeds up test execution by removing real network I/O.

  - **Offline Development**: Enables development and testing without internet connectivity.

- **Disadvantages**:

  - **Maintenance Overhead**: Mocks require ongoing synchronization with evolving real API specifications.

  - **Risk of Divergence**: Mocks can become outdated, leading to false positives if not diligently maintained.

  - **Complexity**: Advanced response logic or stateful mocks can increase test setup complexity.

  - **Learning Curve**: Requires understanding WireMock's DSL and configuration.

## Implementation Tips & Potential Pitfalls
- **Gradle Dependencies**: Ensure correct WireMock dependencies (`com.github.tomakehurst.wiremock:wiremock-junit`) are used to leverage JUnit integration features like `WireMockRule`. Versioning is critical; verify the existence of specific artifact versions on Maven Central.

- **Explicit Stubbing**: Prefer explicit stub definitions for specific "bad" requests (e.g., a stub returning 404 for a malformed JSON body) over relying solely on WireMock's default unmatched request behavior. This enhances test clarity and realism.

- **AI Assistance**: Generative AI can significantly accelerate the creation of mock stubs, assertions, and boilerplate test code. However, it is crucial to review and validate AI-generated code for correctness, adherence to best practices, and alignment with specific test objectives. Challenges may arise with initial setup complexities or subtle dependency versioning issues that AI might not immediately resolve.

## Project Reports
This project generates several key reports to monitor code quality, test coverage, and documentation:

- **JaCoCo Report (Code Coverage)**: Quantifies the proportion of application code exercised by tests.

  - **Location**: `build/reports/jacoco/test/html/index.html`
    - **Command to generate**: `./gradlew clean test jacocoTestReport`

- **Dokka Report (API Documentation)**: Generates browsable API documentation from KDoc comments for both main and test source code.

  - **Location**: `build/dokka/html/index.html`
    - Command to generate: `./gradlew clean dokkaHtml`   

- **JUnit Test Report (Test Results)**: Summarizes JUnit test execution outcomes, including pass/fail status and failure details.

  - **Location**: `build/reports/tests/test/index.html`
    - **Command to generate**: `./gradlew clean test`

These reports collectively provide a robust feedback mechanism for development, contributing to enhanced code quality, improved maintainability, and greater clarity across the project.