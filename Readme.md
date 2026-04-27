# Redis References

Personal reference repo for learning Redis — covering fundamentals, data types, Spring Boot integration, and caching strategies.

## Repo Structure

```
Redis-References/
  notes/
    quick-revision.md         # Docker setup, DB structure, programmatic access quick ref
    cache-strategies.md       # Cache-Aside, Read-Through, Write-Through, Write-Behind, Refresh-Ahead
  Docker/
    Setting-Up-Redis.md       # Docker setup with auth and ACL user management
    compose/docker-compose.yml
  core-concepts/
    Datatypes.md              # CRUD for all Redis data types
    Hash-eg.md                # Hands-on Redis Hashes examples
    How-to-Store-Users-in-Redis.md
    RedisJSON.md              # RedisJSON CRUD
    ttl-expiry-eviction.md    # TTL, key expiry, eviction policies, SCAN vs KEYS
    pub-sub-streams.md        # Pub/Sub (fire-and-forget) and Streams (durable log + consumer groups)
    transactions-pipelining-lua.md  # MULTI/EXEC/WATCH, pipelining, Lua scripting
    distributed-patterns.md   # Distributed locking, rate limiting, session storage, leaderboards, idempotency
    on-prem-ha-monitoring.md  # redis.conf, persistence (RDB+AOF), Sentinel, Cluster, Lettuce vs Jedis, monitoring
  code/
    Java/
      redis-crud-sb3/         # Spring Boot 3 — CrudRepository and RedisTemplate approaches
      sb-redis-cache-optimization-crud/  # Spring Boot 3 + JPA + Redis caching (@Cacheable etc.)
    python/app.py             # Python redis-py client examples
```

---

# Introduction to Redis

## What is Redis?
Redis (Remote Dictionary Server) is an in-memory, key-value data store that is primarily used as a cache, message broker, or real-time database. It is known for its speed, scalability, and simple data structures.

## Why Use Redis?
- **High Performance:** Redis operates in-memory, making read and write operations extremely fast compared to traditional disk-based databases.
- **Scalability:** Supports clustering and replication for handling large-scale applications.
- **Flexible Data Structures:** Supports strings, lists, sets, hashes, bitmaps, and more.
- **Persistence Options:** Can persist data using snapshots (RDB) or append-only files (AOF).
- **Pub/Sub Messaging:** Supports publish/subscribe messaging patterns for real-time communication.
- **Atomic Operations:** Provides atomicity in commands, reducing the need for complex transaction management.

## Who Uses Redis?
Redis is widely used by:
- **Tech Companies:** Facebook, Twitter, Instagram, and Uber for caching and real-time analytics.
- **E-commerce Platforms:** Amazon, Shopify for product recommendations and session storage.
- **Financial Services:** Fraud detection and transaction processing.
- **Gaming Applications:** Leaderboards and session management.

---

# Redis Data Types
Redis supports multiple data structures, making it more than just a simple key-value store:

| Data Type   | Description |
|------------|-------------|
| **Strings** | Stores simple text or binary data. Supports operations like GET, SET, INCR, and APPEND. |
| **Lists** | Ordered collection of strings, similar to a linked list. Supports LPUSH, RPUSH, LPOP, RPOP. |
| **Sets** | Unordered collection of unique elements. Supports operations like SADD, SREM, and set intersections. |
| **Sorted Sets (Zsets)** | Like sets, but with an associated score that determines order. Useful for leaderboards. |
| **Hashes** | Key-value pairs stored under one key, similar to an object. Useful for storing user profiles. |
| **Bitmaps** | Used for bit-level operations like tracking user activity. |
| **HyperLogLogs** | Estimates cardinality (unique elements) efficiently with low memory usage. |
| **Streams** | Log-like structure for event-driven architectures. |

---

# Redis vs. RDBMS (PostgreSQL)

| Feature | Redis | PostgreSQL |
|---------|------|------------|
| **Data Storage** | In-memory (with persistence options) | Disk-based |
| **Schema** | Schema-less, key-value store | Schema-driven (DB > Schema > Tables) |
| **Data Access** | Key-value lookups | SQL queries (SELECT, JOIN, etc.) |
| **Speed** | Extremely fast | Slower due to disk I/O |
| **Transactions** | Basic atomic operations, LUA scripting | ACID-compliant transactions |
| **Use Case** | Caching, session storage, real-time data | Traditional applications with relational data |

### Does Redis Have Tables Like PostgreSQL?
No, Redis does not have a traditional table structure. Instead, data is stored as key-value pairs. However, using **hashes** allows storing structured data similar to a table:

#### Example: Storing User Data
```bash
HSET user:1001 name "Ashfaq" age 30 city "Bangalore"
HGETALL user:1001
```
**Output:**
```
name: Ashfaq
age: 30
city: Bangalore
```
This is similar to a row in a table with columns.

---

# Where Does Redis Store Data?
Redis stores data **in RAM**, making it extremely fast. However, it supports two persistence mechanisms:
1. **RDB (Redis Database File):** Periodic snapshots saved to disk.
2. **AOF (Append-Only File):** Logs every write operation for durability.

### Why is Redis Faster Than Traditional Databases?
- **Memory-Based Storage:** No disk I/O bottlenecks.
- **Single-Threaded with Event Loop:** Efficient request handling without context switching.
- **Optimized Data Structures:** Tailored for quick lookups and minimal overhead.
- **Pipeline Processing:** Supports batching multiple commands to reduce network latency.

---

# Additional Resources
- [Redis Official Documentation](https://redis.io/docs/)
- [Redis Commands Cheat Sheet](https://redis.io/commands/)
- [Redis Use Cases](https://redis.io/use-cases/)
- [Redis vs. Other Databases](https://redis.io/docs/compare/)

Redis is a powerful tool for scenarios requiring high-speed data retrieval, caching, and real-time analytics. Understanding its architecture and strengths helps in making informed choices for application development.