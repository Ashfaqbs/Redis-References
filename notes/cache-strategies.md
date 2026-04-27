# Redis Cache Strategies

Notes on the main caching patterns — when to use each, the trade-offs, and how they map to Spring Boot / Redis.

---

## The Analogy: Whiteboard + Notebook

Think of:
- **Cache (Redis)** = a whiteboard on the wall — fast to read and write, visible immediately
- **Database (PostgreSQL)** = a notebook locked in a drawer — durable, permanent, but slower to access

Every strategy is just a different rule for **when to write to the whiteboard vs. the notebook**.

---

## 1. Cache-Aside (Lazy Loading)

**Analogy:** Check the whiteboard first. If the answer is not there, go open the notebook, read it, write it on the whiteboard, then answer. Next time someone asks the same question, the answer is already on the whiteboard.

**Flow:**
```
Read:
  1. Check cache → hit → return data
  2. Cache miss → query DB → store result in cache → return data

Write:
  1. Write to DB
  2. Invalidate or update the cache key
```

**How it works in Spring:**
```java
@Cacheable(value = "note", key = "#noteId")
public Note getNoteById(Long noteId) {
    return noteRepository.findById(noteId).orElseThrow();
}

@CacheEvict(value = "note", key = "#id")
public void deleteNote(Long id) {
    noteRepository.deleteById(id);
}
```

**Pros:**
- Cache only holds data that is actually requested (no wasted memory)
- DB is always the source of truth — cache failures don't break the app

**Cons:**
- First request always hits the DB (cache miss penalty)
- Potential for stale data if the cache is not invalidated correctly

**When to use:** Default strategy for most read-heavy workloads. This is what `@Cacheable` implements.

---

## 2. Read-Through

**Analogy:** You never touch the notebook yourself. You ask an assistant (the cache layer). If the assistant does not have it, they fetch it from the notebook and hand it to you. You always talk to the assistant only.

**Flow:**
```
Read:
  1. Always ask the cache
  2. Cache layer handles the DB fetch on a miss — transparent to the caller
```

**Key difference from Cache-Aside:** In Cache-Aside, the application code handles the miss (fetch from DB + populate cache). In Read-Through, the cache library handles it transparently. Spring's `@Cacheable` can behave as Read-Through if combined with a `CacheLoader`.

**Pros:**
- Application code is clean — no conditional cache-miss logic
- Cache is always warm for recently requested data

**Cons:**
- More complex cache configuration (requires a cache loader)
- Cold start problem: first requests still hit the DB

**When to use:** When you want the cache miss logic completely out of the application code.

---

## 3. Write-Through

**Analogy:** Every time you update something, you write it on the whiteboard AND in the notebook at the same time, before you tell anyone the write is done.

**Flow:**
```
Write:
  1. Write to cache
  2. Write to DB (synchronously, in the same operation)
  3. Return success only after both writes complete
```

**How it works in Spring:**
```java
@CachePut(value = "note", key = "#note.id")
public Note createNote(Note note) {
    return noteRepository.save(note); // saves to DB and puts in cache
}
```

**Pros:**
- Cache and DB are always in sync
- Reads after writes never return stale data

**Cons:**
- Every write is slower because it blocks on two writes (cache + DB)
- Cache fills up with data that may never be read again (unnecessary writes for write-heavy workloads)

**When to use:** Read-heavy workloads where consistency between cache and DB matters and write latency is acceptable.

**The problem from the video:** This is what was causing the slow writes. Writing to both Redis and PostgreSQL on every create/update added latency proportional to both write operations.

---

## 4. Write-Behind (Write-Back)

**Analogy:** You write on the whiteboard immediately and tell the caller "done." An assistant quietly copies it into the notebook later in the background. The caller never waits for the notebook.

**Flow:**
```
Write:
  1. Write to cache immediately → return success to caller
  2. Background process batches and flushes dirty cache entries to DB asynchronously
```

**Pros:**
- Writes are extremely fast — only one synchronous write (to cache)
- DB writes are batched, reducing load on the database
- Great for write-heavy workloads (event tracking, counters, analytics)

**Cons:**
- Risk of data loss if the cache crashes before the background flush happens (Redis is not fully durable by default)
- More complex to implement — requires a background worker/queue
- Harder to guarantee ordering of writes

**Spring Boot implementation approach:**
Spring Cache annotations (`@Cacheable`, `@CachePut`) do not natively support Write-Behind. It needs to be implemented manually:

```java
// Write to Redis immediately
public Note createNote(Note note) {
    redisTemplate.opsForHash().put("notes", note.getId().toString(), note);
    // Publish to an async queue (Kafka, Redis Stream, or @Async)
    eventPublisher.publishEvent(new NoteSaveEvent(note));
    return note;
}

// Background listener flushes to DB
@Async
@EventListener
public void handleNoteSave(NoteSaveEvent event) {
    noteRepository.save(event.getNote());
}
```

Or use Redis Streams / Kafka as the async write buffer.

**When to use:**
- High-throughput write workloads where DB write latency is the bottleneck
- Counters, view counts, session data, activity logs
- Acceptable to lose a small window of data on a crash

**When NOT to use:**
- Financial transactions, order processing — cannot tolerate data loss
- Anywhere strong consistency between cache and DB is required

---

## 5. Refresh-Ahead

**Analogy:** Before the note on the whiteboard expires, someone proactively rewrites it with a fresh copy from the notebook — so by the time you look at it, it is already up to date.

**Flow:**
```
Read:
  1. Return from cache as normal
  2. If TTL is about to expire, trigger a background refresh from DB
  3. Cache is refreshed before it goes stale
```

**Pros:**
- Near-zero cache miss latency for hot data
- Avoids thundering herd on cache expiry (many requests hitting DB simultaneously on a miss)

**Cons:**
- Wastes resources refreshing data that may not be requested again
- Requires tracking TTL and scheduling refresh jobs

**When to use:** High-traffic keys with predictable access patterns (e.g., homepage banners, featured products, leaderboards).

---

## Strategy Comparison

| Strategy | Write Path | Read Path | Consistency | Write Speed | Complexity |
|----------|-----------|-----------|-------------|-------------|------------|
| Cache-Aside | DB only, evict cache | Cache → miss → DB | Eventual | Normal | Low |
| Read-Through | DB only, evict cache | Cache (auto-loads) | Eventual | Normal | Medium |
| Write-Through | Cache + DB (sync) | Cache | Strong | Slow | Low-Medium |
| Write-Behind | Cache only (DB async) | Cache | Eventual | Fast | High |
| Refresh-Ahead | Any | Cache (pre-warmed) | Eventual | Normal | High |

---

## Key Takeaway

The video pointed out that **Write-Through** causes slow writes because every write blocks on both the cache and the DB. **Write-Behind** solves this by making the DB write asynchronous — the caller only waits for the cache write, and the DB is updated in the background. The trade-off is potential data loss on a cache crash, which is acceptable for non-critical high-throughput writes but not for financial or transactional data.

For most Spring Boot + Redis + PostgreSQL applications:
- Default to **Cache-Aside** for reads
- Use **Write-Through** (`@CachePut`) when consistency matters and write volume is low
- Use **Write-Behind** when write throughput is the bottleneck and eventual DB consistency is acceptable
