-- ==================================================================================
-- Lua 스크립트: promote_all_waiting_for_event.lua
-- KEYS:
--   KEYS[1] = ENTRY_QUEUE_SLOTS_HASH_KEY              (예: "ENTRY_QUEUE_SLOTS")
--   KEYS[2] = "WAITING_QUEUE_INDEX_RECORD:" .. eventId      (예: "WAITING_QUEUE_INDEX_RECORD:42")
--   KEYS[3] = waitingZsetKey                          (예: "waiting:42")
--   KEYS[4] = "WAITING_USER_IDS:" .. eventId      (예: "WAITING_USER_IDS:42")
--   KEYS[5] = ENTRY_QUEUE_STREAM_KEY                   (예: "ENTRY_QUEUE")
-- ARGV:
--   ARGV[1] = eventId
--
-- 이 스크립트가 에러 없이 끝까지 수행되면, 1을 리턴하고
-- 만약 자리가 부족하거나 JSON 파싱 중 예외가 발생할 경우, 전체 롤백 후 0을 리턴
-- ==================================================================================

local eventId = string.gsub(ARGV[1], '"', '')

-- 1) 현재 남은 자리 읽기
local rawCount = redis.call("HGET", KEYS[1], eventId)
if (not rawCount) or (tonumber(rawCount) < 1) then
    return 0
end

local capacity = tonumber(rawCount)
-- 2) waiting ZSet에서 승격 가능한 만큼만 가져오기
--    각 itemJson 형태: "{\"userId\":123}"
local waitingItems = redis.call("ZRANGE", KEYS[3], 0, capacity - 1)
if (#waitingItems == 0) then
    return 0
end

local cnt = 0
-- 3) 각 waitingItem마다 순차 처리
for idx = 1, #waitingItems do
    local itemJson = waitingItems[idx]
    -- JSON 파싱 (cjson 모듈 사용)
    local ok, itemObj = pcall(cjson.decode, itemJson)
    if not ok then
        -- JSON 형식 에러: 해당 아이템만 제외하고 다음으로 진행
        redis.call("ZREM", KEYS[3], itemJson)
        goto continue
    end

    local userId = tostring(itemObj["userId"])
    if (not userId) then
        -- userId 없음: 해당 아이템만 제외하고 다음으로 진행
        redis.call("ZREM", KEYS[3], itemJson)
        goto continue
    end

    -- 3-1) entry queue count를 -1 감소
    redis.call("HINCRBY", KEYS[1], eventId, -1)

    -- 3-2) waiting record 해시에서 해당 record JSON 가져오기
    local waitingHashKey = KEYS[2]         -- "WAITING_QUEUE_INDEX_RECORD:{eventId}"
    local recordJson = redis.call("HGET", waitingHashKey, userId)
    if not recordJson then
        -- 대기 레코드가 없으면, 해당 아이템만 제외하고 다음으로 진행
        redis.call("ZREM", KEYS[3], itemJson)
        redis.call("HDEL", KEYS[4], userId)
        goto continue
    end

    -- 3-3) recordJson도 JSON 파싱 ({"userId":.., "eventId":.., "instanceId":..} 형태라고 가정)
    local ok, recordObj = pcall(cjson.decode, recordJson)
    if not ok then
        -- 레코드 파싱 실패: 해당 아이템만 제외하고 다음으로 진행
        redis.call("ZREM", KEYS[3], itemJson)
        redis.call("HDEL", KEYS[2], userId)
        redis.call("HDEL", KEYS[4], userId)
        goto continue
    end

    local instanceId = tostring(recordObj["instanceId"])
    if (not instanceId) then
        -- instanceId 없음: 해당 아이템만 제외하고 다음으로 진행
        redis.call("ZREM", KEYS[3], itemJson)
        redis.call("HDEL", KEYS[2], userId)
        redis.call("HDEL", KEYS[4], userId)
        goto continue
    end

    -- 3-4) ENTRY_QUEUE 스트림에 XADD (with ID="*")
    local entryMsg = { "userId", userId, "eventId", eventId, "instanceId", instanceId }
    redis.call("XADD", KEYS[5], "*", unpack(entryMsg))

    -- 3-5) waiting ZSet(KEYS[3])에서 해당 itemJson 제거
    redis.call("ZREM", KEYS[3], itemJson)

    -- 3-6) WAITING_QUEUE_INDEX_RECORD 해시(KEYS[2])에서 userId 필드 삭제
    redis.call("HDEL", KEYS[2], userId)

    -- 3-7) WAITING_QUEUE_IN_USER_RECORD 해시(KEYS[4])에서 userId 삭제
    redis.call("HDEL", KEYS[4], userId)

    cnt = cnt + 1

    ::continue::
end

-- 모든 사용자 프로모션 성공
return cnt
