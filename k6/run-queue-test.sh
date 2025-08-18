#!/bin/bash

# Queue Order Test Runner
# Choose between k6 or Node.js implementation

echo "🎯 Queue Order Test Runner"
echo "========================="
echo ""
echo "This test will create 10 users and verify queue ordering"
echo "Make sure the following services are running:"
echo "  - Gateway (port 8080)"
echo "  - Redis"
echo "  - Auth service"
echo "  - Broker service"
echo "  - Dispatcher service (for promotions)"
echo ""

# Check if services are running
echo "🔍 Checking if services are running..."

if ! curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"; then
    echo "❌ Gateway service not responding on port 8080"
    echo "Please start the services first:"
    echo "  ./gradlew :gateway:bootRun"
    exit 1
fi

echo "✅ Gateway service is running"

# Create test users first (optional - they might already exist)
echo ""
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

# Choose test runner
echo ""
echo "Choose test runner:"
echo "1) k6 (requires k6 installation)"
echo "2) Node.js (requires node and npm packages)"
echo "3) Manual curl commands"

read -p "Enter choice (1-3): " choice

case $choice in
    1)
        echo ""
        echo "🚀 Running k6 test..."
        if command -v k6 &> /dev/null; then
            k6 run k6-queue-order-test.js
        else
            echo "❌ k6 not found. Please install k6 first:"
            echo "  https://k6.io/docs/getting-started/installation/"
            echo ""
            echo "Installation options:"
            echo "  - Linux: sudo apt-get install k6"
            echo "  - macOS: brew install k6"
            echo "  - Windows: winget install k6"
        fi
        ;;
    2)
        echo ""
        echo "🚀 Running Node.js test..."
        if command -v node &> /dev/null; then
            # Check if required packages are available
            if node -e "require('node-fetch')" 2>/dev/null && node -e "require('eventsource')" 2>/dev/null; then
                node queue-order-test.mjs
            else
                echo "❌ Required npm packages not found. Installing..."
                npm install node-fetch eventsource
                node queue-order-test.mjs
            fi
        else
            echo "❌ Node.js not found. Please install Node.js first"
        fi
        ;;
    3)
        echo ""
        echo "📋 Manual test instructions:"
        echo "============================================="
        echo ""
        echo "1. Login users (get auth tokens):"
        for i in {1..10}; do
            echo "curl -X POST 'http://localhost:8080/api/v1/auth/login' \\"
            echo "  -H 'Content-Type: application/json' \\"
            echo "  -d '{\"email\":\"queuetest${i}@ticketon.com\",\"password\":\"password123\"}' \\"
            echo "  -D headers${i}.txt"
            echo ""
        done
        echo ""
        echo "2. Extract Authorization headers from headers*.txt files"
        echo ""
        echo "3. Connect to queue (use auth tokens from step 2):"
        echo "curl -N 'http://localhost:8080/api/v1/broker/events/0197f92b-219e-7a57-b11f-0a356687457f/tickets/waiting' \\"
        echo "  -H 'Authorization: Bearer YOUR_TOKEN_HERE' \\"
        echo "  -H 'Accept: text/event-stream'"
        echo ""
        echo "4. Monitor the SSE streams for promotion events"
        echo "5. Verify that promotion order matches entry order"
        ;;
    *)
        echo "❌ Invalid choice"
        ;;
esac

echo ""
echo "🏁 Test runner completed"
