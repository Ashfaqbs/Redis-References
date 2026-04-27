# Redis Eviction Policies

When Redis hits its `maxmemory` limit, it must decide which key to remove to make room for new data. This decision is controlled by the `maxmemory-policy` setting.

---

## The 8 Eviction Policies

```conf
maxmemory-policy <policy-name>
```

### Policy breakdown

| Policy | Evicts from | Algorithm | Notes |
|--------|-------------|-----------|-------|
| `noeviction` | — | None | Returns error on write. Default. |
| `allkeys-lru` | All keys | LRU | Most common for pure cache setups. |
| `volatile-lru` | Only keys with TTL | LRU | Protects permanent keys. |
| `allkeys-lfu` | All keys | LFU | Better than LRU for skewed access patterns. |
| `volatile-lfu` | Only keys with TTL | LFU | LFU + protection of permanent keys. |
| `allkeys-random` | All keys | Random | Only when all keys are equally evictable. |
| `volatile-random` | Only keys with TTL | Random | Random eviction, TTL-scoped. |
| `volatile-ttl` | Only keys with TTL | Smallest TTL first | Evicts soonest-to-expire first. |

---

## LRU — Least Recently Used

**Core idea:** The key that was accessed least recently is the least valuable — evict it first.

**How Redis implements it:** Redis does not maintain a true LRU doubly-linked list (too much memory). Instead, each key stores a 24-bit LRU clock timestamp of its last access. When eviction is needed, Redis samples a random set of `maxmemory-samples` keys (default: 5) and evicts the one with the oldest timestamp.

```conf
maxmemory-samples 10   # higher = more accurate LRU, slightly more CPU
```

**Good for:**
- Workloads where recently accessed data is likely to be accessed again soon (temporal locality)
- General-purpose caches: product pages, user profiles, API responses

**Bad for:**
- Workloads with scan patterns — a one-time bulk scan pollutes the LRU clock and evicts actually-hot keys

---

## LFU — Least Frequently Used

**Core idea:** The key accessed the fewest times is the least valuable — evict it first.

**How Redis implements it:** Each key stores a frequency counter (Morris counter — probabilistic, not exact) and a decay timestamp. The counter increments on access and decays over time, so frequency is measured as "recent frequency", not lifetime frequency.

Two config knobs control the decay:

```conf
lfu-log-factor 10      # higher = slower counter growth (default 10)
lfu-decay-time 1       # minutes before counter halves on idle (default 1)
```

**Good for:**
- Workloads with Zipf-like distribution — a small set of very hot keys, and a long tail of rarely accessed keys
- When you want to keep keys that are accessed often, not just recently
- Avoids the scan-pollution problem that LRU has

**Bad for:**
- Workloads where access patterns shift rapidly — a key that was hot last hour but cold now would stay in cache too long unless decay is tuned aggressively

---

## LRU vs LFU — When to Choose Which

| Scenario | Recommendation |
|----------|---------------|
| Cache for general web content (product pages, user data) | `allkeys-lru` |
| Cache with highly skewed access (20% keys get 80% of reads) | `allkeys-lfu` |
| Mixed Redis (some permanent session/config + some cache keys) | `volatile-lru` or `volatile-lfu` |
| Bulk scan or batch jobs are common | `allkeys-lfu` (LRU gets polluted by scans) |
| Access patterns shift frequently (news feed, trending items) | `allkeys-lru` with short TTL |

---

## noeviction — When Redis Refuses to Evict

With `noeviction`, Redis returns an error on any write operation when memory is full:

```
(error) OOM command not allowed when used memory > 'maxmemory'
```

Reads still work. Only writes fail.

**When to use:**
- Redis is used as a primary database or queue (not a cache) — data loss is unacceptable
- You want to fail loudly and trigger alerts rather than silently losing data

**Not appropriate for cache-only setups** — the application will start getting write errors under load.

---

## volatile-ttl — Evict the Key Closest to Expiry

Evicts the key with the smallest remaining TTL. The logic: if a key is about to expire anyway, evict it early rather than keeping it until its TTL runs out.

**When to use:**
- Keys have meaningful TTLs that represent their staleness (e.g., short-lived OTPs, rate limit windows)
- Preferred when the TTL itself is a signal of how stale or expendable the data is

**Pitfall:** If most keys have no TTL, this falls back to noeviction behavior (nothing in the volatile pool to evict).

---

## Setting Maxmemory and Policy

```bash
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru
maxmemory-samples 10

# Or at runtime (does not persist across restarts)
CONFIG SET maxmemory 2147483648
CONFIG SET maxmemory-policy allkeys-lru

# Verify
CONFIG GET maxmemory
CONFIG GET maxmemory-policy
```

---

## Monitoring Evictions

```bash
INFO stats | grep evicted_keys
```

A non-zero `evicted_keys` means Redis is under memory pressure and actively removing data.

If evictions are happening in a pure-cache setup: expected — increase `maxmemory` if cache hit rate drops, or reduce TTLs to let less-useful data expire naturally first.

If evictions are happening when `noeviction` was expected: check if `maxmemory` is set at all (0 = unlimited in Redis, eviction never triggers).

### Cache hit rate

```bash
INFO stats
# Look for:
# keyspace_hits
# keyspace_misses
```

```
hit_rate = keyspace_hits / (keyspace_hits + keyspace_misses)
```

Target: >90% for a healthy cache. If hit rate drops, it usually means:
- TTLs are too short
- `maxmemory` is too small causing aggressive eviction
- Access patterns changed and the cached data is no longer relevant

---

## Quick Decision Guide

```
Is Redis used as a pure cache (can always reload from DB)?
  YES → allkeys-lru (default choice) or allkeys-lfu (skewed access)

Does Redis hold both permanent data and cache data?
  YES → volatile-lru or volatile-lfu
        (only evict keys that have a TTL — permanent keys are safe)

Is Redis a primary store or queue (data loss not acceptable)?
  YES → noeviction

Are TTLs meaningful signals of staleness?
  YES → volatile-ttl
```
