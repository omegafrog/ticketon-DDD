#!/bin/bash

# Simple Queue Order Test using curl
# Tests queue ordering by creating users sequentially and monitoring results

BASE_URL="http://localhost:8080"
EVENT_ID="0197f92b-219e-7a57-b11f-0a356687457f"
USER_COUNT=10

echo "🚀 [TEST START] Testing queue order with ${USER_COUNT} users"
echo "Event ID: ${EVENT_ID}"
echo ""

# Arrays to store results
declare -a ENTRY_ORDER
declare -a AUTH_TOKENS

# Phase 1: Login users and collect tokens
echo "📝 [PHASE 1] Logging in users..."
for i in $(seq 1 $USER_COUNT); do
    email="queuetest${i}@ticketon.com"
    password="password123"
    
    echo "🔐 [LOGIN] User ${i}: ${email}"
    
    # Login and capture headers
    response=$(curl -s -D headers_${i}.txt -X POST "${BASE_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$password\"}")
    
    # Extract auth token from headers
    auth_token=$(grep -i "authorization:" headers_${i}.txt | cut -d' ' -f2- | tr -d '\r\n')
    
    if [ -n "$auth_token" ]; then
        AUTH_TOKENS[$i]="$auth_token"
        ENTRY_ORDER+=($i)
        echo "✅ [LOGIN SUCCESS] User ${i} - Token: ${auth_token:0:20}..."
    else
        echo "❌ [LOGIN FAILED] User ${i}"
    fi
    
    sleep 0.1
done

echo ""
echo "📝 [PHASE 1 COMPLETE] Successfully logged in ${#AUTH_TOKENS[@]} users"
echo "Entry order: [${ENTRY_ORDER[*]}]"

# Phase 2: Connect to queue sequentially and monitor promotion
echo ""
echo "🎯 [PHASE 2] Connecting users to queue..."
echo "Note: This will show SSE stream data - look for promotion events"
echo ""

# Create temporary files for each user's SSE stream
for i in $(seq 1 $USER_COUNT); do
    if [ -n "${AUTH_TOKENS[$i]}" ]; then
        echo "⏰ [QUEUE ENTRY] User ${i} connecting to queue..."
        
        # Start SSE connection in background and log to file
        curl -N -s -H "Authorization: ${AUTH_TOKENS[$i]}" \
            -H "Accept: text/event-stream" \
            "${BASE_URL}/api/v1/broker/events/${EVENT_ID}/tickets/waiting" \
            | while IFS= read -r line; do
                timestamp=$(date '+%H:%M:%S.%3N')
                echo "[$timestamp] User $i: $line" | tee -a queue_results.log
                
                # Check for promotion event
                if echo "$line" | grep -q '"status":"IN_ENTRY"'; then
                    echo "🎉 [QUIT QUEUE] User $i quit waiting queue at $timestamp" | tee -a promotions.log
                fi
            done &
        
        # Store the PID to kill later
        echo $! > "user_${i}_pid.txt"
        
        sleep 0.2  # Small delay between connections to ensure order
    fi
done

echo ""
echo "⏳ [MONITORING] All users connected. Monitoring for promotions..."
echo "   - Live results: tail -f queue_results.log"
echo "   - Promotions: tail -f promotions.log"
echo ""
echo "   Press Ctrl+C to stop monitoring"
echo ""

# Monitor for 60 seconds or until interrupted
timeout 60s tail -f promotions.log 2>/dev/null || true

# Cleanup: Kill all background processes
echo ""
echo "🧹 [CLEANUP] Stopping all SSE connections..."
for i in $(seq 1 $USER_COUNT); do
    if [ -f "user_${i}_pid.txt" ]; then
        pid=$(cat "user_${i}_pid.txt")
        kill $pid 2>/dev/null || true
        rm "user_${i}_pid.txt"
    fi
done

# Clean up header files
rm -f headers_*.txt

# Phase 3: Analyze results
echo ""
echo "📊 [PHASE 3] ANALYZING RESULTS..."
echo "======================================"

if [ -f "promotions.log" ]; then
    echo ""
    echo "🎉 PROMOTION EVENTS:"
    cat promotions.log
    
    echo ""
    echo "📈 PROMOTION ORDER:"
    promotion_order=$(grep -o 'User [0-9]\+' promotions.log | grep -o '[0-9]\+' | tr '\n' ' ')
    echo "Promotion order: [$promotion_order]"
    
    # Simple order check
    entry_str="${ENTRY_ORDER[*]}"
    promotion_str="$promotion_order"
    
    if [ "${entry_str:0:${#promotion_str}}" = "$promotion_str" ]; then
        echo ""
        echo "✅ RESULT: User queue ordering is working correctly"
        echo "   Entry order matches promotion order"
    else
        echo ""
        echo "🚨 RESULT: User queue ordering has violations"
        echo "   Entry order:    [$entry_str]"
        echo "   Promotion order: [$promotion_str]"
    fi
else
    echo "⚠️  No promotion events recorded"
    echo "   Check if dispatcher service is running"
    echo "   Check queue_results.log for connection issues"
fi

echo ""
echo "📋 Log files created:"
echo "   - queue_results.log: Full SSE stream data"
echo "   - promotions.log: Promotion events only"
echo ""
echo "🏁 [TEST COMPLETE]"