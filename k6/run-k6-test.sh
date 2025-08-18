#!/bin/bash

# K6 Queue Order Test Runner
echo "🎯 K6 Queue Order Test Runner"
echo "============================"
echo ""

# Check if services are running
echo "🔍 Checking if services are running..."
if ! curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"; then
    echo "❌ Gateway service not responding on port 8080"
    echo "Please start the services first:"
    echo "  ./gradlew :gateway:bootRun"
    echo "  ./gradlew :auth:bootRun"
    echo "  ./gradlew :broker:bootRun"
    echo "  ./gradlew :dispatcher:bootRun"
    echo "  docker-compose -f docker/docker-compose.yml up -d"
    exit 1
fi

echo "✅ Services are running"
echo ""

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo "❌ k6 not found. Please install k6 first:"
    echo ""
    echo "Installation options:"
    echo "  - Linux: sudo apt-get install k6"
    echo "  - macOS: brew install k6" 
    echo "  - Windows: winget install k6"
    echo "  - Or visit: https://k6.io/docs/getting-started/installation/"
    echo ""
    echo "🔄 Alternative: Use Node.js version instead:"
    echo "  node queue-order-test.mjs"
    exit 1
fi

echo "✅ k6 is installed ($(k6 version | head -1))"
echo ""

# Create test users (if they don't exist)
echo "📝 Creating test users (if they don't exist)..."
for i in {1..10}; do
    email="queuetest${i}@ticketon.com"
    password="password123"
    
    # Try to create user (ignore if already exists)
    curl -s -X POST "http://localhost:8080/api/v1/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$password\",\"name\":\"Queue Test User $i\"}" \
        > /dev/null 2>&1
done
echo "✅ Test users ready"
echo ""

echo "🚀 Running K6 Queue Order Test..."
echo "=================================="
echo ""
echo "This test will:"
echo "  - Login 10 users sequentially"
echo "  - Connect each user to the waiting queue"
echo "  - Monitor SSE events for IN_ENTRY status"
echo "  - Detect order violations"
echo "  - Measure queue performance metrics"
echo ""

# Run k6 test
k6 run k6-queue-order-test.js

echo ""
echo "🏁 K6 test completed"
echo ""
echo "📊 Check the output above for:"
echo "  - Order violations count"
echo "  - Performance metrics"
echo "  - Detailed violation analysis"
echo ""
echo "🎯 Expected behavior if queue ordering works correctly:"
echo "  - order_violations: 0"
echo "  - Entry order matches promotion order"
echo "  - No race condition detected"
echo ""
echo "🚨 If violations found, this confirms the race condition in:"
echo "  - dispatcher/src/main/java/org/codenbug/messagedispatcher/thread/EntryPromoteThread.java"
echo "  - Lines 125-132: Multithreaded event processing"