local eventId = string.gsub(ARGV[1], '"', '')

local rawCount = redis.call("HGET", KEYS[1], eventId)
if (not rawCount) or (tonumber(rawCount) < 1) then
    return 0
end

local capacity = tonumber(rawCount)

local waitingItems = redis.call("ZRANGE", KEYS[3], 0, capacity - 1)
if (#waitingItems == 0) then
    return 0
end

local cnt = 0

for idx = 1, #waitingItems do
    local rawUserId = waitingItems[idx]
    local userId = nil
    local skip = false

    if rawUserId == nil then
        skip = true
    elseif rawUserId == "" then
        redis.call("ZREM", KEYS[3], rawUserId)
        skip = true
    else
        userId = string.gsub(tostring(rawUserId), '"', '')

    end

    if not skip then
        -- waiting record 해시에서 record JSON 가져오기
        local recordJson = redis.call("HGET", KEYS[2], userId)
        if not recordJson then
            redis.call("ZREM", KEYS[3], userId)
            redis.call("HDEL", KEYS[2], userId)
            redis.call("HDEL", KEYS[4], userId)
            skip = true
        else
            local ok, record = pcall(cjson.decode, recordJson)
            if (not ok) or (record == nil) then
                redis.call("ZREM", KEYS[3], userId)
                redis.call("HDEL", KEYS[2], userId)
                redis.call("HDEL", KEYS[4], userId)
                skip = true
            else
                local instanceId = record["instanceId"]
                if (not instanceId) or (tostring(instanceId) == "") then
                    redis.call("ZREM", KEYS[3], userId)
                    redis.call("HDEL", KEYS[2], userId)
                    redis.call("HDEL", KEYS[4], userId)
                    skip = true
                else
                    -- 여기서부터 “성공 처리”만 모아두기
                    redis.call("HINCRBY", KEYS[1], eventId, -1)

                    local entryMsg = { "userId", userId, "eventId", eventId, "instanceId", tostring(instanceId) }
                    redis.call("XADD", KEYS[5], "*", unpack(entryMsg))

                    redis.call("ZREM", KEYS[3], userId)
                    redis.call("HDEL", KEYS[2], userId)
                    redis.call("HDEL", KEYS[4], userId)

                    cnt = cnt + 1
                end
            end
        end
    end
end

return cnt
