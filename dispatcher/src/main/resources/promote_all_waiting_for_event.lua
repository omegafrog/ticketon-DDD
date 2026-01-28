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
    local itemJson = waitingItems[idx]
    local skip = false

    -- JSON 파싱
    local ok1, itemObj = pcall(cjson.decode, itemJson)
    if not ok1 then
        redis.call("ZREM", KEYS[3], itemJson)
        skip = true
    end

    local userId = nil
    if not skip then
        userId = itemObj["userId"]
        if userId == nil then
            redis.call("ZREM", KEYS[3], itemJson)
            skip = true
        else
            userId = tostring(userId)
        end
    end

    if not skip then
        -- waiting record 해시에서 record JSON 가져오기
        local recordJson = redis.call("HGET", KEYS[2], userId)
        if not recordJson then
            redis.call("ZREM", KEYS[3], itemJson)
            redis.call("HDEL", KEYS[4], userId)
            skip = true
        else
            -- recordJson 파싱
            local ok2, recordObj = pcall(cjson.decode, recordJson)
            if not ok2 then
                redis.call("ZREM", KEYS[3], itemJson)
                redis.call("HDEL", KEYS[2], userId)
                redis.call("HDEL", KEYS[4], userId)
                skip = true
            else
                local instanceId = recordObj["instanceId"]
                if instanceId == nil then
                    redis.call("ZREM", KEYS[3], itemJson)
                    redis.call("HDEL", KEYS[2], userId)
                    redis.call("HDEL", KEYS[4], userId)
                    skip = true
                else
                    instanceId = tostring(instanceId)

                    -- 여기서부터 “성공 처리”만 모아두기
                    redis.call("HINCRBY", KEYS[1], eventId, -1)

                    local entryMsg = { "userId", userId, "eventId", eventId, "instanceId", instanceId }
                    redis.call("XADD", KEYS[5], "*", unpack(entryMsg))

                    redis.call("ZREM", KEYS[3], itemJson)
                    redis.call("HDEL", KEYS[2], userId)
                    redis.call("HDEL", KEYS[4], userId)

                    cnt = cnt + 1
                end
            end
        end
    end
end

return cnt
