# TTL, Key Expiry, and Eviction Policies

---

## TTL and Key Expiry

Every key in Redis can have an expiry (TTL — Time To Live). Once the TTL hits zero, Redis deletes the key automatically.

### Setting TTL at creation time

```bash
SET session:abc123 "user:42" EX 3600       # expires in 3600 seconds (1 hour)
SET otp:9876 "4821" PX 300000              # expires in 300000 milliseconds (5 min)
SET lock:job:1 "worker-1" EX 30 NX        # set only if not exists, expires in 30s
```

### Setting TTL on an existing key

```bash
EXPIRE user:session:123 1800              # set TTL to 30 minutes
PEXPIRE user:session:123 1800000          # same in milliseconds
EXPIREAT user:session:123 1735689600      # expire at a Unix timestamp
```

### Inspecting TTL

```bash
TTL user:session:123       # returns seconds remaining, -1 = no TTL, -2 = key does not exist
PTTL user:session:123      # same in milliseconds
```

### Removing TTL (make a key permanent)

```bash
PERSIST user:session:123
```

---

## Key Naming and TTL Best Practices

- Always set TTL on cache entries — a key with no TTL is a memory leak waiting to happen.
- Keys that should survive restarts but expire eventually (e.g., refresh tokens): use longer TTL + AOF persistence.
- Session keys: short TTL (15-60 min) with sliding expiry — reset TTL on every access.
- OTPs, email verification tokens: very short TTL (5-10 min).
- Never store data without TTL unless it is truly permanent reference data.

**Key naming convention:** `service:entity:id:field`

```bash
myapp:user:123:session
myapp:rate:ip:192.168.1.1
myapp:cache:product:456
```

---

## Lazy vs Active Expiry

Redis uses two mechanisms to expire keys:

1. **Lazy expiry:** A key is only checked for expiry when it is accessed. If no one reads it, it stays in memory even after TTL is up (temporarily).
2. **Active expiry:** Redis periodically samples a random set of keys with TTLs and deletes the expired ones.

The combination means Redis is eventually consistent about expiry — keys will be gone soon after TTL, but not guaranteed to the millisecond unless accessed.

---

## Eviction Policies

When Redis reaches its `maxmemory` limit, it needs to decide what to evict. This is configured via `maxmemory-policy` in `redis.conf` or at runtime.

### Available policies

| Policy | Behavior |
|--------|----------|
| `noeviction` | Return error on write when memory is full. Default. |
| `allkeys-lru` | Evict the least recently used key from all keys. |
| `volatile-lru` | Evict the LRU key only from keys with a TTL set. |
| `allkeys-lfu` | Evict the least frequently used key from all keys. |
| `volatile-lfu` | Evict the LFU key only from keys with a TTL set. |
| `allkeys-random` | Evict a random key from all keys. |
| `volatile-random` | Evict a random key from keys with a TTL. |
| `volatile-ttl` | Evict the key with the shortest remaining TTL. |

### Setting maxmemory and policy

```bash
# In redis.conf
maxmemory 256mb
maxmemory-policy allkeys-lru

# Or at runtime via CLI
CONFIG SET maxmemory 268435456
CONFIG SET maxmemory-policy allkeys-lru
```

### Choosing the right policy

- **Pure cache (can rebuild from DB):** `allkeys-lru` or `allkeys-lfu` — evict from anything.
- **Mixed use (some permanent keys, some cache keys with TTL):** `volatile-lru` — only evict keys that have a TTL, protecting permanent data.
- **Not using Redis as a cache (only as a DB/queue):** `noeviction` — fail loudly instead of losing data silently.

---

## SCAN vs KEYS

**Never use `KEYS *` in production.** It is a blocking O(N) operation that scans every key and blocks the entire Redis server for the duration.

**Use `SCAN` instead** — it is a cursor-based iterator that returns a small batch at a time and never blocks.

```bash
# KEYS (dangerous in production)
KEYS user:*

# SCAN (safe)
SCAN 0 MATCH user:* COUNT 100
# Returns: cursor + batch of matching keys
# Repeat with returned cursor until cursor is 0 (full scan complete)
```

### SCAN in Spring Boot (RedisTemplate)

```java
ScanOptions options = ScanOptions.scanOptions()
    .match("myapp:cache:*")
    .count(100)
    .build();

try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
        .getConnection().scan(options)) {
    while (cursor.hasNext()) {
        String key = new String(cursor.next());
        // process key
    }
}
```

Pattern: use `SCAN` for key inspection, bulk TTL refresh, or cache warm-up scripts. Never `KEYS`.
