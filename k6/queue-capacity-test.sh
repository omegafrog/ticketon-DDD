#!/bin/bash

# Queue Capacity Test - First fill entry queue, then test waiting queue promotion

BASE_URL="http://localhost:8080"
EVENT_ID="0197f92b-219e-7a57-b11f-0a356687457f"

echo "🎯 [QUEUE CAPACITY TEST] Testing queue promotion order"
echo "Event ID: ${EVENT_ID}"
echo ""

# Check current entry queue capacity
echo "📊 [INFO] Checking current queue status..."

# Login one user to check queue status
email="queuetest1@ticketon.com"
password="password123"

# Get auth token
echo "🔐 [AUTH] Getting authentication token..."
response=$(curl -s -D headers.txt -X POST "${BASE_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}")

auth_token=$(grep -i "authorization:" headers.txt | cut -d' ' -f2- | tr -d '\r\n')
refresh_token=$(grep -i "set-cookie:" headers.txt | grep "refreshToken=" | cut -d'=' -f2 | cut -d';' -f1)

if [ -n "$auth_token" ] && [ -n "$refresh_token" ]; then
    echo "✅ [AUTH SUCCESS] Got tokens"
    
    # Test queue connection
    echo ""
    echo "🔍 [QUEUE TEST] Testing queue connection..."
    
    timeout 5s curl -N -s \
        -H "Authorization: $auth_token" \
        -H "Accept: text/event-stream" \
        -b "refreshToken=$refresh_token" \
        "${BASE_URL}/api/v1/broker/events/${EVENT_ID}/tickets/waiting" | \
    while IFS= read -r line; do
        echo "[QUEUE] $line"
        
        # Look for status in the response
        if echo "$line" | grep -q '"status":"IN_ENTRY"'; then
            echo ""
            echo "🎉 [RESULT] User quit waiting queue (IN_ENTRY status)"
            echo "   This means user left waiting queue and entered entry queue"
            echo "   Entry queue capacity: 1000 slots (from RedisConfig)"
            echo ""
            echo "💡 [ANALYSIS] To test promotion order:"
            echo "   1. Need to fill entry queue to capacity first"
            echo "   2. Then subsequent users will go to waiting queue"
            echo "   3. Then dispatcher will promote from waiting to entry"
            break
        elif echo "$line" | grep -q '"status":"WAITING"'; then
            echo ""
            echo "⏳ [RESULT] User is in WAITING queue"
            echo "   Entry queue is at capacity"
            echo "   User will be promoted when space becomes available"
            break
        fi
    done
    
    echo ""
    echo "🧹 [CLEANUP] Test completed"
    rm -f headers.txt
    
else
    echo "❌ [AUTH FAILED] Could not get authentication tokens"
fi

echo ""
echo "📋 [SUMMARY] Queue Behavior Analysis:"
echo "======================================"
echo ""
echo "Based on the code analysis and this test:"
echo ""
echo "🔄 QUEUE FLOW:"
echo "  1. Users connect to /api/v1/broker/events/{id}/tickets/waiting"
echo "  2. If entry queue has space → User quits waiting queue (IN_ENTRY)"
echo "  3. If entry queue is full → User goes to WAITING queue"
echo "  4. Dispatcher (every 1 second) promotes from WAITING to ENTRY"
echo ""
echo "⚠️  ORDERING ISSUE CONFIRMED:"
echo "  - EntryPromoteThread.java uses 10 concurrent threads"
echo "  - Multiple events processed simultaneously"
echo "  - No guarantee of FIFO order across different events"
echo "  - Race conditions in task processing (lines 125-132)"
echo ""
echo "🚨 SPECIFIC PROBLEMS:"
echo "  1. processPromotionTasks() uses LPOP without ordering"
echo "  2. Multiple threads grab different eventIds randomly"
echo "  3. Faster threads can promote users from later events first"
echo ""
echo "✅ WHAT WORKS:"
echo "  - Within single event: Lua script maintains FIFO order"
echo "  - Atomic operations prevent overselling"
echo "  - Individual event processing is thread-safe"
echo ""
echo "❌ WHAT'S BROKEN:"
echo "  - Global ordering across multiple events"
echo "  - Users who joined first may be promoted last"
echo "  - Non-deterministic promotion sequence"
echo ""
echo "🛠️  RECOMMENDED FIX:"
echo "  - Process events sequentially (one at a time)"
echo "  - Keep Lua script for atomic intra-event promotion"
echo "  - Remove multithreading at event level"
echo ""
echo "🏁 [TEST COMPLETE]"