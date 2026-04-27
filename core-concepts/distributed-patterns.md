# Distributed Patterns with Redis

Common production patterns built on top of Redis primitives.

---

## 1. Distributed Locking

A distributed lock prevents multiple instances of a service from executing the same critical section simultaneously (e.g., two workers processing the same job, two requests modifying the same resource).

### The naive approach (wrong)

```bash
# DO NOT DO THIS — two commands, not atomic
SETNX lock:job:1 "worker-1"   # set only if not exists
EXPIRE lock:job:1 30          # set TTL separately
```

**Problem:** If the process crashes between `SETNX` and `EXPIRE`, the lock never expires. Deadlock.

### The correct approach — SET NX EX (atomic)

```bash
# Atomic: set only if not exists AND set TTL in one command
SET lock:job:1 "worker-1" NX EX 30
# Returns OK if lock acquired, nil if already held
```

### Releasing the lock safely (Lua)

Only the owner should release the lock. A plain `DEL lock:job:1` could accidentally delete a lock held by a different worker (if the original holder's TTL expired and another worker acquired it). Use Lua to check-then-delete atomically.

```lua
-- Release lock only if the value matches (i.e., owned by this worker)
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1])
else
    return 0
end
```

```java
// Spring Boot: acquire lock
Boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent("lock:job:1", workerId, Duration.ofSeconds(30));

// Release lock via Lua
DefaultRedisScript<Long> releaseScript = new DefaultRedisScript<>(
    "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
    "  return redis.call('DEL', KEYS[1]) " +
    "else return 0 end",
    Long.class
);
redisTemplate.execute(releaseScript, List.of("lock:job:1"), workerId);
```

### Redisson for production locking

For production distributed locks, use **Redisson** instead of manual SET NX EX. Redisson handles lock renewal (watchdog that auto-extends TTL while the holder is alive), reentrancy, and fair locking.

```java
RLock lock = redissonClient.getLock("lock:job:1");
try {
    if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
        // critical section
    }
} finally {
    lock.unlock();
}
```

### Lock TTL sizing

- Set TTL long enough that the critical section completes under normal conditions.
- Set it short enough that a dead worker doesn't hold it forever.
- With Redisson watchdog: TTL auto-renews every TTL/3 seconds, so it never expires on a live holder.

---

## 2. Rate Limiting

Redis is the standard tool for API rate limiting. The pattern: increment a counter per client per time window. Reject the request if the counter exceeds the limit.

### Fixed Window Counter

```bash
# Key: rate:ip:192.168.1.1:minute:202501151430  (resets each minute)
INCR rate:ip:192.168.1.1
EXPIRE rate:ip:192.168.1.1 60  # window of 60 seconds
```

**Problem:** Requests at the boundary of two windows can double the limit (100 at 59s + 100 at 61s = 200 in 2 seconds).

### Sliding Window with Sorted Sets (precise)

Store each request timestamp as a score in a Sorted Set. Count requests in the last N seconds by range.

```bash
# On each request:
ZADD rate:user:42 <current_timestamp_ms> <unique_request_id>
ZREMRANGEBYSCORE rate:user:42 0 <current_timestamp_ms - window_ms>   # remove old entries
count = ZCARD rate:user:42
EXPIRE rate:user:42 <window_seconds>
# If count > limit: reject
```

### Lua atomic rate limiter (recommended)

```lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

ZREMRANGEBYSCORE(key, 0, now - window)
local count = ZCARD(key)
if count < limit then
    ZADD(key, now, now)
    EXPIRE(key, window / 1000)
    return 1  -- allowed
else
    return 0  -- rejected
end
```

### Spring Boot rate limiting with Bucket4j + Redis

For production, use **Bucket4j** with Redis backend — it implements token bucket algorithm and handles distributed state correctly.

```java
@Bean
public BucketConfiguration bucketConfiguration() {
    return BucketConfiguration.builder()
        .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
        .build();
}
```

---

## 3. Session Storage

Redis is the standard session store for horizontally scaled applications. Instead of storing sessions in application memory (which breaks when there are multiple instances), sessions are stored in Redis and any instance can serve any request.

### Spring Session with Redis

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

```properties
# application.properties
spring.session.store-type=redis
spring.session.timeout=30m
spring.redis.host=localhost
spring.redis.port=6379
```

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {
}
```

Spring Session stores sessions as Redis Hashes under `spring:session:sessions:{sessionId}`. TTL is automatically managed.

---

## 4. Leaderboard / Ranked List

Sorted Sets are the natural fit for leaderboards. The score is the ranking metric (points, time, etc.) and commands operate in O(log N).

```bash
# Add/update scores
ZADD leaderboard 1500 "user:42"
ZADD leaderboard 2200 "user:99"
ZADD leaderboard 1800 "user:17"

# Top 10 (highest score first)
ZREVRANGE leaderboard 0 9 WITHSCORES

# Rank of a specific user (0-based, highest = rank 0)
ZREVRANK leaderboard "user:42"

# Score of a user
ZSCORE leaderboard "user:42"

# Increment score
ZINCRBY leaderboard 100 "user:42"
```

---

## 5. Idempotency Keys

Prevent duplicate processing of the same request (e.g., duplicate payment submission). Store the idempotency key with a TTL. On second request with the same key, return the cached result instead of reprocessing.

```bash
SET idempotency:pay:order:101 '{"status":"success","txId":"abc"}' NX EX 86400
# NX = only set if not exists
# If returns nil — key existed, return cached response
# If returns OK — first time, process the request
```

```java
String key = "idempotency:pay:order:" + orderId;
String cached = (String) redisTemplate.opsForValue().get(key);
if (cached != null) {
    return objectMapper.readValue(cached, PaymentResponse.class); // duplicate
}
// process payment...
redisTemplate.opsForValue().set(key, serialize(response), Duration.ofDays(1));
```

---

## 6. Counter and Bloom Filter (Approximate Counting)

### Atomic counter

```bash
INCR page:views:home       # increment by 1, returns new value
INCRBY page:views:home 5   # increment by N
DECR page:views:home
DECRBY page:views:home 3
```

Atomic — no race condition even under concurrent writes.

### HyperLogLog (approximate unique count)

For counting unique visitors without storing every user ID:

```bash
PFADD unique:visitors:2025-01-15 "user:42" "user:99" "user:17"
PFCOUNT unique:visitors:2025-01-15    # estimated unique count
PFMERGE unique:visitors:week unique:visitors:2025-01-15 unique:visitors:2025-01-16
```

Error rate: ~0.81%. Memory: always 12KB regardless of cardinality. Good for analytics at scale where approximate counts are acceptable.
