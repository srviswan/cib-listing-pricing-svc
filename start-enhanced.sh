#!/bin/bash

echo "🚀 Starting CIB Listing & Pricing Service - ENHANCED MODE"
echo "========================================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose is not installed. Please install it first."
    exit 1
fi

echo "📋 Starting enhanced services (Database + Core Services + Integration Services)..."
echo ""

# Start enhanced services
docker-compose -f docker-compose.enhanced.yml up -d

# Wait for services to start
echo "⏳ Waiting for services to start..."
sleep 15

# Check service status
echo ""
echo "📊 Service Status:"
docker-compose -f docker-compose.enhanced.yml ps

echo ""
echo "🔍 Health Checks:"

# Check database health
if docker exec -it basket-timescaledb pg_isready -U basket_user -d basket_platform > /dev/null 2>&1; then
    echo "✅ Database: HEALTHY"
else
    echo "❌ Database: UNHEALTHY"
fi

# Check Redis health
if docker exec -it basket-redis redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis: HEALTHY"
else
    echo "❌ Redis: UNHEALTHY"
fi

# Check existing services
services=(
    "8081:Basket Core Service"
    "8082:Market Data Service"
    "8083:Publishing Service"
)

for service in "${services[@]}"; do
    port=$(echo $service | cut -d: -f1)
    name=$(echo $service | cut -d: -f2)
    
    if curl -s http://localhost:$port/actuator/health > /dev/null; then
        echo "✅ $name: HEALTHY"
    else
        echo "❌ $name: UNHEALTHY"
    fi
done

# Check new integration services
integration_services=(
    "8084:SMA Adapter Service"
    "8085:FIX Adapter Service"
    "8086:Integration Manager Service"
)

for service in "${integration_services[@]}"; do
    port=$(echo $service | cut -d: -f1)
    name=$(echo $service | cut -d: -f2)
    
    if curl -s http://localhost:$port/actuator/health > /dev/null; then
        echo "✅ $name: HEALTHY"
    else
        echo "❌ $name: UNHEALTHY"
    fi
done

echo ""
echo "🎯 ENHANCED MODE STARTED SUCCESSFULLY!"
echo ""
echo "📋 Available Services:"
echo "   • Database: localhost:5432"
echo "   • Redis: localhost:6379"
echo "   • Kafka: localhost:9092"
echo ""
echo "   • Basket Core Service: localhost:8081 (Basket lifecycle management)"
echo "   • Market Data Service: localhost:8082 (Market data & analytics)"
echo "   • Publishing Service: localhost:8083 (Basket listing & price publishing)"
echo ""
echo "   🆕 NEW INTEGRATION SERVICES:"
echo "   • SMA Adapter Service: localhost:8084 (SMA Refinitiv API integration)"
echo "   • FIX Adapter Service: localhost:8085 (FIX Bloomberg integration)"
echo "   • Integration Manager: localhost:8086 (Orchestration & coordination)"
echo ""
echo "🔍 Quick Health Checks:"
echo "   • Basket Core: curl http://localhost:8081/actuator/health"
echo "   • Market Data: curl http://localhost:8082/actuator/health"
echo "   • Publishing: curl http://localhost:8083/actuator/health"
echo "   • SMA Adapter: curl http://localhost:8084/sma-adapter/actuator/health"
echo "   • FIX Adapter: curl http://localhost:8085/fix-adapter/actuator/health"
echo "   • Integration Manager: curl http://localhost:8086/integration-manager/actuator/health"
echo ""
echo "🔍 Integration Health Check:"
echo "   curl http://localhost:8086/integration-manager/api/v1/integration/health"
echo ""
echo "📊 Basket Statistics:"
echo "   curl http://localhost:8083/api/v1/publishing/baskets/statistics"
echo ""
echo "🧪 Test SMA Integration:"
echo "   curl http://localhost:8084/sma-adapter/api/v1/sma/prices/AAPL"
echo ""
echo "🧪 Test FIX Integration:"
echo "   curl http://localhost:8085/fix-adapter/api/v1/fix/health"
echo ""
echo "📚 Documentation: docs/architecture/sma-fix-integration-design.md"
echo ""
echo "🛑 To stop services: docker-compose -f docker-compose.enhanced.yml down"
