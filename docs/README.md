# Custom Index Basket Management Platform - Documentation

## Overview

This documentation covers the architecture, implementation, and operational aspects of the Custom Index Basket Management Platform - a comprehensive system for creating, backtesting, approving, and managing thematic investment baskets with real-time pricing and vendor integration.

## Platform Capabilities

### Core Features
- **Thematic Basket Creation**: Create custom investment baskets with global equities and indices
- **Historical Backtesting**: Test basket performance against major market indices (up to 15 years)
- **Approval Workflow**: Configurable single or dual approver workflow with full audit trail
- **Vendor Publishing**: Publish baskets to Bloomberg (BSYM/BLPAPI) and Refinitiv (RDP/Elektron)
- **Real-time Pricing**: Sub-5-second basket valuation with real-time market data
- **High Availability**: Kubernetes/Docker deployment with cloud-agnostic architecture

### Key SLA Requirements
- **Approval to Live**: ≤ 2 minutes from approval to first published price
- **Real-time Pricing**: 5-second cadence for basket price updates
- **Backtesting Performance**: 15-year analysis in ≤ 5 minutes
- **High Availability**: 99.9% uptime with automatic failover

## Documentation Structure

### 📐 Architecture Documentation
- **[Basket State Machine](architecture/basket-state-machine.md)**: Complete workflow state management
- **[Service Architecture](architecture/service-architecture.md)**: Microservices design with abstraction layers
- **[Messaging Abstraction Design](architecture/messaging-abstraction-design.md)**: Technology-agnostic messaging and caching patterns
- **[Data Architecture](architecture/data-architecture-timescale.md)**: Unified TimescaleDB with abstraction integration
- **[Testing & Simulation Architecture](architecture/testing-simulation-architecture.md)**: Proxy services for end-to-end testing
- **[Legacy Data Architecture](architecture/data-architecture.md)**: Original InfluxDB design (deprecated)
- **[Vendor Integration](architecture/vendor-integration.md)**: Bloomberg and Refinitiv API patterns
- **[Security Architecture](architecture/security-architecture.md)**: Authentication, authorization, and data protection

### 📋 Service Contracts
- **[Service Contracts Overview](contracts/README.md)**: Complete contract documentation guide
- **[REST API Contracts](contracts/service-contracts.md)**: Input/output specifications for all services
- **[Event Contracts](contracts/event-contracts.md)**: Asynchronous messaging specifications
- **[Actor & gRPC Contracts](contracts/actor-grpc-contracts.md)**: Akka Typed actors and gRPC internal APIs
- **[Hybrid Communication Strategy](contracts/hybrid-communication-strategy.md)**: Smart protocol selection framework
- **[Validation Contracts](contracts/validation-contracts.md)**: Validation rules and error handling patterns

### 🛠️ Implementation Guides
- **[State Machine Code Templates](implementation/state-machine-code-templates.md)**: Ready-to-use code templates
- **[TimescaleDB Implementation](implementation/timescaledb-implementation.md)**: Complete TimescaleDB setup and usage guide
- **[Proxy Services Implementation](implementation/proxy-services-implementation.md)**: External service simulators for testing
- **[Reactive Programming Patterns](implementation/reactive-patterns.md)**: WebFlux and reactive stream implementations
- **[Database Schemas](implementation/database-schemas.md)**: Complete database design and migrations
- **[API Specifications](implementation/api-specifications.md)**: OpenAPI/Swagger documentation
- **[Testing Strategies](implementation/testing-strategies.md)**: Unit, integration, and performance testing

### 🚀 Deployment & Operations
- **[Kubernetes Deployment](deployment/kubernetes-deployment.md)**: Container orchestration and scaling
- **[Monitoring & Observability](deployment/monitoring.md)**: Metrics, logging, and alerting
- **[Disaster Recovery](deployment/disaster-recovery.md)**: Backup and recovery procedures
- **[Performance Tuning](deployment/performance-tuning.md)**: Optimization strategies

### 📊 Business Documentation
- **[Workflow Diagrams](business/workflow-diagrams.md)**: End-to-end process flows
- **[User Guides](business/user-guides.md)**: Step-by-step operational procedures
- **[Compliance](business/compliance.md)**: Regulatory and audit requirements
- **[SLA Metrics](business/sla-metrics.md)**: Key performance indicators

## Quick Start

### Prerequisites
- Docker & Kubernetes
- Java 21+
- PostgreSQL 15+ with TimescaleDB extension
- **Messaging Layer**: Solace PubSub+ (primary) or Apache Kafka 3.0+ (alternative)
- **Caching Layer**: SolCache (primary) or Redis 7+ (alternative)

### Core Services
1. **Basket Core Service**: Lifecycle management and approval workflows
2. **Market Data Service**: Historical data (TimescaleDB) + Real-time data (abstracted messaging/caching)
3. **Publishing Service**: Dual publishing (listing + pricing) to vendors
4. **Analytics Service**: Backtesting and performance analysis

### Development Setup
```bash
# Clone and setup
git clone <repository>
cd custom-index-basket-management

# Start infrastructure (choose profile: solace or kafka)
docker-compose --profile solace up -d postgres-timescale solace solcache
# OR for development/alternative setup:
# docker-compose --profile kafka up -d postgres-timescale redis kafka

# Build services
mvn clean install

# Deploy to local Kubernetes
kubectl apply -f k8s/
```

## Architecture Highlights

### State-Driven Workflow
The platform uses a comprehensive state machine to manage basket lifecycle:
```
DRAFT → BACKTESTING → BACKTESTED → PENDING_APPROVAL → APPROVED → 
LISTING → LISTED → ACTIVE (with real-time pricing)
```

### Unified Data Architecture with Technology Abstraction
- **Historical Data**: TimescaleDB hypertables for 15+ years of market data (backtesting)
- **Real-time Data**: Abstracted messaging layer (Solace PubSub+/Kafka) for live pricing (5-second updates)
- **Caching Layer**: Abstracted cache (SolCache/Redis) for high-performance data access
- **Application Data**: PostgreSQL tables for baskets, approvals, and business logic
- **Technology Flexibility**: Configuration-driven switching between Solace and Kafka/Redis implementations

### Event-Driven Design
- **CQRS Pattern**: Separate command and query responsibilities
- **Event Sourcing**: Complete audit trail for compliance
- **Async Processing**: Non-blocking operations for performance

### Vendor Abstraction
- **Bloomberg Integration**: BSYM (listing) + BLPAPI (pricing)
- **Refinitiv Integration**: RDP (listing) + Elektron (pricing)
- **Circuit Breaker**: Automatic failover and retry mechanisms

## Key Technical Decisions

### Service Consolidation
Reduced from 9+ microservices to 4 core services:
- **Performance**: 20-second SLA improvement
- **Complexity**: 55% reduction in inter-service communication
- **Operational**: Simplified deployment and monitoring

### Reactive Programming with Abstraction
- **Spring WebFlux**: Non-blocking I/O for high throughput
- **R2DBC**: Reactive database connectivity
- **Abstracted Stream Processing**: Solace Event Streams or Kafka Streams
- **Technology-Agnostic Business Logic**: Implementation-independent reactive patterns

### Cloud-Native Design
- **Kubernetes-Native**: Operator patterns and custom resources
- **Service Mesh**: Istio for traffic management and security
- **Observability**: Prometheus, Grafana, Jaeger integration

## Performance Characteristics

### Throughput Targets
- **Basket Creation**: 1,000+ concurrent users
- **Market Data Processing**: 10,000+ price updates/second
- **Publishing Operations**: 100+ baskets/minute
- **Backtest Processing**: 15-year analysis in <5 minutes

### Latency Targets
- **Real-time Pricing**: <2 seconds end-to-end
- **Approval Workflow**: <2 minutes approval to live
- **API Response**: <100ms for standard operations
- **Database Queries**: <50ms for typical queries

## Security & Compliance

### Authentication & Authorization
- **OAuth 2.0 + JWT**: Token-based authentication
- **Role-Based Access**: Fine-grained permissions
- **mTLS**: Service-to-service security

### Data Protection
- **Encryption**: AES-256 at rest, TLS 1.3 in transit
- **Key Management**: HashiCorp Vault integration
- **Audit Logging**: Immutable audit trail

### Regulatory Compliance
- **SOX Compliance**: Financial controls and audit trails
- **GDPR**: Data privacy and protection
- **SOC 2**: Security and availability controls

## Monitoring & Alerting

### Business Metrics
- Basket lifecycle progression rates
- Approval workflow SLA compliance
- Publishing success rates
- Real-time pricing accuracy

### Technical Metrics
- Service response times (p95, p99)
- Error rates by service and operation
- Database performance and connection pools
- Kafka consumer lag and throughput

### Alerting Rules
- SLA violations (>2 minute approval to live)
- Service failures and error spikes
- Database performance degradation
- Vendor API failures or rate limiting

## Contributing

### Development Standards
- **Code Style**: Google Java Style Guide
- **Testing**: 80%+ test coverage required
- **Documentation**: API-first development with OpenAPI
- **Version Control**: GitFlow with feature branches

### Review Process
- **Architecture Reviews**: For significant changes
- **Security Reviews**: For authentication/authorization changes
- **Performance Reviews**: For high-load components
- **Business Reviews**: For workflow modifications

## Support & Maintenance

### Production Support
- **24/7 Monitoring**: Automated alerting and escalation
- **Incident Response**: Defined runbooks and procedures
- **Change Management**: Controlled deployment processes
- **Capacity Planning**: Proactive scaling and optimization

### Maintenance Windows
- **Planned Maintenance**: Monthly maintenance windows
- **Security Patches**: Emergency patching procedures
- **Version Upgrades**: Quarterly major version updates
- **Data Retention**: Automated archival and cleanup

---

## Contact Information

- **Architecture Team**: architecture@company.com
- **DevOps Team**: devops@company.com
- **Business Analysts**: business@company.com
- **Support Team**: support@company.com

For detailed technical questions, please refer to the specific documentation sections or contact the appropriate team.
