# On-Prem Deployment, High Availability, and Monitoring

---

## On-Prem Configuration (redis.conf)

In production, Redis is configured via `redis.conf`. Key settings to tune:

### Memory

```conf
maxmemory 4gb
maxmemory-policy allkeys-lru
```

### Persistence

```conf
# RDB (snapshot)
save 900 1         # save if at least 1 key changed in 900 seconds
save 300 10        # save if 10 keys changed in 300 seconds
save 60 10000      # save if 10000 keys changed in 60 seconds
dbfilename dump.rdb
dir /var/lib/redis

# AOF (append-only file — more durable)
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec    # fsync every second (balance of safety vs performance)
# appendfsync always    # fsync every write (safest, slowest)
# appendfsync no        # OS decides when to fsync (fastest, riskiest)

# AOF rewrite to compact the file
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
```

**RDB vs AOF:**

| | RDB | AOF |
|-|-----|-----|
| File format | Binary snapshot | Append-only log of writes |
| Recovery point | Last snapshot (could be minutes old) | Up to last second (with everysec) |
| Restart speed | Fast (load snapshot) | Slower (replay log) |
| File size | Compact | Larger (grows until rewrite) |
| Best for | Backups, tolerable data loss | Minimal data loss requirement |

For production: enable both (`appendonly yes` + `save` directives) — AOF for durability, RDB for fast restarts.

### Network and security

```conf
bind 127.0.0.1 10.0.0.5    # only listen on loopback and internal interface
port 6379
requirepass YourStrongPassword
protected-mode yes

# TLS (Redis 6+)
tls-port 6380
tls-cert-file /etc/redis/tls/redis.crt
tls-key-file /etc/redis/tls/redis.key
tls-ca-cert-file /etc/redis/tls/ca.crt
```

### Timeouts and connections

```conf
timeout 300               # close idle client connections after 300s
tcp-keepalive 60
maxclients 10000
```

### OS-level tuning

Redis is sensitive to OS settings. On Linux:

```bash
# Disable Transparent Huge Pages (causes latency spikes in Redis)
echo never > /sys/kernel/mm/transparent_hugepage/enabled

# Set vm.overcommit_memory to 1 (required for background saves)
echo "vm.overcommit_memory = 1" >> /etc/sysctl.conf
sysctl -p

# Set a high somaxconn (listen backlog)
echo "net.core.somaxconn = 65535" >> /etc/sysctl.conf
```

Redis will print warnings on startup if these are not set correctly.

---

## High Availability — Redis Sentinel

Redis Sentinel provides automatic failover for a primary-replica setup. Sentinel monitors the primary and replicas, detects failure, elects a new primary, and notifies clients.

### Architecture

```
         +-----------+
         | Sentinel 1 |
         | Sentinel 2 |    (minimum 3 for quorum)
         | Sentinel 3 |
         +-----+-----+
               |
        +------+------+
        |             |
   [Primary]     [Replica 1]
                [Replica 2]
```

### redis.conf for primary

```conf
port 6379
requirepass masterpassword
```

### redis.conf for replica

```conf
port 6380
replicaof 127.0.0.1 6379
masterauth masterpassword
requirepass replicapassword
```

### sentinel.conf

```conf
port 26379
sentinel monitor mymaster 127.0.0.1 6379 2   # 2 = quorum (majority needed to declare failure)
sentinel auth-pass mymaster masterpassword
sentinel down-after-milliseconds mymaster 5000   # 5s before marking primary as down
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1              # how many replicas sync simultaneously during failover
```

### Start Sentinel

```bash
redis-sentinel /etc/redis/sentinel.conf
```

### Spring Boot with Sentinel

```properties
spring.redis.sentinel.master=mymaster
spring.redis.sentinel.nodes=127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381
spring.redis.password=masterpassword
```

**Sentinel guarantees:** automatic failover (primary down → replica promoted). No data sharding — all data lives on one primary.

---

## High Availability — Redis Cluster

Redis Cluster provides horizontal scaling (sharding) across multiple nodes plus automatic failover. Data is split into 16384 hash slots distributed across primary nodes.

### Architecture

```
Shard 1: Primary (slots 0-5460)      + Replica
Shard 2: Primary (slots 5461-10922)  + Replica
Shard 3: Primary (slots 10923-16383) + Replica
```

Each key is mapped to a slot: `HASH_SLOT = CRC16(key) % 16384`. Requests are routed to the node holding the correct slot.

### Minimum cluster setup (6 nodes: 3 primary + 3 replica)

```bash
redis-cli --cluster create \
  127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 \
  127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
  --cluster-replicas 1
```

### Spring Boot with Cluster

```properties
spring.redis.cluster.nodes=127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002
spring.redis.password=clusterpassword
```

### Cluster vs Sentinel

| | Sentinel | Cluster |
|-|---------|---------|
| Sharding (horizontal scale) | No | Yes |
| Automatic failover | Yes | Yes |
| Multi-key operations | Unrestricted | Keys must share a hash slot (`{tag}`) |
| Complexity | Low | Higher |
| When to use | Single large instance needing HA | Data too large for one node or very high throughput |

### Hash tags in Cluster

Multi-key operations (MGET, transactions, Lua) require all keys to be on the same slot. Force keys to the same slot using hash tags:

```bash
SET {user:42}:profile "..."
SET {user:42}:session "..."
# Both hash to the same slot because the tag {user:42} is used for slot calculation
```

---

## Connection Pooling — Lettuce vs Jedis in Spring Boot

Spring Data Redis supports two client libraries. **Lettuce is the default** since Spring Boot 2.

| | Lettuce | Jedis |
|-|---------|-------|
| Connection model | Single shared connection (non-blocking, async) | Per-thread connection pool |
| Thread safety | Yes — one connection handles all threads | No — each thread needs its own connection |
| Async/reactive support | Yes (Reactor) | No |
| Cluster support | Yes | Yes |
| Performance | Better for high concurrency | Good for sync workloads |

### Lettuce pool configuration (if needed)

```properties
spring.redis.lettuce.pool.max-active=16
spring.redis.lettuce.pool.max-idle=8
spring.redis.lettuce.pool.min-idle=2
spring.redis.lettuce.pool.max-wait=1000ms
```

### Switching to Jedis

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <exclusions>
        <exclusion>
            <groupId>io.lettuce</groupId>
            <artifactId>lettuce-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

```properties
spring.redis.jedis.pool.max-active=16
spring.redis.jedis.pool.max-idle=8
spring.redis.jedis.pool.min-idle=2
```

**Recommendation:** Stick with Lettuce. Only switch to Jedis if a specific library requires it or you have a sync-only use case with straightforward pool requirements.

---

## Monitoring

### INFO command

The `INFO` command returns everything about the Redis server. Run it with a section name for focused output.

```bash
INFO server          # version, OS, uptime
INFO clients         # connected clients, blocked clients
INFO memory          # used_memory, maxmemory, mem_fragmentation_ratio
INFO stats           # ops/sec, keyspace hits/misses, expired keys
INFO replication     # role (master/slave), connected replicas, replication lag
INFO keyspace        # per-DB key count, keys with TTL, avg TTL
INFO all             # everything
```

Key metrics to watch:

```bash
# Cache hit rate
INFO stats | grep keyspace_hits
INFO stats | grep keyspace_misses
# hit_rate = hits / (hits + misses)

# Memory fragmentation (>1.5 is a warning, >2.0 needs attention)
INFO memory | grep mem_fragmentation_ratio

# Evictions (non-zero means maxmemory is being hit)
INFO stats | grep evicted_keys

# Replication lag (replica behind primary by X bytes)
INFO replication | grep master_repl_offset
```

### SLOWLOG

Redis logs commands that take longer than a threshold (default 10ms).

```bash
# Configure threshold (in microseconds)
CONFIG SET slowlog-log-slower-than 10000   # 10ms

# Get last 10 slow commands
SLOWLOG GET 10

# Clear the log
SLOWLOG RESET

# Count entries
SLOWLOG LEN
```

### LATENCY

```bash
LATENCY HISTORY event        # latency history for an event
LATENCY LATEST               # most recent latency measurements
LATENCY RESET                # reset measurements
```

### MONITOR (debug only)

```bash
MONITOR    # streams every command processed by the server in real time
```

**Never use MONITOR in production** — it doubles the load on the server. Use it only locally for debugging.

### Key inspection

```bash
DBSIZE                         # number of keys in current DB
TYPE key:name                  # string / list / set / zset / hash / stream
OBJECT ENCODING key:name       # internal encoding (e.g., ziplist, hashtable, listpack)
OBJECT IDLETIME key:name       # seconds since last access
DEBUG OBJECT key:name          # serialized length, LRU time
```

### Keyspace notifications

Subscribe to events when keys expire, are modified, or are deleted. Useful for event-driven cache invalidation.

```bash
# Enable in redis.conf or via CONFIG SET
CONFIG SET notify-keyspace-events "KEA"
# K = keyspace events, E = keyevent events, A = all events (g$lzxed)
# Example: just expiry events
CONFIG SET notify-keyspace-events "Kx"
```

```bash
# Subscribe to expiry events on DB 0
PSUBSCRIBE __keyevent@0__:expired
```

```java
// Spring Boot: listen for expired key events
container.addMessageListener(listener, new PatternTopic("__keyevent@0__:expired"));
```

Use case: when a session key expires, trigger a logout event or cleanup without polling.
