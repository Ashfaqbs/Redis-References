# What to Cache — Deciding What Belongs in Redis

Not everything should be cached. Caching the wrong data wastes memory, adds complexity, and can serve stale results. The goal is to cache data that gives the highest read speedup at the lowest risk of inconsistency.

---

## The Core Question

Before caching anything, ask:

1. **Is this read frequently?** — The more often it is read, the more value caching adds.
2. **Is it expensive to compute or fetch?** — DB query with joins, external API call, heavy computation.
3. **Does it change rarely?** — Frequently mutated data is hard to keep consistent in cache.
4. **Is serving slightly stale data acceptable?** — If yes, caching is safe. If not, careful invalidation is needed.

All four in your favor = strong cache candidate. Only one = think twice.

---

## Good Cache Candidates

### 1. Frequently Read, Rarely Written Data

Data that many users read but almost nobody writes. The read-to-write ratio is the key metric.

**Examples:**
- Product catalog (names, prices, descriptions) — read by every user, updated by admins occasionally
- Country/city lists, currency codes, timezone data
- Application config and feature flags
- Static reference data (categories, tags, enums from DB)

```java
@Cacheable(value = "product", key = "#productId")
public Product getProduct(Long productId) {
    return productRepository.findById(productId).orElseThrow();
}
```

**Appropriate TTL:** Long (minutes to hours). Pair with `@CacheEvict` when the data is updated.

---

### 2. Expensive Database Queries

Queries that involve multiple JOINs, aggregations, or full-table scans — especially when the result is the same for many requests.

**Examples:**
- Dashboard totals: `SELECT COUNT(*), SUM(revenue) FROM orders WHERE status='completed'`
- Complex filtered lists (e.g., top 50 active products by category with ratings)
- Reports or summaries that are expensive to compute but only need to refresh every few minutes

```java
@Cacheable(value = "dashboard:revenue", key = "#tenantId")
public RevenueStats getRevenueSummary(Long tenantId) {
    return reportRepository.computeRevenueSummary(tenantId); // heavy query
}
```

**Appropriate TTL:** Short to medium (30 seconds to 5 minutes). Stale by a few minutes is usually acceptable for analytics.

---

### 3. External API Responses

Calls to third-party services (payment gateways, weather APIs, geocoding, exchange rates). These are slow (network latency), rate-limited, and often costly per-call.

**Examples:**
- Currency exchange rates (refresh every hour)
- Geocoding results for a zip code
- SMS/email delivery status (poll once, cache result)
- Third-party user profile enrichment

```java
@Cacheable(value = "exchange:rate", key = "#currencyPair")
public ExchangeRate getRate(String currencyPair) {
    return externalRateApi.fetch(currencyPair);
}
```

**Appropriate TTL:** Matches the staleness tolerance of the data. Exchange rates every hour, weather every 10 minutes.

---

### 4. Session Data and Auth Tokens

User session state and JWT validation lookups need to be fast because they happen on every request.

**Examples:**
- User session (userId, roles, preferences) after login
- JWT blocklist (invalidated tokens that haven't expired yet)
- OAuth2 access tokens for service-to-service calls
- OTPs and email verification codes (with short TTL)

```java
// Store after login
redisTemplate.opsForValue().set(
    "session:" + sessionId,
    serialize(userSession),
    Duration.ofMinutes(30)
);

// Fetch on each request
UserSession session = deserialize(redisTemplate.opsForValue().get("session:" + sessionId));
```

**Appropriate TTL:** Matches session timeout (15-60 minutes). Sliding expiry — reset TTL on each access.

---

### 5. Computed / Aggregated Results That Are Costly to Rebuild

Pre-computed results that take significant CPU or I/O to generate.

**Examples:**
- ML model inference results for a given input (cache by input hash)
- Rendered HTML fragments for repeated page sections
- Search result pages for popular queries
- Recommendation lists for active users

```java
@Cacheable(value = "recommendations", key = "#userId")
public List<Product> getRecommendations(Long userId) {
    return mlService.computeRecommendations(userId); // expensive
}
```

**Appropriate TTL:** Medium (5-30 minutes). Personalized results need shorter TTL than generic ones.

---

### 6. Rate Limit and Throttle Counters

Request counts per user/IP per time window. Must be fast (runs on every request) and automatically expire.

```java
String key = "rate:user:" + userId + ":" + currentMinute();
Long count = redisTemplate.opsForValue().increment(key);
redisTemplate.expire(key, Duration.ofMinutes(1));
if (count > RATE_LIMIT) throw new RateLimitExceededException();
```

---

### 7. Leaderboards and Real-Time Rankings

Sorted Sets make leaderboard reads O(log N). Recomputing rankings from a DB on every request is expensive.

```bash
ZADD leaderboard 1500 "user:42"
ZREVRANGE leaderboard 0 9 WITHSCORES   # top 10
```

---

## Bad Cache Candidates

### Data that changes on every write

If a value is written as often as it is read, the cache eviction + update overhead may exceed the benefit.

**Example:** A real-time stock ticker updating every 100ms. Caching it adds write complexity with minimal read gain.

**Alternative:** Pub/Sub or Redis Streams to push updates directly.

---

### User-specific data with high cardinality and low reuse

Caching a value per user only makes sense if the same user reads it repeatedly before it changes. If each user reads their data once and moves on, the cache fills with data that is never reused.

**Example:** A one-time generated export/report for each user. Cache it only if the user is likely to re-download it.

---

### Data with strict consistency requirements

If serving a stale value causes a bug or a financial discrepancy, do not cache without a clear invalidation strategy.

**Examples:**
- Current account balance (cache read-your-own-writes only, with strong invalidation on write)
- Inventory count used for purchase decisions — cache with short TTL and accept occasional oversell, or skip caching entirely

---

### Large objects with low read frequency

Caching a 500KB serialized object that is read once every hour wastes memory that could hold thousands of small hot keys.

**Rule:** If the object is large, consider caching only the fields you actually read, not the whole object. Use Redis Hashes instead of serializing the full object.

---

## TTL Sizing Guide

| Data Type | Suggested TTL |
|-----------|--------------|
| OTP / verification code | 5–10 minutes |
| User session | 15–60 minutes (sliding) |
| Rate limit window | Duration of the window (e.g., 1 minute) |
| API response (frequently changing) | 30 seconds – 5 minutes |
| Product/catalog data | 10–60 minutes |
| Static reference data (countries, currencies) | 1–24 hours |
| ML inference result | 5–30 minutes |
| Report / aggregation | 1–10 minutes |
| Leaderboard | No TTL (update in place with ZINCRBY) |

---

## The Caching Decision Checklist

Before adding a new cache key, go through this:

- [ ] Read frequency: is this read more than it is written?
- [ ] Cost: is fetching it from the source slow or expensive?
- [ ] Staleness tolerance: can users tolerate data that is N seconds/minutes old?
- [ ] Invalidation: when the underlying data changes, how does the cache get updated?
- [ ] TTL: is there a sensible TTL set? No eternal keys.
- [ ] Key naming: follows the `service:entity:id` convention?
- [ ] Memory impact: is the cached object small enough to be worth it at scale?

If any of these has no clear answer, the data probably should not be cached yet.
