#!/bin/bash

echo "🚀 Starting CIB Listing & Pricing Service - MINIMAL MODE"
echo "========================================================"
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

echo "📋 Starting minimal services (Database + Core Services)..."
echo ""

# Start minimal services
docker-compose -f docker-compose.minimal.yml up -d

# Wait for services to start
echo "⏳ Waiting for services to start..."
sleep 10

# Check service status
echo ""
echo "📊 Service Status:"
docker-compose -f docker-compose.minimal.yml ps

echo ""
echo "🔍 Health Checks:"

# Check database health
if docker exec -it basket-timescaledb pg_isready -U basket_user -d basket_platform > /dev/null 2>&1; then
    echo "✅ Database: HEALTHY"
else
    echo "❌ Database: UNHEALTHY"
fi

# Check publishing service health
if curl -s http://localhost:8083/actuator/health > /dev/null; then
    echo "✅ Publishing Service: HEALTHY"
else
    echo "❌ Publishing Service: UNHEALTHY"
fi

# Check basket core service health
if curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "✅ Basket Core Service: HEALTHY"
else
    echo "❌ Basket Core Service: UNHEALTHY"
fi

# Check market data service health
if curl -s http://localhost:8082/actuator/health > /dev/null; then
    echo "✅ Market Data Service: HEALTHY"
else
    echo "❌ Market Data Service: UNHEALTHY"
fi

echo ""
echo "🎯 MINIMAL MODE STARTED SUCCESSFULLY!"
echo ""
echo "📋 Available Services:"
echo "   • Database: localhost:5432"
echo "   • Basket Core Service: localhost:8081 (Basket lifecycle management)"
echo "   • Market Data Service: localhost:8082 (Market data & analytics)"
echo "   • Publishing Service: localhost:8083 (Basket listing & price publishing)"
echo ""
echo "🔍 Quick Health Checks:"
echo "   • Basket Core: curl http://localhost:8081/actuator/health"
echo "   • Market Data: curl http://localhost:8082/actuator/health"
echo "   • Publishing: curl http://localhost:8083/actuator/health"
echo ""
echo "📊 Basket Statistics:"
echo "   curl http://localhost:8083/api/v1/publishing/baskets/statistics"
echo ""
echo "📚 Documentation: docs/minimal-deployment-guide.md"
echo ""
echo "🛑 To stop services: docker-compose -f docker-compose.minimal.yml down"
