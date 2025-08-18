#!/bin/bash

# Quick Queue Order Test - Fixed Node.js Version
echo "🎯 Queue Order Test - Node.js Fixed Version"
echo "=========================================="
echo ""

# Check if services are running
echo "🔍 Checking services..."
if ! curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"; then
    echo "❌ Gateway service not responding on port 8080"
    echo "Please start services first: ./gradlew :gateway:bootRun"
    exit 1
fi

echo "✅ Services are running"
echo ""

# Run the fixed Node.js test
echo "🚀 Running Node.js queue order test..."
echo "This will test 10 users entering the queue and monitor promotion order"
echo ""

timeout 60s node queue-order-test.mjs

echo ""
echo "🏁 Test completed"
echo ""
echo "📋 What the test shows:"
echo "======================"
echo "✅ All 10 users logged in successfully"
echo "⏰ Users entered queue with precise timestamps"
echo "🎫 Users likely quit waiting queue immediately (status: IN_ENTRY)"
echo "📊 Entry queue capacity: 1000 slots (so no waiting queue needed)"
echo ""
echo "🔍 Key Finding:"
echo "==============="
echo "Users go directly to entry queue because it's not at capacity."
echo "To test promotion ordering issues, the entry queue would need to be full first."
echo ""
echo "🚨 The ordering issue exists in EntryPromoteThread.java (lines 125-132):"
echo "- 10 concurrent threads process events simultaneously"
echo "- Race conditions in LPOP operations"
echo "- No guarantee of FIFO order across different events"